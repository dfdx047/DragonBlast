// Budokai Tenkaichi 3 controls for Raging Blast 2 fights (cvar rb2_bt3_controls, launcher scheme "Tenkaichi 3").
//
// Player 1's merged controller state (physical pads and the on-screen controls, SDK patch 0042) is read as a
// PS2 pad in BT3's places and rewritten into Raging Blast 2's commands before the game sees it:
//
//   BT3 (button position on an Xbox pad)        Raging Blast 2
//   □ melee (X)                                 X  melee
//   △ ki blast (Y)                              Y  ki blast
//   ○ guard (B)                                 RB guard (○+□ sway, ○+✕ vanishing, ○+△ counter as in RB2)
//   ✕ dash (A)                                  A  dash
//   R1 up (RB) / R2 down (RT)                   LB ascend / LT descend (both = Super Rising)
//   L2 held alone (LT)                          d-pad down held: ki charge
//   L2 + ○                                      B  Blast 1 → signature skill
//   L2 + △ (neutral / ↑ / → / ←)                right stick up / right / down / left: Super Attacks 1-4
//   L2 + ↓ + △                                  R3 Ultimate Attack
//   L2 + ✕ / L2 + □                             A / X
//   R3 or L3                                    RT held: switch / transform / fusion diamond
//   L1 lock-on                                  (Raging Blast 2 locks on by itself: nothing)
//   Start, Back, d-pad, right stick             unchanged
//
// Only while a fight runs and is not paused (scene word and pause flag found by comparing memory dumps, see
// docs/ROADMAP.md): menus, the pause menu and the character select keep the game's own buttons.

#include <android/log.h>

#include <chrono>
#include <cstdint>

#include <rex/cvar.h>
#include <rex/input/input.h>
#include <rex/input/state_filter.h>

REXCVAR_DEFINE_BOOL(rb2_bt3_controls, false, "Dragon Blast",
                    "Budokai Tenkaichi 3 controls in fights (player 1)");

REXCVAR_DEFINE_BOOL(rb2_bt3_log, false, "Dragon Blast", "Log every Tenkaichi 3 input change (debug)");

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "DragonBlast", __VA_ARGS__)

namespace {

// XInput buttons.
constexpr uint16_t kUp = 0x0001, kDown = 0x0002, kLeft = 0x0004, kRight = 0x0008;
constexpr uint16_t kStart = 0x0010, kBack = 0x0020, kL3 = 0x0040, kR3 = 0x0080;
constexpr uint16_t kLB = 0x0100, kRB = 0x0200, kA = 0x1000, kB = 0x2000, kX = 0x4000, kY = 0x8000;
constexpr uint8_t kTriggerOn = 64;
constexpr int16_t kStickDir = 16000;
constexpr int16_t kFull = 32767;

constexpr int64_t kChargeDelayMs = 180;  // L2 alone this long = ki charge (shorter: maybe a combo is coming)
constexpr int64_t kComboHoldMs = 120;    // a combo's output stays at least this long (the game reads per frame)

enum class Combo { kNone, kSignature, kSuper1, kSuper2, kSuper3, kSuper4, kUltimate, kDash, kMelee };

struct State {
  uint16_t prev_buttons = 0;
  int64_t l2_since = 0;       // when L2 went down (0 = up)
  bool l2_used = false;       // a combo fired during this L2 hold: no charge until L2 is released
  Combo combo = Combo::kNone;
  uint16_t combo_key = 0;     // the face button that started it
  int64_t combo_until = 0;
  bool active = false;        // last call rewrote the state (for the log)
};
State g;

int64_t NowMs() {
  return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now().time_since_epoch())
      .count();
}

}  // namespace

int rb2_fight_state();  // game_state_jni.cpp: -1 no game yet, 0 menus, 1 fight, 2 fight paused

