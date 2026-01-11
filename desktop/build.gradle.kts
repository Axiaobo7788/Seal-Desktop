@file:Suppress("UnstableApiUsage")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

val syncAndroidStringsXml by tasks.registering(Copy::class) {
    from(project(":app").layout.projectDirectory.dir("src/main/res")) {
        include("values*/strings.xml")
    }
    into(layout.buildDirectory.dir("generated/androidStringResources"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncAndroidStringsXml)
    from(layout.buildDirectory.dir("generated/androidStringResources"))
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

// Override the bundled ProGuard version to one that understands Java 21 class files.
configurations.all {
    resolutionStrategy {
        force(
            "com.guardsquare:proguard-gradle:7.6.0",
            "com.guardsquare:proguard-base:7.6.0",
            "com.guardsquare:proguard-core:7.6.0",
        )
    }
}

compose.desktop {
    application {
        mainClass = "com.junkfood.seal.desktop.MainKt"

        buildTypes {
            release {
                proguard {
                    isEnabled.set(false)
                    version.set("7.6.0")
                }
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "Seal"
            packageVersion = "0.0.0"
        }
    }
}
