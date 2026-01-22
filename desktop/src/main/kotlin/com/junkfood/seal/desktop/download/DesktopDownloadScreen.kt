@file:OptIn(ExperimentalLayoutApi::class, org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.junkfood.seal.desktop.download

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.junkfood.seal.desktop.settings.desktopDefaultPreferences
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.ui.download.queue.DownloadQueueAction
import com.junkfood.seal.ui.download.queue.DownloadQueueFilter
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueScreenShared
import com.junkfood.seal.ui.download.queue.DownloadQueueState
import com.junkfood.seal.ui.download.queue.DownloadQueueStrings
import com.junkfood.seal.ui.download.queue.DownloadQueueViewMode
import com.junkfood.seal.util.DownloadPreferences
import java.awt.Desktop
import java.io.File
import java.net.URI
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.additional_settings
import com.junkfood.seal.shared.generated.resources.all
import com.junkfood.seal.shared.generated.resources.auto
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.best_quality
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.download
import com.junkfood.seal.shared.generated.resources.download_hint
import com.junkfood.seal.shared.generated.resources.download_queue
import com.junkfood.seal.shared.generated.resources.download_subtitles
import com.junkfood.seal.shared.generated.resources.embed_metadata
import com.junkfood.seal.shared.generated.resources.lowest_quality
import com.junkfood.seal.shared.generated.resources.new_task
import com.junkfood.seal.shared.generated.resources.paste_msg
import com.junkfood.seal.shared.generated.resources.playlist
import com.junkfood.seal.shared.generated.resources.proceed
import com.junkfood.seal.shared.generated.resources.preset
import com.junkfood.seal.shared.generated.resources.reset
import com.junkfood.seal.shared.generated.resources.settings_before_download
import com.junkfood.seal.shared.generated.resources.sponsorblock
import com.junkfood.seal.shared.generated.resources.start_download
import com.junkfood.seal.shared.generated.resources.status_canceled
import com.junkfood.seal.shared.generated.resources.status_completed
import com.junkfood.seal.shared.generated.resources.status_downloading
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.shared.generated.resources.video_url
import com.junkfood.seal.shared.generated.resources.you_ll_find_your_downloads_here
import com.junkfood.seal.shared.generated.resources.desktop_view_grid
import com.junkfood.seal.shared.generated.resources.desktop_view_list

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DesktopDownloadScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = true,
    disablePreview: Boolean = false,
    preferences: DownloadPreferences,
    onPreferencesChange: (DownloadPreferences) -> Unit,
    controller: DesktopDownloadController,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var showSheet by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(DownloadSheetPage.Input) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputUrl by remember { mutableStateOf("") }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var downloadType by remember { mutableStateOf(DesktopDownloadType.Video) }
    var workingPreferences by remember { mutableStateOf(preferences) }
    LaunchedEffect(preferences) { workingPreferences = preferences }
    val logLines = controller.logLines

    val queueStrings =
        DownloadQueueStrings(
            queueTitle = stringResource(Res.string.download_queue),
            addLabel = stringResource(Res.string.download),
            filterAll = stringResource(Res.string.all),
            filterDownloading = stringResource(Res.string.status_downloading),
            filterCanceled = stringResource(Res.string.status_canceled),
            filterFinished = stringResource(Res.string.status_completed),
            emptyTitle = stringResource(Res.string.you_ll_find_your_downloads_here),
            emptyBody = stringResource(Res.string.download_hint),
            gridLabel = stringResource(Res.string.desktop_view_grid),
            listLabel = stringResource(Res.string.desktop_view_list),
        )

    Column(modifier = modifier.fillMaxHeight()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val itemsForUi =
                remember(controller.queueItems, disablePreview) {
                    if (!disablePreview) controller.queueItems
                    else controller.queueItems.map { it.copy(thumbnailUrl = null) }
                }

            DownloadQueueScreenShared(
                state = DownloadQueueState(items = itemsForUi, filter = controller.filter, viewMode = controller.viewMode),
                strings = queueStrings,
                onFilterChange = { controller.filter = it },
                onViewModeChange = { controller.viewMode = it },
                onItemAction = { itemId, action ->
                    val item = controller.queueItems.firstOrNull { it.id == itemId }
                    when (action) {
                        DownloadQueueAction.Cancel -> controller.cancelIfRunning(itemId)
                        DownloadQueueAction.Delete -> controller.deleteQueueItem(itemId)
                        DownloadQueueAction.Resume -> { /* not supported */ }
                        DownloadQueueAction.OpenFile -> {
                            item?.filePath?.let { safeOpenFile(it) }
                        }
                        DownloadQueueAction.ShareFile -> {
                            // Desktop: no share sheet for now; best-effort open folder.
                            item?.filePath?.let { safeRevealInFolder(it) }
                        }
                        DownloadQueueAction.CopyVideoUrl -> {
                            item?.url?.takeIf { it.isNotBlank() }?.let { clipboard.setText(AnnotatedString(it)) }
                        }
                        DownloadQueueAction.OpenVideoUrl -> {
                            item?.url?.takeIf { it.isNotBlank() }?.let { safeBrowse(it) }
                        }
                        DownloadQueueAction.OpenThumbnailUrl -> {
                            if (!disablePreview) {
                                item?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { safeBrowse(it) }
                            }
                        }
                        DownloadQueueAction.CopyError -> {
                            item?.errorMessage?.takeIf { it.isNotBlank() }?.let { clipboard.setText(AnnotatedString(it)) }
                        }
                        DownloadQueueAction.ShowDetails -> {
                            // TODO: add a detail dialog; keep no-op for now.
                        }
                    }
                },
                onAddClick = {
                    sheetPage = DownloadSheetPage.Input
                    showSheet = true
                },
                onMenuClick = onMenuClick,
                isCompact = isCompact,
                showAddButton = false,
                showMenuButton = isCompact,
            )

            FloatingActionButton(
                onClick = {
                    sheetPage = DownloadSheetPage.Input
                    showSheet = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(Res.string.start_download))
            }
        }

        logLines.lastOrNull()?.let { LatestLogRow(it) }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showSheet = false
                    sheetPage = DownloadSheetPage.Input
                    pendingUrl = null
                }
            },
            sheetState = sheetState,
        ) {
            AnimatedContent(
                targetState = sheetPage,
                transitionSpec = {
                    val forward = initialState == DownloadSheetPage.Input && targetState == DownloadSheetPage.Options
                    val slideIn = slideInHorizontally(animationSpec = tween(220)) { fullWidth -> if (forward) fullWidth / 2 else -fullWidth / 2 }
                    val slideOut = slideOutHorizontally(animationSpec = tween(220)) { fullWidth -> if (forward) -fullWidth / 2 else fullWidth / 2 }
                    (slideIn + fadeIn(animationSpec = tween(220))).togetherWith(slideOut + fadeOut(animationSpec = tween(180)))
                },
                label = "downloadSheet",
            ) { page ->
                when (page) {
                    DownloadSheetPage.Input ->
                        DownloadInputSheet(
                            url = inputUrl,
                            onUrlChange = { inputUrl = it },
                            onPasteIntoUrl = { pasted -> inputUrl = pasted },
                            onDismissRequest = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    pendingUrl = null
                                    sheetPage = DownloadSheetPage.Input
                                }
                            },
                            onProceed = {
                                pendingUrl = inputUrl
                                sheetPage = DownloadSheetPage.Options
                            },
                        )

                    DownloadSheetPage.Options ->
                        DownloadOptionsSheet(
                            urlPreview = pendingUrl.orEmpty(),
                            downloadType = downloadType,
                            onTypeChange = { newType ->
                                downloadType = newType
                                val updated = preferencesForType(workingPreferences, newType)
                                workingPreferences = updated
                                onPreferencesChange(updated)
                            },
                            preferences = workingPreferences,
                            onPreferencesChange = {
                                workingPreferences = it
                                onPreferencesChange(it)
                            },
                            onDismissRequest = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    pendingUrl = null
                                    sheetPage = DownloadSheetPage.Input
                                }
                            },
                            onDownload = {
                                pendingUrl?.let { url ->
                                    controller.startDownload(url, downloadType, workingPreferences)
                                }
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    pendingUrl = null
                                    showSheet = false
                                    sheetPage = DownloadSheetPage.Input
                                    inputUrl = ""
                                }
                            },
                        )
                }
            }
        }
    }
}

