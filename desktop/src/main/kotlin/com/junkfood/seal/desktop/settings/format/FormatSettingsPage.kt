package com.junkfood.seal.desktop.settings.format

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.settings.ChoiceDialog
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.desktop.settings.SwitchWithDividerCard
import com.junkfood.seal.desktop.settings.format.FormatSortingDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.advanced_settings
import com.junkfood.seal.shared.generated.resources.audio
import com.junkfood.seal.shared.generated.resources.audio_format_preference
import com.junkfood.seal.shared.generated.resources.audio_quality
import com.junkfood.seal.shared.generated.resources.auto
import com.junkfood.seal.shared.generated.resources.best_quality
import com.junkfood.seal.shared.generated.resources.convert_audio
import com.junkfood.seal.shared.generated.resources.convert_audio_format
import com.junkfood.seal.shared.generated.resources.convert_audio_format_desc
import com.junkfood.seal.shared.generated.resources.crop_artwork
import com.junkfood.seal.shared.generated.resources.crop_artwork_desc
import com.junkfood.seal.shared.generated.resources.custom
import com.junkfood.seal.shared.generated.resources.embed_metadata
import com.junkfood.seal.shared.generated.resources.embed_metadata_desc
import com.junkfood.seal.shared.generated.resources.embed_thumbnail
import com.junkfood.seal.shared.generated.resources.embed_thumbnail_desc
import com.junkfood.seal.shared.generated.resources.extract_audio
import com.junkfood.seal.shared.generated.resources.extract_audio_summary
import com.junkfood.seal.shared.generated.resources.format
import com.junkfood.seal.shared.generated.resources.format_selection
import com.junkfood.seal.shared.generated.resources.format_selection_desc
import com.junkfood.seal.shared.generated.resources.format_sorting
import com.junkfood.seal.shared.generated.resources.format_sorting_desc
import com.junkfood.seal.shared.generated.resources.lowest_bitrate
import com.junkfood.seal.shared.generated.resources.lowest_quality
import com.junkfood.seal.shared.generated.resources.merge_audiostream
import com.junkfood.seal.shared.generated.resources.merge_audiostream_desc
import com.junkfood.seal.shared.generated.resources.presets
import com.junkfood.seal.shared.generated.resources.prefer_compatibility_desc
import com.junkfood.seal.shared.generated.resources.prefer_quality_desc
import com.junkfood.seal.shared.generated.resources.quality
import com.junkfood.seal.shared.generated.resources.remux_container_mkv
import com.junkfood.seal.shared.generated.resources.remux_container_mkv_desc
import com.junkfood.seal.shared.generated.resources.subtitle
import com.junkfood.seal.shared.generated.resources.subtitle_desc
import com.junkfood.seal.shared.generated.resources.video
import com.junkfood.seal.shared.generated.resources.video_format_preference
import com.junkfood.seal.shared.generated.resources.video_quality
import com.junkfood.seal.shared.generated.resources.video_quality_desc
import com.junkfood.seal.shared.generated.resources.clip_video
import com.junkfood.seal.shared.generated.resources.clip_video_desc
import com.junkfood.seal.shared.generated.resources.clip_video_dialog_msg
import com.junkfood.seal.shared.generated.resources.enable_experimental_feature
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ContentCut
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton

