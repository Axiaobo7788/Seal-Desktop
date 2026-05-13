package com.junkfood.seal

import android.app.PendingIntent
import android.util.Log
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.junkfood.seal.App.Companion.applicationScope
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.App.Companion.startService
import com.junkfood.seal.App.Companion.stopService
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.util.COMMAND_DIRECTORY
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.PlaylistEntry
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.ToastUtil
import com.junkfood.seal.util.VideoInfo
import com.junkfood.seal.util.createFromPreferences
import com.junkfood.seal.util.toHttpsUrl
import com.yausername.youtubedl_android.YoutubeDL
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Singleton Downloader for state holder & perform downloads, used by `Activity` & `Service` */
object Downloader {

    private const val TAG = "Downloader"

    sealed class State {
        data class DownloadingPlaylist(val currentItem: Int = 0, val itemCount: Int = 0) : State()

        data object DownloadingVideo : State()

        data object FetchingInfo : State()

        data object Idle : State()

        data object Updating : State()
    }

    sealed class ErrorState(open val url: String = "", open val report: String = "") {
        data class DownloadError(override val url: String, override val report: String) :
            ErrorState(url = url, report = report)

        data class FetchInfoError(override val url: String, override val report: String) :
            ErrorState(url = url, report = report)

        data object None : ErrorState()

        val title: String
            @Composable
            get() =
                when (this) {
                    is DownloadError -> stringResource(id = R.string.download_error_msg)
                    is FetchInfoError -> stringResource(id = R.string.fetch_info_error_msg)
                    None -> ""
                }
    }

    data class CustomCommandTask(
        val template: CommandTemplate,
        val url: String,
        val output: String,
        val state: State,
        val currentLine: String,
    ) {
        fun toKey() = makeKey(url, template.name)

        sealed class State {
            data class Error(val errorReport: String) : State()

            object Completed : State()

            object Canceled : State()

            data class Running(val progress: Float) : State()
        }

        override fun hashCode(): Int {
            return (this.url + this.template.name + this.template.template).hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CustomCommandTask

            if (template != other.template) return false
            if (url != other.url) return false
            if (output != other.output) return false
            if (state != other.state) return false
            if (currentLine != other.currentLine) return false

            return true
        }

        fun onCopyLog(clipboardManager: ClipboardManager) {
            clipboardManager.setText(AnnotatedString(output))
        }

        fun onRestart() {
            applicationScope.launch(Dispatchers.IO) {
                DownloadUtil.executeCommandInBackground(url, template)
            }
        }

        fun onCopyError(clipboardManager: ClipboardManager) {
            clipboardManager.setText(AnnotatedString(currentLine))
            ToastUtil.makeToast(R.string.error_copied)
        }

        fun onCancel() {
            toKey().run {
                YoutubeDL.destroyProcessById(this)
                onProcessCanceled(this)
            }
        }
    }

    data class DownloadTaskItem(
        val webpageUrl: String = "",
        val title: String = "",
        val uploader: String = "",
        val duration: Int = 0,
        val fileSizeApprox: Double = .0,
        val progress: Float = 0f,
        val progressText: String = "",
        val thumbnailUrl: String = "",
        val taskId: String = "",
        val playlistIndex: Int = 0,
    )

    private var currentJob: Job? = null

    private val mutableDownloaderState: MutableStateFlow<State> = MutableStateFlow(State.Idle)
    private val mutableTaskState = MutableStateFlow(DownloadTaskItem())
    private val mutablePlaylistResult = MutableStateFlow(PlaylistResult())
    private val mutableErrorState: MutableStateFlow<ErrorState> = MutableStateFlow(ErrorState.None)
    private val mutableProcessCount = MutableStateFlow(0)
    private val mutableQuickDownloadCount = MutableStateFlow(0)

    val mutableTaskList = mutableStateMapOf<String, CustomCommandTask>()

    val taskState = mutableTaskState.asStateFlow()
    val downloaderState = mutableDownloaderState.asStateFlow()
    val playlistResult = mutablePlaylistResult.asStateFlow()
    val errorState = mutableErrorState.asStateFlow()
    val processCount = mutableProcessCount.asStateFlow()