private enum class DownloadSheetPage {
    Input,
    Options,
}

private fun safeBrowse(url: String) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        Desktop.getDesktop().browse(URI(url))
    }
}

private fun safeOpenFile(path: String) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return
        Desktop.getDesktop().open(File(path))
    }
}

private fun safeRevealInFolder(path: String) {
    runCatching {
        val file = File(path)
        val dir = if (file.isDirectory) file else file.parentFile
        if (dir != null) safeOpenFile(dir.absolutePath)
    }
}

@Composable
private fun DownloadInputSheet(
    url: String,
    onUrlChange: (String) -> Unit,
    onPasteIntoUrl: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onProceed: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Text(stringResource(Res.string.new_task), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(stringResource(Res.string.video_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    val clip = clipboard.getText()?.text
                    if (!clip.isNullOrBlank()) onPasteIntoUrl(clip)
                },
            ) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.paste_msg))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismissRequest) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.cancel))
                }
                Button(onClick = onProceed, enabled = url.isNotBlank()) {
                    Text(stringResource(Res.string.proceed))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun DownloadOptionsSheet(
    urlPreview: String,
    downloadType: DesktopDownloadType,
    onTypeChange: (DesktopDownloadType) -> Unit,
    preferences: DownloadPreferences,
    onPreferencesChange: (DownloadPreferences) -> Unit,
    onDismissRequest: () -> Unit,
    onDownload: () -> Unit,
) {
    var showAdvanced by remember { mutableStateOf(false) }
    val formatSummary = formatSummary(preferences, downloadType)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings_before_download), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(urlPreview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        DownloadTypeSegmentedRow(selected = downloadType, onSelect = onTypeChange)

        FormatPresetCard(summary = formatSummary) {
            onPreferencesChange(preferencesForType(desktopDefaultPreferences(), downloadType))
        }

        HorizontalDivider()

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showAdvanced = !showAdvanced }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.additional_settings), style = MaterialTheme.typography.titleSmall)
            Icon(if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }

        if (showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionToggleRow(
                    title = stringResource(Res.string.embed_metadata),
                    checked = preferences.embedMetadata,
                    onCheckedChange = { onPreferencesChange(preferences.copy(embedMetadata = it)) },
                )
                OptionToggleRow(
                    title = stringResource(Res.string.download_subtitles),
                    checked = preferences.downloadSubtitle,
                    onCheckedChange = { onPreferencesChange(preferences.copy(downloadSubtitle = it, embedSubtitle = it && preferences.embedSubtitle)) },
                )
                OptionToggleRow(
                    title = stringResource(Res.string.sponsorblock),
                    checked = preferences.sponsorBlock,
                    onCheckedChange = { onPreferencesChange(preferences.copy(sponsorBlock = it)) },
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDismissRequest) {
                Icon(Icons.Outlined.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.cancel))
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onDownload) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.start_download))
            }
        }
    }
}

