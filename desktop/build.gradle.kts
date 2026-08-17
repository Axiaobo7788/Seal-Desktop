@file:Suppress("UnstableApiUsage")

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.tasks.Jar
import java.util.zip.ZipFile

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

val syncAndroidPreviewDrawables by tasks.registering(Copy::class) {
    from(project(":app").layout.projectDirectory.dir("src/main/res")) {
        include("drawable/sample*.webp")
    }
    into(layout.buildDirectory.dir("generated/androidPreviewResources"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(syncAndroidStringsXml)
    dependsOn(syncAndroidPreviewDrawables)
    from(layout.buildDirectory.dir("generated/androidStringResources"))
    from(layout.buildDirectory.dir("generated/androidPreviewResources"))
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        main {
            kotlin.srcDir(project(":color").layout.projectDirectory.dir("src/main/java"))
        }
    }
}

val desktopBuildOsName = System.getProperty("os.name").lowercase()
val desktopBuildArchName = System.getProperty("os.arch").lowercase()

fun sqliteNativeOsQualifier(osName: String): String =
    when {
        osName.contains("win") -> "Windows"
        osName.contains("mac") -> "Mac"
        osName.contains("linux") -> "Linux"
        else -> error("Unsupported SQLite desktop build OS: $osName")
    }

fun sqliteNativeArchQualifier(archName: String): String =
    when (archName) {
        "amd64", "x86_64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> error("Unsupported SQLite desktop build architecture: $archName")
    }

val sqliteNativeQualifier =
    "${sqliteNativeOsQualifier(desktopBuildOsName)}/${sqliteNativeArchQualifier(desktopBuildArchName)}"
val sqliteJdbcSource by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
val platformSqliteJdbc by tasks.registering(Jar::class) {
    group = "build setup"
    description = "Repackages sqlite-jdbc with only the current desktop platform native library."

    archiveFileName.set("sqlite-jdbc-${sqliteNativeQualifier.replace('/', '-')}.jar")
    destinationDirectory.set(layout.buildDirectory.dir("generated/sqlite-jdbc"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    inputs.property("sqliteNativeQualifier", sqliteNativeQualifier)

    val sqliteArchives = providers.provider {
        sqliteJdbcSource.files.map { zipTree(it) }
    }
    from(sqliteArchives) {
        exclude("org/sqlite/native/**")
        // Repacking invalidates any upstream archive signature metadata.
        exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
    }
    from(sqliteArchives) {
        include("org/sqlite/native/$sqliteNativeQualifier/**")
    }

    doLast {
        val output = archiveFile.get().asFile
        val targetPrefix = "org/sqlite/native/$sqliteNativeQualifier/"
        val nativeEntries = ZipFile(output).use { archive ->
            archive.entries().asSequence()
                .filterNot { it.isDirectory }
                .map { it.name }
                .filter { it.startsWith("org/sqlite/native/") }
                .toList()
        }
        check(nativeEntries.isNotEmpty()) {
            "sqlite-jdbc does not contain a native library for $sqliteNativeQualifier"
        }
        check(nativeEntries.all { it.startsWith(targetPrefix) }) {
            "Platform sqlite-jdbc unexpectedly contains other native targets: $nativeEntries"
        }

        val sourceBytes = sqliteJdbcSource.singleFile.length()
        logger.lifecycle(
            "Prepared ${output.name} for $sqliteNativeQualifier " +
                "(${sourceBytes / 1024} KiB -> ${output.length() / 1024} KiB)",
        )
    }
}

dependencies {
    add(sqliteJdbcSource.name, libs.sqlite.jdbc)

    implementation(project(":shared"))
    
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(files(platformSqliteJdbc.flatMap { it.archiveFile }).builtBy(platformSqliteJdbc))

    testImplementation(kotlin("test"))
}

val desktopPackageVersion = "${currentVersion.major}.${currentVersion.minor}.${currentVersion.patch}"
val desktopVersionName = currentVersion.name
val desktopMacOSPackageVersion =
    if (currentVersion.major > 0) {
        desktopPackageVersion
    } else {
        // Compose/macOS package validation requires MAJOR > 0. Keep the user-facing
        // app version at 0.x.y via -Dseal.app.version, but use a valid native package version.
        "1.${currentVersion.minor}.${currentVersion.patch}"
    }

fun String.asGradleBoolean(): Boolean =
    equals("true", ignoreCase = true) ||
        equals("yes", ignoreCase = true) ||
        equals("on", ignoreCase = true) ||
        this == "1"

val desktopWindowsDebugLauncher =
    (
        providers.gradleProperty("desktopWindowsDebugLauncher").orNull
            ?: providers.environmentVariable("DESKTOP_WINDOWS_DEBUG_LAUNCHER").orNull
    )?.asGradleBoolean() ?: false

val desktopReleaseProguardDefault = true
val desktopReleaseProguardEnabled =
    (
        providers.gradleProperty("desktopReleaseProguard").orNull
            ?: providers.environmentVariable("DESKTOP_RELEASE_PROGUARD").orNull
    )?.asGradleBoolean() ?: desktopReleaseProguardDefault

tasks.register("printDesktopPackageVersion") {
    group = "help"
    description = "Prints the desktop native package version used by Compose distributions."
    doLast {
        println(desktopPackageVersion)
    }
}

tasks.register("printDesktopVersionName") {
    group = "help"
    description = "Prints the user-facing desktop version name."
    doLast {
        println(desktopVersionName)
    }
}

tasks.register("printDesktopReleaseProguardEnabled") {
    group = "help"
    description = "Prints whether desktop release ProGuard is enabled for this build."
    doLast {
        println(desktopReleaseProguardEnabled)
    }
}

tasks.register("printDesktopMacOSPackageVersion") {
    group = "help"
    description = "Prints the macOS native package version accepted by pkg/dmg validation."
    doLast {
        println(desktopMacOSPackageVersion)
    }
}

tasks.register<JavaExec>("desktopStorageSelfCheck") {
    group = "verification"
    description = "Runs desktop storage self-check for json/dual/sqlite backends"
    dependsOn(tasks.named("classes"))

    val mainSourceSet = sourceSets.named("main").get()
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("com.junkfood.seal.desktop.storage.DesktopStorageSelfCheckMainKt")

    (project.findProperty("storageBackend") as String?)
        ?.takeIf { it.isNotBlank() }
        ?.let { backend ->
            jvmArgs("-Dseal.desktop.storage.backend=$backend")
        }

    (project.findProperty("storageStateDir") as String?)
        ?.takeIf { it.isNotBlank() }
        ?.let { stateDir ->
            jvmArgs("-Dseal.desktop.storage.stateDir=$stateDir")
        }
}

tasks.register<JavaExec>("desktopDependencyDownloadSelfCheck") {
    group = "verification"
    description = "Downloads and executes desktop yt-dlp, ffmpeg, and ffprobe in an isolated directory."
    dependsOn(tasks.named("classes"))

    val mainSourceSet = sourceSets.named("main").get()
    classpath = mainSourceSet.runtimeClasspath
    mainClass.set("com.junkfood.seal.desktop.ytdlp.DesktopDependencyDownloadSelfCheckMainKt")
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
                    isEnabled.set(desktopReleaseProguardEnabled)
                    version.set("7.6.0")
                    configurationFiles.from(project.file("proguard-rules.pro"))
                }
            }
        }

        nativeDistributions {
            val osName = System.getProperty("os.name").lowercase()
            val defaultTargetFormats = when {
                osName.contains("mac") -> arrayOf(TargetFormat.Dmg, TargetFormat.Pkg)
                osName.contains("win") -> arrayOf(TargetFormat.Exe)
                else -> arrayOf(TargetFormat.Deb, TargetFormat.Rpm)
            }
            val requestedTargetFormats =
                (providers.gradleProperty("desktopTargetFormats").orNull
                    ?: providers.environmentVariable("DESKTOP_TARGET_FORMATS").orNull)
                    ?.split(',')
                    ?.map { it.trim().lowercase() }
                    ?.filter { it.isNotEmpty() }
                    ?.map {
                        when (it) {
                            "dmg" -> TargetFormat.Dmg
                            "pkg" -> TargetFormat.Pkg
                            "exe" -> TargetFormat.Exe
                            "deb" -> TargetFormat.Deb
                            "rpm" -> TargetFormat.Rpm
                            else -> error("Unsupported desktop target format: $it")
                        }
                    }
                    ?.toTypedArray()
            val currentTargetFormats = requestedTargetFormats ?: defaultTargetFormats

            targetFormats(*currentTargetFormats)
            jvmArgs("-Dseal.app.version=$desktopVersionName")
            modules("java.sql")
            appResourcesRootDir.set(project.layout.projectDirectory.dir("appResources"))
            packageName = "Seal"
            packageVersion = desktopPackageVersion
            
            macOS {
                packageVersion = desktopMacOSPackageVersion
                packageBuildVersion = desktopMacOSPackageVersion
                dmgPackageVersion = desktopMacOSPackageVersion
                dmgPackageBuildVersion = desktopMacOSPackageVersion
                pkgPackageVersion = desktopMacOSPackageVersion
                pkgPackageBuildVersion = desktopMacOSPackageVersion
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                shortcut = true
                menu = true
                menuGroup = "Seal"
                console = desktopWindowsDebugLauncher
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
