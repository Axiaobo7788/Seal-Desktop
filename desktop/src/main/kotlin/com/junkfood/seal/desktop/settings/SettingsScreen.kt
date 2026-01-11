@file:OptIn(
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.junkfood.seal.desktop.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.about
import com.junkfood.seal.shared.generated.resources.about_page
import com.junkfood.seal.shared.generated.resources.custom_command
import com.junkfood.seal.shared.generated.resources.custom_command_desc
import com.junkfood.seal.shared.generated.resources.interface_and_interaction
import com.junkfood.seal.shared.generated.resources.settings_before_download
import com.junkfood.seal.shared.generated.resources.trouble_shooting
import com.junkfood.seal.shared.generated.resources.trouble_shooting_desc
import com.junkfood.seal.desktop.theme.DesktopThemeState
import org.jetbrains.compose.resources.stringResource

internal enum class SettingsPage {
    General,
    Directory,
    Format,
    Network,
    Commands,
    Appearance,
    DarkTheme,
    Interaction,
    Troubleshooting,
    About,
}

@Composable
fun DesktopSettingsScreen(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onMenuClick: () -> Unit = {},
    settingsState: DesktopSettingsState,
    themeState: DesktopThemeState,
) {
    var currentPage by remember { mutableStateOf<SettingsPage?>(null) }

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            val forward = targetState != null && initialState == null
            val incomingOffset: (Int) -> Int = { full -> (full / 3) * if (forward) 1 else -1 }
            val outgoingOffset: (Int) -> Int = { full -> (full / 3) * if (forward) -1 else 1 }

            (slideInHorizontally(animationSpec = tween(220), initialOffsetX = incomingOffset) +
                fadeIn()).togetherWith(
                slideOutHorizontally(animationSpec = tween(220), targetOffsetX = outgoingOffset) +
                    fadeOut(),
            )
        },
        modifier = modifier,
        label = "settings-pages",
    ) { page ->
        when (page) {
            null ->
                SettingsHome(
                    modifier = Modifier,
                    isCompact = isCompact,
                    onMenuClick = onMenuClick,
                    onOpenPage = { target -> currentPage = target },
                )

            SettingsPage.General ->
                GeneralSettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.Directory ->
                DirectorySettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.Format ->
                FormatSettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.Network ->
                NetworkSettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.Commands ->
                PlaceholderPage(
                    title = stringResource(Res.string.custom_command),
                    body = stringResource(Res.string.custom_command_desc),
                    onBack = { currentPage = null },
                )

            SettingsPage.Appearance ->
                AppearanceSettingsPage(
                    themeState = themeState,
                    onOpenDarkTheme = { currentPage = SettingsPage.DarkTheme },
                    onBack = { currentPage = null },
                )

            SettingsPage.DarkTheme ->
                DarkThemeSettingsPage(
                    themeState = themeState,
                    onBack = { currentPage = SettingsPage.Appearance },
                )

            SettingsPage.Interaction ->
                PlaceholderPage(
                    title = stringResource(Res.string.interface_and_interaction),
                    body = stringResource(Res.string.settings_before_download),
                    onBack = { currentPage = null },
                )

            SettingsPage.Troubleshooting ->
                PlaceholderPage(
                    title = stringResource(Res.string.trouble_shooting),
                    body = stringResource(Res.string.trouble_shooting_desc),
                    onBack = { currentPage = null },
                )

            SettingsPage.About ->
                PlaceholderPage(
                    title = stringResource(Res.string.about),
                    body = stringResource(Res.string.about_page),
                    onBack = { currentPage = null },
                )
        }
    }
}
