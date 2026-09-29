@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.junkfood.seal.desktop.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import java.lang.reflect.Proxy
import kotlin.reflect.KFunction

internal interface DesktopResourceEnvironmentBridge {
    fun systemEnvironment(): Any

    fun environmentWithLocale(base: Any, locale: DesktopResourceLocale): Any

    fun installProvider(provider: KFunction<Any>)

    fun localComposeEnvironment(): ProvidableCompositionLocal<Any>

    fun composeEnvironment(environment: Any): Any
}

internal class DesktopResourceEnvironmentController(
    private val bridge: DesktopResourceEnvironmentBridge,
) {
    @Volatile
    private var languageOverrideTag: String? = null

    private val originalEnvironment: Any? by lazy {
        runCatching(bridge::systemEnvironment).getOrNull()
    }

    fun setLanguageOverride(tag: String?) {
        languageOverrideTag = tag?.takeIf(String::isNotBlank)
    }

    fun installProvider(): Boolean {
        originalEnvironment ?: return false
        return runCatching {
            bridge.installProvider(::providerEnvironment)
            true
        }.getOrDefault(false)
    }

    internal fun currentEnvironmentOrNull(): Any? {
        val base = originalEnvironment ?: return null
        val tag = languageOverrideTag ?: return base
        return runCatching {
            bridge.environmentWithLocale(base, desktopResourceLocaleForTag(tag))
        }.getOrDefault(base)
    }

    @Composable
    fun Provide(content: @Composable () -> Unit) {
        val environment = currentEnvironmentOrNull()
        if (environment == null) {
            content()
            return
        }

        val localEnvironment =
            remember {
                runCatching(bridge::localComposeEnvironment).getOrNull()
            }
        val composeEnvironment =
            remember(environment) {
                runCatching { bridge.composeEnvironment(environment) }.getOrNull()
            }

        if (localEnvironment != null && composeEnvironment != null) {
            CompositionLocalProvider(localEnvironment provides composeEnvironment, content = content)
        } else {
            content()
        }
    }

    private fun providerEnvironment(): Any =
        checkNotNull(currentEnvironmentOrNull()) {
            "Compose resource environment is unavailable"
        }
}

internal object DesktopResourceEnvironmentAdapter {
    private val controller = DesktopResourceEnvironmentController(ReflectionResourceEnvironmentBridge)

    fun installProvider(): Boolean = controller.installProvider()

    fun setLanguageOverride(tag: String?) {
        controller.setLanguageOverride(tag)
    }

    @Composable
    fun Provide(content: @Composable () -> Unit) {
        controller.Provide(content)
    }
}

internal object ReflectionResourceEnvironmentBridge : DesktopResourceEnvironmentBridge {
    private const val resourceEnvironmentClassName = "org.jetbrains.compose.resources.ResourceEnvironment"
    private const val resourceEnvironmentKtClassName = "org.jetbrains.compose.resources.ResourceEnvironmentKt"

    override fun systemEnvironment(): Any {
        val resourceEnvironmentKt = Class.forName(resourceEnvironmentKtClassName)
        return resourceEnvironmentKt.getMethod("getSystemResourceEnvironment").invoke(null)
    }

    override fun environmentWithLocale(base: Any, locale: DesktopResourceLocale): Any {
        val languageQualifierClass = Class.forName("org.jetbrains.compose.resources.LanguageQualifier")
        val regionQualifierClass = Class.forName("org.jetbrains.compose.resources.RegionQualifier")
        val themeQualifierClass = Class.forName("org.jetbrains.compose.resources.ThemeQualifier")
        val densityQualifierClass = Class.forName("org.jetbrains.compose.resources.DensityQualifier")
        val resourceEnvironmentClass = Class.forName(resourceEnvironmentClassName)

        val baseLanguage = qualifier(base, "getLanguage\$library")
        val baseRegion = qualifier(base, "getRegion\$library")
        val baseTheme = qualifier(base, "getTheme\$library")
        val baseDensity = qualifier(base, "getDensity\$library")

        val language =
            locale.language.takeIf(String::isNotBlank)?.let {
                languageQualifierClass.getConstructor(String::class.java).newInstance(it)
            } ?: baseLanguage
        val region =
            locale.region?.let {
                regionQualifierClass.getConstructor(String::class.java).newInstance(it)
            } ?: baseRegion

        val constructor =
            resourceEnvironmentClass.getConstructor(
                languageQualifierClass,
                regionQualifierClass,
                themeQualifierClass,
                densityQualifierClass,
            )
        return constructor.newInstance(language, region, baseTheme, baseDensity)
    }

    override fun installProvider(provider: KFunction<Any>) {
        val resourceEnvironmentKt = Class.forName(resourceEnvironmentKtClassName)
        val method = resourceEnvironmentKt.getMethod("setGetResourceEnvironment", KFunction::class.java)
        method.invoke(null, provider)
    }

    @Suppress("UNCHECKED_CAST")
    override fun localComposeEnvironment(): ProvidableCompositionLocal<Any> {
        val resourceEnvironmentKt = Class.forName(resourceEnvironmentKtClassName)
        return resourceEnvironmentKt
            .getMethod("getLocalComposeEnvironment")
            .invoke(null) as ProvidableCompositionLocal<Any>
    }

    override fun composeEnvironment(environment: Any): Any {
        val interfaceClass = Class.forName("org.jetbrains.compose.resources.ComposeEnvironment")
        return Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
        ) { proxy, method, args ->
            when (method.name) {
                "rememberEnvironment" -> environment
                "toString" -> "DesktopComposeEnvironment($environment)"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }
    }

    private fun qualifier(target: Any, methodName: String): Any =
        checkNotNull(
            runCatching { target.javaClass.getMethod(methodName).invoke(target) }.getOrNull(),
        ) {
            "Missing Compose resource qualifier: $methodName"
        }
}
