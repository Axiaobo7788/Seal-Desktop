package com.junkfood.seal.desktop.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDownloadInputTest {
    @Test
    fun `single URL is normalized and accepted`() {
        val validation = validateDesktopDownloadInput("watch https://example.org/video now")

        assertEquals("https://example.org/video", validation.normalizedInput)
        assertEquals(1, validation.urlCount)
        assertTrue(validation.canProceed)
    }

    @Test
    fun `multiple URLs are rejected instead of becoming one yt-dlp argument`() {
        val validation = validateDesktopDownloadInput("https://example.org/1\nhttps://example.org/2\nhttps://example.org/1")

        assertEquals(2, validation.urlCount)
        assertTrue(validation.hasMultipleUrls)
        assertFalse(validation.canProceed)
    }

    @Test
    fun `configure before download controls the destination`() {
        assertEquals(DownloadInputDestination.Configure, downloadInputDestination(true, false, false))
        assertEquals(DownloadInputDestination.Start, downloadInputDestination(false, false, false))
        assertEquals(DownloadInputDestination.Start, downloadInputDestination(false, true, true))
        assertEquals(DownloadInputDestination.Configure, downloadInputDestination(false, true, false))
        assertEquals(
            DownloadInputDestination.Configure,
            downloadInputDestination(false, false, false, requiresFormatSelection = true),
        )
        assertEquals(
            DownloadInputDestination.Start,
            downloadInputDestination(false, true, true, requiresFormatSelection = true),
        )
    }
}
