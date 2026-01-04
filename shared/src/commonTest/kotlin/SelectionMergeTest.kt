package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectionMergeTest {

    @Test
    fun `merge combines format ids and keeps video+audio split`() {
        val formats = listOf(
            Format(formatId = "137", vcodec = "h264", acodec = null),
            Format(formatId = "140", vcodec = "none", acodec = "aac"),
        )

        val result =
            SelectionMerge.merge(
                basePreferences = DownloadPreferences.EMPTY,
                videoInfo = VideoInfo(id = "id1", title = "title"),
                formatList = formats,
                videoClips = emptyList(),
                splitByChapter = false,
                newTitle = "",
                selectedSubtitles = emptyList(),
                selectedAutoCaptions = emptyList(),
            )

        assertEquals("137+140", result.preferences.formatIdString)
        assertFalse(result.preferences.extractAudio)
        assertFalse(result.preferences.mergeAudioStream)
        assertEquals(listOf(formats[1]), result.audioFormats)
        assertEquals(listOf(formats[0]), result.videoFormats)
    }

    @Test
    fun `audio only selections enable extractAudio and mergeAudioStream`() {
        val formats = listOf(
            Format(formatId = "251", vcodec = null, acodec = "opus"),
            Format(formatId = "250", vcodec = "none", acodec = "opus"),
        )

        val result =
            SelectionMerge.merge(
                basePreferences = DownloadPreferences.EMPTY,
                videoInfo = VideoInfo(id = "id2", title = "title"),
                formatList = formats,
                videoClips = emptyList(),
                splitByChapter = false,
                newTitle = "",
                selectedSubtitles = emptyList(),
                selectedAutoCaptions = emptyList(),
            )

        assertEquals("251+250", result.preferences.formatIdString)
        assertTrue(result.preferences.extractAudio)
        assertTrue(result.preferences.mergeAudioStream)
        assertTrue(result.videoFormats.isEmpty())
        assertEquals(formats, result.audioFormats)
    }

    @Test
    fun `subtitle selection enables flags and language`() {
        val result =
            SelectionMerge.merge(
                basePreferences = DownloadPreferences.EMPTY,
                videoInfo = VideoInfo(id = "id3", title = "title"),
                formatList = emptyList(),
                videoClips = emptyList(),
                splitByChapter = false,
                newTitle = "",
                selectedSubtitles = listOf("en"),
                selectedAutoCaptions = listOf("zh-Hans"),
            )

        assertTrue(result.preferences.downloadSubtitle)
        assertTrue(result.preferences.autoSubtitle)
        assertEquals("en,zh-Hans", result.preferences.subtitleLanguage)
    }

    @Test
    fun `new title and clips propagate`() {
        val clip = VideoClip(start = 1, end = 5)
        val result =
            SelectionMerge.merge(
                basePreferences = DownloadPreferences.EMPTY,
                videoInfo = VideoInfo(id = "id4", title = "old", fileSize = null),
                formatList = listOf(Format(fileSize = 10.0), Format(fileSizeApprox = 5.0)),
                videoClips = listOf(clip),
                splitByChapter = true,
                newTitle = "new title",
                selectedSubtitles = emptyList(),
                selectedAutoCaptions = emptyList(),
            )

        assertEquals("new title", result.videoInfo.title)
        assertEquals(15.0, result.videoInfo.fileSize)
        assertEquals(listOf(clip), result.preferences.videoClips)
        assertTrue(result.preferences.splitByChapter)
        assertEquals("new title", result.preferences.newTitle)
    }
}
