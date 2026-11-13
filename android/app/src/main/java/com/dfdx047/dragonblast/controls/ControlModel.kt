package com.dfdx047.dragonblast.controls

import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.ControlScheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Inputs of the virtual pad. FORWARD/BACKWARD are relative to the fighter (towards / away from the opponent): in
 * Raging Blast 2's behind-the-back camera they become left stick up / down (HelperEngine). RS_* push the right stick
 * (Super Attacks).
 */
enum class PadInput {
    A, B, X, Y, LB, RB, LT, RT, BACK, START, L3, R3,
    UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD,
    RS_UP, RS_RIGHT, RS_DOWN, RS_LEFT,
}

/** One step of a sequence: these inputs for [frames] 60 Hz ticks. */
data class MacroStep(val inputs: Set<PadInput>, val frames: Int)

/** What an on-screen button does. */
sealed class ControlAction {
    /** Holds these pad inputs while the finger is on it (one or several = a combination). */
    data class Press(val inputs: Set<PadInput>) : ControlAction()

    /** Plays the steps on press; with [holdLast] the last step stays held while the finger is down. */
    data class Sequence(val steps: List<MacroStep>, val holdLast: Boolean) : ControlAction()
}

/**
 * BUTTON: a disc. STICK / DPAD: the left thumb. RADIAL: a disc whose drag direction picks one of four actions
 * ([ControlElement.radial]: up, right, down, left); a plain tap plays the first.
 */
enum class ElementKind { BUTTON, STICK, DPAD, RADIAL }

/** How the face buttons are drawn: Dragon Blast icons, or the letters / symbols of a console pad. */
enum class ControlStyle { DRAGON, XBOX, PLAYSTATION, NINTENDO }

/** Small corner badge naming the pad button behind an icon (Dragon style). */
enum class ButtonBadge { NONE, XBOX, PLAYSTATION, NINTENDO }

/** Left thumb: analog stick or 8-way D-pad. */
enum class Directional { STICK, DPAD }

/**
 * Touch tuning of a preset. [hitScale] = how far outside a disc a touch still counts (1 = the disc itself);
 * [dynamicStick] = the stick is centred where the thumb lands on its half of the screen; [swipe] = sliding a finger
 * from one button onto another moves the press (with hysteresis, see TouchControlsView).
 */
data class TouchFeel(val hitScale: Float, val dynamicStick: Boolean, val swipe: Boolean) {
    companion object {
        val PRO = TouchFeel(1.28f, dynamicStick = true, swipe = true)
        val NORMAL = TouchFeel(1.15f, dynamicStick = true, swipe = false)
        val CASUAL = TouchFeel(1.22f, dynamicStick = false, swipe = false)
    }
}

/** Icon drawn on a button (white vector drawables, tinted when drawn). */
enum class ControlIcon(val drawable: Int) {
    PUNCH(R.drawable.ic_btn_punch),
    KICK(R.drawable.ic_btn_kick),
    KI(R.drawable.ic_btn_ki),
    GUARD(R.drawable.ic_btn_guard),
    CHARGE(R.drawable.ic_btn_charge),
    GRAB(R.drawable.ic_btn_grab),
    TRANSFORM(R.drawable.ic_btn_transform),
    HYPER(R.drawable.ic_btn_hyper),
    COUNTER(R.drawable.ic_btn_counter),
    SWAP(R.drawable.ic_btn_swap),
    START(R.drawable.ic_btn_start),
    BACK(R.drawable.ic_btn_back),
    MACRO(R.drawable.ic_btn_macro),
    SMASH(R.drawable.ic_btn_smash),
    AURA(R.drawable.ic_btn_aura),
    BLOW(R.drawable.ic_btn_blow),
    PURSUIT(R.drawable.ic_btn_pursuit),
    CAMERA(R.drawable.ic_btn_camera),
    SUPER(R.drawable.ic_btn_super),
    ULTIMATE(R.drawable.ic_btn_ultimate),
    DASH(R.drawable.ic_btn_dash),
    LOCKON(R.drawable.ic_btn_lockon),
    ASCEND(R.drawable.ic_btn_ascend),
    DESCEND(R.drawable.ic_btn_descend),
    VANISH(R.drawable.ic_btn_vanish),
}

