package com.junkfood.seal.desktop.ytdlp

import java.io.BufferedInputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.setPosixFilePermissions

/**
 * Fetch and cache yt-dlp binary per-platform. Keeps a pinned version for reproducibility.
 */
class YtDlpFetcher(
    /**
     * version: release tag (e.g. "2024.11.04") or "latest" to always use the newest asset.
     */
    private val version: String = DEFAULT_VERSION,
    private val cacheRoot: Path = defaultCacheRoot(),
) {
    private val platform: Platform = detectPlatform()

    fun ensureBinary(): Path {
        findBundledBinary()?.let { return it }
        val target = cacheRoot.resolve(version).resolve(platform.binaryName)
        if (!target.exists()) {
            target.parent.createDirectories()
            val primaryUrl = platform.primaryDownloadUrl(version)
            val fallbackUrl = platform.fallbackDownloadUrl(version)
            runCatching { downloadBinary(target, primaryUrl) }
                .recoverCatching { downloadBinary(target, fallbackUrl) }
                .getOrElse { throw it }
            markExecutable(target)
        } else if (!target.isExecutable()) {
            // Re-assert executable bit if cache was carried over from previous runs.
            markExecutable(target)
        }
        return target
    }

    private fun downloadBinary(target: Path, url: String) {
        try {
            URL(url).openStream().use { input ->
                BufferedInputStream(input).use { buffered ->
                    Files.copy(buffered, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } catch (ioe: IOException) {
            throw IOException("Failed to download yt-dlp from $url", ioe)
        }
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
            // Fallback best-effort: ignore if filesystem doesn't support POSIX perms
        }
        if (!target.isExecutable()) {
            target.toFile().setExecutable(true, /*ownerOnly=*/false)
        }
    }

    private fun findBundledBinary(): Path? {
        val candidates = mutableListOf<Path>()

        // 1) Current working directory (best-effort; not always the app dir on Windows).
        runCatching {
            candidates.add(Path.of(System.getProperty("user.dir")).resolve(platform.binaryName))
        }

        // 2) Directory of the running code source (works well for packaged distributions).
        runCatching {
            val location = YtDlpFetcher::class.java.protectionDomain.codeSource.location
            val codePath = Path.of(location.toURI())
            val baseDir = if (Files.isDirectory(codePath)) codePath else codePath.parent
            if (baseDir != null) {
                candidates.add(baseDir.resolve(platform.binaryName))
                // common layout: app/<something>.jar, binary placed next to exe one level above
                baseDir.parent?.resolve(platform.binaryName)?.let(candidates::add)
            }
        }

        // 3) Fallback: inspect classpath entries and try their parents.
        runCatching {
            val classPath = System.getProperty("java.class.path") ?: return@runCatching
            classPath.split(System.getProperty("path.separator") ?: ";")
                .asSequence()
                .mapNotNull { runCatching { Path.of(it) }.getOrNull() }
                .mapNotNull { p -> if (Files.isDirectory(p)) p else p.parent }
                .take(5)
                .forEach { dir ->
                    candidates.add(dir.resolve(platform.binaryName))
                    dir.parent?.resolve(platform.binaryName)?.let(candidates::add)
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

    companion object {
        private const val DEFAULT_VERSION = "latest"

        private fun defaultCacheRoot(): Path {
            val base = System.getenv("XDG_CACHE_HOME")?.let { Path.of(it) }
            if (base != null) return base.resolve("seal/yt-dlp")
            val userHome = System.getProperty("user.home")
            return Path.of(userHome, ".cache", "seal", "yt-dlp")
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

    fun primaryDownloadUrl(version: String): String =
        when (this) {
            WindowsX64 -> assetUrl(version, "yt-dlp.exe")
            WindowsX86 -> assetUrl(version, "yt-dlp_x86.exe")
            WindowsArm64 -> assetUrl(version, "yt-dlp_arm64.exe")
            MacUniversal -> assetUrl(version, "yt-dlp_macos")
            LinuxArm64 -> assetUrl(version, "yt-dlp_linux_aarch64")
            LinuxX64 -> assetUrl(version, "yt-dlp_linux")
        }

    fun fallbackDownloadUrl(version: String): String =
        when (this) {
            MacUniversal -> assetUrl(version, "yt-dlp_macos")
            WindowsX64, WindowsX86, WindowsArm64 -> assetUrl(version, "yt-dlp.exe")
            LinuxX64 -> assetUrl(version, "yt-dlp_linux")
            LinuxArm64 -> assetUrl(version, "yt-dlp_linux_aarch64")
        }
}

private fun assetUrl(version: String, asset: String): String =
    if (version == "latest") {
        "https://github.com/yt-dlp/yt-dlp/releases/latest/download/$asset"
    } else {
        "https://github.com/yt-dlp/yt-dlp/releases/download/$version/$asset"
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
