package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadPreferences

/**
 * Platform-neutral plan for executing custom yt-dlp commands with user preferences.
 */
 data class CustomCommandPlan(
     val urls: List<String>,
     val options: List<YtDlpOption>,
     val needsCookiesFile: Boolean,
     val needsArchiveFile: Boolean,
 ) {
     fun asCliArgs(): List<String> =
         buildList {
             addAll(options.flatMap { it.asCliArgs() })
             addAll(urls)
         }
 }
 
 fun buildCustomCommandPlan(
     urls: List<String>,
     preferences: DownloadPreferences,
     commandDirectory: String,
 ): CustomCommandPlan {
     val opts = mutableListOf<YtDlpOption>()
     var needsCookies = false
     var needsArchive = false
 
     opts += YtDlpOption.Flag("--newline")
 
     if (commandDirectory.isNotEmpty()) {
         opts += YtDlpOption.KeyValue("-P", commandDirectory)
     }
 
     if (preferences.aria2c) {
         opts += YtDlpOption.KeyValue("--downloader", "libaria2c.so")
     }
     if (preferences.useDownloadArchive) {
         needsArchive = true
     }
     if (preferences.restrictFilenames) {
         opts += YtDlpOption.Flag("--restrict-filenames")
     }
     if (preferences.cookies) {
         needsCookies = true
         if (preferences.userAgentString.isNotEmpty()) {
             opts += YtDlpOption.KeyValue("--add-header", "User-Agent:${preferences.userAgentString}")
         }
     }
 
     return CustomCommandPlan(
         urls = urls,
         options = opts,
         needsCookiesFile = needsCookies,
         needsArchiveFile = needsArchive,
     )
 }