/** One element of a layout. Position = centre as a fraction of the (landscape) screen; size = diameter in dp. */
data class ControlElement(
    val id: String,
    val kind: ElementKind,
    val action: ControlAction,
    val icon: ControlIcon,
    val label: String,
    val x: Float,
    val y: Float,
    val sizeDp: Float,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    /** RADIAL only: the actions for up, right, down, left. */
    val radial: List<ControlAction> = emptyList(),
)

/** [menu] = the layout outside fights (A/B/X/Y with the pad's symbols); otherwise the fight layout. */
data class ControlLayout(
    val elements: List<ControlElement>,
    val version: Int = GameActions.LAYOUT_VERSION,
    val menu: Boolean = false,
    val map: Boolean = false,
) {
    val kind: LayoutKind get() = if (menu) LayoutKind.MENU else LayoutKind.GAMEPLAY
}

enum class LayoutKind { GAMEPLAY, MENU, MAP }

/**
 * Raging Blast 2 (Xbox 360) in fights, from the in-game command list as transcribed in the GameFAQs guides
 * (kliqIMB, GhostOfLegault) and the PSNProfiles tutorial table:
 *
 *  left stick move (tap near the opponent: steps) · X melee, hold = charge attack · Y ki blast, after X…X = smash ·
 *  B signature skill · A dash (no direction: homing dash) · LB jump / ascend · LT descend · RB guard (timed: snap
 *  vanishing, deflect; at the High Tension prompt: Raging Soul) · RB+X sway · RB+A vanishing · RB+Y counter ·
 *  RB+B throw · right stick up/right/down/left: the four Super Attacks · R3 Ultimate (High Tension) · D-pad down
 *  (hold) ki charge · RT (hold) the switch / transformation / fusion diamond, X/Y/B/A pick a slot · Start pause.
 *
 * Three touch schemes share those inputs:
 *  - RAGING_BLAST: the pad as the game was designed, Super Attacks on a radial disc (drag = right stick direction).
 *  - MODERN: one tap per action, Sparking! Zero style: sway / vanishing / counter / throw get their own buttons, plus
 *    Ultimate, ki charge, ascend and descend; the transform / switch diamond on one button.
 *  - TENKAICHI: Budokai Tenkaichi 3 positions (□ melee, △ ki, ○ guard, ✕ dash, R1/R2 up/down, L2 charge,
 *    Blast 1 / Blast 2 / Ultimate, R3 transform) sending the matching Raging Blast 2 inputs.
 */
object GameActions {
    const val LAYOUT_VERSION = 1

    private fun press(vararg i: PadInput) = ControlAction.Press(setOf(*i))

    val MELEE = press(PadInput.X)
    val KI_BLAST = press(PadInput.Y)
    val SIGNATURE = press(PadInput.B)
    val DASH = press(PadInput.A)
    val GUARD = press(PadInput.RB)
    val ASCEND = press(PadInput.LB)
    val DESCEND = press(PadInput.LT)
    val SWAY = press(PadInput.RB, PadInput.X)
    val VANISHING = press(PadInput.RB, PadInput.A)
    val COUNTER = press(PadInput.RB, PadInput.Y)
    val THROW = press(PadInput.RB, PadInput.B)
    val ULTIMATE = press(PadInput.R3)
    val KI_CHARGE = press(PadInput.DOWN)
    val DIAMOND = press(PadInput.RT)
    val SUPER_RISING = press(PadInput.LB, PadInput.LT)
    val SUPERS = listOf(press(PadInput.RS_UP), press(PadInput.RS_RIGHT), press(PadInput.RS_DOWN), press(PadInput.RS_LEFT))

    private const val CX = 0.875f
    private const val CY = 0.66f

