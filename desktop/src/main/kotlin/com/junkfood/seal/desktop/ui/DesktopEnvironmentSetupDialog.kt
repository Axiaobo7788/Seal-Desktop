package com.junkfood.seal.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.BufferedReader
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

enum class SetupDialogState {
    Selection,
    Installing,
    Success,
    Failed
}

@Composable
fun DesktopEnvironmentSetupDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    val isWin = System.getProperty("os.name").lowercase().contains("win")
    val isMac = System.getProperty("os.name").lowercase().contains("mac")
    
    var state by remember { mutableStateOf(SetupDialogState.Selection) }
    var selectedOption by remember { mutableStateOf(if (isWin || isMac) 0 else 1) } // 0 = System, 1 = Portable
    var logOutput by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = { if (state != SetupDialogState.Installing) onDismissRequest() },
        icon = { 
            Icon(
                if (state == SetupDialogState.Success) Icons.Outlined.CheckCircle else Icons.Outlined.Warning, 
                null
            ) 
        },
        title = { 
            Text(
                when (state) {
                    SetupDialogState.Selection -> stringResource(Res.string.env_setup_missing)
                    SetupDialogState.Installing -> stringResource(Res.string.env_setup_installing)
                    SetupDialogState.Success -> stringResource(Res.string.env_setup_success)
                    SetupDialogState.Failed -> stringResource(Res.string.env_setup_failed)
                }
            ) 
        },
        text = {
            AnimatedContent(state) { targetState ->
                when (targetState) {
                    SetupDialogState.Selection -> {
                        Column {
                            Text(stringResource(Res.string.env_setup_missing_desc))
                            Spacer(Modifier.height(16.dp))
                            
                            if (isWin || isMac) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedOption == 0, onClick = { selectedOption = 0 })
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(stringResource(Res.string.env_setup_sys_title), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (isWin) stringResource(Res.string.env_setup_sys_desc_win) else stringResource(Res.string.env_setup_sys_desc_mac),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedOption == 1, onClick = { selectedOption = 1 })
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(stringResource(Res.string.env_setup_port_title), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(Res.string.env_setup_port_desc),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    SetupDialogState.Installing -> {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            ) {
                                val scrollState = rememberScrollState()
                                LaunchedEffect(logOutput) {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                                SelectionContainer {
                                    Text(
                                        text = logOutput,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp).verticalScroll(scrollState)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    SetupDialogState.Success -> {
                        Text(stringResource(Res.string.env_setup_success_msg))
                    }
                    SetupDialogState.Failed -> {
                        Column {
                            Text(stringResource(Res.string.env_setup_failed_msg))
                            Spacer(Modifier.height(12.dp))
                            val manualCommand = when {
                                isWin -> "winget install yt-dlp ffmpeg"
                                isMac -> "brew install yt-dlp ffmpeg"
                                else -> "sudo apt install ffmpeg\nsudo curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp\nsudo chmod a+rx /usr/local/bin/yt-dlp"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SelectionContainer(modifier = Modifier.weight(1f)) {
                                        Text(text = manualCommand, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(manualCommand)) }) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                SetupDialogState.Selection -> {
                    val logStart = stringResource(Res.string.env_setup_log_start)
                    Button(onClick = {
                        state = SetupDialogState.Installing
                        logOutput = logStart
                        scope.launch {
                            val success = runInstallProcess(selectedOption, isWin, isMac) { log ->
                                logOutput += log + "\n"
                            }
                            state = if (success) SetupDialogState.Success else SetupDialogState.Failed
                        }
                    }) {
                        Text(stringResource(Res.string.env_setup_btn_install))
                    }
                }
                SetupDialogState.Installing -> {
                    Button(onClick = { /* disabled */ }, enabled = false) { Text(stringResource(Res.string.env_setup_btn_installing)) }
                }
                SetupDialogState.Success -> {
                    Button(onClick = onDismissRequest) { Text(stringResource(Res.string.env_setup_btn_done)) }
                }
                SetupDialogState.Failed -> {
                    Button(onClick = onDismissRequest) { Text(stringResource(Res.string.env_setup_btn_close)) }
                }
            }
        },
        dismissButton = {
            if (state == SetupDialogState.Selection) {
                TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.env_setup_btn_later)) }
            }
        }
    )
}

private suspend fun runInstallProcess(
    option: Int,
    isWin: Boolean,
    isMac: Boolean,
    onLog: (String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    if (option == 0) {
        // System Package Manager
        val command = when {
            isWin -> listOf("winget", "install", "yt-dlp", "ffmpeg", "--accept-package-agreements", "--accept-source-agreements")
            isMac -> listOf("brew", "install", "yt-dlp", "ffmpeg")
            else -> return@withContext false // Linux not fully supported for zero-interaction sudo
        }
        
        onLog("执行系统命令: ${command.joinToString(" ")}")
        try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    withContext(Dispatchers.Main) {
                        onLog(line ?: "")
                    }
                }
            }
            val exitCode = process.waitFor()
            onLog("\n安装进程退出，代码 $exitCode")
            return@withContext exitCode == 0
        } catch (e: Exception) {
            onLog("执行出错: ${e.message}")
            return@withContext false
        }
    } else {
        // Portable download
        onLog("开始下载便携版依赖 (直连 GitHub，请耐心等待)...\n")
        val success = com.junkfood.seal.desktop.ytdlp.DesktopAuxiliaryDownloader.downloadPortableDependencies(isWin, isMac, onLog)
        return@withContext success
    }
}
