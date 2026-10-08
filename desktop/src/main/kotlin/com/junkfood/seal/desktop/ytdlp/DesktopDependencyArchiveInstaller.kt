package com.junkfood.seal.desktop.ytdlp

import com.junkfood.seal.desktop.i18n.AndroidStrings
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipFile

internal fun installDependencyZip(archive: Path, target: Path, isWindows: Boolean, tools: Set<String>) {
    val staging = Files.createTempDirectory(target, "dependency-stage-")
    try {
        val extracted = mutableSetOf<String>()
        // ZipFile requires the central directory; streaming alone can accept a truncated archive.
        ZipFile(archive.toFile()).use { zip ->
            zip.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
                val name = entry.name.substringAfterLast('/').substringAfterLast('\\').removeSuffix(".exe")
                if (name !in tools) return@forEach
                if (!extracted.add(name)) throw IOException(AndroidStrings.get("desktop_dependency_invalid_archive"))
                val fileName = if (isWindows) "$name.exe" else name
                val checksum = CRC32()
                CheckedInputStream(zip.getInputStream(entry), checksum).use { input ->
                    Files.copy(input, staging.resolve(fileName))
                }
                if (entry.size <= 0 || Files.size(staging.resolve(fileName)) != entry.size || checksum.value != entry.crc) {
                    throw IOException(AndroidStrings.get("desktop_dependency_invalid_archive"))
                }
                if (!isWindows && !staging.resolve(fileName).toFile().setExecutable(true, false)) {
                    throw IOException(AndroidStrings.get("desktop_dependency_invalid_archive"))
                }
            }
        }
        check(extracted.containsAll(tools)) {
            AndroidStrings.format("desktop_dependency_archive_missing_tools", (tools - extracted).joinToString())
        }
        // Publish only after every required payload has passed validation. Keep old binaries on failure.
        tools.forEach { name ->
            val fileName = if (isWindows) "$name.exe" else name
            try {
                Files.move(staging.resolve(fileName), target.resolve(fileName), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(staging.resolve(fileName), target.resolve(fileName), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        DesktopDependencyResolver.invalidateHealth()
    } finally {
        staging.toFile().deleteRecursively()
    }
}
