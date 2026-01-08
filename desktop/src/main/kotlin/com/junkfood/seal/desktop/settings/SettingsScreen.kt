@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.junkfood.seal.desktop.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopSettingsScreen(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onMenuClick: () -> Unit = {},
) {
    val entries =
        listOf(
            SettingsEntry(
                title = stringResource(Res.string.general_settings),
                description = stringResource(Res.string.general_settings_desc),
                icon = Icons.Rounded.SettingsApplications,
            ),
            SettingsEntry(
                title = stringResource(Res.string.download_directory),
                description = stringResource(Res.string.download_directory_desc),
                icon = Icons.Rounded.Folder,
            ),
            SettingsEntry(
                title = stringResource(Res.string.format),
                description = stringResource(Res.string.format_settings_desc),
                icon = Icons.Rounded.VideoFile,
            ),
            SettingsEntry(
                title = stringResource(Res.string.network),
                description = stringResource(Res.string.network_settings_desc),
                icon = Icons.Rounded.SignalWifi4Bar,
            ),
            SettingsEntry(
                title = stringResource(Res.string.custom_command),
                description = stringResource(Res.string.custom_command_desc),
                icon = Icons.Rounded.Terminal,
            ),
            SettingsEntry(
                title = stringResource(Res.string.look_and_feel),
                description = stringResource(Res.string.display_settings),
                icon = Icons.Rounded.Palette,
            ),
            SettingsEntry(
                title = stringResource(Res.string.interface_and_interaction),
                description = stringResource(Res.string.settings_before_download),
                icon = Icons.Rounded.ViewComfy,
            ),
            SettingsEntry(
                title = stringResource(Res.string.trouble_shooting),
                description = stringResource(Res.string.trouble_shooting_desc),
                icon = Icons.Rounded.BugReport,
            ),
            SettingsEntry(
                title = stringResource(Res.string.about),
                description = stringResource(Res.string.about_page),
                icon = Icons.Rounded.Info,
            ),
        )

    Column(modifier = modifier.fillMaxSize()) {
        SettingsHeader(isCompact = isCompact, onMenuClick = onMenuClick)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HighlightCard() }
            items(entries) { entry ->
                SettingRow(entry)
            }
        }
    }
}

@Composable
private fun SettingsHeader(isCompact: Boolean, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isCompact) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = stringResource(Res.string.expand),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HighlightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.battery_configuration), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Res.string.battery_configuration_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class SettingsEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun SettingRow(entry: SettingsEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { entry.onClick?.invoke() },
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } 
        }
    }
}
