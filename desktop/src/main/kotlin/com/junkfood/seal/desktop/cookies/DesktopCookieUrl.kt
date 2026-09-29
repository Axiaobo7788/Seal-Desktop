package com.junkfood.seal.desktop.cookies

import java.net.URI
import java.util.Locale

data class DesktopCookieValidationTarget(
    val url: String,
    val host: String,
)

fun normalizeCookieValidationUrl(input: String): Result<DesktopCookieValidationTarget> =
    runCatching {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty())
        require(trimmed.none { it.isWhitespace() })

        val candidate = if (SCHEME_PATTERN.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val uri = URI(candidate)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        require(scheme == "http" || scheme == "https")
        require(uri.userInfo == null)
        val host = uri.host?.trim()?.lowercase(Locale.ROOT).orEmpty()
        require(host.isValidCookieHost())
        require(uri.port == -1 || uri.port in 1..65535)

        DesktopCookieValidationTarget(url = uri.toASCIIString(), host = host)
    }

private fun String.isValidCookieHost(): Boolean =
    isNotEmpty() &&
        (
            equals("localhost", ignoreCase = true) ||
                isValidDomainName() ||
                isValidIpv4Address() ||
                contains(':')
        )

private fun String.isValidDomainName(): Boolean {
    if (!contains('.')) return false
    return split('.').all { label ->
        label.isNotEmpty() &&
            label.length <= 63 &&
            DOMAIN_LABEL_PATTERN.matches(label) &&
            !label.startsWith('-') &&
            !label.endsWith('-')
    }
}

private fun String.isValidIpv4Address(): Boolean {
    val parts = split('.')
    return parts.size == 4 && parts.all { part ->
        part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) && part.toInt() in 0..255
    }
}

private val SCHEME_PATTERN = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
private val DOMAIN_LABEL_PATTERN = Regex("^[A-Za-z0-9-]+$")
