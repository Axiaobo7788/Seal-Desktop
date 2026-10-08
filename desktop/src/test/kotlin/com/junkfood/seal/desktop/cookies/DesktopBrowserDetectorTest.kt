package com.junkfood.seal.desktop.cookies

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopBrowserDetectorTest {
    @Test
    fun `linux exposes only initialized native flatpak and snap profiles`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.Linux)
        system.file("/usr/bin/google-chrome")
        system.executable("google-chrome", "/usr/bin/google-chrome")
        system.directory("/home/test/.config/google-chrome/Default")
        system.file("/home/test/.config/google-chrome/Default/Preferences")
        system.directory("/home/test/.var/app/com.brave.Browser")
        system.directory("/home/test/.var/app/com.brave.Browser/config/BraveSoftware/Brave-Browser/Profile 1")
        system.file("/home/test/.var/app/com.brave.Browser/config/BraveSoftware/Brave-Browser/Profile 1/Network/Cookies")
        system.directory("/home/test/snap/chromium")

        val result = DesktopBrowserDetector(system).detect()

        val chrome = result.installation(SupportedBrowser.Chrome)
        assertEquals(BrowserAvailability.Available, chrome.availability)
        assertEquals(listOf("Default"), chrome.profiles.map { it.displayName })
        assertTrue(BrowserInstallationSource.Path in chrome.sources)

        val brave = result.installation(SupportedBrowser.Brave)
        assertEquals(BrowserAvailability.Available, brave.availability)
        assertEquals(BrowserInstallationSource.Flatpak, brave.profiles.single().source)

        val chromium = result.installation(SupportedBrowser.Chromium)
        assertEquals(BrowserAvailability.InstalledNoProfile, chromium.availability)
        assertTrue(BrowserInstallationSource.Snap in chromium.sources)
        assertEquals(BrowserAvailability.NotInstalled, result.installation(SupportedBrowser.Safari).availability)
    }

    @Test
    fun `firefox profiles ini uses display names without exposing profile path`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.Linux)
        system.executable("firefox", "/usr/bin/firefox")
        system.file("/usr/bin/firefox")
        system.directory("/home/test/.mozilla/firefox")
        system.directory("/home/test/.mozilla/firefox/abc123.default-release")
        system.file("/home/test/.mozilla/firefox/abc123.default-release/cookies.sqlite")
        system.text(
            "/home/test/.mozilla/firefox/profiles.ini",
            """[Profile0]
Name=Personal
IsRelative=1
Path=abc123.default-release
Default=1
""",
        )

        val firefox = DesktopBrowserDetector(system).detect().installation(SupportedBrowser.Firefox)

        val profile = firefox.profiles.single()
        assertEquals("Personal", profile.displayName)
        assertFalse(profile.id.contains("abc123"))
        assertFalse(profile.id.contains("/home/test"))
        assertTrue(profile.isDefault)
    }

    @Test
    fun `installed browser without initialized profile is not extractable`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.Windows)
        system.executable("msedge.exe", "C:/Program Files/Microsoft/Edge/Application/msedge.exe")
        system.file("C:/Program Files/Microsoft/Edge/Application/msedge.exe")

        val edge = DesktopBrowserDetector(system).detect().installation(SupportedBrowser.Edge)

        assertEquals(BrowserAvailability.InstalledNoProfile, edge.availability)
        assertFalse(edge.canExtract)
    }

    @Test
    fun `windows app paths detection is read only and browser specific`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.Windows)
        system.registryExecutable("chrome.exe", "C:/Apps/Chrome/chrome.exe")
        system.file("C:/Apps/Chrome/chrome.exe")
        system.directory("/home/test/AppData/Local/Google/Chrome/User Data/Default")
        system.file("/home/test/AppData/Local/Google/Chrome/User Data/Default/Preferences")

        val result = DesktopBrowserDetector(system).detect()

        val chrome = result.installation(SupportedBrowser.Chrome)
        assertEquals(BrowserAvailability.Available, chrome.availability)
        assertTrue(BrowserInstallationSource.WindowsRegistry in chrome.sources)
        assertEquals(BrowserAvailability.NotInstalled, result.installation(SupportedBrowser.Chromium).availability)
    }

    @Test
    fun `missing previously selected profile is reported without disabling other profiles`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.Linux)
        system.executable("google-chrome", "/usr/bin/google-chrome")
        system.file("/usr/bin/google-chrome")
        system.directory("/home/test/.config/google-chrome/Default")
        system.file("/home/test/.config/google-chrome/Default/Preferences")

        val result =
            DesktopBrowserDetector(system).detect(
                DesktopBrowserSelection(SupportedBrowser.Chrome, profileId = "standard:removed"),
            )

        val chrome = result.installation(SupportedBrowser.Chrome)
        assertEquals(BrowserAvailability.PreviouslySelectedButUnavailable, chrome.availability)
        assertTrue(chrome.profiles.isNotEmpty())
        assertNull(result.resolve(DesktopBrowserSelection(SupportedBrowser.Chrome, "standard:removed")))
    }

    @Test
    fun `mac safari appears only after application and profile data exist`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.MacOs)
        system.directory("/Applications/Safari.app")

        val before = DesktopBrowserDetector(system).detect().installation(SupportedBrowser.Safari)
        assertEquals(BrowserAvailability.InstalledNoProfile, before.availability)

        system.directory("/home/test/Library/Safari")
        val after = DesktopBrowserDetector(system).detect().installation(SupportedBrowser.Safari)
        assertEquals(BrowserAvailability.Available, after.availability)
        assertNull(after.profiles.single().extractionValue)
    }

    @Test
    fun `legacy browser selection resolves the detected default profile`() {
        val system = FakeBrowserSystem(DesktopOperatingSystem.Linux)
        system.executable("google-chrome", "/usr/bin/google-chrome")
        system.file("/usr/bin/google-chrome")
        system.directory("/home/test/.config/google-chrome/Profile 1")
        system.file("/home/test/.config/google-chrome/Profile 1/Preferences")
        system.directory("/home/test/.config/google-chrome/Default")
        system.file("/home/test/.config/google-chrome/Default/Preferences")
        val result = DesktopBrowserDetector(system).detect()

        val resolved = result.resolve(DesktopBrowserSelection(SupportedBrowser.Chrome))

        assertNotNull(resolved)
        assertEquals("Default", resolved.second.displayName)
    }

    private fun DesktopBrowserDetectionResult.installation(browser: SupportedBrowser): BrowserInstallation =
        installations.single { it.browser == browser }

    private class FakeBrowserSystem(
        override val operatingSystem: DesktopOperatingSystem,
        override val userHome: Path = Path.of("/home/test"),
    ) : DesktopBrowserSystem {
        private val directories = linkedSetOf<Path>()
        private val files = linkedSetOf<Path>()
        private val texts = mutableMapOf<Path, String>()
        private val executables = mutableMapOf<String, Path>()
        private val registry = mutableMapOf<String, MutableList<Path>>()
        private val environment = mutableMapOf<String, String>()

        fun directory(value: String) {
            var current: Path? = Path.of(value)
            while (current != null) {
                directories.add(current)
                current = current.parent
            }
        }

        fun file(value: String) {
            files.add(Path.of(value))
        }

        fun text(value: String, contents: String) {
            val path = Path.of(value)
            files.add(path)
            texts[path] = contents
        }

        fun executable(name: String, value: String) {
            executables[name] = Path.of(value)
        }

        fun registryExecutable(name: String, value: String) {
            registry.getOrPut(name) { mutableListOf() }.add(Path.of(value))
        }

        override fun environment(name: String): String? = environment[name]

        override fun isDirectory(path: Path): Boolean = path in directories

        override fun isRegularFile(path: Path): Boolean = path in files

        override fun listDirectories(path: Path): List<Path> =
            directories.filter { it.parent == path }

        override fun readText(path: Path): String? = texts[path]

        override fun findExecutable(names: List<String>): Path? = names.firstNotNullOfOrNull(executables::get)

        override fun queryWindowsAppPaths(executableName: String): List<Path> = registry[executableName].orEmpty()
    }
}
