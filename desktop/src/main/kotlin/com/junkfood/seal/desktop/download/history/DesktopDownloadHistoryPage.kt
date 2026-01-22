package com.junkfood.seal.desktop.download.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.backup_type
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.clipboard
import com.junkfood.seal.shared.generated.resources.copy_link
import com.junkfood.seal.shared.generated.resources.delete
import com.junkfood.seal.shared.generated.resources.download_history_imported
import com.junkfood.seal.shared.generated.resources.desktop_open_navigation
import com.junkfood.seal.shared.generated.resources.downloads_history
import com.junkfood.seal.shared.generated.resources.export_backup
import com.junkfood.seal.shared.generated.resources.export_download_history
import com.junkfood.seal.shared.generated.resources.export_download_history_msg
import com.junkfood.seal.shared.generated.resources.export_to
import com.junkfood.seal.shared.generated.resources.file
import com.junkfood.seal.shared.generated.resources.full_backup
import com.junkfood.seal.shared.generated.resources.import_backup
import com.junkfood.seal.shared.generated.resources.import_download_history
import com.junkfood.seal.shared.generated.resources.import_download_history_msg
import com.junkfood.seal.shared.generated.resources.import_from
import com.junkfood.seal.shared.generated.resources.item_count
import com.junkfood.seal.shared.generated.resources.open_file
import com.junkfood.seal.shared.generated.resources.open_url
import com.junkfood.seal.shared.generated.resources.search
import com.junkfood.seal.shared.generated.resources.unknown
import com.junkfood.seal.shared.generated.resources.unavailable
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.shared.generated.resources.video_url
import com.junkfood.seal.ui.download.queue.DownloadThumbnail
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class HistoryFilter {
    // Kept for binary compatibility with previous states; not used by UI anymore.
    @Suppress("unused")
    All,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopDownloadHistoryPage(
    entries: List<DesktopDownloadHistoryEntry>,
    onDelete: (String) -> Unit,
    onExportToFile: (DesktopHistoryExportType, Path, (Result<Unit>) -> Unit) -> Unit,
    onExportToClipboard: (DesktopHistoryExportType, (Result<Unit>) -> Unit) -> Unit,
    onImportFromFile: (Path, (Result<Int>) -> Unit) -> Unit,
    onImportFromClipboard: (String, (Result<Int>) -> Unit) -> Unit,
    disablePreview: Boolean = false,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = false,
) {
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    var audioFilter by remember { mutableStateOf(false) }
    var videoFilter by remember { mutableStateOf(false) }
    var activeSourceIndex by remember { mutableStateOf(-1) }

    var actionsOpen by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var errorDialog by remember { mutableStateOf<String?>(null) }

    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val importedSnackbarTemplate = stringResource(Res.string.download_history_imported)

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = { true },
        )

    val searched =
        entries
            .asSequence()
            .filter {
                if (query.isBlank()) true
                else {
                    val q = query.trim().lowercase()
                    it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
                }
            }
            .toList()

    val sourceKeys =
        searched
            .asSequence()
            .map { it.extractor.trim().ifBlank { "Unknown" } }
            .distinct()
            .toList()

    val activeSourceKey = sourceKeys.getOrNull(activeSourceIndex)

    val filtered =
        searched
            .asSequence()
            .filter {
                when {
                    audioFilter && !videoFilter -> it.mediaType == DesktopHistoryMediaType.Audio
                    videoFilter && !audioFilter -> it.mediaType == DesktopHistoryMediaType.Video
                    else -> true
                }
            }
            .filter { entry ->
                if (activeSourceKey == null || activeSourceKey.isBlank()) true
                else (entry.extractor.trim().ifBlank { "Unknown" } == activeSourceKey)
            }
            .toList()

    // "视频来源/平台" 行内展示开关：仅当存在多种平台时显示
    val showPlatformInRows = filtered.asSequence().map { it.platform }.distinct().count() > 1

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.downloads_history)) },
                navigationIcon = {
                    if (isCompact) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Outlined.Menu, contentDescription = stringResource(Res.string.desktop_open_navigation))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Outlined.Search, contentDescription = stringResource(Res.string.search))
                    }

                    Box {
                        IconButton(onClick = { actionsOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = actionsOpen, onDismissRequest = { actionsOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.export_backup)) },
                                onClick = {
                                    actionsOpen = false
                                    showExportDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.import_backup)) },
                                onClick = {
                                    actionsOpen = false
                                    showImportDialog = true
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.search)) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HistoryFilterChip(
                    text = stringResource(Res.string.audio),
                    selected = audioFilter,
                    onClick = {
                        audioFilter = if (audioFilter) false else true
                        if (audioFilter) videoFilter = false
                    },
                )

                HistoryFilterChip(
                    text = stringResource(Res.string.video),
                    selected = videoFilter,
                    onClick = {
                        videoFilter = if (videoFilter) false else true
                        if (videoFilter) audioFilter = false
                    },
                )

                if (sourceKeys.size > 1) {
                    VerticalDivider(
                        modifier = Modifier.padding(horizontal = 6.dp).height(24.dp).align(Alignment.CenterVertically),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )

                    sourceKeys.forEachIndexed { index, key ->
                        val label =
                            if (key == "Unknown") stringResource(Res.string.unknown)
                            else key
                        HistoryFilterChip(
                            text = label,
                            selected = activeSourceIndex == index,
                            onClick = {
                                activeSourceIndex = if (activeSourceIndex == index) -1 else index
                            },
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        HistoryRow(entry = entry, onDelete = onDelete, showPlatform = showPlatformInRows, disablePreview = disablePreview)
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }

    if (showExportDialog) {
        DesktopHistoryExportDialog(
            itemCount = entries.size,
            onDismissRequest = { showExportDialog = false },
            onExport = { type, destination ->
                showExportDialog = false
                when (destination) {
                    DesktopHistoryIoDestination.File -> {
                        val suggested = when (type) {
                            DesktopHistoryExportType.DownloadHistory -> "seal-history.json"
                            DesktopHistoryExportType.UrlList -> "seal-urls.txt"
                        }
                        val path = pickSavePath(suggested)
                        if (path != null) {
                            onExportToFile(type, path) { res ->
                                res.exceptionOrNull()?.let { errorDialog = it.message ?: it.toString() }
                            }
                        }
                    }

                    DesktopHistoryIoDestination.Clipboard -> {
                        onExportToClipboard(type) { res ->
                            res.exceptionOrNull()?.let { errorDialog = it.message ?: it.toString() }
                        }
                    }
                }
            },
        )
    }

    if (showImportDialog) {
        DesktopHistoryImportDialog(
            onDismissRequest = { showImportDialog = false },
            onImport = { destination ->
                showImportDialog = false
                when (destination) {
                    DesktopHistoryIoDestination.File -> {
                        val path = pickOpenPath()
                        if (path != null) {
                            onImportFromFile(path) { res ->
                                res.onSuccess { count ->
                                    if (count > 0) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                importedSnackbarTemplate.format(count.toString()),
                                            )
                                        }
                                    }
                                }.onFailure { e ->
                                    errorDialog = e.message ?: e.toString()
                                }
                            }
                        }
                    }

                    DesktopHistoryIoDestination.Clipboard -> {
                        val text = clipboard.getText()?.text.orEmpty()
                        onImportFromClipboard(text) { res ->
                            res.onSuccess { count ->
                                if (count > 0) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            importedSnackbarTemplate.format(count.toString()),
                                        )
                                    }
                                }
                            }.onFailure { e ->
                                errorDialog = e.message ?: e.toString()
                            }
                        }
                    }
                }
            },
        )
    }

    if (errorDialog != null) {
        AlertDialog(
            onDismissRequest = { errorDialog = null },
            confirmButton = {
                Button(onClick = { errorDialog = null }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text(errorDialog.orEmpty()) },
        )
    }
}

