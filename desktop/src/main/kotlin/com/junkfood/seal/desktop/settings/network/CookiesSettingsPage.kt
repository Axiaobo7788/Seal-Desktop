package com.junkfood.seal.desktop.settings.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.size
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.back
import com.junkfood.seal.shared.generated.resources.clear_all_cookies
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.cookies_in_database
import com.junkfood.seal.shared.generated.resources.cookies_usage_msg
import com.junkfood.seal.shared.generated.resources.export_to_file
import com.junkfood.seal.shared.generated.resources.generate_new_cookies
import com.junkfood.seal.shared.generated.resources.got_it
import com.junkfood.seal.shared.generated.resources.how_does_it_work
import com.junkfood.seal.shared.generated.resources.show_more_actions
import com.junkfood.seal.shared.generated.resources.ua_header
import com.junkfood.seal.shared.generated.resources.use_cookies
import com.junkfood.seal.util.DownloadPreferences
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CookiesSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = { true },
        )

    var showMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var userAgent by remember { mutableStateOf(false) } // Placeholders
    var isCookieEnabled by remember { mutableStateOf(preferences.cookies) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.cookies)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = stringResource(Res.string.how_does_it_work))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(Res.string.show_more_actions))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Checkbox(
                                        checked = userAgent,
                                        onCheckedChange = null,
                                    )
                                },
                                text = { Text(stringResource(Res.string.ua_header)) },
                                onClick = { 
                                    userAgent = !userAgent
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                                text = { Text(stringResource(Res.string.export_to_file)) },
                                onClick = { showMenu = false },
                                enabled = false
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.DeleteForever, null) },
                                text = { Text(stringResource(Res.string.clear_all_cookies)) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item {
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .toggleable(
                            value = isCookieEnabled,
                            onValueChange = { 
                                isCookieEnabled = it
                                onUpdate { prefs -> prefs.copy(cookies = it) }
                            },
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.use_cookies),
                            maxLines = 2,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Switch(
                        checked = isCookieEnabled,
                        interactionSource = interactionSource,
                        onCheckedChange = null,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.clickable { /* TODO: Open CookieGeneratorDialog */ }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp, 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp, end = 16.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.generate_new_cookies),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                val cookiesCount = 0
                val siteCount = 0

                Text(
                    text = stringResource(Res.string.cookies_in_database, cookiesCount, siteCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }

    AnimatedAlertDialog(
        visible = showHelpDialog,
        onDismissRequest = { showHelpDialog = false },
        icon = { Icon(Icons.Outlined.HelpOutline, null) },
        title = { Text(stringResource(Res.string.how_does_it_work)) },
        text = { Text(stringResource(Res.string.cookies_usage_msg)) },
        confirmButton = {
            TextButton(onClick = { showHelpDialog = false }) {
                Text(stringResource(Res.string.got_it))
            }
        }
    )
}
