@file:OptIn(ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.ui.page.downloadv2.configure

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.download.DesktopDownloadController
import com.junkfood.seal.desktop.download.DesktopDownloadType
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.auto_subtitle
import com.junkfood.seal.shared.generated.resources.back
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.clear
import com.junkfood.seal.shared.generated.resources.clip_video
import com.junkfood.seal.shared.generated.resources.discard
import com.junkfood.seal.shared.generated.resources.format_selection
import com.junkfood.seal.shared.generated.resources.playlist
import com.junkfood.seal.shared.generated.resources.rename
import com.junkfood.seal.shared.generated.resources.save
import com.junkfood.seal.shared.generated.resources.search_in_subtitles
import com.junkfood.seal.shared.generated.resources.show_all_items
import com.junkfood.seal.shared.generated.resources.split_video
import com.junkfood.seal.shared.generated.resources.split_video_msg
import com.junkfood.seal.shared.generated.resources.start_download
import com.junkfood.seal.shared.generated.resources.subtitle_language
import com.junkfood.seal.shared.generated.resources.suggested
import com.junkfood.seal.shared.generated.resources.show_more_actions
import com.junkfood.seal.shared.generated.resources.thumbnail
import com.junkfood.seal.shared.generated.resources.title
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.shared.generated.resources.video_only
import com.junkfood.seal.ui.download.queue.DownloadThumbnail
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.SubtitleFormat
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.connectWithDelimiter
import com.junkfood.seal.util.toHttpsUrl
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

private const val NotSelected = -1

private data class FormatConfig(
    val formatList: List<Format>,
    val videoClips: List<VideoClip>,
    val splitByChapter: Boolean,
    val newTitle: String,
    val selectedSubtitles: List<String>,
    val selectedAutoCaptions: List<String>,
)

@Composable
internal fun CustomFormatSelectionSheet(
    url: String,
    controller: DesktopDownloadController,
    downloadType: DesktopDownloadType,
    basePreferences: DownloadPreferences,
    isVideoClipEnabled: Boolean = false,
    onPreferencesUpdated: (DownloadPreferences) -> Unit,
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
        val result = controller.fetchVideoInfo(url, basePreferences)
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
                basePreferences = basePreferences,
                isVideoClipEnabled = isVideoClipEnabled,
                allowMultiAudio = basePreferences.mergeAudioStream,
                onNavigateBack = onBack,
                onDownloadPressed = { config ->
                    controller.startDownloadWithSelection(
                        url = url,
                        type = downloadType,
                        basePreferences = basePreferences,

                        videoInfo = videoInfo!!,
                        formatList = config.formatList,
                        videoClips = config.videoClips,
                        splitByChapter = config.splitByChapter,
                        newTitle = config.newTitle,
                        selectedSubtitles = config.selectedSubtitles,
                        selectedAutoCaptions = config.selectedAutoCaptions,
                        onSelectionApplied = onPreferencesUpdated,
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
    basePreferences: DownloadPreferences,
    allowMultiAudio: Boolean,
    onNavigateBack: () -> Unit,
    onDownloadPressed: (FormatConfig) -> Unit,
    isVideoClipEnabled: Boolean = false,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    val formats = videoInfo.formats.orEmpty()
    if (formats.isEmpty()) return

    val videoOnlyFormats = formats.filter { it.containsVideo() && it.isVideoOnly() }.reversed()
    val audioOnlyFormats = formats.filter { it.containsAudio() && it.isAudioOnly() }.reversed()
    val videoAudioFormats = formats.filter { it.containsAudio() && it.containsVideo() }.reversed()

    val duration = videoInfo.duration ?: 0.0
    val durationSeconds = duration.toInt().coerceAtLeast(0)
    val chapterCount = videoInfo.chapters?.size ?: 0

    val initialSubtitleCodes = remember(basePreferences.subtitleLanguage) { parseLanguageCodes(basePreferences.subtitleLanguage) }
    val subtitleCodes = remember(videoInfo.subtitles) { videoInfo.subtitles.keys.sorted() }
    val autoCaptionCodes = remember(videoInfo.automaticCaptions) { videoInfo.automaticCaptions.keys.sorted() }
    val suggestedSubtitleMap: Map<String, List<SubtitleFormat>> =
        remember(videoInfo.subtitles, videoInfo.automaticCaptions) {
            videoInfo.subtitles.takeIf { it.isNotEmpty() }
                ?: videoInfo.automaticCaptions.filterKeys { it.endsWith("-orig") }
        }
    val suggestedSubtitleUsesAutoCaptions =
        videoInfo.subtitles.isEmpty() && suggestedSubtitleMap.isNotEmpty()
    val otherSubtitleMap: Map<String, List<SubtitleFormat>> =
        remember(videoInfo.subtitles, videoInfo.automaticCaptions, suggestedSubtitleMap) {
            (videoInfo.subtitles + videoInfo.automaticCaptions).filterKeys { it !in suggestedSubtitleMap.keys }
        }

    var videoOnlyItemLimit by remember { mutableIntStateOf(6) }
    var audioOnlyItemLimit by remember { mutableIntStateOf(6) }
    var videoAudioItemLimit by remember { mutableIntStateOf(6) }
    var showSubtitleSelectionDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val isSuggestedFormatAvailable =
        !videoInfo.requestedFormats.isNullOrEmpty() || !videoInfo.requestedDownloads.isNullOrEmpty()
    var isSuggestedFormatSelected by remember { mutableStateOf(isSuggestedFormatAvailable) }

    var selectedVideoAudioFormat by remember { mutableIntStateOf(NotSelected) }
    var selectedVideoOnlyFormat by remember { mutableIntStateOf(NotSelected) }
    val selectedAudioOnlyFormats = remember { mutableStateListOf<Int>() }
    val selectedSubtitles =
        remember(videoInfo.id, basePreferences.subtitleLanguage) {
            mutableStateListOf<String>().apply {
                addAll(subtitleCodes.filter { code -> initialSubtitleCodes.contains(code) })
            }
        }
    val selectedAutoCaptions =
        remember(videoInfo.id, basePreferences.subtitleLanguage, basePreferences.autoSubtitle) {
            mutableStateListOf<String>().apply {
                if (basePreferences.autoSubtitle) {
                    addAll(autoCaptionCodes.filter { code -> initialSubtitleCodes.contains(code) })
                }
            }
        }
    var splitByChapter by remember(videoInfo.id) { mutableStateOf(basePreferences.splitByChapter && chapterCount > 0) }
    var clipVideo by remember(videoInfo.id) { mutableStateOf(basePreferences.videoClips.isNotEmpty()) }
    var clipStartText by remember(videoInfo.id) {
        mutableStateOf(basePreferences.videoClips.firstOrNull()?.start?.coerceAtLeast(0)?.toString() ?: "0")
    }
    var clipEndText by remember(videoInfo.id) {
        mutableStateOf(
            (
                basePreferences.videoClips.firstOrNull()?.end?.takeIf { it > 0 }
                    ?: durationSeconds
            ).toString(),
        )
    }
    var newTitle by remember(videoInfo.id) { mutableStateOf(basePreferences.newTitle.takeIf { it.isNotBlank() } ?: "") }

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
                    onClick = {
                        val clips = buildVideoClips(
                            enabled = clipVideo,
                            startText = clipStartText,
                            endText = clipEndText,
                            maxSeconds = durationSeconds,
                        )
                        onDownloadPressed(
                            FormatConfig(
                                formatList = selectedFormats,
                                videoClips = clips,
                                splitByChapter = splitByChapter && clips.isEmpty(),
                                newTitle = newTitle.trim(),
                                selectedSubtitles = selectedSubtitles.toList(),
                                selectedAutoCaptions = selectedAutoCaptions.toList(),
                            ),
                        )
                    },
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
                FormatPreviewHeader(
                    videoInfo = videoInfo,
                    title = newTitle.ifBlank { videoInfo.title },
                    splitByChapter = splitByChapter,
                    clipVideo = clipVideo,
                    isVideoClipEnabled = isVideoClipEnabled,
                    isSplitByChapterAvailable = chapterCount > 0,
                    onRename = { showRenameDialog = true },
                    onOpenThumbnail = {
                        videoInfo.getBestThumbnailUrl().toHttpsUrl().takeIf { it.isNotBlank() }?.let(uriHandler::openUri)
                    },
                    onSplitByChapterToggle = {
                        splitByChapter = !splitByChapter
                        if (splitByChapter) {
                            clipVideo = false
                        }
                    },
                    onClipVideoToggle = {
                        clipVideo = !clipVideo
                        if (clipVideo) {
                            splitByChapter = false
                        }
                    },
                )
            }

            if (chapterCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SplitVideoInfoRow(
                        visible = splitByChapter,
                        chapterCount = chapterCount,
                        onDiscard = { splitByChapter = false },
                    )
                }
            }

            if (suggestedSubtitleMap.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SubtitleLanguageSection(
                        suggestedSubtitles = suggestedSubtitleMap,
                        selectedCodes = if (suggestedSubtitleUsesAutoCaptions) selectedAutoCaptions else selectedSubtitles,
                        totalCount = subtitleCodes.size + autoCaptionCodes.size,
                        onToggle = { code ->
                            val target =
                                if (suggestedSubtitleUsesAutoCaptions) selectedAutoCaptions
                                else selectedSubtitles
                            if (target.contains(code)) {
                                target.remove(code)
                            } else {
                                target.add(code)
                            }
                        },
                        onShowAll = { showSubtitleSelectionDialog = true },
                    )
                }
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

    SubtitleSelectionDialog(
        visible = showSubtitleSelectionDialog,
        suggestedSubtitles = suggestedSubtitleMap,
        suggestedUsesAutoCaptions = suggestedSubtitleUsesAutoCaptions,
        autoCaptions = otherSubtitleMap,
        selectedSubtitles = selectedSubtitles,
        selectedAutoCaptions = selectedAutoCaptions,
        onDismissRequest = { showSubtitleSelectionDialog = false },
        onConfirm = { subtitles, autoCaptions ->
            selectedSubtitles.clear()
            selectedSubtitles.addAll(subtitles)
            selectedAutoCaptions.clear()
            selectedAutoCaptions.addAll(autoCaptions)
            showSubtitleSelectionDialog = false
        },
    )

    RenameDialog(
        visible = showRenameDialog,
        initialValue = newTitle.ifBlank { videoInfo.title },
        onDismissRequest = { showRenameDialog = false },
        onConfirm = { newTitle = it },
    )
}

@Composable
private fun FormatPreviewHeader(
    videoInfo: VideoInfo,
    title: String,
    splitByChapter: Boolean,
    clipVideo: Boolean,
    isVideoClipEnabled: Boolean,
    isSplitByChapterAvailable: Boolean,
    onRename: () -> Unit,
    onOpenThumbnail: () -> Unit,
    onSplitByChapterToggle: () -> Unit,
    onClipVideoToggle: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var menuExpanded by remember { mutableStateOf(false) }
    val thumbnailUrl = videoInfo.getBestThumbnailUrl().toHttpsUrl()
    val durationText = formatDuration(videoInfo.duration?.toInt() ?: 0)
    val creatorText = listOf(videoInfo.uploader, videoInfo.uploaderId).firstOrNull { !it.isNullOrBlank() }.orEmpty()
    val sourceUrl = videoInfo.webpageUrl?.takeIf { it.isNotBlank() } ?: videoInfo.originalUrl?.takeIf { it.isNotBlank() }
    val platformText = listOf(videoInfo.extractorKey, videoInfo.extractor, videoInfo.webpageUrlDomain).firstOrNull { !it.isNullOrBlank() }?.replace('_', ' ')
    val summaryText = listOfNotNull(
        creatorText.takeIf { it.isNotBlank() },
        platformText?.takeIf { it.isNotBlank() },
        videoInfo.webpageUrlDomain?.takeIf { it.isNotBlank() },
    ).distinct().joinToString(" • ")

    Surface(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(width = 220.dp, height = 124.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    DownloadThumbnail(url = thumbnailUrl, modifier = Modifier.fillMaxSize())
                    if ((videoInfo.duration ?: 0.0) > 0.0) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = title.ifBlank { videoInfo.webpageUrlBasename.orEmpty() },
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    summaryText.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    sourceUrl?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { uriHandler.openUri(it) },
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(Res.string.show_more_actions),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            text = { Text(stringResource(Res.string.rename)) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                            text = { Text(stringResource(Res.string.thumbnail)) },
                            onClick = {
                                menuExpanded = false
                                onOpenThumbnail()
                            },
                        )
                        if (isVideoClipEnabled && !clipVideo && !splitByChapter) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.ContentCut, contentDescription = null) },
                                text = { Text(stringResource(Res.string.clip_video)) },
                                onClick = {
                                    menuExpanded = false
                                    onClipVideoToggle()
                                },
                            )
                        }
                        if (isSplitByChapterAvailable && !clipVideo && !splitByChapter) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.VerticalSplit, contentDescription = null) },
                                text = { Text(stringResource(Res.string.split_video)) },
                                onClick = {
                                    menuExpanded = false
                                    onSplitByChapterToggle()
                                },
                            )
                        }
                    }
                }
            }
            videoInfo.playlist?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = stringResource(Res.string.playlist) + ": " + it,
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
private fun SplitVideoInfoRow(
    visible: Boolean,
    chapterCount: Int,
    onDiscard: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.split_video_msg, chapterCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButtonWithIcon(
                    onClick = onDiscard,
                    icon = Icons.Outlined.Delete,
                    text = stringResource(Res.string.discard),
                    contentColor = MaterialTheme.colorScheme.error,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun TextButtonWithIcon(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, color = contentColor)
    }
}

