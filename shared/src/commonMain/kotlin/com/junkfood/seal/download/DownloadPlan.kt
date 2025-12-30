package com.junkfood.seal.download

/**
 * Platform-agnostic yt-dlp execution plan. This only carries arguments/metadata and never touches
 * Android/desktop APIs; adapters can turn it into YoutubeDLRequest (Android) or a ProcessBuilder
 * (desktop) later.
 */
data class DownloadPlan(
    val options: List<YtDlpOption>,
    val outputTemplate: String,
    val downloadPathHint: String? = null,
    val needsCookiesFile: Boolean = false,
    val needsArchiveFile: Boolean = false,
) {
    /** Flatten options and output template into a CLI argument list. */
    fun asCliArgs(): List<String> =
        buildList {
            options.forEach { addAll(it.asCliArgs()) }
            add("-o")
            add(outputTemplate)
        }
}

sealed interface YtDlpOption {
    fun asCliArgs(): List<String>

    data class Flag(val name: String) : YtDlpOption {
        override fun asCliArgs(): List<String> = listOf(name)
    }

    data class KeyValue(val name: String, val value: String) : YtDlpOption {
        override fun asCliArgs(): List<String> = listOf(name, value)
    }

    /** Options that take multiple values, e.g. --replace-in-metadata title .+ newTitle */
    data class Multi(val name: String, val values: List<String>) : YtDlpOption {
        override fun asCliArgs(): List<String> = buildList { add(name); addAll(values) }
    }
}

class DownloadPlanBuilder {
    private val options = mutableListOf<YtDlpOption>()

    var outputTemplate: String = DEFAULT_OUTPUT_TEMPLATE
    var downloadPathHint: String? = null
    var needsCookiesFile: Boolean = false
    var needsArchiveFile: Boolean = false

    fun flag(name: String): DownloadPlanBuilder = apply { options += YtDlpOption.Flag(name) }

    fun option(name: String, value: String): DownloadPlanBuilder =
        apply { options += YtDlpOption.KeyValue(name, value) }

    fun option(name: String, vararg values: String): DownloadPlanBuilder =
        apply {
            when (values.size) {
                0 -> options += YtDlpOption.Flag(name)
                1 -> options += YtDlpOption.KeyValue(name, values.first())
                else -> options += YtDlpOption.Multi(name, values.toList())
            }
        }

    fun addAll(more: Iterable<YtDlpOption>): DownloadPlanBuilder = apply { options += more }

    fun markNeedsCookies(): DownloadPlanBuilder = apply { needsCookiesFile = true }

    fun markNeedsArchive(): DownloadPlanBuilder = apply { needsArchiveFile = true }

    fun build(): DownloadPlan =
        DownloadPlan(
            options = options.toList(),
            outputTemplate = outputTemplate,
            downloadPathHint = downloadPathHint,
            needsCookiesFile = needsCookiesFile,
            needsArchiveFile = needsArchiveFile,
        )

    companion object {
        /** yt-dlp output template that matches the previous default basename/ext combination. */
        const val DEFAULT_OUTPUT_TEMPLATE = "%(title).200B.%(ext)s"
    }
}
