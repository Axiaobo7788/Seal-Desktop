package com.junkfood.seal.download

import androidx.annotation.CheckResult
import com.junkfood.seal.download.Task.DownloadState.Idle
import com.junkfood.seal.download.Task.DownloadState.ReadyWithInfo
import com.junkfood.seal.download.PlaylistSelectionMapper
import com.junkfood.seal.download.SelectionMerge
import com.junkfood.seal.util.DownloadPreferences
import com.junkfood.seal.util.createFromPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.VideoClip
import com.junkfood.seal.util.VideoInfo
import kotlin.math.roundToInt

object TaskFactory {
    /**
     * @return A [TaskWithState] with extra configurations made by user in the custom format
     *   selection page
     */
    @CheckResult
    fun createWithConfigurations(
        videoInfo: VideoInfo,
        formatList: List<Format>,
        videoClips: List<VideoClip>,
        splitByChapter: Boolean,
        newTitle: String,
        selectedSubtitles: List<String>,
        selectedAutoCaptions: List<String>,
    ): TaskWithState {
        val fileSize =
            formatList.fold(.0) { acc, format ->
                acc + (format.fileSize ?: format.fileSizeApprox ?: .0)
            }

        val basePreferences = DownloadPreferences.createFromPreferences()
        val merged =
            SelectionMerge.merge(
                basePreferences = basePreferences,
                videoInfo = videoInfo,
                formatList = formatList,
                videoClips = videoClips,
                splitByChapter = splitByChapter,
                newTitle = newTitle,
                selectedSubtitles = selectedSubtitles,
                selectedAutoCaptions = selectedAutoCaptions,
            )
        val info = merged.videoInfo
        val preferences = merged.preferences
        val audioOnlyFormats = merged.audioFormats
        val videoFormats = merged.videoFormats

        val task = Task(url = info.originalUrl.toString(), preferences = preferences)
        val state =
            Task.State(
                downloadState = ReadyWithInfo,
                videoInfo = info,
                viewState =
                    Task.ViewState.fromVideoInfo(info = info)
                        .copy(videoFormats = videoFormats, audioOnlyFormats = audioOnlyFormats),
            )

        return TaskWithState(task, state)
    }

    /** @return List of [TaskWithState]s created from playlist items */
    @CheckResult
    fun createWithPlaylistResult(
        playlistUrl: String,
        indexList: List<Int>,
        playlistResult: PlaylistResult,
        preferences: DownloadPreferences,
    ): List<TaskWithState> {
        val mapped = PlaylistSelectionMapper.map(playlistResult, indexList)

        val taskList =
            mapped.map { (index, itemView) ->
                val viewState =
                    Task.ViewState(
                        url = itemView.url,
                        title = itemView.title,
                        duration = itemView.duration,
                        uploader = itemView.uploader,
                        thumbnailUrl = itemView.thumbnailUrl,
                    )
                val task = Task(url = playlistUrl, preferences = preferences, type = Task.TypeInfo.Playlist(index))
                val state = Task.State(downloadState = Idle, videoInfo = null, viewState = viewState)
                TaskWithState(task, state)
            }

        return taskList
    }

    data class TaskWithState(val task: Task, val state: Task.State)
}
