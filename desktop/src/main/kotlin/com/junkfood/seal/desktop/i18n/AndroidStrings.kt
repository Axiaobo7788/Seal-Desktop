package com.junkfood.seal.desktop.i18n

import java.io.InputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory

object AndroidStrings {
    private val cacheByPath: MutableMap<String, Map<String, String>> = ConcurrentHashMap()

    fun get(key: String, locale: Locale = Locale.getDefault()): String {
        val candidates = candidatePaths(locale)
        for (path in candidates) {
            val map = cacheByPath.getOrPut(path) { loadStringsMap(path) }
            val value = map[key]
            if (value != null) return value
        }
        return key
    }

    fun format(key: String, vararg args: Any?, locale: Locale = Locale.getDefault()): String =
        String.format(locale, get(key, locale), *args)

    internal fun candidatePaths(locale: Locale): List<String> {
        val canonicalLanguage = locale.language.orEmpty()
        val region = locale.country.orEmpty()
        val script =
            locale.script.orEmpty().ifBlank {
                if (canonicalLanguage == "zh") {
                    when (region.uppercase(Locale.ROOT)) {
                        "CN", "SG" -> "Hans"
                        "TW", "HK", "MO" -> "Hant"
                        else -> ""
                    }
                } else {
                    ""
                }
            }
        val qualifierLanguages =
            when (canonicalLanguage) {
                "he" -> listOf("iw", "he")
                "id" -> listOf("in", "id")
                else -> listOf(canonicalLanguage)
            }.filter { it.isNotBlank() }

        val paths = ArrayList<String>(6)
        if (canonicalLanguage == "zh") {
            when (script) {
                "Hans" -> paths += "values-zh-rCN/strings.xml"
                "Hant" -> paths += "values-zh-rTW/strings.xml"
            }
        }
        if (region.isNotBlank()) {
            qualifierLanguages.forEach { language ->
                paths += "values-$language-r${region.uppercase(Locale.ROOT)}/strings.xml"
            }
        }
        qualifierLanguages.forEach { language -> paths += "values-$language/strings.xml" }
        paths += "values/strings.xml"
        return paths.distinct()
    }

    private fun loadStringsMap(resourcePath: String): Map<String, String> {
        val stream = classLoader().getResourceAsStream(resourcePath) ?: return emptyMap()
        stream.use {
            return parseAndroidStringsXml(it)
        }
    }

    private fun classLoader(): ClassLoader =
        Thread.currentThread().contextClassLoader ?: AndroidStrings::class.java.classLoader

    private fun parseAndroidStringsXml(input: InputStream): Map<String, String> {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.parse(input)
        val resources = document.documentElement
        val nodes = resources.childNodes

        val result = HashMap<String, String>()
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
            if (node.nodeName != "string") continue

            val element = node as org.w3c.dom.Element
            val name = element.getAttribute("name").orEmpty()
            if (name.isBlank()) continue

            val raw = element.textContent.orEmpty()
            result[name] = decodeAndroidEscapes(raw)
        }

        return result
    }

    private fun decodeAndroidEscapes(value: String): String {
        return value
            .replace("\\\\n", "\n")
            .replace("\\\\t", "\t")
            .replace("\\\\'", "'")
            .replace("\\\\\"", "\"")
            .replace("\\\\\\\\", "\\")
    }
}
