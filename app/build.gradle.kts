import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.legacy.kapt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

kotlin {
    jvmToolchain(17)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.isFile) {
    localPropertiesFile.reader().use { reader ->
        localProperties.load(reader)
    }
}

fun localDebugValue(key: String): String = localProperties.getProperty(key).orEmpty()

fun String.asBuildConfigString(): String =
    "\"" +
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") +
        "\""

// Keep local release builds unsigned; CI opts into signing only with a complete set.
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
val releaseSigningInputs =
    listOf(
        releaseKeystorePath,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    )
val configuredReleaseSigningInputs =
    releaseSigningInputs.count { !it.orNull.isNullOrBlank() }
val hasReleaseSigning = configuredReleaseSigningInputs == releaseSigningInputs.size

check(configuredReleaseSigningInputs == 0 || hasReleaseSigning) {
    "Set all ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
        "ANDROID_KEY_ALIAS, and ANDROID_KEY_PASSWORD variables for release signing."
}

android {
    namespace = "org.kaloscope.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.kaloscope.tv"
        minSdk = 23
        targetSdk = 37
        versionCode = 11
        versionName = "0.3.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "DEBUG_SERVER_NAME", "".asBuildConfigString())
        buildConfigField("String", "DEBUG_SERVER_URL", "".asBuildConfigString())
        buildConfigField("String", "DEBUG_USERNAME", "".asBuildConfigString())
        buildConfigField("String", "DEBUG_PASSWORD", "".asBuildConfigString())
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "DEBUG_SERVER_NAME",
                localDebugValue("kaloscope.debug.serverName").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "DEBUG_SERVER_URL",
                localDebugValue("kaloscope.debug.serverUrl").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "DEBUG_USERNAME",
                localDebugValue("kaloscope.debug.username").asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "DEBUG_PASSWORD",
                localDebugValue("kaloscope.debug.password").asBuildConfigString(),
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        // Legacy kapt loses Kotlin types from Javac's classpath when this source is disabled.
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.kotlin.metadata.jvm)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    implementation(libs.akdanmaku)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.mockwebserver)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.okhttp.bom))
    androidTestImplementation(libs.mockwebserver)
}
