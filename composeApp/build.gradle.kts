@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    androidTarget()
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.ktor.client.okhttp)
            implementation(libs.multiplatformSettings.noArg)


        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.core)

            // Ktor Core and Content Negotiation
//            implementation(libs.bundles.ktor)

            api(libs.ktor.client.core)                 // Core Ktor client functionality
            api(libs.ktor.client.content.negotiation)  // For handling JSON, XML, etc.
            api(libs.ktor.client.logging)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            // implementation(libs.kotlinx.coroutines.play.services) // Use the latest stable version

            // Supabase
            implementation(project.dependencies.platform("io.github.jan-tennert.supabase:bom:3.1.4"))
            implementation("io.github.jan-tennert.supabase:postgrest-kt")
            implementation("io.github.jan-tennert.supabase:storage-kt")
            implementation("io.github.jan-tennert.supabase:auth-kt")
            implementation("io.github.jan-tennert.supabase:realtime-kt")

            implementation(libs.multiplatformSettings.noArg) // Common dependency
            implementation(libs.multiplatformSettings.coroutines) // Optional for coroutines support

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            implementation(libs.slf4j.simple)
            // Ktor client for Desktop (CIO engine)
            // Ktor client dependencies for Desktop
            implementation("io.ktor:ktor-client-core:3.0.0")      // Ensures HttpTimeout and other core features are present
            implementation("io.ktor:ktor-client-cio:3.0.0")      // The CIO engine you are using
            implementation("io.ktor:ktor-client-logging:3.0.0")  // For the Logging plugin
            implementation(libs.multiplatformSettings.noArg) // For Desktop (uses Java Preferences)

        }
    }
}

android {
    namespace = "uz.mobiledv.test1"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "uz.mobiledv.test1"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "uz.mobiledv.test1.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "uz.mobiledv.test1"
            packageVersion = "1.0.0"
        }
    }
}
