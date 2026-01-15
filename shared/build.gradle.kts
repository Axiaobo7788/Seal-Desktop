@file:Suppress("UnstableApiUsage")

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.junkfood.seal.shared.generated.resources"
}

val syncAndroidStringsToComposeResources by
    tasks.registering(Copy::class) {
        group = "resources"
        description = "Sync Android values*/strings.xml into shared composeResources (single source of truth)"

        val androidResDir = project(":app").layout.projectDirectory.dir("src/main/res")

        from(androidResDir) {
            include("values*/strings.xml")
        }
        into(layout.projectDirectory.dir("src/commonMain/composeResources"))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
                implementation(compose.components.resources)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.coil.kt.compose)
            }
        }
        val androidUnitTest by getting

        val desktopMain by getting
        val desktopTest by getting
    }

    jvmToolchain(21)
}

// Ensure Compose resource generation always sees latest synced strings.
tasks.configureEach {
    val name = this.name
    if (
        name.startsWith("generateResourceAccessorsFor") ||
            name == "generateComposeResClass" ||
            name.startsWith("convertXmlValueResourcesFor") ||
            name.startsWith("prepareComposeResources") ||
            name.startsWith("copyNonXmlValueResourcesFor")
    ) {
        dependsOn(syncAndroidStringsToComposeResources)
    }
}

android {
    namespace = "com.junkfood.seal.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}

compose {
    resources {
        publicResClass = true
        packageOfResClass = "com.junkfood.seal.shared.generated.resources"
    }
}
