package com.junkfood.seal.desktop.settings.directory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.ActionCard
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.advanced_settings
import com.junkfood.seal.shared.generated.resources.audio_directory
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.clear_temp_files
import com.junkfood.seal.shared.generated.resources.clear_temp_files_desc
import com.junkfood.seal.shared.generated.resources.clear_temp_files_info
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.custom
import com.junkfood.seal.shared.generated.resources.custom_command_directory
import com.junkfood.seal.shared.generated.resources.custom_command_directory_desc
import com.junkfood.seal.shared.generated.resources.defaults
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.download_archive
import com.junkfood.seal.shared.generated.resources.download_directory
import com.junkfood.seal.shared.generated.resources.output_template
import com.junkfood.seal.shared.generated.resources.output_template_desc
import com.junkfood.seal.shared.generated.resources.playlist_title
import com.junkfood.seal.shared.generated.resources.privacy
import com.junkfood.seal.shared.generated.resources.private_directory
import com.junkfood.seal.shared.generated.resources.private_directory_desc
import com.junkfood.seal.shared.generated.resources.restrict_filenames
import com.junkfood.seal.shared.generated.resources.restrict_filenames_desc
import com.junkfood.seal.shared.generated.resources.subdirectory
import com.junkfood.seal.shared.generated.resources.subdirectory_desc
import com.junkfood.seal.shared.generated.resources.subdirectory_hint
import com.junkfood.seal.shared.generated.resources.video_directory
import com.junkfood.seal.shared.generated.resources.website
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource
import javax.swing.JFileChooser
import javax.swing.UIManager
private const val BASENAME = "%(title).200B"
private const val EXTENSION = ".%(ext)s"
private const val OUTPUT_TEMPLATE_DEFAULT = BASENAME + EXTENSION
private const val OUTPUT_TEMPLATE_ID = "$BASENAME [%(id)s]$EXTENSION"
private suspend fun chooseDirectory(title: String, currentDir: String? = null): String? = withContext(Dispatchers.IO) {
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    val defaultDir = if (currentDir.isNullOrBlank()) System.getProperty("user.home") else currentDir
    if (osName.contains("linux")) {
        val pickerCmd = when {
            isCommandAvailable("kdialog") -> arrayOf("kdialog", "--getexistingdirectory", defaultDir, "--title", title)
            isCommandAvailable("zenity") -> arrayOf("zenity", "--file-selection", "--directory", "--title=$title", "--filename=$defaultDir")
            else -> null
        }
        if (pickerCmd != null) {
            return@withContext runCatching {
                val process = ProcessBuilder(*pickerCmd)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                val result = process.inputStream.bufferedReader().readText().trim()
                if (process.waitFor() == 0 && result.isNotBlank()) result else null
            }.getOrNull()
        }
    }
    return@withContext runCatching {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = title
            if (!currentDir.isNullOrBlank()) {
                currentDirectory = java.io.File(currentDir)
            }
        }
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        } else {
            null
        }
    }.getOrNull()
}

private fun isCommandAvailable(command: String): Boolean {
    return runCatching {
        if (System.getProperty("os.name")?.lowercase()?.contains("win") == true) {
            ProcessBuilder("powershell", "-NoProfile", "-Command", "Get-Command $command -ErrorAction SilentlyContinue")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start().waitFor() == 0
        } else {
            ProcessBuilder("sh", "-c", "command -v $command")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start().waitFor() == 0
        }
    }.getOrDefault(false)
}

