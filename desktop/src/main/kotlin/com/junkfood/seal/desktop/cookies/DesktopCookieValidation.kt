package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataException
import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataFetcher
import java.nio.file.Path

enum class CookieValidationPreset(
    val displayName: String,
    val domainPatterns: List<String>,
) {
    Bilibili("Bilibili", listOf("bilibili.com")),
    YouTube("YouTube", listOf("youtube.com", "youtu.be")),
    Twitter("X / Twitter", listOf("x.com", "twitter.com")),
    Instagram("Instagram", listOf("instagram.com")),
    TikTok("TikTok", listOf("tiktok.com")),
    Custom("", emptyList());

    companion object {
        fun patternsForHost(host: String): List<String> =
            entries.firstOrNull { preset ->
                preset != Custom && preset.domainPatterns.any { CookieDomainMatcher.match(host, it) }
            }?.domainPatterns ?: listOf(host)
    }
}

sealed interface DesktopCookieMediaValidationResult {
    data class Success(val target: DesktopCookieValidationTarget) : DesktopCookieMediaValidationResult

    data class Failure(
        val reason: Reason,
        val diagnostic: String = "",
        val exitCode: Int? = null,
    ) : DesktopCookieMediaValidationResult

    enum class Reason {
        InvalidUrl,
        InvalidCache,
        NoMatchingCookies,
        AuthenticationRequired,
        NetworkError,
        MediaUnavailable,
        ExtractorFailed,
        UnexpectedFailure,
    }
}

class DesktopCookieMediaValidator(
    private val fetchMetadata: (String, String?, DesktopCookieContext, Map<String, String>) -> Unit =
        { url, proxy, context, environment ->
            YtDlpMetadataFetcher().fetch(url, proxy, context, environment)
        },
) {
    fun validate(
        url: String,
        cookiesFile: Path,
        userAgent: String?,
        proxyUrl: String?,
        environment: Map<String, String> = emptyMap(),
    ): DesktopCookieMediaValidationResult {
        val target =
            normalizeCookieValidationUrl(url).getOrElse {
                return DesktopCookieMediaValidationResult.Failure(DesktopCookieMediaValidationResult.Reason.InvalidUrl)
            }
        if (!DesktopCookiesParser.isValidCookiesFile(cookiesFile)) {
            return DesktopCookieMediaValidationResult.Failure(DesktopCookieMediaValidationResult.Reason.InvalidCache)
        }
        val patterns = CookieValidationPreset.patternsForHost(target.host)
        if (DesktopCookiesParser.matchDomains(cookiesFile, patterns).cookieCount == 0) {
            return DesktopCookieMediaValidationResult.Failure(DesktopCookieMediaValidationResult.Reason.NoMatchingCookies)
        }

        return try {
            fetchMetadata(
                target.url,
                proxyUrl,
                DesktopCookieContext.CachedFile(cookiesFile, userAgent),
                environment,
            )
            DesktopCookieMediaValidationResult.Success(target)
        } catch (error: YtDlpMetadataException) {
            classifyMetadataFailure(error)
        } catch (error: Exception) {
            DesktopCookieMediaValidationResult.Failure(
                DesktopCookieMediaValidationResult.Reason.UnexpectedFailure,
                sanitizeCookieDiagnostic(error.message ?: error.toString()),
            )
        }
    }
}

private fun classifyMetadataFailure(error: YtDlpMetadataException): DesktopCookieMediaValidationResult.Failure {
    val diagnostic = sanitizeCookieDiagnostic(error.stderr.ifBlank { error.stdout })
    val normalized = diagnostic.lowercase()
    val reason =
        when {
            AUTH_FAILURE_MARKERS.any(normalized::contains) ->
                DesktopCookieMediaValidationResult.Reason.AuthenticationRequired
            NETWORK_FAILURE_MARKERS.any(normalized::contains) ->
                DesktopCookieMediaValidationResult.Reason.NetworkError
            MEDIA_FAILURE_MARKERS.any(normalized::contains) ->
                DesktopCookieMediaValidationResult.Reason.MediaUnavailable
            else -> DesktopCookieMediaValidationResult.Reason.ExtractorFailed
        }
    return DesktopCookieMediaValidationResult.Failure(reason, diagnostic, error.exitCode)
}

private val AUTH_FAILURE_MARKERS =
    listOf("sign in", "login required", "authentication", "not a bot", "fresh cookies", "account is required")
private val NETWORK_FAILURE_MARKERS =
    listOf("timed out", "timeout", "connection refused", "connection reset", "network is unreachable", "unable to download")
private val MEDIA_FAILURE_MARKERS =
    listOf("video unavailable", "media unavailable", "private video", "has been removed", "not available in your country")

internal const val COOKIE_EXTRACTION_TRIGGER_URL = "https://example.com/"