@Composable
private fun RenameDialog(
    visible: Boolean,
    initialValue: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(visible, initialValue) { mutableStateOf(initialValue) }

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        title = { Text(stringResource(Res.string.rename)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.title)) },
                trailingIcon = {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { value = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(Res.string.clear),
                            )
                        }
                    }
                },
            )
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(value)
                    onDismissRequest()
                },
            ) {
                Text(stringResource(Res.string.save))
            }
        },
    )
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
private fun SubtitleLanguageSection(
    suggestedSubtitles: Map<String, List<SubtitleFormat>>,
    selectedCodes: List<String>,
    totalCount: Int,
    onToggle: (String) -> Unit,
    onShowAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.subtitle_language),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            ClickableTextAction(
                visible = totalCount > suggestedSubtitles.size,
                text = stringResource(Res.string.show_all_items, totalCount),
                onClick = onShowAll,
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestedSubtitles.toList().forEach { (code, formats) ->
                item(key = code) {
                    SubtitleFilterChip(
                        selected = selectedCodes.contains(code),
                        label = formats.subtitleLabel(code),
                        onClick = { onToggle(code) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtitleFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(100),
        label = "subtitleChipBackground",
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(100),
        label = "subtitleChipContent",
    )

    Surface(
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClickableTextAction(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = visible, exit = fadeOut(animationSpec = tween(90))) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            modifier = modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DesktopSealSearchBar(
    text: String,
    placeholderText: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = modifier.widthIn(360.dp, 720.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.size(16.dp))
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (text.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(Res.string.clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtitleSelectionDialog(
    visible: Boolean,
    suggestedSubtitles: Map<String, List<SubtitleFormat>>,
    suggestedUsesAutoCaptions: Boolean,
    autoCaptions: Map<String, List<SubtitleFormat>>,
    selectedSubtitles: List<String>,
    selectedAutoCaptions: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (subs: List<String>, autoSubs: List<String>) -> Unit,
) {
    var searchText by remember(visible) { mutableStateOf("") }
    val dialogSelectedSubtitles =
        remember(visible, selectedSubtitles) {
            mutableStateListOf<String>().apply { addAll(selectedSubtitles) }
        }
    val dialogSelectedAutoCaptions =
        remember(visible, selectedAutoCaptions) {
            mutableStateListOf<String>().apply { addAll(selectedAutoCaptions) }
        }

    val suggestedSelection = if (suggestedUsesAutoCaptions) dialogSelectedAutoCaptions else dialogSelectedSubtitles
    val suggestedFiltered =
        suggestedSubtitles
            .filterWithSearchText(searchText)
            .sortedWithSelection(suggestedSelection)
    val autoCaptionsFiltered =
        autoCaptions
            .filterWithSearchText(searchText)
            .sortedWithSelection(dialogSelectedAutoCaptions)
    val totalCount = suggestedSubtitles.size + autoCaptions.size

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        icon = { Icon(Icons.Outlined.Subtitles, contentDescription = null) },
        title = { Text(stringResource(Res.string.subtitle_language)) },
        textContentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 0.dp),
        text = {
            Column {
                if (totalCount > 5) {
                    DesktopSealSearchBar(
                        text = searchText,
                        placeholderText = stringResource(Res.string.search_in_subtitles),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        onValueChange = { searchText = it },
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    if (suggestedFiltered.isNotEmpty()) {
                        item {
                            DialogSectionTitle(text = stringResource(Res.string.suggested))
                        }
                    }
                    suggestedFiltered.forEach { (code, formats) ->
                        item(key = "suggested-$code") {
                            DialogCheckBoxItem(
                                modifier = Modifier.animateItem(),
                                text = formats.subtitleLabel(code),
                                checked = suggestedSelection.contains(code),
                                onCheckedChange = { checked ->
                                    suggestedSelection.updateSelection(code, checked)
                                },
                            )
                        }
                    }

                    if (autoCaptionsFiltered.isNotEmpty()) {
                        item {
                            DialogSectionTitle(text = stringResource(Res.string.auto_subtitle))
                        }
                    }
                    autoCaptionsFiltered.forEach { (code, formats) ->
                        item(key = "auto-$code") {
                            DialogCheckBoxItem(
                                modifier = Modifier.animateItem(),
                                text = formats.subtitleLabel(code),
                                checked = dialogSelectedAutoCaptions.contains(code),
                                onCheckedChange = { checked ->
                                    dialogSelectedAutoCaptions.updateSelection(code, checked)
                                },
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(dialogSelectedSubtitles.toList(), dialogSelectedAutoCaptions.toList())
                },
            ) {
                Text(stringResource(Res.string.save))
            }
        },
    )
}

@Composable
private fun DialogSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

@Composable
private fun DialogCheckBoxItem(
    modifier: Modifier = Modifier,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun MutableList<String>.updateSelection(code: String, selected: Boolean) {
    if (selected) {
        if (!contains(code)) add(code)
    } else {
        remove(code)
    }
}

private fun Map<String, List<SubtitleFormat>>.filterWithSearchText(searchText: String): Map<String, List<SubtitleFormat>> =
    filter { (code, formats) ->
        searchText.isBlank() ||
            code.contains(searchText, ignoreCase = true) ||
            formats.any { format -> format.name?.contains(searchText, ignoreCase = true) == true }
    }

private fun Map<String, List<SubtitleFormat>>.sortedWithSelection(selectedKeys: List<String>): Map<String, List<SubtitleFormat>> =
    toList()
        .sortedWith { entry1, entry2 ->
            when {
                entry1.first in selectedKeys && entry2.first !in selectedKeys -> -1
                entry1.first !in selectedKeys && entry2.first in selectedKeys -> 1
                else -> entry1.compareSubtitleTo(entry2)
            }
        }
        .toMap()

private fun Pair<String, List<SubtitleFormat>>.compareSubtitleTo(other: Pair<String, List<SubtitleFormat>>): Int {
    val name = second.firstOrNull()?.name
    val otherName = other.second.firstOrNull()?.name
    return if (name != null && otherName != null) {
        name.compareTo(otherName)
    } else {
        first.compareTo(other.first)
    }
}

private fun List<SubtitleFormat>.subtitleLabel(code: String): String =
    firstOrNull()?.run { name ?: protocol ?: code } ?: code

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

private fun parseLanguageCodes(value: String): Set<String> =
    value
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

private fun buildVideoClips(
    enabled: Boolean,
    startText: String,
    endText: String,
    maxSeconds: Int,
): List<VideoClip> {
    if (!enabled || maxSeconds <= 0) return emptyList()
    val rawStart = startText.toIntOrNull() ?: 0
    val rawEnd = endText.toIntOrNull() ?: maxSeconds
    val start = rawStart.coerceIn(0, maxSeconds)
    val end = rawEnd.coerceIn(0, maxSeconds)
    if (end <= start) return emptyList()
    return listOf(VideoClip(start = start, end = end))
}
