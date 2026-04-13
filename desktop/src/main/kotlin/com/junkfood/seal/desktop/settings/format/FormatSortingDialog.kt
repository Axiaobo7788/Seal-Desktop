package com.junkfood.seal.desktop.settings.format

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.format_sorting
import com.junkfood.seal.shared.generated.resources.format_sorting_desc
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.save
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.ui.platform.LocalUriHandler
import com.junkfood.seal.shared.generated.resources.import_from_preferences
import com.junkfood.seal.shared.generated.resources.yt_dlp_docs
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FormatSortingDialog(
    visible: Boolean,
    fields: String,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var sortingFields by remember(fields) { mutableStateOf(fields) }
    val uriHandler = LocalUriHandler.current

    AnimatedAlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.format_sorting)) },
        icon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) },
        confirmButton = {
            TextButton(onClick = { onConfirm(sortingFields) }) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        text = {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = stringResource(Res.string.format_sorting_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    value = sortingFields,
                    onValueChange = { sortingFields = it },
                    leadingIcon = { Text(text = "-S", fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedAssistChip(
                        onClick = onImport,
                        label = { Text(stringResource(Res.string.import_from_preferences)) },
                        leadingIcon = { Icon(Icons.Outlined.SettingsSuggest, null) }
                    )
                    ElevatedAssistChip(
                        onClick = { uriHandler.openUri("https://github.com/yt-dlp/yt-dlp#sorting-formats") },
                        label = { Text(stringResource(Res.string.yt_dlp_docs)) },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) }
                    )
                }
            }
        }
    )
}
