package com.junkfood.seal.desktop.download

import com.junkfood.seal.util.DownloadPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDownloadRetryPreferencesTest {
    @Test
    fun `retry refreshes runtime settings without replacing task intent`() {
        val original =
            DownloadPreferences.EMPTY.copy(
                formatIdString = "137+140",
                subtitleLanguage = "en.*",
                videoDirectory = "/original/video",
                newTitle = "Original title",
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
                formatIdString = "best",
                subtitleLanguage = "ja",
                videoDirectory = "/current/video",
                newTitle = "Current title",
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

        val retry = original.withRetryRuntimePreferences(current)

        assertEquals("137+140", retry.formatIdString)
        assertEquals("en.*", retry.subtitleLanguage)
        assertEquals("/original/video", retry.videoDirectory)
        assertEquals("Original title", retry.newTitle)
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
