// Game state for the Android side (com.dfdx047.dragonblast.game.NativeGameState): reads Raging Blast 2's
// memory through the runtime (Memory::TranslateVirtual).
//
// nativeDumpMemory writes the committed pages of a guest range to a file ("RB2D" + {u32 address, u32 size,
// bytes} chunks) so menus and fights can be compared on the PC (tools/android/memdiff.py) to find the
// addresses that say "in a fight".

#include <jni.h>
#include <android/log.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <string>

#include <rex/memory/utils.h>
#include <rex/runtime.h>
#include <rex/system/xmemory.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "DragonBlast", __VA_ARGS__)

namespace {

rex::memory::Memory* GuestMemory() {
  auto* runtime = rex::Runtime::instance();
  return runtime ? runtime->memory() : nullptr;
}

std::string JString(JNIEnv* env, jstring s) {
  const char* c = env->GetStringUTFChars(s, nullptr);
  std::string out = c ? c : "";
  if (c) env->ReleaseStringUTFChars(s, c);
  return out;
}

// Raging Blast 2 USA Rev 1, found by comparing dumps (tools/android/memdiff.py).
constexpr uint32_t kSceneWord = 0x8264C228;  // 2 = menus, 3 = fight (also while paused)
constexpr uint32_t kSceneFight = 3;
// The fight's pause menu: a word that is 1 while it is open, in a block allocated at boot. Its address moves between
// boots; the global below always points 0x47C bytes after it.
constexpr uint32_t kPauseBasePtr = 0x8264C230;
constexpr uint32_t kPauseOffset = 0x47C;
// 2 while a fight runs (also paused), 0 once it is over and the end-of-fight menu (Fight Again / Save Replay) is up;
// found on the Battle mode, 2026-11-15. Read as "over" only after it was seen non-zero in the same fight, so a mode
// that never sets it keeps the old behaviour.
constexpr uint32_t kBattleWord = 0x82614B0C;

bool Committed(rex::memory::Memory* memory, uint32_t address) {
  auto* heap = memory->LookupHeap(address);
  rex::memory::HeapAllocationInfo info{};
  return heap && heap->QueryRegionInfo(address, &info) && (info.state & rex::memory::kMemoryAllocationCommit);
}

uint32_t Load(rex::memory::Memory* memory, uint32_t address) {
  return rex::memory::load_and_swap<uint32_t>(memory->TranslateVirtual<uint8_t*>(address));
}

}  // namespace

// -1 = no game yet, 0 = menus, 1 = fight, 2 = fight with a menu open (the pause menu or the end-of-fight menu). Cheap: two loads, and a page check
// once a second (the words are only read once their pages are mapped).
int rb2_fight_state() {
  rex::memory::Memory* memory = GuestMemory();
  if (!memory) return -1;
  static std::atomic<int64_t> checked{0};
  static std::atomic<bool> scene_ok{false};
  static std::atomic<uint32_t> pause_word{0};
  static std::atomic<bool> battle_ok{false};
  static std::atomic<bool> battle_seen{false};
  const int64_t now = std::chrono::duration_cast<std::chrono::milliseconds>(
                          std::chrono::steady_clock::now().time_since_epoch()).count();
  if (now - checked.load(std::memory_order_relaxed) > 1000) {
    checked.store(now, std::memory_order_relaxed);
    scene_ok.store(Committed(memory, kSceneWord), std::memory_order_relaxed);
    battle_ok.store(Committed(memory, kBattleWord), std::memory_order_relaxed);
    uint32_t pause = 0;
    if (Committed(memory, kPauseBasePtr)) {
      const uint32_t base = Load(memory, kPauseBasePtr);
      if (base > kPauseOffset && Committed(memory, base - kPauseOffset)) pause = base - kPauseOffset;
    }
    pause_word.store(pause, std::memory_order_relaxed);
  }
  if (!scene_ok.load(std::memory_order_relaxed)) return -1;
  if (Load(memory, kSceneWord) != kSceneFight) {
    battle_seen.store(false, std::memory_order_relaxed);
    return 0;
  }
  if (battle_ok.load(std::memory_order_relaxed)) {
    if (Load(memory, kBattleWord) != 0) {
      battle_seen.store(true, std::memory_order_relaxed);
    } else if (battle_seen.load(std::memory_order_relaxed)) {
      return 2;  // the fight is over: end-of-fight menu
    }
  }
  const uint32_t pause = pause_word.load(std::memory_order_relaxed);
  return pause && Load(memory, pause) == 1 ? 2 : 1;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dfdx047_dragonblast_game_NativeGameState_nativeFightState(JNIEnv*, jclass) {
  return rb2_fight_state();
}

long long rb2_dump_memory(const char* cpath, unsigned jstart, unsigned jend) {
  rex::memory::Memory* memory = GuestMemory();
  if (!memory) return -1;
  const std::string path = cpath;
  FILE* f = std::fopen(path.c_str(), "wb");
  if (!f) return -2;
  std::fwrite("RB2D", 1, 4, f);
  const uint32_t start = static_cast<uint32_t>(jstart);
  const uint32_t end = static_cast<uint32_t>(jend);
  uint64_t written = 0;
  uint32_t addr = start;
  while (addr < end) {
    auto* heap = memory->LookupHeap(addr);
    rex::memory::HeapAllocationInfo info{};
    if (!heap || !heap->QueryRegionInfo(addr, &info) || info.region_size == 0) {
      addr = (addr + 0x1000) & ~0xFFFu;
      continue;
    }
    uint64_t region_end = uint64_t(info.base_address) + info.region_size;
    if (region_end > end) region_end = end;
    const uint32_t from = addr > info.base_address ? addr : info.base_address;
    const uint32_t size = static_cast<uint32_t>(region_end - from);
    if ((info.state & rex::memory::kMemoryAllocationCommit) && (info.protect & rex::memory::kMemoryProtectRead) &&
        size) {
      std::fwrite(&from, 4, 1, f);
      std::fwrite(&size, 4, 1, f);
      std::fwrite(memory->TranslateVirtual<uint8_t*>(from), 1, size, f);
      written += size;
    }
    if (region_end <= addr) break;
    addr = static_cast<uint32_t>(region_end);
  }
  std::fclose(f);
  LOGI("memory dump %08X-%08X: %llu bytes -> %s", start, end, (unsigned long long)written, path.c_str());
  return static_cast<long long>(written);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_dfdx047_dragonblast_game_NativeGameState_nativeDumpMemory(JNIEnv* env, jclass, jstring jpath, jint jstart,
                                                                   jint jend) {
  return rb2_dump_memory(JString(env, jpath).c_str(), static_cast<unsigned>(jstart), static_cast<unsigned>(jend));
}
