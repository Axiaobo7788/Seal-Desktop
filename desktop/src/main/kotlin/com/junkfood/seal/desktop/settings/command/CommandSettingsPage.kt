package com.junkfood.seal.desktop.settings.command

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.ActionCard
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import com.junkfood.seal.desktop.settings.PreferenceSwitchWithContainer
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.edit_shortcuts
import com.junkfood.seal.shared.generated.resources.edit_shortcuts_desc
import com.junkfood.seal.shared.generated.resources.edit_template_desc
import com.junkfood.seal.shared.generated.resources.export_to_clipboard
import com.junkfood.seal.shared.generated.resources.import_from_clipboard
import com.junkfood.seal.shared.generated.resources.new_template
import com.junkfood.seal.shared.generated.resources.template_label
import com.junkfood.seal.shared.generated.resources.use_custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_template
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommandSettingsPage(
    settings: DesktopAppSettings,
    onUpdate: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
    onNavigateToEdit: (Int?) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    var showShortcutsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(
        settings.customCommandTemplates,
        settings.customCommandLabel,
        settings.customCommandTemplate,
    ) {
        if (settings.customCommandTemplates.isEmpty()) {
            val hasLegacy = settings.customCommandLabel.isNotBlank() || settings.customCommandTemplate.isNotBlank()
            val template = if (hasLegacy) {
                DesktopCommandTemplate(1, settings.customCommandLabel.ifEmpty { "Default" }, settings.customCommandTemplate)
            } else {
                DesktopCommandTemplate(1, "yt-dlp Default", "-f bestvideo*+bestaudio/best %(url)s")
            }
            onUpdate {
                it.copy(
                    customCommandTemplates = listOf(template),
                    customCommandTemplateId = 1,
                    customCommandLabel = template.label,
                    customCommandTemplate = template.template
                )
            }
        }
    }

    SettingsPageScaffold(
        title = stringResource(Res.string.custom_command),
        onBack = onBack,
        actions = {
            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.export_to_clipboard)) },
                    onClick = {
                        val activeTemplate = settings.customCommandTemplates.find { it.id == settings.customCommandTemplateId }
                        if (activeTemplate != null) {
                            val json = Json.encodeToString(activeTemplate)
                            clipboardManager.setText(AnnotatedString(json))
                        }
                        isMenuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Outlined.AssignmentReturn, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.import_from_clipboard)) },
                    onClick = {
                        clipboardManager.getText()?.text?.let { text ->
                            runCatching {
                                val template = Json.decodeFromString<DesktopCommandTemplate>(text)
                                val newId = (settings.customCommandTemplates.maxOfOrNull { it.id } ?: 0) + 1
                                val newTemplate = template.copy(id = newId)
                                onUpdate {
                                    it.copy(
                                        customCommandTemplates = it.customCommandTemplates + newTemplate,
                                        customCommandTemplateId = newId,
                                        customCommandLabel = newTemplate.label,
                                        customCommandTemplate = newTemplate.template
                                    )
                                }
                            }
                        }
                        isMenuExpanded = false
                    },
                    leadingIcon = { Icon(Icons.Outlined.ContentPasteGo, contentDescription = null) }
                )
            }
        }
    ) {
        PreferenceSwitchWithContainer(
            title = stringResource(Res.string.use_custom_command),
            description = null,
            icon = null,
            checked = settings.customCommandEnabled,
            onCheckedChange = { checked -> onUpdate { it.copy(customCommandEnabled = checked) } }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

        settings.customCommandTemplates.forEach { template ->
            CommandTemplateItemVariant(
                title = template.label,
                desc = template.template,
                selected = settings.customCommandTemplateId == template.id,
                onClick = { onNavigateToEdit(template.id) },
                onSelect = {
                    onUpdate {
                        it.copy(
                            customCommandTemplateId = template.id,
                            customCommandLabel = template.label,
                            customCommandTemplate = template.template
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ActionCard(
            title = stringResource(Res.string.new_template),
            description = null,
            icon = Icons.Outlined.Add,
            onClick = { onNavigateToEdit(null) }
        )

        ActionCard(
            title = stringResource(Res.string.edit_shortcuts),
            description = null,
            icon = Icons.Outlined.BookmarkAdd,
            onClick = { showShortcutsDialog = true }
        )
    }

    if (showShortcutsDialog) {
        CommandShortcutsDialog(
            visible = true,
            shortcuts = settings.customCommandShortcuts,
            onDismissRequest = { showShortcutsDialog = false },
            onUpdateShortcuts = { updatedList ->
                onUpdate { it.copy(customCommandShortcuts = updatedList) }
            }
        )
    }
}

@Composable
internal fun CommandTemplateItemVariant(
    title: String,
    desc: String?,
    selected: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (desc != null) {
                    Text(
                        text = desc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                RadioButton(
                    modifier = Modifier.semantics { contentDescription = title },
                    selected = selected,
                    onClick = onSelect,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommandShortcutsDialog(
    visible: Boolean,
    shortcuts: List<String>,
    onDismissRequest: () -> Unit,
    onUpdateShortcuts: (List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.BookmarkAdd, null) },
        title = { Text(stringResource(Res.string.edit_shortcuts)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.edit_shortcuts_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shortcuts.forEach { item ->
                            InputChip(
                                selected = false,
                                onClick = { onUpdateShortcuts(shortcuts.filter { it != item }) },
                                label = { Text(item) },
                                trailingIcon = { Icon(Icons.Outlined.Edit, null) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(Res.string.edit_shortcuts)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val newShortcut = text.trim()
                                if (newShortcut.isNotEmpty() && !shortcuts.contains(newShortcut)) {
                                    onUpdateShortcuts(shortcuts + newShortcut)
                                    text = ""
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Add, null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = null
    )
}
