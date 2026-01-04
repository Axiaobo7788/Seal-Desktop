package com.junkfood.seal.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.desktop.ytdlp.YtDlpMetadataFetcher
import com.junkfood.seal.download.buildDownloadPlan
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Seal Desktop") {
        MaterialTheme {
            Surface {
                DownloadScreen()
            }
        }
    }
}

@Composable
private fun DownloadScreen() {
    val scope = rememberCoroutineScope()
    val executor = remember { DownloadPlanExecutor() }
    val metadataFetcher = remember { YtDlpMetadataFetcher() }

    var url by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("等待链接") }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var runningProcess by remember { mutableStateOf<DownloadPlanExecutor.RunningProcess?>(null) }
    val logLines = remember { mutableStateListOf<String>() }

    fun appendLog(line: String) {
        logLines.add(line)
        if (logLines.size > 200) {
            logLines.removeFirst()
        }
    }

    Column(
        modifier = Modifier.padding(16.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("视频链接") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (url.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        status = "获取信息..."
                        try {
                            val fetched = metadataFetcher.fetch(url.trim())
                            info = fetched
                            status = "获取成功：${fetched.title}".take(80)
                            appendLog("metadata ok: ${fetched.title}")
                        } catch (e: Exception) {
                            status = "获取失败：${e.message}"
                            appendLog("metadata failed: ${e.message}")
                        }
                    }
                },
                enabled = !isRunning,
            ) {
                Text("获取信息")
            }

            Button(
                onClick = {
                    if (url.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        isRunning = true
                        status = "下载中..."
                        appendLog("start download: $url")
                        val videoInfo = info ?: VideoInfo(originalUrl = url, webpageUrl = url, title = url)
                        val preferences = defaultDesktopPreferences()
                        val targetUrl = url.trim()
                        val plan = buildDownloadPlan(videoInfo, preferences, playlistUrl = targetUrl, playlistItem = 0)
                        try {
                            val proc =
                                executor.start(
                                    plan,
                                    executor.defaultConfigFor(plan, url = targetUrl, paths = DesktopYtDlpPaths),
                                    onStdout = { appendLog(it) },
                                    onStderr = { appendLog("[err] $it") },
                                )
                            runningProcess = proc
                            val result = proc.waitForResult()
                            status = "下载完成，退出码 ${result.exitCode}"
                        } catch (e: Exception) {
                            status = "下载失败：${e.message}"
                            appendLog("download failed: ${e.message}")
                        } finally {
                            isRunning = false
                            runningProcess = null
                        }
                    }
                },
                enabled = !isRunning,
            ) {
                Text("开始下载")
            }

            TextButton(
                onClick = {
                    runningProcess?.cancel()
                    appendLog("已请求取消")
                    status = "取消中..."
                },
                enabled = isRunning,
            ) {
                Text("取消")
            }
        }

        Text(status)

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(logLines) { line -> Text(line) }
        }

        Spacer(modifier = Modifier.size(4.dp))
    }
}

private fun defaultDesktopPreferences(): DownloadPreferences =
    DownloadPreferences.EMPTY.copy(
        formatSorting = true,
        videoFormat = 2, // QUALITY
        videoResolution = 3, // 1080p
        embedMetadata = true,
    )