    private fun stick() = ControlElement("stick", ElementKind.STICK, press(), ControlIcon.MACRO, "", 0.15f, 0.64f, 150f)
    private fun dpad() = ControlElement("dpad", ElementKind.DPAD, press(), ControlIcon.MACRO, "", 0.15f, 0.64f, 150f)
    private fun btn(id: String, a: ControlAction, icon: ControlIcon, label: String, x: Float, y: Float, size: Float) =
        ControlElement(id, ElementKind.BUTTON, a, icon, label, x, y, size)
    private fun supers(x: Float, y: Float, size: Float) =
        ControlElement("supers", ElementKind.RADIAL, SUPERS[0], ControlIcon.SUPER, "RS", x, y, size, radial = SUPERS)
    /** Start / Back, and the fight ⇄ menus layout switch between them (the overlay handles [TOGGLE]). */
    private fun system() = listOf(
        btn("back", press(PadInput.BACK), ControlIcon.BACK, "BACK", 0.36f, 0.08f, 40f),
        btn(TOGGLE, press(), ControlIcon.SWAP, "", 0.5f, 0.08f, 36f),
        btn("start", press(PadInput.START), ControlIcon.START, "START", 0.64f, 0.08f, 40f),
    )

    /** Id of the button that switches between the fight and the menu layout. */
    const val TOGGLE = "layout_toggle"

    /** The pad as designed: face buttons, guard, flight, the Super Attack disc, Ultimate, charge, the diamond. */
    fun ragingBlastLayout() = ControlLayout(
        listOf(
            stick(), dpad(),
            btn("melee", MELEE, ControlIcon.PUNCH, "X", CX - 0.075f, CY, 62f),
            btn("ki", KI_BLAST, ControlIcon.KI, "Y", CX, CY - 0.18f, 62f),
            btn("signature", SIGNATURE, ControlIcon.HYPER, "B", CX + 0.075f, CY, 62f),
            btn("dash", DASH, ControlIcon.DASH, "A", CX, CY + 0.18f, 62f),
            btn("guard", GUARD, ControlIcon.GUARD, "RB", 0.705f, 0.74f, 56f),
            btn("ascend", ASCEND, ControlIcon.ASCEND, "LB", 0.70f, 0.47f, 48f),
            btn("descend", DESCEND, ControlIcon.DESCEND, "LT", 0.745f, 0.27f, 46f),
            supers(0.84f, 0.21f, 58f),
            btn("diamond", DIAMOND, ControlIcon.TRANSFORM, "RT", 0.945f, 0.27f, 48f),
            btn("ultimate", ULTIMATE, ControlIcon.ULTIMATE, "R3", 0.965f, 0.45f, 44f),
            btn("charge", KI_CHARGE, ControlIcon.CHARGE, "↓", 0.30f, 0.88f, 46f),
        ) + system(),
    )

    /** One tap per action: defensive moves and flight on their own buttons, no button combinations to learn. */
    fun modernLayout() = ControlLayout(
        listOf(
            stick(), dpad(),
            btn("melee", MELEE, ControlIcon.PUNCH, "X", CX - 0.075f, CY, 64f),
            btn("ki", KI_BLAST, ControlIcon.KI, "Y", CX, CY - 0.18f, 62f),
            btn("signature", SIGNATURE, ControlIcon.HYPER, "B", CX + 0.075f, CY, 62f),
            btn("dash", DASH, ControlIcon.DASH, "A", CX, CY + 0.18f, 62f),
            btn("guard", GUARD, ControlIcon.GUARD, "RB", 0.70f, 0.76f, 60f),
            btn("vanishing", VANISHING, ControlIcon.VANISH, "RB+A", 0.695f, 0.52f, 46f),
            btn("counter", COUNTER, ControlIcon.COUNTER, "RB+Y", 0.755f, 0.33f, 44f),
            btn("throw", THROW, ControlIcon.GRAB, "RB+B", 0.615f, 0.90f, 44f),
            supers(0.85f, 0.21f, 58f),
            btn("ultimate", ULTIMATE, ControlIcon.ULTIMATE, "R3", 0.955f, 0.40f, 50f),
            btn("diamond", DIAMOND, ControlIcon.TRANSFORM, "RT", 0.955f, 0.18f, 46f),
            btn("ascend", ASCEND, ControlIcon.ASCEND, "LB", 0.31f, 0.52f, 50f),
            btn("descend", DESCEND, ControlIcon.DESCEND, "LT", 0.31f, 0.78f, 50f),
            btn("charge", KI_CHARGE, ControlIcon.CHARGE, "↓", 0.07f, 0.28f, 50f),
        ) + system(),
    )

