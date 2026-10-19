package com.junkfood.seal.desktop.download

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.derivedStateOf
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueuePreviewProjectionTest {
    @Test
    fun `disabled previews remain reactive to add progress status and delete`() {
        val items =
            mutableStateListOf(
                DownloadQueueItemState(
                    id = "1",
                    title = "first",
                    thumbnailUrl = "https://example.com/1.jpg",
                ),
            )
        val projected = derivedStateOf { projectQueueItemsForPreview(items, disablePreview = true) }

        assertNull(projected.value.single().thumbnailUrl)
        items +=
            DownloadQueueItemState(
                id = "2",
                title = "second",
                thumbnailUrl = "https://example.com/2.jpg",
            )
        assertEquals(listOf("1", "2"), projected.value.map { it.id })
        assertEquals(listOf(null, null), projected.value.map { it.thumbnailUrl })

        items[1] = items[1].copy(progress = 0.5f, status = DownloadQueueStatus.Running)
        assertEquals(0.5f, projected.value[1].progress)
        assertEquals(DownloadQueueStatus.Running, projected.value[1].status)

        items.removeAt(0)
        assertEquals(listOf("2"), projected.value.map { it.id })
    }

    @Test
    fun `enabled previews preserve thumbnails while returning current snapshot`() {
        val item =
            DownloadQueueItemState(
                id = "1",
                title = "first",
                thumbnailUrl = "https://example.com/1.jpg",
            )

        assertEquals(item.thumbnailUrl, projectQueueItemsForPreview(listOf(item), false).single().thumbnailUrl)
    }
}
