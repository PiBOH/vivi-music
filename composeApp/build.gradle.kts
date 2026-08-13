import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// InnerTube API key: injected from the environment at build time (CI) so the
// key is never committed to source. The packaged app reads it via the system
// property set below; local development can also provide it through a `.env`
// file read at runtime (see desktopMain `main.kt`).
val innertubeApiKey: String = System.getenv("INNERTUBE_API_KEY") ?: ""

// Canonical app version (SemVer). Single source of truth for the release tag,
// the in-app About display and the installer version (see `version.txt`).
val appVersion: String = rootProject.file("version.txt").readText().trim()

// The Compose/jpackage installer version must be a plain numeric
// "MAJOR.MINOR.PATCH" with MAJOR > 0 and no pre-release suffix, while the app
// SemVer is still 0.x. Map the numeric part so MAJOR is at least 1:
// "0.0.2-alpha" -> "1.0.2" (the MINOR.PATCH tracks the SemVer exactly).
val installerVersion: String = appVersion
    .substringBefore("-")
    .substringBefore("+")
    .split(".")
    .let { parts ->
        val major = parts.firstOrNull()?.toIntOrNull() ?: 0
        val rest = parts.drop(1).ifEmpty { listOf("0") }
        listOf(if (major > 0) major else 1).map(Int::toString) + rest
    }
    .joinToString(".")

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            implementation(libs.supabase.kt)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            implementation(libs.material.kolor)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }

        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

dependencies {
    add("kspDesktop", libs.room.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

compose.desktop {
    application {
        mainClass = "com.vivimusic.de.MainKt"
        jvmArgs += listOf(
            "-DINNERTUBE_API_KEY=$innertubeApiKey",
            "-DAPP_VERSION=$appVersion",
        )

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Deb,
                TargetFormat.AppImage
            )
            packageName = "ViviMusicDE"
            packageVersion = installerVersion
            description = "Vivi Music DE, desktop client for ViVi Music."
            vendor = "PiBOH"
            copyright = "Copyright (c) 2026 PiBOH. https://piboh.github.io/"

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
                // Add the app to the Start Menu and create a desktop shortcut
                // (both default to false in Compose, so the app is otherwise
                // invisible after install).
                menu = true
                shortcut = true
                menuGroup = "Vivi Music DE"
                // Stable GUID so future MSI versions upgrade the existing
                // install instead of failing with "already installed".
                upgradeUuid = "a1e8f3d2-4b5c-4d6e-8f90-1a2b3c4d5e6f"
            }
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                // Reverse-DNS bundle identifier required by jpackage for a
                // valid macOS app bundle.
                bundleID = "com.vivimusic.de"
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.vivimusic.de.resources"
}
