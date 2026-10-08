package com.junkfood.seal.desktop.download.archive

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopDownloadArchiveEditorTest {
    @Test
    fun `missing archive is viewable and save uses existing atomic service`() = withEditor { path, editor ->
        editor.refresh()
        assertEquals(0, editor.state.value.snapshot?.count)
        editor.updateDraft("youtube first\n")
        assertTrue(editor.state.value.dirty)
        editor.save()
        assertEquals("youtube first\n", path.readText())
        assertEquals(ArchiveEditorNotice.Saved, editor.state.value.notice)
        assertEquals(1, editor.state.value.snapshot?.count)
        assertFalse(editor.state.value.dirty)
    }

    @Test
    fun `refresh cannot discard unsaved changes without confirmation`() = withEditor { path, editor ->
        path.writeText("youtube first\n")
        editor.refresh()
        editor.updateDraft("youtube edited\n")
        editor.refresh()
        assertEquals(ArchiveEditorProblem.UnsavedChanges, editor.state.value.problem)
        assertEquals("youtube edited\n", editor.state.value.draft)
        editor.refresh(discardChanges = true)
        assertEquals("youtube first\n", editor.state.value.draft)
    }

    @Test
    fun `confirmed close discards session draft and reopening loads new disk content`() = withEditor { path, editor ->
        path.writeText("youtube first\n")
        editor.refresh()
        editor.updateDraft("youtube edited\n")
        editor.discardDraft()
        assertFalse(editor.state.value.dirty)
        path.writeText("youtube second\n")
        editor.refresh()
        assertEquals("youtube second\n", editor.state.value.draft)
    }

    @Test
    fun `external append rejects stale save and clear without losing draft`() = withEditor { path, editor ->
        path.writeText("youtube first\n")
        editor.refresh()
        editor.updateDraft("youtube edited\n")
        path.writeText("youtube first\nyoutube new\n")
        editor.save()
        assertEquals(ArchiveEditorProblem.ChangedExternally, editor.state.value.problem)
        assertEquals("youtube edited\n", editor.state.value.draft)
        editor.clear()
        assertEquals(ArchiveEditorProblem.ChangedExternally, editor.state.value.problem)
        assertEquals("youtube first\nyoutube new\n", path.readText())
    }

    @Test
    fun `active downloads block mutations but allow reads`() = runBlocking {
        val root = createTempDirectory("seal-archive-active")
        try {
            val path = root.resolve("archive.txt")
            var allowed = true
            val editor = DesktopDownloadArchiveEditor(DesktopDownloadArchiveService { path }) { allowed }
            editor.refresh()
            editor.updateDraft("youtube first\n")
            allowed = false
            editor.save()
            assertEquals(ArchiveEditorProblem.DownloadsActive, editor.state.value.problem)
            assertFalse(Files.exists(path))
            editor.clear()
            assertEquals(ArchiveEditorProblem.DownloadsActive, editor.state.value.problem)
            allowed = true
            editor.save()
            assertEquals(1, editor.state.value.snapshot?.count)
        } finally { root.toFile().deleteRecursively() }
    }

    @Test
    fun `clear refreshes count and disk while save failure retains draft`() = withEditor { path, editor ->
        path.writeText("youtube first\n")
        editor.refresh()
        editor.clear()
        assertEquals("", path.readText())
        assertEquals(ArchiveEditorNotice.Cleared, editor.state.value.notice)
        assertEquals(0, editor.state.value.snapshot?.count)
        editor.updateDraft("youtube edited\n")
        Files.delete(path)
        Files.createDirectory(path)
        editor.save()
        assertEquals(ArchiveEditorProblem.Save, editor.state.value.problem)
        assertEquals("youtube edited\n", editor.state.value.draft)
        assertFalse(editor.state.value.busy)
    }

    @Test
    fun `read failure is not a success or editable empty archive`() = withEditor { path, editor ->
        Files.createDirectory(path)
        editor.refresh()
        assertEquals(ArchiveEditorProblem.Read, editor.state.value.problem)
        assertEquals(null, editor.state.value.snapshot)
        editor.updateDraft("discarded")
        assertEquals("", editor.state.value.draft)
    }

    @Test
    fun `write failures report save and clear separately and retain the draft`() = runBlocking {
        val root = createTempDirectory("seal-archive-write-failure")
        try {
            val parent = root.resolve("blocked-parent")
            val editor = DesktopDownloadArchiveEditor(DesktopDownloadArchiveService { parent.resolve("archive.txt") })
            editor.refresh()
            editor.updateDraft("youtube edited\n")
            parent.writeText("not a directory")
            editor.save()
            assertEquals(ArchiveEditorProblem.Save, editor.state.value.problem)
            assertEquals("youtube edited\n", editor.state.value.draft)
            editor.clear()
            assertEquals(ArchiveEditorProblem.Clear, editor.state.value.problem)
            assertEquals("youtube edited\n", editor.state.value.draft)
            assertEquals("not a directory", parent.readText())
            assertFalse(editor.state.value.busy)
        } finally { root.toFile().deleteRecursively() }
    }

    private fun withEditor(test: suspend (Path, DesktopDownloadArchiveEditor) -> Unit) = runBlocking {
        val root = createTempDirectory("seal-archive-editor")
        try {
            val path = root.resolve("archive.txt")
            test(path, DesktopDownloadArchiveEditor(DesktopDownloadArchiveService { path }))
        } finally { root.toFile().deleteRecursively() }
    }
}
