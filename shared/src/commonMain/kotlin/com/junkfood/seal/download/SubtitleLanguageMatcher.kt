package com.junkfood.seal.download

/**
 * Matches yt-dlp subtitle language codes against the persisted comma-separated regex contract.
 */
class SubtitleLanguageMatcher private constructor(
    private val patterns: List<Regex>,
) {
    fun matches(languageCode: String): Boolean =
        patterns.any { pattern -> pattern.matchEntire(languageCode) != null }

    fun filter(languageCodes: Collection<String>): Set<String> =
        languageCodes.filterTo(linkedSetOf(), ::matches)

    companion object {
        fun compile(patternExpression: String): SubtitleLanguageMatcher =
            SubtitleLanguageMatcher(
                patternExpression
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .mapNotNull { pattern -> runCatching { Regex(pattern) }.getOrNull() },
            )
    }
}

fun Collection<String>.filterSubtitleLanguages(patternExpression: String): Set<String> =
    SubtitleLanguageMatcher.compile(patternExpression).filter(this)
