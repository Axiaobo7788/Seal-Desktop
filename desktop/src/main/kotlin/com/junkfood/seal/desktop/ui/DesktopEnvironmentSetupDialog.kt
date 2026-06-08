package com.junkfood.seal.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun DesktopEnvironmentSetupDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    val isWin = System.getProperty("os.name").lowercase().contains("win")
    val isMac = System.getProperty("os.name").lowercase().contains("mac")

    val command = when {
        isWin -> "winget install yt-dlp ffmpeg"
        isMac -> "brew install yt-dlp ffmpeg"
        else -> "sudo apt install ffmpeg\nsudo curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp\nsudo chmod a+rx /usr/local/bin/yt-dlp"
    }

    val instructions = when {
        isWin -> "未检测到自带或系统环境变量中的 yt-dlp 和 ffmpeg。\n\n请打开 PowerShell 或终端，使用包管理器安装："
        isMac -> "未检测到自带或系统环境变量中的 yt-dlp 和 ffmpeg。\n\n请打开终端，使用 Homebrew 安装："
        else -> "未检测到自带或系统环境变量中的 yt-dlp 和 ffmpeg。\n\n请打开终端，使用包管理器安装："
    }

    val clipboardManager = LocalClipboardManager.current

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Warning, null) },
        title = { Text("缺少运行环境") },
        text = {
            Column {
                Text(instructions)
                Spacer(modifier = Modifier.height(16.dp))
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
                            Text(
                                text = command,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(command)) }
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Command")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "安装完成后，请重启应用程序。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) { Text("我知道了") }
        }
    )
}
