package com.junkfood.seal.desktop.settings

import com.junkfood.seal.desktop.storage.DesktopStorageBackend
import com.junkfood.seal.util.DownloadPreferences
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopPreferencesStorageTest {
    @Test
    fun `legacy JSON fills missing fields from desktop defaults`() = runBlocking {
        val path = createTempDirectory("seal-preferences-legacy").resolve("settings.json")
        path.writeText("""{"cookies":true,"formatIdString":"legacy-format"}""")
        val sqlite = FakePreferencesSqliteStore()
        val storage = storage(path, DesktopStorageBackend.Json, sqlite)

        val loaded = assertNotNull(storage.load())

        assertTrue(loaded.cookies)
        assertEquals("legacy-format", loaded.formatIdString)
        assertEquals(2, loaded.videoFormat)
        assertEquals(3, loaded.videoResolution)
        assertTrue(loaded.embedMetadata)
        assertEquals(0, sqlite.readCount)
        assertEquals(0, sqlite.writeCount)
    }

    @Test
    fun `corrupt JSON is quarantined instead of overwritten`() = runBlocking {
        val directory = createTempDirectory("seal-preferences-corrupt")
        val path = directory.resolve("settings.json")
        path.writeText("{\"broken\":")
        val storage = storage(path, DesktopStorageBackend.Json, FakePreferencesSqliteStore())

        assertNull(storage.load())

        assertFalse(path.exists())
        assertTrue(
            directory.listDirectoryEntries().any {
                it.fileName.toString().startsWith("settings.json.corrupt-") &&
                    it.fileName.toString().endsWith(".bak")
            },
        )
    }

    @Test
    fun `JSON backend saves atomically without touching SQLite`() = runBlocking {
        val directory = createTempDirectory("seal-preferences-json")
        val path = directory.resolve("settings.json")
        val sqlite = FakePreferencesSqliteStore()
        val storage = storage(path, DesktopStorageBackend.Json, sqlite)
        val expected = desktopDefaultPreferences().copy(proxy = true, proxyUrl = "http://localhost:7890")

        storage.save(expected)

        assertEquals(expected, storage.load())
        assertTrue(path.readText().contains("localhost:7890"))
        assertFalse(directory.listDirectoryEntries().any { ".tmp-" in it.fileName.toString() })
        assertEquals(0, sqlite.readCount)
        assertEquals(0, sqlite.writeCount)
    }

    @Test
    fun `dual backend treats JSON as source of truth and mirrors it`() = runBlocking {
        val path = createTempDirectory("seal-preferences-dual-json").resolve("settings.json")
        val jsonPreferences = desktopDefaultPreferences().copy(formatIdString = "json")
        path.writeText(encodeDesktopPreferences(jsonPreferences))
        val sqlite = FakePreferencesSqliteStore(desktopDefaultPreferences().copy(formatIdString = "sqlite"))
        val storage = storage(path, DesktopStorageBackend.DualWrite, sqlite)

        assertEquals(jsonPreferences, storage.load())
        assertEquals(jsonPreferences, sqlite.value)
        assertEquals(0, sqlite.readCount)
        assertEquals(1, sqlite.writeCount)
    }

    @Test
    fun `dual backend falls back to SQLite when JSON is unavailable`() = runBlocking {
        val path = createTempDirectory("seal-preferences-dual-sqlite").resolve("settings.json")
        val sqlitePreferences = desktopDefaultPreferences().copy(formatIdString = "sqlite")
        val sqlite = FakePreferencesSqliteStore(sqlitePreferences)
        val storage = storage(path, DesktopStorageBackend.DualWrite, sqlite)

        assertEquals(sqlitePreferences, storage.load())
        assertEquals(1, sqlite.readCount)
        assertEquals(0, sqlite.writeCount)
    }

    @Test
    fun `SQLite backend migrates existing JSON when database is empty`() = runBlocking {
        val path = createTempDirectory("seal-preferences-sqlite-migration").resolve("settings.json")
        val legacyPreferences = desktopDefaultPreferences().copy(subtitleLanguage = "en.*,.*-orig")
        path.writeText(encodeDesktopPreferences(legacyPreferences))
        val sqlite = FakePreferencesSqliteStore()
        val storage = storage(path, DesktopStorageBackend.Sqlite, sqlite)

        assertEquals(legacyPreferences, storage.load())
        assertEquals(legacyPreferences, sqlite.value)
        assertEquals(1, sqlite.readCount)
        assertEquals(1, sqlite.writeCount)
    }

    @Test
    fun `SQLite save failure falls back to atomic JSON`() = runBlocking {
        val directory = createTempDirectory("seal-preferences-sqlite-fallback")
        val path = directory.resolve("settings.json")
        val sqlite = FakePreferencesSqliteStore(writeSucceeds = false)
        val storage = storage(path, DesktopStorageBackend.Sqlite, sqlite)
        val expected = desktopDefaultPreferences().copy(maxDownloadRate = "4M")

        storage.save(expected)

        assertEquals(expected, decodeDesktopPreferences(path.readText()))
        assertFalse(directory.listDirectoryEntries().any { ".tmp-" in it.fileName.toString() })
        assertEquals(1, sqlite.writeCount)
    }

    @Test
    fun `dual save writes JSON and SQLite`() = runBlocking {
        val path = createTempDirectory("seal-preferences-dual-save").resolve("settings.json")
        val sqlite = FakePreferencesSqliteStore()
        val storage = storage(path, DesktopStorageBackend.DualWrite, sqlite)
        val expected = desktopDefaultPreferences().copy(forceIpv4 = true)

        storage.save(expected)

        assertEquals(expected, decodeDesktopPreferences(path.readText()))
        assertEquals(expected, sqlite.value)
        assertEquals(1, sqlite.writeCount)
    }

    private fun storage(
        path: java.nio.file.Path,
        backend: DesktopStorageBackend,
        sqlite: DesktopPreferencesSqliteStore,
    ): DesktopPreferencesStorage = DesktopPreferencesStorage(path, backend, sqlite)
}

private class FakePreferencesSqliteStore(
    var value: DownloadPreferences? = null,
    private val writeSucceeds: Boolean = true,
) : DesktopPreferencesSqliteStore {
    var readCount: Int = 0
        private set
    var writeCount: Int = 0
        private set

    override fun read(): DownloadPreferences? {
        readCount += 1
        return value
    }

    override fun write(preferences: DownloadPreferences): Boolean {
        writeCount += 1
        if (writeSucceeds) value = preferences
        return writeSucceeds
    }
}
