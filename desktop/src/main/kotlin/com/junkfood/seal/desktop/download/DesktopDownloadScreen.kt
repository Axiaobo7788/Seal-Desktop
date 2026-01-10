@file:OptIn(ExperimentalLayoutApi::class, org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.junkfood.seal.desktop.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.desktopDefaultPreferences
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataFetcher
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.ui.download.queue.DownloadQueueAction
import com.junkfood.seal.ui.download.queue.DownloadQueueFilter
import com.junkfood.seal.ui.download.queue.DownloadQueueItemState
import com.junkfood.seal.ui.download.queue.DownloadQueueScreenShared
import com.junkfood.seal.ui.download.queue.DownloadQueueState
import com.junkfood.seal.ui.download.queue.DownloadQueueStrings
import com.junkfood.seal.ui.download.queue.DownloadQueueViewMode
import com.junkfood.seal.ui.download.queue.DownloadQueueStatus
import com.junkfood.seal.ui.download.queue.DownloadQueueMediaType
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private enum class DesktopDownloadType { Audio, Video, Playlist }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DesktopDownloadScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = true,
    preferences: DownloadPreferences,
    onPreferencesChange: (DownloadPreferences) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val executor = remember { DownloadPlanExecutor() }
    val metadataFetcher = remember { YtDlpMetadataFetcher() }

    var filter by remember { mutableStateOf(DownloadQueueFilter.All) }
    var viewMode by remember { mutableStateOf(DownloadQueueViewMode.Grid) }
    val queueItems = remember { mutableStateListOf<DownloadQueueItemState>() }

    var showInputSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputUrl by remember { mutableStateOf("") }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var downloadType by remember { mutableStateOf(DesktopDownloadType.Video) }
    var workingPreferences by remember { mutableStateOf(preferences) }
    LaunchedEffect(preferences) { workingPreferences = preferences }
    var runningProcess by remember { mutableStateOf<DownloadPlanExecutor.RunningProcess?>(null) }
    var runningItemId by remember { mutableStateOf<String?>(null) }
    val logLines = remember { mutableStateListOf<String>() }

    fun appendLog(line: String) {
        logLines.add(line)
        if (logLines.size > 200) {
            logLines.removeFirst()
        }
    }

    fun updateItem(itemId: String, transform: (DownloadQueueItemState) -> DownloadQueueItemState) {
        val index = queueItems.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            queueItems[index] = transform(queueItems[index])
        }
    }

    fun startDownload(url: String, type: DesktopDownloadType, basePreferences: DownloadPreferences) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val itemId = System.currentTimeMillis().toString()
        val mediaType = if (type == DesktopDownloadType.Audio) DownloadQueueMediaType.Audio else DownloadQueueMediaType.Video
        val effectivePreferences = preferencesForType(basePreferences, type)

        queueItems.add(
            DownloadQueueItemState(
                id = itemId,
                title = trimmed,
                url = trimmed,
                mediaType = mediaType,
                status = DownloadQueueStatus.FetchingInfo,
            ),
        )

        scope.launch {
            appendLog("start: $trimmed [${type.name.lowercase()}]")

            val videoInfo =
                try {
                    withContext(Dispatchers.IO) { metadataFetcher.fetch(trimmed) }
                } catch (e: Exception) {
                    appendLog("metadata failed: ${e.message}")
                    VideoInfo(originalUrl = trimmed, webpageUrl = trimmed, title = trimmed)
                }

            updateItem(itemId) {
                it.copy(
                    title = videoInfo.title.ifBlank { trimmed },
                    author = videoInfo.uploader.orEmpty(),
                    status = DownloadQueueStatus.Ready,
                )
            }

            val plan = buildDownloadPlan(videoInfo, effectivePreferences, playlistUrl = trimmed, playlistItem = if (type == DesktopDownloadType.Playlist) 0 else 0)

            try {
                runningItemId = itemId
                updateItem(itemId) { it.copy(status = DownloadQueueStatus.Running, progressText = "") }

                val proc =
                    withContext(Dispatchers.IO) {
                        executor.start(
                            plan,
                            executor.defaultConfigFor(plan, url = trimmed, paths = DesktopYtDlpPaths),
                            onStdout = { appendLog(it) },
                            onStderr = { appendLog("[err] $it") },
                        )
                    }

                runningProcess = proc
                val result = withContext(Dispatchers.IO) { proc.waitForResult() }

                updateItem(itemId) {
                    it.copy(
                        status = if (result.exitCode == 0) DownloadQueueStatus.Completed else DownloadQueueStatus.Error,
                        progressText = "Exit code ${result.exitCode}",
                    )
                }
            } catch (e: Exception) {
                appendLog("download failed: ${e.message}")
                updateItem(itemId) { it.copy(status = DownloadQueueStatus.Error, errorMessage = e.message, progressText = e.message.orEmpty()) }
            } finally {
                runningProcess = null
                runningItemId = null
            }
        }
    }

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
            gridLabel = "Grid",
            listLabel = "List",
        )

    Column(modifier = modifier.fillMaxHeight()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            DownloadQueueScreenShared(
                state = DownloadQueueState(items = queueItems, filter = filter, viewMode = viewMode),
                strings = queueStrings,
                onFilterChange = { filter = it },
                onViewModeChange = { viewMode = it },
                onItemAction = { itemId, action ->
                    when (action) {
                        DownloadQueueAction.Cancel -> {
                            if (itemId == runningItemId) {
                                runningProcess?.cancel()
                                updateItem(itemId) { it.copy(status = DownloadQueueStatus.Canceled) }
                            }
                        }
                        DownloadQueueAction.Delete -> queueItems.removeAll { it.id == itemId }
                        DownloadQueueAction.Resume -> { /* not supported */ }
                        DownloadQueueAction.OpenFile,
                        DownloadQueueAction.ShareFile,
                        DownloadQueueAction.CopyVideoUrl,
                        DownloadQueueAction.OpenVideoUrl,
                        DownloadQueueAction.OpenThumbnailUrl,
                        DownloadQueueAction.CopyError,
                        DownloadQueueAction.ShowDetails -> {
                            // Not implemented on desktop for now
                        }
                    }
                },
                onAddClick = { showInputSheet = true },
                onMenuClick = onMenuClick,
                isCompact = isCompact,
                showAddButton = false,
                showMenuButton = isCompact,
            )

            FloatingActionButton(
                onClick = { showInputSheet = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(Res.string.start_download))
            }
        }

        logLines.lastOrNull()?.let { LatestLogRow(it) }
    }

    if (showInputSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInputSheet = false },
            sheetState = sheetState,
        ) {
            DownloadInputSheet(
                url = inputUrl,
                onUrlChange = { inputUrl = it },
                onPasteIntoUrl = { pasted -> inputUrl = pasted },
                onDismissRequest = { showInputSheet = false },
                onConfirm = {
                    pendingUrl = inputUrl
                    showInputSheet = false
                    showOptionsSheet = true
                },
            )
        }
    }

    if (showOptionsSheet && pendingUrl != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showOptionsSheet = false
                pendingUrl = null
            },
            sheetState = sheetState,
        ) {
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
                    showOptionsSheet = false
                    pendingUrl = null
                },
                onDownload = {
                    pendingUrl?.let { url -> startDownload(url, downloadType, workingPreferences) }
                    pendingUrl = null
                    showOptionsSheet = false
                    inputUrl = ""
                },
            )
        }
    }
}

