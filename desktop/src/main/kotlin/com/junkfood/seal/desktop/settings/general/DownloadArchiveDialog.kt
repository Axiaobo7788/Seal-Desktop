package com.junkfood.seal.desktop.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.download.archive.ArchiveEditorNotice
import com.junkfood.seal.desktop.download.archive.ArchiveEditorProblem
import com.junkfood.seal.desktop.download.archive.DesktopDownloadArchiveEditor
import com.junkfood.seal.desktop.download.archive.DesktopDownloadArchiveService
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.*
import java.awt.Desktop
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private enum class ArchiveConfirmation { Close, Refresh, Clear }

@Composable
internal fun DownloadArchiveDialog(
    visible: Boolean,
    hasActiveDownloads: () -> Boolean,
    onDismiss: () -> Unit,
) {
    val activeDownloads = hasActiveDownloads()
    val currentActiveCheck by rememberUpdatedState(hasActiveDownloads)
    val service = remember { DesktopDownloadArchiveService() }
    val editor = remember { DesktopDownloadArchiveEditor(service) { !currentActiveCheck() } }
    val state by editor.state.collectAsState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<ArchiveConfirmation?>(null) }
    var locationFailed by remember { mutableStateOf(false) }
    val path = service.path.toAbsolutePath().toString()

    LaunchedEffect(visible) {
        if (visible) {
            editing = false
            locationFailed = false
            editor.refresh()
        }
    }

    fun requestClose() {
        if (state.busy) return
        if (state.dirty) confirmation = ArchiveConfirmation.Close else onDismiss()
    }

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = ::requestClose,
        icon = { Icon(Icons.Outlined.Archive, null) },
        title = { Text(stringResource(Res.string.desktop_archive_manage)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionContainer { Text(path, style = MaterialTheme.typography.bodySmall) }
                state.snapshot?.let {
                    Text(stringResource(Res.string.desktop_archive_count, it.count))
                    if (it.malformedLineCount > 0) {
                        Text(stringResource(Res.string.desktop_archive_malformed, it.malformedLineCount))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = !state.busy,
                        onClick = {
                            if (state.dirty) confirmation = ArchiveConfirmation.Refresh
                            else scope.launch { editor.refresh() }
                        },
                    ) { Text(stringResource(Res.string.desktop_archive_refresh)) }
                    TextButton(
                        enabled = !state.busy,
                        onClick = {
                            scope.launch {
                                locationFailed = !withContext(Dispatchers.IO) {
                                    runCatching {
                                        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                                        val parent = generateSequence(service.path.toAbsolutePath().parent) { it.parent }
                                            .first { Files.isDirectory(it) }
                                        Desktop.getDesktop().open(parent.toFile())
                                    }.isSuccess
                                }
                            }
                        },
                    ) { Text(stringResource(Res.string.desktop_archive_open_location)) }
                }
                if (activeDownloads) {
                    Text(stringResource(Res.string.desktop_archive_downloads_active), style = MaterialTheme.typography.bodySmall)
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.problem?.let { problem ->
                    Text(
                        stringResource(when (problem) {
                            ArchiveEditorProblem.Read -> Res.string.desktop_archive_read_failed
                            ArchiveEditorProblem.Save -> Res.string.desktop_archive_save_failed
                            ArchiveEditorProblem.Clear -> Res.string.desktop_archive_clear_failed
                            ArchiveEditorProblem.ChangedExternally -> Res.string.desktop_archive_changed_externally
                            ArchiveEditorProblem.DownloadsActive -> Res.string.desktop_archive_downloads_active
                            ArchiveEditorProblem.UnsavedChanges -> Res.string.desktop_archive_discard_desc
                        }),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (locationFailed) Text(stringResource(Res.string.desktop_archive_location_failed), color = MaterialTheme.colorScheme.error)
                state.notice?.let {
                    Text(stringResource(if (it == ArchiveEditorNotice.Saved) Res.string.desktop_archive_saved else Res.string.desktop_archive_cleared))
                }
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = editor::updateDraft,
                    readOnly = !editing || activeDownloads,
                    enabled = state.snapshot != null && !state.busy,
                    label = { Text(stringResource(Res.string.desktop_archive_contents)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 280.dp),
                    minLines = 6,
                    maxLines = 12,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = state.snapshot != null && !state.busy && !activeDownloads,
                        onClick = { editing = true },
                    ) { Text(stringResource(Res.string.edit)) }
                    TextButton(
                        enabled = state.snapshot != null && !state.busy && !activeDownloads && (state.snapshot!!.content.isNotEmpty() || state.dirty),
                        onClick = { confirmation = ArchiveConfirmation.Clear },
                    ) { Text(stringResource(Res.string.clear), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        dismissButton = { TextButton(onClick = ::requestClose, enabled = !state.busy) { Text(stringResource(Res.string.close)) } },
        confirmButton = {
            TextButton(onClick = { scope.launch { editor.save() } }, enabled = state.dirty && !state.busy && !activeDownloads) {
                Text(stringResource(Res.string.save))
            }
        },
    )

    AnimatedAlertDialog(
        visible = confirmation != null,
        onDismissRequest = { confirmation = null },
        title = { Text(stringResource(if (confirmation == ArchiveConfirmation.Clear) Res.string.clear_download_archive else Res.string.desktop_archive_discard_title)) },
        text = { Text(stringResource(if (confirmation == ArchiveConfirmation.Clear) Res.string.desktop_archive_clear_desc else Res.string.desktop_archive_discard_desc)) },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text(stringResource(Res.string.cancel)) } },
        confirmButton = {
            TextButton(onClick = {
                val action = confirmation
                confirmation = null
                when (action) {
                    ArchiveConfirmation.Close -> { editor.discardDraft(); onDismiss() }
                    ArchiveConfirmation.Refresh -> scope.launch { editor.refresh(discardChanges = true) }
                    ArchiveConfirmation.Clear -> scope.launch { editor.clear() }
                    null -> Unit
                }
            }) { Text(stringResource(if (confirmation == ArchiveConfirmation.Clear) Res.string.clear else Res.string.discard)) }
        },
    )
}