    /**
     * Budokai Tenkaichi 3 hands: □ melee (left), △ ki (top), ○ guard (right), ✕ dash (bottom); R1 / R2 ascend /
     * descend on the right shoulder, L2 ki charge on the left; Blast 1 = signature skill, Blast 2 = the Super Attack
     * disc, Ultimate, R3 transform.
     */
    /**
     * Budokai Tenkaichi 3: a PS2 pad in BT3's places (□ melee, △ ki, ○ guard, ✕ dash, L1 lock-on, L2 charge and
     * Blasts, R1 / R2 up / down, R3 transform). It sends the plain buttons; the game side
     * (android/native/src/bt3_controls.cpp) turns them into Raging Blast 2 moves during fights, the same way it does
     * for a physical controller. Blast 1 / Blast 2 / Ultimate are L2 combinations, here also as one-tap buttons.
     */
    fun tenkaichiLayout() = ControlLayout(
        listOf(
            stick(), dpad(),
            btn("bt3_square", press(PadInput.X), ControlIcon.PUNCH, "□", CX - 0.075f, CY, 62f),
            btn("bt3_triangle", press(PadInput.Y), ControlIcon.KI, "△", CX, CY - 0.18f, 62f),
            btn("bt3_circle", press(PadInput.B), ControlIcon.GUARD, "○", CX + 0.075f, CY, 62f),
            btn("bt3_cross", press(PadInput.A), ControlIcon.DASH, "✕", CX, CY + 0.18f, 62f),
            btn("bt3_r1", press(PadInput.RB), ControlIcon.ASCEND, "R1", 0.955f, 0.30f, 48f),
            btn("bt3_r2", press(PadInput.RT), ControlIcon.DESCEND, "R2", 0.955f, 0.13f, 44f),
            btn("bt3_l1", press(PadInput.LB), ControlIcon.LOCKON, "L1", 0.705f, 0.30f, 46f),
            btn("bt3_l2", press(PadInput.LT), ControlIcon.CHARGE, "L2", 0.705f, 0.13f, 46f),
            btn("bt3_r3", press(PadInput.R3), ControlIcon.TRANSFORM, "R3", 0.705f, 0.76f, 46f),
            btn("bt3_blast1", press(PadInput.LT, PadInput.B), ControlIcon.HYPER, "L2+○", 0.80f, 0.13f, 44f),
            btn("bt3_blast2", press(PadInput.LT, PadInput.Y), ControlIcon.SUPER, "L2+△", 0.835f, 0.30f, 48f),
            btn("bt3_ultimate", press(PadInput.LT, PadInput.BACKWARD, PadInput.Y), ControlIcon.ULTIMATE, "L2+↓△", 0.615f, 0.90f, 44f),
        ) + system(),
    )

    fun fightLayout(scheme: ControlScheme): ControlLayout = when (scheme) {
        ControlScheme.RAGING_BLAST -> ragingBlastLayout()
        ControlScheme.MODERN -> modernLayout()
        ControlScheme.TENKAICHI -> tenkaichiLayout()
    }

    /** Menus: A confirm, B back, X / Y and the shoulders page through lists, Start / Back. */
    fun menuLayout(): ControlLayout {
        fun pad(id: String, input: PadInput, x: Float, y: Float, size: Float) =
            ControlElement(id, ElementKind.BUTTON, press(input), ControlIcon.MACRO, classicLabel(input, false), x, y, size)
        return ControlLayout(
            listOf(
                stick(), dpad(),
                pad("m_x", PadInput.X, CX - 0.075f, CY, 58f),
                pad("m_y", PadInput.Y, CX, CY - 0.18f, 58f),
                pad("m_b", PadInput.B, CX + 0.075f, CY, 58f),
                pad("m_a", PadInput.A, CX, CY + 0.18f, 66f),
                pad("m_lb", PadInput.LB, 0.70f, 0.40f, 46f),
                pad("m_rb", PadInput.RB, 0.955f, 0.40f, 46f),
                pad("m_lt", PadInput.LT, 0.70f, 0.20f, 42f),
                pad("m_rt", PadInput.RT, 0.955f, 0.20f, 42f),
                btn("m_back", press(PadInput.BACK), ControlIcon.BACK, "BACK", 0.36f, 0.08f, 40f),
                btn(TOGGLE, press(), ControlIcon.SWAP, "", 0.5f, 0.08f, 36f),
                btn("m_start", press(PadInput.START), ControlIcon.START, "START", 0.64f, 0.08f, 40f),
            ),
            menu = true,
        )
    }

