package com.junkfood.seal.desktop.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.convert_audio_format
import com.junkfood.seal.shared.generated.resources.embed_metadata
import com.junkfood.seal.shared.generated.resources.embed_metadata_desc
import com.junkfood.seal.shared.generated.resources.embed_subtitles
import com.junkfood.seal.shared.generated.resources.embed_subtitles_desc
import com.junkfood.seal.shared.generated.resources.extract_audio
import com.junkfood.seal.shared.generated.resources.extract_audio_summary
import com.junkfood.seal.shared.generated.resources.format
import com.junkfood.seal.shared.generated.resources.format_selection_desc
import com.junkfood.seal.shared.generated.resources.format_sorting
import com.junkfood.seal.shared.generated.resources.format_sorting_desc
import com.junkfood.seal.shared.generated.resources.merge_audiostream
import com.junkfood.seal.shared.generated.resources.merge_audiostream_desc
import com.junkfood.seal.shared.generated.resources.remux_container_mkv
import com.junkfood.seal.shared.generated.resources.remux_container_mkv_desc
import com.junkfood.seal.shared.generated.resources.subtitle
import com.junkfood.seal.shared.generated.resources.subtitle_desc
import com.junkfood.seal.shared.generated.resources.video_quality
import org.jetbrains.compose.resources.stringResource
import com.junkfood.seal.util.DownloadPreferences

@Composable
internal fun FormatSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    var showAudioFormatDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }

    val videoQualityLabel = videoResolutionLabel(preferences.videoResolution)
    val audioFormatLabel = audioFormatLabel(preferences.audioConvertFormat)

    SettingsPageScaffold(title = stringResource(Res.string.format), onBack = onBack) {
        ToggleCard(
            title = stringResource(Res.string.extract_audio),
            description = stringResource(Res.string.extract_audio_summary),
            icon = Icons.Rounded.Terminal,
            checked = preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(extractAudio = checked) } }

        ToggleCard(
            title = stringResource(Res.string.convert_audio_format),
            description = audioFormatLabel,
            icon = Icons.Rounded.ViewComfy,
            checked = preferences.convertAudio,
            enabled = preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(convertAudio = checked) } }

        SelectionCard(
            title = stringResource(Res.string.convert_audio_format),
            description = audioFormatLabel,
            icon = Icons.Rounded.ViewComfy,
            onClick = { showAudioFormatDialog = true },
            enabled = preferences.extractAudio,
        )

        ToggleCard(
            title = stringResource(Res.string.embed_metadata),
            description = stringResource(Res.string.embed_metadata_desc),
            icon = Icons.Rounded.Info,
            checked = preferences.embedMetadata,
            enabled = preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(embedMetadata = checked) } }

        ToggleCard(
            title = stringResource(Res.string.subtitle),
            description = stringResource(Res.string.subtitle_desc),
            icon = Icons.Rounded.VideoFile,
            checked = preferences.downloadSubtitle,
        ) { checked -> onUpdate { it.copy(downloadSubtitle = checked) } }

        ToggleCard(
            title = stringResource(Res.string.embed_subtitles),
            description = stringResource(Res.string.embed_subtitles_desc),
            icon = Icons.Rounded.ViewComfy,
            checked = preferences.embedSubtitle,
            enabled = preferences.downloadSubtitle,
        ) { checked -> onUpdate { it.copy(embedSubtitle = checked, keepSubtitle = checked && preferences.keepSubtitle) } }

        ToggleCard(
            title = stringResource(Res.string.format_sorting),
            description = stringResource(Res.string.format_sorting_desc),
            icon = Icons.Rounded.SettingsApplications,
            checked = preferences.formatSorting,
        ) { checked -> onUpdate { it.copy(formatSorting = checked) } }

        TextFieldCard(
            title = stringResource(Res.string.format_sorting),
            description = stringResource(Res.string.format_selection_desc),
            icon = Icons.Rounded.SettingsApplications,
            value = preferences.sortingFields,
            enabled = preferences.formatSorting,
        ) { newValue -> onUpdate { it.copy(sortingFields = newValue) } }

        ToggleCard(
            title = stringResource(Res.string.merge_audiostream),
            description = stringResource(Res.string.merge_audiostream_desc),
            icon = Icons.Rounded.Terminal,
            checked = preferences.mergeAudioStream,
        ) { checked -> onUpdate { it.copy(mergeAudioStream = checked) } }

        ToggleCard(
            title = stringResource(Res.string.remux_container_mkv),
            description = stringResource(Res.string.remux_container_mkv_desc),
            icon = Icons.Rounded.VideoFile,
            checked = preferences.mergeToMkv,
            enabled = !preferences.extractAudio,
        ) { checked -> onUpdate { it.copy(mergeToMkv = checked) } }

        SelectionCard(
            title = stringResource(Res.string.video_quality),
            description = videoQualityLabel,
            icon = Icons.Rounded.VideoFile,
            onClick = { showVideoQualityDialog = true },
            enabled = !preferences.extractAudio,
        )

        if (showAudioFormatDialog) {
            ChoiceDialog(
                title = stringResource(Res.string.convert_audio_format),
                options = audioFormatOptions(),
                selected = preferences.audioConvertFormat,
                onSelect = { value -> onUpdate { it.copy(audioConvertFormat = value) } },
                onDismiss = { showAudioFormatDialog = false },
            )
        }

        if (showVideoQualityDialog) {
            ChoiceDialog(
                title = stringResource(Res.string.video_quality),
                options = videoQualityOptions(),
                selected = preferences.videoResolution,
                onSelect = { value -> onUpdate { it.copy(videoResolution = value) } },
                onDismiss = { showVideoQualityDialog = false },
            )
        }
    }
}

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

private fun videoResolutionLabel(code: Int): String =
    when (code) {
        0 -> "Best available"
        1 -> "2160p"
        2 -> "1440p"
        3 -> "1080p"
        4 -> "720p"
        5 -> "480p"
        6 -> "360p"
        7 -> "Lowest"
        else -> "Auto"
    }

private fun audioFormatOptions(): List<Pair<String, Int>> =
    listOf(
        "MP3" to 0,
        "M4A" to 1,
    )

private fun audioFormatLabel(code: Int): String =
    audioFormatOptions().firstOrNull { it.second == code }?.first ?: "Auto"
