package com.junkfood.seal.desktop.ytdlp

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YtDlpMetadataFetcherTest {
    @Test
    fun `metadata command does not require ffmpeg location`() {
        val toolRoot = Path.of("build", "test-tools").toAbsolutePath().normalize()
        val ytDlp = toolRoot.resolve("yt-dlp")
        val command =
            buildMetadataCommand(
                ytDlpPath = ytDlp,
                ffmpegPath = null,
                url = "https://example.invalid/video",
            )

        assertEquals(ytDlp.toString(), command.first())
        assertTrue(command.containsAll(listOf("-J", "--no-playlist", "https://example.invalid/video")))
        assertFalse(command.contains("--ffmpeg-location"))
    }

    @Test
    fun `metadata command includes ffmpeg location when available`() {
        val toolRoot = Path.of("build", "test-tools").toAbsolutePath().normalize()
        val command =
            buildMetadataCommand(
                ytDlpPath = toolRoot.resolve("yt-dlp"),
                ffmpegPath = toolRoot.resolve("ffmpeg"),
                url = "https://example.invalid/video",
                proxyUrl = " socks5://127.0.0.1:1080 ",
            )

        assertTrue(command.containsAll(listOf("--ffmpeg-location", toolRoot.toString())))
        assertTrue(command.containsAll(listOf("--proxy", "socks5://127.0.0.1:1080")))
    }
}
