# RB2 Recomp for Android (arm64-v8a): native libraries.
#
# Driven by tools/android/build_native.sh, which stages the results into the Gradle project's
# jniLibs. Builds:
#
#   librb2probe.so   standalone Vulkan capability probe (no SDK, no game)
#   libxeximage.so     the default.xex PE image reader (the launcher derives the libgame key with it)
#   librexruntime.so   the patched ReXGlue SDK runtime (SDL3 is linked statically inside it)
#   librexgpu-xenos.so Xbox 360 GPU -> Vulkan plugin (dlopen'd by the runtime)
#   libgame.so         the recompiled game + the app layer (this directory) + the title's hooks + the Renut. Game
#                      data: build_native.sh encrypts it (tools/android/pack_libgame.py) and the APK only carries
#                      assets/libgame.zdat; the launcher decrypts it with the player's default.xex.
#
# Universal APK: the Armv8.0 set (THPS_MARCH=armv8-a) names its runtime and GPU plugin with THPS_LIB_SUFFIX
# ("_v80"), so both sets fit in one lib/arm64-v8a; the launcher picks one by the CPU's HWCAP (CpuSet.kt).
#
# The game libraries need the SDK tree (tools/android/prepare_sdk.sh) and the generated sources
# (tools/android/codegen.sh). Without generated sources only the probe is built.

cmake_minimum_required(VERSION 3.25)
project(rb2_android LANGUAGES C CXX ASM)

get_filename_component(THPS_REPO "${CMAKE_CURRENT_SOURCE_DIR}/../.." ABSOLUTE)
set(THPS_SDK "${THPS_REPO}/build/rexglue-sdk-android" CACHE PATH "Android-prepared ReXGlue SDK tree")
set(THPS_GEN_DIR "${THPS_REPO}/android/codegen/generated-android/default" CACHE PATH
    "Output of tools/android/codegen.sh")
# Each generated file is a huge translation unit; compiling too many at once exhausts RAM.
set(THPS_RECOMP_JOBS "6" CACHE STRING "Generated files compiled in parallel")
# Inline (LSE) atomics instead of load/store-exclusive loops. The Thor's Snapdragon 8 Gen 2 cores
# are all Armv9, so Armv8.2 is safe; guest code does atomics constantly.
set(THPS_MARCH "armv8.2-a" CACHE STRING "-march for the generated game code")
set(THPS_LIB_SUFFIX "" CACHE STRING "Suffix of this set's runtime and GPU plugin (\"_v80\" for Armv8.0)")

# ---------------------------------------------------------------------------------------------
# Vulkan probe: always built, depends on nothing but the NDK.
# ---------------------------------------------------------------------------------------------
add_library(rb2probe SHARED src/vulkan_probe.cpp)
target_link_libraries(rb2probe PRIVATE vulkan log)
set_target_properties(rb2probe PROPERTIES CXX_STANDARD 20 CXX_STANDARD_REQUIRED ON)

# default.xex PE image reader for the launcher (key of the encrypted libgame): the SDK's lzxd and AES sources only.
set(THPS_SDK_THIRD "${THPS_SDK}/thirdparty")
if(EXISTS "${THPS_SDK_THIRD}/libmspack/cabextract/mspack/lzxd.c")
    add_library(xeximage SHARED src/loader/xex_image.cpp src/loader/xex_image_jni.cpp
        "${THPS_SDK_THIRD}/libmspack/cabextract/mspack/lzxd.c")
    target_include_directories(xeximage PRIVATE src/loader "${THPS_SDK_THIRD}"
        "${THPS_SDK_THIRD}/libmspack/cabextract/mspack")
    target_compile_options(xeximage PRIVATE -w)
    target_link_libraries(xeximage PRIVATE log)
    set_target_properties(xeximage PROPERTIES CXX_STANDARD 20 CXX_STANDARD_REQUIRED ON)
endif()

if(NOT EXISTS "${THPS_GEN_DIR}/sources.cmake")
    message(WARNING "No generated game code in ${THPS_GEN_DIR}; building the Vulkan probe only.")
    return()
endif()
if(NOT EXISTS "${THPS_SDK}/.android-patches/.applied")
    message(FATAL_ERROR "SDK tree ${THPS_SDK} is not prepared. Run tools/android/prepare_sdk.sh")
