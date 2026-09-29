package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.paths.DesktopAppPaths
import com.junkfood.seal.desktop.paths.DesktopPathEnvironment
import com.junkfood.seal.desktop.paths.resolveDesktopAppPathLayout
import java.nio.file.Path

internal object DesktopDependencyPaths {
    fun appPrivateDirectory(): Path = DesktopAppPaths.auxiliaryBinariesDirectory()

    internal fun defaultAppPrivateDirectory(
        isWindows: Boolean,
        isMac: Boolean,
        userHome: String,
        xdgDataHome: String?,
        localAppData: String?,
    ): Path =
        resolveDesktopAppPathLayout(
            DesktopPathEnvironment(
                osName = if (isWindows) "Windows" else if (isMac) "macOS" else "Linux",
                userHome = userHome,
                xdgDataHome = xdgDataHome,
                localAppData = localAppData,
                temporaryDirectory = System.getProperty("java.io.tmpdir"),
            )
        ).auxiliaryBinariesDirectory
}
