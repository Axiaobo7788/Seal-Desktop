package com.junkfood.seal.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val appSettingsJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

@Serializable
data class DesktopAppSettings(
    val customCommandEnabled: Boolean = false,
    val customCommandLabel: String = "",
    val customCommandTemplate: String = "",
    val downloadTypeInitialization: Int = DownloadTypeNone,
    val autoUpdateEnabled: Boolean = false,
    val updateChannel: Int = UpdateChannelStable,
)

private fun appSettingsPath(): Path {
    val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
    val base =
        if (xdg != null) Path.of(xdg) else Path.of(System.getProperty("user.home"), ".local", "state")
    return base.resolve("seal").resolve("app-settings.json")
}

class DesktopAppSettingsStorage(private val path: Path = appSettingsPath()) {
    suspend fun load(): DesktopAppSettings? =
        withContext(Dispatchers.IO) {
            if (!path.exists()) return@withContext null
            runCatching { appSettingsJson.decodeFromString<DesktopAppSettings>(path.readText()) }
                .getOrNull()
        }

    suspend fun save(settings: DesktopAppSettings) {
        withContext(Dispatchers.IO) {
            path.parent?.createDirectories()
            path.writeText(appSettingsJson.encodeToString(settings))
        }
    }
}

class DesktopAppSettingsState(
    private val storage: DesktopAppSettingsStorage,
    private val scope: CoroutineScope,
) {
    var settings by mutableStateOf(DesktopAppSettings())
        private set

    init {
        scope.launch {
            storage.load()?.let { settings = it }
        }
    }

    fun update(transform: (DesktopAppSettings) -> DesktopAppSettings) {
        val updated = transform(settings)
        settings = updated
        scope.launch { storage.save(updated) }
    }

    fun set(newSettings: DesktopAppSettings) {
        settings = newSettings
        scope.launch { storage.save(newSettings) }
    }
}

@Composable
fun rememberDesktopAppSettingsState(
    storage: DesktopAppSettingsStorage = remember { DesktopAppSettingsStorage() },
): DesktopAppSettingsState {
    val scope = rememberCoroutineScope()
    return remember(storage) { DesktopAppSettingsState(storage, scope) }
}

internal const val DownloadTypeNone = 0
internal const val DownloadTypePrevious = 1

internal const val UpdateChannelStable = 0
internal const val UpdateChannelPreview = 1