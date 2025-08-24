plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.swiftconvert"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.swiftconvert"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"

        // Compose vector drawables support
        vectorDrawables { useSupportLibrary = true }
    }

    // ✅ Ensure CI always produces an installable APK
    signingConfigs {
        // Android Gradle Plugin provides a default debug keystore; this just makes it explicit
        getByName("debug") {
            // uses default debug.keystore from ~/.android
        }
        // For CI convenience you can also sign release with the debug key to force an APK output.
        // (Your Play workflow will sign AAB separately with the real upload key.)
        create("ciRelease") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ⚠️ For CI artifacts only. Remove or replace with real signing for production.
            signingConfig = signingConfigs.getByName("ciRelease")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Produce a universal debug APK for easy sideload/testing (keeps splits for release if you want)
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true // 👉 creates app-universal-debug.apk
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1"
        )
    }
}

dependencies {
    // --- Jetpack Compose ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // File access helpers
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Image loading (optional)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Media3 (optional/simple transforms) ---
    val media3 = "1.3.1"
    implementation("androidx.media3:media3-transformer:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-extractor:$media3")
    implementation("androidx.media3:media3-effect:$media3")

    // --- FFmpegKit (LTS via JitPack/Maven routing in settings.gradle.kts) ---
    implementation("com.arthenica:ffmpeg-kit-min-gpl:6.0-2.LTS")
}
