# Building Dragon Blast

You need your own dump of Dragon Ball: Raging Blast 2 (Xbox 360, USA Rev 1). Its `default.xex` must be 7 151 616 bytes
with SHA-256 `d0291e64…e989c`. Nothing from the disc goes into git, and the APK carries the recompiled code only in
encrypted form (see *The encrypted game library* below).

Host: Linux x86-64, Android NDK 28.2, CMake ≥ 3.31, Ninja, Clang, Python 3, JDK 17. Everything a build generates stays
in `build/` (ignored by git). Use `TMPDIR=$PWD/build/tmp`.

## Steps

1. **SDK** — `tools/android/prepare_sdk.sh` → `build/rexglue-sdk-android`: ReXGlue v0.10.0, the pinned third-party
   Android base and `patches/rexglue-sdk-android/*.patch`. It needs `spirv-cross`; without a system package, build tag
   `vulkan-sdk-1.4.304.0` into `build/third_party/spirv-cross-build` and the script finds it.
2. **Host recompiler** — `tools/android/build_host_cli.sh`.
3. **Disc** — `python3 tools/android/xdvdfs.py <your.iso> build/game`, then `tools/android/codegen.sh build/game` →
   `android/codegen/`. Codegen only reads `default.xex`; the rest of the disc can leave the PC afterwards.
4. **Renut** (the default renderer; without it the build has only Xenia):
   - `tools/android/setup_renut.sh` (`build/mgr-nx-deko` plus `patches/renut`).
   - `python3 tools/android/xdvdfs.py <iso> build/game default.xex data/X360/st_pack_boot_pt_x360.afs
     data/X360/st_pack_battle_map_pt_x360.afs`.
   - `tools/renut/shaders.sh` (DXC and XenosRecomp in `build/renut/host`, shaders in `build/renut/out`).

   A change to the Renut tree must become a new patch in `patches/renut`: CMake refuses a hand-edited tree.
5. **Native libraries** — `tools/android/build_native.sh` builds the Armv8.2 set, and
   `THPS_MARCH=armv8-a tools/android/build_native.sh` the Armv8.0 one (CPUs without LSE atomics, e.g. Snapdragon 665).
   Each run strips the libraries, builds `libxeximage.so` and packs the encrypted game library from
   `RB2_XEX` (default `build/game/default.xex`).
6. **APK** — `tools/android/build_apk.sh` assembles the universal APK from both sets →
   `android/app/build/outputs/apk/release/app-release.apk`.
7. **Check** — `python3 tools/check_clean.py` must print `CLEAN` before anything is published. It scans the APK, the
   repository and its history for game code.

Launcher only (no SDK next to the repo): `cd android && ./gradlew assembleRelease`.

## The encrypted game library

- **Packing.** `libgame.so` (the recompiled game plus the translated shaders) is packed by
  `tools/android/pack_libgame.py` into `assets/libgame{,_v80}.zdat`: AES-256-GCM in 1 MiB chunks.
- **Key.** SHA-256 of the decrypted PE image of the player's `default.xex`, rebuilt on the device by `libxeximage.so`.
- **First Play.** The launcher decrypts the right set into the app's private storage (`GameLib.kt`, stamp `libgame.id`)
  and does it again only when an update brings another build.
- **Engine choice.** `CpuSet.kt` picks the Armv8.2 or Armv8.0 set from AT_HWCAP. For a test, an empty
  `files/force_armv80` runs the Armv8.0 set on any CPU.

## Signing and versions

- **Signing.** Release builds sign with `android/dragonblast-release.jks` through `android/keystore.properties`. Both
  are ignored by git; keep a backup, because updates only install over an APK signed with the same key. Without
  `keystore.properties` the build signs with the debug key.
- **Version.** `android/version.properties` holds `versionCode` and `versionName`. Raise both for every release. The
  in-app updater reads the latest GitHub release and installs its `.apk` asset when the tag (`vX.Y.Z`) is newer.

## Turnip in personal builds

Release APKs carry no Vulkan driver; players download one in the Drivers tab. `tools/android/fetch_turnip.sh` puts
Mesa Turnip into `assets/drivers`, and the Automatic driver then uses it on Adreno 6xx–8xx.

## Installing

`adb install -r android/app/build/outputs/apk/release/app-release.apk`. Then import the ISO in the app, or copy the
extracted disc to `Android/data/com.dfdx047.dragonblast/files/game/`. Extra launch options go one per line in
`files/args.txt`, and live renderer options go in `files/live_cvars.txt`.
