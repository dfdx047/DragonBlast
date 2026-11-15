package com.dfdx047.dragonblast.controls

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.ControlScheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The 16 remappable inputs of a physical controller, in the order android/native/src/pad_remap.cpp uses (nibble i
 * of the rb2_pad_remap cvar = the input physical input i acts as). The sticks are not remapped.
 */
enum class PadKey { A, B, X, Y, LB, RB, LT, RT, BACK, START, L3, R3, UP, DOWN, LEFT, RIGHT;

    val padInput: PadInput get() = PadInput.valueOf(name)

    companion object {
        /** Android key code → input, the same way SDL's Android joystick driver reads them (SDL_sysjoystick.c). */
        fun ofKeyCode(code: Int): PadKey? = when (code) {
            KeyEvent.KEYCODE_BUTTON_A -> A
            KeyEvent.KEYCODE_BUTTON_B -> B
            KeyEvent.KEYCODE_BUTTON_X -> X
            KeyEvent.KEYCODE_BUTTON_Y -> Y
            KeyEvent.KEYCODE_BUTTON_L1 -> LB
            KeyEvent.KEYCODE_BUTTON_R1 -> RB
            KeyEvent.KEYCODE_BUTTON_L2 -> LT
            KeyEvent.KEYCODE_BUTTON_R2 -> RT
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK -> BACK
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU -> START
            KeyEvent.KEYCODE_BUTTON_THUMBL -> L3
            KeyEvent.KEYCODE_BUTTON_THUMBR -> R3
            KeyEvent.KEYCODE_DPAD_UP -> UP
            KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
            else -> null
        }

        /** Inputs held according to a joystick motion event: analog triggers and the hat (D-pad as axes). */
        fun ofMotion(e: MotionEvent): Set<PadKey> {
            val out = mutableSetOf<PadKey>()
            if (maxOf(e.getAxisValue(MotionEvent.AXIS_LTRIGGER), e.getAxisValue(MotionEvent.AXIS_BRAKE)) > 0.5f) out += LT
            if (maxOf(e.getAxisValue(MotionEvent.AXIS_RTRIGGER), e.getAxisValue(MotionEvent.AXIS_GAS)) > 0.5f) out += RT
            val hx = e.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
            if (hx < -0.5f) out += LEFT
            if (hx > 0.5f) out += RIGHT
            if (hy < -0.5f) out += UP
            if (hy > 0.5f) out += DOWN
            return out
        }
    }
}

/** What an input does in a fight under one scheme, for the remap screen and the controls guide. */
data class PadAction(val key: PadKey, val icon: ControlIcon?, val name: Int, val desc: Int)

/**
 * Remap profiles, one per fight scheme: the Modern scheme is touch-only, so with it a physical pad plays Raging
 * Blast 2 and uses that profile. A profile maps each physical input to the input it acts as (-1 = nothing); edits
 * swap, so every action keeps one button unless the player clears it.
 */
object PadRemap {
    val IDENTITY: IntArray get() = IntArray(PadKey.entries.size) { it }

    /** The profile a scheme uses: "rb2" or "bt3". */
    fun profileOf(scheme: ControlScheme) = if (scheme == ControlScheme.TENKAICHI) "bt3" else "rb2"
    private fun profileOfKey(schemeKey: String?) = if (schemeKey == "bt3") "bt3" else "rb2"

    /** Raging Blast 2's own commands (GameActions), in the order the screen lists them. */
    val RAGING_BLAST = listOf(
        PadAction(PadKey.X, ControlIcon.PUNCH, R.string.act_melee, R.string.act_melee_rb2),
        PadAction(PadKey.Y, ControlIcon.KI, R.string.act_ki, R.string.act_ki_rb2),
        PadAction(PadKey.B, ControlIcon.HYPER, R.string.act_signature, R.string.act_signature_rb2),
        PadAction(PadKey.A, ControlIcon.DASH, R.string.act_dash, R.string.act_dash_rb2),
        PadAction(PadKey.RB, ControlIcon.GUARD, R.string.act_guard, R.string.act_guard_rb2),
        PadAction(PadKey.LB, ControlIcon.ASCEND, R.string.act_ascend, R.string.act_ascend_rb2),
        PadAction(PadKey.LT, ControlIcon.DESCEND, R.string.act_descend, R.string.act_descend_rb2),
        PadAction(PadKey.RT, ControlIcon.TRANSFORM, R.string.act_diamond, R.string.act_diamond_rb2),
        PadAction(PadKey.R3, ControlIcon.ULTIMATE, R.string.act_ultimate, R.string.act_ultimate_rb2),
        PadAction(PadKey.DOWN, ControlIcon.CHARGE, R.string.act_charge, R.string.act_charge_rb2),
        PadAction(PadKey.START, ControlIcon.START, R.string.act_pause, R.string.act_pause_desc),
        PadAction(PadKey.BACK, ControlIcon.BACK, R.string.act_back, R.string.act_game_button),
        PadAction(PadKey.L3, null, R.string.act_l3, R.string.act_game_button),
        PadAction(PadKey.UP, null, R.string.act_up, R.string.act_game_button),
        PadAction(PadKey.LEFT, null, R.string.act_left, R.string.act_game_button),
        PadAction(PadKey.RIGHT, null, R.string.act_right, R.string.act_game_button),
    )