@Composable
private fun DownloadTypeSegmentedRow(selected: DesktopDownloadType, onSelect: (DesktopDownloadType) -> Unit) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, outline),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            DesktopDownloadType.entries.forEachIndexed { index, type ->
                val isSelected = selected == type
                val icon = when (type) {
                    DesktopDownloadType.Audio -> Icons.Outlined.LibraryMusic
                    DesktopDownloadType.Video -> Icons.Outlined.VideoFile
                    DesktopDownloadType.Playlist -> Icons.Outlined.PlaylistAdd
                }
                val label =
                    when (type) {
                        DesktopDownloadType.Audio -> stringResource(Res.string.audio)
                        DesktopDownloadType.Video -> stringResource(Res.string.video)
                        DesktopDownloadType.Playlist -> stringResource(Res.string.playlist)
                    }
                val segmentShape =
                    when (index) {
                        0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                        DesktopDownloadType.entries.lastIndex ->
                            RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                val background = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                val contentColor =
                    if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(segmentShape)
                        .clickable { onSelect(type) }
                        .background(background)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = contentColor)
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(icon, contentDescription = null, tint = contentColor)
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = contentColor)
                    }
                }

                if (index != DesktopDownloadType.entries.lastIndex) {
                    VerticalDivider(color = outline)
                }
            }
        }
    }
}

@Composable
private fun FormatPresetCard(summary: String, onReset: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        onClick = onReset,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.SettingsSuggest, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.preset), style = MaterialTheme.typography.titleSmall)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onReset, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text(stringResource(Res.string.reset)) }
        }
    }
}

@Composable
private fun OptionToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RunningNowBanner(item: DownloadQueueItemState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title.ifBlank { item.url }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val fallbackStatusText = stringResource(Res.string.status_downloading)
                val statusText = if (item.progressText.isBlank()) fallbackStatusText else item.progressText
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LatestLogRow(line: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            text = line,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun formatSummary(preferences: DownloadPreferences, type: DesktopDownloadType): String {
    val resLabel = videoResolutionLabel(preferences.videoResolution)
    val audioLabel = audioQualityLabel(preferences.audioQuality)
    return when (type) {
        DesktopDownloadType.Audio -> "${stringResource(Res.string.audio)} · $audioLabel"
        DesktopDownloadType.Video -> "${stringResource(Res.string.video)} · $resLabel"
        DesktopDownloadType.Playlist -> "${stringResource(Res.string.playlist)} · $resLabel"
    }
}

@Composable
private fun videoResolutionLabel(code: Int): String =
    when (code) {
        0 -> stringResource(Res.string.best_quality)
        1 -> "2160p"
        2 -> "1440p"
        3 -> "1080p"
        4 -> "720p"
        5 -> "480p"
        6 -> "360p"
        7 -> stringResource(Res.string.lowest_quality)
        else -> stringResource(Res.string.auto)
    }

@Composable
private fun audioQualityLabel(code: Int): String =
    when (code) {
        1 -> stringResource(Res.string.best_quality)
        2 -> "Medium"
        3 -> "Low"
        4 -> "Very low"
        else -> stringResource(Res.string.auto)
    }
