package com.junkfood.seal.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.junkfood.seal.util.DownloadPreferences
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val settingsJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

/** Default preference set for desktop builds. */
fun desktopDefaultPreferences(): DownloadPreferences =
    DownloadPreferences.EMPTY.copy(
        formatSorting = true,
        videoFormat = 2, // QUALITY
        videoResolution = 3, // 1080p
        embedMetadata = true,
    )

private fun defaultSettingsPath(): Path {
    val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
    val base =
        if (xdg != null) Path.of(xdg) else Path.of(System.getProperty("user.home"), ".local", "state")
    return base.resolve("seal").resolve("settings.json")
}

class DesktopPreferencesStorage(private val path: Path = defaultSettingsPath()) {
    suspend fun load(): DownloadPreferences? =
        withContext(Dispatchers.IO) {
            if (!path.exists()) return@withContext null
            runCatching { settingsJson.decodeFromString<DownloadPreferences>(path.readText()) }.getOrNull()
        }

    suspend fun save(preferences: DownloadPreferences) {
        withContext(Dispatchers.IO) {
            path.parent?.createDirectories()
            Files.writeString(path, settingsJson.encodeToString(preferences))
        }
    }
}

class DesktopSettingsState(
    private val storage: DesktopPreferencesStorage,
    private val scope: CoroutineScope,
) {
    var preferences by mutableStateOf(desktopDefaultPreferences())
        private set

    init {
        scope.launch {
            storage.load()?.let { preferences = it }
        }
    }

    fun update(transform: (DownloadPreferences) -> DownloadPreferences) {
        val updated = transform(preferences)
        preferences = updated
        scope.launch { storage.save(updated) }
    }

    fun set(newPreferences: DownloadPreferences) {
        preferences = newPreferences
        scope.launch { storage.save(newPreferences) }
    }

    fun resetToDefaults() = set(desktopDefaultPreferences())
}

@Composable
fun rememberDesktopSettingsState(
    storage: DesktopPreferencesStorage = remember { DesktopPreferencesStorage() },
): DesktopSettingsState {
    val scope = rememberCoroutineScope()
    return remember(storage) { DesktopSettingsState(storage, scope) }
}