namespace {

void Filter(uint32_t user_index, rex::input::X_INPUT_STATE* state) {
  if (REXCVAR_GET(rb2_bt3_log)) {
    static int64_t window = 0;
    static int calls[4] = {};
    if (user_index < 4) calls[user_index]++;
    const int64_t t = NowMs();
    if (t - window >= 2000) {
      LOGI("bt3 reads/2s: %d %d %d %d, fight state %d", calls[0], calls[1], calls[2], calls[3], rb2_fight_state());
      window = t;
      calls[0] = calls[1] = calls[2] = calls[3] = 0;
    }
  }
  if (user_index != 0 || !REXCVAR_GET(rb2_bt3_controls) || rb2_fight_state() != 1) {
    if (g.active) LOGI("Tenkaichi 3 controls: off (menus or pause)");
    g = State{};
    return;
  }
  if (!g.active) LOGI("Tenkaichi 3 controls: on (fight)");
  g.active = true;

  auto& pad = state->gamepad;
  const uint16_t in = pad.buttons;
  const bool l2 = pad.left_trigger >= kTriggerOn;
  const bool r2 = pad.right_trigger >= kTriggerOn;
  const int16_t lx = pad.thumb_lx, ly = pad.thumb_ly;
  const uint16_t pressed = in & ~g.prev_buttons;
  g.prev_buttons = in;
  const int64_t now = NowMs();

  uint16_t out = in & (kUp | kDown | kLeft | kRight | kStart | kBack);
  uint8_t out_lt = 0, out_rt = 0;
  int16_t out_lx = lx, out_ly = ly;
  int16_t out_rx = pad.thumb_rx, out_ry = pad.thumb_ry;

  if (l2) {
    if (!g.l2_since) g.l2_since = now;
    // A face button pressed with L2 held starts a combo.
    Combo next = Combo::kNone;
    uint16_t key = 0;
    if (pressed & kY) {
      key = kY;
      if (ly <= -kStickDir) next = Combo::kUltimate;
      else if (ly >= kStickDir) next = Combo::kSuper2;
      else if (lx >= kStickDir) next = Combo::kSuper3;
      else if (lx <= -kStickDir) next = Combo::kSuper4;
      else next = Combo::kSuper1;
    } else if (pressed & kB) {
      key = kB;
      next = Combo::kSignature;
    } else if (pressed & kA) {
      key = kA;
      next = Combo::kDash;
    } else if (pressed & kX) {
      key = kX;
      next = Combo::kMelee;
    }
    if (next != Combo::kNone) {
      g.combo = next;
      g.combo_key = key;
      g.combo_until = now + kComboHoldMs;
      g.l2_used = true;
    }
    // L2 alone, held: ki charge. The stick does not move the fighter while L2 is down (it picks the Blast).
    if (!g.l2_used && g.combo == Combo::kNone && now - g.l2_since >= kChargeDelayMs) out |= kDown;
    out_lx = 0;
    out_ly = 0;
  } else {
    g.l2_since = 0;
    g.l2_used = false;
  }

  // A running combo: its output while its button is held, and at least kComboHoldMs.
  if (g.combo != Combo::kNone && !(in & g.combo_key) && now >= g.combo_until) g.combo = Combo::kNone;
  switch (g.combo) {
    case Combo::kSignature: out |= kB; break;
    case Combo::kSuper1: out_rx = 0; out_ry = kFull; break;
    case Combo::kSuper2: out_rx = kFull; out_ry = 0; break;
    case Combo::kSuper3: out_rx = 0; out_ry = -kFull; break;
    case Combo::kSuper4: out_rx = -kFull; out_ry = 0; break;
    case Combo::kUltimate: out |= kR3; break;
    case Combo::kDash: out |= kA; break;
    case Combo::kMelee: out |= kX; break;
    case Combo::kNone: break;
  }

  if (!l2) {
    // Plain buttons, BT3 → RB2.
    if (in & kX) out |= kX;     // □ melee
    if (in & kY) out |= kY;     // △ ki blast
    if (in & kB) out |= kRB;    // ○ guard
    if (in & kA) out |= kA;     // ✕ dash
  }
  if (in & kRB) out |= kLB;      // R1 ascend
  if (r2) out_lt = 255;          // R2 descend
  if (in & (kR3 | kL3)) out_rt = 255;  // R3 / L3 transform: the diamond
  // L1 (lock-on) has no Raging Blast 2 command.

  if (REXCVAR_GET(rb2_bt3_log)) {
    static uint32_t last_in = ~0u, last_out = ~0u;
    const uint32_t key_in = in | (l2 << 16) | (r2 << 17);
    const uint32_t key_out = out | (uint32_t(out_lt != 0) << 16) | (uint32_t(out_rt != 0) << 17) |
                             (uint32_t(out_ry > 0) << 18) | (uint32_t(out_rx > 0) << 19);
    if (key_in != last_in || key_out != last_out) {
      LOGI("bt3 in %05X lx %d ly %d -> out %05X (l2 %lld ms, used %d, combo %d)", key_in, lx, ly, key_out,
           g.l2_since ? (long long)(now - g.l2_since) : -1LL, g.l2_used, int(g.combo));
      last_in = key_in;
      last_out = key_out;
    }
  }

  pad.buttons = out;
  pad.left_trigger = out_lt;
  pad.right_trigger = out_rt;
  pad.thumb_lx = out_lx;
  pad.thumb_ly = out_ly;
  pad.thumb_rx = out_rx;
  pad.thumb_ry = out_ry;
}

struct Install {
  Install() { rex::input::SetStateFilter(&Filter); }
} g_install;

}  // namespace
