package com.junkfood.seal.desktop.settings.command

import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommandTemplateImportTest {
    @Test
    fun `backup and single template use explicit JSON shapes`() {
        val backup =
            decodeCommandTemplateImport(
                """{"templates":[{"id":1,"label":"A","template":"--version"}],"shortcuts":[{"option":"-v"}],"future":true}""",
            )
        assertEquals(listOf("-v"), backup.shortcuts)
        assertEquals("A", backup.templates.single().label)

        val single = decodeCommandTemplateImport("""{"label":"B","template":"--version","unknown":42}""")
        assertEquals("B", single.templates.single().label)
        assertEquals(0, single.templates.single().id)
    }

    @Test
    fun `invalid empty and unsupported imports never silently succeed`() {
        listOf("{}", "{\"templates\":[]}").forEach { text ->
            assertEquals(
                CommandImportError.Empty,
                assertFailsWith<CommandImportException> { decodeCommandTemplateImport(text) }.reason,
            )
        }
        listOf("", "{", "[]", "{\"templates\":42}").forEach { text ->
            assertEquals(
                CommandImportError.InvalidJson,
                assertFailsWith<CommandImportException> { decodeCommandTemplateImport(text) }.reason,
            )
        }
        assertEquals(
            CommandImportError.UnsupportedSchema,
            assertFailsWith<CommandImportException> {
                decodeCommandTemplateImport("""{"schemaVersion":2,"label":"A","template":"-v"}""")
            }.reason,
        )
        assertEquals(
            CommandImportError.InvalidTemplate,
            assertFailsWith<CommandImportException> {
                decodeCommandTemplateImport("""{"label":" ","template":"-v"}""")
            }.reason,
        )
    }

    @Test
    fun `duplicate IDs and names are made unique when merged`() {
        val settings =
            DesktopAppSettings(
                customCommandTemplates = listOf(DesktopCommandTemplate(5, "A", "-v")),
            )
        val imported =
            decodeCommandTemplateImport(
                """{"templates":[{"id":5,"label":"A","template":"-v"},{"id":5,"label":"A","template":"-v"}]}""",
            )

        val merged = mergeCommandTemplateImport(settings, imported)

        assertEquals(listOf(5, 6, 7), merged.customCommandTemplates.map { it.id })
        assertEquals(listOf("A", "A (2)", "A (3)"), merged.customCommandTemplates.map { it.label })
        assertEquals(6, merged.customCommandTemplateId)
    }
}
