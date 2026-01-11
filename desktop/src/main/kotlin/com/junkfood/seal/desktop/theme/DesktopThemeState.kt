package com.junkfood.seal.desktop.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val themeJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

@Serializable
data class DesktopThemePreferences(
    val dynamicColorEnabled: Boolean = true,
    /** Index into the built-in palette list shown in the appearance page. */
    val seedColorIndex: Int = 0,
    val darkThemeValue: Int = DarkThemePreference.FOLLOW_SYSTEM,
    val highContrastEnabled: Boolean = false,
)

object DarkThemePreference {
    const val FOLLOW_SYSTEM = 1
    const val ON = 2
    const val OFF = 3
}

private fun defaultThemePath(): Path {
    val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
    val base =
        if (xdg != null) Path.of(xdg) else Path.of(System.getProperty("user.home"), ".local", "state")
    return base.resolve("seal").resolve("theme.json")
}

class DesktopThemeStorage(private val path: Path = defaultThemePath()) {
    suspend fun load(): DesktopThemePreferences? =
        withContext(Dispatchers.IO) {
            if (!path.exists()) return@withContext null
            runCatching { themeJson.decodeFromString<DesktopThemePreferences>(path.readText()) }
                .getOrNull()
        }

    suspend fun save(preferences: DesktopThemePreferences) {
        withContext(Dispatchers.IO) {
            path.parent?.createDirectories()
            Files.writeString(path, themeJson.encodeToString(preferences))
        }
    }
}

class DesktopThemeState(
    private val storage: DesktopThemeStorage,
    private val scope: CoroutineScope,
) {
    var preferences by mutableStateOf(DesktopThemePreferences())
        private set

    init {
        scope.launch {
            storage.load()?.let { preferences = it }
        }
    }

    fun update(transform: (DesktopThemePreferences) -> DesktopThemePreferences) {
        val updated = transform(preferences)
        preferences = updated
        scope.launch { storage.save(updated) }
    }

    fun set(newPreferences: DesktopThemePreferences) {
        preferences = newPreferences
        scope.launch { storage.save(newPreferences) }
    }
}

@Composable
fun rememberDesktopThemeState(
    storage: DesktopThemeStorage = remember { DesktopThemeStorage() },
): DesktopThemeState {
    val scope = rememberCoroutineScope()
    return remember(storage) { DesktopThemeState(storage, scope) }
}
