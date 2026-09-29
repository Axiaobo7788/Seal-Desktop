package com.junkfood.seal.desktop.cookies

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

enum class BrowserAvailability {
    NotInstalled,
    InstalledNoProfile,
    Available,
    PreviouslySelectedButUnavailable,
}

enum class BrowserInstallationSource(val stableId: String) {
    StandardLocation("standard"),
    Path("path"),
    WindowsRegistry("registry"),
    Flatpak("flatpak"),
    Snap("snap"),
    MacApplication("mac-app"),
}

data class BrowserProfile(
    val id: String,
    val displayName: String,
    val isDefault: Boolean,
    internal val extractionValue: String?,
    val source: BrowserInstallationSource,
)

data class BrowserInstallation(
    val browser: SupportedBrowser,
    val availability: BrowserAvailability,
    val profiles: List<BrowserProfile>,
    val sources: Set<BrowserInstallationSource>,
) {
    val canExtract: Boolean
        get() = profiles.isNotEmpty() && availability != BrowserAvailability.NotInstalled
}

data class DesktopBrowserSelection(
    val browser: SupportedBrowser,
    val profileId: String = "",
)

data class DesktopBrowserDetectionResult(
    val installations: List<BrowserInstallation>,
) {
    val available: List<BrowserInstallation>
        get() = installations.filter { it.canExtract }

    val installedWithoutProfiles: List<BrowserInstallation>
        get() = installations.filter { it.availability == BrowserAvailability.InstalledNoProfile }

    val previouslySelectedUnavailable: BrowserInstallation?
        get() = installations.firstOrNull { it.availability == BrowserAvailability.PreviouslySelectedButUnavailable }

    fun resolve(selection: DesktopBrowserSelection?): Pair<BrowserInstallation, BrowserProfile>? {
        if (selection == null) return null
        val installation = installations.firstOrNull { it.browser == selection.browser && it.canExtract } ?: return null
        val profile =
            installation.profiles.firstOrNull { it.id == selection.profileId }
                ?: selection.profileId.takeIf { it.isNotBlank() }?.let { return null }
                ?: installation.profiles.firstOrNull { it.isDefault }
                ?: installation.profiles.firstOrNull()
                ?: return null
        return installation to profile
    }
}

internal enum class DesktopOperatingSystem {
    Windows,
    Linux,
    MacOs,
    Other,
}

internal interface DesktopBrowserSystem {
    val operatingSystem: DesktopOperatingSystem
    val userHome: Path

    fun environment(name: String): String?

    fun isDirectory(path: Path): Boolean

    fun isRegularFile(path: Path): Boolean

    fun listDirectories(path: Path): List<Path>

    fun readText(path: Path): String?

    fun findExecutable(names: List<String>): Path?

    fun queryWindowsAppPaths(executableName: String): List<Path>
}

