package com.junkfood.seal.desktop.storage

enum class DesktopStorageBackend {
    Json,
    DualWrite,
    Sqlite,
}

internal object DesktopStorageConfig {
    private const val ENV_BACKEND = "SEAL_DESKTOP_STORAGE_BACKEND"
    private const val PROP_BACKEND = "seal.desktop.storage.backend"

    val backend: DesktopStorageBackend by lazy {
        val raw = System.getProperty(PROP_BACKEND)?.trim()?.ifBlank { null }
            ?: System.getenv(ENV_BACKEND)?.trim()?.ifBlank { null }
            ?: "dual"

        when (raw.lowercase()) {
            "json" -> DesktopStorageBackend.Json
            "sqlite" -> DesktopStorageBackend.Sqlite
            "dual", "dualwrite", "dual-write", "dual_write" -> DesktopStorageBackend.DualWrite
            else -> DesktopStorageBackend.DualWrite
        }
    }
}
