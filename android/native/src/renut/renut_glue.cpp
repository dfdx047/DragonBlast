// Renut (mgr-nx-deko's native Direct3D renderer) in Dragon Blast, from Dragon Burst's renut_glue.cpp (itself from
// Zandatsu's): the boot-time choice and the hooks the Renut shares with this glue (Swap and the two draws), at
// Raging Blast 2's addresses (docs/renut/HOOKMAP_RB2.md). Built only with DB_RENUT (cmake/renut.cmake).
// The cvar names are Zandatsu's (swordman_*), the same Renut.
//
// swordman_renderer = xenos (default): the emulated Xenia GPU; every Renut hook calls the original function.
// swordman_renderer = renut: the game's Direct3D runs on the Renut (plume/Vulkan), no PM4 ring and no EDRAM.

#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <filesystem>
#include <cstdio>

#include <dlfcn.h>

#include <rex/cvar.h>
#include <rex/hook.h>
#include <rex/logging.h>
#include <rex/memory/utils.h>
#include <rex/system/xmemory.h>
#include <rex/ppc.h>
#include <rex/system/function_dispatcher.h>
#include <rex/system/kernel_state.h>
#include <rex/system/xthread.h>

#include "renut_engine/gpu/backend/render_thread.h"
#include "renut_engine/gpu/banjo_renut_select.h"
#include "renut_engine/gpu/banjo_shared_hooks.h"
#include "renut_engine/gpu/d3d/d3d_native.h"
#include "renut_engine/gpu/sa2_renut_yield.h"
#include "compat/app_dir.h"

REXCVAR_DECLARE(std::string, swordman_renut_bc_decode);  // Renut (renut/0020): BC textures the driver lacks

REXCVAR_DEFINE_STRING(swordman_renderer, "xenos", "SwordMan",
                      "Renderer, chosen at boot. xenos = the emulated Xbox GPU (Xenia, xenos plugin). "
                      "renut = the game's Direct3D natively on mgr-nx-deko's Renut (plume, Vulkan)")
    .allowed({"xenos", "renut"})
    .lifecycle(rex::cvar::Lifecycle::kInitOnly);

REXCVAR_DEFINE_BOOL(swordman_renut_vblank, true, "SwordMan",
                    "Renut: D3D's VBlank handler (8249AED8) at 60 Hz, which the game's Present waits on")
    .lifecycle(rex::cvar::Lifecycle::kInitOnly);

REXCVAR_DEFINE_INT32(swordman_renut_vblank_hz, 60, "SwordMan", "Renut: VBlank rate the game sees (30-240)")
    .range(30, 240)
    .lifecycle(rex::cvar::Lifecycle::kHotReload);

// The Renut's "120 FPS" (renut/0025, backend/present.cpp: a frame generated halfway between each two game frames,
// shown on a display at 100 Hz or more). Its cvars came with Dragon Burst's SDK patch 0092, which this SDK lacks.
REXCVAR_DEFINE_BOOL(dbz3_frame_interpolation, false, "SwordMan",
                    "Renut: frame generation - a frame halfway between each two game frames (60 -> 120 FPS on a "
                    "120 Hz display, one display frame of extra latency)")
    .lifecycle(rex::cvar::Lifecycle::kHotReload);
REXCVAR_DEFINE_INT32(dbz3_frame_interpolation_quality, 1, "SwordMan",
                     "Renut frame generation: 0 = blend only, 1 = motion (3 candidates per pixel), 2 = motion with "
                     "the 4 neighbouring block vectors (better edges)")
    .range(0, 2)
    .lifecycle(rex::cvar::Lifecycle::kHotReload);

// Frames presented by the game in Renut mode.
std::atomic<uint64_t> g_sa2_game_frames{0};

#include <rex/graphics/frame_pacing.h>  // compat/ (the Renut's frame pacing reads the guest VBlank rate there)
#include <rex/perf/frame_rate.h>  // compat/: the display refresh rate the frame generation reads
#include <rex/perf/counter.h>    // the SDK perf counters (FPS overlay)

// plume (renut/0026): called around each submit; Zandatsu's GPU-loss gate. Nothing waits here.
extern "C" void zandatsu_gpu_gate() {}

// A lost Vulkan device (plume, VK_ERROR_DEVICE_LOST): the game cannot go on; it closes with the reason in the log
// (the launcher shows the last session's log).
namespace swordman::gpu_reset {
void Trigger(const char* reason) {
  REXLOG_ERROR("Renut: GPU lost ({}): closing the game", reason);
  rex::FlushLogging();
  std::_Exit(4);
}
}  // namespace swordman::gpu_reset

