package com.junkfood.seal.ui.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon

/**
 * 共享的下载页正文布局，力求贴近 Android DownloadPage 主体。
 * - 由平台提供字符串与回调；
 * - VideoCard、额外内容、操作按钮通过 slot 注入；
 * - 不包含权限/通知等平台功能。
 */
@Composable
fun DownloadUnifiedContent(
    state: DownloadUnifiedState,
    strings: DownloadUnifiedStrings,
    downloadIndicatorText: String,
    modifier: Modifier = Modifier,
    onUrlChange: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCopyErrorClick: () -> Unit,
    onHistoryClick: () -> Unit,
    actionButtons: @Composable () -> Unit = {},
    videoCard: @Composable () -> Unit = {},
    extraContent: @Composable () -> Unit = {},
    inputTrailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        TitleWithProgressIndicatorShared(
            showProgressIndicator = state.showProgressIndicator,
            showDownloadText = state.showDownloadText,
            isDownloadingPlaylist = state.isDownloadingPlaylist,
            currentIndex = state.currentIndex,
            downloadItemCount = state.downloadItemCount,
            title = state.title,
            badge = state.badge,
            onHistoryClick = onHistoryClick,
            historyLabel = strings.historyLabel,
            downloadIndicatorText = downloadIndicatorText,
        )

        Column(Modifier.padding(horizontal = 24.dp).padding(top = 24.dp)) {
            if (state.showVideoCard) {
                AnimatedVisibility(visible = true) { Box { videoCard() } }
            }

            actionButtons()

            InputUrlShared(
                url = state.url,
                error = state.hasError,
                showDownloadProgress = state.showDownloadProgress && !state.showVideoCard,
                progress = state.progress,
                onDone = onDownloadClick,
                showCancelButton = state.showCancelButton && !state.showVideoCard,
                onCancel = onCancelClick,
                onValueChange = onUrlChange,
                videoUrlLabel = strings.videoUrlLabel,
                cancelLabel = strings.cancelLabel,
                trailingIcon = inputTrailingIcon,
            )

            AnimatedVisibility(
                modifier = Modifier.fillMaxWidth(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                visible = state.progressText.isNotEmpty() && state.showOutput,
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = state.progressText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.hasError) {
                ErrorMessageShared(
                    title = state.errorTitle,
                    errorReport = state.errorReport,
                    copyLabel = strings.copyErrorLabel,
                    onCopy = onCopyErrorClick,
                )
            }

            extraContent()

            Spacer(modifier = Modifier.height(160.dp))
        }
    }
}

@Composable
private fun TitleWithProgressIndicatorShared(
    showProgressIndicator: Boolean,
    showDownloadText: Boolean,
    isDownloadingPlaylist: Boolean,
    currentIndex: Int,
    downloadItemCount: Int,
    title: String,
    badge: Int,
    onHistoryClick: () -> Unit,
    historyLabel: String,
    downloadIndicatorText: String,
) {
    Column(modifier = Modifier.padding(start = 12.dp, top = 24.dp)) {
        Row(
            modifier =
                Modifier.clip(MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier,
                text = title,
                style = MaterialTheme.typography.displaySmall,
            )
            AnimatedVisibility(visible = showProgressIndicator) {
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 3.dp)
                }
            }
            AnimatedVisibility(visible = badge > 0) {
                BadgedBox(badge = { Badge { Text(badge.toString()) } }) {}
            }
            Spacer(modifier = Modifier.weight(1f, fill = true))
            OutlinedButton(onClick = onHistoryClick, contentPadding = ButtonDefaults.ContentPadding) {
                Text(text = historyLabel)
            }
        }
        AnimatedVisibility(visible = showDownloadText) {
            Text(
                if (isDownloadingPlaylist)
                    "${currentIndex}/${downloadItemCount}"
                else downloadIndicatorText,
                modifier = Modifier.padding(start = 12.dp, top = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InputUrlShared(
    url: String,
    error: Boolean,
    showDownloadProgress: Boolean,
    progress: Float,
    onDone: () -> Unit,
    showCancelButton: Boolean,
    onCancel: () -> Unit,
    onValueChange: (String) -> Unit,
    videoUrlLabel: String,
    cancelLabel: String,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = url,
        isError = error,
        onValueChange = onValueChange,
        label = { Text(videoUrlLabel) },
        modifier = Modifier.padding(0.dp, 16.dp).fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        maxLines = 3,
        trailingIcon = {
            if (trailingIcon != null) {
                trailingIcon()
            } else if (url.isNotEmpty()) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onValueChange("") },
                )
            }
        },
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        keyboardOptions = KeyboardOptions.Default,
    )
    AnimatedVisibility(visible = showDownloadProgress) {
        Row(Modifier.padding(0.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val progressValue = progress.coerceIn(0f, 100f) / 100f
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier.weight(0.75f).clip(MaterialTheme.shapes.large),
            )
            Text(
                text = if (progress < 0) "0%" else "${progress.toInt()}%",
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.25f),
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = showCancelButton) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Outlined.Cancel, contentDescription = cancelLabel, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = cancelLabel)
            }
        }
    }
}

@Composable
private fun ErrorMessageShared(
    title: String,
    errorReport: String,
    copyLabel: String,
    onCopy: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = errorReport, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onCopy) { Text(copyLabel) }
    }
}

data class DownloadUnifiedState(
    val url: String = "",
    val title: String = "Seal",
    val showProgressIndicator: Boolean = false,
    val showDownloadText: Boolean = false,
    val isDownloadingPlaylist: Boolean = false,
    val currentIndex: Int = 0,
    val downloadItemCount: Int = 0,
    val badge: Int = 0,
    val showDownloadProgress: Boolean = false,
    val progress: Float = 0f,
    val progressText: String = "",
    val showVideoCard: Boolean = false,
    val showCancelButton: Boolean = false,
    val errorTitle: String = "",
    val errorReport: String = "",
    val hasError: Boolean = false,
    val showOutput: Boolean = false,
)

data class DownloadUnifiedStrings(
    val videoUrlLabel: String,
    val cancelLabel: String,
    val copyErrorLabel: String,
    val historyLabel: String,
)
