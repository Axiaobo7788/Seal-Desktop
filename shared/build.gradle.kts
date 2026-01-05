@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.animation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting
        val androidUnitTest by getting

        val desktopMain by getting
        val desktopTest by getting
    }

    jvmToolchain(21)
}

android {
    namespace = "com.junkfood.seal.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}
