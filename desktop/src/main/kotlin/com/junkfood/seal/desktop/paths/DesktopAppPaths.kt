package com.junkfood.seal.desktop.paths

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private const val STATE_DIR_PROPERTY = "seal.desktop.storage.stateDir"
private const val STATE_DIR_ENVIRONMENT = "SEAL_DESKTOP_STORAGE_STATE_DIR"
private const val AUXILIARY_DIR_PROPERTY = "seal.desktop.auxiliaryDir"
private const val AUXILIARY_DIR_ENVIRONMENT = "SEAL_DESKTOP_AUXILIARY_DIR"
private const val MIGRATION_MARKER = ".legacy-state-migrated"

private val knownStateEntries =
    listOf(
        "queue.json",
        "history.json",
        "app-settings.json",
        "settings.json",
        "theme.json",
        "custom-command-tasks.json",
        "seal.db",
        "seal.db-wal",
        "seal.db-shm",
        "yt-dlp",
    )

internal enum class DesktopPlatform {
    Windows,
    MacOS,
    Unix,
}

internal data class DesktopPathEnvironment(
    val osName: String,
    val userHome: String,
    val xdgStateHome: String? = null,
    val xdgCacheHome: String? = null,
    val xdgDataHome: String? = null,
    val localAppData: String? = null,
    val stateDirectoryProperty: String? = null,
    val stateDirectoryEnvironment: String? = null,
    val auxiliaryDirectoryProperty: String? = null,
    val auxiliaryDirectoryEnvironment: String? = null,
    val temporaryDirectory: String,
)

internal data class DesktopAppPathLayout(
    val platform: DesktopPlatform,
    val stateDirectory: Path,
    val legacyStateDirectory: Path,
    val cacheDirectory: Path,
    val dataDirectory: Path,
    val temporaryDirectory: Path,
    val auxiliaryBinariesDirectory: Path,
    val stateOverrideUsed: Boolean,
)

internal fun resolveDesktopAppPathLayout(environment: DesktopPathEnvironment): DesktopAppPathLayout {
    val platform = desktopPlatformFor(environment.osName)
    val home = Path.of(environment.userHome)
    val legacyState = home.resolve(".local").resolve("state").resolve("seal")
    val stateOverride =
        environment.stateDirectoryProperty.nonBlankPath()
            ?: environment.stateDirectoryEnvironment.nonBlankPath()
    val auxiliaryOverride =
        environment.auxiliaryDirectoryProperty.nonBlankPath()
            ?: environment.auxiliaryDirectoryEnvironment.nonBlankPath()

    val nativeState: Path
    val cache: Path
    val data: Path
    when (platform) {
        DesktopPlatform.Windows -> {
            val localRoot =
                environment.localAppData.nonBlankPath()
                    ?: home.resolve("AppData").resolve("Local")
            nativeState = localRoot.resolve("Seal")
            cache = nativeState.resolve("cache")
            data = nativeState
        }
        DesktopPlatform.MacOS -> {
            nativeState = home.resolve("Library").resolve("Application Support").resolve("Seal")
            cache = home.resolve("Library").resolve("Caches").resolve("Seal")
            data = nativeState
        }
        DesktopPlatform.Unix -> {
            val stateRoot =
                environment.xdgStateHome.nonBlankAbsolutePath()
                    ?: home.resolve(".local").resolve("state")
            val cacheRoot = environment.xdgCacheHome.nonBlankAbsolutePath() ?: home.resolve(".cache")
            val dataRoot =
                environment.xdgDataHome.nonBlankAbsolutePath()
                    ?: home.resolve(".local").resolve("share")
            nativeState = stateRoot.resolve("seal")
            cache = cacheRoot.resolve("seal")
            data = dataRoot.resolve("Seal")
        }
    }

    val selectedState = stateOverride?.resolve("seal") ?: nativeState
    return DesktopAppPathLayout(
        platform = platform,
        stateDirectory = selectedState.normalize(),
        legacyStateDirectory = legacyState.normalize(),
        cacheDirectory = cache.normalize(),
        dataDirectory = data.normalize(),
        temporaryDirectory = Path.of(environment.temporaryDirectory).resolve("seal").normalize(),
        auxiliaryBinariesDirectory =
            (auxiliaryOverride ?: data.resolve("bin")).normalize(),
        stateOverrideUsed = stateOverride != null,
    )
}

internal fun desktopPlatformFor(osName: String): DesktopPlatform {
    val normalized = osName.lowercase()
    return when {
        normalized.contains("win") -> DesktopPlatform.Windows
        normalized.contains("mac") || normalized.contains("darwin") -> DesktopPlatform.MacOS
        else -> DesktopPlatform.Unix
    }
}

internal fun resolveActiveStateDirectory(
    layout: DesktopAppPathLayout,
    migrate: (Path, Path) -> Boolean = ::migrateLegacyStateDirectory,
): Path {
    val native = layout.stateDirectory
    val legacy = layout.legacyStateDirectory
    if (layout.stateOverrideUsed || native == legacy) return native
    if (native.resolve(MIGRATION_MARKER).exists()) return native
    if (hasKnownState(native)) return native
    if (!hasKnownState(legacy)) return native

    return if (migrate(legacy, native)) native else legacy
}