@Composable
private fun HistoryRow(
    entry: DesktopDownloadHistoryEntry,
    onDelete: (String) -> Unit,
    showPlatform: Boolean,
    disablePreview: Boolean,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val clipboard = LocalClipboardManager.current
    val exists = entry.filePath?.let { runCatching { Files.exists(Path.of(it)) }.getOrDefault(false) } ?: false
    val metaLine =
        when {
            !exists -> stringResource(Res.string.unavailable)
            entry.fileSizeBytes != null -> humanSize(entry.fileSizeBytes)
            else -> ""
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = exists,
                    ) {
                        if (exists) {
                            entry.filePath?.let { openFile(it) }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(156.dp, 88.dp)) {
                DownloadThumbnail(url = if (disablePreview) null else entry.thumbnailUrl, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.author.isNotBlank()) {
                    Text(
                        text = entry.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val categoryLabel =
                    when (entry.mediaType) {
                        DesktopHistoryMediaType.Audio -> stringResource(Res.string.audio)
                        DesktopHistoryMediaType.Video -> stringResource(Res.string.video)
                    }

                if (showPlatform) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.platform.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        VerticalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp).height(12.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        )
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (metaLine.isNotBlank()) {
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exists) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete(entry.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.copy_link)) },
                        onClick = {
                            menuOpen = false
                            entry.url.takeIf { it.isNotBlank() }?.let { clipboard.setText(AnnotatedString(it)) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.open_url)) },
                        enabled = entry.url.isNotBlank(),
                        onClick = {
                            menuOpen = false
                            entry.url.takeIf { it.isNotBlank() }?.let { safeBrowse(it) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.open_file)) },
                        enabled = exists,
                        onClick = {
                            menuOpen = false
                            entry.filePath?.let { openFile(it) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val background =
        when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    val content =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val borderColor =
        when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        modifier = Modifier.height(32.dp),
        shape = MaterialTheme.shapes.large,
        color = background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
        onClick = { if (enabled) onClick() },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = content,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class DesktopHistoryIoDestination { File, Clipboard }

@Composable
private fun DesktopHistoryExportDialog(
    itemCount: Int,
    onDismissRequest: () -> Unit,
    onExport: (DesktopHistoryExportType, DesktopHistoryIoDestination) -> Unit,
) {
    var type by remember { mutableStateOf(DesktopHistoryExportType.DownloadHistory) }
    var destination by remember { mutableStateOf(DesktopHistoryIoDestination.File) }

    val itemCountLabel = pluralStringResource(Res.plurals.item_count, itemCount, itemCount)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                contentDescription = null,
            )
        },
        confirmButton = {
            Button(onClick = { onExport(type, destination) }) {
                Text(stringResource(Res.string.export_backup))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = { Text(stringResource(Res.string.export_download_history)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text =
                        stringResource(Res.string.export_download_history_msg)
                            .format(itemCountLabel),
                )

                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(Res.string.backup_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryFilterChip(
                        text = stringResource(Res.string.full_backup),
                        selected = type == DesktopHistoryExportType.DownloadHistory,
                        onClick = { type = DesktopHistoryExportType.DownloadHistory },
                    )
                    HistoryFilterChip(
                        text = stringResource(Res.string.video_url),
                        selected = type == DesktopHistoryExportType.UrlList,
                        onClick = { type = DesktopHistoryExportType.UrlList },
                    )
                }

                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(Res.string.export_to),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryFilterChip(
                        text = stringResource(Res.string.file),
                        selected = destination == DesktopHistoryIoDestination.File,
                        onClick = { destination = DesktopHistoryIoDestination.File },
                    )
                    HistoryFilterChip(
                        text = stringResource(Res.string.clipboard),
                        selected = destination == DesktopHistoryIoDestination.Clipboard,
                        onClick = { destination = DesktopHistoryIoDestination.Clipboard },
                    )
                }
            }
        },
    )
}

