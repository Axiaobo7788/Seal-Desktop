package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun `split chapters keeps both chapter and final output templates`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "vp9"),
                preferences = DownloadPreferences.EMPTY.copy(splitByChapter = true),
            )

        val args = plan.asCliArgs()
        val outputTemplates =
            args.zipWithNext().filter { (option, _) -> option == "-o" }.map { (_, value) -> value }

        assertTrue(args.contains("--split-chapters"))
        assertEquals(
            listOf(
                "chapter:%(title).200B/%(section_number)d - %(section_title).200B.%(ext)s",
                "%(title).200B/%(title).200B.%(ext)s",
            ),
            outputTemplates,
        )
    }

    @Test
    fun `enabled SponsorBlock with an empty category does not emit an invalid argument`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "vp9"),
                preferences =
                    DownloadPreferences.EMPTY.copy(
                        sponsorBlock = true,
                        sponsorBlockCategory = "   ",
                    ),
            )

        assertFalse(plan.asCliArgs().contains("--sponsorblock-remove"))
    }

    @Test
    fun `enabled SponsorBlock preserves a non-empty category expression`() {
        val plan =
            buildDownloadPlan(
                videoInfo = VideoInfo(vcodec = "vp9"),
                preferences =
                    DownloadPreferences.EMPTY.copy(
                        sponsorBlock = true,
                        sponsorBlockCategory = " sponsor,intro ",
                    ),
            )

        assertTrue(
            plan.asCliArgs().containsAll(
                listOf("--sponsorblock-remove", "sponsor,intro"),
            ),
        )
    }
}
