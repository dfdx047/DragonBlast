import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Dragon Blast: Kotlin/Compose launcher + the RB2 game shell (SDL GameActivity, in-game menu).
//
// The native libraries are built separately by tools/android/build_native.sh and staged into
// app/src/main/jniLibs (clearer logs and incremental builds than CMake through Gradle).
//
// Launcher-only builds: when the SDK tree (build/rexglue-sdk-android inside the repository) is not there,
// the SDL Java classes are missing, so the game shell (org.rb2recomp.android) is left out. The APK opens
// the launcher; Play says the game engine is not in this build. "-Pdb.uiOnly=true" forces that mode.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val sdkTree: File = (findProperty("rb2.sdk") as String?)?.let { file(it) }
    ?: rootProject.projectDir.parentFile.resolve("build/rexglue-sdk-android")
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
        // android/version.properties (one place for releases); -Papp.versionCode / -Papp.versionName override it.
        val ver = Properties().apply { rootProject.file("version.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
        versionCode = ((findProperty("app.versionCode") as String?) ?: ver.getProperty("versionCode") ?: "1").toInt()
        versionName = (findProperty("app.versionName") as String?) ?: ver.getProperty("versionName") ?: "0.1.0"
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

    // The Armv8.0 engine (tools/android/build_native.sh with THPS_MARCH=armv8-a) stages its libraries elsewhere:
    // -Prb2.jniLibs=<dir> packages that folder instead of src/main/jniLibs.
    (project.findProperty("rb2.jniLibs") as String?)?.let { dir ->
        sourceSets.getByName("main").jniLibs.setSrcDirs(listOf(dir))
    }
    // The encrypted game (libgame.zdat / libgame_v80.zdat + json, tools/android/pack_libgame.py), staged by
    // tools/android/build_apk.sh outside the source tree.
    (project.findProperty("rb2.gameAssets") as String?)?.let { dir ->
        sourceSets.getByName("main").assets.srcDir(dir)
    }

    packaging {
        jniLibs {
            // The SDK dlopen()s librexgpu-xenos.so by path from the native library directory.
            useLegacyPackaging = true
        }
    }

    // The .so files are compressed in the APK (a smaller download): the native ones are extracted at install
    // (useLegacyPackaging) and the Turnip drivers in assets are copied out once per installed APK (GameActivity).
    // The encrypted game does not compress: stored as is.
    androidResources {
        noCompress += listOf("zdat")
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
    // Touch pad glue that talks to SDL's controller manager (absent without the SDK).
    android.sourceSets.getByName("main").java.filter.exclude("org/libsdl/**")
    android.sourceSets.getByName("main").java.filter.exclude("com/dfdx047/dragonblast/game/HelperEngine.kt")
    android.sourceSets.getByName("main").java.filter.exclude("com/dfdx047/dragonblast/game/TouchOverlay.kt")
    android.sourceSets.getByName("main").java.filter.exclude("com/dfdx047/dragonblast/game/GameMenu.kt")
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
