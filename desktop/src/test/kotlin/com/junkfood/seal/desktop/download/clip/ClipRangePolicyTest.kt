package com.junkfood.seal.desktop.download.clip

import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.download.SelectionMerge
import com.junkfood.seal.download.buildDownloadPlan
import kotlin.test.*

class ClipRangePolicyTest {
    @Test fun `finite duration and non-live eligibility`() {
        listOf(null, 0.0, 0.9, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Int.MAX_VALUE.toDouble() + 1).forEach {
            assertFalse(ClipRangePolicy.available(it, null))
        }
        listOf("is_live", "is_upcoming", "post_live").forEach { assertFalse(ClipRangePolicy.available(600.0, it)) }
        assertTrue(ClipRangePolicy.available(5.9, "was_live"))
        assertTrue(ClipRangePolicy.valid(0, 1, 1))
        listOf(VideoClip(-1, 2), VideoClip(0, 0), VideoClip(2, 1), VideoClip(0, 11)).forEach {
            assertFalse(ClipRangePolicy.valid(it.start, it.end, 10))
        }
    }

    @Test fun `draft edits preserve last valid range and slider reset synchronizes drafts`() {
        val initial = ClipEditorState.create(600)
        val valid = initial.edit(start = "0:42", end = "8:17")
        assertEquals(VideoClip(42, 497), valid.range)
        val invalid = valid.edit(start = "1:")
        assertEquals(valid.range, invalid.range)
        assertNull(invalid.validDraft)
        assertEquals(VideoClip(83, 497), invalid.edit(start = "1:23").validDraft)
        val moved = invalid.move(60, 120)
        assertEquals("1:00", moved.startDraft)
        assertEquals("2:00", moved.endDraft)
        assertEquals(moved, moved.move(120, 120))
        assertEquals(initial, moved.reset())
    }

    @Test fun `restore handles bad saved range and edit modes are mutually exclusive`() {
        assertEquals(VideoClip(0, 10), ClipEditorState.create(10, VideoClip(42, 497)).range)
        assertEquals(VideoClip(2, 8), ClipEditorState.create(10, VideoClip(2, 8)).range)
        val restored = VideoEditModePolicy.restore(listOf(VideoClip(2, 8)), true, true, 4)
        assertEquals(VideoEditMode.Clip, restored)
        val split = VideoEditModePolicy.toggle(restored, VideoEditMode.Chapters)
        assertEquals(VideoEditMode.Chapters, split)
        assertEquals(VideoEditMode.Clip, VideoEditModePolicy.toggle(split, VideoEditMode.Clip))
        assertEquals(VideoEditMode.None, VideoEditModePolicy.toggle(split, VideoEditMode.Chapters))
        assertEquals(VideoEditMode.None, VideoEditModePolicy.restore(listOf(VideoClip(0, 1)), true, false, 0))
    }

    @Test fun `validated editor range reaches selection merge and existing download plan unchanged`() {
        val state = ClipEditorState.create(600).edit(start = "0:42", end = "8:17")
        val merged = SelectionMerge.merge(
            basePreferences = DownloadPreferences.EMPTY,
            videoInfo = VideoInfo(duration = 600.0, vcodec = "vp9"),
            formatList = listOf(Format(formatId = "137", vcodec = "vp9", acodec = "none")),
            videoClips = listOf(state.validDraft!!),
            splitByChapter = false,
            newTitle = "",
            selectedSubtitles = emptyList(),
            selectedAutoCaptions = emptyList(),
        )
        val args = buildDownloadPlan(merged.videoInfo, merged.preferences).asCliArgs()
        assertEquals("*42-497", args[args.indexOf("--download-sections") + 1])
        assertFalse(args.contains("--split-chapters"))
        assertTrue(args.any { it.contains("%(section_start)d-%(section_end)d") })
        assertFalse(buildDownloadPlan(merged.videoInfo, merged.preferences.copy(videoClips = emptyList())).asCliArgs().contains("--download-sections"))
    }
}
