package com.junkfood.seal.desktop.download.archive

import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.util.VideoInfo
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class DesktopDownloadArchiveServiceTest {
    @Test
    fun `missing archive reads as empty`() = runBlocking {
        val path = createTempDirectory("seal-archive-empty").resolve("download-archive.txt")
        val snapshot = service(path).read()

        assertEquals(path, snapshot.path)
        assertEquals("", snapshot.content)
        assertEquals(0, snapshot.count)
        assertEquals(0, snapshot.malformedLineCount)
    }

    @Test
    fun `contains and precheck use exact extractor and media id`() = runBlocking {
        val path = createTempDirectory("seal-archive-contains").resolve("download-archive.txt")
        path.writeText("youtube abc123\nvimeo 456\n")
        val service = service(path)

        assertTrue(service.contains("youtube", "abc123"))
        assertFalse(service.contains("Youtube", "abc123"))
        assertFalse(service.contains("youtube", "abc"))
        assertIs<DesktopDownloadArchivePrecheck.AlreadyArchived>(service.precheck("vimeo", "456"))
        assertIs<DesktopDownloadArchivePrecheck.NotArchived>(service.precheck("vimeo", "missing"))
        Unit
    }

    @Test
    fun `save preserves editable text and reports malformed lines`() = runBlocking {
        val path = createTempDirectory("seal-archive-save").resolve("download-archive.txt")
        val content = "youtube first\nmalformed\n\n vimeo second \n"

        val snapshot = service(path).save(content)

        assertEquals(content, path.readText())
        assertEquals(content, snapshot.content)
        assertEquals(2, snapshot.count)
        assertEquals(1, snapshot.malformedLineCount)
        assertEquals(listOf("youtube first", "vimeo second"), snapshot.entries.map { it.archiveId })
        assertFalse(Files.list(path.parent).use { files -> files.anyMatch { ".tmp-" in it.fileName.toString() } })
    }

    @Test
    fun `clear atomically replaces archive with empty content`() = runBlocking {
        val path = createTempDirectory("seal-archive-clear").resolve("download-archive.txt")
        path.writeText("youtube abc123\n")

        val snapshot = service(path).clear()

        assertEquals("", path.readText())
        assertEquals(0, snapshot.count)
    }

    @Test
    fun `read failures retain the archive path for user-facing diagnostics`() = runBlocking {
        val path = createTempDirectory("seal-archive-read-failure").resolve("download-archive.txt")
        Files.createDirectory(path)

        val error = assertFailsWith<DesktopDownloadArchiveException> { service(path).read() }

        assertEquals(path, error.archivePath)
        assertTrue(error.cause != null)
    }

    @Test
    fun `concurrent saves and reads only expose complete snapshots`() = runBlocking {
        val path = createTempDirectory("seal-archive-concurrent").resolve("download-archive.txt")
        val service = service(path)
        val contents = List(24) { index -> "youtube id-$index\nvimeo item-$index\n" }

        val writes = contents.map { content -> async(Dispatchers.Default) { service.save(content) } }
        val reads = List(24) { async(Dispatchers.Default) { service.read().content } }
        writes.awaitAll()
        val observed = reads.awaitAll()

        assertTrue(observed.all { it.isEmpty() || it in contents })
        assertTrue(path.readText() in contents)
        assertEquals(2, service.read().count)
    }

    @Test
    fun `archive identity prefers extractor and safely falls back to extractor key`() {
        assertEquals(
            "youtube id",
            VideoInfo(id = "id", extractor = "youtube", extractorKey = "YoutubeTab").downloadArchiveIdentity()?.archiveId,
        )
        assertEquals(
            "youtube id",
            VideoInfo(id = "id", extractorKey = "Youtube").downloadArchiveIdentity()?.archiveId,
        )
        assertEquals(null, VideoInfo(id = "", extractor = "youtube").downloadArchiveIdentity())
    }

    @Test
    fun `yt-dlp archive skip output is not classified as success`() {
        val skipped =
            DownloadPlanExecutor.ExecutionResult(
                exitCode = 0,
                stdout = listOf("[download] abc123 has already been recorded in the archive"),
                stderr = emptyList(),
            )
        val normal = DownloadPlanExecutor.ExecutionResult(0, listOf("[download] 100%"), emptyList())

        assertTrue(skipped.wasSkippedByDownloadArchive())
        assertFalse(normal.wasSkippedByDownloadArchive())
    }

    private fun service(path: java.nio.file.Path): DesktopDownloadArchiveService =
        DesktopDownloadArchiveService { path }
}