    init {
        applicationScope.launch {
            downloaderState
                .combine(processCount) { state, cnt ->
                    if (cnt > 0) true
                    else
                        when (state) {
                            is State.Idle -> false
                            else -> true
                        }
                }
                .combine(mutableQuickDownloadCount) { isRunning, cnt ->
                    if (!isRunning) cnt > 0 else true
                }
                .collect { if (it) startService() else stopService() }
        }
    }

    fun isDownloaderAvailable(): Boolean {
        return downloaderState.value is State.Idle
    }

    fun makeKey(url: String, templateName: String): String = "${templateName}_$url"

    fun onTaskStarted(template: CommandTemplate, url: String) =
        CustomCommandTask(
                template = template,
                url = url,
                output = "",
                state = CustomCommandTask.State.Running(0f),
                currentLine = "",
            )
            .run { mutableTaskList.put(this.toKey(), this) }

    fun updateTaskOutput(template: CommandTemplate, url: String, line: String, progress: Float) {
        val key = makeKey(url, template.name)
        val oldValue = mutableTaskList[key] ?: return
        val newValue =
            oldValue.run {
                copy(
                    output = output + line + "\n",
                    currentLine = line,
                    state = CustomCommandTask.State.Running(progress),
                )
            }
        mutableTaskList[key] = newValue
    }

    fun onTaskEnded(template: CommandTemplate, url: String, response: String? = null) {
        val key = makeKey(url, template.name)
        NotificationUtil.finishNotification(
            notificationId = key.toNotificationId(),
            title = key,
            text = context.getString(R.string.status_completed),
        )
        mutableTaskList.run {
            val oldValue = get(key) ?: return
            val newValue =
                oldValue.copy(state = CustomCommandTask.State.Completed).run {
                    response?.let { copy(output = response) } ?: this
                }
            this[key] = newValue
        }
        FileUtil.scanDownloadDirectoryToMediaLibrary(COMMAND_DIRECTORY.getString())
    }

    fun onProcessEnded() = mutableProcessCount.update { it - 1 }

    fun onProcessCanceled(taskId: String) =
        mutableTaskList.run {
            get(taskId)?.let { this.put(taskId, it.copy(state = CustomCommandTask.State.Canceled)) }
        }

    fun onTaskError(errorReport: String, template: CommandTemplate, url: String) =
        mutableTaskList.run {
            val key = makeKey(url, template.name)
            NotificationUtil.notifyError(
                title = "",
                notificationId = key.toNotificationId(),
                report = errorReport,
            )
            val oldValue = mutableTaskList[key] ?: return
            mutableTaskList[key] =
                oldValue.copy(
                    state = CustomCommandTask.State.Error(errorReport),
                    currentLine = errorReport,
                    output = oldValue.output + "\n" + errorReport,
                )
        }

    private fun VideoInfo.toTask(playlistIndex: Int = 0, preferencesHash: Int): DownloadTaskItem =
        DownloadTaskItem(
            webpageUrl = webpageUrl.toString(),
            title = title,
            uploader = uploader ?: channel ?: uploaderId.toString(),
            duration = duration?.roundToInt() ?: 0,
            taskId = id + preferencesHash,
            thumbnailUrl = thumbnail.toHttpsUrl(),
            fileSizeApprox = fileSize ?: fileSizeApprox ?: .0,
            playlistIndex = playlistIndex,
        )

    fun updateState(state: State) = mutableDownloaderState.update { state }

    fun clearErrorState() {
        mutableErrorState.update { ErrorState.None }
    }

    private fun fetchInfoError(url: String, errorReport: String) {
        mutableErrorState.update { ErrorState.FetchInfoError(url, errorReport) }
    }

    private fun downloadError(url: String, errorReport: String) {
        mutableErrorState.update { ErrorState.DownloadError(url, errorReport) }
    }

    }

    fun updatePlaylistResult(playlistResult: PlaylistResult = PlaylistResult()) =
        mutablePlaylistResult.update { playlistResult }

    fun executeCommandWithUrl(url: String) =
        applicationScope.launch(Dispatchers.IO) { DownloadUtil.executeCommandInBackground(url) }


    fun onProcessStarted() = mutableProcessCount.update { it + 1 }

    fun String.toNotificationId(): Int = this.hashCode()
}
