package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.desktop.i18n.AndroidStrings
import java.nio.file.Path
import kotlinx.serialization.Serializable

@Serializable
enum class DesktopCookieCacheSource {
    None,
    Browser,
    ImportedFile,
}

@Serializable
enum class DesktopCookieCacheStatus {
    None,
    Ready,
    Failed,
    Cleared,
}

@Serializable
data class DesktopCookieCacheMetadata(
    val source: DesktopCookieCacheSource = DesktopCookieCacheSource.None,
    val browserName: String = "",
    val browserProfileId: String = "",
    val browserProfileName: String = "",
    val validationUrl: String = "",
    val validationHost: String = "",
    val generatedAtEpochMillis: Long = 0L,
    val lastStatus: DesktopCookieCacheStatus = DesktopCookieCacheStatus.None,
)

@Serializable
data class DesktopCookieBrowserPreference(
    val browserName: String = "",
    val profileId: String = "",
    val profileName: String = "",
)

data class DesktopCookieCacheSnapshot(
    val metadata: DesktopCookieCacheMetadata,
    val stats: DesktopCookiesStats,
    val cacheExists: Boolean,
    val cacheValid: Boolean,
)

enum class SupportedBrowser(
    val browserName: String,
    val displayName: String,
) {
    Chrome("chrome", "Google Chrome"),
    Firefox("firefox", "Firefox"),
    Edge("edge", "Microsoft Edge"),
    Chromium("chromium", "Chromium"),
    Opera("opera", "Opera"),
    Brave("brave", "Brave"),
    Vivaldi("vivaldi", "Vivaldi"),
    Safari("safari", "Safari");

    companion object {
        fun fromName(name: String): SupportedBrowser? =
            entries.find { it.browserName.equals(name, ignoreCase = true) }
    }
}

enum class DesktopCookieUnavailableReason(
    internal val stringKey: String,
) {
    MissingCache("desktop_cookies_context_missing"),
    InvalidCache("desktop_cookies_context_invalid"),
    MissingSource("desktop_cookies_context_missing"),
}

sealed interface DesktopCookieContext {
    val userAgent: String?

    data class Disabled(
        override val userAgent: String? = null,
    ) : DesktopCookieContext

    data class CachedFile(
        val path: Path,
        override val userAgent: String? = null,
    ) : DesktopCookieContext

    data class BrowserSource(
        val browser: SupportedBrowser,
        val profile: String? = null,
        val validationUrl: String,
        val targetFile: Path,
        override val userAgent: String? = null,
    ) : DesktopCookieContext

    data class Unavailable(
        val reason: DesktopCookieUnavailableReason,
        override val userAgent: String? = null,
    ) : DesktopCookieContext
}

internal fun DesktopCookieContext.BrowserSource.browserArgument(): String =
    profile?.trim()?.takeIf { it.isNotEmpty() }?.let { "${browser.browserName}:$it" }
        ?: browser.browserName

class DesktopCookieContextException(
    val reason: DesktopCookieUnavailableReason,
) : IllegalStateException(AndroidStrings.get(reason.stringKey))

internal fun DesktopCookieContext.ytDlpArguments(): List<String> =
    buildList {
        when (this@ytDlpArguments) {
            is DesktopCookieContext.Disabled -> Unit
            is DesktopCookieContext.CachedFile -> {
                add("--cookies")
                add(path.toAbsolutePath().normalize().toString())
            }
            is DesktopCookieContext.BrowserSource -> {
                throw DesktopCookieContextException(DesktopCookieUnavailableReason.MissingCache)
            }
            is DesktopCookieContext.Unavailable -> throw DesktopCookieContextException(reason)
        }

        userAgent?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
            add("--add-header")
            add("User-Agent:$value")
        }
    }
