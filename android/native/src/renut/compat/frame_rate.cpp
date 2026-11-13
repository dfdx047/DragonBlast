/**
 * @file        core/perf/frame_rate.cpp
 * @brief       Guest frame rate tracking. See perf/frame_rate.h.
 *
 * @license     BSD 3-Clause License
 *              See LICENSE file in the project root for full license text.
 */
#include <rex/perf/frame_rate.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <mutex>
#include <string>

#include <rex/cvar.h>
#include <rex/logging.h>

REXCVAR_DEFINE_BOOL(latency_log, false, "Perf",
                    "Debug: log each new controller press's way to the screen as [Latency] "
                    "lines (guest read, guest vblanks and swaps, GPU swap, present) with QPC "
                    "milliseconds, for 400 ms after the press.")
    .lifecycle(rex::cvar::Lifecycle::kHotReload);

namespace rex::perf {

namespace {
// Times of the latest swaps, by swap number. Written by the command processor
// thread only.
constexpr size_t kSwapTimeHistory = 256;
std::array<std::atomic<int64_t>, kSwapTimeHistory> g_swap_times_ns{};
std::atomic<uint64_t> g_guest_swap_count{0};
std::atomic<uint32_t> g_frontbuffer_width{0};
std::atomic<uint32_t> g_frontbuffer_height{0};
std::atomic<uint32_t> g_scale_x{0};
std::atomic<uint32_t> g_scale_y{0};
std::atomic<uint32_t> g_requested_scale_x{0};
std::atomic<uint32_t> g_requested_scale_y{0};
std::atomic<uint32_t> g_percent{100};
std::atomic<int64_t> g_latency_window_until_ns{0};
std::atomic<uint64_t> g_guest_vdswap_count{0};

int64_t SteadyNowNs() {
  return std::chrono::duration_cast<std::chrono::nanoseconds>(
             std::chrono::steady_clock::now().time_since_epoch())
      .count();
}
}  // namespace

bool IsLatencyLogging() {
  return REXCVAR_GET(latency_log);
}

void LatencyOpenWindow() {
  if (!REXCVAR_GET(latency_log)) {
    return;
  }
  g_latency_window_until_ns.store(SteadyNowNs() + 400'000'000, std::memory_order_relaxed);
}

bool IsLatencyWindowOpen() {
  return REXCVAR_GET(latency_log) &&
         SteadyNowNs() < g_latency_window_until_ns.load(std::memory_order_relaxed);
}

// Dragon Burst: present= is the frames shown (PresentStats; the PC's host present times are not in this SDK).
void LatencyLog(const char* event, uint64_t a, uint64_t b) {
  const int64_t now = SteadyNowNs();
  if (!REXCVAR_GET(latency_log) || now >= g_latency_window_until_ns.load(std::memory_order_relaxed)) {
    return;
  }
  REXLOG_INFO("[Latency] {} {} {} t={:.3f} vdswap={} xeswap={} present={}", event, a, b,
              double(now) * 1.0e-6, g_guest_vdswap_count.load(std::memory_order_relaxed),
              g_guest_swap_count.load(std::memory_order_relaxed),
              GetPresentStats().shown.load(std::memory_order_relaxed));
}

uint64_t RecordGuestVdSwap() {
  const uint64_t index = g_guest_vdswap_count.fetch_add(1, std::memory_order_relaxed) + 1;
  LatencyLog("vdswap");
  return index;
}

uint64_t GetGuestVdSwapCount() {
  return g_guest_vdswap_count.load(std::memory_order_relaxed);
}

void RecordGuestSwap() {
  const int64_t now = std::chrono::duration_cast<std::chrono::nanoseconds>(
                          std::chrono::steady_clock::now().time_since_epoch())
                          .count();
  // The time first, so readers never see a count with its time missing.
  const uint64_t index = g_guest_swap_count.load(std::memory_order_relaxed);
  g_swap_times_ns[index % kSwapTimeHistory].store(now, std::memory_order_relaxed);
  g_guest_swap_count.store(index + 1, std::memory_order_release);
  LatencyLog("xeswap");
}

uint64_t GetGuestSwapCount() {
  return g_guest_swap_count.load(std::memory_order_relaxed);
}

size_t GetGuestFrameTimes(float* out_ms, size_t max_count) {
  const uint64_t count = g_guest_swap_count.load(std::memory_order_acquire);
  if (!out_ms || count < 2) {
    return 0;
  }
  const size_t frames =
      size_t(std::min<uint64_t>({uint64_t(max_count), count - 1, kSwapTimeHistory - 1}));
  for (size_t i = 0; i < frames; ++i) {
    const uint64_t newer = count - frames + i;
    const int64_t interval_ns =
        g_swap_times_ns[newer % kSwapTimeHistory].load(std::memory_order_relaxed) -
        g_swap_times_ns[(newer - 1) % kSwapTimeHistory].load(std::memory_order_relaxed);
    out_ms[i] = float(std::max<int64_t>(interval_ns, 0)) * 1.0e-6f;
  }
  return frames;
}

void RecordGuestFrontbuffer(uint32_t width, uint32_t height) {
  g_frontbuffer_width.store(width, std::memory_order_relaxed);
  g_frontbuffer_height.store(height, std::memory_order_relaxed);
}

void SetDrawResolutionScale(uint32_t x, uint32_t y, uint32_t requested_x, uint32_t requested_y) {
  g_scale_x.store(x, std::memory_order_relaxed);
  g_scale_y.store(y, std::memory_order_relaxed);
  g_requested_scale_x.store(requested_x, std::memory_order_relaxed);
  g_requested_scale_y.store(requested_y, std::memory_order_relaxed);
}

void SetDrawResolutionPercent(uint32_t percent) {
  g_percent.store(percent ? percent : 100, std::memory_order_relaxed);
}

RenderInfo GetRenderInfo() {
  RenderInfo info;
  info.percent = g_percent.load(std::memory_order_relaxed);
  info.frontbuffer_width = g_frontbuffer_width.load(std::memory_order_relaxed);
  info.frontbuffer_height = g_frontbuffer_height.load(std::memory_order_relaxed);
  info.scale_x = g_scale_x.load(std::memory_order_relaxed);
  info.scale_y = g_scale_y.load(std::memory_order_relaxed);
  info.requested_scale_x = g_requested_scale_x.load(std::memory_order_relaxed);
  info.requested_scale_y = g_requested_scale_y.load(std::memory_order_relaxed);
  return info;
}

namespace {
std::atomic<uint64_t> g_gpu_frames{0}, g_gpu_us{0}, g_cpu_frames{0}, g_cpu_us{0};
}  // namespace

void RecordGpuFrame(double ms) {
  g_gpu_us.fetch_add(uint64_t(std::max(ms, 0.0) * 1000.0), std::memory_order_relaxed);
  g_gpu_frames.fetch_add(1, std::memory_order_release);
}

void RecordMainThreadFrame(double ms) {
  g_cpu_us.fetch_add(uint64_t(std::max(ms, 0.0) * 1000.0), std::memory_order_relaxed);
  g_cpu_frames.fetch_add(1, std::memory_order_release);
}

FrameCostTotals GetFrameCostTotals() {
  FrameCostTotals t;
  t.gpu_frames = g_gpu_frames.load(std::memory_order_acquire);
  t.gpu_us = g_gpu_us.load(std::memory_order_relaxed);
  t.cpu_frames = g_cpu_frames.load(std::memory_order_acquire);
  t.cpu_us = g_cpu_us.load(std::memory_order_relaxed);
  return t;
}

PresentStats& GetPresentStats() {
  static PresentStats stats;
  return stats;
}

namespace {
std::atomic<float> g_display_refresh_hz{0.0f};
std::mutex g_present_info_mutex;
std::string g_present_info;
}  // namespace

void SetDisplayRefreshRate(float hz) {
  g_display_refresh_hz.store(hz > 0.0f ? hz : 0.0f, std::memory_order_relaxed);
}

float GetDisplayRefreshRate() {
  return g_display_refresh_hz.load(std::memory_order_relaxed);
}

void SetPresentInfo(const std::string& info) {
  std::lock_guard<std::mutex> lock(g_present_info_mutex);
  g_present_info = info;
}

std::string GetPresentInfo() {
  std::lock_guard<std::mutex> lock(g_present_info_mutex);
  return g_present_info;
}

TextureCacheStats& GetTextureCacheStats() {
  static TextureCacheStats stats;
  return stats;
}

const char* GetGpuPassName(GpuPass pass) {
  switch (pass) {
    case GpuPass::kOther:
      return "other";
    case GpuPass::kDraw:
      return "draws";
    case GpuPass::kDrawMemexport:
      return "draws_memexport";
    case GpuPass::kRenderTargets:
      return "rt_transfers";
    case GpuPass::kRtTransferDepth:
      return "rt_transfers_depth";
    case GpuPass::kRtTransferStencil:
      return "rt_transfers_stencil";
    case GpuPass::kResolve:
      return "resolve_dumps";
    case GpuPass::kResolveCopy:
      return "resolve_copies";
    case GpuPass::kResolveClear:
      return "resolve_clears";
    case GpuPass::kTextureLoad:
      return "texture_loads";
    case GpuPass::kSharedMemory:
      return "memory_uploads";
    case GpuPass::kDlssInputs:
      return "dlss_inputs";
    case GpuPass::kDlssEvaluate:
      return "dlss_evaluate";
    case GpuPass::kDlssCopyBack:
      return "dlss_copy_back";
    case GpuPass::kDlssMirror:
      return "dlss_hud_mirror";
    case GpuPass::kDlssFreezeHud:
      return "dlss_freeze_hud";
    case GpuPass::kDlssCompose:
      return "dlss_compose";
    case GpuPass::kSwap:
      return "swap_gamma";
    case GpuPass::kPresenterGuest:
      return "present_effects";
    case GpuPass::kPresenterUi:
      return "present_ui";
    case GpuPass::kDrawFocus:
      return "draws_focus";
    default:
      return "?";
  }
}

GpuProfileStats& GetGpuProfileStats() {
  static GpuProfileStats stats;
  return stats;
}

namespace {
std::mutex g_upscaler_override_mutex;
std::string g_upscaler_override;
}  // namespace

void SetUpscalerOverride(std::string text) {
  std::lock_guard lock(g_upscaler_override_mutex);
  g_upscaler_override = std::move(text);
}

std::string GetUpscalerOverride() {
  std::lock_guard lock(g_upscaler_override_mutex);
  return g_upscaler_override;
}

GpuThreadState& GetGpuThreadState() {
  static GpuThreadState state;
  return state;
}

void SetGpuThreadWhere(GpuThreadWhere where) {
  GpuThreadState& state = GetGpuThreadState();
  if (state.where.exchange(uint32_t(where), std::memory_order_relaxed) != uint32_t(where)) {
    state.where_since_ms.store(
        uint64_t(std::chrono::duration_cast<std::chrono::milliseconds>(
                     std::chrono::steady_clock::now().time_since_epoch())
                     .count()),
        std::memory_order_relaxed);
  }
}

const char* GetGpuThreadWhereName(uint32_t where) {
  switch (GpuThreadWhere(where)) {
    case GpuThreadWhere::kStarting:
      return "starting";
    case GpuThreadWhere::kIdle:
      return "idle (waiting for the game's commands)";
    case GpuThreadWhere::kExecuting:
      return "executing";
    case GpuThreadWhere::kWaitRegMem:
      return "WAIT_REG_MEM";
    case GpuThreadWhere::kFenceWait:
      return "waiting for a GPU submission";
    case GpuThreadWhere::kQueueOpsWait:
      return "waiting for GPU queue operations";
    default:
      return "?";
  }
}

}  // namespace rex::perf
