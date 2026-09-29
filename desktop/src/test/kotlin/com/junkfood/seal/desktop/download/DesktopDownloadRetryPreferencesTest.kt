package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoClip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDownloadRetryPreferencesTest {
    @Test
    fun `retry refreshes runtime settings without replacing task intent`() {
        val original =
            DownloadPreferences.EMPTY.copy(
                extractAudio = true,
                downloadPlaylist = false,
                commandDirectory = "/original/command",
                downloadSubtitle = true,
                embedSubtitle = true,
                keepSubtitle = true,
                formatIdString = "137+140",
                subtitleLanguage = "en.*",
                autoSubtitle = true,
                convertSubtitle = 1,
                audioFormat = 2,
                videoFormat = 3,
                videoResolution = 2160,
                videoDirectory = "/original/video",
                audioDirectory = "/original/audio",
                outputTemplate = "%(title)s-original",
                newTitle = "Original title",
                videoClips = listOf(VideoClip(start = 4, end = 12)),
                splitByChapter = true,
                cookies = false,
                cookiesBrowser = "",
                aria2c = false,
                concurrentFragments = 2,
                debug = false,
                proxy = false,
                proxyUrl = "",
                userAgentString = "old-agent",
                rateLimit = false,
                maxDownloadRate = "100",
                useDownloadArchive = false,
                forceIpv4 = false,
            )
        val current =
            DownloadPreferences.EMPTY.copy(
                extractAudio = false,
                downloadPlaylist = true,
                commandDirectory = "/current/command",
                downloadSubtitle = false,
                embedSubtitle = false,
                keepSubtitle = false,
                formatIdString = "best",
                subtitleLanguage = "ja",
                autoSubtitle = false,
                convertSubtitle = 2,
                audioFormat = 4,
                videoFormat = 5,
                videoResolution = 720,
                videoDirectory = "/current/video",
                audioDirectory = "/current/audio",
                outputTemplate = "%(id)s-current",
                newTitle = "Current title",
                videoClips = listOf(VideoClip(start = 20, end = 40)),
                splitByChapter = false,
                cookies = true,
                cookiesBrowser = "firefox",
                aria2c = true,
                concurrentFragments = 8,
                debug = true,
                proxy = true,
                proxyUrl = "http://127.0.0.1:7890",
                userAgentString = "current-agent",
                rateLimit = true,
                maxDownloadRate = "2048",
                useDownloadArchive = true,
                forceIpv4 = true,
            )

        val originalRequest =
            DesktopDownloadRequest(
                url = "https://example.com/original",
                type = DesktopDownloadType.Audio,
                preferences = original,
            )
        val retryRequest = originalRequest.withRetryRuntimePreferences(current)
        val retry = retryRequest.preferences

        assertEquals("https://example.com/original", retryRequest.url)
        assertEquals(DesktopDownloadType.Audio, retryRequest.type)
        assertTrue(retry.extractAudio)
        assertFalse(retry.downloadPlaylist)
        assertEquals("/original/command", retry.commandDirectory)
        assertTrue(retry.downloadSubtitle)
        assertTrue(retry.embedSubtitle)
        assertTrue(retry.keepSubtitle)
        assertEquals("137+140", retry.formatIdString)
        assertEquals("en.*", retry.subtitleLanguage)
        assertTrue(retry.autoSubtitle)
        assertEquals(1, retry.convertSubtitle)
        assertEquals(2, retry.audioFormat)
        assertEquals(3, retry.videoFormat)
        assertEquals(2160, retry.videoResolution)
        assertEquals("/original/video", retry.videoDirectory)
        assertEquals("/original/audio", retry.audioDirectory)
        assertEquals("%(title)s-original", retry.outputTemplate)
        assertEquals("Original title", retry.newTitle)
        assertEquals(listOf(VideoClip(start = 4, end = 12)), retry.videoClips)
        assertTrue(retry.splitByChapter)
        assertTrue(retry.cookies)
        assertEquals("firefox", retry.cookiesBrowser)
        assertTrue(retry.aria2c)
        assertEquals(8, retry.concurrentFragments)
        assertTrue(retry.debug)
        assertTrue(retry.proxy)
        assertEquals("http://127.0.0.1:7890", retry.proxyUrl)
        assertEquals("current-agent", retry.userAgentString)
        assertTrue(retry.rateLimit)
        assertEquals("2048", retry.maxDownloadRate)
        assertTrue(retry.useDownloadArchive)
        assertTrue(retry.forceIpv4)
    }

    @Test
    fun `retry privacy is enabled when either snapshot or current setting is private`() {
        val publicSnapshot = DownloadPreferences.EMPTY.copy(privateMode = false)
        val privateSnapshot = DownloadPreferences.EMPTY.copy(privateMode = true)

        assertTrue(
            publicSnapshot
                .withRetryRuntimePreferences(DownloadPreferences.EMPTY.copy(privateMode = true))
                .privateMode,
        )
        assertTrue(
            privateSnapshot
                .withRetryRuntimePreferences(DownloadPreferences.EMPTY.copy(privateMode = false))
                .privateMode,
        )
        assertFalse(
            publicSnapshot
                .withRetryRuntimePreferences(DownloadPreferences.EMPTY.copy(privateMode = false))
                .privateMode,
        )
    }
}