internal class DesktopBrowserDetector(
    private val system: DesktopBrowserSystem = DefaultDesktopBrowserSystem(),
) {
    fun detect(previousSelection: DesktopBrowserSelection? = null): DesktopBrowserDetectionResult {
        val installations =
            SupportedBrowser.entries.map { browser ->
                detectBrowser(browser, previousSelection?.takeIf { it.browser == browser })
            }
        return DesktopBrowserDetectionResult(installations)
    }

    private fun detectBrowser(
        browser: SupportedBrowser,
        previousSelection: DesktopBrowserSelection?,
    ): BrowserInstallation {
        val candidate = candidateFor(browser)
        val sources = linkedSetOf<BrowserInstallationSource>()
        if (system.findExecutable(candidate.executableNames) != null) sources += BrowserInstallationSource.Path
        if (candidate.standardExecutables.any(system::isRegularFile)) sources += BrowserInstallationSource.StandardLocation
        if (system.operatingSystem == DesktopOperatingSystem.Windows &&
            candidate.executableNames.filter { it.endsWith(".exe", ignoreCase = true) }
                .any { system.queryWindowsAppPaths(it).any(system::isRegularFile) }
        ) {
            sources += BrowserInstallationSource.WindowsRegistry
        }
        candidate.installMarkers.forEach { marker ->
            if (system.isDirectory(marker.path) || system.isRegularFile(marker.path)) sources += marker.source
        }

        val profiles =
            candidate.profileRoots.flatMap { root ->
                if (!system.isDirectory(root.path)) emptyList() else detectProfiles(browser, root)
            }.distinctBy { it.id }.let(::disambiguateProfileNames)
        sources += profiles.map { it.source }

        val installed = sources.isNotEmpty() || profiles.isNotEmpty()
        val selectionAvailable =
            previousSelection == null ||
                (installed && profiles.any { previousSelection.profileId.isBlank() || it.id == previousSelection.profileId })
        val availability =
            when {
                previousSelection != null && !selectionAvailable -> BrowserAvailability.PreviouslySelectedButUnavailable
                !installed -> BrowserAvailability.NotInstalled
                profiles.isEmpty() -> BrowserAvailability.InstalledNoProfile
                else -> BrowserAvailability.Available
            }
        return BrowserInstallation(browser, availability, profiles, sources)
    }

    private fun detectProfiles(browser: SupportedBrowser, root: BrowserDataRoot): List<BrowserProfile> =
        when (browser) {
            SupportedBrowser.Firefox -> detectFirefoxProfiles(root)
            SupportedBrowser.Safari -> detectSafariProfile(root)
            else -> detectChromiumProfiles(browser, root)
        }

    private fun detectChromiumProfiles(browser: SupportedBrowser, root: BrowserDataRoot): List<BrowserProfile> {
        val directories =
            buildList {
                if (browser == SupportedBrowser.Opera && isInitializedChromiumProfile(root.path)) add(root.path)
                addAll(
                    system.listDirectories(root.path).filter { directory ->
                        val name = directory.fileName?.toString().orEmpty()
                        (name == "Default" || name.startsWith("Profile ")) && isInitializedChromiumProfile(directory)
                    },
                )
            }
        return directories.map { directory ->
            val rawName = directory.fileName?.toString().orEmpty()
            val isDefault = browser == SupportedBrowser.Opera || rawName == "Default"
            browserProfile(
                path = directory,
                displayName = if (isDefault) "Default" else rawName,
                isDefault = isDefault,
                source = root.source,
            )
        }
    }

    private fun isInitializedChromiumProfile(path: Path): Boolean =
        listOf("Preferences", "Cookies", "Network/Cookies", "History")
            .map(path::resolve)
            .any(system::isRegularFile)

    private fun detectFirefoxProfiles(root: BrowserDataRoot): List<BrowserProfile> {
        val fromIni = parseFirefoxProfilesIni(root)
        if (fromIni.isNotEmpty()) return fromIni
        return system.listDirectories(root.path)
            .filter(::isInitializedFirefoxProfile)
            .map { directory ->
                browserProfile(
                    path = directory,
                    displayName = firefoxFallbackName(directory.fileName?.toString().orEmpty()),
                    isDefault = directory.fileName?.toString()?.contains("default", ignoreCase = true) == true,
                    source = root.source,
                )
            }
    }

    private fun parseFirefoxProfilesIni(root: BrowserDataRoot): List<BrowserProfile> {
        val contents = system.readText(root.path.resolve("profiles.ini")) ?: return emptyList()
        return parseIniSections(contents).mapNotNull { values ->
            val pathValue = values["Path"]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val profilePath =
                if (values["IsRelative"] == "0") runCatching { Path.of(pathValue) }.getOrNull()
                else root.path.resolve(pathValue).normalize()
            if (profilePath == null || !system.isDirectory(profilePath) || !isInitializedFirefoxProfile(profilePath)) {
                return@mapNotNull null
            }
            val isDefault = values["Default"] == "1" || pathValue.contains("default", ignoreCase = true)
            val displayName =
                values["Name"]?.let(::sanitizeProfileName)?.takeIf { it.isNotBlank() }
                    ?: firefoxFallbackName(profilePath.fileName?.toString().orEmpty())
            browserProfile(profilePath, displayName, isDefault, root.source)
        }
    }

    private fun isInitializedFirefoxProfile(path: Path): Boolean =
        listOf("cookies.sqlite", "prefs.js", "places.sqlite").map(path::resolve).any(system::isRegularFile)

    private fun detectSafariProfile(root: BrowserDataRoot): List<BrowserProfile> {
        if (!system.isDirectory(root.path)) return emptyList()
        return listOf(
            BrowserProfile(
                id = stableProfileId(root.source, root.path),
                displayName = "Default",
                isDefault = true,
                extractionValue = null,
                source = root.source,
            ),
        )
    }

    private fun browserProfile(
        path: Path,
        displayName: String,
        isDefault: Boolean,
        source: BrowserInstallationSource,
    ) = BrowserProfile(
        id = stableProfileId(source, path),
        displayName = sanitizeProfileName(displayName).ifBlank { "Profile" },
        isDefault = isDefault,
        extractionValue = path.toAbsolutePath().normalize().toString(),
        source = source,
    )

    private fun disambiguateProfileNames(profiles: List<BrowserProfile>): List<BrowserProfile> {
        val duplicateNames = profiles.groupingBy { it.displayName }.eachCount().filterValues { it > 1 }.keys
        return profiles.map { profile ->
            if (profile.displayName !in duplicateNames) profile
            else profile.copy(displayName = "${profile.displayName} (${profile.source.displayLabel()})")
        }.sortedWith(compareByDescending<BrowserProfile> { it.isDefault }.thenBy { it.displayName.lowercase(Locale.ROOT) })
    }

    private fun candidateFor(browser: SupportedBrowser): BrowserCandidate {
        val home = system.userHome
        val env = system::environment
        return when (system.operatingSystem) {
            DesktopOperatingSystem.Windows -> windowsCandidate(browser, home, env)
            DesktopOperatingSystem.Linux -> linuxCandidate(browser, home, env)
            DesktopOperatingSystem.MacOs -> macCandidate(browser, home)
            DesktopOperatingSystem.Other -> BrowserCandidate(browser.executableNames())
        }
    }
}

