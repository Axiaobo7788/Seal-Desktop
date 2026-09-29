package com.junkfood.seal.desktop.cookies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopCookieUrlTest {
    @Test
    fun `adds https scheme and normalizes host`() {
        val target = normalizeCookieValidationUrl("  BILIBILI.COM/video  ").getOrThrow()

        assertEquals("https://BILIBILI.COM/video", target.url)
        assertEquals("bilibili.com", target.host)
    }

    @Test
    fun `preserves explicit http and https schemes`() {
        assertEquals("http://example.com/path", normalizeCookieValidationUrl("http://example.com/path").getOrThrow().url)
        assertEquals("https://example.com", normalizeCookieValidationUrl("https://example.com").getOrThrow().url)
    }

    @Test
    fun `rejects unsupported malformed and ambiguous values`() {
        listOf(
            "",
            "not-a-host",
            "ftp://example.com",
            "https://user@example.com",
            "https://example.com:99999",
            "https://bad host.com",
            "https://999.999.999.999",
            "https://bad..example.com",
        )
            .forEach { input -> assertTrue(normalizeCookieValidationUrl(input).isFailure, input) }
    }
}
