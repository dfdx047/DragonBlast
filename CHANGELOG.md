# Changelog

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
  - Switches automatically between the menu and fight layouts.
- **Drivers.** No driver comes inside the APK any more. Download Turnip (Adreno) or PanVK (Mali) in the Drivers tab;
  Adreno 6xx phones such as the Mi A3 need a Turnip for Renut.
- **Launcher.** In Portuguese, English and Spanish, and fits square screens (RG Cube).
