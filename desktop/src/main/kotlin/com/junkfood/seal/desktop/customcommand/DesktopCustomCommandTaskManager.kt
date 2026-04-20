package com.junkfood.seal.desktop.customcommand

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.junkfood.seal.desktop.network.DesktopProxyResolver
import com.junkfood.seal.desktop.settings.DesktopAppSettings
import com.junkfood.seal.desktop.settings.DesktopCommandTemplate
import com.junkfood.seal.desktop.ytdlp.DesktopYtDlpPaths
import com.junkfood.seal.desktop.ytdlp.DownloadPlanExecutor
import com.junkfood.seal.download.CustomCommandPlan
import com.junkfood.seal.download.YtDlpOption
import com.junkfood.seal.download.buildCustomCommandPlan
import com.junkfood.seal.util.DownloadPreferences
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DesktopCustomCommandTaskStatus {
    Running,
    Completed,
    Canceled,
    Error,
}

data class DesktopCustomCommandTask(
    val id: String,
    val templateId: Int,
    val templateLabel: String,
    val templateContent: String,
    val urlInput: String,
    val startedAtMillis: Long,
    val status: DesktopCustomCommandTaskStatus,
    val progress: Float?,
    val currentLine: String,
    val output: String,
    val errorReport: String?,
    val exitCode: Int?,
)

object DesktopCustomCommandTaskManager {
    private const val MAX_OUTPUT_CHARS = 120_000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val executor = DownloadPlanExecutor()
    private val runningProcesses = ConcurrentHashMap<String, DownloadPlanExecutor.RunningProcess>()
    private val canceledTaskIds = ConcurrentHashMap.newKeySet<String>()

    private val _tasks = mutableStateListOf<DesktopCustomCommandTask>()
    val tasks: SnapshotStateList<DesktopCustomCommandTask> = _tasks

    fun start(
        urlInput: String,
        template: DesktopCommandTemplate,
        preferences: DownloadPreferences,
        appSettings: DesktopAppSettings = DesktopAppSettings(),
    ): Result<String> {
        val urls = parseUrls(urlInput)
        if (urls.isEmpty()) {
            return Result.failure(IllegalArgumentException("URL is empty"))
        }

        val taskId = "cmd-${System.currentTimeMillis()}-${(_tasks.size + 1)}"
        val initial =
            DesktopCustomCommandTask(
                id = taskId,
                templateId = template.id,
                templateLabel = template.label,
                templateContent = template.template,
                urlInput = urlInput.trim(),
                startedAtMillis = System.currentTimeMillis(),
                status = DesktopCustomCommandTaskStatus.Running,
                progress = null,
                currentLine = "",
                output = "",
                errorReport = null,
                exitCode = null,
            )
        _tasks.add(0, initial)

        scope.launch {
            var configFile: Path? = null
            try {
                val plan = buildCustomCommandPlan(urls, preferences, preferences.commandDirectory)
                configFile = writeTemplateConfig(taskId, template.template)
                val args = buildCommandArgs(plan, urls, configFile)

                val runtimeProxy = DesktopProxyResolver.resolveProxyUrl(preferences, appSettings)
                val proxyEnv = DesktopProxyResolver.buildProxyEnvironment(runtimeProxy)
                val workingDir = DesktopYtDlpPaths.downloadDirectory(preferences.commandDirectory)

                val running =
                    withContext(Dispatchers.IO) {
                        executor.startArgs(
                            args = args,
                            config =
                                DownloadPlanExecutor.CommandExecutionConfig(
                                    workingDirectory = workingDir,
                                    extraEnv = proxyEnv,
                                ),
                            onStdout = { line ->
                                scope.launch { appendLine(taskId, line, isError = false) }
                            },
                            onStderr = { line ->
                                scope.launch { appendLine(taskId, line, isError = isYtDlpErrorLine(line)) }
                            },
                        )
                    }

                runningProcesses[taskId] = running
                val result = withContext(Dispatchers.IO) { running.waitForResult() }
                runningProcesses.remove(taskId)

                val canceled = canceledTaskIds.remove(taskId)
                if (canceled) {
                    updateTask(taskId) {
                        it.copy(
                            status = DesktopCustomCommandTaskStatus.Canceled,
                            progress = null,
                        )
                    }
                    return@launch
                }

                val success = result.exitCode == 0
                updateTask(taskId) {
                    it.copy(
                        status =
                            if (success) DesktopCustomCommandTaskStatus.Completed
                            else DesktopCustomCommandTaskStatus.Error,
                        progress = if (success) 1f else it.progress,
                        errorReport =
                            if (success) {
                                null
                            } else {
                                result.stderr.lastOrNull { line -> isYtDlpErrorLine(line) }
                                    ?: result.stderr.lastOrNull()
                            },
                        exitCode = result.exitCode,
                    )
                }
            } catch (e: Exception) {
                updateTask(taskId) {
                    it.copy(
                        status = DesktopCustomCommandTaskStatus.Error,
                        errorReport = e.message ?: e.toString(),
                        currentLine = e.message ?: e.toString(),
                    )
                }
            } finally {
                runningProcesses.remove(taskId)
                configFile?.let { runCatching { Files.deleteIfExists(it) } }
            }
        }

        return Result.success(taskId)
    }

