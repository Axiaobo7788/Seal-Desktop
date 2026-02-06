@file:OptIn(ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.ui.page.downloadv2.configure

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.download.DesktopDownloadController
import com.junkfood.seal.desktop.download.DesktopDownloadType
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.back
import com.junkfood.seal.shared.generated.resources.format_selection
import com.junkfood.seal.shared.generated.resources.show_all_items
import com.junkfood.seal.shared.generated.resources.start_download
import com.junkfood.seal.shared.generated.resources.suggested
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.shared.generated.resources.video_only
import com.junkfood.seal.ui.download.queue.DownloadThumbnail
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.connectWithDelimiter
import com.junkfood.seal.util.toHttpsUrl
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

private const val NotSelected = -1

@Composable
internal fun CustomFormatSelectionSheet(
    url: String,
    controller: DesktopDownloadController,
    downloadType: DesktopDownloadType,
    basePreferences: DownloadPreferences,
    onBack: () -> Unit,
    onDownloadComplete: () -> Unit,
) {
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(url, reloadToken) {
        videoInfo = null
        error = null
        if (url.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        val result = controller.fetchVideoInfo(url)
        result.onSuccess {
            videoInfo = it
            isLoading = false
        }.onFailure {
            error = it.message
            isLoading = false
        }
    }

    when {
        isLoading ->
            LoadingState(onBack = onBack)
        error != null ->
            ErrorState(message = error.orEmpty(), onRetry = { reloadToken += 1 }, onBack = onBack)
        videoInfo != null ->
            FormatPageImpl(
                videoInfo = videoInfo!!,
                allowMultiAudio = basePreferences.mergeAudioStream,
                onNavigateBack = onBack,
                onDownloadPressed = { formats ->
                    controller.startDownloadWithSelection(
                        url = url,
                        type = downloadType,
                        basePreferences = basePreferences,
                        videoInfo = videoInfo!!,
                        formatList = formats,
                    )
                    onDownloadComplete()
                },
            )
        else ->
            EmptyState(onBack = onBack)
    }
}

@Composable
private fun LoadingState(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.format_selection)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text("正在获取视频信息...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.format_selection)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Outlined.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("加载失败：$message", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRetry) { Text("重试") }
                    Button(onClick = onBack) { Text("返回") }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.format_selection)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("未获取到可用链接", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onBack) { Text("返回") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPageImpl(
    videoInfo: VideoInfo,
    allowMultiAudio: Boolean,
    onNavigateBack: () -> Unit,
    onDownloadPressed: (List<Format>) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val formats = videoInfo.formats.orEmpty()
    if (formats.isEmpty()) return

    val videoOnlyFormats = formats.filter { it.containsVideo() && it.isVideoOnly() }.reversed()
    val audioOnlyFormats = formats.filter { it.containsAudio() && it.isAudioOnly() }.reversed()
    val videoAudioFormats = formats.filter { it.containsAudio() && it.containsVideo() }.reversed()

    val duration = videoInfo.duration ?: 0.0

    var videoOnlyItemLimit by remember { mutableIntStateOf(6) }
    var audioOnlyItemLimit by remember { mutableIntStateOf(6) }
    var videoAudioItemLimit by remember { mutableIntStateOf(6) }

    val isSuggestedFormatAvailable =
        !videoInfo.requestedFormats.isNullOrEmpty() || !videoInfo.requestedDownloads.isNullOrEmpty()
    var isSuggestedFormatSelected by remember { mutableStateOf(isSuggestedFormatAvailable) }

    var selectedVideoAudioFormat by remember { mutableIntStateOf(NotSelected) }
    var selectedVideoOnlyFormat by remember { mutableIntStateOf(NotSelected) }
    val selectedAudioOnlyFormats = remember { mutableStateListOf<Int>() }

    val lazyGridState = rememberLazyGridState()
    val isFabExpanded by remember { derivedStateOf { lazyGridState.firstVisibleItemIndex > 0 } }

    val selectedFormats: List<Format> by remember {
        derivedStateOf {
            mutableListOf<Format>().apply {
                if (isSuggestedFormatSelected) {
                    videoInfo.requestedFormats?.let { addAll(it) }
                        ?: videoInfo.requestedDownloads?.forEach { it.requestedFormats?.let { addAll(it) } }
                } else {
                    selectedAudioOnlyFormats.forEach { index -> add(audioOnlyFormats.elementAt(index)) }
                    videoAudioFormats.getOrNull(selectedVideoAudioFormat)?.let { add(it) }
                    videoOnlyFormats.getOrNull(selectedVideoOnlyFormat)?.let { add(it) }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.format_selection)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(Res.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (selectedFormats.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onDownloadPressed(selectedFormats) },
                    icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                    text = { Text(stringResource(Res.string.start_download)) },
                    expanded = isFabExpanded,
                )
            }
        },
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier.padding(paddingValues),
            state = lazyGridState,
            columns = GridCells.Adaptive(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FormatPreviewHeader(videoInfo = videoInfo)
            }

            if (isSuggestedFormatAvailable) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionTitle(stringResource(Res.string.suggested))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SuggestedFormatCard(
                        videoInfo = videoInfo,
                        selected = isSuggestedFormatSelected,
                        onClick = {
                            isSuggestedFormatSelected = true
                            selectedAudioOnlyFormats.clear()
                            selectedVideoAudioFormat = NotSelected
                            selectedVideoOnlyFormat = NotSelected
                        },
                    )
                }
            }

            if (audioOnlyFormats.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionRow(
                        title = stringResource(Res.string.audio),
                        icon = Icons.Outlined.AudioFile,
                        showAll = audioOnlyItemLimit < audioOnlyFormats.size,
                        showAllText = stringResource(Res.string.show_all_items, audioOnlyFormats.size),
                        onShowAll = { audioOnlyItemLimit = Int.MAX_VALUE },
                    )
                }
                itemsIndexed(audioOnlyFormats.subList(0, min(audioOnlyItemLimit, audioOnlyFormats.size))) { index, format ->
                    FormatItemCard(
                        formatInfo = format,
                        duration = duration,
                        selected = selectedAudioOnlyFormats.contains(index),
                        outlineColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        if (selectedAudioOnlyFormats.contains(index)) {
                            selectedAudioOnlyFormats.remove(index)
                        } else {
                            if (!allowMultiAudio) {
                                selectedAudioOnlyFormats.clear()
                            }
                            isSuggestedFormatSelected = false
                            selectedAudioOnlyFormats.add(index)
                        }
                    }
                }
            }

            if (videoOnlyFormats.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionRow(
                        title = stringResource(Res.string.video_only),
                        icon = Icons.Outlined.Videocam,
                        showAll = videoOnlyItemLimit < videoOnlyFormats.size,
                        showAllText = stringResource(Res.string.show_all_items, videoOnlyFormats.size),
                        onShowAll = { videoOnlyItemLimit = Int.MAX_VALUE },
                    )
                }
                itemsIndexed(videoOnlyFormats.subList(0, min(videoOnlyItemLimit, videoOnlyFormats.size))) { index, format ->
                    FormatItemCard(
                        formatInfo = format,
                        duration = duration,
                        selected = selectedVideoOnlyFormat == index,
                        outlineColor = MaterialTheme.colorScheme.tertiary,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        selectedVideoOnlyFormat =
                            if (selectedVideoOnlyFormat == index) NotSelected
                            else {
                                isSuggestedFormatSelected = false
                                selectedVideoAudioFormat = NotSelected
                                index
                            }
                    }
                }
            }

            if (videoAudioFormats.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionRow(
                        title = stringResource(Res.string.video),
                        icon = Icons.Outlined.Movie,
                        showAll = videoAudioItemLimit < videoAudioFormats.size,
                        showAllText = stringResource(Res.string.show_all_items, videoAudioFormats.size),
                        onShowAll = { videoAudioItemLimit = Int.MAX_VALUE },
                    )
                }
                itemsIndexed(videoAudioFormats.subList(0, min(videoAudioItemLimit, videoAudioFormats.size))) { index, format ->
                    FormatItemCard(
                        formatInfo = format,
                        duration = duration,
                        selected = selectedVideoAudioFormat == index,
                    ) {
                        selectedVideoAudioFormat =
                            if (selectedVideoAudioFormat == index) NotSelected
                            else {
                                isSuggestedFormatSelected = false
                                selectedAudioOnlyFormats.clear()
                                selectedVideoOnlyFormat = NotSelected
                                index
                            }
                    }
                }
            }

            if (audioOnlyFormats.isNotEmpty() && videoOnlyFormats.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FormatHintInfo(text = "音频与无音轨视频可组合下载")
                }
            }

            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun FormatPreviewHeader(videoInfo: VideoInfo) {
    val thumbnailUrl = videoInfo.thumbnail.toHttpsUrl()
    val durationText = formatDuration(videoInfo.duration?.toInt() ?: 0)
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 200.dp, height = 112.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            DownloadThumbnail(url = thumbnailUrl, modifier = Modifier.fillMaxSize())
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = videoInfo.title.ifBlank { videoInfo.webpageUrlBasename.orEmpty() },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            videoInfo.uploader?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
private fun SectionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showAll: Boolean,
    showAllText: String,
    onShowAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        if (showAll) {
            Text(
                text = showAllText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onShowAll() },
            )
        }
    }
}

