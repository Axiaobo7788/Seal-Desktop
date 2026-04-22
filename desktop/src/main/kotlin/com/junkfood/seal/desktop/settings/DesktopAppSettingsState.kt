package com.junkfood.seal.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.storage.DesktopSqliteStorage
import com.junkfood.seal.desktop.storage.DesktopStorageBackend
import com.junkfood.seal.desktop.storage.DesktopStorageConfig
import com.junkfood.seal.desktop.storage.DesktopStorageEventLogger
import com.junkfood.seal.desktop.storage.appSettingsJsonPath
import com.junkfood.seal.desktop.storage.quarantineCorruptedFile
import com.junkfood.seal.desktop.storage.writeTextAtomically
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val appSettingsJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

@Serializable
data class DesktopCommandTemplate(
    val id: Int,
    val label: String,
    val template: String,
)

@Serializable
data class DesktopAppSettings(
    val customCommandEnabled: Boolean = false,
    val customCommandLabel: String = "",
    val customCommandTemplate: String = "",
    val customCommandTemplateId: Int = 0,
    val customCommandTemplates: List<DesktopCommandTemplate> = emptyList(),
    val customCommandShortcuts: List<String> = emptyList(),
    val downloadTypeInitialization: Int = DownloadTypeNone,
    val downloadNotificationEnabled: Boolean = false,
    val disablePreview: Boolean = false,
    val autoUpdateEnabled: Boolean = false,
    val updateChannel: Int = UpdateChannelStable,
    val languageTag: String? = null,
    val autoProxyEnabled: Boolean = false,
    val configureBeforeDownload: Boolean = true,
    val isVideoClipEnabled: Boolean = false,
    val isFormatSelectionEnabled: Boolean = true,
    val ytDlpUpdateChannel: Int = 0,
    val ytDlpAutoUpdate: Boolean = true,
    val ytDlpUpdateInterval: Long = 604800000L,
)

private fun appSettingsPath(): Path {
    return appSettingsJsonPath()
}

class DesktopAppSettingsStorage(private val path: Path = appSettingsPath()) {
    private fun loadFromJsonOrNull(): DesktopAppSettings? {
        if (!path.exists()) return null

        return runCatching { appSettingsJson.decodeFromString<DesktopAppSettings>(path.readText()) }
            .getOrElse {
                val quarantined = quarantineCorruptedFile(path)
                if (quarantined != null) {
                    DesktopStorageEventLogger.warn(
                        component = "DesktopAppSettingsStorage",
                        event = "json_app_settings_quarantined",
                        message = "Corrupted app settings JSON file quarantined",
                        details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                    )
                }
                logAppSettingsStorageWarning("Failed to parse app settings JSON, fallback to defaults", it)
                null
            }
    }

    private fun saveToJson(settings: DesktopAppSettings) {
        writeTextAtomically(path, appSettingsJson.encodeToString(settings))
    }

    suspend fun load(): DesktopAppSettings? =
        withContext(Dispatchers.IO) {
            when (DesktopStorageConfig.backend) {
                DesktopStorageBackend.Json -> loadFromJsonOrNull()
                DesktopStorageBackend.DualWrite -> {
                    val settingsFromJson = loadFromJsonOrNull()
                    if (settingsFromJson != null) {
                        runCatching { DesktopSqliteStorage.writeAppSettings(settingsFromJson) }
                            .onFailure {
                                logAppSettingsStorageWarning(
                                    "Failed to mirror app settings to SQLite on load",
                                    it,
                                )
                            }
                        settingsFromJson
                    } else {
                        val settingsFromSqlite = DesktopSqliteStorage.readAppSettings()
                        if (settingsFromSqlite != null) {
                            DesktopStorageEventLogger.info(
                                component = "DesktopAppSettingsStorage",
                                event = "dual_mode_fallback_to_sqlite",
                                message = "App settings loaded from SQLite because JSON was unavailable",
                                details = mapOf("backend" to DesktopStorageConfig.backend.name),
                            )
                        }
                        settingsFromSqlite
                    }
                }
                DesktopStorageBackend.Sqlite -> {
                    DesktopSqliteStorage.readAppSettings() ?: loadFromJsonOrNull()
                }
            }
        }

    suspend fun save(settings: DesktopAppSettings) {
        withContext(Dispatchers.IO) {
            when (DesktopStorageConfig.backend) {
                DesktopStorageBackend.Json -> saveToJson(settings)
                DesktopStorageBackend.DualWrite -> {
                    saveToJson(settings)
                    runCatching { DesktopSqliteStorage.writeAppSettings(settings) }
                        .onFailure {
                            logAppSettingsStorageWarning(
                                "Failed to mirror app settings to SQLite on save",
                                it,
                            )
                        }
                }
                DesktopStorageBackend.Sqlite -> {
                    runCatching { DesktopSqliteStorage.writeAppSettings(settings) }
                        .onFailure {
                            logAppSettingsStorageWarning(
                                "Failed to save app settings to SQLite, falling back to JSON",
                                it,
                            )
                            saveToJson(settings)
                        }
                }
            }
        }
    }
}

class DesktopAppSettingsState(
    private val storage: DesktopAppSettingsStorage,
    private val scope: CoroutineScope,
) {
    var settings by mutableStateOf(DesktopAppSettings())
        private set

    init {
        scope.launch {
            storage.load()?.let { settings = it }
        }
    }

    fun update(transform: (DesktopAppSettings) -> DesktopAppSettings) {
        val updated = transform(settings)
        settings = updated
        scope.launch { storage.save(updated) }
    }

    fun set(newSettings: DesktopAppSettings) {
        settings = newSettings
        scope.launch { storage.save(newSettings) }
    }
}

@Composable
fun rememberDesktopAppSettingsState(
    storage: DesktopAppSettingsStorage = remember { DesktopAppSettingsStorage() },
): DesktopAppSettingsState {
    val scope = rememberCoroutineScope()
    return remember(storage) { DesktopAppSettingsState(storage, scope) }
}

internal const val DownloadTypeNone = 0
internal const val DownloadTypePrevious = 1

internal const val UpdateChannelStable = 0
internal const val UpdateChannelPreview = 1

private fun logAppSettingsStorageWarning(message: String, throwable: Throwable) {
    DesktopStorageEventLogger.warn(
        component = "DesktopAppSettingsStorage",
        event = "app_settings_storage_warning",
        message = message,
        details = mapOf("backend" to DesktopStorageConfig.backend.name),
        throwable = throwable,
    )
}
