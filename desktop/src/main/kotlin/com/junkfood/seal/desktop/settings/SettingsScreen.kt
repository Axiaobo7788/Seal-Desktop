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
import com.junkfood.seal.desktop.theme.DesktopThemeState

internal enum class SettingsPage {
    General,
    Directory,
    Format,
    Subtitle,
    Network,
    Commands,
    Appearance,
    DarkTheme,
    Interaction,
    Troubleshooting,
    About,
    Credits,
}

@Composable
fun DesktopSettingsScreen(
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onMenuClick: () -> Unit = {},
    settingsState: DesktopSettingsState,
    appSettingsState: DesktopAppSettingsState,
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
                    onOpenSubtitle = { currentPage = SettingsPage.Subtitle },
                    onBack = { currentPage = null },
                )

            SettingsPage.Subtitle ->
                SubtitleSettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = SettingsPage.Format },
                )

            SettingsPage.Network ->
                NetworkSettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.Commands ->
                CommandSettingsPage(
                    settings = appSettingsState.settings,
                    onUpdate = appSettingsState::update,
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
                InteractionSettingsPage(
                    settings = appSettingsState.settings,
                    onUpdate = appSettingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.Troubleshooting ->
                TroubleshootingSettingsPage(
                    preferences = settingsState.preferences,
                    onUpdate = settingsState::update,
                    onBack = { currentPage = null },
                )

            SettingsPage.About ->
                AboutSettingsPage(
                    settings = appSettingsState.settings,
                    onUpdate = appSettingsState::update,
                    onOpenCredits = { currentPage = SettingsPage.Credits },
                    onBack = { currentPage = null },
                )

            SettingsPage.Credits ->
                CreditsSettingsPage(
                    onBack = { currentPage = SettingsPage.About },
                )
        }
    }
}