@Composable
private fun FormatHintInfo(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SuggestedFormatCard(
    videoInfo: VideoInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val requestedFormats =
        videoInfo.requestedFormats
            ?: videoInfo.requestedDownloads?.flatMap { it.requestedFormats.orEmpty() }
            ?: emptyList()
    if (requestedFormats.isEmpty()) return

    val duration = videoInfo.duration ?: 0.0
    val containsVideo = requestedFormats.any { it.containsVideo() }
    val containsAudio = requestedFormats.any { it.containsAudio() }
    val title = requestedFormats.joinToString(separator = " + ") { it.format ?: it.formatId.orEmpty() }

    val totalFileSize =
        requestedFormats.fold(0.0) { acc, format ->
            acc + (format.fileSize ?: format.fileSizeApprox ?: (duration * (format.tbr ?: 0.0) * 125))
        }
    val fileSizeText = totalFileSize.toFileSizeText()

    val totalTbr = requestedFormats.fold(0.0) { acc, format -> acc + (format.tbr ?: 0.0) }
    val tbrText = totalTbr.toBitrateText()

    val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")
    val codecText = connectWithDelimiter(videoInfo.ext, buildCodecText(videoInfo.vcodec, videoInfo.acodec), delimiter = " ").uppercase()

    FormatItemCard(
        title = title,
        containsAudio = containsAudio,
        containsVideo = containsVideo,
        firstLineText = firstLineText,
        secondLineText = codecText,
        selected = selected,
        outlineColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormatItemCard(
    formatInfo: Format,
    duration: Double,
    selected: Boolean,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit,
) {
    val vcodecText = formatInfo.vcodec?.substringBefore(".") ?: ""
    val acodecText = formatInfo.acodec?.substringBefore(".") ?: ""
    val codec = connectWithDelimiter(vcodecText, acodecText, delimiter = " ").run { if (isNotBlank()) "($this)" else this }
    val tbrText = (formatInfo.tbr ?: 0.0).toBitrateText()
    val fileSizeValue = formatInfo.fileSize ?: formatInfo.fileSizeApprox ?: (formatInfo.tbr ?: 0.0) * duration * 125
    val fileSizeText = fileSizeValue.toFileSizeText()

    val firstLineText = connectWithDelimiter(fileSizeText, tbrText, delimiter = " ")
    val secondLineText = connectWithDelimiter(formatInfo.ext, codec, delimiter = " ").uppercase()

    FormatItemCard(
        title = formatInfo.format ?: formatInfo.formatId.orEmpty(),
        containsAudio = formatInfo.containsAudio(),
        containsVideo = formatInfo.containsVideo(),
        firstLineText = firstLineText,
        secondLineText = secondLineText,
        selected = selected,
        outlineColor = outlineColor,
        containerColor = containerColor,
        onClick = onClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormatItemCard(
    title: String,
    containsAudio: Boolean,
    containsVideo: Boolean,
    firstLineText: String,
    secondLineText: String,
    selected: Boolean,
    outlineColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
) {
    val animatedTitleColor by animateColorAsState(
        if (selected) outlineColor else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(100),
        label = "formatTitleColor",
    )
    val animatedContainerColor by animateColorAsState(
        if (selected) containerColor else MaterialTheme.colorScheme.surface,
        animationSpec = tween(100),
        label = "formatContainerColor",
    )
    val animatedOutlineColor by animateColorAsState(
        if (selected) outlineColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(100),
        label = "formatOutlineColor",
    )

    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).border(1.dp, animatedOutlineColor, MaterialTheme.shapes.large).clickable { onClick() },
        color = animatedContainerColor,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = animatedTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = firstLineText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = secondLineText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (containsVideo) Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    if (containsAudio) Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun buildCodecText(vcodec: String?, acodec: String?): String {
    val v = vcodec?.substringBefore(".")?.takeIf { it.isNotBlank() }.orEmpty()
    val a = acodec?.substringBefore(".")?.takeIf { it.isNotBlank() }.orEmpty()
    return connectWithDelimiter(v, a, delimiter = " ").run { if (isNotBlank()) "($this)" else this }
}

private fun Double.toFileSizeText(): String {
    if (this <= 0.0) return ""
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = this
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (value >= 100) "%.0f %s".format(value, units[index]) else "%.2f %s".format(value, units[index])
}

private fun Double.toBitrateText(): String {
    if (this <= 0.0) return ""
    return if (this < 1024) "%.1f Kbps".format(this) else "%.2f Mbps".format(this / 1024)
}

private fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return "00:00"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}