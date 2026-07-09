package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadPlanFactoryTest {

    @Test
    fun `defaults aria2c downloader to Android library name`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "none"),
                preferences = DownloadPreferences.EMPTY.copy(aria2c = true),
            )

        val args = plan.asCliArgs()
        assertTrue(args.containsAll(listOf("--downloader", "libaria2c.so")))
    }

    @Test
    fun `allows desktop adapter to override aria2c downloader name`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "none"),
                preferences = DownloadPreferences.EMPTY.copy(aria2c = true),
                aria2cDownloader = "aria2c",
            )

        val args = plan.asCliArgs()
        assertTrue(args.containsAll(listOf("--downloader", "aria2c")))
        assertFalse(args.contains("libaria2c.so"))
    }

    @Test
    fun `embedding subtitles remuxes video to mkv even when explicit mkv preference is off`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "vp9"),
                preferences =
                    DownloadPreferences.EMPTY.copy(
                        downloadSubtitle = true,
                        embedSubtitle = true,
                        mergeToMkv = false,
                    ),
            )

        val args = plan.asCliArgs()
        assertTrue(args.containsAll(listOf("--embed-subs", "--remux-video", "mkv", "--merge-output-format")))
    }

    @Test
    fun `downloading subtitles without embedding does not force mkv remux`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "vp9"),
                preferences =
                    DownloadPreferences.EMPTY.copy(
                        downloadSubtitle = true,
                        embedSubtitle = false,
                        mergeToMkv = false,
                    ),
            )

        val args = plan.asCliArgs()
        assertTrue(args.contains("--write-subs"))
        assertFalse(args.contains("--embed-subs"))
        assertFalse(args.contains("--remux-video"))
        assertFalse(args.contains("--merge-output-format"))
    }
}
