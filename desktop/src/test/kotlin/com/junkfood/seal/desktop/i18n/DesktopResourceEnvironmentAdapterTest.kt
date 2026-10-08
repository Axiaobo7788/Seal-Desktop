package com.junkfood.seal.desktop.i18n

import androidx.compose.runtime.ProvidableCompositionLocal
import kotlin.reflect.KFunction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopResourceEnvironmentAdapterTest {
    @Test
    fun `current Compose resources API resolves system and overridden environments`() {
        val controller = DesktopResourceEnvironmentController(ReflectionResourceEnvironmentBridge)

        assertNotNull(controller.currentEnvironmentOrNull())
        controller.setLanguageOverride("zh-Hans")
        assertNotNull(controller.currentEnvironmentOrNull())
    }

    @Test
    fun `system environment is captured before provider installation`() {
        val base = Any()
        val override = Any()
        val bridge = FakeResourceEnvironmentBridge(base, override)
        val controller = DesktopResourceEnvironmentController(bridge)

        controller.setLanguageOverride("zh-Hans")

        assertTrue(controller.installProvider())
        assertEquals(listOf("system", "install"), bridge.events)
        assertTrue(bridge.installedProvider != null)
        assertSame(override, controller.currentEnvironmentOrNull())
    }

    @Test
    fun `locale reflection failure falls back to captured system environment`() {
        val base = Any()
        val bridge = FakeResourceEnvironmentBridge(base, Any(), failOverride = true)
        val controller = DesktopResourceEnvironmentController(bridge)

        controller.setLanguageOverride("zh-Hans")

        assertSame(base, controller.currentEnvironmentOrNull())
    }

    @Test
    fun `missing reflection leaves the default Compose provider untouched`() {
        val bridge = FakeResourceEnvironmentBridge(systemEnvironment = null, overrideEnvironment = Any())
        val controller = DesktopResourceEnvironmentController(bridge)

        assertFalse(controller.installProvider())
        assertNull(controller.currentEnvironmentOrNull())
        assertEquals(listOf("system"), bridge.events)
    }

    @Test
    fun `provider installation failure does not discard the system environment`() {
        val base = Any()
        val bridge = FakeResourceEnvironmentBridge(base, Any(), failInstall = true)
        val controller = DesktopResourceEnvironmentController(bridge)

        assertFalse(controller.installProvider())
        assertSame(base, controller.currentEnvironmentOrNull())
    }
}

private class FakeResourceEnvironmentBridge(
    private val systemEnvironment: Any?,
    private val overrideEnvironment: Any,
    private val failOverride: Boolean = false,
    private val failInstall: Boolean = false,
) : DesktopResourceEnvironmentBridge {
    val events = mutableListOf<String>()
    var installedProvider: KFunction<Any>? = null

    override fun systemEnvironment(): Any {
        events += "system"
        return systemEnvironment ?: error("reflection unavailable")
    }

    override fun environmentWithLocale(base: Any, locale: DesktopResourceLocale): Any {
        if (failOverride) error("qualifier API changed")
        return overrideEnvironment
    }

    override fun installProvider(provider: KFunction<Any>) {
        events += "install"
        if (failInstall) error("provider API changed")
        installedProvider = provider
    }

    override fun localComposeEnvironment(): ProvidableCompositionLocal<Any> =
        error("not needed by this test")

    override fun composeEnvironment(environment: Any): Any =
        error("not needed by this test")
}