    fun defaultLayout(kind: LayoutKind, scheme: ControlScheme): ControlLayout =
        if (kind == LayoutKind.GAMEPLAY) fightLayout(scheme) else menuLayout()

    /** Extra buttons the editor can add to any fight layout. */
    val HELPERS: List<Triple<String, ControlAction, ControlIcon>> = listOf(
        Triple("sway", SWAY, ControlIcon.PURSUIT),
        Triple("vanishing", VANISHING, ControlIcon.VANISH),
        Triple("counter", COUNTER, ControlIcon.COUNTER),
        Triple("throw", THROW, ControlIcon.GRAB),
        Triple("ultimate", ULTIMATE, ControlIcon.ULTIMATE),
        Triple("charge", KI_CHARGE, ControlIcon.CHARGE),
        Triple("ascend", ASCEND, ControlIcon.ASCEND),
        Triple("descend", DESCEND, ControlIcon.DESCEND),
        Triple("rising", SUPER_RISING, ControlIcon.HYPER),
        Triple("diamond", DIAMOND, ControlIcon.TRANSFORM),
        Triple("super1", SUPERS[0], ControlIcon.SUPER),
        Triple("super2", SUPERS[1], ControlIcon.SUPER),
        Triple("super3", SUPERS[2], ControlIcon.SUPER),
        Triple("super4", SUPERS[3], ControlIcon.SUPER),
    )

    /** Label of a pad input on an Xbox, PlayStation or Nintendo pad (Nintendo: by position, A on the right). */
    fun classicLabel(input: PadInput, playstation: Boolean, nintendo: Boolean = false): String = when (input) {
        PadInput.A -> if (playstation) "✕" else if (nintendo) "B" else "A"
        PadInput.B -> if (playstation) "○" else if (nintendo) "A" else "B"
        PadInput.X -> if (playstation) "□" else if (nintendo) "Y" else "X"
        PadInput.Y -> if (playstation) "△" else if (nintendo) "X" else "Y"
        PadInput.LB -> if (playstation) "L1" else if (nintendo) "L" else "LB"
        PadInput.RB -> if (playstation) "R1" else if (nintendo) "R" else "RB"
        PadInput.LT -> if (playstation) "L2" else if (nintendo) "ZL" else "LT"
        PadInput.RT -> if (playstation) "R2" else if (nintendo) "ZR" else "RT"
        PadInput.BACK -> if (playstation) "SELECT" else if (nintendo) "−" else "BACK"
        PadInput.START -> if (nintendo) "+" else "START"
        PadInput.L3 -> "L3"
        PadInput.R3 -> "R3"
        PadInput.UP -> "↑"
        PadInput.DOWN -> "↓"
        PadInput.LEFT -> "←"
        PadInput.RIGHT -> "→"
        PadInput.FORWARD -> "▲"
        PadInput.BACKWARD -> "▼"
        PadInput.RS_UP -> "R↑"
        PadInput.RS_RIGHT -> "R→"
        PadInput.RS_DOWN -> "R↓"
        PadInput.RS_LEFT -> "R←"
    }
}

/** Layouts edited by the player, as JSON in the app's internal folder: one file per scheme and kind. */
object ControlLayoutStore {

    private fun file(kind: LayoutKind, scheme: ControlScheme) = java.io.File(
        AppPaths.controls,
        if (kind == LayoutKind.GAMEPLAY) "fight_${if (scheme == ControlScheme.TENKAICHI) "bt3pad" else scheme.key}.json" else "menu.json",
    )

