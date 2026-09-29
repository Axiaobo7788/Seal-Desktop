package com.junkfood.seal.desktop.settings.network

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.cookies.DesktopCookieCache
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheMetadata
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheSnapshot
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheSource
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheStatus
import com.junkfood.seal.desktop.cookies.DesktopCookieContext
import com.junkfood.seal.desktop.cookies.DesktopCookieExtractionResult
import com.junkfood.seal.desktop.cookies.DesktopCookieExtractor
import com.junkfood.seal.desktop.cookies.DesktopCookiesStats
import com.junkfood.seal.desktop.cookies.SupportedBrowser
import com.junkfood.seal.desktop.cookies.normalizeCookieValidationUrl
import com.junkfood.seal.desktop.i18n.AndroidStrings
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.Res
import com.junkfood.seal.shared.generated.resources.back
import com.junkfood.seal.shared.generated.resources.cancel
import com.junkfood.seal.shared.generated.resources.clear_all_cookies
import com.junkfood.seal.shared.generated.resources.confirm
import com.junkfood.seal.shared.generated.resources.cookies
import com.junkfood.seal.shared.generated.resources.desktop_cookies_cache_missing
import com.junkfood.seal.shared.generated.resources.desktop_cookies_cache_ready
import com.junkfood.seal.shared.generated.resources.desktop_cookies_delete_file_desc
import com.junkfood.seal.shared.generated.resources.desktop_cookies_export_dialog_title
import com.junkfood.seal.shared.generated.resources.desktop_cookies_extract_description
import com.junkfood.seal.shared.generated.resources.desktop_cookies_help_msg
import com.junkfood.seal.shared.generated.resources.desktop_cookies_import_dialog_title
import com.junkfood.seal.shared.generated.resources.desktop_cookies_import_from_file
import com.junkfood.seal.shared.generated.resources.desktop_cookies_last_generated
import com.junkfood.seal.shared.generated.resources.desktop_cookies_last_validation
import com.junkfood.seal.shared.generated.resources.desktop_cookies_select_browser
import com.junkfood.seal.shared.generated.resources.desktop_cookies_source_imported
import com.junkfood.seal.shared.generated.resources.desktop_cookies_source_none
import com.junkfood.seal.shared.generated.resources.desktop_cookies_source_status
import com.junkfood.seal.shared.generated.resources.export_to_file
import com.junkfood.seal.shared.generated.resources.generate_new_cookies
import com.junkfood.seal.shared.generated.resources.got_it
import com.junkfood.seal.shared.generated.resources.how_does_it_work
import com.junkfood.seal.shared.generated.resources.show_more_actions
import com.junkfood.seal.shared.generated.resources.use_cookies
import com.junkfood.seal.shared.generated.resources.url_label
import com.junkfood.seal.ui.PlatformVerticalScrollbar
import com.junkfood.seal.ui.PlatformVerticalScrollbarGutter
import com.junkfood.seal.util.DownloadPreferences
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private data class CookieOperationFeedback(val message: String, val isError: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CookiesSettingsPage(
    preferences: DownloadPreferences,
    onUpdate: ((DownloadPreferences) -> DownloadPreferences) -> Unit,
    appSettings: DesktopAppSettings,
    onUpdateAppSettings: ((DesktopAppSettings) -> DesktopAppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val cookiesFilePath = DesktopYtDlpPaths.cookiesFile()
    val cache = remember { DesktopCookieCache() }
    val extractor = remember { DesktopCookieExtractor(cache = cache) }
    val scope = rememberCoroutineScope()
    val cookiesListState = rememberLazyListState()
    val exportTitle = stringResource(Res.string.desktop_cookies_export_dialog_title)
    val importTitle = stringResource(Res.string.desktop_cookies_import_dialog_title)

    var showMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBrowserExtractDialog by remember { mutableStateOf(false) }
    var extractUrl by remember(appSettings.cookieCacheMetadata.validationUrl) {
        mutableStateOf(appSettings.cookieCacheMetadata.validationUrl)
    }
    var extractBrowser by remember(appSettings.cookieCacheMetadata.browserName, preferences.cookiesBrowser) {
        mutableStateOf(
            SupportedBrowser.fromName(appSettings.cookieCacheMetadata.browserName)
                ?: SupportedBrowser.fromName(preferences.cookiesBrowser)
                ?: SupportedBrowser.entries.first(),
        )
    }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionJob by remember { mutableStateOf<Job?>(null) }
    var extractionError by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<CookieOperationFeedback?>(null) }
    var snapshot by remember {
        mutableStateOf(
            DesktopCookieCacheSnapshot(
                metadata = appSettings.cookieCacheMetadata,
                stats = DesktopCookiesStats(0, 0),
                cacheExists = false,
                cacheValid = false,
            ),
        )
    }

    LaunchedEffect(appSettings.cookieCacheMetadata) {
        snapshot = withContext(Dispatchers.IO) { cache.inspect(cookiesFilePath, appSettings.cookieCacheMetadata) }
    }

    fun persistMetadata(metadata: DesktopCookieCacheMetadata) {
        onUpdateAppSettings { it.copy(cookieCacheMetadata = metadata) }
        snapshot = snapshot.copy(metadata = metadata)
    }

    fun cancelExtraction() {
        extractor.cancel()
        extractionJob?.cancel()
        extractionJob = null
        isExtracting = false
        extractionError = null
        showBrowserExtractDialog = false
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
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                text = { Text(stringResource(Res.string.desktop_cookies_import_from_file)) },
                                onClick = {
                                    showMenu = false
                                    showImportDialog = true
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                                text = { Text(stringResource(Res.string.export_to_file)) },
                                enabled = snapshot.cacheValid,
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            val dialog = FileDialog(null as Frame?, exportTitle, FileDialog.SAVE)
                                            dialog.file = "cookies.txt"
                                            dialog.isVisible = true
                                            val file = dialog.file
                                            val directory = dialog.directory
                                            if (file != null && directory != null) {
                                                Files.copy(cookiesFilePath, java.io.File(directory, file).toPath(), REPLACE_EXISTING)
                                            }
                                        }
                                    }
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.DeleteForever, null) },
                                text = { Text(stringResource(Res.string.clear_all_cookies)) },
                                enabled = snapshot.cacheExists,
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(end = PlatformVerticalScrollbarGutter),
                state = cookiesListState,
                contentPadding = innerPadding,
            ) {
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .toggleable(
                                    value = preferences.cookies,
                                    onValueChange = { target ->
                                        if (target && !snapshot.cacheValid) {
                                            feedback = CookieOperationFeedback(AndroidStrings.get("desktop_cookies_context_missing"), true)
                                            showHelpDialog = true
                                        } else {
                                            onUpdate { it.copy(cookies = target) }
                                        }
                                    },
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                ).padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.use_cookies),
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Switch(checked = preferences.cookies, interactionSource = interactionSource, onCheckedChange = null)
                    }
                }

                item {
                    Surface(modifier = Modifier.clickable { showBrowserExtractDialog = true }) {
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
                            Text(stringResource(Res.string.generate_new_cookies), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                item {
                    val metadata = snapshot.metadata
                    val sourceLabel =
                        when (metadata.source) {
                            DesktopCookieCacheSource.Browser ->
                                SupportedBrowser.fromName(metadata.browserName)?.displayName
                                    ?: metadata.browserName.ifBlank { stringResource(Res.string.desktop_cookies_source_none) }
                            DesktopCookieCacheSource.ImportedFile -> stringResource(Res.string.desktop_cookies_source_imported)
                            DesktopCookieCacheSource.None -> stringResource(Res.string.desktop_cookies_source_none)
                        }
                    val cacheLabel =
                        stringResource(
                            if (snapshot.cacheValid) Res.string.desktop_cookies_cache_ready
                            else Res.string.desktop_cookies_cache_missing,
                        )
                    HorizontalDivider()
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Text(
                            stringResource(
                                Res.string.desktop_cookies_source_status,
                                sourceLabel,
                                snapshot.stats.cookieCount,
                                snapshot.stats.siteCount,
                                cacheLabel,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        metadata.validationHost.takeIf { it.isNotBlank() }?.let { host ->
                            Text(
                                stringResource(Res.string.desktop_cookies_last_validation, host),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        metadata.generatedAtEpochMillis.takeIf { it > 0L }?.let { generatedAt ->
                            val formatted = remember(generatedAt) {
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(generatedAt))
                            }
                            Text(
                                stringResource(Res.string.desktop_cookies_last_generated, formatted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                feedback?.let { current ->
                    item {
                        Text(
                            text = current.message,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (current.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            PlatformVerticalScrollbar(
                state = cookiesListState,
                modifier =
                    Modifier.align(Alignment.CenterEnd)
                        .padding(
                            top = innerPadding.calculateTopPadding() + 4.dp,
                            bottom = innerPadding.calculateBottomPadding() + 4.dp,
                        ),
            )
        }
    }

    AnimatedAlertDialog(
        visible = showBrowserExtractDialog,
        onDismissRequest = { if (isExtracting) cancelExtraction() else showBrowserExtractDialog = false },
        icon = { Icon(Icons.Outlined.Cookie, null) },
        title = { Text(stringResource(Res.string.generate_new_cookies)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.desktop_cookies_extract_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                OutlinedTextField(
                    value = extractUrl,
                    onValueChange = {
                        extractUrl = it
                        extractionError = null
                    },
                    label = { Text(stringResource(Res.string.url_label)) },
                    singleLine = true,
                    enabled = !isExtracting,
                    isError = extractionError != null,
                    supportingText = extractionError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isExtracting) expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = extractBrowser.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.desktop_cookies_select_browser)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isExtracting,
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SupportedBrowser.entries.forEach { browser ->
                            DropdownMenuItem(
                                text = { Text(browser.displayName) },
                                onClick = {
                                    extractBrowser = browser
                                    onUpdate { it.copy(cookiesBrowser = browser.browserName) }
                                    onUpdateAppSettings {
                                        it.copy(cookieCacheMetadata = it.cookieCacheMetadata.copy(browserName = browser.browserName))
                                    }
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                if (isExtracting) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (isExtracting) cancelExtraction() else showBrowserExtractDialog = false }) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = normalizeCookieValidationUrl(extractUrl).getOrNull()
                    if (target == null) {
                        extractionError = AndroidStrings.get("desktop_cookies_invalid_url")
                        return@TextButton
                    }
                    extractionError = null
                    isExtracting = true
                    extractionJob =
                        scope.launch {
                            val result =
                                withContext(Dispatchers.IO) {
                                    extractor.extract(
                                        DesktopCookieContext.BrowserSource(
                                            browser = extractBrowser,
                                            validationUrl = target.url,
                                            targetFile = cookiesFilePath,
                                            userAgent = preferences.userAgentString.trim().takeIf { it.isNotEmpty() },
                                        ),
                                    )
                                }
                            if (!isActive) return@launch
                            when (result) {
                                is DesktopCookieExtractionResult.Success -> {
                                    val metadata =
                                        DesktopCookieCacheMetadata(
                                            source = DesktopCookieCacheSource.Browser,
                                            browserName = extractBrowser.browserName,
                                            validationUrl = result.validationTarget.url,
                                            validationHost = result.validationTarget.host,
                                            generatedAtEpochMillis = System.currentTimeMillis(),
                                            lastStatus = DesktopCookieCacheStatus.Ready,
                                        )
                                    onUpdate { it.copy(cookies = true, cookiesBrowser = extractBrowser.browserName) }
                                    persistMetadata(metadata)
                                    snapshot =
                                        snapshot.copy(
                                            metadata = metadata,
                                            stats = result.stats,
                                            cacheExists = true,
                                            cacheValid = true,
                                        )
                                    feedback =
                                        CookieOperationFeedback(
                                            result.permissionWarning?.let {
                                                AndroidStrings.format("desktop_cookies_permission_warning", it)
                                            } ?: AndroidStrings.get("desktop_cookies_extract_success"),
                                            false,
                                        )
                                    showBrowserExtractDialog = false
                                }
                                is DesktopCookieExtractionResult.Failure -> {
                                    extractionError = result.toUserMessage()
                                    persistMetadata(
                                        appSettings.cookieCacheMetadata.copy(
                                            source = DesktopCookieCacheSource.Browser,
                                            browserName = extractBrowser.browserName,
                                            validationUrl = target.url,
                                            validationHost = target.host,
                                            lastStatus = DesktopCookieCacheStatus.Failed,
                                        ),
                                    )
                                }
                            }
                            isExtracting = false
                            extractionJob = null
                        }
                },
                enabled = !isExtracting && extractUrl.isNotBlank(),
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
        text = { Text(stringResource(Res.string.desktop_cookies_delete_file_desc)) },
        dismissButton = {
            TextButton(onClick = { showClearConfirmDialog = false }) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { cache.clear(cookiesFilePath) }.fold(
                            onSuccess = {
                                onUpdate { it.copy(cookies = false) }
                                val metadata =
                                    appSettings.cookieCacheMetadata.copy(
                                        generatedAtEpochMillis = 0L,
                                        lastStatus = DesktopCookieCacheStatus.Cleared,
                                    )
                                persistMetadata(metadata)
                                snapshot =
                                    snapshot.copy(
                                        metadata = metadata,
                                        stats = DesktopCookiesStats(0, 0),
                                        cacheExists = false,
                                        cacheValid = false,
                                    )
                                feedback = CookieOperationFeedback(AndroidStrings.get("desktop_cookies_clear_success"), false)
                            },
                            onFailure = {
                                feedback = CookieOperationFeedback(
                                    AndroidStrings.format("desktop_cookies_operation_failed", it.message ?: it.toString()),
                                    true,
                                )
                            },
                        )
                    }
                    showClearConfirmDialog = false
                },
            ) { Text(stringResource(Res.string.clear_all_cookies)) }
        },
    )

    AnimatedAlertDialog(
        visible = showImportDialog,
        onDismissRequest = { showImportDialog = false },
        icon = { Icon(Icons.Outlined.Cookie, null) },
        title = { Text(stringResource(Res.string.desktop_cookies_import_from_file)) },
        text = { Text(stringResource(Res.string.desktop_cookies_help_msg)) },
        dismissButton = {
            TextButton(onClick = { showImportDialog = false }) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showImportDialog = false
                    scope.launch {
                        val source =
                            withContext(Dispatchers.IO) {
                                val dialog = FileDialog(null as Frame?, importTitle, FileDialog.LOAD)
                                dialog.isVisible = true
                                val file = dialog.file
                                val directory = dialog.directory
                                if (file != null && directory != null) java.io.File(directory, file).toPath() else null
                            } ?: return@launch
                        withContext(Dispatchers.IO) { cache.importFrom(source, cookiesFilePath) }.fold(
                            onSuccess = { imported ->
                                val metadata =
                                    DesktopCookieCacheMetadata(
                                        source = DesktopCookieCacheSource.ImportedFile,
                                        generatedAtEpochMillis = System.currentTimeMillis(),
                                        lastStatus = DesktopCookieCacheStatus.Ready,
                                    )
                                onUpdate { it.copy(cookies = true, cookiesBrowser = "") }
                                persistMetadata(metadata)
                                snapshot =
                                    snapshot.copy(
                                        metadata = metadata,
                                        stats = imported.stats,
                                        cacheExists = true,
                                        cacheValid = true,
                                    )
                                feedback =
                                    CookieOperationFeedback(
                                        imported.permissionWarning?.let {
                                            AndroidStrings.format("desktop_cookies_permission_warning", it)
                                        } ?: AndroidStrings.get("desktop_cookies_import_success"),
                                        false,
                                    )
                            },
                            onFailure = {
                                feedback = CookieOperationFeedback(
                                    AndroidStrings.format("desktop_cookies_operation_failed", it.message ?: it.toString()),
                                    true,
                                )
                            },
                        )
                    }
                },
            ) { Text(stringResource(Res.string.confirm)) }
        },
    )

    AnimatedAlertDialog(
        visible = showHelpDialog,
        onDismissRequest = { showHelpDialog = false },
        icon = { Icon(Icons.Outlined.HelpOutline, null) },
        title = { Text(stringResource(Res.string.how_does_it_work)) },
        text = { Text(stringResource(Res.string.desktop_cookies_help_msg)) },
        confirmButton = {
            TextButton(onClick = { showHelpDialog = false }) { Text(stringResource(Res.string.got_it)) }
        },
    )
}

private fun DesktopCookieExtractionResult.Failure.toUserMessage(): String =
    when (reason) {
        DesktopCookieExtractionResult.Reason.InvalidUrl -> AndroidStrings.get("desktop_cookies_invalid_url")
        DesktopCookieExtractionResult.Reason.TimedOut -> AndroidStrings.get("desktop_cookies_extract_timeout")
        DesktopCookieExtractionResult.Reason.Canceled -> AndroidStrings.get("desktop_cookies_extract_canceled")
        else -> AndroidStrings.format("desktop_cookies_operation_failed", diagnostic)
    }
