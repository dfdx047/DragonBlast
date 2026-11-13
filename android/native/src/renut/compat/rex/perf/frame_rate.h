/**
 * @file        perf/frame_rate.h
 * @brief       Guest frame rate tracking, available in every build config.
 *
 * @license     BSD 3-Clause License
 *              See LICENSE file in the project root for full license text.
 */
#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <string>

namespace rex::perf {

// Called once per guest swap (VdSwap) by the command processor.
void RecordGuestSwap();

// Total guest swaps since startup.
uint64_t GetGuestSwapCount();

// Times between the latest guest swaps in milliseconds, oldest first (up to
// the last 255). Returns how many were written.
size_t GetGuestFrameTimes(float* out_ms, size_t max_count);

// SwordMan 0185: what the FPS overlay shows next to the guest frame rate, with the meaning of the log lines. Totals
// since startup; the overlay takes differences over its window. GPU: the GPU time of one drawn frame (Xenia: the dbz3
// timestamps, with dbz3_perf_logging; Renut: its timestamp queries). Main thread: the time the game's main thread
// worked on one frame (Xenia: its CPU time from VdSwap to VdSwap; Renut: "from Swap to Swap" of the game thread line,
// without the pacing wait).
void RecordGpuFrame(double ms);
void RecordMainThreadFrame(double ms);
struct FrameCostTotals {
  uint64_t gpu_frames = 0;
  uint64_t gpu_us = 0;
  uint64_t cpu_frames = 0;
  uint64_t cpu_us = 0;
};
FrameCostTotals GetFrameCostTotals();

// Input latency probe (debug cvar latency_log): "[Latency] <event>" log lines
// with the steady clock in milliseconds (QPC milliseconds on Windows, the
// clock PresentMon --qpc_time_ms uses), for a press's way to the screen.
// Lines are written for 400 ms after a new button press the guest read
// (LatencyOpenWindow), so a whole session doesn't flood the log.
bool IsLatencyLogging();
void LatencyOpenWindow();
bool IsLatencyWindowOpen();
void LatencyLog(const char* event, uint64_t a = 0, uint64_t b = 0);
// Guest VdSwap calls (the guest thread's end of a frame), for the probe.
uint64_t RecordGuestVdSwap();
uint64_t GetGuestVdSwapCount();

// Guest frontbuffer size (before resolution scaling) of the latest swap.
void RecordGuestFrontbuffer(uint32_t width, uint32_t height);

// Draw resolution scale the GPU backend renders with, and the one the config
// asked for (higher when an upscaler quality mode renders below it).
void SetDrawResolutionScale(uint32_t x, uint32_t y, uint32_t requested_x, uint32_t requested_y);
// Rendering below 1x: the percent of the 1x resolution the game is drawn at
// (100 = off).
void SetDrawResolutionPercent(uint32_t percent);
// SwordMan (0175): what the debug overlay shows as "Upscaler" when the app draws the frame itself (no presenter, e.g.
// the Renut renderer): empty = the presenter's settings (default).
void SetUpscalerOverride(std::string text);
std::string GetUpscalerOverride();

struct RenderInfo {
  uint32_t frontbuffer_width = 0;
  uint32_t frontbuffer_height = 0;
  uint32_t scale_x = 0;
  uint32_t scale_y = 0;
  uint32_t requested_scale_x = 0;
  uint32_t requested_scale_y = 0;
  uint32_t percent = 100;
};
RenderInfo GetRenderInfo();

// Texture cache activity, for the perf_report debug command (totals since
// startup; the reader takes differences). Written by the GPU thread.
struct TextureCacheStats {
  std::atomic<uint64_t> created{0};             // host textures created
  std::atomic<uint64_t> destroyed{0};           // destroyed by the memory limits
  std::atomic<uint64_t> loaded{0};              // texture data uploads (guest or replacement)
  std::atomic<uint64_t> replacement_uploads{0}; // uploads from replacement images
  std::atomic<uint64_t> replacement_lookups{0}; // texture lookups checked for a replacement
  std::atomic<uint64_t> replacement_hashes{0};  // full replacement key hashes of guest data
  std::atomic<uint64_t> replacement_hash_ns{0};
  std::atomic<uint64_t> replacement_decodes{0}; // image files decoded on the GPU thread
  std::atomic<uint64_t> replacement_decode_ns{0};
  // Replacements decoded in the background (texture_replace_async).
  std::atomic<uint64_t> replacement_async_loads{0};
  std::atomic<uint64_t> replacement_async_ns{0};
  // Current values.
  std::atomic<uint64_t> memory_bytes{0};        // host memory of the cached textures
  std::atomic<uint32_t> limit_soft_mb{0};
  std::atomic<uint32_t> limit_hard_mb{0};
  std::atomic<uint64_t> vram_usage_bytes{0};    // the process's local video memory use
  std::atomic<uint64_t> vram_budget_bytes{0};   // the OS budget for it
};
TextureCacheStats& GetTextureCacheStats();

// Dragon Burst (0092): what reaches the screen. Totals since startup; the reader takes
// differences (the perf line every 5 s). Written by the presenter.
struct PresentStats {
  std::atomic<uint64_t> presents{0};          // host presents: real + generated frames
  // SwordMan 0185: frames that reach the screen, each once: new guest frames + generated ones (the presents repeat
  // the last frame at the display rate). The FPS overlay's "Display".
  std::atomic<uint64_t> shown{0};
  std::atomic<uint64_t> generated{0};         // generated (interpolated) frames presented
  std::atomic<uint64_t> not_generated{0};     // new guest frames shown without one (no history,
                                              // size change) while frame generation is on
  std::atomic<uint64_t> cuts{0};              // generated frames that were a scene cut (= the new one)
  std::atomic<uint64_t> fallback_pixels{0};   // pixels of generated frames that used the blend /
  std::atomic<uint64_t> moving_pixels{0};     // one-sided fallback, of the pixels that changed
  std::atomic<uint64_t> gpu_ns{0};            // GPU time of the frame generation passes
  std::atomic<uint64_t> gpu_samples{0};
};
PresentStats& GetPresentStats();

// Refresh rate of the display the game is on, from the platform (Android: the activity, which
// also picks the display mode). 0 = unknown.
void SetDisplayRefreshRate(float hz);
float GetDisplayRefreshRate();

// One line about how frames reach the screen (present mode, display rate, frame generation),
// for the logs and the render self-test.
void SetPresentInfo(const std::string& info);
std::string GetPresentInfo();

// GPU time per part of a frame, from timestamp queries (gpu_profile debug
// command). Off unless a measurement is running.
enum class GpuPass : uint32_t {
  kOther,          // everything not below (uploads, index conversion, barriers)
  kDraw,           // the game's draws
  kDrawMemexport,  // the game's draws exporting to memory (with their barriers)
  kRenderTargets,  // render target cache: ownership transfers between host RTs (color)
  kRtTransferDepth,    // ownership transfers into depth render targets (depth part)
  kRtTransferStencil,  // ... their stencil, bit by bit (no stencil reference output)
  kResolve,        // resolves: render target dump to the EDRAM buffer
  kResolveCopy,    // resolves: EDRAM buffer -> (scaled) resolve memory
  kResolveClear,   // clears done as part of resolves
  kTextureLoad,    // texture loading / untiling (incl. resolved textures)
  kSharedMemory,   // guest memory uploads for vertex buffers etc.
  kDlssInputs,     // DLSS/FSR: depth + motion vector compute
  kDlssEvaluate,   // NGX evaluate / FSR dispatch
  kDlssCopyBack,   // upscaler output back into the render target (+ downsample)
  kDlssMirror,     // HUD draws mirrored into the output-size picture
  kDlssFreezeHud,  // copy of the render target at its first resolve
  kDlssCompose,    // compose of the upscaled picture with the presented frame
  kSwap,           // gamma ramp / FXAA of the frontbuffer
  kPresenterGuest, // presenter: guest output effects (CAS, FSR, bilinear)
  kPresenterUi,    // presenter: overlays and UI
  kDrawFocus,      // the game's draws with a pixel shader in gpu_profile_focus_ps
  kCount,
};
const char* GetGpuPassName(GpuPass pass);

struct GpuProfileStats {
  std::atomic<bool> enabled{false};
  std::atomic<uint64_t> pass_ns[size_t(GpuPass::kCount)] = {};
  std::atomic<uint64_t> pass_count[size_t(GpuPass::kCount)] = {};  // timestamped intervals
  // GPU time from the first to the last timestamp of each command processor
  // submission / presenter paint.
  std::atomic<uint64_t> submission_ns{0};
  std::atomic<uint64_t> submissions{0};
  std::atomic<uint64_t> submissions_unprofiled{0};
  std::atomic<uint64_t> paint_ns{0};
  std::atomic<uint64_t> paints{0};
  // GPU idle time between consecutive command processor submissions (presenter
  // paints in between included).
  std::atomic<uint64_t> gap_ns{0};
  // CPU side, command processor thread.
  std::atomic<uint64_t> fence_waits{0};      // waits for the GPU (submission fences)
  std::atomic<uint64_t> fence_wait_ns{0};
  std::atomic<uint64_t> occlusion_queries{0};
  std::atomic<uint64_t> draws{0};
  std::atomic<uint64_t> resolves{0};
  std::atomic<uint64_t> clears_in_place{0};
  std::atomic<uint64_t> memory_upload_batches{0};  // shared memory UploadRanges with work
  std::atomic<uint64_t> memory_upload_bytes{0};
  std::atomic<uint64_t> texture_loads{0};
  std::atomic<uint64_t> texture_load_bytes{0};  // host texture bytes written by loads  // clear draws done as clears (gpu_clear_draws_in_place)
  std::atomic<uint64_t> cp_submissions{0};
  // Presenter (UI thread): time in PaintAndPresent, and in Present itself.
  std::atomic<uint64_t> present_calls{0};
  std::atomic<uint64_t> present_cpu_ns{0};
};
GpuProfileStats& GetGpuProfileStats();
// What the command processor (GPU) thread is doing, for the gpu_state debug
// command (finding where a stall is: waiting for the game, for the GPU, or
// stuck in between). Written by the GPU thread, read by anyone.
enum class GpuThreadWhere : uint32_t {
  kStarting,
  kIdle,          // waiting for the game to submit commands
  kExecuting,     // executing commands
  kWaitRegMem,    // PM4 WAIT_REG_MEM: waiting for a memory / register value
  kFenceWait,     // waiting for a GPU submission to complete
  kQueueOpsWait,  // waiting for out-of-submission queue operations
  kCount,
};
struct GpuThreadState {
  std::atomic<uint32_t> where{0};
  std::atomic<uint64_t> where_since_ms{0};  // host uptime when `where` last changed
  std::atomic<uint64_t> primary_buffers{0}; // ExecutePrimaryBuffer calls
  std::atomic<uint32_t> read_ptr{0};
  std::atomic<uint32_t> write_ptr{0};
  std::atomic<uint32_t> wait_reg_mem_address{0};  // dword address (register or memory)
  std::atomic<uint32_t> wait_reg_mem_ref{0};
  std::atomic<uint32_t> wait_reg_mem_value{0};
  std::atomic<uint32_t> wait_reg_mem_flags{0};
  std::atomic<uint64_t> fence_await{0};
  std::atomic<uint64_t> fence_completed{0};
};
GpuThreadState& GetGpuThreadState();
void SetGpuThreadWhere(GpuThreadWhere where);
const char* GetGpuThreadWhereName(uint32_t where);
inline bool IsGpuProfiling() {
  return GetGpuProfileStats().enabled.load(std::memory_order_relaxed);
}

}  // namespace rex::perf