internal fun migrateLegacyStateDirectory(source: Path, target: Path): Boolean {
    val copiedRoots = mutableListOf<Path>()
    return runCatching {
        target.createDirectories()
        for (entryName in knownStateEntries) {
            val sourceEntry = source.resolve(entryName)
            if (!sourceEntry.exists()) continue

            val targetEntry = target.resolve(entryName)
            check(!targetEntry.exists()) { "Target state entry already exists: $targetEntry" }
            copiedRoots.add(targetEntry)
            copyRecursively(sourceEntry, targetEntry)
        }
        writeMigrationMarker(target)
        true
    }.getOrElse { error ->
        copiedRoots.asReversed().forEach(::deleteRecursivelyBestEffort)
        System.err.println(
            "[DesktopAppPaths] Legacy state migration failed; continuing with the legacy directory: ${error.message.orEmpty()}"
        )
        false
    }
}

private fun hasKnownState(directory: Path): Boolean =
    knownStateEntries.any { directory.resolve(it).exists() }

private fun copyRecursively(source: Path, target: Path) {
    if (!source.isDirectory()) {
        target.parent?.createDirectories()
        Files.copy(source, target, COPY_ATTRIBUTES)
        return
    }

    Files.walk(source).use { paths ->
        paths.forEach { current ->
            val relative = source.relativize(current)
            val destination = target.resolve(relative)
            if (Files.isDirectory(current)) {
                destination.createDirectories()
            } else {
                destination.parent?.createDirectories()
                Files.copy(current, destination, COPY_ATTRIBUTES)
            }
        }
    }
}

private fun writeMigrationMarker(target: Path) {
    val marker = target.resolve(MIGRATION_MARKER)
    val temporary = target.resolve("$MIGRATION_MARKER.tmp-${System.nanoTime()}")
    Files.writeString(temporary, "legacy state copied; source retained\n")
    try {
        try {
            Files.move(temporary, marker, REPLACE_EXISTING, ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, marker, REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temporary)
    }
}

private fun deleteRecursivelyBestEffort(path: Path) {
    if (!path.exists()) return
    runCatching {
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}

private fun String?.nonBlankPath(): Path? =
    this?.trim()?.takeIf(String::isNotBlank)?.let(Path::of)

private fun String?.nonBlankAbsolutePath(): Path? = nonBlankPath()?.takeIf(Path::isAbsolute)

object DesktopAppPaths {
    private val layout: DesktopAppPathLayout by lazy {
        resolveDesktopAppPathLayout(
            DesktopPathEnvironment(
                osName = System.getProperty("os.name").orEmpty(),
                userHome = System.getProperty("user.home"),
                xdgStateHome = System.getenv("XDG_STATE_HOME"),
                xdgCacheHome = System.getenv("XDG_CACHE_HOME"),
                xdgDataHome = System.getenv("XDG_DATA_HOME"),
                localAppData = System.getenv("LOCALAPPDATA"),
                stateDirectoryProperty = System.getProperty(STATE_DIR_PROPERTY),
                stateDirectoryEnvironment = System.getenv(STATE_DIR_ENVIRONMENT),
                auxiliaryDirectoryProperty = System.getProperty(AUXILIARY_DIR_PROPERTY),
                auxiliaryDirectoryEnvironment = System.getenv(AUXILIARY_DIR_ENVIRONMENT),
                temporaryDirectory = System.getProperty("java.io.tmpdir"),
            )
        )
    }

    private val activeStateDirectory: Path by lazy { resolveActiveStateDirectory(layout) }

    fun stateDirectory(): Path = activeStateDirectory

    fun cacheDirectory(): Path = layout.cacheDirectory

    fun dataDirectory(): Path = layout.dataDirectory

    fun databaseFile(): Path = stateDirectory().resolve("seal.db")

    fun preferencesFile(): Path = stateDirectory().resolve("settings.json")

    fun appSettingsFile(): Path = stateDirectory().resolve("app-settings.json")

    fun themeSettingsFile(): Path = stateDirectory().resolve("theme.json")

    fun queueFile(): Path = stateDirectory().resolve("queue.json")

    fun historyFile(): Path = stateDirectory().resolve("history.json")

    fun customCommandTasksFile(): Path = stateDirectory().resolve("custom-command-tasks.json")

    fun ytDlpStateDirectory(): Path = stateDirectory().resolve("yt-dlp")

    fun cookiesFile(): Path = ytDlpStateDirectory().resolve("cookies.txt")

    fun downloadArchiveFile(): Path = ytDlpStateDirectory().resolve("download-archive.txt")

    fun auxiliaryBinariesDirectory(): Path = layout.auxiliaryBinariesDirectory

    fun temporaryDirectory(): Path = layout.temporaryDirectory
}
