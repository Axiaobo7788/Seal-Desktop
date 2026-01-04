package com.junkfood.seal.desktop.ytdlp

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Desktop-side path conventions for yt-dlp assets and downloads.
 */
object DesktopYtDlpPaths {
    private val stateRoot: Path by lazy { resolveStateRoot() }
    private val downloadsRoot: Path by lazy { resolveDownloadsRoot() }

    fun cookiesFile(): Path {
        val dir = stateRoot
        dir.createDirectories()
        return dir.resolve("cookies.txt")
    }

    fun archiveFile(): Path {
        val dir = stateRoot
        dir.createDirectories()
        return dir.resolve("download-archive.txt")
    }

    fun downloadDirectory(hint: String?): Path {
        val sub = when (hint) {
            "audio" -> "Audio"
            "video" -> "Video"
            else -> null
        }
        val dir = sub?.let { downloadsRoot.resolve(it) } ?: downloadsRoot
        if (!dir.exists()) Files.createDirectories(dir)
        return dir
    }

    private fun resolveStateRoot(): Path {
        val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
        if (xdg != null) return xdg.resolve("seal/yt-dlp")
        val home = System.getProperty("user.home")
        return Path.of(home, ".local", "state", "seal", "yt-dlp")
    }

    private fun resolveDownloadsRoot(): Path {
        System.getenv("XDG_DOWNLOAD_DIR")?.takeIf { it.isNotBlank() }?.let { return Path.of(it) }
        val home = System.getProperty("user.home")
        val downloads = Path.of(home, "Downloads")
        if (!downloads.exists()) Files.createDirectories(downloads)
        return downloads
    }
}
