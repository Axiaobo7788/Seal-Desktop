package com.junkfood.seal.desktop.download.history

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class AndroidBackup(
    val downloadHistory: List<AndroidDownloadedVideoInfo> = emptyList(),
)

@Serializable
private data class AndroidDownloadedVideoInfo(
    val id: Int = 0,
    val videoTitle: String = "",
    val videoAuthor: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val videoPath: String = "",
    val extractor: String = "Unknown",
)

private fun AndroidDownloadedVideoInfo.toDesktopEntry(nowEpochMillis: Long): DesktopDownloadHistoryEntry {
    val platform =
        when {
            extractor.contains("bili", ignoreCase = true) ||
                videoUrl.contains("bilibili", ignoreCase = true) -> DesktopHistoryPlatform.BiliBili
            extractor.contains("you", ignoreCase = true) ||
                videoUrl.contains("youtube", ignoreCase = true) ||
                videoUrl.contains("youtu", ignoreCase = true) -> DesktopHistoryPlatform.YouTube
            else -> DesktopHistoryPlatform.Other
        }

    val lowerPath = videoPath.lowercase()
    val mediaType =
        when {
            lowerPath.endsWith(".mp3") ||
                lowerPath.endsWith(".aac") ||
                lowerPath.endsWith(".opus") ||
                lowerPath.endsWith(".m4a") ||
                lowerPath.endsWith(".flac") ||
                lowerPath.endsWith(".wav") -> DesktopHistoryMediaType.Audio
            else -> DesktopHistoryMediaType.Video
        }

    return DesktopDownloadHistoryEntry(
        id = "android-$id",
        title = videoTitle.ifBlank { videoUrl },
        author = videoAuthor,
        url = videoUrl,
        mediaType = mediaType,
        platform = platform,
        extractor = extractor,
        thumbnailUrl = thumbnailUrl.takeIf { it.isNotBlank() },
        filePath = videoPath.takeIf { it.isNotBlank() },
        createdAtEpochMillis = nowEpochMillis,
    )
}

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
    val extractor: String = "",
    val thumbnailUrl: String? = null,
    val filePath: String? = null,
    val fileSizeBytes: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

enum class DesktopHistoryExportType { DownloadHistory, UrlList }

enum class DesktopHistoryImportMode { Merge, Replace }

internal fun encodeHistoryEntries(entries: List<DesktopDownloadHistoryEntry>): String =
    historyJson.encodeToString(entries)

@Throws(SerializationException::class)
internal fun decodeHistoryEntries(text: String): List<DesktopDownloadHistoryEntry> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return emptyList()

    return runCatching { historyJson.decodeFromString<List<DesktopDownloadHistoryEntry>>(trimmed) }
        .getOrElse { desktopErr ->
            // Compatibility: Android exports download history as Backup(downloadHistory=[DownloadedVideoInfo...]).
            runCatching { historyJson.decodeFromString<AndroidBackup>(trimmed) }
                .map { backup ->
                    if (backup.downloadHistory.isEmpty()) throw desktopErr
                    val now = System.currentTimeMillis()
                    backup.downloadHistory.map { it.toDesktopEntry(nowEpochMillis = now) }
                }
                .getOrElse {
                    val se = desktopErr as? SerializationException
                    throw (se ?: SerializationException(desktopErr.message ?: "Invalid JSON", desktopErr))
                }
        }
}

internal fun encodeHistoryUrls(entries: List<DesktopDownloadHistoryEntry>): String =
    entries
        .asSequence()
        .map { it.url.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n")

internal fun decodeHistoryUrls(text: String): List<String> =
    text
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()

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

    suspend fun exportTo(target: Path, entries: List<DesktopDownloadHistoryEntry>) {
        withContext(Dispatchers.IO) {
            target.parent?.createDirectories()
            Files.writeString(target, encodeHistoryEntries(entries))
        }
    }

    suspend fun importFrom(source: Path): List<DesktopDownloadHistoryEntry> =
        withContext(Dispatchers.IO) {
            if (!Files.exists(source)) return@withContext emptyList()
            val text = Files.readString(source)
            runCatching { decodeHistoryEntries(text) }.getOrDefault(emptyList())
        }
}
