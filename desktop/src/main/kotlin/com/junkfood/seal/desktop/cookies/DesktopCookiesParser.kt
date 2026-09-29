package com.junkfood.seal.desktop.cookies

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.exists

data class DesktopCookiesStats(
    val cookieCount: Int,
    val siteCount: Int,
)

object DesktopCookiesParser {
    fun parseStats(path: Path): DesktopCookiesStats {
        if (!path.exists()) return DesktopCookiesStats(0, 0)

        var cookieCount = 0
        val domains = linkedSetOf<String>()
        Files.newBufferedReader(path).useLines { lines ->
            lines.forEach { line ->
                parseCookieDomain(line)?.let { domain ->
                    cookieCount += 1
                    domains += domain
                }
            }
        }
        return DesktopCookiesStats(cookieCount = cookieCount, siteCount = domains.size)
    }

    fun isValidCookiesFile(path: Path): Boolean {
        if (!path.exists() || !Files.isRegularFile(path)) return false
        return runCatching {
            Files.newBufferedReader(path).useLines { lines -> lines.any { parseCookieDomain(it) != null } }
        }.getOrDefault(false)
    }

    internal fun parseCookieDomain(line: String): String? {
        if (line.isBlank()) return null

        val dataLine =
            when {
                line.startsWith(HTTP_ONLY_PREFIX) -> line.removePrefix(HTTP_ONLY_PREFIX)
                line.startsWith("#") -> return null
                else -> line
            }
        val fields = dataLine.split('\t', limit = NETSCAPE_FIELD_COUNT)
        if (fields.size < NETSCAPE_FIELD_COUNT) return null
        return fields[0]
            .trim()
            .removePrefix(".")
            .takeIf { it.isNotEmpty() }
            ?.lowercase(Locale.ROOT)
    }

    private const val HTTP_ONLY_PREFIX = "#HttpOnly_"
    private const val NETSCAPE_FIELD_COUNT = 7
}
