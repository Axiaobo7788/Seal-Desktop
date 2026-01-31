@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.customcommand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.settings.DesktopAppSettingsState
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.PreferenceSubtitle
import com.junkfood.seal.desktop.settings.TextFieldCard
import com.junkfood.seal.desktop.settings.ToggleCard
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.custom_command_enabled_hint
import com.junkfood.seal.shared.generated.resources.custom_command_template
import com.junkfood.seal.shared.generated.resources.edit_template_desc
import com.junkfood.seal.shared.generated.resources.general_settings
import com.junkfood.seal.shared.generated.resources.reset
import com.junkfood.seal.shared.generated.resources.template_label
import com.junkfood.seal.shared.generated.resources.use_custom_command
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopCustomCommandScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = false,
    appSettingsState: DesktopAppSettingsState,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
    val settings = appSettingsState.settings

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.custom_command)) },
                navigationIcon = {
                    if (isCompact) {
                        androidx.compose.material3.IconButton(onClick = onMenuClick) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = stringResource(Res.string.custom_command),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreferenceInfo(text = stringResource(Res.string.custom_command_desc))

            PreferenceSubtitle(text = stringResource(Res.string.general_settings))

            ToggleCard(
                title = stringResource(Res.string.use_custom_command),
                description = stringResource(Res.string.custom_command_enabled_hint),
                icon = Icons.Rounded.Terminal,
                checked = settings.customCommandEnabled,
            ) { checked -> appSettingsState.update { it.copy(customCommandEnabled = checked) } }

            PreferenceSubtitle(text = stringResource(Res.string.custom_command_template))

            TextFieldCard(
                title = stringResource(Res.string.template_label),
                description = stringResource(Res.string.custom_command_template),
                icon = Icons.Rounded.Code,
                value = settings.customCommandLabel,
                enabled = settings.customCommandEnabled,
            ) { newValue -> appSettingsState.update { it.copy(customCommandLabel = newValue) } }

            TextFieldCard(
                title = stringResource(Res.string.custom_command_template),
                description = stringResource(Res.string.edit_template_desc),
                icon = Icons.Rounded.Code,
                value = settings.customCommandTemplate,
                enabled = settings.customCommandEnabled,
                singleLine = false,
                maxLines = 10,
            ) { newValue -> appSettingsState.update { it.copy(customCommandTemplate = newValue) } }

            Row(
                modifier = Modifier.fillMaxWidth().padding(PaddingValues(top = 4.dp)),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        appSettingsState.update {
                            it.copy(
                                customCommandEnabled = false,
                                customCommandLabel = "",
                                customCommandTemplate = "",
                            )
                        }
                    },
                ) {
                    Text(stringResource(Res.string.reset))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
