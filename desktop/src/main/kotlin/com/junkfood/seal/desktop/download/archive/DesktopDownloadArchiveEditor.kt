package com.junkfood.seal.desktop.download.archive

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

internal enum class ArchiveEditorProblem { Read, Save, Clear, ChangedExternally, DownloadsActive, UnsavedChanges }
internal enum class ArchiveEditorNotice { Saved, Cleared }

internal data class ArchiveEditorState(
    val snapshot: DesktopDownloadArchiveSnapshot? = null,
    val draft: String = "",
    val busy: Boolean = false,
    val problem: ArchiveEditorProblem? = null,
    val notice: ArchiveEditorNotice? = null,
) {
    val dirty: Boolean get() = snapshot != null && draft != snapshot.content
}

/** Session-only editor state. Disk access and atomic writes remain in the existing service. */
internal class DesktopDownloadArchiveEditor(
    private val service: DesktopDownloadArchiveService,
    private val mutationsAllowed: () -> Boolean = { true },
) {
    private val mutableState = MutableStateFlow(ArchiveEditorState())
    val state = mutableState.asStateFlow()
    private val operation = Mutex()

    fun updateDraft(content: String) {
        val current = mutableState.value
        if (current.busy || current.snapshot == null) return
        mutableState.value = current.copy(draft = content, problem = null, notice = null)
    }

    fun discardDraft() {
        val current = mutableState.value
        if (!current.busy) mutableState.value = current.copy(draft = current.snapshot?.content.orEmpty(), problem = null, notice = null)
    }

    suspend fun refresh(discardChanges: Boolean = false) {
        run(ArchiveEditorProblem.Read) {
            if (mutableState.value.dirty && !discardChanges) {
                problem(ArchiveEditorProblem.UnsavedChanges)
            } else {
                accept(service.read())
            }
        }
    }

    suspend fun save() = modify(ArchiveEditorProblem.Save) { service.save(mutableState.value.draft) }

    suspend fun clear() = modify(ArchiveEditorProblem.Clear) { service.clear() }

    private suspend fun modify(
        failure: ArchiveEditorProblem,
        write: suspend () -> DesktopDownloadArchiveSnapshot,
    ) = run(failure) {
        val original = mutableState.value.snapshot ?: return@run
        if (!mutationsAllowed()) {
            problem(ArchiveEditorProblem.DownloadsActive)
        } else if (service.read().content != original.content) {
            // yt-dlp and external editors can append while this dialog is open.
            // Do not silently replace a newer archive with an old editor snapshot.
            problem(ArchiveEditorProblem.ChangedExternally)
        } else if (!mutationsAllowed()) {
            problem(ArchiveEditorProblem.DownloadsActive)
        } else {
            accept(write(), if (failure == ArchiveEditorProblem.Clear) ArchiveEditorNotice.Cleared else ArchiveEditorNotice.Saved)
        }
    }

    private suspend fun run(failure: ArchiveEditorProblem, block: suspend () -> Unit) {
        if (!operation.tryLock()) return
        mutableState.value = mutableState.value.copy(busy = true, problem = null, notice = null)
        try {
            block()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: DesktopDownloadArchiveException) {
            problem(failure)
        } finally {
            mutableState.value = mutableState.value.copy(busy = false)
            operation.unlock()
        }
    }

    private fun problem(value: ArchiveEditorProblem) {
        mutableState.value = mutableState.value.copy(problem = value)
    }

    private fun accept(snapshot: DesktopDownloadArchiveSnapshot, notice: ArchiveEditorNotice? = null) {
        mutableState.value = ArchiveEditorState(snapshot, snapshot.content, busy = true, notice = notice)
    }
}
