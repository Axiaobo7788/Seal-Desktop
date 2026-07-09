package com.junkfood.seal.desktop.download

import com.junkfood.seal.desktop.download.history.DesktopDownloadHistoryEntry
import com.junkfood.seal.util.DownloadPreferences
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDownloadHistoryPrivacyTest {
    @Test
    fun `private mode skips history append and save`() = runBlocking {
        val existing = historyEntry("existing")
        val entries = mutableListOf(existing)
        val savedSnapshots = mutableListOf<List<DesktopDownloadHistoryEntry>>()

        val appended =
            appendDesktopHistoryEntryIfAllowed(
                historyEntries = entries,
                preferences = DownloadPreferences.EMPTY.copy(privateMode = true),
                entry = historyEntry("private"),
                saveEntries = { savedSnapshots += it },
            )

        assertFalse(appended)
        assertEquals(listOf(existing), entries)
        assertTrue(savedSnapshots.isEmpty())
    }

    @Test
    fun `normal mode prepends history entry trims overflow and saves snapshot`() = runBlocking {
        val entries = MutableList(500) { index -> historyEntry("old-$index") }
        val savedSnapshots = mutableListOf<List<DesktopDownloadHistoryEntry>>()
        val newest = historyEntry("new")

        val appended =
            appendDesktopHistoryEntryIfAllowed(
                historyEntries = entries,
                preferences = DownloadPreferences.EMPTY.copy(privateMode = false),
                entry = newest,
                saveEntries = { savedSnapshots += it },
            )

        assertTrue(appended)
        assertEquals(500, entries.size)
        assertEquals(newest, entries.first())
        assertFalse(entries.any { it.id == "old-499" })
        assertEquals(listOf(entries.toList()), savedSnapshots)
    }

    private fun historyEntry(id: String): DesktopDownloadHistoryEntry =
        DesktopDownloadHistoryEntry(
            id = id,
            title = "Title $id",
            url = "https://example.invalid/$id",
        )
}
