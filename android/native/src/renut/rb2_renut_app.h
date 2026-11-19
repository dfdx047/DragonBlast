// The Renut in Rb2App (src/rb2_app.h): what Zandatsu's MgrApp and Dragon Burst's db_renut_app.h do in the app phases.
// Only with DB_RENUT (android/native/cmake/renut.cmake); swordman_renderer = renut chooses it.
#pragma once

#if defined(DB_RENUT)
#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <functional>
#include <stdexcept>
#include <string>

#include <rex/cvar.h>
#include <rex/logging.h>
#include <rex/rex_app.h>

#include "renut_engine/gpu/banjo_renut_select.h"
#include "renut_engine/gpu/overlay/app_integration.h"
#include "compat/app_dir.h"

// renut_glue.cpp: the Vulkan features the Renut assumes.
bool SwordManRenutDeviceSupported(std::string* why = nullptr);

REXCVAR_DECLARE(int32_t, rb2_render_scale);             // src/rb2_resolution.h: Xenia's resolution (50-100 %)
REXCVAR_DECLARE(int32_t, sa2_render_height_handheld);  // renut_glue.cpp: the Renut's height

// OnPreSetup: true with the Renut (no emulated GPU nor plugin; the Renut draws to the window with its own Vulkan
// device). A driver without what the Renut needs (the stock Qualcomm driver of the Adreno 6xx: descriptor indexing
// with fewer than 8192 textures, issues #1, #2 and #4) runs the game on Xenia instead of closing it: the Renut stays
// off for the session, Xenia gets the launcher's height as its percent of 720p, and files/renut_fallback.txt tells
// the launcher why (it shows it under Graphics > Renderer).
inline bool Rb2RenutPreSetup(rex::RuntimeConfig& config) {
  const auto note = dragonblast::AppDirectory() / "renut_fallback.txt";
  if (!banjo::renut_gpu::Active()) return false;
  std::string why;
  if (!SwordManRenutDeviceSupported(&why)) {
    banjo::renut_gpu::detail::g_active = -1;
    const int height = REXCVAR_GET(sa2_render_height_handheld);
    REXCVAR_SET(rb2_render_scale, std::clamp(height * 100 / 720, 50, 100));
    REXLOG_ERROR("Renut: the driver lacks what the Renut needs ({}); the game runs on Xenia ({} % of 720p)", why,
                 REXCVAR_GET(rb2_render_scale));
    if (FILE* f = std::fopen(note.c_str(), "w")) {
      std::fprintf(f, "%s\n", why.c_str());
      std::fclose(f);
    }
    return false;
  }
  std::error_code ec;
  std::filesystem::remove(note, ec);
  renut::gpu::ConfigureRuntime(config);
  REXLOG_INFO("graphics: Renut (mgr-nx-deko's native Direct3D, plume/Vulkan), no PM4 nor EDRAM");
  return true;
}

// OnFinalizePaths: the window exists; the Renut's Vulkan device before the runtime.
template <typename Drawer>
inline void Rb2RenutStart(rex::ui::WindowedAppContext& context, Drawer&& imgui_drawer) {
  if (banjo::renut_gpu::Active() && !renut::gpu::StartRenderer(context, std::forward<Drawer>(imgui_drawer))) {
    REXLOG_ERROR("Renut: the renderer did not start (swordman_renderer = xenos to use Xenia)");
    throw std::runtime_error("Renut: the renderer did not start");
  }
}
#endif
