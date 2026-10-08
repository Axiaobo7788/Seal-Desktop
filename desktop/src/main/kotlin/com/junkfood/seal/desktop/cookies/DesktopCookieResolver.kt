package com.junkfood.seal.desktop.cookies

import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.util.DownloadPreferences
import java.nio.file.Path
import kotlin.io.path.exists

class DesktopCookieResolver(
    private val cookiesFileProvider: () -> Path = DesktopYtDlpPaths::cookiesFile,
) {
    fun resolve(preferences: DownloadPreferences): DesktopCookieContext {
        val userAgent = preferences.userAgentString.trim().takeIf { it.isNotEmpty() }
        if (!preferences.cookies) return DesktopCookieContext.Disabled(userAgent)

        val cookiesFile = cookiesFileProvider()
        if (cookiesFile.exists()) {
            return if (DesktopCookiesParser.isValidCookiesFile(cookiesFile)) {
                DesktopCookieContext.CachedFile(cookiesFile, userAgent)
            } else {
                DesktopCookieContext.Unavailable(DesktopCookieUnavailableReason.InvalidCache, userAgent)
            }
        }

        val browser = SupportedBrowser.fromName(preferences.cookiesBrowser)
        return if (browser != null) {
            DesktopCookieContext.BrowserSource(
                browser = browser,
                validationUrl = "",
                targetFile = cookiesFile,
                userAgent = userAgent,
            )
        } else {
            DesktopCookieContext.Unavailable(DesktopCookieUnavailableReason.MissingSource, userAgent)
        }
    }
}
