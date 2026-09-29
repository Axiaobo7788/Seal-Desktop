package com.junkfood.seal.desktop.settings.network

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.desktop.cookies.BrowserInstallation
import com.junkfood.seal.desktop.cookies.BrowserProfile
import com.junkfood.seal.desktop.cookies.CookieDomainMatcher
import com.junkfood.seal.desktop.cookies.CookieValidationPreset
import com.junkfood.seal.desktop.cookies.DesktopBrowserDetectionResult
import com.junkfood.seal.desktop.cookies.DesktopBrowserDetector
import com.junkfood.seal.desktop.cookies.DesktopBrowserSelection
import com.junkfood.seal.desktop.cookies.DesktopCookieBrowserPreference
import com.junkfood.seal.desktop.cookies.DesktopCookieCache
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheMetadata
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheSnapshot
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheSource
import com.junkfood.seal.desktop.cookies.DesktopCookieCacheStatus
import com.junkfood.seal.desktop.cookies.DesktopCookieContext
import com.junkfood.seal.desktop.cookies.DesktopCookieExtractionResult
import com.junkfood.seal.desktop.cookies.DesktopCookieExtractor
import com.junkfood.seal.desktop.cookies.DesktopCookieMediaValidationResult
import com.junkfood.seal.desktop.cookies.DesktopCookieMediaValidator
import com.junkfood.seal.desktop.cookies.DesktopCookiesParser
import com.junkfood.seal.desktop.cookies.DesktopCookiesStats
import com.junkfood.seal.desktop.cookies.SupportedBrowser
import com.junkfood.seal.desktop.cookies.COOKIE_EXTRACTION_TRIGGER_URL
import com.junkfood.seal.desktop.cookies.matchDomains
import com.junkfood.seal.desktop.i18n.AndroidStrings
import com.junkfood.seal.desktop.network.DesktopProxyResolver
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.ui.AnimatedAlertDialog
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.shared.generated.resources.*
import com.junkfood.seal.ui.PlatformVerticalScrollbar
import com.junkfood.seal.ui.PlatformVerticalScrollbarGutter
import com.junkfood.seal.util.DownloadPreferences
import java.awt.Desktop
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

