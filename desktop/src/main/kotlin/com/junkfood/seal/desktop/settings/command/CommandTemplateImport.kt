package com.junkfood.seal.desktop.settings.command

import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

@Serializable
internal data class DesktopOptionShortcutBackup(
    val id: Long = 0,
    val option: String,
)

@Serializable
internal data class DesktopCommandBackup(
    val templates: List<DesktopCommandTemplate>? = null,
    val shortcuts: List<DesktopOptionShortcutBackup>? = null,
)

internal data class CommandTemplateImport(
    val templates: List<DesktopCommandTemplate>,
    val shortcuts: List<String>,
)

internal enum class CommandImportError {
    InvalidJson,
    UnsupportedSchema,
    Empty,
    InvalidTemplate,
}

internal class CommandImportException(
    val reason: CommandImportError,
) : IllegalArgumentException(reason.name)

private val commandImportJson = Json { ignoreUnknownKeys = true }

internal fun decodeCommandTemplateImport(text: String): CommandTemplateImport {
    val root =
        runCatching { commandImportJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: throw CommandImportException(CommandImportError.InvalidJson)

    listOf("version", "schemaVersion").forEach { key ->
        if (key in root && (root[key] as? JsonPrimitive)?.intOrNull != 1) {
            throw CommandImportException(CommandImportError.UnsupportedSchema)
        }
    }

    val imported =
        try {
            when {
                "templates" in root || "shortcuts" in root -> {
                    val backup = commandImportJson.decodeFromJsonElement<DesktopCommandBackup>(root)
                    CommandTemplateImport(
                        templates = backup.templates.orEmpty(),
                        shortcuts = backup.shortcuts.orEmpty().map { it.option }.filter { it.isNotBlank() }.distinct(),
                    )
                }
                "label" in root && "template" in root -> {
                    val normalized = JsonObject(root + ("id" to (root["id"] ?: JsonPrimitive(0))))
                    CommandTemplateImport(
                        templates = listOf(commandImportJson.decodeFromJsonElement<DesktopCommandTemplate>(normalized)),
                        shortcuts = emptyList(),
                    )
                }
                else -> throw CommandImportException(CommandImportError.Empty)
            }
        } catch (error: CommandImportException) {
            throw error
        } catch (_: Exception) {
            throw CommandImportException(CommandImportError.InvalidJson)
        }

    if (imported.templates.isEmpty() && imported.shortcuts.isEmpty()) {
        throw CommandImportException(CommandImportError.Empty)
    }
    if (imported.templates.any { it.label.isBlank() || it.template.isBlank() }) {
        throw CommandImportException(CommandImportError.InvalidTemplate)
    }
    return imported
}

internal fun mergeCommandTemplateImport(
    settings: DesktopAppSettings,
    imported: CommandTemplateImport,
): DesktopAppSettings {
    var id = settings.customCommandTemplates.maxOfOrNull { it.id } ?: 0
    val names = settings.customCommandTemplates.mapTo(mutableSetOf()) { it.label }
    val templates =
        imported.templates.map { template ->
            val base = template.label.trim()
            var label = base
            var suffix = 2
            while (!names.add(label)) label = "$base (${suffix++})"
            id = Math.incrementExact(id)
            template.copy(id = id, label = label)
        }
    val selection = templates.firstOrNull()
    return settings.copy(
        customCommandTemplates = settings.customCommandTemplates + templates,
        customCommandShortcuts = (settings.customCommandShortcuts + imported.shortcuts).distinct(),
        customCommandTemplateId = selection?.id ?: settings.customCommandTemplateId,
        customCommandLabel = selection?.label ?: settings.customCommandLabel,
        customCommandTemplate = selection?.template ?: settings.customCommandTemplate,
    )
}
