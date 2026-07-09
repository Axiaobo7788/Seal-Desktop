package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.download.DownloadPlan
import com.junkfood.seal.util.DownloadPreferences
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
