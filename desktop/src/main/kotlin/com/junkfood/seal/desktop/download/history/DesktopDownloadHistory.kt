package com.junkfood.seal.desktop.download.history

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val historyJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

@Serializable
enum class DesktopHistoryMediaType { Audio, Video }

@Serializable
enum class DesktopHistoryPlatform { YouTube, BiliBili, Other }

@Serializable
data class DesktopDownloadHistoryEntry(
    val id: String,
    val title: String,
    val author: String = "",
    val url: String = "",
    val mediaType: DesktopHistoryMediaType = DesktopHistoryMediaType.Video,
    val platform: DesktopHistoryPlatform = DesktopHistoryPlatform.Other,
    val thumbnailUrl: String? = null,
    val filePath: String? = null,
    val fileSizeBytes: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

private fun defaultHistoryPath(): Path {
    val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
    val base =
        if (xdg != null) Path.of(xdg) else Path.of(System.getProperty("user.home"), ".local", "state")
    return base.resolve("seal").resolve("history.json")
}

class DesktopDownloadHistoryStorage(private val path: Path = defaultHistoryPath()) {
    suspend fun load(): List<DesktopDownloadHistoryEntry> =
        withContext(Dispatchers.IO) {
            if (!path.exists()) return@withContext emptyList()
            runCatching { historyJson.decodeFromString<List<DesktopDownloadHistoryEntry>>(path.readText()) }
                .getOrNull()
                ?: emptyList()
        }

    suspend fun save(entries: List<DesktopDownloadHistoryEntry>) {
        withContext(Dispatchers.IO) {
            path.parent?.createDirectories()
            Files.writeString(path, historyJson.encodeToString(entries))
        }
    }
}
