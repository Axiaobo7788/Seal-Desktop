package com.junkfood.seal.desktop.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import com.junkfood.seal.desktop.ytdlp.DesktopAuxiliaryDownloader
import com.junkfood.seal.desktop.ytdlp.DesktopDependencySource
import com.junkfood.seal.desktop.ytdlp.YtDlpFetcher
import com.junkfood.seal.desktop.ytdlp.YtDlpUpdateResult
import com.junkfood.seal.desktop.ytdlp.readYtDlpVersion
import com.junkfood.seal.ui.PlatformVerticalScrollbar
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.additional_settings
import com.junkfood.seal.shared.generated.resources.auto_update
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.disabled
import com.junkfood.seal.shared.generated.resources.dismiss
import com.junkfood.seal.shared.generated.resources.every_day
import com.junkfood.seal.shared.generated.resources.every_month
import com.junkfood.seal.shared.generated.resources.every_week
import com.junkfood.seal.shared.generated.resources.env_pref_system
import com.junkfood.seal.shared.generated.resources.env_setup_missing
import com.junkfood.seal.shared.generated.resources.env_setup_sys_title
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
    val systemPathText = stringResource(Res.string.env_pref_system)
    val packageManagerText = stringResource(Res.string.env_setup_sys_title)
    val missingEnvironmentText = stringResource(Res.string.env_setup_missing)

    val scope = rememberCoroutineScope()
    val fetcher =
        remember(appSettings.environmentPreference) {
            YtDlpFetcher(environmentPreferenceProvider = { appSettings.environmentPreference })
        }
    var isUpdatingYtDlp by remember { mutableStateOf(false) }
    var ytDlpDesc by remember(ytdlpUpdateText) { mutableStateOf(ytdlpUpdateText) }
    var updateLogLine by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fetcher, ytdlpUpdateText, ytdlpVersionLabel) {
        ytDlpDesc =
            withContext(Dispatchers.IO) {
                buildDependencyDescription(
                    fetcher = fetcher,
                    ytdlpVersionLabel = ytdlpVersionLabel,
                    fallback = ytdlpUpdateText,
                    systemManagedText = "$systemPathText · $packageManagerText",
                    systemMissingText = "$systemPathText · $missingEnvironmentText",
                )
            }
    }

    Column {
        ActionWithDividerCard(
            title = stringResource(Res.string.ytdlp_update_action),
            description = ytDlpDesc,
            icon = Icons.Outlined.Update,
            trailingIcon = Icons.Outlined.Settings,
            enabled = !isUpdatingYtDlp,
            loading = isUpdatingYtDlp,
            useDisabledAlpha = false,
            onClick = {
                scope.launch {
                    isUpdatingYtDlp = true
                    updateLogLine = ytdlpUpdateText
                    ytDlpDesc = ytdlpUpdateText
                    val updated =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val result =
                                    fetcher.updateBinary(
                                        ytDlpUpdateChannel = appSettings.ytDlpUpdateChannel,
                                        onLog = { line ->
                                            scope.launch {
                                                updateLogLine = line.toStatusLine()
                                            }
                                        },
                                    )
                                if (result == YtDlpUpdateResult.Failed) error(ytdlpUpdateFailText)
                                buildDependencyDescription(
                                    fetcher = fetcher,
                                    ytdlpVersionLabel = ytdlpVersionLabel,
                                    fallback = ytdlpUpdateText,
                                    systemManagedText = "$systemPathText · $packageManagerText",
                                    systemMissingText = "$systemPathText · $missingEnvironmentText",
                                )
                            }
                        }

                    ytDlpDesc =
                        updated.getOrNull()?.takeIf { it.isNotBlank() } ?: ytdlpUpdateFailText

                    updateLogLine = null
                    isUpdatingYtDlp = false
                }
            },
            onTrailingClick = { showYtdlpDialog = true },
        )

        AnimatedVisibility(
            visible = isUpdatingYtDlp,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = updateLogLine ?: ytdlpUpdateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        DesktopYtdlpUpdateChannelDialog(
            visible = showYtdlpDialog,
            settings = appSettings,
            onUpdate = onUpdateAppSettings,
            onDismissRequest = { showYtdlpDialog = false }
        )
    }
}

private suspend fun buildDependencyDescription(
    fetcher: YtDlpFetcher,
    ytdlpVersionLabel: String,
    fallback: String,
    systemManagedText: String,
    systemMissingText: String,
): String {
    val resolution = fetcher.resolveDependencies()
    val ytDlp =
        resolution.ytDlp
            ?: return if (resolution.environmentPreference == EnvPrefSystem) {
                systemMissingText
            } else {
                "yt-dlp: missing"
            }
    val version = readYtDlpVersion(ytDlp.path)?.takeIf { it.isNotBlank() } ?: "unknown"
    val versionDescription = "yt-dlp (${ytDlp.source.label()}): $ytdlpVersionLabel: $version".ifBlank { fallback }
    return if (ytDlp.source == DesktopDependencySource.SystemPath) {
        "$versionDescription\n$systemManagedText"
    } else {
        versionDescription
    }
}

private fun String.toStatusLine(): String =
    lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .lastOrNull()
        ?: trim()

private fun DesktopDependencySource.label(): String =
    when (this) {
        DesktopDependencySource.AppPrivate -> "selfhost"
        DesktopDependencySource.SystemPath -> "system"
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
    val dialogScrollState = rememberScrollState()

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
            Box {
                Column(modifier = Modifier.verticalScroll(dialogScrollState).padding(end = 10.dp)) {
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
                    selected = updateChannel == DesktopAuxiliaryDownloader.YT_DLP_CHANNEL_STABLE,
                    onClick = { updateChannel = DesktopAuxiliaryDownloader.YT_DLP_CHANNEL_STABLE }
                )

                DialogSingleChoiceItemVariantWithLabel(
                    text = "yt-dlp-nightly-builds",
                    label = "Nightly",
                    selected = updateChannel == DesktopAuxiliaryDownloader.YT_DLP_CHANNEL_NIGHTLY,
                    isLabelTertiary = true,
                    onClick = { updateChannel = DesktopAuxiliaryDownloader.YT_DLP_CHANNEL_NIGHTLY }
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
                PlatformVerticalScrollbar(
                    state = dialogScrollState,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(top = 4.dp, bottom = 4.dp),
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
