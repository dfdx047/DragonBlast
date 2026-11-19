package com.dfdx047.dragonblast.controls

import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.ControlScheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Inputs of the virtual pad. FORWARD/BACKWARD are relative to the fighter (towards / away from the opponent): in
 * Raging Blast 2's behind-the-back camera they become left stick up / down (HelperEngine); STICK_LEFT / STICK_RIGHT
 * push the left stick sideways (the Tenkaichi 3 scheme's L2 + →△ / ←△). RS_* push the right stick (Super Attacks).
 */
enum class PadInput {
    A, B, X, Y, LB, RB, LT, RT, BACK, START, L3, R3,
    UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD,
    RS_UP, RS_RIGHT, RS_DOWN, RS_LEFT,
    STICK_LEFT, STICK_RIGHT,
}

/**
 * Layout x (0..1) ↔ pixels. Layouts are drawn for a 16:9 screen; elsewhere each side keeps its distance from its own
 * edge in units of the screen height, so on wider phones (20:9) the thumb clusters stay by the edges and on narrow or
 * square screens (RG Cube) they keep their shape instead of being squeezed. The middle third stretches between them.
 * On square screens the reference narrows (K) so the middle keeps some room.
 */
object ScreenMap {
    private const val REF = 16f / 9f
    private const val K = 0.85f
    private const val EDGE = 0.35f

    private fun ref(w: Float, h: Float) = minOf(h * REF, w / K)

    fun toPx(x: Float, w: Float, h: Float): Float {
        if (w <= 0f || h <= 0f) return x * w
        val r = ref(w, h)
        val a = EDGE * r
        val b = w - EDGE * r
        return when {
            x <= EDGE -> x * r
            x >= 1f - EDGE -> w - (1f - x) * r
            else -> a + (b - a) * (x - EDGE) / (1f - 2 * EDGE)
        }
    }

    fun fromPx(px: Float, w: Float, h: Float): Float {
        if (w <= 0f || h <= 0f) return px / maxOf(w, 1f)
        val r = ref(w, h)
        val a = EDGE * r
        val b = w - EDGE * r
        return when {
            px <= a -> px / r
            px >= b -> 1f - (w - px) / r
            else -> EDGE + (1f - 2 * EDGE) * (px - a) / (b - a)
        }.coerceIn(0f, 1f)
    }
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
 * BUTTON: a disc. STICK / DPAD: the left thumb. RADIAL: a swipe button. While the finger is on it, its
 * [ControlElement.action] is held (e.g. RT, which opens the transformation diamond; may be empty); sliding the finger
 * towards ↑ → ↓ ← adds that direction's action ([ControlElement.radial]: up, right, down, left) for as long as it
 * stays there; lifting it without having slid plays [ControlElement.tap]. One button instead of five.
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
    /** RADIAL only: the actions for up, right, down, left (an empty Press = nothing that way). */
    val radial: List<ControlAction> = emptyList(),
    /** RADIAL only: what a tap without sliding plays (null = nothing beyond [action]). */
    val tap: ControlAction? = null,
    /** RADIAL only: short marks drawn at up, right, down, left and (5th) under the icon for the tap. */
    val hints: List<String> = emptyList(),
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
 * A phone has two thumbs and no shoulder buttons, so the default layouts fold the button combinations into swipe
 * buttons (ElementKind.RADIAL): touch, slide towards a direction, lift. The direction always matches the place of the
 * face button it adds on the pad's diamond (↑ Y / △, → B / ○, ↓ A / ✕, ← X / □):
 *  - Transform: holds RT (R3 in Tenkaichi 3) and slides to the diamond slot.
 *  - Super Attacks: tap = Super 1; ↑ Ultimate; → / ↓ / ← Super 2 / 3 / 4.
 *  - Guard: holds RB (○ in Tenkaichi 3); ↑ counter, → throw, ↓ vanishing, ← sway.
 *  - Flight: ↑ ascend, ↓ descend, held while the finger stays there.
 *
 * Three touch schemes share those inputs:
 *  - RAGING_BLAST: the game's buttons, combinations on the swipe buttons.
 *  - MODERN: the same plus one-tap vanishing and counter (no slide for the two most used defensive moves).
 *  - TENKAICHI: Budokai Tenkaichi 3 positions (□ melee, △ ki, ○ guard, ✕ dash, R1 / R2 flight, L2 charge, R3
 *    transform) sending the PS2 buttons; android/native/src/bt3_controls.cpp turns them into Raging Blast 2 moves.
 */
object GameActions {
    /**
     * 2: x goes through [ScreenMap]. Layouts saved as 1 keep x as a plain fraction of the width, as drawn.
     * 3: swipe buttons (RADIAL with a held action, tap and hints). Fight layouts saved before 3 are replaced by the new
     *    defaults (ControlLayoutStore): their RADIAL meant something else and the old button walls are gone.
     */
    const val LAYOUT_VERSION = 3

    private fun press(vararg i: PadInput) = ControlAction.Press(setOf(*i))
    val NOTHING = ControlAction.Press(emptySet())

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

    // Tenkaichi 3 (PS2 buttons as sent: □ X, △ Y, ○ B, ✕ A, L1 LB, R1 RB, L2 LT, R2 RT).
    /** L2 + a stick direction + △: the direction first (2 frames), so bt3_controls.cpp reads it with the △. */
    private fun bt3Blast(dir: PadInput?): ControlAction =
        if (dir == null) press(PadInput.LT, PadInput.Y)
        else ControlAction.Sequence(
            listOf(MacroStep(setOf(PadInput.LT, dir), 2), MacroStep(setOf(PadInput.LT, dir, PadInput.Y), 8)),
            holdLast = false,
        )
    val BT3_BLAST1 = press(PadInput.LT, PadInput.B)
    val BT3_SUPER1 = bt3Blast(null)
    val BT3_SUPER2 = bt3Blast(PadInput.FORWARD)
    val BT3_SUPER3 = bt3Blast(PadInput.STICK_RIGHT)
    val BT3_SUPER4 = bt3Blast(PadInput.STICK_LEFT)
    val BT3_ULTIMATE = bt3Blast(PadInput.BACKWARD)

    private const val CX = 0.865f
    private const val CY = 0.68f

    // The arc around the face buttons, the same places in every scheme (16:9 reference, ScreenMap): from the
    // bottom-left of the diamond, where the thumb rests, up and over to the top-right corner.
    private const val S1X = 0.675f; private const val S1Y = 0.84f   // guard
    private const val S2X = 0.672f; private const val S2Y = 0.56f   // flight
    private const val S4X = 0.80f;  private const val S4Y = 0.30f   // Super Attacks
    private const val S5X = 0.945f; private const val S5Y = 0.34f   // switch / transform
    private const val S6X = 0.70f;  private const val S6Y = 0.32f   // extra (Modern: counter, BT3: Blast 1)

    private fun stick() = ControlElement("stick", ElementKind.STICK, press(), ControlIcon.MACRO, "", 0.15f, 0.64f, 150f)
    private fun dpad() = ControlElement("dpad", ElementKind.DPAD, press(), ControlIcon.MACRO, "", 0.15f, 0.64f, 150f)
    private fun btn(id: String, a: ControlAction, icon: ControlIcon, label: String, x: Float, y: Float, size: Float) =
        ControlElement(id, ElementKind.BUTTON, a, icon, label, x, y, size)

    private fun swipe(
        id: String, icon: ControlIcon, label: String, x: Float, y: Float, size: Float,
        hold: ControlAction, up: ControlAction, right: ControlAction, down: ControlAction, left: ControlAction,
        tap: ControlAction?, hints: List<String>,
    ) = ControlElement(id, ElementKind.RADIAL, hold, icon, label, x, y, size,
        radial = listOf(up, right, down, left), tap = tap, hints = hints)

    // Swipe buttons of the Raging Blast and Modern schemes (also offered by the editor).
    fun swipeSupers(x: Float = S4X, y: Float = S4Y, size: Float = 66f) = swipe(
        "swipe_supers", ControlIcon.SUPER, "RS", x, y, size, NOTHING,
        ULTIMATE, SUPERS[1], SUPERS[2], SUPERS[3], tap = SUPERS[0], hints = listOf("★", "2", "3", "4", "1"),
    )
    fun swipeTransform(x: Float = S5X, y: Float = S5Y, size: Float = 54f) = swipe(
        "swipe_transform", ControlIcon.TRANSFORM, "RT", x, y, size, DIAMOND,
        KI_BLAST, SIGNATURE, DASH, MELEE, tap = null, hints = listOf("Y", "B", "A", "X"),
    )
    fun swipeGuard(x: Float = S1X, y: Float = S1Y, size: Float = 62f) = swipe(
        "swipe_guard", ControlIcon.GUARD, "RB", x, y, size, GUARD,
        press(PadInput.Y), press(PadInput.B), press(PadInput.A), press(PadInput.X), tap = null,
        hints = listOf("Y", "B", "A", "X"),
    )
    fun swipeFlight(x: Float = S2X, y: Float = S2Y, size: Float = 54f) = swipe(
        "swipe_flight", ControlIcon.ASCEND, "LB/LT", x, y, size, NOTHING,
        ASCEND, NOTHING, DESCEND, NOTHING, tap = null, hints = listOf("▲", "", "▼", ""),
    )

    // Tenkaichi 3 swipe buttons.
    fun bt3SwipeSpecials(x: Float = S4X, y: Float = S4Y, size: Float = 66f) = swipe(
        "bt3_specials", ControlIcon.SUPER, "L2+△", x, y, size, NOTHING,
        BT3_ULTIMATE, BT3_SUPER2, BT3_SUPER3, BT3_SUPER4, tap = BT3_SUPER1, hints = listOf("★", "2", "3", "4", "1"),
    )
    fun bt3SwipeTransform(x: Float = S5X, y: Float = S5Y, size: Float = 54f) = swipe(
        "bt3_transform", ControlIcon.TRANSFORM, "R3", x, y, size, press(PadInput.R3),
        press(PadInput.Y), press(PadInput.B), press(PadInput.A), press(PadInput.X), tap = null,
        hints = listOf("△", "○", "✕", "□"),
    )
    fun bt3SwipeFlight(x: Float = S2X, y: Float = S2Y, size: Float = 54f) = swipe(
        "bt3_flight", ControlIcon.ASCEND, "R1/R2", x, y, size, NOTHING,
        press(PadInput.RB), NOTHING, press(PadInput.RT), NOTHING, tap = null, hints = listOf("R1", "", "R2", ""),
    )
    /** ○ is guard in Tenkaichi 3; sliding adds △ / ✕ / □ for counter, vanishing and sway (bt3_controls.cpp). */
    fun bt3SwipeGuard(x: Float, y: Float, size: Float) = swipe(
        "bt3_circle", ControlIcon.GUARD, "○", x, y, size, press(PadInput.B),
        press(PadInput.Y), NOTHING, press(PadInput.A), press(PadInput.X), tap = null,
        hints = listOf("△", "", "✕", "□"),
    )

    /** Start / Back, and the fight ⇄ menus layout switch between them (the overlay handles [TOGGLE]). */
    private fun system() = listOf(
        btn("back", press(PadInput.BACK), ControlIcon.BACK, "BACK", 0.36f, 0.08f, 40f),
        btn(TOGGLE, press(), ControlIcon.SWAP, "", 0.5f, 0.08f, 36f),
        btn("start", press(PadInput.START), ControlIcon.START, "START", 0.64f, 0.08f, 40f),
    )

    /** Id of the button that switches between the fight and the menu layout. */
    const val TOGGLE = "layout_toggle"

    /** The game's buttons; guard, flight, Super Attacks and the diamond as swipe buttons. 9 buttons + stick. */
    fun ragingBlastLayout() = ControlLayout(
        listOf(
            stick(), dpad(),
            btn("melee", MELEE, ControlIcon.PUNCH, "X", CX - 0.075f, CY, 64f),
            btn("ki", KI_BLAST, ControlIcon.KI, "Y", CX, CY - 0.17f, 62f),
            btn("signature", SIGNATURE, ControlIcon.HYPER, "B", CX + 0.075f, CY, 62f),
            btn("dash", DASH, ControlIcon.DASH, "A", CX, CY + 0.17f, 62f),
            swipeGuard(),
            swipeFlight(),
            swipeSupers(),
            swipeTransform(),
            btn("charge", KI_CHARGE, ControlIcon.CHARGE, "↓", 0.31f, 0.84f, 52f),
        ) + system(),
    )

    /** Raging Blast plus one-tap vanishing and counter, the two defensive moves used most. */
    fun modernLayout() = ControlLayout(
        listOf(
            stick(), dpad(),
            btn("melee", MELEE, ControlIcon.PUNCH, "X", CX - 0.075f, CY, 64f),
            btn("ki", KI_BLAST, ControlIcon.KI, "Y", CX, CY - 0.17f, 62f),
            btn("signature", SIGNATURE, ControlIcon.HYPER, "B", CX + 0.075f, CY, 62f),
            btn("dash", DASH, ControlIcon.DASH, "A", CX, CY + 0.17f, 62f),
            swipeGuard(),
            btn("vanishing", VANISHING, ControlIcon.VANISH, "RB+A", 0.50f, 0.90f, 48f),
            btn("counter", COUNTER, ControlIcon.COUNTER, "RB+Y", S6X - 0.12f, S6Y + 0.06f, 46f),
            swipeFlight(0.31f, 0.56f),
            swipeSupers(),
            swipeTransform(),
            btn("charge", KI_CHARGE, ControlIcon.CHARGE, "↓", 0.31f, 0.84f, 52f),
        ) + system(),
    )

    /**
     * Budokai Tenkaichi 3: a PS2 pad in BT3's places (□ melee, △ ki, ○ guard, ✕ dash; L2 for ki charge on the left,
     * where the left hand presses it on the pad). It sends the plain buttons; the game side
     * (android/native/src/bt3_controls.cpp) turns them into Raging Blast 2 moves during fights, the same way it does
     * for a physical controller. The L2 + △ Blasts, R1 / R2 and R3 + slot are swipe buttons.
     */
    fun tenkaichiLayout() = ControlLayout(
        listOf(
            stick(), dpad(),
            btn("bt3_square", press(PadInput.X), ControlIcon.PUNCH, "□", CX - 0.075f, CY, 64f),
            btn("bt3_triangle", press(PadInput.Y), ControlIcon.KI, "△", CX, CY - 0.17f, 62f),
            bt3SwipeGuard(CX + 0.075f, CY, 62f),
            btn("bt3_cross", press(PadInput.A), ControlIcon.DASH, "✕", CX, CY + 0.17f, 62f),
            bt3SwipeFlight(),
            bt3SwipeSpecials(),
            btn("bt3_blast1", BT3_BLAST1, ControlIcon.HYPER, "L2+○", S1X, S1Y, 56f),
            bt3SwipeTransform(),
            btn("bt3_l2", press(PadInput.LT), ControlIcon.CHARGE, "L2", 0.31f, 0.84f, 54f),
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

    /** Extra one-tap buttons the editor can add to a fight layout of the Raging Blast / Modern schemes. */
    val HELPERS: List<Triple<String, ControlAction, ControlIcon>> = listOf(
        Triple("sway", SWAY, ControlIcon.PURSUIT),
        Triple("vanishing", VANISHING, ControlIcon.VANISH),
        Triple("counter", COUNTER, ControlIcon.COUNTER),
        Triple("throw", THROW, ControlIcon.GRAB),
        Triple("guard", GUARD, ControlIcon.GUARD),
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

    /** Extra one-tap buttons for the Tenkaichi 3 scheme (BT3 buttons and combinations). */
    val BT3_HELPERS: List<Triple<String, ControlAction, ControlIcon>> = listOf(
        Triple("bt3_super1", BT3_SUPER1, ControlIcon.SUPER),
        Triple("bt3_super2", BT3_SUPER2, ControlIcon.SUPER),
        Triple("bt3_super3", BT3_SUPER3, ControlIcon.SUPER),
        Triple("bt3_super4", BT3_SUPER4, ControlIcon.SUPER),
        Triple("bt3_ultimate", BT3_ULTIMATE, ControlIcon.ULTIMATE),
        Triple("bt3_r1", press(PadInput.RB), ControlIcon.ASCEND),
        Triple("bt3_r2", press(PadInput.RT), ControlIcon.DESCEND),
        Triple("bt3_r3", press(PadInput.R3), ControlIcon.TRANSFORM),
        Triple("bt3_sway", press(PadInput.B, PadInput.X), ControlIcon.PURSUIT),
        Triple("bt3_vanishing", press(PadInput.B, PadInput.A), ControlIcon.VANISH),
        Triple("bt3_counter", press(PadInput.B, PadInput.Y), ControlIcon.COUNTER),
        Triple("bt3_rising", press(PadInput.RB, PadInput.RT), ControlIcon.HYPER),
    )

    fun helpers(scheme: ControlScheme) = if (scheme == ControlScheme.TENKAICHI) BT3_HELPERS else HELPERS

    /** Swipe buttons the editor can add (each id once). */
    fun swipeHelpers(scheme: ControlScheme): List<ControlElement> = if (scheme == ControlScheme.TENKAICHI) {
        listOf(bt3SwipeSpecials(0.5f, 0.5f), bt3SwipeTransform(0.5f, 0.5f), bt3SwipeFlight(0.5f, 0.5f),
            bt3SwipeGuard(0.5f, 0.5f, 60f))
    } else {
        listOf(swipeSupers(0.5f, 0.5f), swipeTransform(0.5f, 0.5f), swipeGuard(0.5f, 0.5f), swipeFlight(0.5f, 0.5f))
    }

    /** What a swipe button's hold / tap / direction can be set to in the editor, per scheme. */
    data class Choice(val key: String, val action: ControlAction, val hint: String)

    fun choices(scheme: ControlScheme): List<Choice> = if (scheme == ControlScheme.TENKAICHI) listOf(
        Choice("none", NOTHING, ""),
        Choice("bt3_square", press(PadInput.X), "□"), Choice("bt3_triangle", press(PadInput.Y), "△"),
        Choice("bt3_circle", press(PadInput.B), "○"), Choice("bt3_cross", press(PadInput.A), "✕"),
        Choice("bt3_r1", press(PadInput.RB), "R1"), Choice("bt3_r2", press(PadInput.RT), "R2"),
        Choice("bt3_l2", press(PadInput.LT), "L2"), Choice("bt3_r3", press(PadInput.R3), "R3"),
        Choice("bt3_blast1", BT3_BLAST1, "B1"), Choice("bt3_super1", BT3_SUPER1, "1"), Choice("bt3_super2", BT3_SUPER2, "2"),
        Choice("bt3_super3", BT3_SUPER3, "3"), Choice("bt3_super4", BT3_SUPER4, "4"), Choice("bt3_ultimate", BT3_ULTIMATE, "★"),
        Choice("bt3_sway", press(PadInput.B, PadInput.X), "≈"), Choice("bt3_vanishing", press(PadInput.B, PadInput.A), "≫"),
        Choice("bt3_counter", press(PadInput.B, PadInput.Y), "↺"), Choice("bt3_rising", press(PadInput.RB, PadInput.RT), "⇑"),
    ) else listOf(
        Choice("none", NOTHING, ""),
        Choice("melee", MELEE, "X"), Choice("ki", KI_BLAST, "Y"), Choice("signature", SIGNATURE, "B"), Choice("dash", DASH, "A"),
        Choice("guard", GUARD, "RB"), Choice("ascend", ASCEND, "▲"), Choice("descend", DESCEND, "▼"), Choice("rising", SUPER_RISING, "⇑"),
        Choice("charge", KI_CHARGE, "↓"), Choice("diamond", DIAMOND, "RT"), Choice("ultimate", ULTIMATE, "★"),
        Choice("super1", SUPERS[0], "1"), Choice("super2", SUPERS[1], "2"), Choice("super3", SUPERS[2], "3"), Choice("super4", SUPERS[3], "4"),
        Choice("sway", SWAY, "≈"), Choice("vanishing", VANISHING, "≫"), Choice("counter", COUNTER, "↺"), Choice("throw", THROW, "T"),
    )

    /** True for an action that sends nothing. */
    fun isNothing(a: ControlAction?): Boolean = a == null || (a is ControlAction.Press && a.inputs.isEmpty())

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
        PadInput.STICK_LEFT -> "◀"
        PadInput.STICK_RIGHT -> "▶"
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
            val saved = fromJson(JSONObject(f.readText()))
            // Before version 3 a RADIAL was the Super Attack disc and the fight layouts were walls of buttons: the
            // swipe-button defaults replace them (menu layouts carry over).
            if (kind == LayoutKind.GAMEPLAY && saved.version < 3) return GameActions.defaultLayout(kind, scheme)
            saved.copy(menu = kind != LayoutKind.GAMEPLAY)
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
                        .put("radial", JSONArray().apply { e.radial.forEach { put(actionToJson(it)) } })
                        .apply { e.tap?.let { put("tap", actionToJson(it)) } }
                        .put("hints", JSONArray().apply { e.hints.forEach { put(it) } }),
                )
            }
        },
    )

    fun fromJson(o: JSONObject): ControlLayout {
        val arr = o.getJSONArray("elements")
        val list = (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            val radial = e.optJSONArray("radial")
            val hints = e.optJSONArray("hints")
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
                tap = e.optJSONObject("tap")?.let { actionFromJson(it) },
                hints = if (hints == null) emptyList() else (0 until hints.length()).map { hints.optString(it) },
            )
        }
        return ControlLayout(list, o.optInt("version", 1))
    }
}