@Composable
private fun DownloadInputSheet(
    url: String,
    onUrlChange: (String) -> Unit,
    onPasteIntoUrl: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.new_task), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(stringResource(Res.string.video_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                val clip = clipboard.getText()?.text
                if (!clip.isNullOrBlank()) onPasteIntoUrl(clip)
            }) { Text(stringResource(Res.string.paste_msg)) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
                Button(onClick = onConfirm, enabled = url.isNotBlank()) { Text(stringResource(Res.string.proceed)) }
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

        TypeSelectorRow(selected = downloadType, onSelect = onTypeChange)

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
            TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
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
private fun TypeSelectorRow(selected: DesktopDownloadType, onSelect: (DesktopDownloadType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DesktopDownloadType.entries.forEach { type ->
            val chosen = selected == type
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

            if (chosen) {
                FilledTonalButton(onClick = { onSelect(type) }) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(label)
                }
            } else {
                OutlinedButton(onClick = { onSelect(type) }) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(label)
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
                val statusText = item.progressText.ifBlank { stringResource(Res.string.status_downloading) }
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

private fun preferencesForType(base: DownloadPreferences, type: DesktopDownloadType): DownloadPreferences =
    base.copy(
        extractAudio = type == DesktopDownloadType.Audio,
        downloadPlaylist = type == DesktopDownloadType.Playlist,
        subdirectoryPlaylistTitle = if (type == DesktopDownloadType.Playlist) true else base.subdirectoryPlaylistTitle,
        embedMetadata = if (type == DesktopDownloadType.Audio) true else base.embedMetadata,
    )

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
