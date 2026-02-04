@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.customcommand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.DesktopAppSettingsState
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.custom_command_enabled_hint
import com.junkfood.seal.shared.generated.resources.custom_command_template
import com.junkfood.seal.shared.generated.resources.custom_command_template_desc
import com.junkfood.seal.shared.generated.resources.dismiss
import com.junkfood.seal.shared.generated.resources.edit
import com.junkfood.seal.shared.generated.resources.edit_template_desc
import com.junkfood.seal.shared.generated.resources.how_does_it_work
import com.junkfood.seal.shared.generated.resources.new_template
import com.junkfood.seal.shared.generated.resources.remove
import com.junkfood.seal.shared.generated.resources.remove_template
import com.junkfood.seal.shared.generated.resources.remove_template_desc
import com.junkfood.seal.shared.generated.resources.template_label
import com.junkfood.seal.shared.generated.resources.template_selection
import com.junkfood.seal.shared.generated.resources.use_custom_command
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopCustomCommandScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = false,
    appSettingsState: DesktopAppSettingsState,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
    val settings = appSettingsState.settings
    val templates = settings.customCommandTemplates
    var showHelpDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingTemplateId by remember { mutableStateOf<Int?>(null) }
    var editingLabel by remember { mutableStateOf("") }
    var editingTemplate by remember { mutableStateOf("") }
    var labelError by remember { mutableStateOf(false) }

    LaunchedEffect(
        templates,
        settings.customCommandLabel,
        settings.customCommandTemplate,
        settings.customCommandTemplateId,
    ) {
        if (
            templates.isEmpty() &&
                (settings.customCommandLabel.isNotBlank() || settings.customCommandTemplate.isNotBlank())
        ) {
            appSettingsState.update {
                it.copy(
                    customCommandTemplates =
                        listOf(
                            DesktopCommandTemplate(
                                id = 1,
                                label = settings.customCommandLabel,
                                template = settings.customCommandTemplate,
                            )
                        ),
                    customCommandTemplateId = 1,
                )
            }
        } else if (templates.isNotEmpty() && templates.none { it.id == settings.customCommandTemplateId }) {
            val first = templates.first()
            appSettingsState.update {
                it.copy(
                    customCommandTemplateId = first.id,
                    customCommandLabel = first.label,
                    customCommandTemplate = first.template,
                )
            }
        }
    }

    fun openEditDialog(template: DesktopCommandTemplate?) {
        editingTemplateId = template?.id
        editingLabel = template?.label.orEmpty()
        editingTemplate = template?.template.orEmpty()
        labelError = false
        showEditDialog = true
    }

    fun selectTemplate(template: DesktopCommandTemplate) {
        appSettingsState.update {
            it.copy(
                customCommandTemplateId = template.id,
                customCommandLabel = template.label,
                customCommandTemplate = template.template,
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.custom_command)) },
                navigationIcon = {
                    if (isCompact) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = stringResource(Res.string.custom_command),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(Res.string.how_does_it_work),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreferenceInfo(text = stringResource(Res.string.custom_command_desc))

            ToggleCard(
                title = stringResource(Res.string.use_custom_command),
                description = stringResource(Res.string.custom_command_enabled_hint),
                icon = Icons.Rounded.Terminal,
                checked = settings.customCommandEnabled,
            ) { checked -> appSettingsState.update { it.copy(customCommandEnabled = checked) } }

            PreferenceSubtitle(text = stringResource(Res.string.template_selection))
            PreferenceInfo(text = stringResource(Res.string.custom_command_template_desc))

            if (templates.isNotEmpty()) {
                templates.forEach { template ->
                    DesktopTemplateItem(
                        template = template,
                        selected = template.id == settings.customCommandTemplateId,
                        onSelect = { selectTemplate(template) },
                        onEdit = { openEditDialog(template) },
                        onDelete = {
                            editingTemplateId = template.id
                            showDeleteDialog = true
                        },
                    )
                }
            }

            SelectionCard(
                title = stringResource(Res.string.new_template),
                description = stringResource(Res.string.custom_command_template_desc),
                icon = Icons.Outlined.Add,
                onClick = { openEditDialog(null) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
            title = { Text(stringResource(Res.string.custom_command)) },
            text = { Text(stringResource(Res.string.custom_command_desc)) },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(Res.string.dismiss))
                }
            },
        )
    }

    if (showEditDialog) {
        val isNewTemplate = editingTemplateId == null
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            icon = {
                Icon(
                    imageVector = if (isNewTemplate) Icons.Outlined.Add else Icons.Outlined.Edit,
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(if (isNewTemplate) Res.string.new_template else Res.string.edit)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingLabel.isBlank()) {
                            labelError = true
                            return@Button
                        }
                        appSettingsState.update { current ->
                            val currentTemplates = current.customCommandTemplates
                            val existingId = editingTemplateId
                            val updatedTemplates =
                                if (existingId == null) {
                                    val newId = (currentTemplates.maxOfOrNull { it.id } ?: 0) + 1
                                    currentTemplates +
                                        DesktopCommandTemplate(
                                            id = newId,
                                            label = editingLabel.trim(),
                                            template = editingTemplate,
                                        )
                                } else {
                                    currentTemplates.map {
                                        if (it.id == existingId)
                                            it.copy(label = editingLabel.trim(), template = editingTemplate)
                                        else it
                                    }
                                }
                            val shouldSelectNew = existingId == null && current.customCommandTemplateId == 0
                            val resolvedSelectedId =
                                when {
                                    shouldSelectNew -> updatedTemplates.last().id
                                    existingId != null && current.customCommandTemplateId == existingId -> existingId
                                    updatedTemplates.any { it.id == current.customCommandTemplateId } ->
                                        current.customCommandTemplateId
                                    else -> updatedTemplates.firstOrNull()?.id ?: 0
                                }
                            val resolvedTemplate =
                                updatedTemplates.firstOrNull { it.id == resolvedSelectedId }
                            current.copy(
                                customCommandTemplates = updatedTemplates,
                                customCommandTemplateId = resolvedSelectedId,
                                customCommandLabel = resolvedTemplate?.label.orEmpty(),
                                customCommandTemplate = resolvedTemplate?.template.orEmpty(),
                            )
                        }
                        showEditDialog = false
                    },
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(Res.string.dismiss))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.edit_template_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = editingLabel,
                        onValueChange = {
                            editingLabel = it
                            labelError = false
                        },
                        isError = labelError,
                        label = { Text(stringResource(Res.string.template_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions.Default,
                    )
                    OutlinedTextField(
                        value = editingTemplate,
                        onValueChange = { editingTemplate = it },
                        label = { Text(stringResource(Res.string.custom_command_template)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 12,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            },
        )
    }

    if (showDeleteDialog) {
        val template = templates.firstOrNull { it.id == editingTemplateId }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(Res.string.remove_template)) },
            text = {
                Text(
                    stringResource(Res.string.remove_template_desc).format(template?.label.orEmpty())
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        template?.let { toDelete ->
                            appSettingsState.update { current ->
                                val updatedTemplates =
                                    current.customCommandTemplates.filterNot { it.id == toDelete.id }
                                val resolvedSelectedId =
                                    if (current.customCommandTemplateId == toDelete.id)
                                        updatedTemplates.firstOrNull()?.id ?: 0
                                    else current.customCommandTemplateId
                                val resolvedTemplate =
                                    updatedTemplates.firstOrNull { it.id == resolvedSelectedId }
                                current.copy(
                                    customCommandTemplates = updatedTemplates,
                                    customCommandTemplateId = resolvedSelectedId,
                                    customCommandLabel = resolvedTemplate?.label.orEmpty(),
                                    customCommandTemplate = resolvedTemplate?.template.orEmpty(),
                                    customCommandEnabled =
                                        if (updatedTemplates.isEmpty()) false else current.customCommandEnabled,
                                )
                            }
                        }
                        showDeleteDialog = false
                    },
                ) {
                    Text(stringResource(Res.string.remove))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.dismiss))
                }
            },
        )
    }
}

@Composable
private fun DesktopTemplateItem(
    template: DesktopCommandTemplate,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 1.dp,
        onClick = onSelect,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = template.label, style = MaterialTheme.typography.titleMedium, color = contentColor)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = template.template,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(Res.string.edit),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.remove),
                )
            }
        }
    }
}
