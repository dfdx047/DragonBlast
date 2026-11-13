#!/usr/bin/env bash
# Manifest -> codegen -> native libraries -> APK, copied to build/apk/DragonBlast-<tag>.apk.
#
#   tools/android/rebuild.sh <tag> [mkmanifest flags, e.g. --no-file-crt]
#
# Game files: RB2_GAME = the folder holding YOUR extracted disc (default.xex, data/, nxeart).
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
TAG="${1:?usage: rebuild.sh <tag> [mkmanifest flags]}"; shift
GAME="${RB2_GAME:?set RB2_GAME to the folder with your extracted disc (default.xex)}"
cd "$REPO"
LOGS="$REPO/build/logs"; mkdir -p "$LOGS"
python3 tools/android/mkmanifest.py rb2_config.toml config/rb2_manifest.toml "$@"
tools/android/codegen.sh "$GAME" > "$LOGS"/rb2_codegen.log 2>&1 || { tail -20 "$LOGS"/rb2_codegen.log; exit 1; }
echo "unresolved branches: $(grep -c Unresolved "$LOGS"/rb2_codegen.log || true)"
tools/android/build_native.sh > "$LOGS"/rb2_native.log 2>&1 || { grep -m5 -B2 -A8 "error:" "$LOGS"/rb2_native.log; tail -5 "$LOGS"/rb2_native.log; exit 1; }
tools/android/build_apk.sh > "$LOGS"/rb2_apk.log 2>&1 || { tail -30 "$LOGS"/rb2_apk.log; exit 1; }
mkdir -p build/apk
cp android/app/build/outputs/apk/release/app-release.apk "build/apk/DragonBlast-$TAG.apk"
echo "built build/apk/DragonBlast-$TAG.apk"
