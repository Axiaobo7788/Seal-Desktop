package com.junkfood.seal.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Platform-neutral download screen UI. State and events are pure; platform hooks stay outside.
 */
@Composable
fun DownloadScreenShared(
    state: DownloadUiState,
    onEvent: (DownloadEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.url,
            onValueChange = { onEvent(DownloadEvent.UrlChanged(it)) },
            label = { Text("视频链接") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEvent(DownloadEvent.FetchInfo) }, enabled = !state.isRunning) {
                Text("获取信息")
            }
            Button(onClick = { onEvent(DownloadEvent.StartDownload) }, enabled = !state.isRunning) {
                Text("开始下载")
            }
            TextButton(onClick = { onEvent(DownloadEvent.Cancel) }, enabled = state.isRunning) {
                Text("取消")
            }
        }

        Text(state.status, style = MaterialTheme.typography.bodyMedium)

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(state.logLines) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Spacer for edge-to-edge safety on some platforms
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
    }
}

/** UI state holder for the shared download screen. */
data class DownloadUiState(
    val url: String = "",
    val status: String = "",
    val isRunning: Boolean = false,
    val logLines: List<String> = emptyList(),
)

/** UI events the host platform must handle. */
sealed interface DownloadEvent {
    data class UrlChanged(val url: String) : DownloadEvent
    object FetchInfo : DownloadEvent
    object StartDownload : DownloadEvent
    object Cancel : DownloadEvent
}
