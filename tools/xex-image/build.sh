#!/usr/bin/env bash
# Builds tools/xex-image (PC) into build/host/xex-image when its sources changed. Same reader as the device
# (android/native/src/loader/xex_image.cpp): the libgame key is the SHA-256 of the default.xex PE image.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SDK="${THPS_SDK:-$ROOT/build/rexglue-sdk-android}/thirdparty"
OUT="$ROOT/build/host/xex-image"
SRC=("$ROOT/tools/xex-image/main.cpp" "$ROOT/android/native/src/loader/xex_image.cpp" "$ROOT/android/native/src/loader/xex_image.h")
if [ -x "$OUT" ] && [ -z "$(find "${SRC[@]}" -newer "$OUT")" ]; then exit 0; fi
mkdir -p "$(dirname "$OUT")" "$ROOT/build/tmp"
T=$(mktemp -d -p "$ROOT/build/tmp"); trap 'rm -rf "$T"' EXIT
cc -O2 -w -c "$SDK/libmspack/cabextract/mspack/lzxd.c" -I"$SDK/libmspack/cabextract/mspack" -o "$T/lzxd.o"
c++ -O2 -std=c++20 -w "$ROOT/tools/xex-image/main.cpp" "$ROOT/android/native/src/loader/xex_image.cpp" "$T/lzxd.o" \
  -I"$ROOT/android/native/src/loader" -I"$SDK" -I"$SDK/libmspack/cabextract/mspack" -o "$OUT"
