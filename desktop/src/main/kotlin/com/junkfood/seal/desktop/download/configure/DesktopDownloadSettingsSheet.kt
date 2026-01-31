@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.download.configure

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.download.DesktopDownloadType
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.additional_settings
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.auto
import com.junkfood.seal.shared.generated.resources.best_quality
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.create_thumbnail
import com.junkfood.seal.shared.generated.resources.custom
import com.junkfood.seal.shared.generated.resources.download
import com.junkfood.seal.shared.generated.resources.download_subtitles
import com.junkfood.seal.shared.generated.resources.edit_preset
import com.junkfood.seal.shared.generated.resources.embed_metadata
import com.junkfood.seal.shared.generated.resources.format_selection
import com.junkfood.seal.shared.generated.resources.format_selection_desc
import com.junkfood.seal.shared.generated.resources.legacy
import com.junkfood.seal.shared.generated.resources.lowest_quality
import com.junkfood.seal.shared.generated.resources.new_task
import com.junkfood.seal.shared.generated.resources.paste_msg
import com.junkfood.seal.shared.generated.resources.playlist
import com.junkfood.seal.shared.generated.resources.preset
import com.junkfood.seal.shared.generated.resources.proceed
import com.junkfood.seal.shared.generated.resources.quality
import com.junkfood.seal.shared.generated.resources.save
import com.junkfood.seal.shared.generated.resources.settings_before_download
import com.junkfood.seal.shared.generated.resources.sponsorblock
import com.junkfood.seal.shared.generated.resources.start_download
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.shared.generated.resources.video_format_preference
import com.junkfood.seal.shared.generated.resources.video_quality
import com.junkfood.seal.shared.generated.resources.video_url
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DownloadInputSheet(
    url: String,
    onUrlChange: (String) -> Unit,
    onPasteIntoUrl: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onProceed: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Text(stringResource(Res.string.new_task), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text(stringResource(Res.string.video_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    val clip = clipboard.getText()?.text
                    if (!clip.isNullOrBlank()) onPasteIntoUrl(clip)
                },
            ) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.paste_msg))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismissRequest) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.cancel))
                }
                Button(onClick = onProceed, enabled = url.isNotBlank()) {
                    Text(stringResource(Res.string.proceed))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
internal fun DownloadOptionsSheet(
    urlPreview: String,
    downloadType: DesktopDownloadType,
    onTypeChange: (DesktopDownloadType) -> Unit,
    preferences: DownloadPreferences,
    onPreferencesChange: (DownloadPreferences) -> Unit,
    onDismissRequest: () -> Unit,
    onDownload: () -> Unit,
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    val formatSummary = formatSummary(preferences, downloadType)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.DoneAll, contentDescription = null)
            Text(
                stringResource(Res.string.settings_before_download),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                urlPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SectionTitle(text = stringResource(Res.string.download))
        DownloadTypeSegmentedRow(selected = downloadType, onSelect = onTypeChange)

        SectionTitle(text = stringResource(Res.string.format_selection))
        FormatSelectionSection(
            summary = formatSummary,
            onPresetClick = { showPresetDialog = true },
            onCustomClick = { showPresetDialog = true },
            showEdit = downloadType != DesktopDownloadType.Playlist,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { showAdvanced = !showAdvanced },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionTitle(text = stringResource(Res.string.additional_settings))
            Icon(if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }

        if (showAdvanced) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OptionChipRow(
                    title = stringResource(Res.string.download_subtitles),
                    checked = preferences.downloadSubtitle,
                    onCheckedChange = { onPreferencesChange(preferences.copy(downloadSubtitle = it, embedSubtitle = it && preferences.embedSubtitle)) },
                )
                OptionChipRow(
                    title = stringResource(Res.string.create_thumbnail),
                    checked = preferences.embedThumbnail,
                    onCheckedChange = { onPreferencesChange(preferences.copy(embedThumbnail = it)) },
                )
                OptionChipRow(
                    title = stringResource(Res.string.embed_metadata),
                    checked = preferences.embedMetadata,
                    onCheckedChange = { onPreferencesChange(preferences.copy(embedMetadata = it)) },
                )
                OptionChipRow(
                    title = stringResource(Res.string.sponsorblock),
                    checked = preferences.sponsorBlock,
                    onCheckedChange = { onPreferencesChange(preferences.copy(sponsorBlock = it)) },
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onDismissRequest) {
                Icon(Icons.Outlined.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.cancel))
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onDownload) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.start_download))
            }
        }
    }

    if (showPresetDialog && downloadType != DesktopDownloadType.Audio) {
        var resolution by remember(preferences) { mutableIntStateOf(preferences.videoResolution) }
        var format by remember(preferences) { mutableIntStateOf(preferences.videoFormat) }

        VideoPresetDialog(
            videoResolution = resolution,
            videoFormatPreference = format,
            onResolutionSelect = { resolution = it },
            onFormatSelect = { format = it },
            onDismissRequest = { showPresetDialog = false },
            onSave = {
                onPreferencesChange(
                    preferences.copy(
                        videoResolution = resolution,
                        videoFormat = format,
                    ),
                )
            },
        )
    }
}

