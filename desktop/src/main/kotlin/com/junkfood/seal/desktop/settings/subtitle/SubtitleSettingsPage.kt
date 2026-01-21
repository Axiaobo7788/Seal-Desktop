package com.junkfood.seal.desktop.settings.subtitle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.junkfood.seal.desktop.settings.ChoiceDialog
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.desktop.settings.SettingsPageScaffold
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.auto_subtitle
import com.junkfood.seal.shared.generated.resources.auto_subtitle_desc
import com.junkfood.seal.shared.generated.resources.auto_translated_subtitles
import com.junkfood.seal.shared.generated.resources.auto_translated_subtitles_msg
import com.junkfood.seal.shared.generated.resources.convert_subtitle
import com.junkfood.seal.shared.generated.resources.convert_subtitle_desc
import com.junkfood.seal.shared.generated.resources.convert_to
import com.junkfood.seal.shared.generated.resources.download_subtitles
import com.junkfood.seal.shared.generated.resources.embed_subtitles
import com.junkfood.seal.shared.generated.resources.embed_subtitles_desc
import com.junkfood.seal.shared.generated.resources.embed_subtitles_mkv_msg
import com.junkfood.seal.shared.generated.resources.keep_subtitle_files
import com.junkfood.seal.shared.generated.resources.not_convert
import com.junkfood.seal.shared.generated.resources.subtitle
import com.junkfood.seal.shared.generated.resources.subtitle_language
import com.junkfood.seal.shared.generated.resources.subtitle_language_desc
import com.junkfood.seal.shared.generated.resources.subtitle_sponsorblock
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SubtitleSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    var showConvertDialog by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(Res.string.subtitle), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.subtitle))

        ToggleCard(
            title = stringResource(Res.string.download_subtitles),
            description = null,
            icon = Icons.Rounded.Subtitles,
            checked = preferences.downloadSubtitle,
        ) { checked -> onUpdate { it.copy(downloadSubtitle = checked) } }

        TextFieldCard(
            title = stringResource(Res.string.subtitle_language),
            description = stringResource(Res.string.subtitle_language_desc),
            icon = Icons.Rounded.Language,
            value = preferences.subtitleLanguage,
            enabled = preferences.downloadSubtitle,
        ) { newValue -> onUpdate { it.copy(subtitleLanguage = newValue) } }

        SelectionCard(
            title = stringResource(Res.string.convert_subtitle),
            description = convertSubtitleLabel(preferences.convertSubtitle),
            icon = Icons.Rounded.Sync,
            enabled = preferences.downloadSubtitle,
            onClick = { showConvertDialog = true },
        )

        ToggleCard(
            title = stringResource(Res.string.auto_subtitle),
            description = stringResource(Res.string.auto_subtitle_desc),
            icon = Icons.Rounded.Subtitles,
            checked = preferences.autoSubtitle,
            enabled = preferences.downloadSubtitle,
        ) { checked ->
            onUpdate {
                it.copy(
                    autoSubtitle = checked,
                    autoTranslatedSubtitles = if (checked) preferences.autoTranslatedSubtitles else false,
                )
            }
        }

        ToggleCard(
            title = stringResource(Res.string.auto_translated_subtitles),
            description = null,
            icon = Icons.Rounded.Translate,
            checked = preferences.autoTranslatedSubtitles,
            enabled = preferences.downloadSubtitle && preferences.autoSubtitle,
        ) { checked -> onUpdate { it.copy(autoTranslatedSubtitles = checked) } }

        ToggleCard(
            title = stringResource(Res.string.embed_subtitles),
            description = stringResource(Res.string.embed_subtitles_desc),
            icon = Icons.Rounded.Subtitles,
            checked = preferences.embedSubtitle,
            enabled = !preferences.extractAudio,
        ) { checked ->
            onUpdate {
                it.copy(
                    embedSubtitle = checked,
                    keepSubtitle = if (checked) preferences.keepSubtitle else false,
                )
            }
        }

        ToggleCard(
            title = stringResource(Res.string.keep_subtitle_files),
            description = null,
            icon = Icons.Rounded.Save,
            checked = preferences.keepSubtitle,
            enabled = !preferences.extractAudio && preferences.embedSubtitle,
        ) { checked -> onUpdate { it.copy(keepSubtitle = checked) } }

        if (preferences.sponsorBlock) {
            PreferenceInfo(text = stringResource(Res.string.subtitle_sponsorblock))
        }
        if (preferences.embedSubtitle) {
            PreferenceInfo(text = stringResource(Res.string.embed_subtitles_mkv_msg))
        }
        if (preferences.autoTranslatedSubtitles) {
            PreferenceInfo(text = stringResource(Res.string.auto_translated_subtitles_msg))
        }
    }

    if (showConvertDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.convert_subtitle),
            options = convertSubtitleOptions(),
            selected = preferences.convertSubtitle,
            onSelect = { value -> onUpdate { it.copy(convertSubtitle = value) } },
            onDismiss = { showConvertDialog = false },
        )
    }
}

@Composable
private fun convertSubtitleLabel(code: Int): String =
    when (code) {
        1 -> stringResource(Res.string.convert_to, "ASS")
        2 -> stringResource(Res.string.convert_to, "LRC")
        3 -> stringResource(Res.string.convert_to, "SRT")
        4 -> stringResource(Res.string.convert_to, "VTT")
        else -> stringResource(Res.string.not_convert)
    }

@Composable
private fun convertSubtitleOptions(): List<Pair<String, Int>> =
    listOf(
        stringResource(Res.string.not_convert) to 0,
        stringResource(Res.string.convert_to, "ASS") to 1,
        stringResource(Res.string.convert_to, "LRC") to 2,
        stringResource(Res.string.convert_to, "SRT") to 3,
        stringResource(Res.string.convert_to, "VTT") to 4,
    )
