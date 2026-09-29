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
import com.junkfood.seal.desktop.storage.preferencesJsonPath
import com.junkfood.seal.desktop.storage.quarantineCorruptedFile
import com.junkfood.seal.desktop.storage.writeTextAtomically
import com.junkfood.seal.util.DownloadPreferences
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

private val settingsJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

/** Default preference set for desktop builds. */
fun desktopDefaultPreferences(): DownloadPreferences =
    DownloadPreferences.EMPTY.copy(
        formatSorting = false,
        videoFormat = 2, // QUALITY
        videoResolution = 3, // 1080p
        embedMetadata = true,
    )

internal fun encodeDesktopPreferences(preferences: DownloadPreferences): String =
    settingsJson.encodeToString(preferences)

internal fun decodeDesktopPreferences(payload: String): DownloadPreferences {
    val persisted = settingsJson.parseToJsonElement(payload).jsonObject
    val defaults = settingsJson.encodeToJsonElement(desktopDefaultPreferences()).jsonObject
    return settingsJson.decodeFromJsonElement(JsonObject(defaults + persisted))
}

internal interface DesktopPreferencesSqliteStore {
    fun read(): DownloadPreferences?

    fun write(preferences: DownloadPreferences): Boolean
}

private object DefaultDesktopPreferencesSqliteStore : DesktopPreferencesSqliteStore {
    override fun read(): DownloadPreferences? = DesktopSqliteStorage.readPreferences()

    override fun write(preferences: DownloadPreferences): Boolean =
        DesktopSqliteStorage.writePreferences(preferences)
}

class DesktopPreferencesStorage private constructor(
    private val path: Path,
    private val backendProvider: () -> DesktopStorageBackend,
    private val sqliteStore: DesktopPreferencesSqliteStore,
) {
    constructor(path: Path = preferencesJsonPath()) : this(
        path = path,
        backendProvider = { DesktopStorageConfig.backend },
        sqliteStore = DefaultDesktopPreferencesSqliteStore,
    )

    internal constructor(
        path: Path,
        backend: DesktopStorageBackend,
        sqliteStore: DesktopPreferencesSqliteStore,
    ) : this(path, { backend }, sqliteStore)

    private fun loadFromJsonOrNull(): DownloadPreferences? {
        if (!path.exists()) return null

        return runCatching { decodeDesktopPreferences(path.readText()) }
            .getOrElse {
                val quarantined = quarantineCorruptedFile(path)
                if (quarantined != null) {
                    DesktopStorageEventLogger.warn(
                        component = "DesktopPreferencesStorage",
                        event = "json_preferences_quarantined",
                        message = "Corrupted download preferences JSON file quarantined",
                        details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                    )
                }
                logPreferencesStorageWarning(
                    backend = backendProvider(),
                    message = "Failed to parse download preferences JSON, falling back to another backend or defaults",
                    throwable = it,
                )
                null
            }
    }

    private fun saveToJson(preferences: DownloadPreferences) {
        writeTextAtomically(path, encodeDesktopPreferences(preferences))
    }

    private fun mirrorToSqlite(
        preferences: DownloadPreferences,
        backend: DesktopStorageBackend,
        reason: String,
    ) {
        if (!sqliteStore.write(preferences)) {
            logPreferencesStorageWarning(
                backend = backend,
                message = reason,
            )
        }
    }

    suspend fun load(): DownloadPreferences? =
        withContext(Dispatchers.IO) {
            val backend = backendProvider()
            when (backend) {
                DesktopStorageBackend.Json -> loadFromJsonOrNull()
                DesktopStorageBackend.DualWrite -> {
                    val preferencesFromJson = loadFromJsonOrNull()
                    if (preferencesFromJson != null) {
                        mirrorToSqlite(
                            preferences = preferencesFromJson,
                            backend = backend,
                            reason = "Failed to mirror download preferences to SQLite on load",
                        )
                        preferencesFromJson
                    } else {
                        sqliteStore.read()?.also {
                            DesktopStorageEventLogger.info(
                                component = "DesktopPreferencesStorage",
                                event = "dual_mode_fallback_to_sqlite",
                                message = "Download preferences loaded from SQLite because JSON was unavailable",
                                details = mapOf("backend" to backend.name),
                            )
                        }
                    }
                }
                DesktopStorageBackend.Sqlite -> {
                    sqliteStore.read()
                        ?: loadFromJsonOrNull()?.also {
                            mirrorToSqlite(
                                preferences = it,
                                backend = backend,
                                reason = "Failed to migrate JSON download preferences to SQLite",
                            )
                        }
                }
            }
        }

    suspend fun save(preferences: DownloadPreferences) {
        withContext(Dispatchers.IO) {
            val backend = backendProvider()
            when (backend) {
                DesktopStorageBackend.Json -> saveToJson(preferences)
                DesktopStorageBackend.DualWrite -> {
                    saveToJson(preferences)
                    mirrorToSqlite(
                        preferences = preferences,
                        backend = backend,
                        reason = "Failed to mirror download preferences to SQLite on save",
                    )
                }
                DesktopStorageBackend.Sqlite -> {
                    if (!sqliteStore.write(preferences)) {
                        logPreferencesStorageWarning(
                            backend = backend,
                            message = "Failed to save download preferences to SQLite, falling back to JSON",
                        )
                        saveToJson(preferences)
                    }
                }
            }
        }
    }
}

private fun logPreferencesStorageWarning(
    backend: DesktopStorageBackend,
    message: String,
    throwable: Throwable? = null,
) {
    DesktopStorageEventLogger.warn(
        component = "DesktopPreferencesStorage",
        event = "preferences_storage_warning",
        message = message,
        details = mapOf("backend" to backend.name),
        throwable = throwable,
    )
}

class DesktopSettingsState(
    private val storage: DesktopPreferencesStorage,
    private val scope: CoroutineScope,
) {
    var preferences by mutableStateOf(desktopDefaultPreferences())
        private set

    init {
        scope.launch {
            storage.load()?.let { preferences = it }
        }
    }

    fun update(transform: (DownloadPreferences) -> DownloadPreferences) {
        val updated = transform(preferences)
        preferences = updated
        scope.launch { storage.save(updated) }
    }

    fun set(newPreferences: DownloadPreferences) {
        preferences = newPreferences
        scope.launch { storage.save(newPreferences) }
    }

    fun resetToDefaults() = set(desktopDefaultPreferences())
}

@Composable
fun rememberDesktopSettingsState(
    storage: DesktopPreferencesStorage = remember { DesktopPreferencesStorage() },
): DesktopSettingsState {
    val scope = rememberCoroutineScope()
    return remember(storage) { DesktopSettingsState(storage, scope) }
}
