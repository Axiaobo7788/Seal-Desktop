package com.junkfood.seal.desktop.network

import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.util.DownloadPreferences

internal object DesktopProxyResolver {
    fun resolveProxyUrl(preferences: DownloadPreferences, appSettings: DesktopAppSettings): String? {
        if (!preferences.proxy) return null
        val auto = if (appSettings.autoProxyEnabled) DesktopProxyAutoDetector.detectXrayProxy() else null
        val manual = preferences.proxyUrl.trim().takeIf { it.isNotBlank() }
        return auto ?: manual
    }

    fun applyToPreferences(preferences: DownloadPreferences, appSettings: DesktopAppSettings): DownloadPreferences {
        val resolved = resolveProxyUrl(preferences, appSettings) ?: return preferences
        return preferences.copy(proxy = true, proxyUrl = resolved)
    }

    fun buildProxyEnvironment(proxyUrl: String?): Map<String, String> {
        val proxy = proxyUrl?.trim()?.takeIf { it.isNotBlank() } ?: return emptyMap()
        return mapOf(
            "HTTP_PROXY" to proxy,
            "HTTPS_PROXY" to proxy,
            "ALL_PROXY" to proxy,
            "http_proxy" to proxy,
            "https_proxy" to proxy,
            "all_proxy" to proxy,
        )
    }
}