extern "C" void zandatsu_gpu_lost(const char* call) {
  swordman::gpu_reset::Trigger((std::string("VK_ERROR_DEVICE_LOST in ") + call).c_str());
}

// ---------------------------------------------------------------------------------------------------------------
// D3D's VBlank with the Renut. Raging Blast 2 keeps time by the VBlank like Burst Limit: its Present (0x823A8888)
// yields until the counter 0x8264C4F8 reaches the requested interval, and only the game's VBlank callback (0x823A8510,
// registered with D3DDevice_SetVerticalBlankCallback) advances it. On the console D3D's VBlank handler (0x8249AED8,
// called from the GPU interrupt) runs that callback; with the Renut there is no GraphicsSystem and no interrupt, so a
// host thread calls the handler at the VBlank rate. The flip queue stays empty (Swap is hooked), so the handler never
// writes a GPU register.
// ---------------------------------------------------------------------------------------------------------------
// Diagnostics (renut/0202 and the draw hooks below): logged every 10 s by the VBlank thread.
extern "C" std::atomic<uint32_t> rb2_renut_cb_begin, rb2_renut_cb_end, rb2_renut_cb_run_recorded,
    rb2_renut_cb_run_queued;
std::atomic<uint32_t> g_rb2_draws{0}, g_rb2_indexed_draws{0}, g_rb2_slots_marked{0};

namespace {
constexpr uint32_t kRb2VBlankHandler = 0x8249AED8;
std::atomic<uint64_t> g_vblanks{0};
rex::system::object_ref<rex::system::XHostThread> g_vblank_thread;

std::chrono::nanoseconds VBlankInterval() {
  const double hz = double(REXCVAR_GET(swordman_renut_vblank_hz));
  rex::graphics::SetGuestVblankRate(hz);
  return std::chrono::nanoseconds(int64_t(1e9 / (hz >= 30.0 && hz <= 240.0 ? hz : 60.0)));
}

int VBlankThreadMain() {
  using clock = std::chrono::steady_clock;
  auto interval = VBlankInterval();
  auto next = clock::now() + interval;
  auto* kernel = rex::system::kernel_state();
  auto* dispatcher = kernel->function_dispatcher();
  auto* thread = rex::system::XThread::GetCurrentThread();
  for (;;) {
    std::this_thread::sleep_until(next);
    interval = VBlankInterval();
    next += interval;
    // More than 4 VBlanks late (thread suspended): go on from now, without a burst.
    if (clock::now() - next > 4 * interval) next = clock::now() + interval;
    const uint32_t device = renut::gpu::native::GlobalDevice();
    if (!device) continue;
    uint64_t args[] = {device};
    dispatcher->ExecuteInterrupt(thread->thread_state(), kRb2VBlankHandler, args, 1);
    const uint64_t n = g_vblanks.fetch_add(1, std::memory_order_relaxed) + 1;
    // The display's refresh rate, which the frame generation needs (>= 100 Hz): GameActivity writes it to
    // files/display_hz.txt on start and on every display mode change. Read once a second.
    // The launcher's FPS counter (the SDK's compact debug overlay) reads the frame time from the perf counter
    // snapshot, which only the emulated GPU's command processor fills: the game's frame rate every half second.
    if (n % 30 == 0) {
      static uint64_t last_frames = 0;
      static auto last_time = std::chrono::steady_clock::now();
      const uint64_t frames = g_sa2_game_frames.load(std::memory_order_relaxed);
      const auto now = std::chrono::steady_clock::now();
      const int64_t us = std::chrono::duration_cast<std::chrono::microseconds>(now - last_time).count();
      if (frames > last_frames && us > 0) {
        rex::perf::SetCounter(rex::perf::CounterId::kFrameTimeUs, us / int64_t(frames - last_frames));
        rex::perf::ResetFrameCounters();  // the snapshot the overlay reads
      }
      last_frames = frames;
      last_time = now;
    }
    if (n % 60 == 1) {
      if (FILE* f = std::fopen((dragonblast::AppDirectory() / "display_hz.txt").c_str(), "r")) {
        int hz = 0;
        if (std::fscanf(f, "%d", &hz) == 1 && hz > 0) rex::perf::SetDisplayRefreshRate(float(hz));
        std::fclose(f);
      }
    }
    if (n == 1 || n == 600)
      REXLOG_INFO("Renut: D3D VBlank at {:.0f} Hz ({} vblanks, device {:08X})", 1e9 / double(interval.count()), n,
                  device);
    if (n % 600 == 0)
      REXLOG_INFO("Renut: last 10 s: DrawVertices {}, DrawIndexedVertices {}, command buffers begun {} ended {}, "
                  "runs recorded {} queued {}, empty slots marked {}",
                  g_rb2_draws.exchange(0), g_rb2_indexed_draws.exchange(0), rb2_renut_cb_begin.exchange(0),
                  rb2_renut_cb_end.exchange(0), rb2_renut_cb_run_recorded.exchange(0),
                  rb2_renut_cb_run_queued.exchange(0), g_rb2_slots_marked.exchange(0));
  }
}

void StartVBlank() {
  static std::once_flag once;
  std::call_once(once, [] {
    if (!REXCVAR_GET(swordman_renut_vblank)) return;
    g_vblank_thread = rex::system::object_ref<rex::system::XHostThread>(
        new rex::system::XHostThread(rex::system::kernel_state(), 128 * 1024, 0, [] { return VBlankThreadMain(); }));
    g_vblank_thread->set_name("Renut VBlank");
    g_vblank_thread->Create();
  });
}
}  // namespace

