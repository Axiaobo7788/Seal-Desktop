package com.junkfood.seal.ui.download.queue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import com.junkfood.seal.ui.svg.DynamicColorImageVectors
import com.junkfood.seal.ui.svg.drawablevectors.download
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.media_info
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.video
import org.jetbrains.compose.resources.stringResource

private val StatusLabelContainerColor = Color.Black.copy(alpha = 0.68f)

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

    val filteredItems = state.items.filter { item ->
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
            if (state.isLoading) {
                LoadingPlaceholderShared(modifier = Modifier.fillMaxSize())
            } else {
                EmptyPlaceholderShared(strings = strings, modifier = Modifier.fillMaxSize())
            }
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
                            strings = strings,
                            onMoreClick = { sheetItemId = item.id },
                            onClick = { sheetItemId = item.id },
                            onOverlayAction = { action -> onItemAction(item.id, action) },
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
                            strings = strings,
                            onMoreClick = { sheetItemId = item.id },
                            onClick = { sheetItemId = item.id },
                            onOverlayAction = { action -> onItemAction(item.id, action) },
                            modifier = Modifier,
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

        val isGrid = viewMode == DownloadQueueViewMode.Grid

        FilledIconButton(
            onClick = onToggleView,
            modifier = Modifier.size(36.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isGrid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (isGrid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Icon(
                imageVector = if (isGrid) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.List,
                contentDescription = if (isGrid) strings.gridLabel else strings.listLabel,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun QueueCardShared(
    item: DownloadQueueItemState,
    strings: DownloadQueueStrings,
    onMoreClick: () -> Unit,
    onClick: () -> Unit,
    onOverlayAction: (DownloadQueueAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerLow
    Card(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                DownloadThumbnail(url = item.thumbnailUrl, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                StatusPill(item = item, strings = strings, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
                DownloadOverlay(item = item, modifier = Modifier.align(Alignment.Center), onOverlayAction = onOverlayAction)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { item.url },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.author.isNotBlank()) {
                        Text(
                            text = item.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusRowShared(item = item, strings = strings)
                }
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun QueueListItemShared(
    item: DownloadQueueItemState,
    strings: DownloadQueueStrings,
    onMoreClick: () -> Unit,
    onClick: () -> Unit,
    onOverlayAction: (DownloadQueueAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onClick() }.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            DownloadThumbnail(url = item.thumbnailUrl, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            StatusPill(item = item, strings = strings, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
            DownloadOverlay(item = item, modifier = Modifier.align(Alignment.Center), onOverlayAction = onOverlayAction)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = item.title.ifBlank { item.url },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.author.isNotBlank()) {
                Text(
                    text = item.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusRowShared(item = item, strings = strings)
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Outlined.MoreVert, contentDescription = null)
        }
    }
}

@Composable
private fun ProgressText(item: DownloadQueueItemState, strings: DownloadQueueStrings) {
    val statusLabel =
        when (item.status) {
            DownloadQueueStatus.Idle -> strings.statusIdle
            DownloadQueueStatus.FetchingInfo -> strings.statusFetchingInfo
            DownloadQueueStatus.Ready -> strings.statusReady
            DownloadQueueStatus.Running -> strings.statusRunning
            DownloadQueueStatus.Completed -> strings.statusCompleted
            DownloadQueueStatus.Canceled -> strings.statusCanceled
            DownloadQueueStatus.Error -> strings.statusError
        }
    val progressPercent = item.progress?.let { " ${String.format("%.1f %%", it * 100f)}" } ?: ""
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
private fun StatusRowShared(item: DownloadQueueItemState, strings: DownloadQueueStrings) {
    val sizeModifier = Modifier.size(14.dp)
    val statusColor =
        when (item.status) {
            DownloadQueueStatus.Completed -> MaterialTheme.colorScheme.tertiary
            DownloadQueueStatus.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (item.status) {
            DownloadQueueStatus.Canceled -> {
                Icon(Icons.Outlined.Cancel, contentDescription = null, tint = statusColor, modifier = sizeModifier)
            }
            DownloadQueueStatus.Completed -> {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = statusColor, modifier = sizeModifier)
            }
            DownloadQueueStatus.Error -> {
                Icon(Icons.Outlined.Error, contentDescription = null, tint = statusColor, modifier = sizeModifier)
            }
            DownloadQueueStatus.FetchingInfo,
            DownloadQueueStatus.Ready,
            DownloadQueueStatus.Idle -> {
                CircularProgressIndicator(modifier = sizeModifier, strokeWidth = 2.5.dp)
            }
            DownloadQueueStatus.Running -> {
                val progress = item.progress ?: 0f
                CircularProgressIndicator(progress = { progress }, modifier = sizeModifier, strokeWidth = 2.5.dp)
            }
        }
        Spacer(Modifier.width(8.dp))
        val statusLabel =
            when (item.status) {
                DownloadQueueStatus.Idle -> strings.statusIdle
                DownloadQueueStatus.FetchingInfo -> strings.statusFetchingInfo
                DownloadQueueStatus.Ready -> strings.statusReady
                DownloadQueueStatus.Running -> strings.statusRunning
                DownloadQueueStatus.Completed -> strings.statusCompleted
                DownloadQueueStatus.Canceled -> strings.statusCanceled
                DownloadQueueStatus.Error -> strings.statusError
            }
        val progressPercent = item.progress?.let { " ${String.format("%.1f %%", it * 100f)}" } ?: ""
        val extra = if (item.progressText.isNotBlank()) " • ${item.progressText}" else ""
        Text(
            text = statusLabel + progressPercent + extra,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusPill(item: DownloadQueueItemState, strings: DownloadQueueStrings, modifier: Modifier = Modifier) {
    val text =
        when (item.status) {
            DownloadQueueStatus.Completed -> strings.statusCompleted
            DownloadQueueStatus.Error -> strings.statusError
            DownloadQueueStatus.Canceled -> strings.statusCanceled
            DownloadQueueStatus.Running -> strings.statusRunning
            DownloadQueueStatus.FetchingInfo -> strings.statusFetchingInfo
            DownloadQueueStatus.Ready -> strings.statusReady
            DownloadQueueStatus.Idle -> strings.statusIdle
        }
    Surface(color = StatusLabelContainerColor, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun DownloadOverlay(item: DownloadQueueItemState, modifier: Modifier = Modifier, onOverlayAction: (DownloadQueueAction) -> Unit) {
    val showProgress =
        item.status == DownloadQueueStatus.Running ||
            item.status == DownloadQueueStatus.FetchingInfo ||
            item.status == DownloadQueueStatus.Ready ||
            item.status == DownloadQueueStatus.Canceled ||
            item.status == DownloadQueueStatus.Completed
    if (!showProgress) return
    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        val ringColor = MaterialTheme.colorScheme.primary
        if (item.progress != null) {
            CircularProgressIndicator(
                progress = { if (item.status == DownloadQueueStatus.Completed) 1f else item.progress!! },
                strokeWidth = 4.dp,
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = Color.Transparent,
            )
        } else {
            CircularProgressIndicator(
                strokeWidth = 4.dp,
                modifier = Modifier.fillMaxSize(),
                color = ringColor,
                trackColor = Color.Transparent,
            )
        }
        val interaction = remember { MutableInteractionSource() }
        val action =
            when (item.status) {
                DownloadQueueStatus.Completed -> DownloadQueueAction.OpenFile
                DownloadQueueStatus.Canceled, DownloadQueueStatus.Error -> DownloadQueueAction.Resume
                else -> DownloadQueueAction.Cancel
            }
        val icon =
            when (action) {
                DownloadQueueAction.Resume -> Icons.Rounded.RestartAlt
                DownloadQueueAction.OpenFile -> Icons.Outlined.PlayArrow
                else -> Icons.Rounded.Pause
            }

        Surface(
            shape = CircleShape,
            tonalElevation = 3.dp,
            color = if (item.status == DownloadQueueStatus.Completed) MaterialTheme.colorScheme.tertiary else StatusLabelContainerColor,
            modifier = Modifier
                .size(64.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onOverlayAction(action) },
                ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
expect fun DownloadThumbnail(url: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop)

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
            modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(0.5f).widthIn(max = 240.dp),
        )
        Text(
            strings.emptyTitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
private fun LoadingPlaceholderShared(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ActionSheetShared(
    item: DownloadQueueItemState,
    strings: DownloadQueueStrings,
    onAction: (DownloadQueueAction) -> Unit,
    onDismiss: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        item {
            ActionSheetTitle(item = item, strings = strings)
        }

        item {
            LazyRow(
                modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButtonsForStatus(item = item, strings = strings, onAction = onAction, onDismiss = onDismiss)
            }
        }

        item {
            HorizontalDivider()
            Text(
                stringResource(Res.string.media_info),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }

        val sizeText = item.fileSizeApproxBytes?.let { formatSize(it) }
        val durationText = item.durationSeconds?.let { formatDuration(it) }
        val summary = listOfNotNull(sizeText, durationText).joinToString(separator = " · ")
        if (summary.isNotBlank()) {
            item {
                ActionSheetItemShared(
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Download, contentDescription = null) },
                    text = {
                        Text(summary, style = MaterialTheme.typography.titleSmall)
                    },
                )
            }
        }

        item.videoFormats?.forEachIndexed { index, fmt ->
            item {
                val fileSizeText = (fmt.fileSize ?: fmt.fileSizeApprox)?.let { formatSize(it) }
                val bitRateText = fmt.vbr?.let { formatBitrate(it) }
                val codecText = fmt.vcodec?.substringBefore(".")
                val title = "${stringResource(Res.string.video)} #${index + 1}: ${fmt.formatNote.orEmpty()}"
                val details =
                    listOf(codecText, fmt.resolution, bitRateText, fileSizeText)
                        .filterNot { it.isNullOrBlank() }
                        .joinToString(separator = " · ")

                ActionSheetItemShared(
                    leadingIcon = { Icon(imageVector = Icons.Outlined.VideoFile, contentDescription = null) },
                    text = {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        if (details.isNotBlank()) {
                            Text(details, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                )
            }
        }

        val audioFormats: List<com.junkfood.seal.util.Format> = buildList {
            item.videoFormats?.filter { it.containsAudio() }?.let { addAll(it) }
            item.audioOnlyFormats?.let { addAll(it) }
        }
        audioFormats.forEachIndexed { index, fmt ->
            item {
                val fileSizeText = (fmt.fileSize ?: fmt.fileSizeApprox)?.let { formatSize(it) }
                val bitRateText = fmt.abr?.let { formatBitrate(it) }
                val codecText = fmt.acodec?.substringBefore(".")
                val title = "${stringResource(Res.string.audio)} #${index + 1}: ${fmt.formatNote.orEmpty()}"
                val details =
                    listOf(codecText, bitRateText, fileSizeText)
                        .filterNot { it.isNullOrBlank() }
                        .joinToString(separator = " · ")

                ActionSheetItemShared(
                    leadingIcon = { Icon(imageVector = Icons.Outlined.AudioFile, contentDescription = null) },
                    text = {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        if (details.isNotBlank()) {
                            Text(details, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                )
            }
        }

        if (item.extractorKey.isNotBlank() || item.url.isNotBlank()) {
            item {
                ActionSheetItemShared(
                    leadingIcon = { Icon(imageVector = Icons.Outlined.Link, contentDescription = null) },
                    text = {
                        if (item.extractorKey.isNotBlank()) {
                            Text(item.extractorKey, style = MaterialTheme.typography.titleSmall)
                        }
                        if (item.url.isNotBlank()) {
                            Text(item.url, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                )
            }
        }

        if (item.errorMessage?.isNotBlank() == true) {
            item {
                ErrorInfoBlock(
                    message = item.errorMessage,
                    onCopy = { onAction(DownloadQueueAction.CopyError) },
                    copyLabel = strings.copyErrorLabel,
                )
            }
        }
    }
}

private fun LazyListScope.ActionButtonsForStatus(
    item: DownloadQueueItemState,
    strings: DownloadQueueStrings,
    onAction: (DownloadQueueAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val primaryActions = buildList {
        when (item.status) {
            DownloadQueueStatus.Completed -> {
                add(ActionSpec(strings.openFileLabel, Icons.Outlined.PlayArrow, DownloadQueueAction.OpenFile, ActionTone.Primary))
                add(ActionSpec(strings.shareFileLabel, Icons.Outlined.Share, DownloadQueueAction.ShareFile, ActionTone.Secondary))
            }
            DownloadQueueStatus.Error -> {
                add(ActionSpec(strings.resumeLabel, Icons.Outlined.RestartAlt, DownloadQueueAction.Resume, ActionTone.Tertiary))
                add(ActionSpec(strings.copyErrorLabel, Icons.Outlined.ErrorOutline, DownloadQueueAction.CopyError, ActionTone.Error))
            }
            DownloadQueueStatus.Canceled -> {
                add(ActionSpec(strings.resumeLabel, Icons.Outlined.RestartAlt, DownloadQueueAction.Resume, ActionTone.Tertiary))
            }
            else -> {
                add(ActionSpec(strings.cancelLabel, Icons.Outlined.Cancel, DownloadQueueAction.Cancel, ActionTone.Neutral))
            }
        }
        if (item.status == DownloadQueueStatus.Completed || item.status == DownloadQueueStatus.Canceled || item.status == DownloadQueueStatus.Error) {
            add(ActionSpec(strings.deleteLabel, Icons.Outlined.Delete, DownloadQueueAction.Delete, ActionTone.Outline))
        }
        add(ActionSpec(strings.copyUrlLabel, Icons.Outlined.ContentCopy, DownloadQueueAction.CopyVideoUrl, ActionTone.Outline))
        add(ActionSpec(strings.openUrlLabel, Icons.AutoMirrored.Outlined.OpenInNew, DownloadQueueAction.OpenVideoUrl, ActionTone.Outline))
        if (!item.thumbnailUrl.isNullOrBlank()) {
            add(ActionSpec(strings.openThumbLabel, Icons.Outlined.Image, DownloadQueueAction.OpenThumbnailUrl, ActionTone.Outline))
        }
        if (strings.showDetailsLabel.isNotBlank()) {
            add(ActionSpec(strings.showDetailsLabel, Icons.Outlined.MoreVert, DownloadQueueAction.ShowDetails, ActionTone.Outline))
        }
    }

    primaryActions.forEach { action ->
        item {
            ActionPrimaryButtonShared(
                label = action.label,
                icon = action.icon,
                tone = action.tone,
            ) {
                onAction(action.action)
                onDismiss()
            }
        }
    }
}

private data class ActionSpec(
    val label: String,
    val icon: ImageVector,
    val action: DownloadQueueAction,
    val tone: ActionTone,
)

private enum class ActionTone {
    Primary,
    Secondary,
    Tertiary,
    Neutral,
    Error,
    Outline,
}

@Composable
private fun ActionPrimaryButtonShared(
    label: String,
    icon: ImageVector,
    tone: ActionTone,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val (containerColor, contentColor, outlineColor) =
        when (tone) {
            ActionTone.Primary ->
                Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    Color.Unspecified,
                )
            ActionTone.Secondary ->
                Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    Color.Unspecified,
                )
            ActionTone.Tertiary ->
                Triple(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    Color.Unspecified,
                )
            ActionTone.Error ->
                Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    Color.Unspecified,
                )
            ActionTone.Outline ->
                Triple(
                    Color.Transparent,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.outlineVariant,
                )
            ActionTone.Neutral ->
                Triple(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    MaterialTheme.colorScheme.onSurface,
                    Color.Unspecified,
                )
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .widthIn(min = 88.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onClick, indication = null, interactionSource = interactionSource)
                .padding(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .width(80.dp)
                    .height(64.dp)
                    .clip(CircleShape)
                    .then(
                        if (outlineColor != Color.Unspecified) {
                            Modifier.border(width = 1.dp, color = outlineColor, shape = CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .background(containerColor)
                    .indication(interactionSource, indication = LocalIndication.current),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp).align(Alignment.Center),
                tint = contentColor,
            )
        }
        Spacer(Modifier.height(8.dp))
        ProvideTextStyle(LocalTextStyle.current.merge(MaterialTheme.typography.labelMedium)) {
            Text(label)
        }
    }
}

@Composable
private fun ActionSheetItemShared(
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    text: @Composable (ColumnScope.() -> Unit),
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(vertical = 16.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.invoke()
        if (leadingIcon != null) Spacer(Modifier.width(20.dp))
        ProvideTextStyle(LocalTextStyle.current.merge(MaterialTheme.typography.titleSmall)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                text.invoke(this)
            }
        }
    }
}

@Composable
private fun ErrorInfoBlock(
    message: String,
    copyLabel: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopy) {
                    Text(copyLabel)
                }
            }
        }
    }
}

@Composable
private fun ActionSheetTitle(item: DownloadQueueItemState, strings: DownloadQueueStrings) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = item.title.ifBlank { item.url },
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.author.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressText(item = item, strings = strings)
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun formatSize(bytes: Double): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.0f KB", bytes / kb)
        else -> String.format("%.0f B", bytes)
    }
}

private fun formatBitrate(kbps: Double): String {
    return if (kbps >= 1000) String.format("%.2f Mbps", kbps / 1000.0) else String.format("%.0f Kbps", kbps)
}
