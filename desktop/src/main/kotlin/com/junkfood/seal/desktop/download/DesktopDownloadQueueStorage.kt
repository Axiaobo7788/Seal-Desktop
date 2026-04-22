package com.junkfood.seal.desktop.download

import com.junkfood.seal.desktop.storage.DesktopSqliteStorage
import com.junkfood.seal.desktop.storage.DesktopStorageBackend
import com.junkfood.seal.desktop.storage.DesktopStorageConfig
import com.junkfood.seal.desktop.storage.DesktopStorageEventLogger
import com.junkfood.seal.desktop.storage.quarantineCorruptedFile
import com.junkfood.seal.desktop.storage.queueJsonPath
import com.junkfood.seal.desktop.storage.writeTextAtomically
import com.junkfood.seal.util.DownloadPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

@Serializable
data class DesktopDownloadRequestBackup(
    val url: String,
    val type: String,
    val preferences: DownloadPreferences,
)

@Serializable
data class DesktopQueueItemBackup(
    val id: String,
    val title: String,
    val author: String,
    val url: String,
    val thumbnailUrl: String?,
    val mediaType: String,
    val status: String,
    val request: DesktopDownloadRequestBackup,
)

@Serializable
data class DesktopQueueBackup(
    val version: Int = 1,
    val items: List<DesktopQueueItemBackup> = emptyList()
)

private val queueJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

private fun defaultQueuePath(): Path {
    return queueJsonPath()
}

class DesktopDownloadQueueStorage(private val path: Path = defaultQueuePath()) {

    private fun loadFromJsonOrNull(): DesktopQueueBackup? {
        if (!path.exists()) return null

        return runCatching {
            queueJson.decodeFromString<DesktopQueueBackup>(path.readText())
        }.getOrElse {
            val quarantined = quarantineCorruptedFile(path)
            if (quarantined != null) {
                DesktopStorageEventLogger.warn(
                    component = "DesktopDownloadQueueStorage",
                    event = "json_queue_quarantined",
                    message = "Corrupted queue JSON file quarantined",
                    details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                )
            }
            logStorageWarning("Failed to parse queue JSON, fallback to empty queue", it)
            null
        }
    }

    private fun loadFromJsonWithDefault(): DesktopQueueBackup = loadFromJsonOrNull() ?: DesktopQueueBackup()

    private fun saveToJson(backup: DesktopQueueBackup) {
        writeTextAtomically(path, queueJson.encodeToString(backup))
    }

    suspend fun load(): DesktopQueueBackup =
        withContext(Dispatchers.IO) {
            when (DesktopStorageConfig.backend) {
                DesktopStorageBackend.Json -> loadFromJsonWithDefault()
                DesktopStorageBackend.DualWrite -> {
                    val backupFromJson = loadFromJsonOrNull()
                    if (backupFromJson != null) {
                        runCatching { DesktopSqliteStorage.writeQueue(backupFromJson) }
                            .onFailure {
                                logStorageWarning("Failed to mirror queue state to SQLite on load", it)
                            }
                        backupFromJson
                    } else {
                        val backupFromSqlite = DesktopSqliteStorage.readQueue()
                        if (backupFromSqlite != null) {
                            DesktopStorageEventLogger.info(
                                component = "DesktopDownloadQueueStorage",
                                event = "dual_mode_fallback_to_sqlite",
                                message = "Queue loaded from SQLite because JSON was unavailable",
                                details = mapOf("backend" to DesktopStorageConfig.backend.name),
                            )
                        }
                        backupFromSqlite ?: DesktopQueueBackup()
                    }
                }
                DesktopStorageBackend.Sqlite -> {
                    DesktopSqliteStorage.readQueue() ?: loadFromJsonWithDefault()
                }
            }
        }

    suspend fun save(backup: DesktopQueueBackup) {
        withContext(Dispatchers.IO) {
            when (DesktopStorageConfig.backend) {
                DesktopStorageBackend.Json -> saveToJson(backup)
                DesktopStorageBackend.DualWrite -> {
                    saveToJson(backup)
                    runCatching { DesktopSqliteStorage.writeQueue(backup) }
                        .onFailure {
                            logStorageWarning("Failed to mirror queue state to SQLite on save", it)
                        }
                }
                DesktopStorageBackend.Sqlite -> {
                    runCatching { DesktopSqliteStorage.writeQueue(backup) }
                        .onFailure {
                            logStorageWarning(
                                "Failed to save queue state to SQLite, falling back to JSON",
                                it,
                            )
                            saveToJson(backup)
                        }
                }
            }
        }
    }
}

private fun logStorageWarning(message: String, throwable: Throwable) {
    DesktopStorageEventLogger.warn(
        component = "DesktopDownloadQueueStorage",
        event = "queue_storage_warning",
        message = message,
        details = mapOf("backend" to DesktopStorageConfig.backend.name),
        throwable = throwable,
    )
}

