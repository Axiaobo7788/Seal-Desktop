package com.junkfood.seal.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.ytdlp.YtDlpFetcher
import com.junkfood.seal.desktop.ytdlp.readYtDlpVersion
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.dismiss
import com.junkfood.seal.shared.generated.resources.yt_dlp_update_fail
import com.junkfood.seal.shared.generated.resources.ytdlp_update
import com.junkfood.seal.shared.generated.resources.ytdlp_update_action
import com.junkfood.seal.shared.generated.resources.ytdlp_version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun YtdlpUpdateCard(
    appSettings: DesktopAppSettings,
    onUpdateAppSettings: (DesktopAppSettings) -> Unit
) {
    var showYtdlpDialog by remember { mutableStateOf(false) }

    val ytdlpUpdateText = stringResource(Res.string.ytdlp_update)
    val ytdlpVersionLabel = stringResource(Res.string.ytdlp_version)
    val ytdlpUpdateFailText = stringResource(Res.string.yt_dlp_update_fail)

    val scope = rememberCoroutineScope()
    val fetcher = remember { YtDlpFetcher() }
    var isUpdatingYtDlp by remember { mutableStateOf(false) }
    var ytDlpDesc by remember(ytdlpUpdateText) { mutableStateOf(ytdlpUpdateText) }

    LaunchedEffect(Unit) {
        val existing = fetcher.findExistingBinary()
        if (existing != null) {
            val version = readYtDlpVersion(existing)
            ytDlpDesc =
                if (!version.isNullOrBlank()) {
                    "$ytdlpVersionLabel: $version"
                } else {
                    ytdlpUpdateText
                }
        }
    }

    ActionWithDividerCard(
        title = stringResource(Res.string.ytdlp_update_action),
        description = ytDlpDesc,
        icon = Icons.Outlined.Update,
        trailingIcon = Icons.Outlined.Settings,
        enabled = !isUpdatingYtDlp,
        onClick = {
            scope.launch {
                isUpdatingYtDlp = true
                ytDlpDesc = ytdlpUpdateText
                val updated =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            fetcher.invalidateCachedBinary()
                            val binary = fetcher.ensureBinary()
                            readYtDlpVersion(binary)
                        }
                    }

                ytDlpDesc =
                    updated.getOrNull()?.takeIf { it.isNotBlank() }?.let { v ->
                        "$ytdlpVersionLabel: $v"
                    } ?: ytdlpUpdateFailText

                isUpdatingYtDlp = false
            }
        },
        onTrailingClick = { showYtdlpDialog = true },
    )

    if (showYtdlpDialog) {
        DesktopYtdlpUpdateChannelDialog(
            settings = appSettings,
            onUpdate = onUpdateAppSettings,
            onDismissRequest = { showYtdlpDialog = false }
        )
    }
}

@Composable
internal fun DesktopYtdlpUpdateChannelDialog(
    settings: DesktopAppSettings,
    onUpdate: (DesktopAppSettings) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var updateChannel by remember { mutableStateOf(settings.ytDlpUpdateChannel) }
    AnimatedAlertDialog(
        visible = true,
        onDismissRequest = onDismissRequest,
        icon = null,
        title = {
            Text("Update Channel") // Fallback
        },
        text = {
            Column {
                DialogSingleChoiceItemVariantWithLabel(
                    text = "yt-dlp",
                    label = "Stable",
                    selected = updateChannel == 0,
                    onClick = { updateChannel = 0 }
                )
                
                DialogSingleChoiceItemVariantWithLabel(
                    text = "yt-dlp-nightly-builds",
                    label = "Nightly",
                    selected = updateChannel == 1,
                    isLabelTertiary = true,
                    onClick = { updateChannel = 1 }
                )
            }
        },
        dismissButton = {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .clickable(onClick = onDismissRequest)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                text = stringResource(Res.string.dismiss),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        confirmButton = {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .clickable {
                        onUpdate(settings.copy(ytDlpUpdateChannel = updateChannel))
                        onDismissRequest()
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                text = stringResource(Res.string.confirm),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}

@Composable
private fun DialogSingleChoiceItemVariantWithLabel(
    text: String,
    label: String,
    selected: Boolean,
    isLabelTertiary: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.padding(end = 24.dp),
            )
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isLabelTertiary) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(
                    color = if (isLabelTertiary) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(percent = 50)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
