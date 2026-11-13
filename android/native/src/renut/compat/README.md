# Renut compatibility layer

Files from the ReXGlue SDK fork Dragon Burst builds on (iExplosiveRage/rexglue-sdk, commit 465f9ef, BSD-3-Clause;
`include/rex/perf/frame_rate.h`, `include/rex/graphics/draw_overrides.h`, `include/rex/graphics/frame_pacing.h`, `include/rex/graphics/wide_aspect.h` and
their `src/core` sources). The Renut includes these headers; Dragon Blast's SDK (v0.10.0 + its own patches) has no
equivalent, so they are compiled into libmain.so next to the Renut. `app_dir.h` stands in for that fork's
`rex::GetAndroidAppDirectory()`.
