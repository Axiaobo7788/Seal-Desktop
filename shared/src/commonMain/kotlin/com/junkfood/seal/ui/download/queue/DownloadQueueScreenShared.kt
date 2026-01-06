package com.junkfood.seal.ui.download.queue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.junkfood.seal.ui.svg.DynamicColorImageVectors
import com.junkfood.seal.ui.svg.drawablevectors.download

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreenShared(
    state: DownloadQueueState,
    strings: DownloadQueueStrings,
    onFilterChange: (DownloadQueueFilter) -> Unit,
    onViewModeChange: (DownloadQueueViewMode) -> Unit,
    onItemAction: (itemId: String, action: DownloadQueueAction) -> Unit,
    onAddClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isCompact: Boolean = true,
    showAddButton: Boolean = true,
    showMenuButton: Boolean = true,
) {
    var sheetItemId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val filteredItems = remember(state.items, state.filter) {
        state.items.filter { item ->
            when (state.filter) {
                DownloadQueueFilter.All -> true
                DownloadQueueFilter.Downloading ->
                    item.status == DownloadQueueStatus.FetchingInfo ||
                        item.status == DownloadQueueStatus.Ready ||
                        item.status == DownloadQueueStatus.Running
                DownloadQueueFilter.Canceled -> item.status == DownloadQueueStatus.Canceled || item.status == DownloadQueueStatus.Error
                DownloadQueueFilter.Finished -> item.status == DownloadQueueStatus.Completed
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HeaderShared(strings = strings, onMenuClick = onMenuClick, onAddClick = onAddClick, isCompact = isCompact, showAddButton = showAddButton, showMenuButton = showMenuButton)

        FilterRowShared(
            strings = strings,
            active = state.filter,
            onSelect = onFilterChange,
        )

        SubHeaderShared(
            strings = strings,
            videoCount = filteredItems.count { it.mediaType == DownloadQueueMediaType.Video },
            audioCount = filteredItems.count { it.mediaType == DownloadQueueMediaType.Audio },
            viewMode = state.viewMode,
            onToggleView = {
                val target = if (state.viewMode == DownloadQueueViewMode.Grid) DownloadQueueViewMode.List else DownloadQueueViewMode.Grid
                onViewModeChange(target)
            },
        )

        if (filteredItems.isEmpty()) {
            EmptyPlaceholderShared(strings = strings, modifier = Modifier.fillMaxSize())
        } else {
            if (state.viewMode == DownloadQueueViewMode.Grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(240.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        QueueCardShared(
                            item = item,
                            onMoreClick = { sheetItemId = item.id },
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        QueueListItemShared(
                            item = item,
                            onMoreClick = { sheetItemId = item.id },
                        )
                    }
                }
            }
        }
    }

    if (sheetItemId != null) {
        val item = state.items.find { it.id == sheetItemId }
        if (item != null) {
            LaunchedEffect(sheetItemId) { scope.launch { sheetState.show() } }
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { sheetItemId = null }
                },
                sheetState = sheetState,
            ) {
                ActionSheetShared(
                    item = item,
                    strings = strings,
                    onAction = { action -> onItemAction(item.id, action) },
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { sheetItemId = null }
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderShared(
    strings: DownloadQueueStrings,
    onMenuClick: () -> Unit,
    onAddClick: () -> Unit,
    isCompact: Boolean,
    showAddButton: Boolean,
    showMenuButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showMenuButton) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = strings.queueTitle,
            style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        if (showAddButton) {
            ElevatedButton(onClick = onAddClick) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.addLabel)
            }
        }
    }
}

@Composable
private fun FilterRowShared(
    strings: DownloadQueueStrings,
    active: DownloadQueueFilter,
    onSelect: (DownloadQueueFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipShared(label = strings.filterAll, selected = active == DownloadQueueFilter.All) { onSelect(DownloadQueueFilter.All) }
        FilterChipShared(label = strings.filterDownloading, selected = active == DownloadQueueFilter.Downloading) { onSelect(DownloadQueueFilter.Downloading) }
        FilterChipShared(label = strings.filterCanceled, selected = active == DownloadQueueFilter.Canceled) { onSelect(DownloadQueueFilter.Canceled) }
        FilterChipShared(label = strings.filterFinished, selected = active == DownloadQueueFilter.Finished) { onSelect(DownloadQueueFilter.Finished) }
    }
}

@Composable
private fun FilterChipShared(label: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = container,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.clip(MaterialTheme.shapes.large).clickable { onClick() },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

@Composable
private fun SubHeaderShared(
    strings: DownloadQueueStrings,
    videoCount: Int,
    audioCount: Int,
    viewMode: DownloadQueueViewMode,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val parts = buildList {
            if (videoCount > 0) add(strings.videoCountLabel(videoCount))
            if (audioCount > 0) add(strings.audioCountLabel(audioCount))
        }
        Text(text = parts.joinToString(separator = "  "), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
        FilledIconButton(
            onClick = onToggleView,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Icon(
                imageVector = if (viewMode == DownloadQueueViewMode.Grid) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.List,
                contentDescription = if (viewMode == DownloadQueueViewMode.Grid) strings.gridLabel else strings.listLabel,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun QueueCardShared(item: DownloadQueueItemState, onMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Spacer(modifier = Modifier.aspectRatio(16f / 9f))
                ProgressBadge(item = item, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (item.author.isNotBlank()) {
                        Text(text = item.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    ProgressText(item)
                }
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun QueueListItemShared(item: DownloadQueueItemState, onMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            ProgressBadge(item = item, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(text = item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.author.isNotBlank()) {
                Text(text = item.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ProgressText(item)
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Outlined.MoreVert, contentDescription = null)
        }
    }
}

@Composable
private fun ProgressText(item: DownloadQueueItemState) {
    val statusLabel = when (item.status) {
        DownloadQueueStatus.Idle -> "Idle"
        DownloadQueueStatus.FetchingInfo -> "Fetching info"
        DownloadQueueStatus.Ready -> "Ready"
        DownloadQueueStatus.Running -> "Downloading"
        DownloadQueueStatus.Completed -> "Completed"
        DownloadQueueStatus.Canceled -> "Canceled"
        DownloadQueueStatus.Error -> "Error"
    }
    val progressPercent = item.progress?.let { " ${(it * 100f).toInt()}%" } ?: ""
    val extra = if (item.progressText.isNotBlank()) " • ${item.progressText}" else ""
    Text(
        text = statusLabel + progressPercent + extra,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ProgressBadge(item: DownloadQueueItemState, modifier: Modifier = Modifier) {
    val bg = when (item.status) {
        DownloadQueueStatus.Completed -> MaterialTheme.colorScheme.secondaryContainer
        DownloadQueueStatus.Error -> MaterialTheme.colorScheme.errorContainer
        DownloadQueueStatus.Canceled -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Surface(color = bg, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        val text = when (item.status) {
            DownloadQueueStatus.Completed -> "Done"
            DownloadQueueStatus.Error -> "Error"
            DownloadQueueStatus.Canceled -> "Canceled"
            DownloadQueueStatus.Running -> item.progress?.let { "${(it * 100).toInt()}%" } ?: "..."
            DownloadQueueStatus.FetchingInfo -> "Info"
            DownloadQueueStatus.Ready -> "Ready"
            DownloadQueueStatus.Idle -> "Idle"
        }
        Text(text = text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EmptyPlaceholderShared(strings: DownloadQueueStrings, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val painter = rememberVectorPainter(image = DynamicColorImageVectors.download())
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.5f).widthIn(max = 240.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text(strings.emptyTitle, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            strings.emptyBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionSheetShared(
    item: DownloadQueueItemState,
    strings: DownloadQueueStrings,
    onAction: (DownloadQueueAction) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (item.author.isNotBlank()) {
            Text(text = item.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider()
        ActionButtonsForStatus(item = item, strings = strings, onAction = onAction, onDismiss = onDismiss)
    }
}

@Composable
private fun ActionButtonsForStatus(
    item: DownloadQueueItemState,
    strings: DownloadQueueStrings,
    onAction: (DownloadQueueAction) -> Unit,
    onDismiss: () -> Unit,
) {
    @Composable
    fun button(label: String, action: DownloadQueueAction, prominent: Boolean = false) {
        val content: @Composable () -> Unit = { Text(label) }
        if (prominent) {
            Button(onClick = { onAction(action); onDismiss() }, modifier = Modifier.fillMaxWidth()) { content() }
        } else {
            OutlinedButton(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth()) { content() }
        }
    }

    when (item.status) {
        DownloadQueueStatus.Error, DownloadQueueStatus.Canceled -> button(strings.resumeLabel, DownloadQueueAction.Resume, prominent = true)
        DownloadQueueStatus.Running, DownloadQueueStatus.FetchingInfo, DownloadQueueStatus.Ready, DownloadQueueStatus.Idle -> button(strings.cancelLabel, DownloadQueueAction.Cancel, prominent = true)
        DownloadQueueStatus.Completed -> button(strings.openFileLabel, DownloadQueueAction.OpenFile, prominent = true)
    }

    if (item.status == DownloadQueueStatus.Completed) {
        button(strings.shareFileLabel, DownloadQueueAction.ShareFile)
    }
    button(strings.deleteLabel, DownloadQueueAction.Delete)
    button(strings.copyUrlLabel, DownloadQueueAction.CopyVideoUrl)
    button(strings.openUrlLabel, DownloadQueueAction.OpenVideoUrl)
    if (item.thumbnailUrl != null) {
        button(strings.openThumbLabel, DownloadQueueAction.OpenThumbnailUrl)
    }
    if (item.errorMessage?.isNotBlank() == true) {
        button(strings.copyErrorLabel, DownloadQueueAction.CopyError)
    }
    button(strings.showDetailsLabel, DownloadQueueAction.ShowDetails)
}
