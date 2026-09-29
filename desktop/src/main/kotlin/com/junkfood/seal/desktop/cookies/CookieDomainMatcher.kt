package com.junkfood.seal.desktop.cookies

import java.net.IDN
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.exists

data class CookieDomainMatch(
    val requestedPatterns: List<String>,
    val cookieCount: Int,
    val matchedDomains: Set<String>,
)

object CookieDomainMatcher {
    fun match(cookieDomain: String, domainPattern: String): Boolean {
        val cookie = normalizeDomain(cookieDomain) ?: return false
        val target = normalizePattern(domainPattern) ?: return false
        return cookie == target || cookie.endsWith(".$target") || target.endsWith(".$cookie")
    }

    fun normalizePattern(value: String): String? {
        val trimmed = value.trim().removePrefix("*.").removePrefix(".")
        val host =
            if (trimmed.contains("://")) {
                runCatching { URI(trimmed).host }.getOrNull()
            } else {
                trimmed.substringBefore('/').substringBefore(':')
            }
        return normalizeDomain(host.orEmpty())
    }

    internal fun normalizeDomain(value: String): String? {
        val normalized =
            runCatching {
                IDN.toASCII(value.trim().removePrefix(".").lowercase(Locale.ROOT))
                    .lowercase(Locale.ROOT)
            }.getOrNull() ?: return null
        if (normalized.isEmpty() || normalized.length > 253 || normalized.any { it.isWhitespace() }) return null
        val labels = normalized.split('.')
        if (labels.any { label -> label.isEmpty() || label.length > 63 || !DOMAIN_LABEL.matches(label) }) return null
        return normalized
    }

    private val DOMAIN_LABEL = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")
}

fun DesktopCookiesParser.matchDomains(path: Path, patterns: List<String>): CookieDomainMatch {
    val normalizedPatterns = patterns.mapNotNull(CookieDomainMatcher::normalizePattern).distinct()
    if (!path.exists() || normalizedPatterns.isEmpty()) {
        return CookieDomainMatch(normalizedPatterns, 0, emptySet())
    }

    var cookieCount = 0
    val domains = linkedSetOf<String>()
    Files.newBufferedReader(path).useLines { lines ->
        lines.forEach { line ->
            parseCookieDomain(line)?.let { domain ->
                if (normalizedPatterns.any { CookieDomainMatcher.match(domain, it) }) {
                    cookieCount += 1
                    domains += domain
                }
            }
        }
    }
    return CookieDomainMatch(normalizedPatterns, cookieCount, domains)
}
