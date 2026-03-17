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
        val xdg = parseXdgDownloadDir()
        if (xdg != null) {
            runCatching {
                if (!xdg.exists()) Files.createDirectories(xdg)
                return xdg
            }
        }

        val downloads = defaultDownloadsDirectory()
        if (!downloads.exists()) Files.createDirectories(downloads)
        return downloads
    }

    private fun parseXdgDownloadDir(): Path? {
        val raw = System.getenv("XDG_DOWNLOAD_DIR")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val stripped = raw.removeSurrounding("\"")
        val home = System.getProperty("user.home")
        val expanded =
            when {
                stripped == "~" -> home
                stripped.startsWith("~/") -> home + stripped.removePrefix("~")
                stripped.startsWith("\$HOME/") -> home + stripped.removePrefix("\$HOME")
                stripped.startsWith("\${HOME}/") -> home + stripped.removePrefix("\${HOME}")
                else -> stripped
            }

        return runCatching { Path.of(expanded) }
            .getOrNull()
            ?.takeIf { it.isAbsolute }
    }

    private fun defaultDownloadsDirectory(): Path {
        val osName = System.getProperty("os.name")?.lowercase().orEmpty()
        val userHome = System.getProperty("user.home")

        if (osName.contains("win")) {
            val base = System.getenv("USERPROFILE")?.takeIf { it.isNotBlank() } ?: userHome
            return Path.of(base, "Downloads")
        }

        val base = System.getenv("HOME")?.takeIf { it.isNotBlank() } ?: userHome.ifBlank {
            Path.of("/home", System.getProperty("user.name")).toString()
        }
        return Path.of(base, "Downloads")
    }
}
