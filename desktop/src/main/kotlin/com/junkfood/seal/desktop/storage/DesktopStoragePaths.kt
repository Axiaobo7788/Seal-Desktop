package com.junkfood.seal.desktop.storage

import com.junkfood.seal.desktop.paths.DesktopAppPaths
import java.nio.file.Path

internal fun desktopAppStateDir(): Path = DesktopAppPaths.stateDirectory()

internal fun queueJsonPath(): Path = DesktopAppPaths.queueFile()

internal fun historyJsonPath(): Path = DesktopAppPaths.historyFile()

internal fun appSettingsJsonPath(): Path = DesktopAppPaths.appSettingsFile()

internal fun preferencesJsonPath(): Path = DesktopAppPaths.preferencesFile()

internal fun customCommandTasksJsonPath(): Path = DesktopAppPaths.customCommandTasksFile()

internal fun sqliteDbPath(): Path = DesktopAppPaths.databaseFile()
