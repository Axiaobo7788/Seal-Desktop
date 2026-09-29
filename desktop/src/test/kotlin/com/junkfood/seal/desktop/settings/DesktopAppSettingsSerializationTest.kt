package com.junkfood.seal.desktop.settings

import com.junkfood.seal.desktop.cookies.DesktopCookieCacheMetadata
import com.junkfood.seal.desktop.cookies.DesktopCookieBrowserPreference
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheSource
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopAppSettingsSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `old settings payload receives empty cookie metadata defaults`() {
        val settings = json.decodeFromString<DesktopAppSettings>("""{"languageTag":"en"}""")

        assertEquals("en", settings.languageTag)
        assertEquals(DesktopCookieBrowserPreference(), settings.cookieBrowserPreference)
        assertEquals(DesktopCookieCacheMetadata(), settings.cookieCacheMetadata)
    }

    @Test
    fun `cookie source metadata round trips without cookie values`() {
        val original =
            DesktopAppSettings(
                cookieBrowserPreference =
                    DesktopCookieBrowserPreference(
                        browserName = "firefox",
                        profileId = "native:profile-id",
                        profileName = "Default",
                    ),
                cookieCacheMetadata =
                    DesktopCookieCacheMetadata(
                        source = DesktopCookieCacheSource.Browser,
                        browserName = "firefox",
                        browserProfileId = "native:profile-id",
                        browserProfileName = "Default",
                        validationUrl = "https://example.com",
                        validationHost = "example.com",
                        generatedAtEpochMillis = 1234L,
                        lastStatus = DesktopCookieCacheStatus.Ready,
                    ),
            )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<DesktopAppSettings>(encoded)

        assertEquals(original.cookieCacheMetadata, decoded.cookieCacheMetadata)
        assertEquals(original.cookieBrowserPreference, decoded.cookieBrowserPreference)
        check(!encoded.contains("cookie-value"))
    }
}
