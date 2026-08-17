package com.junkfood.seal.desktop.ytdlp

internal fun systemDependencyInstallCommands(
    isWindows: Boolean,
    isMac: Boolean,
    resolution: DesktopDependencyResolution,
    packageManagerExecutable: String? = null,
): List<List<String>> =
    when {
        isWindows ->
            buildList {
                val winget = packageManagerExecutable ?: "winget"
                if (resolution.ytDlp == null) add(wingetInstallCommand(winget, "yt-dlp.yt-dlp"))
                if (resolution.ffmpeg == null) add(wingetInstallCommand(winget, "Gyan.FFmpeg"))
            }
        isMac -> {
            val formulas =
                buildList {
                    if (resolution.ytDlp == null) add("yt-dlp")
                    if (resolution.ffmpeg == null) add("ffmpeg")
                }
            if (formulas.isEmpty()) {
                emptyList()
            } else {
                listOf(listOf(packageManagerExecutable ?: "brew", "install") + formulas)
            }
        }
        else -> emptyList()
    }

private fun wingetInstallCommand(winget: String, packageId: String): List<String> =
    listOf(
        winget,
        "install",
        "--id",
        packageId,
        "--exact",
        "--accept-package-agreements",
        "--accept-source-agreements",
        "--disable-interactivity",
    )
