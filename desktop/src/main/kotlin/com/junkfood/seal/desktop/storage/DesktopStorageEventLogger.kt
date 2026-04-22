package com.junkfood.seal.desktop.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class DesktopStorageEvent(
    val timestampEpochMillis: Long,
    val level: String,
    val component: String,
    val event: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

internal object DesktopStorageEventLogger {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    fun info(
        component: String,
        event: String,
        message: String,
        details: Map<String, String> = emptyMap(),
    ) {
        log("INFO", component, event, message, details, null)
    }

    fun warn(
        component: String,
        event: String,
        message: String,
        details: Map<String, String> = emptyMap(),
        throwable: Throwable? = null,
    ) {
        log("WARN", component, event, message, details, throwable)
    }

    private fun log(
        level: String,
        component: String,
        event: String,
        message: String,
        details: Map<String, String>,
        throwable: Throwable?,
    ) {
        val payload =
            DesktopStorageEvent(
                timestampEpochMillis = System.currentTimeMillis(),
                level = level,
                component = component,
                event = event,
                message = message,
                details = details,
            )

        System.err.println("[DesktopStorageEvent] ${json.encodeToString(payload)}")
        throwable?.printStackTrace(System.err)
    }
}
