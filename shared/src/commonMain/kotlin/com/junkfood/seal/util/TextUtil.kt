package com.junkfood.seal.util

/** Join non-blank strings with a delimiter. */
fun connectWithDelimiter(vararg strings: String?, delimiter: String): String =
    strings
        .toList()
        .filter { !it.isNullOrBlank() }
        .joinToString(separator = delimiter) { it.orEmpty() }

/** Check whether the string is an integer within [start, end] (inclusive). */
fun String.isNumberInRange(start: Int, end: Int): Boolean {
    if (isEmpty()) return false
    if (length >= 10) return false
    if (any { !it.isDigit() }) return false
    val value = toInt()
    return value in start..end
}

/** Overload for IntRange. */
fun String.isNumberInRange(range: IntRange): Boolean = isNumberInRange(range.first, range.last)

/** Ensure URL uses https; returns empty string if null. */
fun String?.toHttpsUrl(): String =
    this?.let { if (it.startsWith("http:")) it.replaceFirst("http", "https") else it } ?: ""
