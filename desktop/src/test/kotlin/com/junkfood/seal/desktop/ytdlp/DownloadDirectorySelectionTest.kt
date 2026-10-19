package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.download.DownloadPlan
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadDirectorySelectionTest {
    @Test
    fun `audio plans use the configured audio directory`() {
        val audioDir = createTempDirectory("seal-audio-dir")
        val plan = DownloadPlan(options = emptyList(), outputTemplate = "%(title)s", downloadPathHint = "audio")

        val config =
            DownloadPlanExecutor().defaultConfigFor(
                plan = plan,
                url = "https://example.invalid/audio",
                preferences = DownloadPreferences.EMPTY.copy(audioDirectory = audioDir.toString()),
            )

        assertEquals(audioDir.normalizedString(), config.workingDirectory?.normalizedString())
    }

    @Test
    fun `video plans use the configured video directory`() {
        val videoDir = createTempDirectory("seal-video-dir")
        val plan = DownloadPlan(options = emptyList(), outputTemplate = "%(title)s", downloadPathHint = "video")

        val config =
            DownloadPlanExecutor().defaultConfigFor(
                plan = plan,
                url = "https://example.invalid/video",
                preferences = DownloadPreferences.EMPTY.copy(videoDirectory = videoDir.toString()),
            )

        assertEquals(videoDir.normalizedString(), config.workingDirectory?.normalizedString())
    }

    @Test
    fun `custom command uses the configured command directory`() {
        val commandDir = createTempDirectory("seal-command-dir")

        val resolved = DesktopYtDlpPaths.configuredDownloadDirectory(commandDir.toString())

        assertEquals(commandDir.normalizedString(), resolved.normalizedString())
    }

    @Test
    fun `website subdirectory composes with video audio and playlist directories`() {
        val videoDir = createTempDirectory("seal-video-website")
        val audioDir = createTempDirectory("seal-audio-website")
        val preferences =
            DownloadPreferences.EMPTY.copy(
                videoDirectory = videoDir.toString(),
                audioDirectory = audioDir.toString(),
                subdirectoryExtractor = true,
                subdirectoryPlaylistTitle = true,
                downloadPlaylist = true,
            )
        val videoInfo = VideoInfo(vcodec = "avc1", extractorKey = "BiliBili", playlist = "Course")

        val videoPlan = buildDownloadPlan(videoInfo, preferences)
        val videoConfig =
            DownloadPlanExecutor().defaultConfigFor(
                plan = videoPlan,
                url = "https://example.invalid/playlist",
                preferences = preferences,
                extractorKey = videoInfo.extractorKey,
            )
        val audioPlan = buildDownloadPlan(videoInfo, preferences.copy(extractAudio = true))
        val audioConfig =
            DownloadPlanExecutor().defaultConfigFor(
                plan = audioPlan,
                url = "https://example.invalid/playlist",
                preferences = preferences.copy(extractAudio = true),
                extractorKey = videoInfo.extractorKey,
            )

        assertEquals(videoDir.resolve("BiliBili").normalizedString(), videoConfig.workingDirectory?.normalizedString())
        assertEquals(audioDir.resolve("BiliBili").normalizedString(), audioConfig.workingDirectory?.normalizedString())
        assertTrue(videoPlan.outputTemplate.startsWith("%(playlist)s/"))
        assertTrue(audioPlan.outputTemplate.startsWith("%(playlist)s/"))
    }

    @Test
    fun `website subdirectory toggle controls only the extractor path segment`() {
        val videoDir = createTempDirectory("seal-video-base")
        val base = DownloadPreferences.EMPTY.copy(videoDirectory = videoDir.toString())

        val withoutWebsite = DesktopYtDlpPaths.downloadDirectoryFor(base, "video", "Youtube")
        val withWebsite =
            DesktopYtDlpPaths.downloadDirectoryFor(
                base.copy(subdirectoryExtractor = true),
                "video",
                "Youtube",
            )

        assertEquals(videoDir.normalizedString(), withoutWebsite.normalizedString())
        assertEquals(videoDir.resolve("Youtube").normalizedString(), withWebsite.normalizedString())
    }

    @Test
    fun `missing or malformed extractor safely falls back to the configured base`() {
        val videoDir = createTempDirectory("seal-video-safe")
        val preferences =
            DownloadPreferences.EMPTY.copy(
                videoDirectory = videoDir.toString(),
                subdirectoryExtractor = true,
            )

        listOf(null, "", "..", "../escape", "nested/path", "C:\\escape", "bad:name").forEach { extractor ->
            val resolved = DesktopYtDlpPaths.downloadDirectoryFor(preferences, "video", extractor)
            assertEquals(videoDir.normalizedString(), resolved.normalizedString(), extractor.orEmpty())
        }
    }

    @Test
    fun `subdirectory preview covers all website and playlist combinations`() {
        assertEquals("file_name", DesktopYtDlpPaths.subdirectoryPreviewSuffix(false, false))
        assertEquals("website/file_name", DesktopYtDlpPaths.subdirectoryPreviewSuffix(true, false))
        assertEquals("playlist_title/file_name", DesktopYtDlpPaths.subdirectoryPreviewSuffix(false, true))
        assertEquals("website/playlist_title/file_name", DesktopYtDlpPaths.subdirectoryPreviewSuffix(true, true))
    }

    @Test
    fun `crop artwork injects ffmpeg crop postprocessor args for audio metadata downloads`() {
        val plan = DownloadPlan(options = emptyList(), outputTemplate = "%(title)s", downloadPathHint = "audio")

        val config =
            DownloadPlanExecutor().defaultConfigFor(
                plan = plan,
                url = "https://example.invalid/audio",
                preferences =
                    DownloadPreferences.EMPTY.copy(
                        extractAudio = true,
                        embedMetadata = true,
                        cropArtwork = true,
                    ),
            )

        assertTrue(config.extraArgs.contains("--ppa"))
        assertTrue(config.extraArgs.any { it.contains("crop=") })
    }

    @Test
    fun `crop artwork does not inject postprocessor args without metadata embedding`() {
        val plan = DownloadPlan(options = emptyList(), outputTemplate = "%(title)s", downloadPathHint = "audio")

        val config =
            DownloadPlanExecutor().defaultConfigFor(
                plan = plan,
                url = "https://example.invalid/audio",
                preferences =
                    DownloadPreferences.EMPTY.copy(
                        extractAudio = true,
                        embedMetadata = false,
                        cropArtwork = true,
                    ),
            )

        assertFalse(config.extraArgs.contains("--ppa"))
        assertFalse(config.extraArgs.any { it.contains("crop=") })
    }
}

private fun java.nio.file.Path.normalizedString(): String =
    toAbsolutePath().normalize().toString()
