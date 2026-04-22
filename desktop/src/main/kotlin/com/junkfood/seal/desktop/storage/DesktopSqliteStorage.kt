package com.junkfood.seal.desktop.storage

import com.junkfood.seal.desktop.download.DesktopQueueBackup
import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryEntry
import com.junkfood.seal.desktop.download.history.decodeHistoryEntries
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object DesktopSqliteStorage {
    private const val SCHEMA_VERSION = 1

    private const val TABLE_SCHEMA_META = "schema_meta"
    private const val TABLE_QUEUE_STATE = "queue_state"
    private const val TABLE_HISTORY_STATE = "history_state"
    private const val TABLE_APP_SETTINGS_STATE = "app_settings_state"

    private val storageJson = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val dbPath = sqliteDbPath()
    private val dbUrl: String by lazy { "jdbc:sqlite:${dbPath.toAbsolutePath()}" }

    @Volatile
    private var initialized = false

    private val initLock = Any()

    fun readQueue(): DesktopQueueBackup? =
        runCatching {
            ensureInitialized()
            withConnection { connection ->
                connection.prepareStatement("SELECT payload FROM $TABLE_QUEUE_STATE WHERE id = 1")
                    .use { statement ->
                        statement.executeQuery().use { rs ->
                            if (!rs.next()) return@withConnection null
                            storageJson.decodeFromString<DesktopQueueBackup>(rs.getString("payload"))
                        }
                    }
            }
        }.onFailure {
            logWarning("Failed to read queue state from SQLite", it)
        }.getOrNull()

    fun writeQueue(backup: DesktopQueueBackup) {
        runCatching {
            ensureInitialized()
            withConnection { connection ->
                connection.prepareStatement(
                    """
                    INSERT INTO $TABLE_QUEUE_STATE (id, payload, updated_at)
                    VALUES (1, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                      payload = excluded.payload,
                      updated_at = excluded.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, storageJson.encodeToString(backup))
                    statement.setLong(2, System.currentTimeMillis())
                    statement.executeUpdate()
                }
            }
        }.onFailure {
            logWarning("Failed to write queue state to SQLite", it)
        }
    }

    fun readHistory(): List<DesktopDownloadHistoryEntry>? =
        runCatching {
            ensureInitialized()
            withConnection { connection ->
                connection.prepareStatement("SELECT payload FROM $TABLE_HISTORY_STATE WHERE id = 1")
                    .use { statement ->
                        statement.executeQuery().use { rs ->
                            if (!rs.next()) return@withConnection null
                            storageJson.decodeFromString<List<DesktopDownloadHistoryEntry>>(rs.getString("payload"))
                        }
                    }
            }
        }.onFailure {
            logWarning("Failed to read history from SQLite", it)
        }.getOrNull()

    fun writeHistory(entries: List<DesktopDownloadHistoryEntry>) {
        runCatching {
            ensureInitialized()
            withConnection { connection ->
                connection.prepareStatement(
                    """
                    INSERT INTO $TABLE_HISTORY_STATE (id, payload, updated_at)
                    VALUES (1, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                      payload = excluded.payload,
                      updated_at = excluded.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, storageJson.encodeToString(entries))
                    statement.setLong(2, System.currentTimeMillis())
                    statement.executeUpdate()
                }
            }
        }.onFailure {
            logWarning("Failed to write history to SQLite", it)
        }
    }

    fun readAppSettings(): DesktopAppSettings? =
        runCatching {
            ensureInitialized()
            withConnection { connection ->
                connection.prepareStatement("SELECT payload FROM $TABLE_APP_SETTINGS_STATE WHERE id = 1")
                    .use { statement ->
                        statement.executeQuery().use { rs ->
                            if (!rs.next()) return@withConnection null
                            storageJson.decodeFromString<DesktopAppSettings>(rs.getString("payload"))
                        }
                    }
            }
        }.onFailure {
            logWarning("Failed to read app settings from SQLite", it)
        }.getOrNull()

    fun writeAppSettings(settings: DesktopAppSettings) {
        runCatching {
            ensureInitialized()
            withConnection { connection ->
                connection.prepareStatement(
                    """
                    INSERT INTO $TABLE_APP_SETTINGS_STATE (id, payload, updated_at)
                    VALUES (1, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                      payload = excluded.payload,
                      updated_at = excluded.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, storageJson.encodeToString(settings))
                    statement.setLong(2, System.currentTimeMillis())
                    statement.executeUpdate()
                }
            }
        }.onFailure {
            logWarning("Failed to write app settings to SQLite", it)
        }
    }

    private fun ensureInitialized() {
        if (initialized) return

        synchronized(initLock) {
            if (initialized) return

            dbPath.parent?.createDirectories()

            withConnection { connection ->
                connection.autoCommit = false
                runCatching {
                    createTables(connection)
                    ensureSchemaVersion(connection)
                    importJsonOnFirstRun(connection)
                    connection.commit()
                }.onFailure { error ->
                    runCatching { connection.rollback() }
                    throw error
                }
            }

            initialized = true
            DesktopStorageEventLogger.info(
                component = "DesktopSqliteStorage",
                event = "sqlite_storage_initialized",
                message = "SQLite storage initialized",
                details = mapOf("path" to dbPath.toAbsolutePath().toString()),
            )
        }
    }

    private fun createTables(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_SCHEMA_META (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_QUEUE_STATE (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    payload TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_HISTORY_STATE (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    payload TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_APP_SETTINGS_STATE (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    payload TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private fun ensureSchemaVersion(connection: Connection) {
        connection.prepareStatement(
            "INSERT OR IGNORE INTO $TABLE_SCHEMA_META (key, value) VALUES ('schema_version', ?)"
        ).use { statement ->
            statement.setString(1, SCHEMA_VERSION.toString())
            statement.executeUpdate()
        }
    }

    private fun importJsonOnFirstRun(connection: Connection) {
        val imported =
            connection.prepareStatement(
                "SELECT value FROM $TABLE_SCHEMA_META WHERE key = 'json_bootstrap_done'"
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    rs.next() && rs.getString("value") == "1"
                }
            }

        if (imported) return

        var importedQueue = false
        var importedHistory = false
        var importedAppSettings = false

        readQueueFromJsonFile()?.let { backup ->
            connection.prepareStatement(
                "INSERT INTO $TABLE_QUEUE_STATE (id, payload, updated_at) VALUES (1, ?, ?)"
            ).use { statement ->
                statement.setString(1, storageJson.encodeToString(backup))
                statement.setLong(2, System.currentTimeMillis())
                statement.executeUpdate()
            }
            importedQueue = true
        }

        readHistoryFromJsonFile()?.let { entries ->
            connection.prepareStatement(
                "INSERT INTO $TABLE_HISTORY_STATE (id, payload, updated_at) VALUES (1, ?, ?)"
            ).use { statement ->
                statement.setString(1, storageJson.encodeToString(entries))
                statement.setLong(2, System.currentTimeMillis())
                statement.executeUpdate()
            }
            importedHistory = true
        }

        readAppSettingsFromJsonFile()?.let { settings ->
            connection.prepareStatement(
                "INSERT INTO $TABLE_APP_SETTINGS_STATE (id, payload, updated_at) VALUES (1, ?, ?)"
            ).use { statement ->
                statement.setString(1, storageJson.encodeToString(settings))
                statement.setLong(2, System.currentTimeMillis())
                statement.executeUpdate()
            }
            importedAppSettings = true
        }

        connection.prepareStatement(
            "INSERT OR REPLACE INTO $TABLE_SCHEMA_META (key, value) VALUES ('json_bootstrap_done', '1')"
        ).use { statement ->
            statement.executeUpdate()
        }

        DesktopStorageEventLogger.info(
            component = "DesktopSqliteStorage",
            event = "sqlite_json_bootstrap_completed",
            message = "Initial JSON bootstrap import into SQLite completed",
            details =
                mapOf(
                    "importedQueue" to importedQueue.toString(),
                    "importedHistory" to importedHistory.toString(),
                    "importedAppSettings" to importedAppSettings.toString(),
                ),
        )
    }

    private fun readQueueFromJsonFile(): DesktopQueueBackup? {
        val path = queueJsonPath()
        if (!path.exists()) return null

        return runCatching {
            storageJson.decodeFromString<DesktopQueueBackup>(path.readText())
        }.getOrElse {
            val quarantined = quarantineCorruptedFile(path)
            if (quarantined != null) {
                DesktopStorageEventLogger.warn(
                    component = "DesktopSqliteStorage",
                    event = "sqlite_import_queue_quarantined",
                    message = "Corrupted queue JSON file quarantined during SQLite import",
                    details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                )
            }
            logWarning("Failed to import queue.json into SQLite", it)
            null
        }
    }

    private fun readHistoryFromJsonFile(): List<DesktopDownloadHistoryEntry>? {
        val path = historyJsonPath()
        if (!path.exists()) return null

        return runCatching {
            decodeHistoryEntries(path.readText())
        }.getOrElse {
            val quarantined = quarantineCorruptedFile(path)
            if (quarantined != null) {
                DesktopStorageEventLogger.warn(
                    component = "DesktopSqliteStorage",
                    event = "sqlite_import_history_quarantined",
                    message = "Corrupted history JSON file quarantined during SQLite import",
                    details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                )
            }
            logWarning("Failed to import history.json into SQLite", it)
            null
        }
    }

    private fun readAppSettingsFromJsonFile(): DesktopAppSettings? {
        val path = appSettingsJsonPath()
        if (!path.exists()) return null

        return runCatching {
            storageJson.decodeFromString<DesktopAppSettings>(path.readText())
        }.getOrElse {
            val quarantined = quarantineCorruptedFile(path)
            if (quarantined != null) {
                DesktopStorageEventLogger.warn(
                    component = "DesktopSqliteStorage",
                    event = "sqlite_import_app_settings_quarantined",
                    message = "Corrupted app-settings JSON file quarantined during SQLite import",
                    details = mapOf("path" to quarantined.toAbsolutePath().toString()),
                )
            }
            logWarning("Failed to import app-settings.json into SQLite", it)
            null
        }
    }

    private fun configureConnection(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA journal_mode = WAL")
            statement.execute("PRAGMA synchronous = NORMAL")
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA busy_timeout = 3000")
        }
    }

    private fun <T> withConnection(block: (Connection) -> T): T {
        DriverManager.getConnection(dbUrl).use { connection ->
            configureConnection(connection)
            return block(connection)
        }
    }

    private fun logWarning(message: String, throwable: Throwable) {
        DesktopStorageEventLogger.warn(
            component = "DesktopSqliteStorage",
            event = "sqlite_storage_warning",
            message = message,
            details = mapOf("path" to dbPath.toAbsolutePath().toString()),
            throwable = throwable,
        )
    }
}
