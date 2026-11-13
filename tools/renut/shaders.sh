#!/usr/bin/env bash
# Renut shaders for Raging Blast 2, made on the PC from the player's own game (from Dragon Burst's
# tools/renut-shaders.sh):
#
#   disc data (RB2_GAME, default build/game) -> tools/renut/extract_rb2_shaders.py (Xenos shader containers)
#     -> XenosRecomp (RENUT_RECOMP) + DXC Linux
#     -> build/renut/out/shader_cache.cpp        (the game's shaders as SPIR-V: game data, never in git)
#   the Renut's own shaders (gpu/shaders/host/*.hlsl) -> DXC
#     -> build/renut/out/host_shaders/<name>.spirv.h
#
# android/native/cmake/renut.cmake compiles these into libmain.so. The PC tools (XenosRecomp) are built with the host
# compiler in build/renut/host, never with the NDK; DXC is the official Linux release (pinned below).
# Shaders the game builds at run time can be captured on the device (files/shader_capture/) and copied to
# build/renut/captured/; they are added too.
#
# Usage (from the repository root):
#   tools/renut/shaders.sh              # only when an input changed
#   tools/renut/shaders.sh --force      # always
#   tools/renut/shaders.sh --host-only  # only the Renut's own shaders (and an empty game cache if there is none)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
export TMPDIR="${TMPDIR:-$ROOT/build/tmp}"
mkdir -p "$TMPDIR"

MGRNX="build/mgr-nx-deko"
XR="$MGRNX/app/thirdparty/reblue-XenosRecomp"
HOST_HLSL="$MGRNX/app/src/renut_engine/gpu/shaders/host"
GAME="${RB2_GAME:-build/game}"
CAPTURED="build/renut/captured"
OUT="build/renut/out"
HOST="build/renut/host"
MODE="${1:-}"

DXC_TAG="v1.9.2609"
DXC_TGZ="linux_dxc_2026_09_28.x86_x64.tar.gz"
DXC_SHA256="96faadc7f5c282d2ffda49804beb4c3ee38127bc252b723234e3c5cdf7aa39a1"

if [ ! -f "$XR/XenosRecomp/main.cpp" ]; then
  echo "ERROR: $MGRNX is empty. Run tools/android/setup_renut.sh" >&2
  exit 1
fi
mkdir -p "$OUT" "$HOST"

# DXC (official Linux release, pinned).
if [ ! -x "$HOST/dxc/bin/dxc" ]; then
  echo "DXC $DXC_TAG -> $HOST/dxc"
  mkdir -p "$HOST/dxc-dl" "$HOST/dxc"
  if [ ! -f "$HOST/dxc-dl/$DXC_TGZ" ]; then
    curl -fL -o "$HOST/dxc-dl/$DXC_TGZ" \
      "https://github.com/microsoft/DirectXShaderCompiler/releases/download/$DXC_TAG/$DXC_TGZ"
  fi
  echo "$DXC_SHA256  $HOST/dxc-dl/$DXC_TGZ" | sha256sum -c --quiet
  tar xzf "$HOST/dxc-dl/$DXC_TGZ" -C "$HOST/dxc"
fi
DXC="$ROOT/$HOST/dxc/bin/dxc"

# The Renut's own shaders (as gpu.cmake's renut_host_shader: SPIR-V, D3D layout, Y flipped in the VS).
mkdir -p "$OUT/host_shaders"
for SP in overlay_vs:vs_6_0 overlay_ps:ps_6_0 present_vs:vs_6_0 present_ps:ps_6_0 \
          depth_resolve_ps:ps_6_0 depth_resolve_ms_ps:ps_6_0 texture_mip_ps:ps_6_0 resolve_copy_ps:ps_6_0 \
          rectangle_list_gs:gs_6_0 point_sprite_gs:gs_6_0 point_sprite_ubo_gs:gs_6_0 stream_convert_cs:cs_6_0 \
          target_readback_cs:cs_6_0 bc_decode_cs:cs_6_0 \
          present_easu_ps:ps_6_0 present_rcas_ps:ps_6_0 present_cas_ps:ps_6_0 frame_interp_ps:ps_6_0; do
  S="${SP%%:*}"; P="${SP##*:}"
  EXTRA=()
  HV=2021
  [[ "$P" == vs* ]] && EXTRA=(-fvk-invert-y)
  # FSR 1 and CAS (renut/0019): AMD's headers (MIT) predate HLSL 2021.
  [[ "$S" == present_easu_ps || "$S" == present_rcas_ps || "$S" == present_cas_ps ]] &&
    HV=2018 && EXTRA+=(-I "$ROOT/third_party/fidelityfx")
  "$DXC" -T "$P" -E main -HV "$HV" -all-resources-bound -Wno-ignored-attributes -spirv -fvk-use-dx-layout \
    "${EXTRA[@]}" -Fh "$OUT/host_shaders/$S.spirv.h" -Vn "g_${S}_spirv" "$HOST_HLSL/$S.hlsl"
