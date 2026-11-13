package com.dfdx047.dragonblast.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

/** How the game window is held. */
enum class Orientation(val key: String) { LANDSCAPE("landscape"), PORTRAIT("portrait"), SENSOR("sensor") }

/** Frame rate modes, same numbers as LaunchConfig.fps_mode (only the 60 modes: RB2's logic runs per frame). */
enum class FpsMode(val code: Int) { SIXTY(2), SIXTY_FG(3) }

enum class Upscaler(val key: String) { BILINEAR("bilinear"), FSR("fsr") }

/** Which renderer draws the game. Renut (native Direct3D -> Vulkan) is being written for this game. */
enum class Renderer(val key: String) { XENIA("xenia"), RENUT("renut") }

enum class TouchMode(val key: String) { OFF("off"), AUTO("auto"), ALWAYS("always") }

/** The three ready-made on-screen controllers. */
enum class TouchPreset(val key: String) { PRO("pro"), NORMAL("normal"), CASUAL("casual") }

/** Look of the on-screen face buttons. */
enum class ButtonStyle(val key: String) { DRAGON("dragon"), XBOX("xbox"), PLAYSTATION("playstation"), NINTENDO("nintendo") }

/** Fight controls: Raging Blast 2's own, or Budokai Tenkaichi 3's layout (fights only, menus untouched). */
enum class ControlScheme(val key: String) { RAGING_BLAST("rb2"), TENKAICHI("bt3") }

enum class StickMode(val key: String) { FIXED("fixed"), DYNAMIC("dynamic") }

enum class Haptics(val key: String) { OFF("off"), LIGHT("light"), STRONG("strong") }

/** App language: the phone's (pt -> Portuguese, es -> Spanish, anything else -> English) or a fixed one. */
enum class AppLanguage(val tag: String) { SYSTEM(""), EN("en"), PT("pt-BR"), ES("es") }

data class Settings(
    // Home
    val orientation: Orientation = Orientation.LANDSCAPE,
    // Graphics
    val renderScale: Int = 100,
    val upscaler: Upscaler = Upscaler.FSR,
    val fpsMode: FpsMode = FpsMode.SIXTY,
    val renderer: Renderer = Renderer.XENIA,
    val showFps: Boolean = false,
    val pauseInBackground: Boolean = true,
    val stereoAudio: Boolean = true,
    // Controls
    val touchMode: TouchMode = TouchMode.AUTO,
    val touchPreset: TouchPreset = TouchPreset.NORMAL,
    val buttonStyle: ButtonStyle = ButtonStyle.DRAGON,
    val controlScheme: ControlScheme = ControlScheme.RAGING_BLAST,
    val touchOpacity: Int = 70,
    val touchScale: Int = 100,
    val stickMode: StickMode = StickMode.DYNAMIC,
    val stickDeadZone: Int = 12,
    val swipeButtons: Boolean = false,
    val leftHanded: Boolean = false,
    val haptics: Haptics = Haptics.LIGHT,
    val nintendoLayout: Boolean = false,
    // Debug
    val verboseLog: Boolean = false,
    val gpuStats: Boolean = false,
    val extraArgs: String = "",
    // App
    val language: AppLanguage = AppLanguage.SYSTEM,
)

