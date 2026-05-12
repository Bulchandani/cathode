plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.bulchandani.cathode"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.bulchandani.cathode"
        minSdk = 23
        targetSdk = 35
        versionCode = 96
        versionName = "0.8.16"
    }

    // Signing configs:
    // - `debug`: committed debug keystore. Local debug builds + any developer's
    //   machine produces an APK signed with this key. Public, no security value.
    // - `release`: production keystore. NOT committed. Loaded from env vars set
    //   by GitHub Actions from repo secrets (KEYSTORE_BASE64 base64-decoded
    //   to app/cathode-release.keystore, plus KEYSTORE_PASSWORD / KEY_ALIAS /
    //   KEY_PASSWORD). If those env vars are absent (local dev), release builds
    //   will fail to sign — debug builds are unaffected.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug-signing.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            val keystoreFile = file(
                System.getenv("CATHODE_KEYSTORE_PATH") ?: "cathode-release.keystore"
            )
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("CATHODE_KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("CATHODE_KEY_ALIAS") ?: "cathode-release"
                keyPassword = System.getenv("CATHODE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Only attach the release signing config if the keystore is actually
            // present — otherwise gradle errors out before any task runs even on
            // unrelated tasks (lint, etc.) just for trying to evaluate the config.
            if (file(System.getenv("CATHODE_KEYSTORE_PATH") ?: "cathode-release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.systemProperty("robolectric.graphicsMode", "NATIVE") }
        }
    }

    // lintVitalRelease has crashed inside AGP's Compose detector on this
    // build matrix. Skip it — we don't ship lint-gated releases.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.tv.foundation)
    implementation(libs.tv.material)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.okhttp)

    implementation(libs.coil.compose)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