private data class BrowserDataRoot(val source: BrowserInstallationSource, val path: Path)

private data class BrowserInstallMarker(val source: BrowserInstallationSource, val path: Path)

private data class BrowserCandidate(
    val executableNames: List<String>,
    val standardExecutables: List<Path> = emptyList(),
    val installMarkers: List<BrowserInstallMarker> = emptyList(),
    val profileRoots: List<BrowserDataRoot> = emptyList(),
)

private fun windowsCandidate(
    browser: SupportedBrowser,
    home: Path,
    environment: (String) -> String?,
): BrowserCandidate {
    val local = environment("LOCALAPPDATA")?.let(Path::of) ?: home.resolve("AppData/Local")
    val roaming = environment("APPDATA")?.let(Path::of) ?: home.resolve("AppData/Roaming")
    val programFiles = listOfNotNull(environment("ProgramFiles"), environment("ProgramFiles(x86)")).map(Path::of)
    val executableSuffixes =
        when (browser) {
            SupportedBrowser.Chrome -> listOf("Google/Chrome/Application/chrome.exe")
            SupportedBrowser.Edge -> listOf("Microsoft/Edge/Application/msedge.exe")
            SupportedBrowser.Firefox -> listOf("Mozilla Firefox/firefox.exe")
            SupportedBrowser.Chromium -> listOf("Chromium/Application/chrome.exe")
            SupportedBrowser.Brave -> listOf("BraveSoftware/Brave-Browser/Application/brave.exe")
            SupportedBrowser.Opera -> listOf("Opera/launcher.exe", "Programs/Opera/launcher.exe")
            SupportedBrowser.Vivaldi -> listOf("Vivaldi/Application/vivaldi.exe")
            SupportedBrowser.Safari -> emptyList()
        }
    val roots =
        when (browser) {
            SupportedBrowser.Chrome -> listOf(local.resolve("Google/Chrome/User Data"))
            SupportedBrowser.Edge -> listOf(local.resolve("Microsoft/Edge/User Data"))
            SupportedBrowser.Firefox -> listOf(roaming.resolve("Mozilla/Firefox"))
            SupportedBrowser.Chromium -> listOf(local.resolve("Chromium/User Data"))
            SupportedBrowser.Brave -> listOf(local.resolve("BraveSoftware/Brave-Browser/User Data"))
            SupportedBrowser.Opera -> listOf(roaming.resolve("Opera Software/Opera Stable"))
            SupportedBrowser.Vivaldi -> listOf(local.resolve("Vivaldi/User Data"))
            SupportedBrowser.Safari -> emptyList()
        }
    return BrowserCandidate(
        executableNames = browser.executableNames(),
        standardExecutables = programFiles.flatMap { base -> executableSuffixes.map(base::resolve) } +
            executableSuffixes.map(local::resolve),
        profileRoots = roots.map { BrowserDataRoot(BrowserInstallationSource.StandardLocation, it) },
    )
}

