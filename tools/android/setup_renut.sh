#!/usr/bin/env bash
# Prepares the Renut (mgr-nx-deko's native Direct3D renderer) for the Dragon Blast build:
#   build/mgr-nx-deko = upstream tuancapoo/mgr-nx-deko at the pinned commit + patches/renut (in order).
# Then tools/renut/shaders.sh makes the shaders from the player's disc (build/game) and the native build
# (tools/android/build_native.sh) picks the Renut up by itself (android/native/cmake/renut.cmake).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SUB="$ROOT/build/mgr-nx-deko"
URL="https://github.com/tuancapoo/mgr-nx-deko"
COMMIT="ba1a837"  # v0.2.2 (Dragon Burst's external/mgr-nx-deko)

if [ ! -d "$SUB/.git" ]; then
  git clone "$URL" "$SUB"
fi
git -C "$SUB" fetch --quiet origin || true
git -C "$SUB" checkout --quiet "$COMMIT"
bash "$ROOT/tools/android/apply_renut_patches.sh"
echo "Renut ready in $SUB. Next: tools/renut/shaders.sh (needs the disc in build/game)"
