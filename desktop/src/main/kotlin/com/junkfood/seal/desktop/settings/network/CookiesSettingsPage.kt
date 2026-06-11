package com.junkfood.seal.desktop.settings.network

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator

import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.size
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.back
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.clear_all_cookies
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.cookies_in_database
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.cookies_usage_msg
import com.junkfood.seal.shared.generated.resources.export_to_file
import com.junkfood.seal.shared.generated.resources.generate_new_cookies
import com.junkfood.seal.shared.generated.resources.got_it
import com.junkfood.seal.shared.generated.resources.how_does_it_work
import com.junkfood.seal.shared.generated.resources.show_more_actions
import com.junkfood.seal.shared.generated.resources.ua_header
import com.junkfood.seal.shared.generated.resources.use_cookies
import com.junkfood.seal.util.DownloadPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Files
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText

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
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBrowserExtractDialog by remember { mutableStateOf(false) }
    var extractUrl by remember { mutableStateOf("") }
    var extractBrowser by remember { mutableStateOf(SupportedBrowser.entries.first()) }
    var isExtracting by remember { mutableStateOf(false) }
    var userAgent by remember { mutableStateOf(false) }
    var isCookieEnabled by remember { mutableStateOf(preferences.cookies) }
    var cookiesStats by remember { mutableStateOf(DesktopCookiesStats(0, 0)) }

    val cookiesFilePath = DesktopYtDlpPaths.cookiesFile()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cookiesStats = DesktopCookiesParser.parseStats(cookiesFilePath)
        }
    }

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
                                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                text = { Text("从文件导入 (Fallback)") },
                                onClick = {
                                    showMenu = false
                                    showImportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                                text = { Text(stringResource(Res.string.export_to_file)) },
                                enabled = cookiesFilePath.exists(),
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val dialog =
                                                FileDialog(
                                                    null as Frame?,
                                                    "Export Cookies",
                                                    FileDialog.SAVE,
                                                )
                                            dialog.file = "cookies.txt"
                                            dialog.isVisible = true
                                            if (dialog.file != null) {
                                                val target =
                                                    java.io.File(dialog.directory, dialog.file).toPath()
                                                cookiesFilePath.copyTo(target, overwrite = true)
                                            }
                                        }
                                    }
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.DeleteForever, null) },
                                text = { Text(stringResource(Res.string.clear_all_cookies)) },
                                enabled = cookiesFilePath.exists(),
                                onClick = {
                                    showMenu = false
                                    showClearConfirmDialog = true
                                },
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
                            onValueChange = { target ->
                                if (target && preferences.cookiesBrowser.isEmpty() && !cookiesFilePath.exists()) {
                                    showHelpDialog = true
                                    return@toggleable
                                }
                                isCookieEnabled = target
                                onUpdate { prefs -> prefs.copy(cookies = target) }
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
                    modifier = Modifier.clickable { showBrowserExtractDialog = true }
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
                Text(
                    text = stringResource(Res.string.cookies_in_database, cookiesStats.cookieCount, cookiesStats.siteCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }


    AnimatedAlertDialog(
        visible = showBrowserExtractDialog,
        onDismissRequest = { if (!isExtracting) showBrowserExtractDialog = false },
        icon = { Icon(Icons.Outlined.Cookie, null) },
        title = { Text(stringResource(Res.string.generate_new_cookies)) },
        text = {
            Column {
                Text(
                    text = "输入目标网站 URL 并选择浏览器以提取 Cookies",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = extractUrl,
                    onValueChange = { extractUrl = it },
                    label = { Text("URL") },
                    singleLine = true,
                    enabled = !isExtracting,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                
                var expanded by remember { mutableStateOf(false) }
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isExtracting) expanded = !expanded }
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = extractBrowser.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择浏览器") },
                        trailingIcon = {
                            androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isExtracting
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SupportedBrowser.entries.forEach { browser ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(browser.displayName) },
                                onClick = {
                                    extractBrowser = browser
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (isExtracting) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showBrowserExtractDialog = false },
                enabled = !isExtracting
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (extractUrl.isBlank()) return@TextButton
                    isExtracting = true
                    scope.launch {
                        val ytDlpPath = com.junkfood.seal.desktop.ytdlp.YtDlpFetcher().ensureBinary().toAbsolutePath().toString()
                        val success = withContext(Dispatchers.IO) {
                            runCatching {
                                val pb = ProcessBuilder(
                                    ytDlpPath,
                                    "--cookies-from-browser",
                                    extractBrowser.browserName,
                                    "--cookies",
                                    cookiesFilePath.toAbsolutePath().toString(),
                                    "--simulate",
                                    extractUrl
                                ).redirectErrorStream(true)
                                val p = pb.start()
                                p.waitFor() == 0
                            }.getOrDefault(false)
                        }
                        
                        if (success) {
                            withContext(Dispatchers.IO) {
                                cookiesStats = DesktopCookiesParser.parseStats(cookiesFilePath)
                            }
                        }
                        
                        isExtracting = false
                        showBrowserExtractDialog = false
                    }
                },
                enabled = !isExtracting && extractUrl.isNotBlank()
            ) {
                Text(stringResource(Res.string.generate_new_cookies))
            }
        },
    )

    AnimatedAlertDialog(
        visible = showClearConfirmDialog,
        onDismissRequest = { showClearConfirmDialog = false },
        icon = { Icon(Icons.Outlined.DeleteForever, null) },
        title = { Text(stringResource(Res.string.clear_all_cookies)) },
        text = { Text("This will delete the cookies file, and yt-dlp will no longer use stored cookies.") },
        dismissButton = {
            TextButton(onClick = { showClearConfirmDialog = false }) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            cookiesFilePath.deleteIfExists()
                            cookiesStats = DesktopCookiesParser.parseStats(cookiesFilePath)
                        }
                    }
                    showClearConfirmDialog = false
                }
            ) {
                Text(stringResource(Res.string.clear_all_cookies))
            }
        },
    )

    AnimatedAlertDialog(
        visible = showImportDialog,
        onDismissRequest = { showImportDialog = false },
        icon = { Icon(Icons.Outlined.Cookie, null) },
        title = { Text(stringResource(Res.string.generate_new_cookies)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.cookies_usage_msg),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { showImportDialog = false }) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showImportDialog = false
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val dialog =
                                FileDialog(
                                    null as Frame?,
                                    "Import Cookies",
                                    FileDialog.LOAD,
                                )
                            dialog.isVisible = true
                            val file = dialog.file
                            val dir = dialog.directory
                            if (file != null && dir != null) {
                                val source = java.io.File(dir, file).toPath()
                                if (DesktopCookiesParser.isValidCookiesFile(source)) {
                                    source.copyTo(cookiesFilePath, overwrite = true)
                                    cookiesStats =
                                        DesktopCookiesParser.parseStats(cookiesFilePath)
                                }
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
    )

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
