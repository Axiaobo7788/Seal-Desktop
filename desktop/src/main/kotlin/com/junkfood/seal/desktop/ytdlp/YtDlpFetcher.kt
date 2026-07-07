package com.junkfood.seal.desktop.ytdlp

import java.nio.file.Path

class EnvironmentMissingException(message: String) : Exception(message)

/**
 * Compatibility facade for callers that still only need the yt-dlp path.
 * Dependency validation itself lives in [DesktopDependencyResolver].
 */
class YtDlpFetcher(
    // Retained for API compatibility.
    version: String = "latest",
    cacheRoot: Path? = null,
    private val environmentPreferenceProvider: () -> Int = { DesktopDependencyResolver.defaultEnvironmentPreference() },
) {
    fun cachedBinaryPath(): Path = Path.of("")

    fun resolveDependencies(): DesktopDependencyResolution =
        DesktopDependencyResolver.resolve(environmentPreferenceProvider())

    fun ensureDependencies(): DesktopDependencyResolution =
        DesktopDependencyResolver.requireComplete(environmentPreferenceProvider())

    fun findExistingBinary(): Path? =
        resolveDependencies().ytDlp?.path

    suspend fun invalidateCachedBinary(
        ytDlpUpdateChannel: Int = DesktopAuxiliaryDownloader.YT_DLP_CHANNEL_STABLE,
        onLog: (String) -> Unit = {},
    ): Boolean {
        val osName = System.getProperty("os.name").lowercase()
        val isWin = osName.contains("win")
        val isMac = osName.contains("mac") || osName.contains("darwin")
        return DesktopAuxiliaryDownloader.downloadYtDlpBinary(
            isWin = isWin,
            isMac = isMac,
            onLog = onLog,
            ytDlpUpdateChannel = ytDlpUpdateChannel,
        )
    }

    fun ensureBinary(): Path =
        ensureDependencies().ytDlp?.path
            ?: throw EnvironmentMissingException("yt-dlp is not bundled and not found in system or auxiliary paths.")
}
