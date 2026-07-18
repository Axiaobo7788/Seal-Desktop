@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.junkfood.seal.desktop.sponsor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import com.junkfood.seal.desktop.settings.PreferenceInfo
import com.junkfood.seal.desktop.settings.SelectionCard
import com.junkfood.seal.ui.PlatformVerticalScrollbar
import com.junkfood.seal.ui.PlatformVerticalScrollbarGutter
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.readme
import com.junkfood.seal.shared.generated.resources.readme_desc
import com.junkfood.seal.shared.generated.resources.release
import com.junkfood.seal.shared.generated.resources.release_desc
import com.junkfood.seal.shared.generated.resources.sponsor
import com.junkfood.seal.shared.generated.resources.sponsor_desc
import org.jetbrains.compose.resources.stringResource

private const val repoUrl = "https://github.com/JunkFood02/Seal"
private const val releaseUrl = "https://github.com/JunkFood02/Seal/releases"
private const val sponsorUrl = "https://github.com/sponsors/JunkFood02"

@Composable
fun DesktopSponsorScreen(
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    isCompact: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current
    val contentScrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.sponsor)) },
                navigationIcon = {
                    if (isCompact) {
                        androidx.compose.material3.IconButton(onClick = onMenuClick) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = stringResource(Res.string.sponsor),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScrollState)
                    .padding(
                        start = 16.dp,
                        end = 16.dp + PlatformVerticalScrollbarGutter,
                        top = 4.dp,
                        bottom = 12.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreferenceInfo(text = stringResource(Res.string.sponsor_desc), icon = Icons.Rounded.VolunteerActivism)

                SelectionCard(
                    title = stringResource(Res.string.sponsor),
                    description = sponsorUrl,
                    icon = Icons.Rounded.VolunteerActivism,
                    onClick = { uriHandler.openUri(sponsorUrl) },
                )

                SelectionCard(
                    title = stringResource(Res.string.readme),
                    description = stringResource(Res.string.readme_desc),
                    icon = Icons.Rounded.Info,
                    onClick = { uriHandler.openUri(repoUrl) },
                )

                SelectionCard(
                    title = stringResource(Res.string.release),
                    description = stringResource(Res.string.release_desc),
                    icon = Icons.Rounded.NewReleases,
                    onClick = { uriHandler.openUri(releaseUrl) },
                )
            }
            PlatformVerticalScrollbar(
                state = contentScrollState,
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
