package com.junkfood.seal.desktop.download.history

import com.junkfood.seal.desktop.i18n.AndroidStrings
import com.junkfood.seal.util.findURLsFromString
import java.util.UUID

internal const val HISTORY_RETENTION_LIMIT = 500

data class HistoryImportResult(
    val imported: Int = 0,
    val duplicates: Int = 0,
    val skipped: Int = 0,
    val truncated: Int = 0,
    val invalid: Int = 0,
)

internal data class HistoryImportUpdate(
    val entries: List<DesktopDownloadHistoryEntry>,
    val result: HistoryImportResult,
)

internal fun historyImportMessage(result: HistoryImportResult): String =
    AndroidStrings.format(
        "desktop_history_import_summary",
        result.imported,
        result.duplicates,
        result.skipped,
        result.truncated,
        result.invalid,
    )

internal fun importHistory(
    text: String,
    existing: List<DesktopDownloadHistoryEntry>,
    mode: DesktopHistoryImportMode,
): HistoryImportUpdate {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return HistoryImportUpdate(existing, HistoryImportResult())

    var invalid = 0
    val importedEntries =
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            runCatching { decodeHistoryEntries(trimmed) }
                .getOrElse {
                    throw IllegalArgumentException(AndroidStrings.get("desktop_history_import_invalid"), it)
                }
        } else {
            buildList {
                trimmed.lineSequence().forEach { line ->
                    val urls = findURLsFromString(line).distinct()
                    if (urls.isEmpty()) {
                        if (line.isNotBlank()) invalid += 1
                    } else {
                        urls.forEach { url ->
                            add(
                                DesktopDownloadHistoryEntry(
                                    id = UUID.randomUUID().toString(),
                                    title = url,
                                    url = url,
                                ),
                            )
                        }
                    }
                }
            }
        }

    val base = if (mode == DesktopHistoryImportMode.Merge) existing else emptyList()
    val ids = base.mapTo(mutableSetOf()) { it.id }
    val keys = base.mapTo(mutableSetOf()) { it.url.ifBlank { it.title } }
    var duplicates = 0
    var skipped = 0
    val added =
        importedEntries.filter { entry ->
            val key = entry.url.ifBlank { entry.title }
            when {
                entry.id.isBlank() || key.isBlank() -> {
                    skipped += 1
                    false
                }
                entry.id in ids || key in keys -> {
                    duplicates += 1
                    false
                }
                else -> {
                    ids += entry.id
                    keys += key
                    true
                }
            }
        }

    // Imported entries follow the same newest-first order as completed downloads.
    val combined = added + base
    val retained = if (added.isEmpty()) existing else combined.take(HISTORY_RETENTION_LIMIT)
    val retainedImportedIds = retained.asSequence().map { it.id }.toHashSet()
    val imported = added.count { it.id in retainedImportedIds }
    val truncated = if (added.isEmpty()) 0 else (combined.size - HISTORY_RETENTION_LIMIT).coerceAtLeast(0)

    return HistoryImportUpdate(
        entries = retained,
        result =
            HistoryImportResult(
                imported = imported,
                duplicates = duplicates,
                skipped = skipped,
                truncated = truncated,
                invalid = invalid,
            ),
    )
}