/**
 * Stored in SharedPreferences "options", the file the game shell (org.rb2recomp.android.LaunchConfig) reads
 * when it starts: render_scale, fsr, fps_mode, show_fps and gpu_driver keep RB2-Android's names and meaning.
 * The rest are Dragon Blast's own keys, read by the game shell as it grows (touch controls, overlay).
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("options", Context.MODE_PRIVATE)
    private val app = context.applicationContext

    fun load(): Settings {
        val d = Settings(renderScale = defaultRenderScale(), touchPreset = TouchPreset.NORMAL)
        fun <E : Enum<E>> e(key: String, values: Array<E>, def: E, keyOf: (E) -> String) =
            prefs.getString(key, null)?.let { s -> values.firstOrNull { keyOf(it) == s } } ?: def
        return Settings(
            orientation = e("orientation", Orientation.entries.toTypedArray(), d.orientation) { it.key },
            renderScale = prefs.getInt("render_scale", d.renderScale),
            upscaler = if (prefs.getBoolean("fsr", true)) Upscaler.FSR else Upscaler.BILINEAR,
            fpsMode = if (prefs.getInt("fps_mode", 2) == 3) FpsMode.SIXTY_FG else FpsMode.SIXTY,
            renderer = e("renderer", Renderer.entries.toTypedArray(), d.renderer) { it.key },
            showFps = prefs.getBoolean("show_fps", false),
            pauseInBackground = prefs.getBoolean("pause_background", true),
            stereoAudio = prefs.getBoolean("audio_stereo", true),
            touchMode = e("touch_mode", TouchMode.entries.toTypedArray(), d.touchMode) { it.key },
            touchPreset = e("touch_preset", TouchPreset.entries.toTypedArray(), d.touchPreset) { it.key },
            buttonStyle = e("button_style", ButtonStyle.entries.toTypedArray(), d.buttonStyle) { it.key },
            controlScheme = e("control_scheme", ControlScheme.entries.toTypedArray(), d.controlScheme) { it.key },
            touchOpacity = prefs.getInt("touch_opacity", d.touchOpacity),
            touchScale = prefs.getInt("touch_scale", d.touchScale),
            stickMode = e("stick_mode", StickMode.entries.toTypedArray(), d.stickMode) { it.key },
            stickDeadZone = prefs.getInt("stick_dead_zone", d.stickDeadZone),
            swipeButtons = prefs.getBoolean("swipe_buttons", false),
            leftHanded = prefs.getBoolean("left_handed", false),
            haptics = e("haptics", Haptics.entries.toTypedArray(), d.haptics) { it.key },
            nintendoLayout = prefs.getBoolean("nintendo_layout", false),
            verboseLog = prefs.getBoolean("verbose_log", false),
            gpuStats = prefs.getBoolean("gpu_stats", false),
            extraArgs = prefs.getString("extra_args", "") ?: "",
            language = e("language", AppLanguage.entries.toTypedArray(), d.language) { it.name },
        )
    }

    fun save(s: Settings) {
        prefs.edit()
            .putString("orientation", s.orientation.key)
            .putInt("render_scale", s.renderScale)
            .putBoolean("fsr", s.upscaler == Upscaler.FSR)
            .putInt("fps_mode", s.fpsMode.code)
            .putString("renderer", s.renderer.key)
            .putBoolean("show_fps", s.showFps)
            .putBoolean("pause_background", s.pauseInBackground)
            .putBoolean("audio_stereo", s.stereoAudio)
            .putString("touch_mode", s.touchMode.key)
            .putString("touch_preset", s.touchPreset.key)
            .putString("button_style", s.buttonStyle.key)
            .putString("control_scheme", s.controlScheme.key)
            .putInt("touch_opacity", s.touchOpacity)
            .putInt("touch_scale", s.touchScale)
            .putString("stick_mode", s.stickMode.key)
            .putInt("stick_dead_zone", s.stickDeadZone)
            .putBoolean("swipe_buttons", s.swipeButtons)
            .putBoolean("left_handed", s.leftHanded)
            .putString("haptics", s.haptics.key)
            .putBoolean("nintendo_layout", s.nintendoLayout)
            .putBoolean("verbose_log", s.verboseLog)
            .putBoolean("gpu_stats", s.gpuStats)
            .putString("extra_args", s.extraArgs)
            .putString("language", s.language.name)
            .apply()
    }

    fun reset() {
        val keep = prefs.getString("language", null)
        prefs.edit().clear().apply()
        if (keep != null) prefs.edit().putString("language", keep).apply()
    }

    /**
     * Starting resolution (RB2-Android's rule): handhelds with a fan render at 100 %, high-end Adreno phones
     * at 85 %, everything else at 75 %; FSR 1 scales the picture back up.
     */
    private fun defaultRenderScale(): Int {
        val maker = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        if (listOf("ayn", "retroid", "ayaneo", "anbernic", "odin").any { it in maker }) return 100
        val gpu = GpuInfo.get(app)
        return if (gpu.adrenoGeneration >= 7) 85 else 75
    }

    companion object {
        val RENDER_SCALES = listOf(50, 60, 75, 85, 100)

        /** Internal resolution at a scale (the game renders 1280x720); same rounding as src/rb2_resolution.h. */
        fun renderSize(scale: Int): String {
            val s = minOf(100, scale)
            val w = maxOf(640, (1280 * s / 100 + 8) and 15.inv())
            val h = maxOf(360, (720 * s / 100 + 8) and 15.inv())
            return "${w}x$h"
        }
    }
}

/** Touch preset numbers: what Pro, Normal and Casual change (read by the touch controls). */
data class PresetTuning(
    val buttonHitScale: Float,
    val stickRadiusDp: Int,
    val stickMode: StickMode,
    val swipe: Boolean,
    val holdToCharge: Boolean,
)

fun TouchPreset.tuning(): PresetTuning = when (this) {
    // Competitive: dynamic stick that appears under the thumb anywhere on the left half, bigger touch areas,
    // sliding between buttons for strings, no automatic holds.
    TouchPreset.PRO -> PresetTuning(1.25f, 74, StickMode.DYNAMIC, swipe = true, holdToCharge = false)
    TouchPreset.NORMAL -> PresetTuning(1.10f, 70, StickMode.DYNAMIC, swipe = false, holdToCharge = false)
    // Relaxed: fixed stick, large buttons, charge and guard as toggles.
    TouchPreset.CASUAL -> PresetTuning(1.20f, 78, StickMode.FIXED, swipe = false, holdToCharge = true)
}
