package com.junkfood.seal.desktop.download.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HistoryImportPolicyTest {
    private fun entry(index: Int) =
        DesktopDownloadHistoryEntry(
            id = index.toString(),
            title = "video$index",
            url = "https://example.com/$index",
        )

    @Test
    fun `full history keeps imported entry and removes oldest entry`() {
        val existing = (0 until HISTORY_RETENTION_LIMIT).map(::entry)

        val update =
            importHistory(
                encodeHistoryEntries(listOf(entry(501))),
                existing,
                DesktopHistoryImportMode.Merge,
            )

        assertEquals(HistoryImportResult(imported = 1, truncated = 1), update.result)
        assertEquals(listOf("501") + existing.take(499).map { it.id }, update.entries.map { it.id })
    }

    @Test
    fun `duplicates malformed input and invalid URL lines are reported`() {
        val existing = listOf(entry(1))
        val update =
            importHistory(
                encodeHistoryEntries(listOf(entry(1), entry(2), entry(2))),
                existing,
                DesktopHistoryImportMode.Merge,
            )

        assertEquals(HistoryImportResult(imported = 1, duplicates = 2), update.result)
        assertFailsWith<IllegalArgumentException> {
            importHistory("{", existing, DesktopHistoryImportMode.Replace)
        }

        val urls =
            importHistory(
                "https://example.com/a\nhttps://example.com/a\nnot-a-url",
                existing,
                DesktopHistoryImportMode.Replace,
            )
        assertEquals(HistoryImportResult(imported = 1, duplicates = 1, invalid = 1), urls.result)
    }

    @Test
    fun `empty replace import preserves existing history`() {
        val existing = listOf(entry(1))

        assertEquals(existing, importHistory("", existing, DesktopHistoryImportMode.Replace).entries)
    }

    @Test
    fun `desktop export preserves output directory while Android backup remains readable`() {
        val original = entry(1).copy(id = "uuid", outputDirectory = "/download/chapters")

        assertEquals(listOf(original), decodeHistoryEntries(encodeHistoryEntries(listOf(original))))
        val legacy =
            decodeHistoryEntries(
                """{"downloadHistory":[{"id":1,"videoTitle":"legacy","videoUrl":"https://example.com/a"}]}""",
            )
        assertEquals("android-1", legacy.single().id)
        assertEquals(null, legacy.single().outputDirectory)
    }
}
