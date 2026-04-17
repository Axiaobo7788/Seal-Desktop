@file:OptIn(ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.customcommand

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.DesktopAppSettingsState
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import com.junkfood.seal.desktop.settings.rememberDesktopSettingsState
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.custom_command_template
import com.junkfood.seal.shared.generated.resources.edit
import com.junkfood.seal.shared.generated.resources.edit_template
import com.junkfood.seal.shared.generated.resources.edit_template_desc
import com.junkfood.seal.shared.generated.resources.new_task
import com.junkfood.seal.shared.generated.resources.new_template
import com.junkfood.seal.shared.generated.resources.proceed
import com.junkfood.seal.shared.generated.resources.paste_msg
import com.junkfood.seal.shared.generated.resources.running_tasks
import com.junkfood.seal.shared.generated.resources.start
import com.junkfood.seal.shared.generated.resources.status_canceled
import com.junkfood.seal.shared.generated.resources.status_completed
import com.junkfood.seal.shared.generated.resources.status_downloading
import com.junkfood.seal.shared.generated.resources.status_error
import com.junkfood.seal.shared.generated.resources.template_label
import com.junkfood.seal.shared.generated.resources.template_selection
import com.junkfood.seal.shared.generated.resources.video_url
import com.junkfood.seal.shared.generated.resources.yt_dlp_docs
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopCustomCommandScreen(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onMenuClick: () -> Unit = {},
    appSettingsState: DesktopAppSettingsState,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val settingsState = rememberDesktopSettingsState()
    val tasks = DesktopCustomCommandTaskManager.tasks

    var showNewTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.running_tasks)) },
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
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.new_task))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                DesktopCustomCommandTaskItem(
                    task = task,
                    onCancel = { DesktopCustomCommandTaskManager.cancel(task.id) }
                )
            }
        }
    }

    if (showNewTaskDialog) {
        NewDownloadTaskDialog(
            visible = showNewTaskDialog,
            onDismiss = { showNewTaskDialog = false },
            appSettingsState = appSettingsState,
            onStart = { url, template ->
                DesktopCustomCommandTaskManager.start(
                    urlInput = url,
                    template = template,
                    preferences = settingsState.preferences
                )
            }
        )
    }
}