private data class BrowserProfileChoice(
    val browser: SupportedBrowser,
    val profile: BrowserProfile,
)

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
    val mediaValidator = remember { DesktopCookieMediaValidator() }
    val browserDetector = remember { DesktopBrowserDetector() }
    val scope = rememberCoroutineScope()
    val cookiesListState = rememberLazyListState()
    val exportTitle = stringResource(Res.string.desktop_cookies_export_dialog_title)
    val importTitle = stringResource(Res.string.desktop_cookies_import_dialog_title)

    var showHelpDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showMediaValidationDialog by remember { mutableStateOf(false) }
    var showCacheMenu by remember { mutableStateOf(false) }
    var detectionRevision by remember { mutableIntStateOf(0) }
    var isDetecting by remember { mutableStateOf(true) }
    var browserDetection by remember { mutableStateOf<DesktopBrowserDetectionResult?>(null) }
    var selectedSource by remember { mutableStateOf<BrowserProfileChoice?>(null) }
    var pendingSource by remember { mutableStateOf<BrowserProfileChoice?>(null) }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionJob by remember { mutableStateOf<Job?>(null) }
    var feedback by remember { mutableStateOf<CookieOperationFeedback?>(null) }
    var selectedPreset by remember { mutableStateOf(CookieValidationPreset.Bilibili) }
    var customDomain by remember { mutableStateOf("") }
    var domainFeedback by remember { mutableStateOf<CookieOperationFeedback?>(null) }
    var mediaUrl by remember { mutableStateOf(appSettings.cookieCacheMetadata.validationUrl) }
    var mediaFeedback by remember { mutableStateOf<CookieOperationFeedback?>(null) }
    var isMediaValidating by remember { mutableStateOf(false) }
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

    fun storedSelection(): DesktopBrowserSelection? {
        val preference = appSettings.cookieBrowserPreference
        val browser =
            SupportedBrowser.fromName(preference.browserName)
                ?: SupportedBrowser.fromName(preferences.cookiesBrowser)
                ?: return null
        return DesktopBrowserSelection(browser, preference.profileId)
    }

    fun persistMetadata(metadata: DesktopCookieCacheMetadata) {
        onUpdateAppSettings { it.copy(cookieCacheMetadata = metadata) }
        snapshot = snapshot.copy(metadata = metadata)
    }

    fun persistSource(choice: BrowserProfileChoice) {
        selectedSource = choice
        onUpdate { it.copy(cookiesBrowser = choice.browser.browserName) }
        onUpdateAppSettings {
            it.copy(
                cookieBrowserPreference =
                    DesktopCookieBrowserPreference(
                        browserName = choice.browser.browserName,
                        profileId = choice.profile.id,
                        profileName = choice.profile.displayName,
                    ),
            )
        }
    }

    fun refreshCookies() {
        val source = selectedSource
        if (source == null) {
            feedback = CookieOperationFeedback(AndroidStrings.get("desktop_cookies_select_source_first"), true)
            return
        }
        if (isExtracting) return
        isExtracting = true
        feedback = null
        extractionJob =
            scope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        extractor.extract(
                            DesktopCookieContext.BrowserSource(
                                browser = source.browser,
                                profile = source.profile.extractionValue,
                                validationUrl = COOKIE_EXTRACTION_TRIGGER_URL,
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
                                browserName = source.browser.browserName,
                                browserProfileId = source.profile.id,
                                browserProfileName = source.profile.displayName,
                                generatedAtEpochMillis = System.currentTimeMillis(),
                                lastStatus = DesktopCookieCacheStatus.Ready,
                            )
                        onUpdate { it.copy(cookies = true, cookiesBrowser = source.browser.browserName) }
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
                    }
                    is DesktopCookieExtractionResult.Failure -> {
                        persistMetadata(snapshot.metadata.copy(lastStatus = DesktopCookieCacheStatus.Failed))
                        feedback = CookieOperationFeedback(result.toUserMessage(), true)
                    }
                }
                isExtracting = false
                extractionJob = null
            }
    }

    fun cancelExtraction() {
        extractor.cancel()
        extractionJob?.cancel()
        extractionJob = null
        isExtracting = false
        feedback = CookieOperationFeedback(AndroidStrings.get("desktop_cookies_extract_canceled"), true)
    }

    LaunchedEffect(appSettings.cookieCacheMetadata) {
        snapshot = withContext(Dispatchers.IO) { cache.inspect(cookiesFilePath, appSettings.cookieCacheMetadata) }
    }

    LaunchedEffect(detectionRevision) {
        isDetecting = true
        val previous = selectedSource?.let { DesktopBrowserSelection(it.browser, it.profile.id) } ?: storedSelection()
        val detected = withContext(Dispatchers.IO) { browserDetector.detect(previous) }
        browserDetection = detected
        selectedSource =
            detected.resolve(previous)?.let { (installation, profile) ->
                BrowserProfileChoice(installation.browser, profile)
            }
        isDetecting = false
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .toggleable(
                                    value = preferences.cookies,
                                    onValueChange = { target ->
                                        if (target && !snapshot.cacheValid) {
                                            feedback = CookieOperationFeedback(AndroidStrings.get("desktop_cookies_context_missing"), true)
                                        } else {
                                            onUpdate { it.copy(cookies = target) }
                                        }
                                    },
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                ).padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(Res.string.use_cookies),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = stringResource(Res.string.cookies_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Switch(checked = preferences.cookies, interactionSource = interactionSource, onCheckedChange = null)
                    }
                }

                item {
                    SettingsSectionTitle(
                        icon = { Icon(Icons.Outlined.Cookie, null) },
                        title = stringResource(Res.string.desktop_cookies_source_title),
                    )
                    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                stringResource(Res.string.desktop_cookies_source_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            when {
                                isDetecting -> {
                                    Text(stringResource(Res.string.desktop_cookies_detecting))
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                }
                                selectedSource != null -> {
                                    val source = selectedSource!!
                                    Text(source.browser.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(source.profile.displayName, style = MaterialTheme.typography.bodyMedium)
                                }
                                storedSelection() != null -> {
                                    val preference = appSettings.cookieBrowserPreference
                                    val browser = storedSelection()!!.browser
                                    Text(
                                        stringResource(
                                            Res.string.desktop_cookies_source_unavailable,
                                            browser.displayName,
                                            preference.profileName.ifBlank { "Default" },
                                        ),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    if (snapshot.cacheValid) {
                                        Text(
                                            stringResource(Res.string.desktop_cookies_cache_still_available),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                browserDetection?.available?.isNotEmpty() == true -> {
                                    Text(
                                        stringResource(Res.string.desktop_cookies_choose_profile),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                else -> {
                                    Text(
                                        stringResource(Res.string.desktop_cookies_none_detected),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        stringResource(Res.string.desktop_cookies_none_detected_desc),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            browserDetection?.installedWithoutProfiles?.forEach { installation ->
                                Text(
                                    stringResource(
                                        Res.string.desktop_cookies_installed_no_profile,
                                        installation.browser.displayName,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            if (browserDetection?.installedWithoutProfiles?.isNotEmpty() == true) {
                                Text(
                                    stringResource(Res.string.desktop_cookies_browser_initialized_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        pendingSource = selectedSource
                                        showSourceDialog = true
                                    },
                                    enabled = !isDetecting && browserDetection?.available?.isNotEmpty() == true,
                                ) {
                                    Text(stringResource(Res.string.desktop_cookies_change))
                                }
                                TextButton(onClick = { detectionRevision += 1 }, enabled = !isDetecting) {
                                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(Res.string.desktop_cookies_redetect))
                                }
                                if (browserDetection?.available?.isEmpty() == true) {
                                    TextButton(onClick = { showImportDialog = true }) {
                                        Text(stringResource(Res.string.desktop_cookies_import_from_file))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSectionTitle(
                        icon = { Icon(Icons.Outlined.Storage, null) },
                        title = stringResource(Res.string.desktop_cookies_local_cache_title),
                    )
                    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val metadata = snapshot.metadata
                            Text(cacheSourceLabel(metadata), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (snapshot.cacheValid) {
                                    stringResource(
                                        Res.string.desktop_cookies_cache_summary,
                                        snapshot.stats.cookieCount,
                                        snapshot.stats.siteCount,
                                    )
                                } else {
                                    stringResource(Res.string.desktop_cookies_cache_missing)
                                },
                                color =
                                    if (snapshot.cacheValid) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.error,
                            )
                            if (metadata.generatedAtEpochMillis > 0L) {
                                Text(
                                    stringResource(
                                        Res.string.desktop_cookies_cache_updated,
                                        formatTimestamp(metadata.generatedAtEpochMillis),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isExtracting) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = ::refreshCookies, enabled = selectedSource != null && !isExtracting) {
                                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(stringResource(Res.string.desktop_cookies_refresh))
                                }
                                Box {
                                    OutlinedButton(onClick = { showCacheMenu = true }) {
                                        Icon(Icons.Outlined.MoreVert, null, Modifier.size(18.dp))
                                        Spacer(Modifier.size(6.dp))
                                        Text(stringResource(Res.string.show_more_actions))
                                    }
                                    DropdownMenu(
                                        expanded = showCacheMenu,
                                        onDismissRequest = { showCacheMenu = false },
                                    ) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.Add, null) },
                                            text = { Text(stringResource(Res.string.desktop_cookies_import_from_file)) },
                                            onClick = {
                                                showCacheMenu = false
                                                showImportDialog = true
                                            },
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                                            text = { Text(stringResource(Res.string.export_to_file)) },
                                            enabled = snapshot.cacheValid,
                                            onClick = {
                                                showCacheMenu = false
                                                scope.launch {
                                                    runCatching {
                                                        withContext(Dispatchers.IO) {
                                                            val dialog = FileDialog(null as Frame?, exportTitle, FileDialog.SAVE)
                                                            dialog.file = "cookies.txt"
                                                            dialog.isVisible = true
                                                            val file = dialog.file
                                                            val directory = dialog.directory
                                                            if (file != null && directory != null) {
                                                                Files.copy(
                                                                    cookiesFilePath,
                                                                    java.io.File(directory, file).toPath(),
                                                                    REPLACE_EXISTING,
                                                                )
                                                            }
                                                        }
                                                    }.onFailure {
                                                        feedback =
                                                            CookieOperationFeedback(
                                                                AndroidStrings.format(
                                                                    "desktop_cookies_operation_failed",
                                                                    it.message ?: it.toString(),
                                                                ),
                                                                true,
                                                            )
                                                    }
                                                }
                                            },
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.FolderOpen, null) },
                                            text = { Text(stringResource(Res.string.desktop_cookies_show_location)) },
                                            enabled = snapshot.cacheExists,
                                            onClick = {
                                                showCacheMenu = false
                                                scope.launch {
                                                    val opened =
                                                        withContext(Dispatchers.IO) {
                                                            runCatching {
                                                                val parent = cookiesFilePath.toAbsolutePath().normalize().parent.toFile()
                                                                check(
                                                                    Desktop.isDesktopSupported() &&
                                                                        Desktop.getDesktop().isSupported(Desktop.Action.OPEN),
                                                                )
                                                                Desktop.getDesktop().open(parent)
                                                            }.isSuccess
                                                        }
                                                    if (!opened) {
                                                        feedback =
                                                            CookieOperationFeedback(
                                                                AndroidStrings.get("desktop_cookies_open_location_failed"),
                                                                true,
                                                            )
                                                    }
                                                }
                                            },
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Outlined.DeleteForever, null) },
                                            text = { Text(stringResource(Res.string.clear_all_cookies)) },
                                            enabled = snapshot.cacheExists,
                                            onClick = {
                                                showCacheMenu = false
                                                showClearConfirmDialog = true
                                            },
                                        )
                                    }
                                }
                            }
                            if (isExtracting) {
                                TextButton(onClick = ::cancelExtraction) {
                                    Text(stringResource(Res.string.cancel))
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSectionTitle(
                        icon = { Icon(Icons.Outlined.VerifiedUser, null) },
                        title = stringResource(Res.string.desktop_cookies_validation_title),
                    )
                    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                stringResource(Res.string.desktop_cookies_domain_validation_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CookieValidationPreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = selectedPreset == preset,
                                        onClick = {
                                            selectedPreset = preset
                                            domainFeedback = null
                                        },
                                        label = {
                                            Text(
                                                if (preset == CookieValidationPreset.Custom) {
                                                    stringResource(Res.string.desktop_cookies_custom_domain)
                                                } else {
                                                    preset.displayName
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            if (selectedPreset == CookieValidationPreset.Custom) {
                                OutlinedTextField(
                                    value = customDomain,
                                    onValueChange = {
                                        customDomain = it
                                        domainFeedback = null
                                    },
                                    label = { Text(stringResource(Res.string.desktop_cookies_domain_label)) },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (customDomain.isNotEmpty()) {
                                            IconButton(onClick = { customDomain = "" }) {
                                                Icon(Icons.Outlined.Clear, contentDescription = null)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    enabled = snapshot.cacheValid,
                                    onClick = {
                                        val patterns =
                                            if (selectedPreset == CookieValidationPreset.Custom) {
                                                val normalized = CookieDomainMatcher.normalizePattern(customDomain)
                                                if (normalized == null) {
                                                    domainFeedback =
                                                        CookieOperationFeedback(
                                                            AndroidStrings.get("desktop_cookies_domain_invalid"),
                                                            true,
                                                        )
                                                    return@Button
                                                }
                                                listOf(normalized)
                                            } else {
                                                selectedPreset.domainPatterns
                                            }
                                        val label =
                                            if (selectedPreset == CookieValidationPreset.Custom) patterns.first()
                                            else selectedPreset.displayName
                                        scope.launch {
                                            val result =
                                                withContext(Dispatchers.IO) {
                                                    DesktopCookiesParser.matchDomains(cookiesFilePath, patterns)
                                                }
                                            domainFeedback =
                                                if (result.cookieCount > 0) {
                                                    CookieOperationFeedback(
                                                        AndroidStrings.format(
                                                            "desktop_cookies_domain_match_found",
                                                            result.cookieCount,
                                                            result.matchedDomains.size,
                                                        ),
                                                        false,
                                                    )
                                                } else {
                                                    CookieOperationFeedback(
                                                        AndroidStrings.format("desktop_cookies_domain_match_missing", label),
                                                        true,
                                                    )
                                                }
                                        }
                                    },
                                ) {
                                    Text(stringResource(Res.string.desktop_cookies_validate_domain))
                                }
                                TextButton(
                                    onClick = {
                                        mediaFeedback = null
                                        showMediaValidationDialog = true
                                    },
                                    enabled = snapshot.cacheValid,
                                ) {
                                    Text(stringResource(Res.string.desktop_cookies_actual_url_action))
                                }
                            }
                            domainFeedback?.let { current -> FeedbackText(current) }
                        }
                    }
                }

                feedback?.let { current ->
                    item {
                        FeedbackText(current, Modifier.padding(horizontal = 24.dp))
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
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

    val availableChoices =
        browserDetection?.available.orEmpty().flatMap { installation ->
            installation.profiles.map { BrowserProfileChoice(installation.browser, it) }
        }
    AnimatedAlertDialog(
        visible = showSourceDialog,
        onDismissRequest = { showSourceDialog = false },
        icon = { Icon(Icons.Outlined.Cookie, null) },
        title = { Text(stringResource(Res.string.desktop_cookies_choose_profile)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(availableChoices, key = { "${it.browser.browserName}:${it.profile.id}" }) { choice ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { pendingSource = choice }
                                .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = pendingSource == choice,
                            onClick = { pendingSource = choice },
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(choice.browser.displayName, fontWeight = FontWeight.Medium)
                            Text(
                                choice.profile.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                browserDetection?.installedWithoutProfiles?.let { installations ->
                    items(installations, key = BrowserInstallation::browser) { installation ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Text(installation.browser.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                stringResource(Res.string.desktop_cookies_browser_initialized_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { showSourceDialog = false }) { Text(stringResource(Res.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                enabled = pendingSource != null,
                onClick = {
                    pendingSource?.let(::persistSource)
                    showSourceDialog = false
                },
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
    )

    AnimatedAlertDialog(
        visible = showMediaValidationDialog,
        onDismissRequest = { if (!isMediaValidating) showMediaValidationDialog = false },
        icon = { Icon(Icons.Outlined.VerifiedUser, null) },
        title = { Text(stringResource(Res.string.desktop_cookies_actual_url_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.desktop_cookies_actual_url_desc))
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = {
                        mediaUrl = it
                        mediaFeedback = null
                    },
                    label = { Text(stringResource(Res.string.url_label)) },
                    singleLine = true,
                    enabled = !isMediaValidating,
                    trailingIcon = {
                        if (mediaUrl.isNotEmpty() && !isMediaValidating) {
                            IconButton(onClick = { mediaUrl = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isMediaValidating) {
                    Text(stringResource(Res.string.desktop_cookies_media_validating))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                mediaFeedback?.let { FeedbackText(it) }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showMediaValidationDialog = false },
                enabled = !isMediaValidating,
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = mediaUrl.isNotBlank() && !isMediaValidating,
                onClick = {
                    isMediaValidating = true
                    mediaFeedback = null
                    scope.launch {
                        val proxyUrl = DesktopProxyResolver.resolveProxyUrl(preferences, appSettings)
                        val result =
                            withContext(Dispatchers.IO) {
                                mediaValidator.validate(
                                    url = mediaUrl,
                                    cookiesFile = cookiesFilePath,
                                    userAgent = preferences.userAgentString.trim().takeIf { it.isNotEmpty() },
                                    proxyUrl = proxyUrl,
                                    environment = DesktopProxyResolver.buildProxyEnvironment(proxyUrl),
                                )
                            }
                        when (result) {
                            is DesktopCookieMediaValidationResult.Success -> {
                                val metadata =
                                    snapshot.metadata.copy(
                                        validationUrl = result.target.url,
                                        validationHost = result.target.host,
                                    )
                                persistMetadata(metadata)
                                mediaFeedback =
                                    CookieOperationFeedback(
                                        AndroidStrings.get("desktop_cookies_media_success"),
                                        false,
                                    )
                            }
                            is DesktopCookieMediaValidationResult.Failure -> {
                                mediaFeedback = CookieOperationFeedback(result.toUserMessage(), true)
                            }
                        }
                        isMediaValidating = false
                    }
                },
            ) {
                Text(stringResource(Res.string.desktop_cookies_actual_url_action))
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
                                val metadata = DesktopCookieCacheMetadata(lastStatus = DesktopCookieCacheStatus.Cleared)
                                persistMetadata(metadata)
                                snapshot =
                                    snapshot.copy(
                                        metadata = metadata,
                                        stats = DesktopCookiesStats(0, 0),
                                        cacheExists = false,
                                        cacheValid = false,
                                    )
                                domainFeedback = null
                                mediaFeedback = null
                                feedback = CookieOperationFeedback(AndroidStrings.get("desktop_cookies_clear_success"), false)
                            },
                            onFailure = {
                                feedback =
                                    CookieOperationFeedback(
                                        AndroidStrings.format("desktop_cookies_operation_failed", it.message ?: it.toString()),
                                        true,
                                    )
                            },
                        )
                    }
                    showClearConfirmDialog = false
                },
            ) {
                Text(stringResource(Res.string.clear_all_cookies))
            }
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
                                onUpdate { it.copy(cookies = true) }
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
                                feedback =
                                    CookieOperationFeedback(
                                        AndroidStrings.format("desktop_cookies_operation_failed", it.message ?: it.toString()),
                                        true,
                                    )
                            },
                        )
                    }
                },
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
        text = { Text(stringResource(Res.string.desktop_cookies_help_msg)) },
        confirmButton = {
            TextButton(onClick = { showHelpDialog = false }) { Text(stringResource(Res.string.got_it)) }
        },
    )
}

@Composable
private fun SettingsSectionTitle(
    icon: @Composable () -> Unit,
    title: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun FeedbackText(
    feedback: CookieOperationFeedback,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (feedback.isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (feedback.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
            text = feedback.message,
            style = MaterialTheme.typography.bodySmall,
            color = if (feedback.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun cacheSourceLabel(metadata: DesktopCookieCacheMetadata): String =
    when (metadata.source) {
        DesktopCookieCacheSource.Browser -> {
            val browser =
                SupportedBrowser.fromName(metadata.browserName)?.displayName
                    ?: metadata.browserName.ifBlank { stringResource(Res.string.desktop_cookies_source_none) }
            metadata.browserProfileName.takeIf { it.isNotBlank() }?.let { "$browser · $it" } ?: browser
        }
        DesktopCookieCacheSource.ImportedFile -> stringResource(Res.string.desktop_cookies_source_imported)
        DesktopCookieCacheSource.None -> stringResource(Res.string.desktop_cookies_source_none)
    }

private fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

private fun DesktopCookieExtractionResult.Failure.toUserMessage(): String =
    when (reason) {
        DesktopCookieExtractionResult.Reason.InvalidUrl -> AndroidStrings.get("desktop_cookies_invalid_url")
        DesktopCookieExtractionResult.Reason.TimedOut -> AndroidStrings.get("desktop_cookies_extract_timeout")
        DesktopCookieExtractionResult.Reason.Canceled -> AndroidStrings.get("desktop_cookies_extract_canceled")
        else -> AndroidStrings.format("desktop_cookies_operation_failed", diagnostic)
    }

private fun DesktopCookieMediaValidationResult.Failure.toUserMessage(): String =
    when (reason) {
        DesktopCookieMediaValidationResult.Reason.InvalidUrl -> AndroidStrings.get("desktop_cookies_invalid_url")
        DesktopCookieMediaValidationResult.Reason.InvalidCache -> AndroidStrings.get("desktop_cookies_media_invalid_cache")
        DesktopCookieMediaValidationResult.Reason.NoMatchingCookies -> AndroidStrings.get("desktop_cookies_media_no_match")
        DesktopCookieMediaValidationResult.Reason.AuthenticationRequired ->
            AndroidStrings.get("desktop_cookies_media_auth_required")
        DesktopCookieMediaValidationResult.Reason.NetworkError -> AndroidStrings.get("desktop_cookies_media_network_error")
        DesktopCookieMediaValidationResult.Reason.MediaUnavailable -> AndroidStrings.get("desktop_cookies_media_unavailable")
        DesktopCookieMediaValidationResult.Reason.ExtractorFailed ->
            AndroidStrings.format("desktop_cookies_media_extractor_error", diagnostic)
        DesktopCookieMediaValidationResult.Reason.UnexpectedFailure ->
            AndroidStrings.format("desktop_cookies_media_unexpected_error", diagnostic)
    }