    /** Budokai Tenkaichi 3's buttons (bt3_controls.cpp turns them into Raging Blast 2 commands). */
    val TENKAICHI = listOf(
        PadAction(PadKey.X, ControlIcon.PUNCH, R.string.act_melee, R.string.act_melee_bt3),
        PadAction(PadKey.Y, ControlIcon.KI, R.string.act_ki, R.string.act_ki_bt3),
        PadAction(PadKey.B, ControlIcon.GUARD, R.string.act_guard, R.string.act_guard_bt3),
        PadAction(PadKey.A, ControlIcon.DASH, R.string.act_dash, R.string.act_dash_bt3),
        PadAction(PadKey.LT, ControlIcon.CHARGE, R.string.act_charge_blasts, R.string.act_charge_bt3),
        PadAction(PadKey.RB, ControlIcon.ASCEND, R.string.act_ascend, R.string.act_ascend_bt3),
        PadAction(PadKey.RT, ControlIcon.DESCEND, R.string.act_descend, R.string.act_descend_bt3),
        PadAction(PadKey.R3, ControlIcon.TRANSFORM, R.string.act_transform, R.string.act_transform_bt3),
        PadAction(PadKey.L3, ControlIcon.TRANSFORM, R.string.act_transform, R.string.act_transform_bt3),
        PadAction(PadKey.LB, ControlIcon.LOCKON, R.string.act_lockon, R.string.act_lockon_bt3),
        PadAction(PadKey.START, ControlIcon.START, R.string.act_pause, R.string.act_pause_desc),
        PadAction(PadKey.BACK, ControlIcon.BACK, R.string.act_select, R.string.act_game_button),
        PadAction(PadKey.DOWN, null, R.string.act_down, R.string.act_game_button),
        PadAction(PadKey.UP, null, R.string.act_up, R.string.act_game_button),
        PadAction(PadKey.LEFT, null, R.string.act_left, R.string.act_game_button),
        PadAction(PadKey.RIGHT, null, R.string.act_right, R.string.act_game_button),
    )

    fun actions(profile: String) = if (profile == "bt3") TENKAICHI else RAGING_BLAST

    /** Button label: Tenkaichi 3 uses the PlayStation symbols it was made with, Raging Blast 2 the Xbox ones. */
    fun glyph(key: PadKey, profile: String) = GameActions.classicLabel(key.padInput, playstation = profile == "bt3")

    private fun file(context: Context) = File(context.filesDir, "controls/pad_remap.json")

    fun load(context: Context, profile: String): IntArray = runCatching {
        val a = JSONObject(file(context).readText()).getJSONArray(profile)
        val m = IntArray(PadKey.entries.size) { i -> a.optInt(i, i) }
        if (m.all { it in -1 until PadKey.entries.size }) m else IDENTITY
    }.getOrDefault(IDENTITY)

    fun save(context: Context, profile: String, map: IntArray) {
        val f = file(context)
        f.parentFile?.mkdirs()
        val o = runCatching { JSONObject(f.readText()) }.getOrDefault(JSONObject())
        o.put(profile, JSONArray().apply { map.forEach { put(it) } })
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(o.toString())
        if (!tmp.renameTo(f)) {
            f.writeText(o.toString())
            tmp.delete()
        }
    }

    /** The physical input that does [action], or null. */
    fun physicalOf(map: IntArray, action: PadKey): PadKey? =
        map.indexOf(action.ordinal).takeIf { it >= 0 }?.let { PadKey.entries[it] }

    /** [action] moves to [physical]; whatever that button did moves to the button [action] had (a swap). */
    fun assign(map: IntArray, action: PadKey, physical: PadKey?): IntArray {
        val m = map.copyOf()
        val old = m.indexOf(action.ordinal)
        if (physical == null) {
            if (old >= 0) m[old] = -1
            return m
        }
        val displaced = m[physical.ordinal]
        m[physical.ordinal] = action.ordinal
        if (old >= 0 && old != physical.ordinal) m[old] = displaced
        return m
    }

    /** A/B and X/Y swapped: a Nintendo-layout pad by position. */
    fun nintendo(): IntArray = IDENTITY.also { m ->
        m[PadKey.A.ordinal] = PadKey.B.ordinal
        m[PadKey.B.ordinal] = PadKey.A.ordinal
        m[PadKey.X.ordinal] = PadKey.Y.ordinal
        m[PadKey.Y.ordinal] = PadKey.X.ordinal
    }

    fun changes(map: IntArray) = map.withIndex().count { (i, v) -> v != i }

    /** rb2_pad_remap: nibble i = the input physical input i acts as (decimal, as the SDK's cvar parser reads). */
    fun cvarMap(map: IntArray): String {
        var v = 0L
        for (i in map.indices) v = v or ((if (map[i] < 0) i else map[i]).toLong() and 0xF shl (4 * i))
        return java.lang.Long.toUnsignedString(v)
    }

    /** rb2_pad_remap_off: bit i = physical input i does nothing. */
    fun cvarOff(map: IntArray): String = map.withIndex().filter { it.value < 0 }.sumOf { 1 shl it.index }.toString()

    /** Launch options for the scheme saved in "options" control_scheme (LaunchConfig). */
    @JvmStatic
    fun launchArgs(context: Context, schemeKey: String?): List<String> {
        val m = load(context, profileOfKey(schemeKey))
        return listOf("--rb2_pad_remap=" + cvarMap(m), "--rb2_pad_remap_off=" + cvarOff(m))
    }
}
