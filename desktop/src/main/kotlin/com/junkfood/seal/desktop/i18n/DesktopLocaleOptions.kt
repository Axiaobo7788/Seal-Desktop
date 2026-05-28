package com.junkfood.seal.desktop.i18n

import java.util.Locale

internal data class DesktopLocaleOption(
    val displayLocale: Locale,
    val persistedTag: String,
    val resourceLanguageOverride: String? = null,
) {
    fun matchesTag(tag: String?): Boolean {
        val normalized = normalizeDesktopLanguageTag(tag)
        return normalized != null && normalized == persistedTag
    }
}

internal data class DesktopResourceLocale(
    val locale: Locale,
    val language: String,
    val region: String?,
)

internal fun supportedDesktopLocaleOptions(): List<DesktopLocaleOption> =
    listOf(
        localeOption("ar"),
        localeOption("az"),
        localeOption("eu"),
        localeOption("be"),
        localeOption("bn"),
        localeOption("ca"),
        localeOption("zh-Hans"),
        localeOption("zh-Hant"),
        localeOption("hr"),
        localeOption("cs"),
        localeOption("da"),
        localeOption("nl"),
        localeOption("en-US"),
        localeOption("fil"),
        localeOption("fr"),
        localeOption("de"),
        localeOption("el"),
        localeOption("he", resourceLanguageOverride = "iw"),
        localeOption("hi"),
        localeOption("hu"),
        localeOption("id", resourceLanguageOverride = "in"),
        localeOption("it"),
        localeOption("ja"),
        localeOption("kn"),
        localeOption("km"),
        localeOption("ko"),
        localeOption("ms"),
        localeOption("ml"),
        localeOption("mn"),
        localeOption("nb"),
        localeOption("nn"),
        localeOption("fa"),
        localeOption("pl"),
        localeOption("pt"),
        localeOption("pt-PT"),
        localeOption("pt-BR"),
        localeOption("pa"),
        localeOption("ru"),
        localeOption("sr"),
        localeOption("si"),
        localeOption("es"),
        localeOption("sv"),
        localeOption("ta"),
        localeOption("th"),
        localeOption("tr"),
        localeOption("uk"),
        localeOption("vi"),
    )

internal fun findDesktopLocaleOption(tag: String?): DesktopLocaleOption? {
    val normalized = normalizeDesktopLanguageTag(tag) ?: return null
    return supportedDesktopLocaleOptions().firstOrNull { it.persistedTag == normalized }
}

internal fun normalizeDesktopLanguageTag(tag: String?): String? {
    val trimmed = tag?.takeIf { it.isNotBlank() } ?: return null
    val locale = Locale.forLanguageTag(trimmed)
    if (locale.language.isBlank()) return null
    return when (locale.language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            locale.script.equals("Hans", ignoreCase = true) -> "zh-Hans"
            locale.script.equals("Hant", ignoreCase = true) -> "zh-Hant"
            locale.country.uppercase(Locale.ROOT) in setOf("CN", "SG") -> "zh-Hans"
            locale.country.uppercase(Locale.ROOT) in setOf("TW", "HK", "MO") -> "zh-Hant"
            else -> locale.toLanguageTag()
        }
        "iw", "he" -> "he"
        "in", "id" -> "id"
        else -> locale.toLanguageTag()
    }
}

internal fun desktopResourceLocaleForTag(tag: String): DesktopResourceLocale {
    val normalizedTag = normalizeDesktopLanguageTag(tag) ?: tag
    val option = findDesktopLocaleOption(normalizedTag)
    val locale = option?.displayLocale ?: Locale.forLanguageTag(normalizedTag)
    val language = option?.resourceLanguageOverride ?: locale.language
    val region = locale.country.ifBlank {
        when {
            locale.script.equals("Hans", ignoreCase = true) -> "CN"
            locale.script.equals("Hant", ignoreCase = true) -> "TW"
            else -> ""
        }
    }.takeIf { it.isNotBlank() }

    return DesktopResourceLocale(locale = locale, language = language, region = region)
}

internal fun Locale.matchesLanguageAndScript(other: Locale): Boolean {
    if (!language.equals(other.language, ignoreCase = true)) return false

    val script = inferredScript()
    val otherScript = other.inferredScript()

    return script.isBlank() ||
        otherScript.isBlank() ||
        script.equals(otherScript, ignoreCase = true)
}

private fun localeOption(
    tag: String,
    resourceLanguageOverride: String? = null,
): DesktopLocaleOption {
    val locale = Locale.forLanguageTag(tag)
    return DesktopLocaleOption(
        displayLocale = locale,
        persistedTag = normalizeDesktopLanguageTag(tag) ?: tag,
        resourceLanguageOverride = resourceLanguageOverride,
    )
}

private fun Locale.inferredScript(): String {
    val explicitScript = script
    if (explicitScript.isNotBlank()) return explicitScript
    if (!language.equals("zh", ignoreCase = true)) return ""

    return when (country.uppercase(Locale.ROOT)) {
        "CN", "SG" -> "Hans"
        "TW", "HK", "MO" -> "Hant"
        else -> ""
    }
}