// The game's Present (0x823A8888) waits for VBlanks right after its first Swap: the thread must run by then.
REX_EXTERN(__imp__sub_8249B688);
REX_HOOK_RAW(sub_8249B688) {  // D3DDevice_Swap (RB2; MGR sub_826B3510)
  if (banjo::renut_gpu::Active()) {
    StartVBlank();
    static std::atomic<uint32_t> swaps{0};
    const uint32_t n = swaps.fetch_add(1, std::memory_order_relaxed);
    if (n < 3)
      REXLOG_INFO("Renut: Swap {} device {:08X} front buffer {:08X} parameters {:08X}", n, ctx.r3.u32, ctx.r4.u32,
                  ctx.r5.u32);
    renut_native_sub_8249B688(ctx, base);  // counts g_sa2_game_frames
    return;
  }
  __imp__sub_8249B688(ctx, base);
}

// The game's double-buffered command buffers. Its render object ([r3+124]) has two pairs at +304 and +344:
// { +0 memory size (2 MiB), +4 last frame's count, +8 slot index, +12/+16 count per slot, +20/+24 memory, +28/+32
// D3DCommandBuffer, +36 recording device }. 0x823A4928 records slot [+8] on the two devices of type 2; 0x823A87D8
// runs it on the title device only when its count is above 0. The count grows as D3D takes memory from the game's
// allocator for the buffer's segments; the Renut records the draws on the host and never asks, so the count stays 0
// and the game would never run a buffer. Here an empty count becomes 1 (an empty recorded buffer runs as nothing);
// 0x823A4AD0 zeroes the slot again at the next flip.
REX_EXTERN(__imp__sub_823A87D8);
REX_HOOK_RAW(sub_823A87D8) {
  if (banjo::renut_gpu::Active()) {
    auto* memory = rex::system::kernel_state()->memory();
    const auto word = [memory](uint32_t va) { return memory->TranslateVirtual<uint8_t*>(va); };
    const uint32_t object = rex::memory::load_and_swap<uint32_t>(word(ctx.r3.u32 + 124));
    if (object) {
      for (uint32_t pair : {304u, 344u}) {
        const uint32_t slot = rex::memory::load_and_swap<uint32_t>(word(object + pair + 8));
        if (slot > 1) continue;
        uint8_t* count = word(object + pair + 12 + slot * 4);
        if (int32_t(rex::memory::load_and_swap<uint32_t>(count)) <= 0) {
          rex::memory::store_and_swap<uint32_t>(count, 1);
          g_rb2_slots_marked.fetch_add(1, std::memory_order_relaxed);
        }
      }
    }
  }
  __imp__sub_823A87D8(ctx, base);
}

REX_EXTERN(__imp__sub_824A8408);
REX_HOOK_RAW(sub_824A8408) {  // D3DDevice_DrawIndexedVertices (RB2; MGR sub_826BCD40)
  g_rb2_indexed_draws.fetch_add(1, std::memory_order_relaxed);
  SA2_RENUT_YIELD_SHARED(sub_824A8408);
  __imp__sub_824A8408(ctx, base);
}

REX_EXTERN(__imp__sub_824A7FF0);
REX_HOOK_RAW(sub_824A7FF0) {  // D3DDevice_DrawVertices (RB2; MGR sub_826BC928)
  g_rb2_draws.fetch_add(1, std::memory_order_relaxed);
  SA2_RENUT_YIELD_SHARED(sub_824A7FF0);
  __imp__sub_824A7FF0(ctx, base);
}