    fun load(kind: LayoutKind, scheme: ControlScheme): ControlLayout {
        val f = file(kind, scheme)
        if (!f.isFile) return GameActions.defaultLayout(kind, scheme)
        return try {
            fromJson(JSONObject(f.readText())).copy(menu = kind != LayoutKind.GAMEPLAY)
        } catch (e: Exception) {
            GameActions.defaultLayout(kind, scheme)
        }
    }

    fun save(layout: ControlLayout, scheme: ControlScheme) {
        val f = file(layout.kind, scheme)
        f.parentFile?.mkdirs()
        val tmp = java.io.File(f.parentFile, f.name + ".tmp")
        tmp.writeText(toJson(layout).toString(2))
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun reset(kind: LayoutKind, scheme: ControlScheme) {
        file(kind, scheme).delete()
    }

    fun resetAll() {
        AppPaths.controls.listFiles()?.forEach { it.delete() }
    }

    private fun inputsToJson(s: Set<PadInput>) = JSONArray().apply { s.forEach { put(it.name) } }
    private fun inputsFromJson(a: JSONArray?): Set<PadInput> =
        if (a == null) emptySet() else (0 until a.length()).mapNotNull { i ->
            runCatching { PadInput.valueOf(a.getString(i)) }.getOrNull()
        }.toSet()

    private fun actionToJson(a: ControlAction): JSONObject = when (a) {
        is ControlAction.Press -> JSONObject().put("type", "press").put("inputs", inputsToJson(a.inputs))
        is ControlAction.Sequence -> JSONObject().put("type", "sequence").put("holdLast", a.holdLast)
            .put("steps", JSONArray().apply {
                a.steps.forEach { put(JSONObject().put("inputs", inputsToJson(it.inputs)).put("frames", it.frames)) }
            })
    }

    private fun actionFromJson(o: JSONObject): ControlAction = when (o.optString("type")) {
        "sequence" -> {
            val steps = o.optJSONArray("steps") ?: JSONArray()
            ControlAction.Sequence(
                (0 until steps.length()).map { i ->
                    val s = steps.getJSONObject(i)
                    MacroStep(inputsFromJson(s.optJSONArray("inputs")), s.optInt("frames", 2).coerceIn(1, 600))
                },
                o.optBoolean("holdLast", false),
            )
        }
        else -> ControlAction.Press(inputsFromJson(o.optJSONArray("inputs")))
    }

    fun toJson(layout: ControlLayout): JSONObject = JSONObject().put("version", layout.version).put(
        "elements",
        JSONArray().apply {
            layout.elements.forEach { e ->
                put(
                    JSONObject()
                        .put("id", e.id).put("kind", e.kind.name).put("action", actionToJson(e.action))
                        .put("icon", e.icon.name).put("label", e.label)
                        .put("x", e.x.toDouble()).put("y", e.y.toDouble()).put("size", e.sizeDp.toDouble())
                        .put("opacity", e.opacity.toDouble()).put("visible", e.visible)
                        .put("radial", JSONArray().apply { e.radial.forEach { put(actionToJson(it)) } }),
                )
            }
        },
    )

    fun fromJson(o: JSONObject): ControlLayout {
        val arr = o.getJSONArray("elements")
        val list = (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            val radial = e.optJSONArray("radial")
            ControlElement(
                id = e.getString("id"),
                kind = runCatching { ElementKind.valueOf(e.getString("kind")) }.getOrDefault(ElementKind.BUTTON),
                action = actionFromJson(e.optJSONObject("action") ?: JSONObject()),
                icon = runCatching { ControlIcon.valueOf(e.optString("icon")) }.getOrDefault(ControlIcon.MACRO),
                label = e.optString("label"),
                x = e.optDouble("x", 0.5).toFloat().coerceIn(0f, 1f),
                y = e.optDouble("y", 0.5).toFloat().coerceIn(0f, 1f),
                sizeDp = e.optDouble("size", 60.0).toFloat().coerceIn(28f, 260f),
                opacity = e.optDouble("opacity", 1.0).toFloat().coerceIn(0.05f, 1f),
                visible = e.optBoolean("visible", true),
                radial = if (radial == null) emptyList() else (0 until radial.length()).map { actionFromJson(radial.getJSONObject(it)) },
            )
        }
        return ControlLayout(list, o.optInt("version", 1))
    }
}
