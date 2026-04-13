package com.junkfood.seal.desktop.settings.subtitle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.runtime.CompositionLocalProvider
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.enable_experimental_feature
import com.junkfood.seal.shared.generated.resources.reset
import com.junkfood.seal.shared.generated.resources.yt_dlp_docs
import androidx.compose.ui.platform.LocalUriHandler
import com.junkfood.seal.desktop.settings.PreferenceSwitchWithContainer
import com.junkfood.seal.desktop.settings.DialogSingleChoiceItem
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEmbedSubtitleDialog by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(Res.string.subtitle), onBack = onBack) {
        PreferenceSubtitle(text = stringResource(Res.string.subtitle))

        PreferenceSwitchWithContainer(
            title = stringResource(Res.string.download_subtitles),
            description = null,
            icon = null,
            checked = preferences.downloadSubtitle,
        ) { checked -> onUpdate { it.copy(downloadSubtitle = checked) } }

        SelectionCard(
            title = stringResource(Res.string.subtitle_language),
            description = preferences.subtitleLanguage.ifEmpty { "en.*,.*-orig" },
            icon = Icons.Rounded.Language,
            enabled = preferences.downloadSubtitle,
            onClick = { showLanguageDialog = true },
        )

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

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

        ToggleCard(
            title = stringResource(Res.string.embed_subtitles),
            description = stringResource(Res.string.embed_subtitles_desc),
            icon = Icons.Rounded.Subtitles,
            checked = preferences.embedSubtitle,
            enabled = !preferences.extractAudio,
        ) { checked ->
            if (checked) {
                showEmbedSubtitleDialog = true
            } else {
                onUpdate {
                    it.copy(
                        embedSubtitle = false,
                        keepSubtitle = false,
                    )
                }
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

    SubtitleConversionDialog(
        visible = showConvertDialog,
        currentFormat = preferences.convertSubtitle,
        onDismissRequest = { showConvertDialog = false },
        onConfirm = { value -> onUpdate { it.copy(convertSubtitle = value) } }
    )

    AnimatedAlertDialog(
        visible = showEmbedSubtitleDialog,
        onDismissRequest = { showEmbedSubtitleDialog = false },
        icon = { Icon(Icons.Rounded.Subtitles, null) },
        title = { Text(stringResource(Res.string.enable_experimental_feature)) },
        text = { Text(stringResource(Res.string.embed_subtitles_mkv_msg)) },
        confirmButton = {
            TextButton(onClick = {
                onUpdate { it.copy(embedSubtitle = true) }
                showEmbedSubtitleDialog = false
            }) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { showEmbedSubtitleDialog = false }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )

    SubtitleLanguageDialog(
        visible = showLanguageDialog,
        initialLanguages = preferences.subtitleLanguage.ifEmpty { "en.*,.*-orig" },
        onDismissRequest = { showLanguageDialog = false },
        onConfirm = { value -> 
            onUpdate { it.copy(subtitleLanguage = value) }
            showLanguageDialog = false
        },
        onReset = {
            onUpdate { it.copy(subtitleLanguage = "en.*,.*-orig") }
        }
    )
}

@Composable
fun SubtitleLanguageDialog(
    visible: Boolean,
    initialLanguages: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    onReset: () -> Unit,
) {
    var languages by remember(initialLanguages) { mutableStateOf(initialLanguages) }
    val uriHandler = LocalUriHandler.current

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Language, null) },
        title = { Text(stringResource(Res.string.subtitle_language)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.subtitle_language_desc),
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProvideTextStyle(
                    value = LocalTextStyle.current.merge(fontFamily = FontFamily.Monospace)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = languages,
                        onValueChange = { languages = it },
                        label = { Text(stringResource(Res.string.subtitle_language)) },
                        singleLine = true,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedAssistChip(
                        onClick = onReset,
                        label = { Text(stringResource(Res.string.reset)) },
                        leadingIcon = { Icon(Icons.Rounded.Sync, null) }
                    )
                    ElevatedAssistChip(
                        onClick = { uriHandler.openUri("https://github.com/yt-dlp/yt-dlp#subtitle-options") },
                        label = { Text(stringResource(Res.string.yt_dlp_docs)) },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(languages) }
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
fun SubtitleConversionDialog(
    visible: Boolean,
    currentFormat: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedFormat by remember(currentFormat) { mutableStateOf(currentFormat) }

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.Sync, null) },
        title = { Text(stringResource(Res.string.convert_subtitle)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.convert_subtitle_desc),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    convertSubtitleOptions().forEach { (label, formatCode) ->
                        DialogSingleChoiceItem(
                            text = label,
                            selected = selectedFormat == formatCode,
                            onClick = { selectedFormat = formatCode }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedFormat) }
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
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
