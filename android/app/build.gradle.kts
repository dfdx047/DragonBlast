import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Dragon Blast: Kotlin/Compose launcher + the RB2 game shell (SDL GameActivity, in-game menu).
//
// The native libraries are built separately by tools/android/build_native.sh and staged into
// app/src/main/jniLibs (clearer logs and incremental builds than CMake through Gradle).
//
// Launcher-only builds: when the SDK tree (rexglue-sdk-android, next to the repository) is not there,
// the SDL Java classes are missing, so the game shell (org.rb2recomp.android) is left out. The APK opens
// the launcher; Play says the game engine is not in this build. "-Pdb.uiOnly=true" forces that mode.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sdkTree: File = (findProperty("rb2.sdk") as String?)?.let { file(it) }
    ?: rootProject.projectDir.parentFile.parentFile.resolve("rexglue-sdk-android")
val sdlJava = sdkTree.resolve("thirdparty/sdl3/android-project/app/src/main/java")
val uiOnly = (findProperty("db.uiOnly") as String?) == "true" || !sdlJava.isDirectory

android {
    namespace = "com.dfdx047.dragonblast"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.dfdx047.dragonblast"
        minSdk = 29      // memfd / ASharedMemory and a linker with the namespaces the SDK needs
        targetSdk = 35
        versionCode = (findProperty("app.versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("app.versionName") as String?) ?: "0.1.0"
        ndk { abiFilters += "arm64-v8a" }
        buildConfigField("boolean", "UI_ONLY", uiOnly.toString())
        // Only the three languages the launcher is translated to.
        @Suppress("DEPRECATION")
        resourceConfigurations += listOf("en", "pt", "es")
    }

    // Release key: keystore.properties next to settings.gradle.kts (not in git) with storeFile,
    // storePassword, keyAlias, keyPassword. Without it, the debug key.
    val keyFile = rootProject.file("keystore.properties")
    if (keyFile.exists()) {
        val p = Properties().apply { keyFile.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = rootProject.file(p.getProperty("storeFile"))
            storePassword = p.getProperty("storePassword")
            keyAlias = p.getProperty("keyAlias")
            keyPassword = p.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (keyFile.exists()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            // The SDK dlopen()s librexgpu-xenos.so by path from the native library directory.
            useLegacyPackaging = true
        }
    }

    androidResources {
        noCompress += listOf("so")
    }

    sourceSets {
        getByName("main") {
            if (!uiOnly) java.srcDir(sdlJava)
            // Third-party licence notices (android/licenses/NOTICE.txt), shown in About.
            assets.srcDir(rootProject.projectDir.resolve("licenses"))
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

// Launcher-only: the game shell needs SDL's classes.
if (uiOnly) {
    android.sourceSets.getByName("main").java.filter.exclude("org/rb2recomp/**")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
