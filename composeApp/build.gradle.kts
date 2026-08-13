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

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Deb,
                TargetFormat.AppImage
            )
            packageName = "ViviMusicDE"
            // jpackage requires a numeric MAJOR >= 1 and no prerelease suffix,
            // so the installer version is kept separate from the app SemVer in
            // version.txt (currently 0.0.1-alpha).
            packageVersion = "1.0.0"
            description = "Vivi Music DE, desktop client for ViVi Music."
            vendor = "Vivi Music DE"

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.vivimusic.de.resources"
}