@Composable
internal fun FormatSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    isVideoClipEnabled: Boolean,
    onUpdateVideoClipEnabled: (Boolean) -> Unit,
    isFormatSelectionEnabled: Boolean,
    onUpdateFormatSelectionEnabled: (Boolean) -> Unit,
    onOpenSubtitle: () -> Unit,
    onBack: () -> Unit,
) {
    var showAudioPresetDialog by remember { mutableStateOf(false) }
    var showAudioFormatDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showConvertAudioDialog by remember { mutableStateOf(false) }
    var showVideoFormatDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showVideoClipDialog by remember { mutableStateOf(false) }
    var showFormatSortingDialog by remember { mutableStateOf(false) }

    val videoQualityLabel = videoResolutionLabel(preferences.videoResolution)
    val audioPresetLabel = audioPresetLabel(preferences.useCustomAudioPreset)
    val audioConvertLabel = audioConvertFormatLabel(preferences.audioConvertFormat)
    val videoFormatDesc = videoFormatDescription(preferences.videoFormat)

    SettingsPageScaffold(title = stringResource(Res.string.format), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.audio))

        ToggleCard(
            title = stringResource(Res.string.extract_audio),
            description = stringResource(Res.string.extract_audio_summary),
            icon = Icons.Rounded.Terminal,
            checked = preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(extractAudio = checked) } }

        SwitchWithDividerCard(
            title = stringResource(Res.string.convert_audio_format),
            description = audioConvertLabel,
            icon = Icons.Rounded.Sync,
            checked = preferences.convertAudio,
            enabled = preferences.extractAudio,
            onCheckedChange = { checked -> onUpdate { it.copy(convertAudio = checked) } },
            onClick = { showConvertAudioDialog = true },
        )

        ToggleCard(
            title = stringResource(Res.string.embed_metadata),
            description = stringResource(Res.string.embed_metadata_desc),
            icon = Icons.Rounded.Info,
            checked = preferences.embedMetadata,
            enabled = preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(embedMetadata = checked) } }

        ToggleCard(
            title = stringResource(Res.string.crop_artwork),
            description = stringResource(Res.string.crop_artwork_desc),
            icon = Icons.Rounded.Crop,
            checked = preferences.cropArtwork,
            enabled = preferences.extractAudio && preferences.embedMetadata,
        ) { checked -> onUpdate { it.copy(cropArtwork = checked) } }

        PreferenceSubtitle(text = stringResource(Res.string.video))

        SelectionCard(
            title = stringResource(Res.string.video_format_preference),
            description = videoFormatDesc,
            icon = Icons.Rounded.VideoFile,
            onClick = { showVideoFormatDialog = true },
            enabled = !preferences.extractAudio,
        )

        SelectionCard(
            title = stringResource(Res.string.video_quality),
            description = videoQualityLabel,
            icon = Icons.Rounded.VideoFile,
            onClick = { showVideoQualityDialog = true },
            enabled = !preferences.extractAudio,
        )

        ToggleCard(
            title = stringResource(Res.string.remux_container_mkv),
            description = stringResource(Res.string.remux_container_mkv_desc),
            icon = Icons.Rounded.VideoFile,
            checked = preferences.mergeToMkv,
            enabled = !preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(mergeToMkv = checked) } }

        PreferenceSubtitle(text = stringResource(Res.string.advanced_settings))

        SelectionCard(
            title = stringResource(Res.string.subtitle),
            description = stringResource(Res.string.subtitle_desc),
            icon = Icons.Rounded.Subtitles,
            onClick = onOpenSubtitle,
        )

        SwitchWithDividerCard(
            title = stringResource(Res.string.format_sorting),
            description = stringResource(Res.string.format_sorting_desc),
            icon = Icons.AutoMirrored.Rounded.Sort,
            checked = preferences.formatSorting,
            onCheckedChange = { checked -> onUpdate { it.copy(formatSorting = checked) } },
            onClick = { showFormatSortingDialog = true },
            enabled = !preferences.extractAudio,
        )

        ToggleCard(
            title = stringResource(Res.string.format_selection),
            description = stringResource(Res.string.format_selection_desc),
            icon = Icons.Rounded.SettingsApplications,
            checked = isFormatSelectionEnabled,
            enabled = !preferences.extractAudio,
        ) { checked -> onUpdateFormatSelectionEnabled(checked) }

        ToggleCard(
            title = stringResource(Res.string.clip_video),
            description = stringResource(Res.string.clip_video_desc),
            icon = Icons.Rounded.ContentCut,
            checked = isVideoClipEnabled,
            enabled = !preferences.extractAudio && isFormatSelectionEnabled,
        ) { checked ->
            if (checked) {
                showVideoClipDialog = true
            } else {
                onUpdateVideoClipEnabled(false)
            }
        }

        ToggleCard(
            title = stringResource(Res.string.merge_audiostream),
            description = stringResource(Res.string.merge_audiostream_desc),
            icon = Icons.Rounded.GraphicEq,
            checked = preferences.mergeAudioStream,
            enabled = !preferences.extractAudio && isFormatSelectionEnabled,
        ) { checked -> onUpdate { it.copy(mergeAudioStream = checked) } }

        FormatSortingDialog(
            visible = showFormatSortingDialog,
            fields = preferences.sortingFields,
            onDismiss = { showFormatSortingDialog = false },
            onImport = { /* No-op on desktop for now */ },
            onConfirm = { value -> 
                onUpdate { it.copy(sortingFields = value) }
                showFormatSortingDialog = false
            }
        )

        ChoiceDialog(
            visible = showConvertAudioDialog,
            title = stringResource(Res.string.convert_audio_format),
            options = audioConvertFormatOptions(),
            selected = preferences.audioConvertFormat,
            onSelect = { value -> onUpdate { it.copy(audioConvertFormat = value) } },
            onDismiss = { showConvertAudioDialog = false },
        )

        ChoiceDialog(
            visible = showVideoFormatDialog,
            title = stringResource(Res.string.video_format_preference),
            options = videoFormatOptions(),
            selected = preferences.videoFormat,
            onSelect = { value -> onUpdate { it.copy(videoFormat = value) } },
            onDismiss = { showVideoFormatDialog = false },
        )

        ChoiceDialog(
            visible = showVideoQualityDialog,
            title = stringResource(Res.string.video_quality),
            options = videoQualityOptions(),
            selected = preferences.videoResolution,
            onSelect = { value -> onUpdate { it.copy(videoResolution = value) } },
            onDismiss = { showVideoQualityDialog = false },
        )

        AnimatedAlertDialog(
            visible = showVideoClipDialog,
            onDismissRequest = { showVideoClipDialog = false },
            icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
            title = { Text(stringResource(Res.string.clip_video)) },
            text = { Text(stringResource(Res.string.clip_video_dialog_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showVideoClipDialog = false
                    onUpdateVideoClipEnabled(true)
                }) {
                    Text(stringResource(Res.string.enable_experimental_feature))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoClipDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun videoQualityOptions(): List<Pair<String, Int>> =
    listOf(
        videoResolutionLabel(0) to 0,
        videoResolutionLabel(1) to 1,
        videoResolutionLabel(2) to 2,
        videoResolutionLabel(3) to 3,
        videoResolutionLabel(4) to 4,
        videoResolutionLabel(5) to 5,
        videoResolutionLabel(6) to 6,
        videoResolutionLabel(7) to 7,
    )

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
private fun audioConvertFormatOptions(): List<Pair<String, Int>> =
    listOf(
        "MP3" to 0,
        "M4A" to 1,
    )

@Composable
private fun audioConvertFormatLabel(code: Int): String =
    audioConvertFormatOptions().firstOrNull { it.second == code }?.first ?: "MP3"

@Composable
private fun audioPresetOptions(): List<Pair<String, Boolean>> =
    listOf(
        stringResource(Res.string.best_quality) to false,
        stringResource(Res.string.custom) to true,
    )

@Composable
private fun audioPresetLabel(useCustom: Boolean): String =
    if (useCustom) stringResource(Res.string.custom) else stringResource(Res.string.best_quality)

@Composable
private fun videoFormatOptions(): List<Pair<String, Int>> =
    listOf(
        stringResource(Res.string.quality) to 2,
        "MP4(H.264)" to 1,
    )

@Composable
private fun videoFormatLabel(code: Int): String =
    videoFormatOptions().firstOrNull { it.second == code }?.first ?: stringResource(Res.string.auto)

@Composable
private fun videoFormatDescription(code: Int): String =
    when (code) {
        1 -> stringResource(Res.string.prefer_compatibility_desc)
        2 -> stringResource(Res.string.prefer_quality_desc)
        else -> stringResource(Res.string.video_quality_desc)
    }
