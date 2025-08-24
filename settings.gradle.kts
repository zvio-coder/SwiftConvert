pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") } // ✅ added JitPack for FFmpegKit
    }
    plugins {
        // Pin plugin versions so CI resolves them reliably
        id("com.android.application") version "8.5.2"
        id("org.jetbrains.kotlin.android") version "1.9.24"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // ✅ ensure available for dependencies
    }
}

rootProject.name = "SwiftConvert"
include(":app")