done
echo "  $(ls "$OUT/host_shaders" | wc -l) host shaders"

# PC tools.
GEN=()
command -v ninja > /dev/null && GEN=(-G Ninja)
CC_HOST="$(command -v clang || command -v gcc)"
CXX_HOST="$(command -v clang++ || command -v g++)"
cmake -S tools/renut/host -B "$HOST/build" "${GEN[@]}" -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_C_COMPILER="$CC_HOST" -DCMAKE_CXX_COMPILER="$CXX_HOST" -DDXC_ROOT="$ROOT/$HOST/dxc" \
  -Wno-dev > "$HOST/configure.log" 2>&1 || { cat "$HOST/configure.log"; exit 1; }
cmake --build "$HOST/build" --target XenosRecomp > "$HOST/build.log" 2>&1 \
  || { tail -40 "$HOST/build.log"; exit 1; }

if [ "$MODE" = "--host-only" ]; then
  if [ ! -f "$OUT/shader_cache.cpp" ]; then
    mkdir -p "$OUT/containers-empty"
    "$HOST/build/xr/XenosRecomp" "$OUT/containers-empty" "$OUT/shader_cache.cpp" "$XR/XenosRecomp/shader_common.h" \
      > "$OUT/xenosrecomp.log" 2>&1 || { tail -20 "$OUT/xenosrecomp.log"; exit 1; }
    rmdir "$OUT/containers-empty"
    echo "  empty game shader cache (no game shaders yet)"
  fi
  exit 0
fi

STAMP="$( {
  find "$GAME" -maxdepth 3 -type f -printf '%P %s %T@\n' 2>/dev/null | sort
  cat "$CAPTURED"/*.bin 2>/dev/null || true
  git -C "$MGRNX" rev-parse HEAD
  cat patches/renut/*.patch 2>/dev/null || true
  cat "$0" tools/renut/host/CMakeLists.txt tools/renut/extract_rb2_shaders.py
} | sha1sum | cut -d' ' -f1)"
if [ "$MODE" != "--force" ] && [ -f "$OUT/shader_cache.cpp" ] && [ "$(cat "$OUT/.stamp" 2>/dev/null)" = "$STAMP" ]; then
  echo "Renut shaders: nothing changed, skipped"
  exit 0
fi

# 1. The game's shader containers.
rm -rf "$OUT/containers"
python3 -I tools/renut/extract_rb2_shaders.py "$GAME" "$OUT/containers"
if compgen -G "$CAPTURED/*.bin" > /dev/null; then
  cp "$CAPTURED"/*.bin "$OUT/containers/"
  echo "  $(ls "$CAPTURED"/*.bin | wc -l) captured on the device ($CAPTURED)"
fi

# 2. Translation.
TMP="$OUT/shader_cache.cpp.tmp"
if ! "$HOST/build/xr/XenosRecomp" "$OUT/containers" "$TMP" "$XR/XenosRecomp/shader_common.h" \
     > "$OUT/xenosrecomp.log" 2>&1; then
  grep -v '^Recompiling shaders' "$OUT/xenosrecomp.log" | tail -40
  exit 1
fi
grep -v '^Recompiling shaders' "$OUT/xenosrecomp.log" | sed 's/^/  /' | tail -20
mv "$TMP" "$OUT/shader_cache.cpp"
echo "  $(grep -o 'g_shaderCacheEntryCount = [0-9]*' "$OUT/shader_cache.cpp")"
echo "$STAMP" > "$OUT/.stamp"
echo "Renut shaders ready in $OUT"
