package com.junkfood.seal.desktop.ytdlp

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.setPosixFilePermissions
import com.junkfood.seal.desktop.storage.DesktopSqliteStorage
import com.junkfood.seal.desktop.settings.EnvPrefAuto
import com.junkfood.seal.desktop.settings.EnvPrefBundled
import com.junkfood.seal.desktop.settings.EnvPrefSystem

class EnvironmentMissingException(message: String) : Exception(message)

/**
 * Locates yt-dlp binary either from the bundled resources or from the system PATH.
 * Retains the name YtDlpFetcher for compatibility with existing codebase.
 */
class YtDlpFetcher(
    // Retained for API compatibility but unused in the new logic
    private val version: String = "latest",
    private val cacheRoot: Path? = null,
) {
    private val platform: Platform = detectPlatform()

    /**
     * Required for API compatibility with YtdlpUpdateCard.
     * With the new architecture, we no longer maintain a standalone cache,
     * so this always returns a dummy path or the system path if needed.
     */
    fun cachedBinaryPath(): Path = Path.of("")

    /**
     * Best-effort: returns an existing binary without throwing exceptions.
     */
    fun findExistingBinary(): Path? {
        val pref = DesktopSqliteStorage.readAppSettings()?.environmentPreference ?: EnvPrefAuto
        return when (pref) {
            EnvPrefBundled -> findBundledBinary()
            EnvPrefSystem -> findSystemBinary()
            else -> findBundledBinary() ?: findSystemBinary() ?: findAuxiliaryBinary()
        }
    }

    /**
     * Required for API compatibility. Now a no-op because we don't manage caching anymore.
     */
    fun invalidateCachedBinary(): Boolean {
        return false
    }

    /**
     * Locates the binary or throws EnvironmentMissingException if not found.
     */
    fun ensureBinary(): Path {
        return findExistingBinary() ?: throw EnvironmentMissingException("yt-dlp is not bundled and not found in system or auxiliary paths.")
    }

    private fun findBundledBinary(): Path? {
        val candidates = mutableListOf<Path>()

        // 1) Current working directory
        runCatching {
            candidates.add(Path.of(System.getProperty("user.dir")).resolve(platform.binaryName))
        }

        // 1a) Common distributable layout
        runCatching {
            candidates.add(Path.of(System.getProperty("user.dir")).resolve("bin").resolve(platform.binaryName))
        }

        // 1b) Compose Desktop appResources
        runCatching {
            val resourcesDir = System.getProperty("compose.application.resources.dir")
            if (resourcesDir != null) {
                candidates.add(Path.of(resourcesDir).resolve(platform.binaryName))
            }
        }

        // 2) Directory of the running code source
        runCatching {
            val location = YtDlpFetcher::class.java.protectionDomain.codeSource.location
            val codePath = Path.of(location.toURI())
            val baseDir = if (Files.isDirectory(codePath)) codePath else codePath.parent
            if (baseDir != null) {
                candidates.add(baseDir.resolve(platform.binaryName))
                baseDir.parent?.resolve(platform.binaryName)?.let(candidates::add)
                candidates.add(baseDir.resolve("bin").resolve(platform.binaryName))
                baseDir.parent?.resolve("bin")?.resolve(platform.binaryName)?.let(candidates::add)
            }
        }

        val found = candidates.firstOrNull { it.exists() }
        if (found != null) {
            if (!found.isExecutable()) {
                markExecutable(found)
            }
            return found
        }
        return null
    }

    private fun findSystemBinary(): Path? {
        val binaryName = if (System.getProperty("os.name").lowercase().contains("win")) "yt-dlp.exe" else "yt-dlp"
        val pathEnv = System.getenv("PATH") ?: return null
        val separator = File.pathSeparator
        
        val paths = pathEnv.split(separator)
        for (dir in paths) {
            val file = Path.of(dir).resolve(binaryName)
            if (file.exists() && file.isExecutable()) {
                return file
            }
        }
        return null
    }

    private fun findAuxiliaryBinary(): Path? {
        val osName = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val binaryName = if (osName.contains("win")) "yt-dlp.exe" else "yt-dlp"
        
        val dir = when {
            osName.contains("win") -> Path.of(System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local", "Seal", "bin")
            osName.contains("mac") || osName.contains("darwin") -> Path.of(userHome, "Library", "Application Support", "Seal", "bin")
            else -> Path.of(userHome, ".local", "bin")
        }
        val file = dir.resolve(binaryName)
        if (file.exists()) {
            if (!file.isExecutable()) {
                markExecutable(file)
            }
            return file
        }
        return null
    }

    private fun markExecutable(target: Path) {
        when (platform) {
            Platform.WindowsX64, Platform.WindowsX86, Platform.WindowsArm64 -> return
            else -> Unit
        }
        try {
            target.setPosixFilePermissions(setOf(
                java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                java.nio.file.attribute.PosixFilePermission.OTHERS_READ,
            ))
        } catch (_: UnsupportedOperationException) {
        }
        if (!target.isExecutable()) {
            target.toFile().setExecutable(true, /*ownerOnly=*/false)
        }
    }
}

private enum class Platform(val binaryName: String) {
    WindowsX64("yt-dlp.exe"),
    WindowsX86("yt-dlp_x86.exe"),
    WindowsArm64("yt-dlp_arm64.exe"),
    MacUniversal("yt-dlp_macos"),
    LinuxArm64("yt-dlp_linux_aarch64"),
    LinuxX64("yt-dlp_linux");
}

private fun detectPlatform(): Platform {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    val isArm = arch.contains("aarch64") || arch.contains("arm64")
    val isX86 = arch.contains("x86") || arch.contains("i386") || arch.contains("i686")
    val isMac = os.contains("mac") || os.contains("darwin")
    val isWin = os.contains("win")

    return when {
        isWin && isArm -> Platform.WindowsArm64
        isWin && isX86 -> Platform.WindowsX86
        isWin -> Platform.WindowsX64
        isMac -> Platform.MacUniversal
        !isMac && isArm -> Platform.LinuxArm64
        else -> Platform.LinuxX64
    }
}
