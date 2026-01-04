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
        if (platform == Platform.Windows) return
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
    Windows("yt-dlp.exe"),
    MacArm64("yt-dlp_macos_aarch64"),
    MacX64("yt-dlp_macos"),
    LinuxArm64("yt-dlp_linux_aarch64"),
    LinuxX64("yt-dlp_linux"),
    LinuxFallback("yt-dlp"),
    MacFallback("yt-dlp_macos"),
    WindowsFallback("yt-dlp.exe");

    fun primaryDownloadUrl(version: String): String =
        when (this) {
            Windows -> assetUrl(version, "yt-dlp.exe")
            MacArm64 -> assetUrl(version, "yt-dlp_macos_aarch64")
            MacX64 -> assetUrl(version, "yt-dlp_macos")
            LinuxArm64 -> assetUrl(version, "yt-dlp_linux_aarch64")
            LinuxX64 -> assetUrl(version, "yt-dlp_linux")
            LinuxFallback -> assetUrl(version, "yt-dlp")
            MacFallback -> assetUrl(version, "yt-dlp_macos")
            WindowsFallback -> assetUrl(version, "yt-dlp.exe")
        }

    fun fallbackDownloadUrl(version: String): String =
        when (this) {
            LinuxX64, LinuxArm64 -> assetUrl(version, "yt-dlp")
            MacX64, MacArm64 -> assetUrl(version, "yt-dlp_macos")
            Windows -> assetUrl(version, "yt-dlp.exe")
            else -> primaryDownloadUrl(version)
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
    val isMac = os.contains("mac") || os.contains("darwin")
    val isWin = os.contains("win")

    return when {
        isWin -> Platform.Windows
        isMac && isArm -> Platform.MacArm64
        isMac -> Platform.MacX64
        !isMac && isArm -> Platform.LinuxArm64
        else -> Platform.LinuxX64
    }
}
