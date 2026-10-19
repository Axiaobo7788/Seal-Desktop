package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.toAudioFormatSorter
import com.junkfood.seal.util.toFormatSorter

/** Extraction context only; task-specific format IDs and post-processing stay in the download plan. */
data class MetadataRuntimeOptions(
    val proxyUrl: String? = null,
    val forceIpv4: Boolean = false,
    val formatSorting: String? = null,
) {
    companion object {
        fun from(preferences: DownloadPreferences, proxyUrl: String?): MetadataRuntimeOptions {
            val sorter =
                when {
                    preferences.extractAudio && preferences.convertAudio -> null
                    preferences.formatSorting && preferences.sortingFields.isNotBlank() -> preferences.sortingFields
                    preferences.extractAudio -> preferences.toAudioFormatSorter()
                    else -> preferences.toFormatSorter()
                }
            return MetadataRuntimeOptions(
                proxyUrl = proxyUrl,
                forceIpv4 = preferences.forceIpv4,
                formatSorting = sorter?.takeIf { it.isNotBlank() },
            )
        }
    }
}