@Composable
private fun DesktopHistoryImportDialog(
    onDismissRequest: () -> Unit,
    onImport: (DesktopHistoryIoDestination) -> Unit,
) {
    var destination by remember { mutableStateOf(DesktopHistoryIoDestination.File) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = null,
            )
        },
        confirmButton = {
            Button(onClick = { onImport(destination) }) {
                Text(stringResource(Res.string.import_backup))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = { Text(stringResource(Res.string.import_download_history)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(Res.string.import_download_history_msg),
                )

                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(Res.string.backup_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryFilterChip(
                        text = stringResource(Res.string.full_backup),
                        selected = true,
                        enabled = false,
                        onClick = {},
                    )
                }

                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(Res.string.import_from),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryFilterChip(
                        text = stringResource(Res.string.file),
                        selected = destination == DesktopHistoryIoDestination.File,
                        onClick = { destination = DesktopHistoryIoDestination.File },
                    )
                    HistoryFilterChip(
                        text = stringResource(Res.string.clipboard),
                        selected = destination == DesktopHistoryIoDestination.Clipboard,
                        onClick = { destination = DesktopHistoryIoDestination.Clipboard },
                    )
                }
            }
        },
    )
}

private fun pickOpenPath(): Path? {
    val dialog = FileDialog(null as Frame?, "Import", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return runCatching { Path.of(dialog.directory, file) }.getOrNull()
}

private fun pickSavePath(suggestedFileName: String): Path? {
    val dialog = FileDialog(null as Frame?, "Export", FileDialog.SAVE)
    dialog.file = suggestedFileName
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return try {
        Path.of(dialog.directory, file)
    } catch (_: InvalidPathException) {
        null
    }
}

private fun humanSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun openFile(path: String) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        Desktop.getDesktop().open(File(path))
    }
}

private fun safeBrowse(url: String) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        Desktop.getDesktop().browse(java.net.URI(url))
    }
}