@Composable
private fun DownloadTypeSegmentedRow(selected: DesktopDownloadType, onSelect: (DesktopDownloadType) -> Unit) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, outline),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            DesktopDownloadType.entries.forEachIndexed { index, type ->
                val isSelected = selected == type
                val icon = when (type) {
                    DesktopDownloadType.Audio -> Icons.Outlined.LibraryMusic
                    DesktopDownloadType.Video -> Icons.Outlined.VideoFile
                    DesktopDownloadType.Playlist -> Icons.Outlined.PlaylistAdd
                }
                val label =
                    when (type) {
                        DesktopDownloadType.Audio -> stringResource(Res.string.audio)
                        DesktopDownloadType.Video -> stringResource(Res.string.video)
                        DesktopDownloadType.Playlist -> stringResource(Res.string.playlist)
                    }
                val segmentShape =
                    when (index) {
                        0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                        DesktopDownloadType.entries.lastIndex ->
                            RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                val background = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent
                val contentColor =
                    if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(segmentShape)
                        .clickable { onSelect(type) }
                        .background(background)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = contentColor)
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(icon, contentDescription = null, tint = contentColor)
                        Spacer(Modifier.width(6.dp))
                        Text(label, color = contentColor)
                    }
                }

                if (index != DesktopDownloadType.entries.lastIndex) {
                    VerticalDivider(color = outline)
                }
            }
        }
    }
}

@Composable
private fun FormatSelectionSection(
    summary: String,
    onPresetClick: () -> Unit,
    onCustomClick: () -> Unit,
    showEdit: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.large,
            onClick = onPresetClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.SettingsSuggest, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.preset), style = MaterialTheme.typography.titleSmall)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showEdit) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp,
            shape = MaterialTheme.shapes.large,
            onClick = onCustomClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.custom), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(Res.string.format_selection_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun OptionChipRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val background = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (checked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (checked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.height(36.dp),
        shape = MaterialTheme.shapes.large,
        color = background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, border),
        onClick = { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, color = content, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun VideoPresetDialog(
    videoResolution: Int,
    videoFormatPreference: Int,
    onResolutionSelect: (Int) -> Unit,
    onFormatSelect: (Int) -> Unit,
    onSave: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val formatOptions =
        listOf(
            stringResource(Res.string.quality) to 2,
            stringResource(Res.string.legacy) to 1,
        )
    val resolutionOptions = listOf(0, 1, 2, 3, 4, 5, 6, 7)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.edit_preset)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.video_format_preference), style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    formatOptions.forEach { (label, value) ->
                        ChoiceRow(
                            title = label,
                            selected = videoFormatPreference == value,
                            onClick = { onFormatSelect(value) },
                        )
                    }
                }

                HorizontalDivider()

                Text(stringResource(Res.string.video_quality), style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    resolutionOptions.forEach { value ->
                        ChoiceRow(
                            title = videoResolutionLabel(value),
                            selected = videoResolution == value,
                            onClick = { onResolutionSelect(value) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave()
                onDismissRequest()
            }) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

@Composable
private fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun formatSummary(preferences: DownloadPreferences, type: DesktopDownloadType): String {
    val resLabel = videoResolutionLabel(preferences.videoResolution)
    val audioLabel = audioQualityLabel(preferences.audioQuality)
    return when (type) {
        DesktopDownloadType.Audio -> "${stringResource(Res.string.audio)} · $audioLabel"
        DesktopDownloadType.Video -> "${stringResource(Res.string.video)} · $resLabel"
        DesktopDownloadType.Playlist -> "${stringResource(Res.string.playlist)} · $resLabel"
    }
}

@Composable
private fun videoResolutionLabel(code: Int): String =
    when (code) {
        0 -> stringResource(Res.string.best_quality)
        1 -> "2160p"
        2 -> "1440p"
        3 -> "1080p"
        4 -> "720p"
        5 -> "480p"
        6 -> "360p"
        7 -> stringResource(Res.string.lowest_quality)
        else -> stringResource(Res.string.auto)
    }

@Composable
private fun audioQualityLabel(code: Int): String =
    when (code) {
        1 -> stringResource(Res.string.best_quality)
        2 -> "Medium"
        3 -> "Low"
        4 -> "Very low"
        else -> stringResource(Res.string.auto)
    }
