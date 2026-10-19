package com.junkfood.seal.desktop.download.configure

import com.junkfood.seal.util.DownloadPreferences
import kotlin.test.*

class AdditionalSettingsSummaryTest {
    private val disabled = DownloadPreferences.EMPTY.copy(cookies = false, downloadSubtitle = false, embedThumbnail = false, embedMetadata = false, sponsorBlock = false)

    @Test fun `no enabled settings has no summary`() { assertTrue(enabledAdditionalSettings(disabled).isEmpty()) }

    @Test fun `only enabled options in stable order without changing preferences`() {
        val preferences = disabled.copy(cookies = true, downloadSubtitle = true, embedThumbnail = true, embedMetadata = true, sponsorBlock = true)
        assertEquals(AdditionalSetting.entries, enabledAdditionalSettings(preferences))
        assertEquals(listOf(AdditionalSetting.Cookies, AdditionalSetting.Metadata), enabledAdditionalSettings(disabled.copy(cookies = true, embedMetadata = true)))
        assertEquals(preferences, preferences.copy())
    }

    @Test fun `summary follows existing chip semantics rather than redefining flags`() {
        assertTrue(enabledAdditionalSettings(disabled.copy(createThumbnail = true, autoSubtitle = true)).isEmpty())
        assertEquals(listOf(AdditionalSetting.SponsorBlock), enabledAdditionalSettings(disabled.copy(sponsorBlock = true, sponsorBlockCategory = "")))
    }
}
