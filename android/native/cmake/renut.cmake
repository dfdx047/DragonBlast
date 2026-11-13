# Renut: mgr-nx-deko's native Direct3D renderer (reNut + plume, Vulkan) for Raging Blast 2, chosen at boot with
# swordman_renderer = renut (Xenia stays the default in the same APK). From Dragon Burst's cmake/renut.cmake (itself
# from Zandatsu's), with its patches/renut 0001-0120 and Dragon Blast's 0200+ (Raging Blast 2's layout and hooks).
# Hook map and device layout: docs/renut/HOOKMAP_RB2.md. GPL-3.0 (the Renut and its ports), credits in docs/CREDITS.md.
#
# Shaders: translated on the PC (tools/renut/shaders.sh) into build/renut/out; this file only compiles them.
# An OBJECT library on purpose: the hooks replace the generated weak sub_X definitions, and members of a static
# archive are only pulled in by missing symbols (the hooks would never get in).
#
# Uses: THPS_REPO, THPS_SDK, THPS_MARCH (CMakeLists.txt). Defines: mgr_renut_gpu, db_renut_setup(<target>).

set(SM_ROOT        "${THPS_REPO}")
set(SM_GLUE_DIR    "${THPS_REPO}/android/native/src/renut")
set(SM_MGRNX       "${SM_ROOT}/build/mgr-nx-deko")
set(RENUT_APP_DIR  "${SM_MGRNX}/app")
set(RENUT_GPU_DIR  "${RENUT_APP_DIR}/src/renut_engine/gpu")
set(RENUT_XR_DIR   "${RENUT_APP_DIR}/thirdparty/reblue-XenosRecomp")
set(SM_RENUT_OUT   "${SM_ROOT}/build/renut/out")

if(NOT EXISTS "${RENUT_GPU_DIR}/gpu.cmake")
    message(FATAL_ERROR "Falta ${SM_MGRNX}. Run: tools/android/setup_renut.sh")
endif()
if(NOT EXISTS "${SM_RENUT_OUT}/shader_cache.cpp" OR NOT EXISTS "${SM_RENUT_OUT}/host_shaders/present_ps.spirv.h")
    message(FATAL_ERROR "Faltam os shaders do Renut em ${SM_RENUT_OUT}. Run: tools/renut/shaders.sh")
endif()
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${SM_RENUT_OUT}/.stamp")

# As alteracoes Android ao submodulo (patches/renut) tem de estar aplicadas.
file(GLOB SM_RENUT_PATCHES "${SM_ROOT}/patches/renut/[0-9][0-9][0-9][0-9]-*.patch")
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS ${SM_RENUT_PATCHES})
execute_process(COMMAND bash "${SM_ROOT}/tools/android/apply_renut_patches.sh" --check
                RESULT_VARIABLE SM_RENUT_PATCH_MISSING ERROR_VARIABLE SM_RENUT_PATCH_ERROR)
if(SM_RENUT_PATCH_MISSING)
    message(FATAL_ERROR "Patches do Renut por aplicar: ${SM_RENUT_PATCH_ERROR}")
endif()

# plume (Vulkan; o ramo Android ja existe nele: VK_KHR_android_surface).
if(NOT TARGET plume)
    add_subdirectory("${RENUT_APP_DIR}/thirdparty/plume" "${CMAKE_BINARY_DIR}/plume")
endif()

# Descompressao do arquivo de shaders: zstd (so o descompressor), smol-v, xxHash, das copias do XenosRecomp.
set(RENUT_ZSTD_LIB_DIR "${RENUT_XR_DIR}/thirdparty/zstd/lib")
file(GLOB renut_zstd_sources "${RENUT_ZSTD_LIB_DIR}/common/*.c" "${RENUT_ZSTD_LIB_DIR}/decompress/*.c")
add_library(renut_zstd STATIC ${renut_zstd_sources})
target_include_directories(renut_zstd PUBLIC "${RENUT_ZSTD_LIB_DIR}")
target_compile_definitions(renut_zstd PRIVATE ZSTD_DISABLE_ASM)