@Composable
private fun DesktopCustomCommandTaskItem(
    task: DesktopCustomCommandTask,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = task.templateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = task.urlInput,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusTextStr = when (task.status) {
                    DesktopCustomCommandTaskStatus.Running -> stringResource(Res.string.status_downloading)
                    DesktopCustomCommandTaskStatus.Completed -> stringResource(Res.string.status_completed)
                    DesktopCustomCommandTaskStatus.Canceled -> stringResource(Res.string.status_canceled)
                    DesktopCustomCommandTaskStatus.Error -> stringResource(Res.string.status_error)
                }

                if (task.status == DesktopCustomCommandTaskStatus.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = buildString {
                        append(statusTextStr)
                        if (task.progress != null && task.status == DesktopCustomCommandTaskStatus.Running) {
                            append(" (").append(task.progress).append("%)")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                if (task.status == DesktopCustomCommandTaskStatus.Running) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = stringResource(Res.string.cancel),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewDownloadTaskDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    appSettingsState: DesktopAppSettingsState,
    onStart: (String, DesktopCommandTemplate) -> Unit
) {
    val templates = appSettingsState.settings.customCommandTemplates
    var selectedTemplateId by remember(visible, appSettingsState.settings.customCommandTemplateId) {
        mutableStateOf(appSettingsState.settings.customCommandTemplateId)
    }
    
    val selectedTemplate = templates.find { it.id == selectedTemplateId } ?: templates.firstOrNull()

    var url by remember(visible) { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    var showSelectionDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showNewTemplateDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(
                    stringResource(Res.string.new_task),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(Res.string.custom_command_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.video_url)) },
                    singleLine = true,
                    trailingIcon = {
                        androidx.compose.material3.IconButton(onClick = {
                            clipboardManager?.getText()?.text?.let { url = it }
                        }) {
                            Icon(Icons.Outlined.ContentPasteGo, null)
                        }
                    }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ElevatedAssistChip(
                        onClick = { showSelectionDialog = true },
                        label = { Text(stringResource(Res.string.custom_command_template)) },
                        leadingIcon = { Icon(Icons.Rounded.Code, null, modifier = Modifier.size(18.dp)) }
                    )

                    if (selectedTemplate != null) {
                        ElevatedAssistChip(
                            onClick = { showEditDialog = true },
                            label = { Text(stringResource(Res.string.edit_template, selectedTemplate.label)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    ElevatedAssistChip(
                        onClick = { showNewTemplateDialog = true },
                        label = { Text(stringResource(Res.string.new_template)) },
                        leadingIcon = { Icon(Icons.Outlined.BookmarkAdd, null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.material3.OutlinedButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.cancel))
                        }
                        Button(
                            onClick = {
                                if (url.isNotBlank() && selectedTemplate != null) {
                                    onStart(url, selectedTemplate)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                                }
                            }, 
                            enabled = url.isNotBlank() && selectedTemplate != null
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.start))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    TemplateSelectionDialog(
        visible = showSelectionDialog,
        onDismiss = { showSelectionDialog = false },
        templates = templates,
        selectedId = selectedTemplate?.id ?: -1,
        onSelect = { id ->
            appSettingsState.update { it.copy(customCommandTemplateId = id) }
            selectedTemplateId = id
        }
    )

    TemplateEditDialog(
        visible = showEditDialog && selectedTemplate != null,
        onDismiss = { showEditDialog = false },
        initialTemplate = selectedTemplate,
        onSave = { label, templateText ->
            if (selectedTemplate != null) {
                val newTemplates = templates.map {
                    if (it.id == selectedTemplate.id) it.copy(label = label, template = templateText) else it
                }
                appSettingsState.update { it.copy(customCommandTemplates = newTemplates) }
            }
        }
    )

    TemplateEditDialog(
        visible = showNewTemplateDialog,
        onDismiss = { showNewTemplateDialog = false },
        initialTemplate = null,
        onSave = { label, templateText ->
            val newId = (templates.maxOfOrNull { it.id } ?: 0) + 1
            val newTemplate = DesktopCommandTemplate(newId, label, templateText)
            val newTemplates = templates + newTemplate
            appSettingsState.update {
                it.copy(
                    customCommandTemplates = newTemplates,
                    customCommandTemplateId = newId
                )
            }
            selectedTemplateId = newId
        }
    )
}

@Composable
private fun TemplateSelectionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    templates: List<DesktopCommandTemplate>,
    selectedId: Int,
    onSelect: (Int) -> Unit
) {
    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Code, null) },
        title = { Text(stringResource(Res.string.template_selection)) },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(templates, key = { it.id }) { template ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = template.id == selectedId,
                                onClick = {
                                    onSelect(template.id)
                                    onDismiss()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = template.id == selectedId,
                            onClick = null
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(template.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                template.template,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {},
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun TemplateEditDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    initialTemplate: DesktopCommandTemplate?,
    onSave: (label: String, template: String) -> Unit
) {
    var label by remember(visible) { mutableStateOf(initialTemplate?.label ?: "") }
    var templateText by remember(visible) { mutableStateOf(initialTemplate?.template ?: "") }
    val uriHandler = LocalUriHandler.current

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        icon = { Icon(if (initialTemplate != null) Icons.Outlined.Edit else Icons.Outlined.BookmarkAdd, null) },
        title = {
            Text(stringResource(if (initialTemplate != null) Res.string.edit else Res.string.new_template))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(Res.string.edit_template_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(Res.string.template_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = templateText,
                    onValueChange = { templateText = it },
                    label = { Text(stringResource(Res.string.custom_command_template)) },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    trailingIcon = {
                        if (templateText.isNotEmpty()) {
                            IconButton(onClick = { templateText = "" }) {
                                Icon(Icons.Outlined.Cancel, null)
                            }
                        }
                    }
                )

                TextButton(onClick = { uriHandler.openUri("https://github.com/yt-dlp/yt-dlp") }) {
                    Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.yt_dlp_docs))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (label.isNotBlank() && templateText.isNotBlank()) {
                        onSave(label, templateText)
                        onDismiss()
                    }
                },
                enabled = label.isNotBlank() && templateText.isNotBlank()
            ) {
                Text(stringResource(Res.string.confirm))
            }
        }
    )
}
