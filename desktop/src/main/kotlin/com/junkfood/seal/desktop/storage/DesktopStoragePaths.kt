package com.junkfood.seal.desktop.storage

import java.nio.file.Path

private const val STORAGE_STATE_DIR_PROPERTY = "seal.desktop.storage.stateDir"
private const val STORAGE_STATE_DIR_ENV = "SEAL_DESKTOP_STORAGE_STATE_DIR"

internal fun resolveDesktopStateBaseDir(
    propertyStateDir: String?,
    environmentStateDir: String?,
    xdgStateHome: String?,
    userHome: String,
): Path {
    val customStateDir = propertyStateDir?.trim()?.takeIf { it.isNotBlank() }
        ?: environmentStateDir?.trim()?.takeIf { it.isNotBlank() }
    if (customStateDir != null) {
        return Path.of(customStateDir)
    }

    val xdg = xdgStateHome?.trim()?.takeIf { it.isNotBlank() }
    return if (xdg != null) {
        Path.of(xdg)
    } else {
        Path.of(userHome, ".local", "state")
    }
}

private fun desktopStateBaseDir(): Path {
    return resolveDesktopStateBaseDir(
        propertyStateDir = System.getProperty(STORAGE_STATE_DIR_PROPERTY),
        environmentStateDir = System.getenv(STORAGE_STATE_DIR_ENV),
        xdgStateHome = System.getenv("XDG_STATE_HOME"),
        userHome = System.getProperty("user.home"),
    )
}

internal fun desktopAppStateDir(): Path = desktopStateBaseDir().resolve("seal")

internal fun queueJsonPath(): Path = desktopAppStateDir().resolve("queue.json")

internal fun historyJsonPath(): Path = desktopAppStateDir().resolve("history.json")

internal fun appSettingsJsonPath(): Path = desktopAppStateDir().resolve("app-settings.json")

internal fun customCommandTasksJsonPath(): Path = desktopAppStateDir().resolve("custom-command-tasks.json")

internal fun sqliteDbPath(): Path = desktopAppStateDir().resolve("seal.db")
