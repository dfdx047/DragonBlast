// The Renut in Rb2App (src/rb2_app.h): what Zandatsu's MgrApp and Dragon Burst's db_renut_app.h do in the app phases.
// Only with DB_RENUT (android/native/cmake/renut.cmake); swordman_renderer = renut chooses it.
#pragma once

#if defined(DB_RENUT)
#include <cstdlib>
#include <functional>
#include <stdexcept>
#include <string>

#include <rex/logging.h>
#include <rex/rex_app.h>

#include "renut_engine/gpu/banjo_renut_select.h"
#include "renut_engine/gpu/overlay/app_integration.h"

// renut_glue.cpp: the Vulkan features the Renut assumes.
bool SwordManRenutDeviceSupported(std::string* why = nullptr);

// OnPreSetup: true with the Renut (no emulated GPU nor plugin; the Renut draws to the window with its own Vulkan
// device). A driver without what the Renut needs closes the game with the reason in the log.
inline bool Rb2RenutPreSetup(rex::RuntimeConfig& config) {
  if (!banjo::renut_gpu::Active()) return false;
  std::string why;
  if (!SwordManRenutDeviceSupported(&why)) {
    banjo::renut_gpu::detail::g_active = -1;
    REXLOG_ERROR("Renut: the driver lacks what the Renut needs ({}); the game closes", why);
    std::_Exit(3);
  }
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
