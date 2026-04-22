package com.junkfood.seal.desktop.download.history

import com.junkfood.seal.desktop.storage.DesktopSqliteStorage
import com.junkfood.seal.desktop.storage.DesktopStorageBackend
import com.junkfood.seal.desktop.storage.DesktopStorageConfig
import com.junkfood.seal.desktop.storage.DesktopStorageEventLogger
import com.junkfood.seal.desktop.storage.historyJsonPath
import com.junkfood.seal.desktop.storage.quarantineCorruptedFile
import com.junkfood.seal.desktop.storage.writeTextAtomically
import java.nio.file.Path
import java.nio.file.Files
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
    val downloadHistory: List<AndroidDownloadedVideoInfo>? = emptyList(),
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

private fun DesktopDownloadHistoryEntry.toAndroidEntry(): AndroidDownloadedVideoInfo {
    val maybeId = id.removePrefix("android-").toIntOrNull() ?: 0
    return AndroidDownloadedVideoInfo(
        id = maybeId,
        videoTitle = title,
        videoAuthor = author,
        videoUrl = url,
        thumbnailUrl = thumbnailUrl ?: "",
        videoPath = filePath ?: "",
        extractor = extractor
    )
}

internal fun encodeHistoryEntries(entries: List<DesktopDownloadHistoryEntry>): String {
    val backup = AndroidBackup(downloadHistory = entries.map { it.toAndroidEntry() })
    return historyJson.encodeToString(backup)
}

@Throws(SerializationException::class)
internal fun decodeHistoryEntries(text: String): List<DesktopDownloadHistoryEntry> {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return emptyList()

    return runCatching { historyJson.decodeFromString<List<DesktopDownloadHistoryEntry>>(trimmed) }
        .getOrElse { desktopErr ->
            // Compatibility: Android exports download history as Backup(downloadHistory=[DownloadedVideoInfo...]).
            runCatching { historyJson.decodeFromString<AndroidBackup>(trimmed) }
                .map { backup ->
                    val historyList = backup.downloadHistory ?: emptyList()
                    if (historyList.isEmpty()) throw desktopErr
                    val now = System.currentTimeMillis()
                    historyList.map { it.toDesktopEntry(nowEpochMillis = now) }
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
    return historyJsonPath()
}

class DesktopDownloadHistoryStorage(private val path: Path = defaultHistoryPath()) {
    private fun loadFromJsonOrNull(): List<DesktopDownloadHistoryEntry>? {
        if (!path.exists()) return null

        return runCatching { historyJson.decodeFromString<List<DesktopDownloadHistoryEntry>>(path.readText()) }
            .getOrElse {
                val quarantined = quarantineCorruptedFile(path)
                if (quarantined != null) {
                    DesktopStorageEventLogger.warn(
                        component = "DesktopDownloadHistoryStorage",
                        event = "json_history_quarantined",
                        message = "Corrupted history JSON file quarantined",
                        details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                    )
                }
                logHistoryStorageWarning("Failed to parse history JSON, fallback to empty history", it)
                null
            }
    }

    private fun loadFromJsonWithDefault(): List<DesktopDownloadHistoryEntry> =
        loadFromJsonOrNull() ?: emptyList()

    private fun saveToJson(entries: List<DesktopDownloadHistoryEntry>) {
        writeTextAtomically(path, historyJson.encodeToString(entries))
    }

    suspend fun load(): List<DesktopDownloadHistoryEntry> =
        withContext(Dispatchers.IO) {
            when (DesktopStorageConfig.backend) {
                DesktopStorageBackend.Json -> loadFromJsonWithDefault()
                DesktopStorageBackend.DualWrite -> {
                    val entriesFromJson = loadFromJsonOrNull()
                    if (entriesFromJson != null) {
                        runCatching { DesktopSqliteStorage.writeHistory(entriesFromJson) }
                            .onFailure {
                                logHistoryStorageWarning("Failed to mirror history to SQLite on load", it)
                            }
                        entriesFromJson
                    } else {
                        val entriesFromSqlite = DesktopSqliteStorage.readHistory()
                        if (entriesFromSqlite != null) {
                            DesktopStorageEventLogger.info(
                                component = "DesktopDownloadHistoryStorage",
                                event = "dual_mode_fallback_to_sqlite",
                                message = "History loaded from SQLite because JSON was unavailable",
                                details = mapOf("backend" to DesktopStorageConfig.backend.name),
                            )
                        }
                        entriesFromSqlite ?: emptyList()
                    }
                }
                DesktopStorageBackend.Sqlite -> {
                    DesktopSqliteStorage.readHistory() ?: loadFromJsonWithDefault()
                }
            }
        }

    suspend fun save(entries: List<DesktopDownloadHistoryEntry>) {
        withContext(Dispatchers.IO) {
            when (DesktopStorageConfig.backend) {
                DesktopStorageBackend.Json -> saveToJson(entries)
                DesktopStorageBackend.DualWrite -> {
                    saveToJson(entries)
                    runCatching { DesktopSqliteStorage.writeHistory(entries) }
                        .onFailure {
                            logHistoryStorageWarning("Failed to mirror history to SQLite on save", it)
                        }
                }
                DesktopStorageBackend.Sqlite -> {
                    runCatching { DesktopSqliteStorage.writeHistory(entries) }
                        .onFailure {
                            logHistoryStorageWarning(
                                "Failed to save history to SQLite, falling back to JSON",
                                it,
                            )
                            saveToJson(entries)
                        }
                }
            }
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

private fun logHistoryStorageWarning(message: String, throwable: Throwable) {
    DesktopStorageEventLogger.warn(
        component = "DesktopDownloadHistoryStorage",
        event = "history_storage_warning",
        message = message,
        details = mapOf("backend" to DesktopStorageConfig.backend.name),
        throwable = throwable,
    )
}
