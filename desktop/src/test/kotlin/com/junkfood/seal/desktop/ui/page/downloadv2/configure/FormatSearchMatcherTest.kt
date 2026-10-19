package com.junkfood.seal.desktop.ui.page.downloadv2.configure

import com.junkfood.seal.util.Format
import kotlin.test.*

class FormatSearchMatcherTest {
    private val video = Format(formatId = "137", width = 1920.0, height = 1080.0, fps = 60.0, ext = "webm", vcodec = "av01.0.08M.08", acodec = "none", format = "137 - high quality")
    private val audio = Format(formatId = "140", ext = "m4a", vcodec = "none", acodec = "opus", abr = 128.0)

    @Test fun `search ids dimensions description fps bitrate and codec aliases`() {
        listOf("", "  ", "137", "1080", "1920", "1080p60", "1920x1080", "AV1", "WEBM", "high quality", "webm 60")
            .forEach { assertTrue(FormatSearchMatcher.matches(video, it), it) }
        listOf("opus", "M4A", "128", "128kbps").forEach { assertTrue(FormatSearchMatcher.matches(audio, it), it) }
        assertTrue(FormatSearchMatcher.matches(video.copy(vcodec = "vp09.00"), "VP9"))
        assertFalse(FormatSearchMatcher.matches(video, "mp4"))
        assertFalse(FormatSearchMatcher.matches(Format(), "1080"))
    }

    @Test fun `filter preserves original indexes and can match beyond collapsed limit`() {
        val formats = (0..20).map { audio.copy(formatId = "audio-$it") }
        assertEquals((0..5).toList(), FormatSearchMatcher.visible(formats, "", 6).map { it.index })
        assertEquals(20, FormatSearchMatcher.visible(formats, "audio-20", 6).single().index)
        assertEquals(formats[20], FormatSearchMatcher.visible(formats, "audio-20", 6).single().value)
        assertTrue(FormatSearchMatcher.visible(formats, "vp9", 6).isEmpty())
        assertEquals(formats.size, FormatSearchMatcher.visible(formats, "", Int.MAX_VALUE).size)
    }
}