// ---------------------------------------------------------------------------------------------------------------
// What the Renut reads from their SA2 glue (app/src/cpu/sa2_enhancements.cpp, me/sa2_render_mode.cpp,
// sa2_capture.cpp): only what the renderer calls, with their defaults.
// ---------------------------------------------------------------------------------------------------------------
#include "cpu/sa2_enhancements.h"
#include "me/sa2_render_mode.h"
#include "sa2_capture.h"
#include <rex/ui/presenter.h>

REXCVAR_DEFINE_INT32(sa2_render_height_handheld, 720, "SwordMan",
                     "Renut: render height (720 = the game's 1280x720). Multiples of 180 from 360 to 2160; the width "
                     "follows 16:9")
    .range(180, 4320)
    .lifecycle(rex::cvar::Lifecycle::kHotReload);

namespace sa2::enh {
bool CpuMaster() { return true; }
bool NonCpuMaster() { return true; }
}  // namespace sa2::enh

namespace sa2::render {
bool Docked() { return false; }
int32_t RenderHeightOf(bool) { return REXCVAR_GET(sa2_render_height_handheld); }
void OnPresented(uint64_t) {}
}  // namespace sa2::render

// Diagnostics: files/capture.req asks for the next presented frame, written as files/logs/capture_<frame>.ppm (the
// file is checked every 30 frames).
namespace sa2::capture {
namespace {
std::atomic<bool> g_capture_wanted{false};
const std::filesystem::path& CaptureRequest() {
  static const std::filesystem::path path = dragonblast::AppDirectory() / "capture.req";
  return path;
}
}  // namespace

bool RenutLockstepOn() {
  static uint32_t calls = 0;
  if (++calls % 30 == 0) {
    std::error_code ec;
    g_capture_wanted.store(std::filesystem::exists(CaptureRequest(), ec), std::memory_order_relaxed);
  }
  return g_capture_wanted.load(std::memory_order_relaxed);
}

void RenutLockstepFrame(uint64_t frame, const rex::ui::RawImage* image) {
  std::error_code ec;
  std::filesystem::remove(CaptureRequest(), ec);
  g_capture_wanted.store(false, std::memory_order_relaxed);
  if (!image || !image->width || !image->height) {
    REXLOG_WARN("Renut: frame {} capture failed", frame);
    return;
  }
  const auto path = dragonblast::AppDirectory() / "logs" / ("capture_" + std::to_string(frame) + ".ppm");
  if (FILE* f = std::fopen(path.c_str(), "wb")) {
    std::fprintf(f, "P6\n%u %u\n255\n", image->width, image->height);
    for (uint32_t y = 0; y < image->height; ++y)
      for (uint32_t x = 0; x < image->width; ++x) std::fwrite(&image->data[y * image->stride + x * 4], 1, 3, f);
    std::fclose(f);
    REXLOG_INFO("Renut: frame {} captured ({}x{}) to {}", frame, image->width, image->height, path.string());
  }
}
}  // namespace sa2::capture

// ---------------------------------------------------------------------------------------------------------------
// plume's Vulkan driver (renut/0006): the system one for now (the Drivers tab's custom driver is the Xenia path's,
// custom_driver.cpp).
// ---------------------------------------------------------------------------------------------------------------
extern "C" void* plume_android_vulkan_library() { return nullptr; }

// ---------------------------------------------------------------------------------------------------------------
// What the translated shaders and plume assume without checking. Runs in OnPreSetup, before any surface: without
// these features the game closes (rb2_renut_app.h), with the list of what is missing in the log.
// ---------------------------------------------------------------------------------------------------------------
#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>

namespace plume {
void SetAndroidClearFold(bool enabled);  // renut/0106
}
namespace renut::gpu {
void SetPointSpriteUbo(bool on);  // renut/0108
}

REXCVAR_DEFINE_BOOL(swordman_renut_point_ubo, false, "SwordMan",
                    "Renut: point sprites read SharedConstants from the UBO even with shaderInt64")
    .lifecycle(rex::cvar::Lifecycle::kInitOnly);
REXCVAR_DEFINE_BOOL(swordman_renut_clear_fold, true, "SwordMan",
                    "Renut: a whole-target clear before the pass's first draw opens the pass with loadOp CLEAR")
    .lifecycle(rex::cvar::Lifecycle::kInitOnly);

