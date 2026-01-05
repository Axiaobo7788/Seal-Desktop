package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomCommandPlanTest {

    @Test
    fun `builds basic options and flags`() {
        val prefs = DownloadPreferences.EMPTY.copy(
            aria2c = true,
            useDownloadArchive = true,
            restrictFilenames = true,
            cookies = true,
            userAgentString = "UA",
            commandDirectory = "/tmp/cmd",
        )

        val plan = buildCustomCommandPlan(urls = listOf("u1", "u2"), preferences = prefs, commandDirectory = prefs.commandDirectory)

        assertTrue(plan.needsArchiveFile)
        assertTrue(plan.needsCookiesFile)
        assertEquals(listOf("u1", "u2"), plan.urls)

        val args = plan.asCliArgs()
        // Order: options then urls
        assertTrue(args.containsAll(listOf("--newline", "-P", "/tmp/cmd", "--downloader", "libaria2c.so", "--restrict-filenames")))
        assertTrue(args.contains("--add-header"))
        assertTrue(args.contains("User-Agent:UA"))
        assertEquals(listOf("u1", "u2"), args.takeLast(2))
    }

    @Test
    fun `respects empty directory and no cookies`() {
        val prefs = DownloadPreferences.EMPTY.copy(aria2c = false, useDownloadArchive = false, restrictFilenames = false, cookies = false)
        val plan = buildCustomCommandPlan(urls = listOf("u"), preferences = prefs, commandDirectory = "")

        assertFalse(plan.needsArchiveFile)
        assertFalse(plan.needsCookiesFile)
        assertEquals(listOf("u"), plan.urls)
        val args = plan.asCliArgs()
        assertEquals(listOf("--newline", "u"), args)
    }
}
