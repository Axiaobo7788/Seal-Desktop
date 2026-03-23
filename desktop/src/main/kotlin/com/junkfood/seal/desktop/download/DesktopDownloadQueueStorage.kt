package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.DownloadPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
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
    val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
    val base =
        if (xdg != null) Path.of(xdg) else Path.of(System.getProperty("user.home"), ".local", "state")
    return base.resolve("seal").resolve("queue.json")
}

class DesktopDownloadQueueStorage(private val path: Path = defaultQueuePath()) {

    suspend fun load(): DesktopQueueBackup =
        withContext(Dispatchers.IO) {
            if (!path.exists()) return@withContext DesktopQueueBackup()
            runCatching {
                queueJson.decodeFromString<DesktopQueueBackup>(path.readText())
            }.getOrDefault(DesktopQueueBackup())
        }

    suspend fun save(backup: DesktopQueueBackup) {
        withContext(Dispatchers.IO) {
            path.parent?.createDirectories()
            Files.writeString(path, queueJson.encodeToString(backup))
        }
    }
}