private fun linuxCandidate(
    browser: SupportedBrowser,
    home: Path,
    environment: (String) -> String?,
): BrowserCandidate {
    val config = environment("XDG_CONFIG_HOME")?.let(Path::of) ?: home.resolve(".config")
    val nativeRoot =
        when (browser) {
            SupportedBrowser.Chrome -> config.resolve("google-chrome")
            SupportedBrowser.Edge -> config.resolve("microsoft-edge")
            SupportedBrowser.Firefox -> home.resolve(".mozilla/firefox")
            SupportedBrowser.Chromium -> config.resolve("chromium")
            SupportedBrowser.Brave -> config.resolve("BraveSoftware/Brave-Browser")
            SupportedBrowser.Opera -> config.resolve("opera")
            SupportedBrowser.Vivaldi -> config.resolve("vivaldi")
            SupportedBrowser.Safari -> null
        }
    val flatpak =
        when (browser) {
            SupportedBrowser.Chrome -> "com.google.Chrome" to "config/google-chrome"
            SupportedBrowser.Edge -> "com.microsoft.Edge" to "config/microsoft-edge"
            SupportedBrowser.Firefox -> "org.mozilla.firefox" to ".mozilla/firefox"
            SupportedBrowser.Chromium -> "org.chromium.Chromium" to "config/chromium"
            SupportedBrowser.Brave -> "com.brave.Browser" to "config/BraveSoftware/Brave-Browser"
            SupportedBrowser.Opera -> "com.opera.Opera" to "config/opera"
            SupportedBrowser.Vivaldi -> "com.vivaldi.Vivaldi" to "config/vivaldi"
            SupportedBrowser.Safari -> null
        }
    val snap =
        when (browser) {
            SupportedBrowser.Chromium -> "chromium" to ".config/chromium"
            SupportedBrowser.Brave -> "brave" to ".config/BraveSoftware/Brave-Browser"
            SupportedBrowser.Firefox -> "firefox" to ".mozilla/firefox"
            SupportedBrowser.Opera -> "opera" to ".config/opera"
            else -> null
        }
    val profileRoots = buildList {
        nativeRoot?.let { add(BrowserDataRoot(BrowserInstallationSource.StandardLocation, it)) }
        flatpak?.let { (appId, suffix) ->
            add(BrowserDataRoot(BrowserInstallationSource.Flatpak, home.resolve(".var/app/$appId/$suffix")))
        }
        snap?.let { (packageName, suffix) ->
            add(BrowserDataRoot(BrowserInstallationSource.Snap, home.resolve("snap/$packageName/current/$suffix")))
        }
    }
    val markers = buildList {
        flatpak?.let { add(BrowserInstallMarker(BrowserInstallationSource.Flatpak, home.resolve(".var/app/${it.first}"))) }
        snap?.let { add(BrowserInstallMarker(BrowserInstallationSource.Snap, home.resolve("snap/${it.first}"))) }
    }
    return BrowserCandidate(browser.executableNames(), installMarkers = markers, profileRoots = profileRoots)
}

