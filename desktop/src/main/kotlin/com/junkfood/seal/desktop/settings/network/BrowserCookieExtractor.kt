package com.junkfood.seal.desktop.settings.network

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
