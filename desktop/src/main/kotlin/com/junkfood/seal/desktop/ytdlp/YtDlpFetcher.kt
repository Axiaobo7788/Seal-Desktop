package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.settings.EnvPrefSystem
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

    suspend fun updateBinary(
        ytDlpUpdateChannel: Int = DesktopAuxiliaryDownloader.YT_DLP_CHANNEL_STABLE,
        onLog: (String) -> Unit = {},
    ): YtDlpUpdateResult {
        val resolution = resolveDependencies()
        if (resolution.ytDlpUpdateDisposition() == YtDlpUpdateDisposition.SystemManaged) {
            onLog("yt-dlp 由系统包管理器管理，Seal 不会下载或覆盖它。")
            return YtDlpUpdateResult.SystemManaged(resolution.ytDlp?.path)
        }

        val osName = System.getProperty("os.name").lowercase()
        val isWin = osName.contains("win")
        val isMac = osName.contains("mac") || osName.contains("darwin")
        return if (
            DesktopAuxiliaryDownloader.downloadYtDlpBinary(
                isWin = isWin,
                isMac = isMac,
                onLog = onLog,
                ytDlpUpdateChannel = ytDlpUpdateChannel,
            )
        ) {
            YtDlpUpdateResult.Updated
        } else {
            YtDlpUpdateResult.Failed
        }
    }

    fun ensureBinary(): Path =
        ensureDependencies().ytDlp?.path
            ?: throw EnvironmentMissingException("yt-dlp is not bundled and not found in system or auxiliary paths.")
}

internal enum class YtDlpUpdateDisposition {
    DownloadAppPrivate,
    SystemManaged,
}

internal fun DesktopDependencyResolution.ytDlpUpdateDisposition(): YtDlpUpdateDisposition =
    if (environmentPreference == EnvPrefSystem || ytDlp?.source == DesktopDependencySource.SystemPath) {
        YtDlpUpdateDisposition.SystemManaged
    } else {
        YtDlpUpdateDisposition.DownloadAppPrivate
    }

sealed interface YtDlpUpdateResult {
    data object Updated : YtDlpUpdateResult

    data class SystemManaged(val path: Path?) : YtDlpUpdateResult

    data object Failed : YtDlpUpdateResult
}