    fun cancel(taskId: String) {
        canceledTaskIds.add(taskId)
        runningProcesses[taskId]?.cancel()
        updateTask(taskId) {
            it.copy(
                status = DesktopCustomCommandTaskStatus.Canceled,
                progress = null,
            )
        }
    }

    fun restart(
        taskId: String,
        preferences: DownloadPreferences,
        appSettings: DesktopAppSettings = DesktopAppSettings(),
    ): Result<String> {
        val task = _tasks.firstOrNull { it.id == taskId }
            ?: return Result.failure(IllegalArgumentException("Task not found"))

        return start(
            urlInput = task.urlInput,
            template =
                DesktopCommandTemplate(
                    id = task.templateId,
                    label = task.templateLabel,
                    template = task.templateContent,
                ),
            preferences = preferences,
            appSettings = appSettings,
        )
    }

    private fun updateTask(taskId: String, transform: (DesktopCustomCommandTask) -> DesktopCustomCommandTask) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            _tasks[index] = transform(_tasks[index])
        }
    }

    private fun appendLine(taskId: String, line: String, isError: Boolean) {
        val display = if (isError) "[err] $line" else line
        val progress = parseProgress(line)
        updateTask(taskId) { current ->
            val nextOutput =
                if (current.output.isBlank()) display
                else (current.output + "\n" + display).takeLast(MAX_OUTPUT_CHARS)
            current.copy(
                currentLine = display,
                output = nextOutput,
                progress = progress ?: current.progress,
                errorReport = if (isError) line else current.errorReport,
            )
        }
    }

    private fun buildCommandArgs(plan: CustomCommandPlan, urls: List<String>, configFile: Path): List<String> {
        val args = mutableListOf<String>()
        args += plan.options.flatMap { it.asCliArgs() }
        if (plan.needsCookiesFile) {
            args += listOf("--cookies", DesktopYtDlpPaths.cookiesFile().toAbsolutePath().toString())
        }
        if (plan.needsArchiveFile) {
            args +=
                listOf(
                    "--download-archive",
                    DesktopYtDlpPaths.archiveFile().toAbsolutePath().toString(),
                )
        }
        args += listOf("--config-locations", configFile.toAbsolutePath().toString())
        args += urls
        return args
    }

    private fun writeTemplateConfig(taskId: String, template: String): Path {
        val dir = DesktopYtDlpPaths.tempDirectory()
        runCatching { Files.createDirectories(dir) }
        val path = dir.resolve("cmd-template-$taskId.conf")
        Files.writeString(path, template)
        return path
    }

    private fun parseUrls(input: String): List<String> =
        input.split(Regex("[\\n ]+")).map { it.trim() }.filter { it.isNotBlank() }

    private fun parseProgress(line: String): Float? {
        val match = Regex("(\\d{1,3}(?:\\.\\d+)?)%")
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toFloatOrNull()
            ?: return null
        return (match / 100f).coerceIn(0f, 1f)
    }

    private fun isYtDlpErrorLine(line: String): Boolean {
        val normalized = line.trim().lowercase()
        if (normalized.isBlank()) return false
        if (normalized.startsWith("[debug]")) return false
        if (normalized.startsWith("debug:")) return false
        if (normalized.contains("ffmpeg command line")) return false
        return normalized.contains("error:") ||
            normalized.contains("traceback") ||
            normalized.contains("exception") ||
            normalized.contains(" failed")
    }
}
