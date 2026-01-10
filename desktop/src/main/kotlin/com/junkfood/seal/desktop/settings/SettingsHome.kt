package com.junkfood.seal.desktop.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewComfy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.about
import com.junkfood.seal.shared.generated.resources.about_page
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.display_settings
import com.junkfood.seal.shared.generated.resources.download_directory
import com.junkfood.seal.shared.generated.resources.download_directory_desc
import com.junkfood.seal.shared.generated.resources.format
import com.junkfood.seal.shared.generated.resources.format_settings_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.general_settings_desc
import com.junkfood.seal.shared.generated.resources.interface_and_interaction
import com.junkfood.seal.shared.generated.resources.look_and_feel
import com.junkfood.seal.shared.generated.resources.network
import com.junkfood.seal.shared.generated.resources.network_settings_desc
import com.junkfood.seal.shared.generated.resources.settings
import com.junkfood.seal.shared.generated.resources.settings_before_download
import com.junkfood.seal.shared.generated.resources.trouble_shooting
import com.junkfood.seal.shared.generated.resources.trouble_shooting_desc
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingsHome(
    modifier: Modifier,
    isCompact: Boolean,
    onMenuClick: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val entries =
        listOf(
            SettingsEntry(
                title = stringResource(Res.string.general_settings),
                description = stringResource(Res.string.general_settings_desc),
                icon = Icons.Rounded.SettingsApplications,
                onClick = { onOpenPage(SettingsPage.General) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.download_directory),
                description = stringResource(Res.string.download_directory_desc),
                icon = Icons.Rounded.Folder,
                onClick = { onOpenPage(SettingsPage.Directory) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.format),
                description = stringResource(Res.string.format_settings_desc),
                icon = Icons.Rounded.VideoFile,
                onClick = { onOpenPage(SettingsPage.Format) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.network),
                description = stringResource(Res.string.network_settings_desc),
                icon = Icons.Rounded.SignalWifi4Bar,
                onClick = { onOpenPage(SettingsPage.Network) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.custom_command),
                description = stringResource(Res.string.custom_command_desc),
                icon = Icons.Rounded.Terminal,
                onClick = { onOpenPage(SettingsPage.Commands) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.look_and_feel),
                description = stringResource(Res.string.display_settings),
                icon = Icons.Rounded.Palette,
                onClick = { onOpenPage(SettingsPage.Appearance) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.interface_and_interaction),
                description = stringResource(Res.string.settings_before_download),
                icon = Icons.Rounded.ViewComfy,
                onClick = { onOpenPage(SettingsPage.Interaction) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.trouble_shooting),
                description = stringResource(Res.string.trouble_shooting_desc),
                icon = Icons.Rounded.BugReport,
                onClick = { onOpenPage(SettingsPage.Troubleshooting) },
            ),
            SettingsEntry(
                title = stringResource(Res.string.about),
                description = stringResource(Res.string.about_page),
                icon = Icons.Rounded.Info,
                onClick = { onOpenPage(SettingsPage.About) },
            ),
        )

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.settings)) },
                navigationIcon = {
                    if (isCompact) {
                        androidx.compose.material3.IconButton(onClick = onMenuClick) {
                            androidx.compose.material3.Icon(Icons.Outlined.Menu, contentDescription = stringResource(Res.string.settings))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 4.dp,
            bottom = innerPadding.calculateBottomPadding() + 12.dp,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries) { entry ->
                SettingRow(entry)
            }
        }
    }
}
