package com.junkfood.seal.desktop.storage

import com.junkfood.seal.desktop.download.DesktopDownloadQueueStorage
import com.junkfood.seal.desktop.download.DesktopDownloadRequestBackup
import com.junkfood.seal.desktop.download.DesktopQueueBackup
import com.junkfood.seal.desktop.download.DesktopQueueItemBackup
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryEntry
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryStorage
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DesktopAppSettingsStorage
import com.junkfood.seal.util.DownloadPreferences
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking

private fun check(condition: Boolean, message: String) {
    if (!condition) {
        throw IllegalStateException("[DesktopStorageSelfCheck] $message")
    }
}

private fun sampleQueueBackup(): DesktopQueueBackup =
    DesktopQueueBackup(
        version = 1,
        items =
            listOf(
                DesktopQueueItemBackup(
                    id = "selfcheck-1",
                    title = "Self Check Item",
                    author = "self-check",
                    url = "https://example.com/video",
                    thumbnailUrl = null,
                    mediaType = "Video",
                    status = "Canceled",
                    request =
                        DesktopDownloadRequestBackup(
                            url = "https://example.com/video",
                            type = "Video",
                            preferences = DownloadPreferences.EMPTY,
                        ),
                )
            ),
    )

private fun sampleHistory(): List<DesktopDownloadHistoryEntry> =
    listOf(
        DesktopDownloadHistoryEntry(
            id = "selfcheck-history-1",
            title = "Self Check History",
            url = "https://example.com/history",
        )
    )

private fun sampleSettings(): DesktopAppSettings =
    DesktopAppSettings(
        customCommandEnabled = true,
        customCommandLabel = "self-check",
        customCommandTemplate = "echo self-check",
    )

fun main() = runBlocking {
    val stateDir = desktopAppStateDir()
    stateDir.createDirectories()

    val queueStorage = DesktopDownloadQueueStorage()
    val historyStorage = DesktopDownloadHistoryStorage()
    val settingsStorage = DesktopAppSettingsStorage()

    val queueBackup = sampleQueueBackup()
    val historyEntries = sampleHistory()
    val settings = sampleSettings()

    queueStorage.save(queueBackup)
    historyStorage.save(historyEntries)
    settingsStorage.save(settings)

    val loadedQueue = queueStorage.load()
    val loadedHistory = historyStorage.load()
    val loadedSettings = settingsStorage.load()

    check(loadedQueue.items.size == queueBackup.items.size, "Queue size mismatch after save/load")
    check(loadedHistory.isNotEmpty(), "History should not be empty after save/load")
    check(loadedSettings?.customCommandLabel == settings.customCommandLabel, "Settings mismatch after save/load")

    if (DesktopStorageConfig.backend == DesktopStorageBackend.DualWrite) {
        val queuePath = queueJsonPath()
        val historyPath = historyJsonPath()
        val settingsPath = appSettingsJsonPath()

        // Corrupt JSON files to verify dual mode fallback can recover from SQLite.
        queuePath.writeText("{\"broken\":")
        historyPath.writeText("{\"broken\":")
        settingsPath.writeText("{\"broken\":")

        val queueAfterCorruption = queueStorage.load()
        val historyAfterCorruption = historyStorage.load()
        val settingsAfterCorruption = settingsStorage.load()

        check(
            queueAfterCorruption.items.any { it.id == "selfcheck-1" },
            "Dual mode queue fallback to SQLite failed",
        )
        check(
            historyAfterCorruption.any { it.id == "selfcheck-history-1" },
            "Dual mode history fallback to SQLite failed",
        )
        check(
            settingsAfterCorruption?.customCommandLabel == "self-check",
            "Dual mode settings fallback to SQLite failed",
        )

        val quarantinedQueue = queuePath.parent?.toFile()?.listFiles()?.any { it.name.contains("queue.json.corrupt-") } == true
        val quarantinedHistory = historyPath.parent?.toFile()?.listFiles()?.any { it.name.contains("history.json.corrupt-") } == true
        val quarantinedSettings = settingsPath.parent?.toFile()?.listFiles()?.any { it.name.contains("app-settings.json.corrupt-") } == true
        check(quarantinedQueue, "Queue corrupted file was not quarantined")
        check(quarantinedHistory, "History corrupted file was not quarantined")
        check(quarantinedSettings, "Settings corrupted file was not quarantined")
    }

    if (DesktopStorageConfig.backend == DesktopStorageBackend.Sqlite) {
        val sqliteFileExists = sqliteDbPath().toFile().exists()
        check(sqliteFileExists, "SQLite backend should create seal.db")
    }

    val eventLogHint = "[DesktopStorageSelfCheck] backend=${DesktopStorageConfig.backend.name} stateDir=${stateDir.toAbsolutePath()}"
    println(eventLogHint)

    // Print a short summary from JSON files for troubleshooting when using json backend.
    if (DesktopStorageConfig.backend == DesktopStorageBackend.Json) {
        println("[DesktopStorageSelfCheck] queue.json bytes=${queueJsonPath().readText().length}")
        println("[DesktopStorageSelfCheck] history.json bytes=${historyJsonPath().readText().length}")
        println("[DesktopStorageSelfCheck] app-settings.json bytes=${appSettingsJsonPath().readText().length}")
    }
}
