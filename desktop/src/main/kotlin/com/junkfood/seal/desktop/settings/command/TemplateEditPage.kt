package com.junkfood.seal.desktop.settings.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.custom_command_template
import com.junkfood.seal.shared.generated.resources.edit_shortcuts
import com.junkfood.seal.shared.generated.resources.edit_template_desc
import com.junkfood.seal.shared.generated.resources.new_template
import com.junkfood.seal.shared.generated.resources.template_label
import com.junkfood.seal.shared.generated.resources.yt_dlp_docs
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TemplateEditPage(
    templateId: Int?,
    settings: DesktopAppSettings,
    onUpdate: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val initialTemplate = remember(templateId) {
        settings.customCommandTemplates.find { it.id == templateId }
            ?: DesktopCommandTemplate(
                id = (settings.customCommandTemplates.maxOfOrNull { it.id } ?: 0) + 1,
                label = "",
                template = ""
            )
    }

    var label by remember(initialTemplate) { mutableStateOf(initialTemplate.label) }
    var templateText by remember(initialTemplate) { mutableStateOf(initialTemplate.template) }
    val uriHandler = LocalUriHandler.current
    val shortcuts = settings.customCommandShortcuts

    SettingsPageScaffold(
        title = if (initialTemplate.label.isEmpty()) stringResource(Res.string.new_template) else initialTemplate.label,
        onBack = onBack,
        actions = {
            val exists = settings.customCommandTemplates.any { it.id == initialTemplate.id }
            if (exists) {
                IconButton(onClick = {
                    val newTemplates = settings.customCommandTemplates.filter { it.id != initialTemplate.id }
                    if (newTemplates.isNotEmpty()) {
                        onUpdate {
                            val currentSelectionRemoved = it.customCommandTemplateId == initialTemplate.id
                            val activeTemplate =
                                if (currentSelectionRemoved) {
                                    newTemplates.first()
                                } else {
                                    newTemplates.find { template -> template.id == it.customCommandTemplateId }
                                        ?: newTemplates.first()
                                }
                            it.copy(
                                customCommandTemplates = newTemplates,
                                customCommandTemplateId = activeTemplate.id,
                                customCommandLabel = activeTemplate.label,
                                customCommandTemplate = activeTemplate.template
                            )
                        }
                    } else {
                        val defaultTemplate = DesktopCommandTemplate(
                            id = 1,
                            label = "yt-dlp Default",
                            template = "-f bestvideo*+bestaudio/best %(url)s"
                        )
                        onUpdate {
                            it.copy(
                                customCommandTemplates = listOf(defaultTemplate),
                                customCommandTemplateId = 1,
                                customCommandLabel = defaultTemplate.label,
                                customCommandTemplate = defaultTemplate.template
                            )
                        }
                    }
                    onBack()
                }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(
                onClick = {
                    val updatedTemplate = initialTemplate.copy(label = label, template = templateText)
                    val newTemplates = if (exists) {
                        settings.customCommandTemplates.map { if (it.id == updatedTemplate.id) updatedTemplate else it }
                    } else {
                        settings.customCommandTemplates + updatedTemplate
                    }
                    onUpdate {
                        val shouldUpdateSelection =
                            it.customCommandTemplateId == updatedTemplate.id ||
                                it.customCommandTemplates.isEmpty() ||
                                it.customCommandTemplates.none { template -> template.id == it.customCommandTemplateId }
                        if (shouldUpdateSelection) {
                            it.copy(
                                customCommandTemplates = newTemplates,
                                customCommandTemplateId = updatedTemplate.id,
                                customCommandLabel = updatedTemplate.label,
                                customCommandTemplate = updatedTemplate.template
                            )
                        } else {
                            it.copy(customCommandTemplates = newTemplates)
                        }
                    }
                    onBack()
                },
                enabled = label.isNotBlank() && templateText.isNotBlank()
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(Res.string.template_label)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = templateText,
                onValueChange = { templateText = it },
                label = { Text(stringResource(Res.string.custom_command_template)) },
                minLines = 4,
                maxLines = 10,
            )
            Text(
                text = stringResource(Res.string.edit_template_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/yt-dlp/yt-dlp#usage-and-options") },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(Res.string.yt_dlp_docs))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (shortcuts.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.edit_shortcuts),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 16.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shortcuts.forEach { item ->
                        ElevatedAssistChip(
                            onClick = {
                                templateText = templateText.run {
                                    if (isEmpty()) item
                                    else if (endsWith(" ")) this + item
                                    else "$this $item"
                                }
                            },
                            label = { Text(item) }
                        )
                    }
                }
            }
        }
    }
}