private fun macCandidate(browser: SupportedBrowser, home: Path): BrowserCandidate {
    val applicationNames =
        when (browser) {
            SupportedBrowser.Chrome -> listOf("Google Chrome.app")
            SupportedBrowser.Edge -> listOf("Microsoft Edge.app")
            SupportedBrowser.Firefox -> listOf("Firefox.app")
            SupportedBrowser.Chromium -> listOf("Chromium.app")
            SupportedBrowser.Brave -> listOf("Brave Browser.app")
            SupportedBrowser.Opera -> listOf("Opera.app")
            SupportedBrowser.Vivaldi -> listOf("Vivaldi.app")
            SupportedBrowser.Safari -> listOf("Safari.app")
        }
    val roots =
        when (browser) {
            SupportedBrowser.Chrome -> listOf(home.resolve("Library/Application Support/Google/Chrome"))
            SupportedBrowser.Edge -> listOf(home.resolve("Library/Application Support/Microsoft Edge"))
            SupportedBrowser.Firefox -> listOf(home.resolve("Library/Application Support/Firefox"))
            SupportedBrowser.Chromium -> listOf(home.resolve("Library/Application Support/Chromium"))
            SupportedBrowser.Brave -> listOf(home.resolve("Library/Application Support/BraveSoftware/Brave-Browser"))
            SupportedBrowser.Opera -> listOf(home.resolve("Library/Application Support/com.operasoftware.Opera"))
            SupportedBrowser.Vivaldi -> listOf(home.resolve("Library/Application Support/Vivaldi"))
            SupportedBrowser.Safari -> listOf(home.resolve("Library/Safari"))
        }
    val appPaths = applicationNames.flatMap { name -> listOf(Path.of("/Applications/$name"), home.resolve("Applications/$name")) }
    return BrowserCandidate(
        executableNames = browser.executableNames(),
        installMarkers = appPaths.map { BrowserInstallMarker(BrowserInstallationSource.MacApplication, it) },
        profileRoots = roots.map { BrowserDataRoot(BrowserInstallationSource.MacApplication, it) },
    )
}

private fun SupportedBrowser.executableNames(): List<String> =
    when (this) {
        SupportedBrowser.Chrome -> listOf("google-chrome", "google-chrome-stable", "chrome", "chrome.exe")
        SupportedBrowser.Firefox -> listOf("firefox", "firefox.exe")
        SupportedBrowser.Edge -> listOf("microsoft-edge", "microsoft-edge-stable", "msedge", "msedge.exe")
        SupportedBrowser.Chromium -> listOf("chromium", "chromium-browser", "chromium.exe")
        SupportedBrowser.Opera -> listOf("opera", "opera.exe")
        SupportedBrowser.Brave -> listOf("brave-browser", "brave", "brave.exe")
        SupportedBrowser.Vivaldi -> listOf("vivaldi", "vivaldi-stable", "vivaldi.exe")
        SupportedBrowser.Safari -> emptyList()
    }

private fun parseIniSections(contents: String): List<Map<String, String>> {
    val sections = mutableListOf<MutableMap<String, String>>()
    var current: MutableMap<String, String>? = null
    contents.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("[") && line.endsWith("]") -> {
                current = linkedMapOf()
                sections += current!!
            }
            line.isNotEmpty() && !line.startsWith(';') && !line.startsWith('#') -> {
                val separator = line.indexOf('=')
                if (separator > 0) current?.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim())
            }
        }
    }
    return sections
}

