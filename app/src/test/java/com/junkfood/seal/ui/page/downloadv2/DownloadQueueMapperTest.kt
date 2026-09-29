package com.junkfood.seal.ui.page.downloadv2

import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.download.queue.DownloadQueueStatus
import com.junkfood.seal.util.DownloadPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadQueueMapperTest {
    private val task =
        Task(url = "https://example.com/video", preferences = DownloadPreferences.EMPTY)
    private val viewState = Task.ViewState(title = "Example")

    @Test
    fun runningStatePreservesProgress() {
        val state =
            Task.State(
                downloadState =
                    Task.DownloadState.Running(
                        taskId = task.id,
                        progress = 0.42f,
                        progressText = "42%",
                    ),
                videoInfo = null,
                viewState = viewState,
            )

        val item = task.toQueueItemState(state)

        assertEquals(DownloadQueueStatus.Running, item.status)
        assertEquals(0.42f, item.progress)
        assertEquals("42%", item.progressText)
    }

    @Test
    fun completedStateExposesDownloadedFile() {
        val state =
            Task.State(
                downloadState = Task.DownloadState.Completed("/downloads/example.mp4"),
                videoInfo = null,
                viewState = viewState,
            )

        val item = task.toQueueItemState(state)

        assertEquals(DownloadQueueStatus.Completed, item.status)
        assertEquals(1f, item.progress)
        assertEquals("/downloads/example.mp4", item.filePath)
    }

    @Test
    fun errorStateExposesFailureMessage() {
        val state =
            Task.State(
                downloadState =
                    Task.DownloadState.Error(
                        throwable = IllegalStateException("download failed"),
                        action = Task.RestartableAction.Download,
                    ),
                videoInfo = null,
                viewState = viewState,
            )

        val item = task.toQueueItemState(state)

        assertEquals(DownloadQueueStatus.Error, item.status)
        assertEquals("download failed", item.errorMessage)
        assertNull(item.filePath)
    }
}
