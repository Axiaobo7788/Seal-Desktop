package com.junkfood.seal.desktop.download.configure

import com.junkfood.seal.util.DownloadPreferences

internal enum class AdditionalSetting { Cookies, Subtitles, Thumbnail, Metadata, SponsorBlock }

internal fun enabledAdditionalSettings(preferences: DownloadPreferences): List<AdditionalSetting> = buildList {
    if (preferences.cookies) add(AdditionalSetting.Cookies)
    if (preferences.downloadSubtitle) add(AdditionalSetting.Subtitles)
    if (preferences.embedThumbnail) add(AdditionalSetting.Thumbnail)
    if (preferences.embedMetadata) add(AdditionalSetting.Metadata)
    if (preferences.sponsorBlock) add(AdditionalSetting.SponsorBlock)
}