set(RENUT_XXHASH_DIR "${RENUT_XR_DIR}/thirdparty/xxHash")
add_library(renut_xxhash STATIC "${RENUT_XXHASH_DIR}/xxhash.c")
target_include_directories(renut_xxhash PUBLIC "${RENUT_XXHASH_DIR}")

set(RENUT_SMOLV_DIR "${RENUT_XR_DIR}/thirdparty/smol-v/source")
add_library(renut_smolv STATIC "${RENUT_SMOLV_DIR}/smolv.cpp")
target_include_directories(renut_smolv PUBLIC "${RENUT_SMOLV_DIR}")

set_target_properties(renut_zstd renut_xxhash renut_smolv PROPERTIES POSITION_INDEPENDENT_CODE ON)

# O o1heap do SDK chama-se o1heap aqui; o Renut procura rex::o1heap.
if(TARGET o1heap AND NOT TARGET rex::o1heap)
    add_library(rex::o1heap ALIAS o1heap)
endif()

add_library(mgr_renut_gpu OBJECT
    # O dispositivo, o frame e a thread de render.
    "${RENUT_GPU_DIR}/backend/backend.cpp"
    "${RENUT_GPU_DIR}/backend/gpu_profile.cpp"
    "${RENUT_GPU_DIR}/backend/memory_report.cpp"
    "${RENUT_GPU_DIR}/backend/native_window.cpp"
    "${RENUT_GPU_DIR}/backend/present.cpp"
    "${RENUT_GPU_DIR}/backend/queued.cpp"
    "${RENUT_GPU_DIR}/backend/render_thread.cpp"
    "${RENUT_GPU_DIR}/backend/video.cpp"
    # O Direct3D do jogo, nativo, e os seus hooks (sem d3d/hooks/state.cpp, como no deles).
    "${RENUT_GPU_DIR}/d3d/d3d_native_state.cpp"
    "${RENUT_GPU_DIR}/d3d/hooks/device.cpp"
    "${RENUT_GPU_DIR}/d3d/hooks/draw.cpp"
    "${RENUT_GPU_DIR}/d3d/hooks/resource.cpp"
    "${RENUT_GPU_DIR}/d3d/hooks/shader.cpp"
    "${RENUT_GPU_DIR}/guest/callbacks.cpp"
    "${RENUT_GPU_DIR}/guest/command_buffers.cpp"
    "${RENUT_GPU_DIR}/guest/guest_records.cpp"
    "${RENUT_GPU_DIR}/guest/sa2_screen.cpp"
    "${RENUT_GPU_DIR}/overlay/app_integration.cpp"
    "${RENUT_GPU_DIR}/overlay/overlay_drawer.cpp"
    "${RENUT_GPU_DIR}/overlay/overlay_ui.cpp"
    "${RENUT_GPU_DIR}/render/draw.cpp"
    "${RENUT_GPU_DIR}/render/format.cpp"
    "${RENUT_GPU_DIR}/render/graphics_options.cpp"
    "${RENUT_GPU_DIR}/render/pipeline.cpp"
    "${RENUT_GPU_DIR}/render/shader_library.cpp"
    "${RENUT_GPU_DIR}/render/surfaces.cpp"
    "${RENUT_GPU_DIR}/render/texture_layout.cpp"
    "${RENUT_GPU_DIR}/render/textures.cpp"
    "${RENUT_GPU_DIR}/render/upload_ring.cpp"
    "${RENUT_GPU_DIR}/render/vertex_format.cpp"
    # Os do mgr_renut_gpu.cmake: escolha, afinacao, verificacoes, ritmo dos frames.
    "${RENUT_GPU_DIR}/banjo_renut_select.cpp"
    "${RENUT_GPU_DIR}/banjo_renut_tuning.cpp"
    "${RENUT_GPU_DIR}/banjo_renut_checks.cpp"
    "${RENUT_GPU_DIR}/banjo_renut_record.cpp"
    "${RENUT_GPU_DIR}/sa2_renut_pacing.cpp"
    "${RENUT_APP_DIR}/src/renut_engine/display_sync.cpp"
    "${RENUT_APP_DIR}/src/renut_engine/perf/frame_stats.cpp"
    "${RENUT_APP_DIR}/src/renut_engine/sa2_compat/frame_latency.cpp"
    "${RENUT_APP_DIR}/src/renut_engine/sa2_compat/sa2_compat.cpp"
    # Marcadores de GPU por draw (swordman_renut_gpu_markers; patch renut/0014 chama-os).
    "${SM_GLUE_DIR}/renut_gpu_markers.cpp"
    # Dump de um frame (swordman_renut_frame_dump; patch renut/0015 chama-o).
    "${SM_GLUE_DIR}/renut_frame_dump.cpp"
    # Sombras e brilho do MGR (swordman_renut_shadows / _bloom; patch renut/0016 chama-o). Escolhe os passes pelos
    # shaders do MGR: no Burst Limit nao reconhece nenhum e fica tudo como o jogo desenha.
    "${SM_GLUE_DIR}/renut_effects.cpp"
    # Fora: mgr_widescreen.cpp (hook do MGR; sem ele a largura fica a 1280) e mgr_audio_trace.cpp (Wwise do MGR).
    # Dragon Blast: what the Renut takes from Dragon Burst's SDK fork (frame rate, draw overrides, frame pacing).
    "${SM_GLUE_DIR}/compat/frame_rate.cpp"
    "${SM_GLUE_DIR}/compat/draw_overrides.cpp"
    "${SM_GLUE_DIR}/compat/frame_pacing.cpp"
    "${SM_GLUE_DIR}/compat/wide_aspect.cpp"
    # Os shaders do jogo traduzidos no PC (dados do jogo: so builds pessoais).
    "${SM_RENUT_OUT}/shader_cache.cpp")
