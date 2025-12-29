package com.junkfood.seal.shared

/**
 * Keep this file intentionally tiny.
 * We'll expand shared APIs incrementally during the A -> B migration.
 */
expect fun platformName(): String

object SharedInfo {
    const val name: String = "SealShared"

    fun greeting(): String = "Hello from ${platformName()}"
}
