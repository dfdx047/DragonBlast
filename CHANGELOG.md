# Changelog

## 0.3.1 — 2026-11-19
Not tested on a device yet (none was free): built and checked on the PC only.

- **Game did not open (#1, #2, #4).**
  - Adreno 6xx on the phone's own driver (Snapdragon 860/865, Adreno 640/650) lacks what the Renut needs. The game
    closed; it now runs on Xenia instead, and Graphics → Renderer says why. A Turnip from the Drivers tab brings the
    Renut back.
  - Since 0.3.0 Xenia could not start on any phone ("GPU plugin 'xenos' not found"), and drivers from the Drivers tab
    (Turnip) failed to load ("the driver found no GPU"): both looked for their files next to the unpacked game instead
    of in the APK's library folder. Fixed.
- **Renut fixes from Zandatsu** (same renderer):
  - Mali-G710/G720/G925 (POCO X7 Pro, X8 Pro, Pixel 7, Dimensity 9000–9400): picture cut to a corner. Viewport and
    scissor are set again in each render pass, the screen output has its own command list, and each scissor change on
    three-digit Mali opens its own pass.
  - Mali-G615 crash while compiling shaders and PowerVR BXM crash at start: the shader the driver dies on is recorded,
    pipelines are then compiled one at a time, and one that kills the driver twice is skipped.
  - Infinite shader constants become the largest finite value (0 × inf = 0 as on the Xbox 360; black sky on Mali in
    Zandatsu). May also fix the missing water on Adreno (#3), still to confirm.
  - The log names the Vulkan driver and its version.
- **Xenia:** occlusion queries off outside Adreno (the Mali-G57 driver crashes in them).
- **Graphics → Renderer:** on Adreno without a Turnip driver, a warning says to use Turnip for the Renut, with a button to the Drivers tab.
- **Touch controls: swipe buttons.** Fewer buttons on screen: touch, slide towards a direction, lift.
  - Transform: holds RT (Tenkaichi 3: R3) and the slide picks the diamond slot (↑ Y/△, → B/○, ↓ A/✕, ← X/□).
  - Super Attacks (Tenkaichi 3: L2 + △): tap = Super Attack 1, slide ↑ = Ultimate, → / ↓ / ← = Super Attacks 2, 3, 4.
  - Guard (Tenkaichi 3: ○): hold to block, slide ↑ counter, → throw, ↓ vanishing, ← sway.
  - Flight: slide ↑ to ascend, ↓ to descend.
  - Raging Blast goes from 11 to 9 fight buttons, Tenkaichi 3 from 12 to 9 (the L1 that did nothing is gone).
  - Everything stays editable: the editor adds any swipe or one-tap button and sets what each direction, the tap and
    the hold do. Fight layouts saved by 0.3.0 are replaced by the new defaults once (menu layouts stay).
  - Tenkaichi 3 on a physical controller: with R3 held, ○ now picks the diamond's right slot (it was guard).

## 0.3.0 — 2026-11-15
First public release.

- **Universal APK.**
  - One APK for every ARM64 phone, with an Armv8.2 engine and an Armv8.0 engine (CPUs without LSE atomics, e.g.
    Snapdragon 665), picked at run time.
  - The recompiled game is encrypted with a key that comes from your own `default.xex`, and is unpacked on the first
    Play after you import your disc.
- **Updates.** The launcher looks for new versions on GitHub (More → About → Updates) and installs them over the old
  one, keeping the game, saves and settings.
- **Renderers.** Renut, the native Direct3D renderer, is the default; Xenia stays as an alternative.
  - Renut has a resolution choice from 360p to 4K, bilinear, FSR 1 and CAS filters, and bloom and shadow toggles.
  - The in-game menu changes all of them live.
- **Weak devices** start at 360p with bloom and shadows off: Adreno below 630, entry Mali (G51/G52/G57/G71/G72) and
  Armv8.0 CPUs. 120 FPS is hidden on 60 Hz screens.
- **Touch controls.**
  - Three fight schemes: Raging Blast, Modern and Tenkaichi 3.
  - Pro, Normal and Casual layouts, and an editor.
  - Switches automatically between the menu and fight layouts, also on the end-of-fight menu.
  - New default layouts: the shoulder actions sit in an arc around the four main buttons, and keep their shape on
    wide (20:9) and square (RG Cube) screens.
  - Controls → "How the controls work": every on-screen button of each scheme, numbered on a drawing.
- **Physical controller remap.** Controls → Physical controller → Remap buttons: a drawing of the pad, one profile for
  Raging Blast and one for Tenkaichi 3, swap A/B X/Y, defaults. It applies in fights only; menus keep the pad's own
  buttons, and the touch controls are never remapped.
- **Drivers.** No driver comes inside the APK any more. Download Turnip (Adreno) or PanVK (Mali) in the Drivers tab;
  Adreno 6xx phones such as the Mi A3 need a Turnip for Renut.
- **Launcher.** In Portuguese, English and Spanish, and fits square screens (RG Cube).
