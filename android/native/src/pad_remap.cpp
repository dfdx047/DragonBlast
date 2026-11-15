// Physical controller remapping for fights (launcher: Controls → Physical controller; one profile per fight scheme).
//
// Runs on each physical pad's own state before player 1's devices are merged (SDK patch 0047), so the on-screen
// touch pad is never touched, and before the Tenkaichi 3 conversion (bt3_controls.cpp): with that scheme the player
// remaps the BT3 buttons. Only while a fight runs and is not paused: menus keep the controller's own buttons.
//
// The 16 inputs, in this order: A B X Y LB RB LT RT BACK START L3 R3 UP DOWN LEFT RIGHT.
//   rb2_pad_remap      nibble i (bits 4i..4i+3) = the input physical input i acts as (identity 0xFEDCBA9876543210)
//   rb2_pad_remap_off  bit i set = physical input i does nothing
// Numbers rather than a string: the launcher's menu changes them live while the input thread reads them.

#include <android/log.h>

#include <cstdint>

#include <rex/cvar.h>
#include <rex/input/input.h>
#include <rex/input/state_filter.h>

REXCVAR_DEFINE_UINT64(rb2_pad_remap, 0xFEDCBA9876543210ull, "Dragon Blast",
                      "Physical controller remap in fights: nibble i = the input physical input i acts as");
REXCVAR_DEFINE_UINT32(rb2_pad_remap_off, 0, "Dragon Blast",
                      "Physical controller remap in fights: bit i = physical input i does nothing");

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "DragonBlast", __VA_ARGS__)

int rb2_fight_state();  // game_state_jni.cpp: -1 no game yet, 0 menus, 1 fight, 2 fight paused

namespace {

constexpr uint64_t kIdentity = 0xFEDCBA9876543210ull;
constexpr int kInputs = 16;
constexpr int kLT = 6, kRT = 7;
constexpr uint8_t kTriggerOn = 64;

// XInput button bit of each input (0 for the two triggers).
constexpr uint16_t kBit[kInputs] = {
    0x1000, 0x2000, 0x4000, 0x8000,  // A B X Y
    0x0100, 0x0200, 0, 0,            // LB RB LT RT
    0x0020, 0x0010, 0x0040, 0x0080,  // BACK START L3 R3
    0x0001, 0x0002, 0x0004, 0x0008,  // UP DOWN LEFT RIGHT
};

void Remap(uint32_t user_index, bool synthetic, rex::input::X_INPUT_STATE* state) {
  if (synthetic || user_index != 0) return;
  const uint64_t map = REXCVAR_GET(rb2_pad_remap);
  const uint32_t off = REXCVAR_GET(rb2_pad_remap_off);
  static bool was_on = false;
  const bool on = (map != kIdentity || off != 0) && rb2_fight_state() == 1;
  if (on != was_on) {
    LOGI("pad remap %s (map %016llX, off %04X)", on ? "on (fight)" : "off", (unsigned long long)map, off);
    was_on = on;
  }
  if (!on) return;

  auto& pad = state->gamepad;
  // How far each input is pressed, 0..255 (buttons: 0 or 255; triggers: their analog value).
  uint8_t level[kInputs];
  for (int i = 0; i < kInputs; i++) level[i] = (pad.buttons & kBit[i]) ? 255 : 0;
  level[kLT] = pad.left_trigger;
  level[kRT] = pad.right_trigger;

  uint16_t buttons = pad.buttons & ~uint16_t(0xF3FF);  // keep the bits that are not one of the 16 inputs
  uint8_t lt = 0, rt = 0;
  for (int i = 0; i < kInputs; i++) {
    if (!level[i] || (off & (1u << i))) continue;
    const int to = int((map >> (4 * i)) & 0xF);
    if (to == kLT) {
      if (level[i] > lt) lt = level[i];
    } else if (to == kRT) {
      if (level[i] > rt) rt = level[i];
    } else if ((i != kLT && i != kRT) || level[i] >= kTriggerOn) {
      buttons |= kBit[to];
    }
  }
  pad.buttons = buttons;
  pad.left_trigger = lt;
  pad.right_trigger = rt;
}

struct Install {
  Install() { rex::input::SetDeviceStateFilter(&Remap); }
} g_install;

}  // namespace
