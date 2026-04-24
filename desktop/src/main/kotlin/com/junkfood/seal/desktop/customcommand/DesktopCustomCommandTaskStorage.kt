package com.junkfood.seal.desktop.customcommand

import com.junkfood.seal.desktop.storage.DesktopStorageEventLogger
import com.junkfood.seal.desktop.storage.customCommandTasksJsonPath
import com.junkfood.seal.desktop.storage.quarantineCorruptedFile
import com.junkfood.seal.desktop.storage.writeTextAtomically
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
data class DesktopCustomCommandTasksBackup(
    val version: Int = 1,
    val tasks: List<DesktopCustomCommandTask> = emptyList()
)

object DesktopCustomCommandTaskStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun path(): Path = customCommandTasksJsonPath()

    suspend fun load(): List<DesktopCustomCommandTask> = withContext(Dispatchers.IO) {
        val file = path()
        if (!file.exists()) return@withContext emptyList()

        runCatching {
            val backup = json.decodeFromString<DesktopCustomCommandTasksBackup>(file.readText())
            // 恢复时将 Running 统一回写为 Canceled
            backup.tasks.map { task ->
                if (task.status == DesktopCustomCommandTaskStatus.Running) {
                    task.copy(status = DesktopCustomCommandTaskStatus.Canceled)
                } else {
                    task
                }
            }
        }.getOrElse {
            val quarantined = quarantineCorruptedFile(file)
            if (quarantined != null) {
                DesktopStorageEventLogger.warn(
                    component = "DesktopCustomCommandTaskStorage",
                    event = "json_tasks_quarantined",
                    message = "Corrupted custom command tasks JSON file quarantined",
                    details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                )
            }
            emptyList()
        }
    }

    suspend fun save(tasks: List<DesktopCustomCommandTask>) {
        withContext(Dispatchers.IO) {
            val backup = DesktopCustomCommandTasksBackup(tasks = tasks)
            writeTextAtomically(path(), json.encodeToString(backup))
        }
    }
}