package com.junkfood.seal.desktop.cookies

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CookieDomainMatcherTest {
    @Test
    fun `normalizes exact wildcard url and case patterns`() {
        assertTrue(CookieDomainMatcher.match(".Example.COM", "example.com"))
        assertTrue(CookieDomainMatcher.match("accounts.example.com", "*.example.com"))
        assertTrue(CookieDomainMatcher.match("example.com", "https://www.example.com/path"))
        assertFalse(CookieDomainMatcher.match("notexample.com", "example.com"))
    }

    @Test
    fun `malformed domains fail closed`() {
        listOf("", "http://", "bad domain", "-example.com", "example..com", "*. ").forEach {
            assertEquals(null, CookieDomainMatcher.normalizePattern(it), it)
        }
    }

    @Test
    fun `counts matching normal and HttpOnly cookie rows without reading values`() {
        val directory = Files.createTempDirectory("seal-cookie-domain-match")
        try {
            val file = directory.resolve("cookies.txt")
            file.writeText(
                listOf(
                    "# Netscape HTTP Cookie File",
                    ".bilibili.com\tTRUE\t/\tTRUE\t0\tsess\tsecret",
                    "#HttpOnly_account.bilibili.com\tTRUE\t/\tTRUE\t0\tauth\tsecret",
                    ".youtube.com\tTRUE\t/\tTRUE\t0\tsid\tsecret",
                ).joinToString("\n", postfix = "\n"),
            )

            val result = DesktopCookiesParser.matchDomains(file, listOf("*.bilibili.com"))

            assertEquals(2, result.cookieCount)
            assertEquals(setOf("bilibili.com", "account.bilibili.com"), result.matchedDomains)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
