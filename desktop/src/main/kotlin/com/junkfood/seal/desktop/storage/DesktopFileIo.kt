package com.junkfood.seal.desktop.storage

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

private val pathLocks = ConcurrentHashMap<String, Any>()

private inline fun <T> withPathLock(path: Path, block: () -> T): T {
    val key = path.toAbsolutePath().normalize().toString()
    val lock = pathLocks.computeIfAbsent(key) { Any() }
    return synchronized(lock) { block() }
}

internal fun writeTextAtomically(path: Path, content: String) {
    withPathLock(path) {
        path.parent?.createDirectories()
        val tempPath =
            path.resolveSibling(
                "${path.fileName}.tmp-${System.currentTimeMillis()}-${Thread.currentThread().id}"
            )

        Files.writeString(tempPath, content)

        try {
            try {
                Files.move(tempPath, path, REPLACE_EXISTING, ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempPath, path, REPLACE_EXISTING)
            }
        } finally {
            runCatching { Files.deleteIfExists(tempPath) }
        }
    }
}

internal fun quarantineCorruptedFile(path: Path): Path? {
    return withPathLock(path) {
        if (!path.exists()) return@withPathLock null

        val backupPath = path.resolveSibling("${path.fileName}.corrupt-${System.currentTimeMillis()}.bak")

        runCatching {
            try {
                Files.move(path, backupPath, REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.copy(path, backupPath, REPLACE_EXISTING)
                Files.deleteIfExists(path)
            }
            backupPath
        }.getOrNull()
    }
}
