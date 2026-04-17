package com.junkfood.seal.desktop.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.desktop.ytdlp.YtDlpFetcher
import com.junkfood.seal.desktop.ytdlp.readYtDlpVersion
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.additional_settings
import com.junkfood.seal.shared.generated.resources.auto_update
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.disabled
import com.junkfood.seal.shared.generated.resources.dismiss
import com.junkfood.seal.shared.generated.resources.every_day
import com.junkfood.seal.shared.generated.resources.every_month
import com.junkfood.seal.shared.generated.resources.every_week
import com.junkfood.seal.shared.generated.resources.update
import com.junkfood.seal.shared.generated.resources.update_channel
import com.junkfood.seal.shared.generated.resources.yt_dlp_update_fail
import com.junkfood.seal.shared.generated.resources.ytdlp_update
import com.junkfood.seal.shared.generated.resources.ytdlp_update_action
import com.junkfood.seal.shared.generated.resources.ytdlp_version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private const val INTERVAL_DAY = 86_400_000L
private const val INTERVAL_WEEK = 604_800_000L
private const val INTERVAL_MONTH = 2_592_000_000L

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

    androidx.compose.foundation.layout.Box {
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

        DesktopYtdlpUpdateChannelDialog(
            visible = showYtdlpDialog,
            settings = appSettings,
            onUpdate = onUpdateAppSettings,
            onDismissRequest = { showYtdlpDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopYtdlpUpdateChannelDialog(
    visible: Boolean,
    settings: DesktopAppSettings,
    onUpdate: (DesktopAppSettings) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var updateChannel by remember { mutableStateOf(settings.ytDlpUpdateChannel) }
    var autoUpdate by remember { mutableStateOf(settings.ytDlpAutoUpdate) }
    var updateInterval by remember { mutableStateOf(settings.ytDlpUpdateInterval) }

    val intervalList = listOf(
        INTERVAL_DAY to Res.string.every_day,
        INTERVAL_WEEK to Res.string.every_week,
        INTERVAL_MONTH to Res.string.every_month,
    )

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.SyncAlt, null) },
        title = {
            Text(stringResource(Res.string.update))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(Res.string.update_channel),
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )

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

                Text(
                    text = stringResource(Res.string.additional_settings),
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value =
                            if (!autoUpdate) stringResource(Res.string.disabled)
                            else intervalList.find { it.first == updateInterval }?.let { stringResource(it.second) } ?: stringResource(Res.string.disabled),
                        onValueChange = {},
                        label = { Text(text = stringResource(Res.string.auto_update)) },
                        readOnly = true,
                        modifier =
                            Modifier.fillMaxWidth()
                                .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.disabled)) },
                            onClick = {
                                autoUpdate = false
                                expanded = false
                            },
                        )
                        for ((interval, stringResource) in intervalList) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(stringResource)) },
                                onClick = {
                                    autoUpdate = true
                                    updateInterval = interval
                                    expanded = false
                                },
                            )
                        }
                    }
                }
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
                        onUpdate(settings.copy(
                            ytDlpUpdateChannel = updateChannel,
                            ytDlpAutoUpdate = autoUpdate,
                            ytDlpUpdateInterval = updateInterval
                        ))
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

