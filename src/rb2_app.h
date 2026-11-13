// rb2 - Dragon Ball: Raging Blast 2, recompiled with ReXGlue (SDK 0.10).
//
// The original project (WistfulHopes/RB2) predates the SDK's 0.2 overhaul and replaced the game's
// heap and file I/O by hand (src/bdengine). SDK 0.10 ships the same thing as its rexcrt layer, so the
// codegen manifest (config/rb2_manifest.toml) maps the game's CRT routines to it instead.

#pragma once

#include <rex/cvar.h>
#include <rex/logging.h>
#include <rex/perf/counter.h>
#include <rex/rex_app.h>

// Dragon Blast: the Renut (swordman_renderer = renut), android/native/src/renut/rb2_renut_app.h.
#if defined(DB_RENUT)
#include "rb2_renut_app.h"
#endif

class Rb2App : public rex::ReXApp {
 public:
  using rex::ReXApp::ReXApp;

  static std::unique_ptr<rex::ui::WindowedApp> Create(rex::ui::WindowedAppContext& ctx) {
    return std::unique_ptr<Rb2App>(new Rb2App(ctx, "rb2", PPCImageConfig));
  }

#if defined(DB_RENUT)
  void OnPreSetup(rex::RuntimeConfig& config) override { Rb2RenutPreSetup(config); }

  std::optional<rex::PathConfig> OnFinalizePaths(const rex::PathConfig& defaults,
                                                 std::function<void(rex::PathConfig)> resume) override {
    Rb2RenutStart(app_context(), [this] { return imgui_drawer(); });
    return ReXApp::OnFinalizePaths(defaults, std::move(resume));
  }

  // The SDK's ImGui (menus) drawn by plume, and its window.
  std::unique_ptr<rex::ui::ImmediateDrawer> OnCreateImmediateDrawer() override {
    if (banjo::renut_gpu::Active()) return renut::gpu::CreateOverlayDrawer();
    return ReXApp::OnCreateImmediateDrawer();
  }

  void OnWindowPixelSizeChanged(uint32_t width, uint32_t height) override {
    ReXApp::OnWindowPixelSizeChanged(width, height);
    if (banjo::renut_gpu::Active()) renut::gpu::OnWindowResized();
  }

  void OnShutdown() override {
    if (banjo::renut_gpu::Active()) renut::gpu::ShutdownRenderer();
  }
#endif

  // The runtime compiles the per-frame perf-counter CSV writer in, but nothing in the SDK opens
  // the --perf_log_csv file: wire it once logging is up (same as Project8Recomp).
  void OnPostSetup() override {
    std::string csv_path = REXCVAR_QUERY(std::string, perf_log_csv);
    if (!csv_path.empty()) {
      rex::perf::SetCsvLogPath(csv_path);
      REXLOG_INFO("perf: csv -> {}", csv_path);
    }
  }
};
