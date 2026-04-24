package com.junkfood.seal.desktop.storage

import java.nio.file.Path

private fun desktopStateBaseDir(): Path {
    val customStateDir = System.getProperty("seal.desktop.storage.stateDir")?.trim()?.takeIf { it.isNotBlank() }
    if (customStateDir != null) {
        return Path.of(customStateDir)
    }

    val xdg = System.getenv("XDG_STATE_HOME")?.takeIf { it.isNotBlank() }
    return if (xdg != null) {
        Path.of(xdg)
    } else {
        Path.of(System.getProperty("user.home"), ".local", "state")
    }
}

internal fun desktopAppStateDir(): Path = desktopStateBaseDir().resolve("seal")

internal fun queueJsonPath(): Path = desktopAppStateDir().resolve("queue.json")

internal fun historyJsonPath(): Path = desktopAppStateDir().resolve("history.json")

internal fun appSettingsJsonPath(): Path = desktopAppStateDir().resolve("app-settings.json")

internal fun customCommandTasksJsonPath(): Path = desktopAppStateDir().resolve("custom-command-tasks.json")

internal fun sqliteDbPath(): Path = desktopAppStateDir().resolve("seal.db")
