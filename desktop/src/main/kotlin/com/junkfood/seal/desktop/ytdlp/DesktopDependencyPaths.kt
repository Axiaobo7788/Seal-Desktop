package com.junkfood.seal.desktop.ytdlp

import java.nio.file.Path

internal object DesktopDependencyPaths {
    fun appPrivateDirectory(): Path {
        System.getProperty(AUXILIARY_DIR_PROPERTY)
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
            ?.let { return it }
        System.getenv(AUXILIARY_DIR_ENVIRONMENT)
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
            ?.let { return it }

        val osName = System.getProperty("os.name").lowercase()
        return defaultAppPrivateDirectory(
            isWindows = osName.contains("win"),
            isMac = osName.contains("mac") || osName.contains("darwin"),
            userHome = System.getProperty("user.home"),
            xdgDataHome = System.getenv("XDG_DATA_HOME"),
            localAppData = System.getenv("LOCALAPPDATA"),
        )
    }

    internal fun defaultAppPrivateDirectory(
        isWindows: Boolean,
        isMac: Boolean,
        userHome: String,
        xdgDataHome: String?,
        localAppData: String?,
    ): Path =
        when {
            isWindows -> Path.of(localAppData ?: "$userHome\\AppData\\Local", "Seal", "bin")
            isMac -> Path.of(userHome, "Library", "Application Support", "Seal", "bin")
            else -> {
                val configuredDataHome =
                    xdgDataHome
                        ?.takeIf { it.isNotBlank() }
                        ?.let(Path::of)
                        ?.takeIf { it.isAbsolute }
                val dataHome = configuredDataHome ?: Path.of(userHome, ".local", "share")
                dataHome.resolve("Seal").resolve("bin")
            }
        }

    private const val AUXILIARY_DIR_PROPERTY = "seal.desktop.auxiliaryDir"
    private const val AUXILIARY_DIR_ENVIRONMENT = "SEAL_DESKTOP_AUXILIARY_DIR"
}
