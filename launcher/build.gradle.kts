import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

group = "dev.aetherclient"
version = "1.0.0"

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.material.icons.extended)

            implementation(libs.material.kolor)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.150.2")
            implementation(libs.kotlinx.coroutines.swing)
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.resources {
    packageOfResClass = "dev.aetherclient.launcher.resources"
}

compose.desktop {
    application {
        mainClass = "dev.aetherclient.launcher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "AetherLauncher"
            packageVersion = "1.0.0"
            description = "Material Design 3 launcher for AetherClient (Minecraft 1.21.11 Fabric)"
            vendor = "AetherClient"

            linux {
                packageName = "aether-launcher"
            }
            windows {
                menuGroup = "AetherClient"
                upgradeUuid = "b9f7e1c2-4d3a-4b88-9c31-7a6e0f3d2111"
            }
        }
    }
}
