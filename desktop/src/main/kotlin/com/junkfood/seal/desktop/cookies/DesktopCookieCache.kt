package com.junkfood.seal.desktop.cookies

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

data class DesktopCookieFileResult(
    val stats: DesktopCookiesStats,
    val permissionWarning: String? = null,
)

class DesktopCookieCache(
    private val permissionProtector: (Path) -> String? = ::restrictCookieFilePermissions,
) {
    fun inspect(path: Path, metadata: DesktopCookieCacheMetadata): DesktopCookieCacheSnapshot {
        val exists = path.exists() && Files.isRegularFile(path)
        val valid = exists && DesktopCookiesParser.isValidCookiesFile(path)
        val stats =
            if (valid) {
                runCatching { DesktopCookiesParser.parseStats(path) }.getOrDefault(DesktopCookiesStats(0, 0))
            } else {
                DesktopCookiesStats(0, 0)
            }
        return DesktopCookieCacheSnapshot(
            metadata = metadata,
            stats = stats,
            cacheExists = exists,
            cacheValid = valid,
        )
    }

    fun importFrom(source: Path, target: Path): Result<DesktopCookieFileResult> =
        runCatching {
            require(DesktopCookiesParser.isValidCookiesFile(source)) { "Invalid Netscape cookies file" }
            if (source.toAbsolutePath().normalize() == target.toAbsolutePath().normalize()) {
                return@runCatching DesktopCookieFileResult(
                    stats = DesktopCookiesParser.parseStats(target),
                    permissionWarning = permissionProtector(target),
                )
            }

            val temporary = createTemporaryTarget(target)
            try {
                Files.copy(source, temporary, REPLACE_EXISTING)
                promoteGeneratedFile(temporary, target).getOrThrow()
            } finally {
                runCatching { Files.deleteIfExists(temporary) }
            }
        }

    fun promoteGeneratedFile(source: Path, target: Path): Result<DesktopCookieFileResult> =
        runCatching {
            require(DesktopCookiesParser.isValidCookiesFile(source)) { "Generated cookies file is empty or invalid" }
            target.parent?.createDirectories()
            try {
                Files.move(source, target, REPLACE_EXISTING, ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(source, target, REPLACE_EXISTING)
            }
            DesktopCookieFileResult(
                stats = DesktopCookiesParser.parseStats(target),
                permissionWarning = permissionProtector(target),
            )
        }

    fun clear(path: Path): Result<Boolean> = runCatching { Files.deleteIfExists(path) }

    internal fun createTemporaryTarget(target: Path): Path {
        target.parent?.createDirectories()
        return target.resolveSibling(".${target.fileName}.tmp-${System.nanoTime()}")
    }
}

internal fun restrictCookieFilePermissions(
    path: Path,
    posixSupported: Boolean = FileSystems.getDefault().supportedFileAttributeViews().contains("posix"),
    permissionSetter: (Path, Set<PosixFilePermission>) -> Unit = Files::setPosixFilePermissions,
): String? {
    if (!posixSupported) return null
    return runCatching {
        permissionSetter(
            path,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
            ),
        )
    }.exceptionOrNull()?.let { it.message ?: it.toString() }
}