set_source_files_properties("${SM_RENUT_OUT}/shader_cache.cpp" PROPERTIES SKIP_PRECOMPILE_HEADERS ON)

target_include_directories(mgr_renut_gpu BEFORE PRIVATE
    # O xxhash.h do XenosRecomp, a frente do do SDK (outra versao).
    "${RENUT_XXHASH_DIR}"
    "${SM_GLUE_DIR}/compat")
target_include_directories(mgr_renut_gpu PRIVATE
    "${RENUT_APP_DIR}"
    "${RENUT_APP_DIR}/src"
    "${RENUT_APP_DIR}/src/renut_engine/sa2_compat"
    "${RENUT_GPU_DIR}/shaders"
    "${RENUT_XR_DIR}/XenosRecomp"
    # host_shaders/<nome>.spirv.h
    "${SM_RENUT_OUT}"
    "${THPS_SDK}/src/core"
    # renut_gpu_markers.h e companhia, incluidos pelos patches renut/0014-0016.
    "${SM_GLUE_DIR}"
    "${THPS_SDK}/thirdparty/imgui"
    "${CMAKE_BINARY_DIR}/rexglue-sdk/include")
target_compile_definitions(mgr_renut_gpu PRIVATE MGR_RENUT_RENDERER=1 SA2_RENUT_RENDERER=1 RENUT_RECOMP)
target_compile_options(mgr_renut_gpu PRIVATE -march=${THPS_MARCH} -ffp-contract=off -fno-strict-aliasing)
target_link_libraries(mgr_renut_gpu PRIVATE rex::runtime plume renut_zstd renut_smolv renut_xxhash rex::o1heap)

# Liga o Renut ao libgame.so (<alvo>) e diz ao codigo do jogo que ele existe (DB_RENUT).
function(db_renut_setup TARGET)
    # adrenotools: o driver Vulkan do plume (renut_glue.cpp, plume_android_vulkan_library).
    target_link_libraries(${TARGET} PRIVATE mgr_renut_gpu)
    target_compile_definitions(${TARGET} PRIVATE DB_RENUT=1 MGR_RENUT_RENDERER=1 SA2_RENUT_RENDERER=1)
    target_include_directories(${TARGET} PRIVATE "${RENUT_APP_DIR}" "${RENUT_APP_DIR}/src" "${SM_GLUE_DIR}"
                               "${SM_GLUE_DIR}/compat")
    target_sources(${TARGET} PRIVATE "${SM_GLUE_DIR}/renut_glue.cpp")
endfunction()