private fun sanitizeProfileName(value: String): String =
    value.replace(Regex("[\\/\\r\\n\\t]"), " ").trim().take(80)

private fun firefoxFallbackName(directoryName: String): String {
    val suffix = directoryName.substringAfter('.', directoryName)
    return when {
        suffix.equals("default-release", ignoreCase = true) -> "Default Release"
        suffix.equals("default", ignoreCase = true) -> "Default"
        else -> sanitizeProfileName(suffix)
    }
}

private fun stableProfileId(source: BrowserInstallationSource, path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(path.toAbsolutePath().normalize().toString().toByteArray())
    val shortHash = digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    return "${source.stableId}:$shortHash"
}

private fun BrowserInstallationSource.displayLabel(): String =
    when (this) {
        BrowserInstallationSource.StandardLocation -> "Native"
        BrowserInstallationSource.Path -> "PATH"
        BrowserInstallationSource.WindowsRegistry -> "System"
        BrowserInstallationSource.Flatpak -> "Flatpak"
        BrowserInstallationSource.Snap -> "Snap"
        BrowserInstallationSource.MacApplication -> "Application"
    }

private class DefaultDesktopBrowserSystem : DesktopBrowserSystem {
    override val operatingSystem: DesktopOperatingSystem =
        System.getProperty("os.name").lowercase(Locale.ROOT).let { name ->
            when {
                name.contains("win") -> DesktopOperatingSystem.Windows
                name.contains("mac") || name.contains("darwin") -> DesktopOperatingSystem.MacOs
                name.contains("linux") -> DesktopOperatingSystem.Linux
                else -> DesktopOperatingSystem.Other
            }
        }
    override val userHome: Path = Path.of(System.getProperty("user.home"))

    override fun environment(name: String): String? = System.getenv(name)

    override fun isDirectory(path: Path): Boolean = path.isDirectory()

    override fun isRegularFile(path: Path): Boolean = path.isRegularFile()

    override fun listDirectories(path: Path): List<Path> =
        runCatching {
            Files.list(path).use { entries -> entries.filter(Files::isDirectory).toList() }
        }.getOrDefault(emptyList())

    override fun readText(path: Path): String? = runCatching { Files.readString(path) }.getOrNull()

    override fun findExecutable(names: List<String>): Path? {
        if (names.isEmpty()) return null
        val pathValue = environment("PATH").orEmpty()
        val pathExtensions =
            if (operatingSystem == DesktopOperatingSystem.Windows) {
                environment("PATHEXT").orEmpty().split(';').filter { it.isNotBlank() }.ifEmpty { listOf(".EXE") }
            } else {
                listOf("")
            }
        return pathValue.split(java.io.File.pathSeparatorChar).asSequence()
            .filter { it.isNotBlank() }
            .map(Path::of)
            .flatMap { directory ->
                names.asSequence().flatMap { name ->
                    if (name.contains('.') || pathExtensions == listOf("")) sequenceOf(directory.resolve(name))
                    else pathExtensions.asSequence().map { extension -> directory.resolve(name + extension.lowercase(Locale.ROOT)) }
                }
            }
            .firstOrNull(::isRegularFile)
    }

    override fun queryWindowsAppPaths(executableName: String): List<Path> {
        if (operatingSystem != DesktopOperatingSystem.Windows || executableName.isBlank()) return emptyList()
        val keys =
            listOf(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\$executableName",
                "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\$executableName",
                "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\App Paths\\$executableName",
            )
        return keys.mapNotNull { key ->
            runCatching {
                val process = ProcessBuilder("reg", "query", key, "/ve").redirectErrorStream(true).start()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    return@runCatching null
                }
                if (process.exitValue() != 0) return@runCatching null
                val value = process.inputStream.bufferedReader().readLines()
                    .firstOrNull { it.contains("REG_SZ", ignoreCase = true) }
                    ?.substringAfter("REG_SZ")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                value?.let(Path::of)
            }.getOrNull()
        }
    }
}
