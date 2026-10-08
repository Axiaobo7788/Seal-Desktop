package com.junkfood.seal.desktop.storage

import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopSqliteSchemaMigrationTest {
    @Test
    fun `schema one database gains preferences table without losing existing rows`() {
        val database = createTempDirectory("seal-schema-migration").resolve("seal.db")

        DriverManager.getConnection("jdbc:sqlite:${database.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE schema_meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)"
                )
                statement.execute(
                    "INSERT INTO schema_meta (key, value) VALUES ('schema_version', '1')"
                )
                statement.execute(
                    """
                    CREATE TABLE queue_state (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        payload TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    "INSERT INTO queue_state (id, payload, updated_at) VALUES (1, 'legacy-queue', 123)"
                )
            }

            DesktopSqliteStorage.migrateSchema(connection)

            val schemaVersion =
                connection.prepareStatement(
                    "SELECT value FROM schema_meta WHERE key = 'schema_version'"
                ).use { statement ->
                    statement.executeQuery().use { result ->
                        assertTrue(result.next())
                        result.getString("value")
                    }
                }
            val queuePayload =
                connection.prepareStatement("SELECT payload FROM queue_state WHERE id = 1")
                    .use { statement ->
                        statement.executeQuery().use { result ->
                            assertTrue(result.next())
                            result.getString("payload")
                        }
                    }
            val preferencesTableExists =
                connection.prepareStatement(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'preferences_state'"
                ).use { statement ->
                    statement.executeQuery().use { result -> result.next() }
                }

            assertEquals("2", schemaVersion)
            assertEquals("legacy-queue", queuePayload)
            assertTrue(preferencesTableExists)
        }
    }
}