bool SwordManRenutDeviceSupported(std::string* why) {
  plume::SetAndroidClearFold(REXCVAR_GET(swordman_renut_clear_fold));
  void* library = plume_android_vulkan_library();
  if (!library) library = dlopen("libvulkan.so", RTLD_NOW | RTLD_LOCAL);
  auto get_instance_proc =
      library ? reinterpret_cast<PFN_vkGetInstanceProcAddr>(dlsym(library, "vkGetInstanceProcAddr")) : nullptr;
  if (!get_instance_proc) {
    REXLOG_ERROR("Renut: no Vulkan driver");
    if (why) *why = "vulkan";
    return false;
  }
  auto create_instance = reinterpret_cast<PFN_vkCreateInstance>(get_instance_proc(nullptr, "vkCreateInstance"));
  VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};
  app.apiVersion = VK_API_VERSION_1_2;
  VkInstanceCreateInfo info{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
  info.pApplicationInfo = &app;
  VkInstance instance = VK_NULL_HANDLE;
  if (!create_instance || create_instance(&info, nullptr, &instance) != VK_SUCCESS) {
    REXLOG_ERROR("Renut: vkCreateInstance (Vulkan 1.2) failed");
    if (why) *why = "vulkan-1.2";
    return false;
  }
#define SM_VK(name) auto name = reinterpret_cast<PFN_##name>(get_instance_proc(instance, #name))
  SM_VK(vkDestroyInstance);
  SM_VK(vkEnumeratePhysicalDevices);
  SM_VK(vkGetPhysicalDeviceFeatures2);
  SM_VK(vkGetPhysicalDeviceProperties2);
  SM_VK(vkGetPhysicalDeviceFormatProperties);
#undef SM_VK
  uint32_t count = 1;
  VkPhysicalDevice device = VK_NULL_HANDLE;
  vkEnumeratePhysicalDevices(instance, &count, &device);
  std::string missing;
  if (!device) {
    missing = " device";
  } else {
    VkPhysicalDeviceVulkan12Features f12{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES};
    VkPhysicalDeviceFeatures2 f{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2, &f12};
    vkGetPhysicalDeviceFeatures2(device, &f);
    VkPhysicalDeviceVulkan12Properties p12{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_PROPERTIES};
    VkPhysicalDeviceProperties2 p{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2, &p12};
    vkGetPhysicalDeviceProperties2(device, &p);
    VkFormatProperties d32s8{};
    vkGetPhysicalDeviceFormatProperties(device, VK_FORMAT_D32_SFLOAT_S8_UINT, &d32s8);
    const auto need = [&](bool ok, const char* what) {
      if (!ok) missing += std::string(" ") + what;
    };
    need(f12.bufferDeviceAddress, "bufferDeviceAddress");
    if (!f.features.shaderInt64 && !f.features.textureCompressionBC)
      REXCVAR_SET(swordman_renut_bc_decode, std::string("cpu"));
    renut::gpu::SetPointSpriteUbo(REXCVAR_GET(swordman_renut_point_ubo) || !f.features.shaderInt64);
    need(f12.descriptorIndexing && f12.runtimeDescriptorArray && f12.descriptorBindingPartiallyBound &&
             f12.descriptorBindingVariableDescriptorCount && f12.descriptorBindingSampledImageUpdateAfterBind,
         "descriptorIndexing");
    need(p12.maxDescriptorSetUpdateAfterBindSampledImages >= 8192, "8192-textures");
    if (!f.features.textureCompressionBC)
      REXLOG_INFO("Renut: no BC textures in the driver; decompressed at upload (swordman_renut_bc_decode = {})",
                  REXCVAR_GET(swordman_renut_bc_decode));
    if (!f.features.vertexPipelineStoresAndAtomics)
      REXLOG_WARN("Renut: no vertexPipelineStoresAndAtomics (memexport in vertex shaders not guaranteed)");
    need(d32s8.optimalTilingFeatures & VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT, "D32S8");
    if (p.properties.vendorID != 0x5143) need(p.properties.limits.maxBoundDescriptorSets >= 5, "5-descriptor-sets");
    // The Renut's vertex layouts bind every location of RENUT_VERTEX_INPUT_LOCATIONS (0-23, renut/0201).
    need(p.properties.limits.maxVertexInputAttributes >= 24, "24-vertex-attributes");
    REXLOG_INFO("Renut: {} (Vulkan {}.{}), geometryShader {}, maxBoundDescriptorSets {}, maxVertexInputAttributes {}",
                p.properties.deviceName, VK_API_VERSION_MAJOR(p.properties.apiVersion),
                VK_API_VERSION_MINOR(p.properties.apiVersion), f.features.geometryShader ? "yes" : "no",
                p.properties.limits.maxBoundDescriptorSets, p.properties.limits.maxVertexInputAttributes);
  }
  vkDestroyInstance(instance, nullptr);
  if (!missing.empty()) {
    REXLOG_ERROR("Renut: the driver lacks{}", missing);
    if (why) *why = missing.substr(1);
    return false;
  }
  return true;
}
