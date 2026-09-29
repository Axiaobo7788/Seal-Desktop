package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.util.DownloadPreferences
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopCookieResolverTest {
    @Test
    fun `disabled context keeps user agent without cookies arguments`() {
        val context =
            DesktopCookieResolver { Files.createTempDirectory("unused").resolve("cookies.txt") }
                .resolve(DownloadPreferences.EMPTY.copy(cookies = false, userAgentString = " agent "))

        assertIs<DesktopCookieContext.Disabled>(context)
        assertEquals(listOf("--add-header", "User-Agent:agent"), context.ytDlpArguments())
    }

    @Test
    fun `valid cache resolves to cached file arguments`() {
        val directory = Files.createTempDirectory("seal-cookie-resolver-test")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText(validCookie("example.com"))
            val context =
                DesktopCookieResolver { file }.resolve(
                    DownloadPreferences.EMPTY.copy(cookies = true, userAgentString = "UA"),
                )

            assertIs<DesktopCookieContext.CachedFile>(context)
            assertEquals(
                listOf("--cookies", file.toAbsolutePath().normalize().toString(), "--add-header", "User-Agent:UA"),
                context.ytDlpArguments(),
            )
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `missing cache with selected browser requires materialization`() {
        val file = Files.createTempDirectory("seal-cookie-source-test").resolve("cookies.txt")
        val context =
            DesktopCookieResolver { file }.resolve(
                DownloadPreferences.EMPTY.copy(cookies = true, cookiesBrowser = "firefox"),
            )

        assertIs<DesktopCookieContext.BrowserSource>(context)
        assertEquals(SupportedBrowser.Firefox, context.browser)
        assertFailsWith<DesktopCookieContextException> { context.ytDlpArguments() }
    }

    @Test
    fun `invalid cache fails closed instead of falling back to browser`() {
        val directory = Files.createTempDirectory("seal-cookie-invalid-test")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText("broken")
            val context =
                DesktopCookieResolver { file }.resolve(
                    DownloadPreferences.EMPTY.copy(cookies = true, cookiesBrowser = "chrome"),
                )

            assertIs<DesktopCookieContext.Unavailable>(context)
            assertEquals(DesktopCookieUnavailableReason.InvalidCache, context.reason)
            assertTrue(runCatching { context.ytDlpArguments() }.isFailure)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
