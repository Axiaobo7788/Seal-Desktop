package com.junkfood.seal.desktop.download.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.copy_link
import com.junkfood.seal.shared.generated.resources.delete
import com.junkfood.seal.shared.generated.resources.desktop_open_navigation
import com.junkfood.seal.shared.generated.resources.downloads_history
import com.junkfood.seal.shared.generated.resources.open_file
import com.junkfood.seal.shared.generated.resources.open_url
import com.junkfood.seal.shared.generated.resources.search
import com.junkfood.seal.shared.generated.resources.unavailable
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.ui.download.queue.DownloadThumbnail
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.jetbrains.compose.resources.stringResource

private enum class HistoryFilter {
    All,
    Audio,
    Video,
    BiliBili,
    YouTube,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopDownloadHistoryPage(
    entries: List<DesktopDownloadHistoryEntry>,
    onDelete: (String) -> Unit,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = false,
) {
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.All) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = { true },
        )

    val filtered =
        entries
            .asSequence()
            .filter {
                when (filter) {
                    HistoryFilter.All -> true
                    HistoryFilter.Audio -> it.mediaType == DesktopHistoryMediaType.Audio
                    HistoryFilter.Video -> it.mediaType == DesktopHistoryMediaType.Video
                    HistoryFilter.BiliBili -> it.platform == DesktopHistoryPlatform.BiliBili
                    HistoryFilter.YouTube -> it.platform == DesktopHistoryPlatform.YouTube
                }
            }
            .filter {
                if (query.isBlank()) true
                else {
                    val q = query.trim().lowercase()
                    it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
                }
            }
            .toList()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
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

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = filter == HistoryFilter.Audio, onClick = { filter = HistoryFilter.Audio }, label = { Text(stringResource(Res.string.audio)) })
                FilterChip(selected = filter == HistoryFilter.Video, onClick = { filter = HistoryFilter.Video }, label = { Text(stringResource(Res.string.video)) })
                FilterChip(selected = filter == HistoryFilter.BiliBili, onClick = { filter = HistoryFilter.BiliBili }, label = { Text("BiliBili") })
                FilterChip(selected = filter == HistoryFilter.YouTube, onClick = { filter = HistoryFilter.YouTube }, label = { Text("YouTube") })
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
                        HistoryRow(entry = entry, onDelete = onDelete)
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: DesktopDownloadHistoryEntry,
    onDelete: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
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
        onClick = {
            if (exists) {
                entry.filePath?.let { openFile(it) }
            }
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.size(156.dp, 88.dp)) {
                DownloadThumbnail(url = entry.thumbnailUrl, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
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
