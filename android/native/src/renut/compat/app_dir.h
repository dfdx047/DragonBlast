// The app's files folder on Android (the fork's rex::GetAndroidAppDirectory()), for the Renut's diagnostics.
#pragma once
#include <filesystem>
namespace dragonblast {
inline std::filesystem::path AppDirectory() {
  return std::filesystem::path("/sdcard/Android/data/com.dfdx047.dragonblast/files");
}
}  // namespace dragonblast
