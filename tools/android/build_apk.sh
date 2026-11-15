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

# The universal APK: both native sets in one lib/arm64-v8a. The Armv8.0 set (build_native.sh with THPS_MARCH=armv8-a)
# brings its own runtime and GPU plugin (librexruntime_v80*.so, librexgpu-xenos_v80*.so) and the libraries both sets
# share (xex reader, Vulkan probe, adrenotools hooks, libc++), all built for Armv8.0; the Armv8.2 set adds its runtime
# and GPU plugin. Each set's game is in assets, encrypted (libgame.zdat, libgame_v80.zdat). JNILIBS=<dir> packages one
# set only (that set's assets: GAME_ASSETS=<dir>).
NB="$REPO/android/native-build"
if [ -z "${JNILIBS:-}" ]; then
  V82="$REPO/android/app/src/main/jniLibs/arm64-v8a"; V80="$NB/jniLibs-armv8-a/arm64-v8a"
  for d in "$V82" "$V80" "$NB/assets" "$NB/assets-armv8-a"; do
    [ -d "$d" ] || { echo "missing $d: run build_native.sh (and THPS_MARCH=armv8-a build_native.sh) first"; exit 1; }
  done
  U="$NB/universal"; rm -rf "$U"; mkdir -p "$U/jni/arm64-v8a" "$U/assets"
  cp "$V80"/*.so "$U/jni/arm64-v8a/"
  cp "$V82"/librexruntime*.so "$V82"/librexgpu-xenos*.so "$U/jni/arm64-v8a/"
  cp "$NB/assets"/* "$NB/assets-armv8-a"/* "$U/assets/"
  JNI="$U/jni"; GAME_ASSETS="$U/assets"
  echo "universal APK: $(ls "$U/jni/arm64-v8a" | tr '\n' ' ')"
else
  JNI="$JNILIBS"
  case "$JNI" in /*|app/*) ;; *) JNI="$REPO/$JNI" ;; esac   # relative to the repository
  GAME_ASSETS="${GAME_ASSETS:-$NB/assets}"
fi
ls "$JNI"/arm64-v8a/*.so >/dev/null 2>&1 || { echo "no native libraries staged in $JNI: run build_native.sh first"; exit 1; }
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}" ./gradlew --no-daemon -Prb2.sdk="$SDK_TREE" \
  -Prb2.jniLibs="$(realpath "$JNI")" -Prb2.gameAssets="$(realpath "$GAME_ASSETS")" assembleRelease "$@"
APK=app/build/outputs/apk/release/app-release.apk
ls -la "$APK"
