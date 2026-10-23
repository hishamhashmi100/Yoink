import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// CI passes -PappVersion=1.2.3 from the tag. Local builds fall back to 0.0.0.
val appVersion = providers.gradleProperty("appVersion").getOrElse("0.0.0")
val appVersionCode = appVersion.split(".").map { it.toIntOrNull() ?: 0 }
    .let { (it + listOf(0, 0, 0)).take(3) }
    .let { (major, minor, patch) -> major * 10_000 + minor * 100 + patch }
    .coerceAtLeast(1)

android {
    namespace = "io.github.hishamhashmi100.yoink"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.hishamhashmi100.yoink"
        minSdk = 29
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        // A fixed debug key, so CI debug builds can be installed over each other.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // Signed later with apksigner in the release workflow.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            // The universal APK also carries x86_64 (emulators, Chromebooks).
            isUniversalApk = true
        }
    }

    packaging {
        jniLibs {
            // Python and ffmpeg are executed from nativeLibraryDir, so they must be extracted on install.
            useLegacyPackaging = true
            // 32-bit x86 devices are practically gone; keep the universal APK smaller.
            excludes += "**/x86/*.so"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.android)
}