@Composable
internal fun DirectorySettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val archivePath = DesktopYtDlpPaths.archiveFile().toAbsolutePath().toString()
    val defaultStr = stringResource(Res.string.defaults)
    val defaultDownloadDir = DesktopYtDlpPaths.downloadDirectory(null).toAbsolutePath().toString()
    var showClearTempDialog by remember { mutableStateOf(false) }
    var showOutputTemplateDialog by remember { mutableStateOf(false) }
    var showSubdirectoryDialog by remember { mutableStateOf(false) }
    SettingsPageScaffold(title = stringResource(Res.string.download_directory), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.general_settings))
        ActionCard(
            title = stringResource(Res.string.video_directory),
            description = preferences.videoDirectory.ifBlank { defaultDownloadDir },
            icon = Icons.Outlined.VideoLibrary,
        ) {
            scope.launch {
                val newDir = chooseDirectory(title = "Select Video Directory", currentDir = preferences.videoDirectory.ifBlank { defaultDownloadDir })
                if (newDir != null) {
                    onUpdate { it.copy(videoDirectory = newDir) }
                }
            }
        }
        ActionCard(
            title = stringResource(Res.string.audio_directory),
            description = preferences.audioDirectory.ifBlank { defaultDownloadDir },
            icon = Icons.Outlined.LibraryMusic,
        ) {
            scope.launch {
                val newDir = chooseDirectory(title = "Select Audio Directory", currentDir = preferences.audioDirectory.ifBlank { defaultDownloadDir })
                if (newDir != null) {
                    onUpdate { it.copy(audioDirectory = newDir) }
                }
            }
        }
        ToggleCard(
            title = stringResource(Res.string.restrict_filenames),
            description = stringResource(Res.string.restrict_filenames_desc),
            icon = Icons.Outlined.Spellcheck,
            checked = preferences.restrictFilenames,
        ) { checked -> onUpdate { it.copy(restrictFilenames = checked) } }
        ActionCard(
            title = stringResource(Res.string.clear_temp_files),
            description = stringResource(Res.string.clear_temp_files_desc),
            icon = Icons.Outlined.FolderDelete,
        ) {
            showClearTempDialog = true
        }
        ActionCard(
            title = stringResource(Res.string.output_template),
            description = preferences.outputTemplate.ifBlank { OUTPUT_TEMPLATE_DEFAULT },
            icon = Icons.Outlined.FolderSpecial,
        ) {
            showOutputTemplateDialog = true
        }
        ActionCard(
            title = stringResource(Res.string.subdirectory),
            description = stringResource(Res.string.subdirectory_desc),
            icon = Icons.Outlined.SnippetFolder,
        ) {
            showSubdirectoryDialog = true
        }
        ActionCard(
            title = stringResource(Res.string.custom_command_directory),
            description = stringResource(Res.string.custom_command_directory_desc) + "\n" + preferences.commandDirectory.ifBlank { defaultStr },
            icon = Icons.Rounded.Folder,
        ) {
            scope.launch {
                val newDir = chooseDirectory(title = "Select Custom Command Directory", currentDir = preferences.commandDirectory)
                if (newDir != null) {
                    onUpdate { it.copy(commandDirectory = newDir) }
                }
            }
        }
        PreferenceSubtitle(text = stringResource(Res.string.privacy))
        ToggleCard(
            title = stringResource(Res.string.private_directory),
            description = stringResource(Res.string.private_directory_desc),
            icon = Icons.Outlined.Lock,
            checked = preferences.privateDirectory,
        ) { checked -> onUpdate { it.copy(privateDirectory = checked) } }
        PreferenceSubtitle(text = stringResource(Res.string.advanced_settings))
        ToggleCard(
            title = stringResource(Res.string.download_archive),
            description = archivePath,
            icon = Icons.Rounded.Archive,
            checked = preferences.useDownloadArchive,
        ) { checked -> onUpdate { it.copy(useDownloadArchive = checked) } }
    }
    AnimatedAlertDialog(
        visible = showClearTempDialog,
        onDismissRequest = { showClearTempDialog = false },
        title = { Text(stringResource(Res.string.clear_temp_files)) },
        text = { Text(stringResource(Res.string.clear_temp_files_info, DesktopYtDlpPaths.tempDirectory().toAbsolutePath().toString())) },
        confirmButton = {
            TextButton(onClick = {
                DesktopYtDlpPaths.clearTempFiles()
                showClearTempDialog = false
            }) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { showClearTempDialog = false }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )

    OutputTemplateDialog(
        visible = showOutputTemplateDialog,
        selectedTemplate = preferences.outputTemplate.ifBlank { OUTPUT_TEMPLATE_DEFAULT },
        onDismissRequest = { showOutputTemplateDialog = false },
        onConfirm = { newTemplate ->
            onUpdate { it.copy(outputTemplate = newTemplate) }
            showOutputTemplateDialog = false
        }
    )

    DirectoryPreferenceDialog(
        visible = showSubdirectoryDialog,
        isWebsiteSelected = preferences.subdirectoryExtractor,
        isPlaylistTitleSelected = preferences.subdirectoryPlaylistTitle,
        onDismissRequest = { showSubdirectoryDialog = false },
        onConfirm = { website, playlist ->
            onUpdate { it.copy(subdirectoryExtractor = website, subdirectoryPlaylistTitle = playlist) }
            showSubdirectoryDialog = false
        }
    )
}
@Composable
private fun OutputTemplateDialog(
    visible: Boolean,
    selectedTemplate: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var editingTemplate by remember { mutableStateOf(selectedTemplate.ifBlank { OUTPUT_TEMPLATE_DEFAULT }) }
    var selectedItem by remember {
        mutableIntStateOf(
            when (selectedTemplate) {
                OUTPUT_TEMPLATE_DEFAULT -> 1
                OUTPUT_TEMPLATE_ID -> 2
                else -> 3
            }
        )
    }
    var error by remember { mutableIntStateOf(0) }
    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.FolderSpecial, contentDescription = null) },
        title = { Text(stringResource(Res.string.output_template)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.output_template_desc),
                    modifier = Modifier.padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DialogSingleChoiceItem(
                        text = OUTPUT_TEMPLATE_DEFAULT,
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
                    )
                    DialogSingleChoiceItem(
                        text = OUTPUT_TEMPLATE_ID,
                        selected = selectedItem == 2,
                        onClick = { selectedItem = 2 }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedItem == 3,
                            onClick = { selectedItem = 3 },
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                        OutlinedTextField(
                            value = editingTemplate,
                            onValueChange = {
                                error = if (!it.contains(BASENAME)) 1 else if (!it.endsWith(EXTENSION)) 2 else 0
                                editingTemplate = it
                            },
                            isError = error != 0,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.custom)) },
                            supportingText = {
                                Text("Required: $BASENAME, $EXTENSION", fontFamily = FontFamily.Monospace)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = when (selectedItem) {
                        1 -> OUTPUT_TEMPLATE_DEFAULT
                        2 -> OUTPUT_TEMPLATE_ID
                        else -> editingTemplate
                    }
                    onConfirm(result)
                },
                enabled = error == 0 || selectedItem != 3
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
@Composable
private fun DirectoryPreferenceDialog(
    visible: Boolean,
    isWebsiteSelected: Boolean,
    isPlaylistTitleSelected: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit,
) {
    var website by remember { mutableStateOf(isWebsiteSelected) }
    var playlist by remember { mutableStateOf(isPlaylistTitleSelected) }
    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.SnippetFolder, contentDescription = null) },
        title = { Text(stringResource(Res.string.subdirectory)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.subdirectory_desc),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                DialogCheckBoxItem(
                    text = stringResource(Res.string.website),
                    checked = website,
                    onCheckedChange = { website = it }
                )
                DialogCheckBoxItem(
                    text = stringResource(Res.string.playlist_title),
                    checked = playlist,
                    onCheckedChange = { playlist = it }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(4.dp))
                val dirStr = StringBuilder(".../").run {
                    if (website) append("website/")
                    if (playlist) append("playlist_title/")
                    append("file_name")
                }.toString()
                Text(
                    text = stringResource(Res.string.subdirectory_hint) + "\n" + dirStr,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(website, playlist) }) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
@Composable
private fun DialogSingleChoiceItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.clearAndSetSemantics {}
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp),
            fontFamily = FontFamily.Monospace
        )
    }
}
@Composable
private fun DialogCheckBoxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {}
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}