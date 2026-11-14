#!/usr/bin/env bash
# Build the Android native libraries and stage them into the Gradle project's jniLibs.
#
#   tools/android/build_native.sh                 full build (needs prepare_sdk.sh + codegen.sh first)
#   PROBE_ONLY=1 tools/android/build_native.sh    just the Vulkan probe (no SDK, no game, a minute)
#
# Environment: ANDROID_HOME (default ~/android-sdk), BUILD_TYPE (default RelWithDebInfo: the SDK
# compiles its perf counters, and so the frame-time CSV, out of Release builds), JOBS, STRIP=1,
# OPT_FLAGS (RelWithDebInfo compiler flags, default "-O3 -mtune=cortex-a715 -g -DNDEBUG": -1.5 % CP time, -0.25 W vs -O2),
# THPS_MARCH (default armv8.2-a; armv8-a = the Armv8.0 engine for phones without LSE atomics such as the
# Snapdragon 665 of the Mi A3: own build folder, tuned for the Cortex-A73).
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
NDK_VERSION=28.2.13676358
CMAKE_VERSION=3.31.6
NDK="$ANDROID_HOME/ndk/$NDK_VERSION"
CMAKE="$ANDROID_HOME/cmake/$CMAKE_VERSION/bin/cmake"
NINJA="$ANDROID_HOME/cmake/$CMAKE_VERSION/bin/ninja"
BUILD_TYPE="${BUILD_TYPE:-RelWithDebInfo}"
JOBS="${JOBS:-$(nproc)}"
[ -d "$NDK" ] || { echo "NDK $NDK_VERSION not found in $ANDROID_HOME"; exit 1; }
# No SDK-managed CMake 3.31.6: use the system one (CMake 4 needs the policy floor for old third-party lists).
if [ ! -x "$CMAKE" ]; then
  CMAKE="$(command -v cmake)"; NINJA="$(command -v ninja)"
  export CMAKE_POLICY_VERSION_MINIMUM=3.5
fi

EXTRA=()
if [ "${PROBE_ONLY:-0}" = 1 ]; then
  OUT="$REPO/android/native-build/probe"
  EXTRA+=(-DTHPS_GEN_DIR=/nonexistent)
else
  OUT="$REPO/android/native-build/$BUILD_TYPE"
fi
LIBS="$REPO/android/app/src/main/jniLibs/arm64-v8a"
MARCH="${THPS_MARCH:-armv8.2-a}"
TUNE=cortex-a715
SUFFIX=""
if [ "$MARCH" != armv8.2-a ]; then
  SUFFIX="-$MARCH"
  OUT="$OUT$SUFFIX"
  TUNE=cortex-a73
  LIBS="$REPO/android/native-build/jniLibs$SUFFIX/arm64-v8a"   # build_apk.sh JNILIBS=... packages it
fi
DEFAULT_OPT="-O3 -mtune=$TUNE -g -DNDEBUG"

"$CMAKE" -S "$REPO/android/native" -B "$OUT" -G Ninja \
  -DCMAKE_MAKE_PROGRAM="$NINJA" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DANDROID_STL=c++_shared \
  -DCMAKE_BUILD_TYPE="$BUILD_TYPE" -DTHPS_MARCH="$MARCH" -DREXGLUE_ANDROID_MARCH="$MARCH" -DREXGLUE_OUT_SUFFIX="$SUFFIX" \
  -DCMAKE_C_FLAGS_RELWITHDEBINFO="${OPT_FLAGS:-$DEFAULT_OPT}" -DCMAKE_CXX_FLAGS_RELWITHDEBINFO="${OPT_FLAGS:-$DEFAULT_OPT}" "${EXTRA[@]}"
"$CMAKE" --build "$OUT" --parallel "$JOBS"

rm -rf "$LIBS"; mkdir -p "$LIBS"
# The SDK suffixes its libraries with the build config (RelWithDebInfo -> "rd", Release -> none), and
# both the runtime and the GPU plugin look each other up with that suffix, so keep the names as built.
# The SDK also writes its libraries into its own tree (out/android-arm64), not into $OUT.
SDK_OUT="${THPS_SDK:-$REPO/build/rexglue-sdk-android}/out/android-arm64$SUFFIX"
stage() { # glob
  local f; f="$(find "$OUT" "$SDK_OUT" -name "$1" -type f -newer "$OUT/CMakeCache.txt" 2>/dev/null | head -1)"
  [ -n "$f" ] || f="$(find "$OUT" "$SDK_OUT" -name "$1" -type f 2>/dev/null | head -1)"
  [ -n "$f" ] || { echo "missing build output: $1"; exit 1; }
  cp "$f" "$LIBS/"
  echo "  staged $(basename "$f") ($(du -h "$f" | cut -f1))"
}
stage librb2probe.so
if [ "${PROBE_ONLY:-0}" != 1 ]; then
  stage libmain.so
  stage "librexruntime*.so"
  stage "librexgpu-xenos*.so"
  # libadrenotools hooks (custom Vulkan driver support); only present when it was fetched.
  for hook in libmain_hook.so libhook_impl.so libfile_redirect_hook.so libgsl_alloc_hook.so; do
    if [ -n "$(find "$OUT" -name "$hook" -type f 2>/dev/null | head -1)" ]; then stage "$hook"; fi
  done
fi
cp "$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" "$LIBS/"
if [ "${STRIP:-0}" = 1 ]; then
  for so in "$LIBS"/*.so; do "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$so"; done
fi
echo "[ok] native libraries staged in $LIBS"
