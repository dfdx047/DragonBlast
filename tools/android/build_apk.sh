#!/usr/bin/env bash
# Package the staged native libraries (tools/android/build_native.sh) into an APK.
#
#   tools/android/build_apk.sh        -> android/app/build/outputs/apk/release/app-release.apk
#
# Extra arguments go to Gradle (e.g. -Papp.versionCode=100 -Papp.versionName=1.0.0); set the RELEASE_*
# signing variables (see android/app/build.gradle.kts) to sign with a release key.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
SDK_TREE="${THPS_SDK:-$REPO/build/rexglue-sdk-android}"
cd "$REPO/android"

# The Gradle wrapper is not committed (it is a binary); take SDL3's, which pins the Gradle version
# its Android project is known to build with.
SDL_PROJECT="$SDK_TREE/thirdparty/sdl3/android-project"
if [ ! -x gradlew ]; then
  cp "$SDL_PROJECT/gradlew" gradlew; chmod +x gradlew
  mkdir -p gradle/wrapper
  cp "$SDL_PROJECT/gradle/wrapper/gradle-wrapper.jar" "$SDL_PROJECT/gradle/wrapper/gradle-wrapper.properties" gradle/wrapper/
fi
echo "sdk.dir=$ANDROID_HOME" > local.properties

# JNILIBS=<dir with arm64-v8a/>: package another engine (the Armv8.0 one: android/native-build/jniLibs-armv8-a).
JNI="${JNILIBS:-app/src/main/jniLibs}"
case "$JNI" in /*|app/*) ;; *) JNI="$REPO/$JNI" ;; esac   # relative to the repository
ls "$JNI"/arm64-v8a/*.so >/dev/null 2>&1 || { echo "no native libraries staged in $JNI: run build_native.sh first"; exit 1; }
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}" ./gradlew --no-daemon -Prb2.sdk="$SDK_TREE" -Prb2.jniLibs="$(realpath "$JNI")" assembleRelease "$@"
APK=app/build/outputs/apk/release/app-release.apk
ls -la "$APK"
