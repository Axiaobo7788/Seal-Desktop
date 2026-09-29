package com.junkfood.seal.desktop.cookies

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopCookieCacheTest {
    @Test
    fun `import validates protects and replaces the cache`() = withCookieDirectory { directory ->
        val source = directory.resolve("source.txt")
        val target = directory.resolve("cache/cookies.txt")
        source.writeText(validCookie("example.com"))
        val protected = mutableListOf<Path>()
        val cache = DesktopCookieCache(permissionProtector = { path -> protected.add(path); null })

        val result = cache.importFrom(source, target).getOrThrow()

        assertEquals(DesktopCookiesStats(1, 1), result.stats)
        assertEquals(listOf(target), protected)
        assertTrue(target.exists())
        assertTrue(cache.inspect(target, DesktopCookieCacheMetadata()).cacheValid)
    }

    @Test
    fun `invalid import preserves an existing valid cache`() = withCookieDirectory { directory ->
        val source = directory.resolve("invalid.txt")
        val target = directory.resolve("cookies.txt")
        source.writeText("not cookies")
        target.writeText(validCookie("existing.example"))

        assertTrue(DesktopCookieCache().importFrom(source, target).isFailure)
        assertEquals(DesktopCookiesStats(1, 1), DesktopCookiesParser.parseStats(target))
        assertEquals("existing.example", DesktopCookiesParser.parseCookieDomain(target.toFile().readLines().single()))
    }

    @Test
    fun `clear deletes cache and invalidates snapshot`() = withCookieDirectory { directory ->
        val target = directory.resolve("cookies.txt")
        target.writeText(validCookie("example.com"))
        val cache = DesktopCookieCache()

        assertTrue(cache.clear(target).getOrThrow())
        assertFalse(cache.inspect(target, DesktopCookieCacheMetadata()).cacheExists)
    }

    @Test
    fun `permission helper is a no-op on non-posix systems`() {
        assertNull(restrictCookieFilePermissions(Path.of("unused"), posixSupported = false))
    }

    @Test
    fun `permission helper requests owner read and write only`() {
        var requested = emptySet<PosixFilePermission>()
        val warning =
            restrictCookieFilePermissions(Path.of("unused"), posixSupported = true) { _, permissions ->
                requested = permissions
            }

        assertNull(warning)
        assertEquals(
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
            requested,
        )
    }

    @Test
    fun `permission failure is diagnostic but non-fatal`() {
        val warning =
            restrictCookieFilePermissions(Path.of("unused"), posixSupported = true) { _, _ ->
                error("permission denied")
            }

        assertTrue(warning.orEmpty().contains("permission denied"))
    }

    private fun withCookieDirectory(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("seal-cookie-cache-test")
        try {
            block(directory)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}

internal fun validCookie(domain: String): String = "$domain\tTRUE\t/\tFALSE\t0\tname\tvalue\n"