endif()

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Tracy is a separate .so that does nothing in an APK; tests are desktop-only. Android has no D3D12.
set(REXGLUE_ENABLE_TRACY OFF CACHE BOOL "" FORCE)
set(REXGLUE_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(REXGLUE_USE_VULKAN ON CACHE BOOL "" FORCE)
set(REXGLUE_USE_D3D12 OFF CACHE BOOL "" FORCE)

# Inline LSE atomics for the SDK runtime and GPU plugin too: at plain Armv8-A the compiler calls the
# out-of-line __aarch64_cas*/ldadd* helpers, which showed up at several percent of the GPU-command
# and UI threads in the first on-device profile.
string(APPEND CMAKE_C_FLAGS " -march=${THPS_MARCH}")
string(APPEND CMAKE_CXX_FLAGS " -march=${THPS_MARCH}")

add_subdirectory("${THPS_SDK}" rexglue-sdk)
# The runtime loads the GPU plugin of its own set (SDK patch 0046). Only on that target: a global definition would
# change every target's flags, the generated game code included.
if(THPS_LIB_SUFFIX)
    target_compile_definitions(rexruntime PRIVATE REX_GPU_PLUGIN_SUFFIX="${THPS_LIB_SUFFIX}")
endif()
if(THPS_LIB_SUFFIX)
    set_target_properties(rexruntime PROPERTIES OUTPUT_NAME "rexruntime${THPS_LIB_SUFFIX}")
    set_target_properties(rexgpu-xenos PROPERTIES OUTPUT_NAME "rexgpu-xenos${THPS_LIB_SUFFIX}")
endif()

# Headers the app and generated code need beyond rex::runtime (rex_app.h pulls in imgui, the Vulkan
# stack, ...). Take include paths only: never link rexui/rexcore here, they are OBJECT libraries and
# linking them would duplicate their global state (the cvar registry) inside libgame.so.
add_library(thps_headers INTERFACE)
target_include_directories(thps_headers INTERFACE
    $<TARGET_PROPERTY:imgui,INTERFACE_INCLUDE_DIRECTORIES>)
target_link_libraries(thps_headers INTERFACE
    renderdoc Vulkan::Headers GPUOpen::VulkanMemoryAllocator spirv-tools-headers)

# ---------------------------------------------------------------------------------------------
# The recompiled game (generated). An OBJECT library, like the desktop build: every translation
# unit is linked into libgame.so, including the weak aliases the title's hooks override.
# ---------------------------------------------------------------------------------------------
include("${THPS_GEN_DIR}/sources.cmake")   # defines GENERATED_SOURCES
add_library(rb2_recomp_objs OBJECT ${GENERATED_SOURCES})
target_include_directories(rb2_recomp_objs PRIVATE "${THPS_GEN_DIR}")
target_link_libraries(rb2_recomp_objs PRIVATE rex::runtime thps_headers)
rexglue_apply_target_settings(rb2_recomp_objs)
# After the SDK's own -march=armv8-a so this one wins (last -march on the command line is used).
target_compile_options(rb2_recomp_objs PRIVATE -march=${THPS_MARCH} -gline-tables-only -Wno-unused-variable)
target_precompile_headers(rb2_recomp_objs PRIVATE "${THPS_GEN_DIR}/rb2_pch.h")
set_property(GLOBAL APPEND PROPERTY JOB_POOLS recomp=${THPS_RECOMP_JOBS})
set_target_properties(rb2_recomp_objs PROPERTIES JOB_POOL_COMPILE recomp)

# ---------------------------------------------------------------------------------------------
# libgame.so (target "main"): SDL_main entry, the title's app class (src/game/src, unchanged) and the hooks.
# ---------------------------------------------------------------------------------------------
add_library(main SHARED
    src/android_main.cpp
    src/rb2_android_app.cpp
    src/thread_affinity.cpp
    src/display_vsync.cpp
    src/live_settings.cpp
    src/game_state_jni.cpp
    src/bt3_controls.cpp
    src/pad_remap.cpp
    src/custom_driver.cpp
    "${THPS_SDK}/src/ui/rex_app.cpp"
    $<TARGET_OBJECTS:rb2_recomp_objs>)
target_include_directories(main PRIVATE
    
    "${THPS_REPO}/src"                # rb2_app.h and the title's hook headers
    "${THPS_GEN_DIR}"                 # rb2_init.h
    "${THPS_SDK}/include"
    "${THPS_SDK}/src/ui")
target_link_libraries(main PRIVATE rex::runtime thps_headers android log)
set_target_properties(main PROPERTIES OUTPUT_NAME game)
target_compile_definitions(main PRIVATE REXGLUE_BUILD_CONFIG="$<CONFIG>")
target_compile_options(main PRIVATE -march=${THPS_MARCH})
# Optional custom Vulkan driver loader (libadrenotools, BSD-2-Clause; fetched by
# tools/android/fetch_adrenotools.sh into build/third_party, never committed).
set(THPS_ADRENOTOOLS "${THPS_REPO}/build/third_party/libadrenotools" CACHE PATH "libadrenotools checkout")
if(EXISTS "${THPS_ADRENOTOOLS}/CMakeLists.txt")
    add_subdirectory("${THPS_ADRENOTOOLS}" adrenotools)
    target_link_libraries(main PRIVATE adrenotools)
    target_compile_definitions(main PRIVATE THPS_HAVE_ADRENOTOOLS=1)
    add_dependencies(main main_hook hook_impl file_redirect_hook gsl_alloc_hook)
endif()

# The Renut (mgr-nx-deko's native Direct3D renderer, swordman_renderer = renut): only when its tree is prepared
# (tools/android/setup_renut.sh) and the shaders are made (tools/renut/shaders.sh). Game data inside: personal builds.
set(THPS_RENUT AUTO CACHE STRING "Build the Renut renderer (ON, OFF, AUTO = when build/mgr-nx-deko exists)")
if(THPS_RENUT STREQUAL "ON" OR (THPS_RENUT STREQUAL "AUTO" AND EXISTS "${THPS_REPO}/build/mgr-nx-deko/app"))
    include("${CMAKE_CURRENT_SOURCE_DIR}/cmake/renut.cmake")
    db_renut_setup(main)
    target_include_directories(main PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/src/renut")
    message(STATUS "Renut: on")
endif()

# The runtime resolves PPCImageConfig, the function table and the app creator from this library.
target_link_options(main PRIVATE -Wl,--export-dynamic)

# The GPU plugin is dlopen'd, not linked.
add_dependencies(main rexgpu-xenos)
