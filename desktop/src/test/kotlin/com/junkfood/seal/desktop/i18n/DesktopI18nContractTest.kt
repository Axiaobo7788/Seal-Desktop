package com.junkfood.seal.desktop.i18n

import com.junkfood.seal.desktop.cookies.DesktopCookieUnavailableReason
import com.junkfood.seal.desktop.download.DesktopDownloadType
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopI18nContractTest {
    @Test
    fun `literal AndroidStrings calls resolve in default resources`() {
        val sourceRoot = repositoryRoot().resolve("desktop/src/main/kotlin")
        val literalCall = Regex("""AndroidStrings\.(?:get|format)\s*\(\s*\"([A-Za-z0-9_]+)\"""")
        val keys = linkedSetOf<String>()

        Files.walk(sourceRoot).use { paths ->
            paths.filter { it.isRegularFile() && it.fileName.toString().endsWith(".kt") }
                .forEach { path -> literalCall.findAll(path.readText()).forEach { keys += it.groupValues[1] } }
        }

        val missing = keys.filterNot(AndroidStrings::hasDefaultKey)
        assertTrue(missing.isEmpty(), "Missing default Android string resources: ${missing.sorted()}")
    }

    @Test
    fun `known dynamic AndroidStrings domains resolve in default resources`() {
        val dynamicKeys =
            buildSet {
                DesktopCookieUnavailableReason.entries.forEach { add(it.stringKey) }
                DesktopDownloadType.entries.forEach { add(it.name.lowercase(Locale.ROOT)) }
                add("desktop_dependency_optional_missing")
                add("desktop_dependency_optional_broken")
            }

        val missing = dynamicKeys.filterNot(AndroidStrings::hasDefaultKey)
        assertTrue(missing.isEmpty(), "Missing dynamic Android string resources: ${missing.sorted()}")
    }

    @Test
    fun `desktop locale normalization matches resource qualifiers`() {
        assertEquals("zh-Hans", normalizeDesktopLanguageTag("zh-CN"))
        assertEquals("zh-Hans", normalizeDesktopLanguageTag("zh-SG"))
        assertEquals("zh-Hant", normalizeDesktopLanguageTag("zh-TW"))
        assertEquals("zh-Hant", normalizeDesktopLanguageTag("zh-HK"))
        assertEquals("he", normalizeDesktopLanguageTag("iw"))
        assertEquals("id", normalizeDesktopLanguageTag("in"))
        assertEquals(null, normalizeDesktopLanguageTag(null))

        assertEquals("zh", desktopResourceLocaleForTag("zh-Hans").language)
        assertEquals("CN", desktopResourceLocaleForTag("zh-Hans").region)
        assertEquals("zh", desktopResourceLocaleForTag("zh-Hant").language)
        assertEquals("TW", desktopResourceLocaleForTag("zh-Hant").region)
        assertEquals("iw", desktopResourceLocaleForTag("he").language)
        assertEquals("in", desktopResourceLocaleForTag("id").language)
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of("").toAbsolutePath().normalize()) { it.parent }
            .firstOrNull { it.resolve("desktop/src/main/kotlin").isDirectory() }
            ?: error("Could not locate repository root from ${Path.of("").toAbsolutePath()}")
}
