package com.dfdx047.dragonblast.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.dfdx047.dragonblast.controls.ControlAction
import com.dfdx047.dragonblast.controls.ControlElement
import com.dfdx047.dragonblast.controls.ControlIcon
import com.dfdx047.dragonblast.controls.ControlLayout
import com.dfdx047.dragonblast.controls.ElementKind
import com.dfdx047.dragonblast.controls.GameActions
import com.dfdx047.dragonblast.controls.PadInput
import com.dfdx047.dragonblast.controls.ButtonBadge
import com.dfdx047.dragonblast.controls.ControlStyle
import com.dfdx047.dragonblast.controls.Directional
import com.dfdx047.dragonblast.controls.ScreenMap as ControlScreenMap
import com.dfdx047.dragonblast.controls.TouchFeel
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * On-screen controls drawn over the game (and in the layout editor and the launcher preview).
 *
 * Styles: DRAGON (graphite glass discs with action icons — punch, ki, guard, dash... — in the Dragon Blast palette)
 * and the classic XBOX / PLAYSTATION / NINTENDO (letters or symbols). Helper buttons (transform, switch, specials)
 * always show their icon. Based on the Dragon Burst view; [feel] adds the per-preset touch tuning (dynamic stick,
 * hit area, slide between buttons with hysteresis).
 */
@SuppressLint("ViewConstructor")
class TouchControlsView(context: Context) : View(context) {

    interface Listener {
        fun onPress(element: ControlElement)
        fun onRelease(element: ControlElement)
        fun onStick(x: Float, y: Float)
        fun onDpad(element: ControlElement, inputs: Set<PadInput>)
        /** A swipe button was touched (its held action starts). */
        fun onSwipeStart(element: ControlElement)
        /** The finger on a swipe button slid to [dir] (0 up, 1 right, 2 down, 3 left) or back to the middle (-1). */
        fun onSwipeDir(element: ControlElement, dir: Int)
        /** The finger left a swipe button; [tapped] = it never slid (the button's tap action plays). */
        fun onSwipeEnd(element: ControlElement, tapped: Boolean)
    }

    interface EditListener {
        fun onSelected(id: String?)
        fun onMoved(layout: ControlLayout)
    }

    var layout: ControlLayout = GameActions.ragingBlastLayout()
        set(value) {
            field = value
            invalidate()
        }
    var style: ControlStyle = ControlStyle.DRAGON
        set(value) {
            field = value
            invalidate()
        }
    /** D-pad de 8 direcoes ou analogico (o outro fica escondido). */
    var directional: Directional = Directional.DPAD
        set(value) {
            field = value
            invalidate()
        }
    /** Indicacao do botao do comando (Xbox / PlayStation) no canto de cada botao. */
    var badge: ButtonBadge = ButtonBadge.XBOX
        set(value) {
            field = value
            invalidate()
        }
    var globalOpacity: Float = 0.75f
        set(value) {
            field = value.coerceIn(0.1f, 1f)
            invalidate()
        }
    var haptics = true
    /** Corner badges show each element's own label (Tenkaichi scheme: BT3 glyphs) instead of the pad inputs sent. */
    var badgeFromLabel = false
    /** Touch tuning of the chosen preset (Pro / Normal / Casual). */
    var feel: TouchFeel = TouchFeel.NORMAL
    /** Left-handed: everything mirrored (stick on the right, buttons on the left). */
    var mirrored = false
        set(value) {
            field = value
            invalidate()
        }
    var editMode = false
    var selectedId: String? = null
        set(value) {
            field = value
            invalidate()
        }
    var listener: Listener? = null
    var editListener: EditListener? = null

    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val icons = HashMap<ControlIcon, Drawable>()

    private val pressed = HashSet<String>()
    /** pointerId -> id do elemento */
    private val pointers = HashMap<Int, String>()
    private var stickPointer = -1
    private var stickDx = 0f
    private var stickDy = 0f
    /** Where the stick is centred while held (the thumb's landing point with a dynamic stick). */
    private var stickOx = 0f
    private var stickOy = 0f
    private val dpadState = HashMap<String, Set<PadInput>>()
    /** Swipe button id -> chosen direction (0 up, 1 right, 2 down, 3 left; -1 none). */
    private val radialDir = HashMap<String, Int>()
    /** Swipe button id -> where the finger landed (directions are measured from there, not from the centre). */
    private val radialOrigin = HashMap<String, android.graphics.PointF>()
    /** Swipe buttons whose finger slid at some point (no tap action on release). */
    private val radialSlid = HashSet<String>()

    // Edicao
    private var dragId: String? = null
    private var dragOffX = 0f
    private var dragOffY = 0f

    // Dragon Blast palette: graphite glass, steel edge, ember icons; pressed lights up in ember.
    private val gold = Color.parseColor("#DDA05A")      // ember (icons)
    private val orange = Color.parseColor("#C9783A")    // deep ember (pressed glow)
    private val yellow = Color.parseColor("#F2C487")    // light ember (pressed edge, selection)
    private val glassEdge = Color.parseColor("#9DB0C4") // steel

    init {
        isHapticFeedbackEnabled = true
    }

    private fun icon(i: ControlIcon): Drawable =
        icons.getOrPut(i) { ContextCompat.getDrawable(context, i.drawable)!!.mutate() }

    /** Centre on screen: ScreenMap, then kept whole inside the view (no element is cut by an edge). */
    private fun cx(e: ControlElement): Float {
        val w = width.toFloat()
        val fx = if (mirrored) 1f - e.x else e.x
        val px = if (layout.version < 2) fx * w else ControlScreenMap.toPx(fx, w, height.toFloat())
        val m = radius(e) + 4 * density
        return if (w > 2 * m) px.coerceIn(m, w - m) else px
    }

    private fun cy(e: ControlElement): Float {
        val h = height.toFloat()
        val m = radius(e) + 4 * density
        return if (h > 2 * m) (e.y * h).coerceIn(m, h - m) else e.y * h
    }

    /** Where an element is drawn (the controls guide numbers them). */
    fun centerOf(e: ControlElement): android.graphics.PointF = android.graphics.PointF(cx(e), cy(e))
    private fun radius(e: ControlElement) =
        e.sizeDp * density * scale / 2f * (if (referenceWidthPx > 0 && width > 0) width.toFloat() / referenceWidthPx else 1f)

    /** Preview: the real screen's landscape width; sizes shrink so the miniature matches the game's proportions. */
    var referenceWidthPx = 0

    /** Global size of the controls (launcher "Size", 70..140 %). */
    var scale = 1f
        set(value) {
            field = value.coerceIn(0.5f, 1.6f)
            invalidate()
        }

    /** Applies the launcher's Controls tab: scheme feel, style, opacity, size, left-handed, haptics. */
    fun applySettings(s: com.dfdx047.dragonblast.data.Settings) {
        val base = when (s.touchPreset) {
            com.dfdx047.dragonblast.data.TouchPreset.PRO -> TouchFeel.PRO
            com.dfdx047.dragonblast.data.TouchPreset.NORMAL -> TouchFeel.NORMAL
            com.dfdx047.dragonblast.data.TouchPreset.CASUAL -> TouchFeel.CASUAL
        }
        feel = base.copy(
            dynamicStick = s.stickMode == com.dfdx047.dragonblast.data.StickMode.DYNAMIC,
            swipe = s.swipeButtons,
        )
        style = when (s.buttonStyle) {
            com.dfdx047.dragonblast.data.ButtonStyle.DRAGON -> ControlStyle.DRAGON
            com.dfdx047.dragonblast.data.ButtonStyle.XBOX -> ControlStyle.XBOX
            com.dfdx047.dragonblast.data.ButtonStyle.PLAYSTATION -> ControlStyle.PLAYSTATION
            com.dfdx047.dragonblast.data.ButtonStyle.NINTENDO -> ControlStyle.NINTENDO
        }
        // Dragon icons: the corner badge names the pad button (PlayStation glyphs for the Tenkaichi scheme).
        val bt3 = s.controlScheme == com.dfdx047.dragonblast.data.ControlScheme.TENKAICHI
        badge = if (bt3) ButtonBadge.PLAYSTATION else ButtonBadge.XBOX
        badgeFromLabel = bt3
        globalOpacity = s.touchOpacity / 100f
        scale = s.touchScale / 100f
        mirrored = s.leftHanded
        haptics = s.haptics != com.dfdx047.dragonblast.data.Haptics.OFF
        directional = Directional.STICK
    }

    private fun alphaOf(e: ControlElement, base: Float): Int =
        (255 * base * e.opacity * (if (editMode) 1f else globalOpacity)).toInt().coerceIn(0, 255)

    /**
     * Visivel agora: escondido pelo usuario ou pelo tipo de direcional escolhido. No layout
     * do mapa (voo livre, precisa do analogico) cada um so depende do proprio "visivel".
     */
    private fun isShown(e: ControlElement): Boolean = e.visible && when (e.kind) {
        ElementKind.STICK -> layout.map || directional == Directional.STICK
        ElementKind.DPAD -> layout.map || directional == Directional.DPAD
        ElementKind.BUTTON, ElementKind.RADIAL -> true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (e in layout.elements) {
            if (!isShown(e)) continue
            when (e.kind) {
                ElementKind.BUTTON -> drawButton(canvas, e)
                ElementKind.STICK -> drawStick(canvas, e)
                ElementKind.DPAD -> drawDpad(canvas, e)
                ElementKind.RADIAL -> drawRadial(canvas, e)
            }
            if (editMode && e.id == selectedId) {
                ring.color = yellow
                ring.alpha = 255
                ring.strokeWidth = 2.5f * density
                ring.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8f * density, 6f * density), 0f)
                canvas.drawCircle(cx(e), cy(e), radius(e) + 6 * density, ring)
                ring.pathEffect = null
            }
        }
    }

    /** Entrada principal do botao (para o rotulo classico). */
    private fun mainInput(e: ControlElement): PadInput? =
        (e.action as? ControlAction.Press)?.inputs?.singleOrNull()

    private fun isFaceButton(e: ControlElement): Boolean =
        mainInput(e) in setOf(PadInput.A, PadInput.B, PadInput.X, PadInput.Y)

    /**
     * Fundo "vidro fosco": branco muito translucido com um brilho em cima e uma
     * borda clara fina. Premido: o vidro acende em laranja por baixo do icone.
     */
    private fun drawGlass(c: Canvas, e: ControlElement, x: Float, y: Float, r: Float, down: Boolean) {
        fill.shader = RadialGradient(
            x, y - r * 0.45f, r * 1.4f,
            intArrayOf(Color.argb(120, 58, 64, 76), Color.argb(96, 28, 32, 40), Color.argb(84, 10, 12, 16)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        fill.alpha = alphaOf(e, 1f)
        c.drawCircle(x, y, r, fill)
        if (down) {
            fill.shader = RadialGradient(x, y, r, intArrayOf(Color.argb(170, 242, 196, 135), Color.argb(110, 201, 120, 58), Color.argb(0, 201, 120, 58)),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            fill.alpha = alphaOf(e, 1f)
            c.drawCircle(x, y, r, fill)
        }
        fill.shader = null
        ring.strokeWidth = 1.5f * density
        ring.color = if (down) yellow else glassEdge
        ring.alpha = alphaOf(e, if (down) 0.95f else 0.55f)
        c.drawCircle(x, y, r - ring.strokeWidth / 2, ring)
    }

    private fun drawButton(c: Canvas, e: ControlElement) {
        val x = cx(e)
        val y = cy(e)
        val r = radius(e)
        val down = e.id in pressed
        // Nos menus os A/B/X/Y mostram sempre o simbolo do comando (no estilo Dragon
        // Fury, o da indicacao escolhida).
        val classic = (style != ControlStyle.DRAGON || layout.menu) && isFaceButton(e)
        val psGlyph = style == ControlStyle.PLAYSTATION || (style == ControlStyle.DRAGON && badge == ButtonBadge.PLAYSTATION)
        val nin = style == ControlStyle.NINTENDO || (style == ControlStyle.DRAGON && badge == ButtonBadge.NINTENDO)

        drawGlass(c, e, x, y, r, down)

        if (classic) {
            drawClassicGlyph(c, mainInput(e)!!, psGlyph, x, y, r * 0.95f, alphaOf(e, 1f), nin)
        } else if (layout.menu && e.icon == ControlIcon.MACRO && mainInput(e) != null) {
            text.color = if (down) Color.WHITE else gold
            text.alpha = alphaOf(e, 1f)
            text.textSize = r * 0.5f
            c.drawText(GameActions.classicLabel(mainInput(e)!!, psGlyph, nin), x, y + text.textSize / 3, text)
        } else if (e.icon == ControlIcon.MACRO && e.label.isNotBlank()) {
            text.color = if (down) Color.WHITE else gold
            text.alpha = alphaOf(e, 1f)
            text.textSize = r * 0.55f
            c.drawText(e.label.take(4), x, y + text.textSize / 3, text)
        } else {
            val d = icon(e.icon)
            val s = (r * 1.05f).toInt()
            d.setBounds((x - s / 2).toInt(), (y - s / 2).toInt(), (x + s / 2).toInt(), (y + s / 2).toInt())
            d.colorFilter = PorterDuffColorFilter(if (down) Color.WHITE else gold, PorterDuff.Mode.SRC_IN)
            d.alpha = alphaOf(e, 1f)
            d.draw(c)
        }
        // Indicacao do botao do comando no canto (nos classicos so nos que nao sao A/B/X/Y).
        val badgeStyle = when {
            style == ControlStyle.XBOX -> ButtonBadge.XBOX
            style == ControlStyle.PLAYSTATION -> ButtonBadge.PLAYSTATION
            style == ControlStyle.NINTENDO -> ButtonBadge.NINTENDO
            else -> badge
        }
        if (badgeStyle != ButtonBadge.NONE && !classic && !layout.menu) {
            drawBadge(c, e, badgeStyle == ButtonBadge.PLAYSTATION, x, y, r, badgeStyle == ButtonBadge.NINTENDO)
        }
    }

    /** O que o botao faz no comando: um botao, uma combinacao ou "defesa + tras tras". */
    private fun badgeInputs(e: ControlElement): Pair<List<PadInput>, String> = when (val a = e.action) {
        is ControlAction.Press -> a.inputs.toList() to ""
        is ControlAction.Sequence -> run {
            // Comando com direcao (especiais, dash): o ultimo passo, ex. "▶+B".
            val last = a.steps.lastOrNull()?.inputs ?: emptySet()
            val buttons = last.filter { it.ordinal <= PadInput.R3.ordinal }
            val dirs = last.filter { it.ordinal > PadInput.R3.ordinal }
            if (dirs.isNotEmpty() && buttons.isNotEmpty()) return@run (dirs + buttons) to ""
            val firstButtons = a.steps.flatMap { it.inputs }.filter { it.ordinal <= PadInput.R3.ordinal }.distinct()
            val backs = a.steps.count { PadInput.BACKWARD in it.inputs }
            firstButtons to (if (backs > 0) "◀".repeat(backs.coerceAtMost(2)) else "")
        }
    }

    private fun drawBadge(c: Canvas, e: ControlElement, ps: Boolean, x: Float, y: Float, r: Float, nin: Boolean = false) {
        if (badgeFromLabel && e.label.isNotBlank() && e.icon != ControlIcon.MACRO) {
            drawLabelBadge(c, e, e.label, x, y, r)
            return
        }
        val (inputs, suffix) = badgeInputs(e)
        if (inputs.isEmpty()) return
        // Selo pequeno a 45 graus, em cima a direita.
        val br = (r * 0.36f).coerceIn(9f * density, 15f * density)
        val bx = x + r * 0.72f
        val by = y - r * 0.72f
        val face = inputs.filter { it in setOf(PadInput.A, PadInput.B, PadInput.X, PadInput.Y) }
        val label = if (face.size == inputs.size && face.size == 1 && suffix.isEmpty()) null
        else inputs.joinToString("+") { GameActions.classicLabel(it, ps, nin) } + suffix
        if (label != null) {
            // Texto (LB, R1, ✕◀◀, X+Y...): pilula escura translucida.
            text.textSize = br * 0.95f
            val w = text.measureText(label) + br * 0.9f
            val px = keepInside(bx, w)
            fill.shader = null
            fill.color = Color.argb(160, 10, 12, 16)
            fill.alpha = alphaOf(e, 0.75f)
            c.drawRoundRect(px - w / 2, by - br * 0.8f, px + w / 2, by + br * 0.8f, br, br, fill)
            text.color = Color.WHITE
            text.alpha = alphaOf(e, 1f)
            c.drawText(label, px, by + text.textSize / 3, text)
            return
        }
        fill.shader = null
        fill.color = Color.argb(170, 10, 12, 16)
        fill.alpha = alphaOf(e, 0.8f)
        c.drawCircle(bx, by, br, fill)
        drawClassicGlyph(c, face[0], ps, bx, by, br * 1.05f, alphaOf(e, 1f), nin)
    }

    /** A pill [w] wide centred at [bx], moved in so it is not cut by the left or right edge. */
    private fun keepInside(bx: Float, w: Float): Float {
        val m = 2 * density
        return if (width > w + 2 * m) bx.coerceIn(w / 2 + m, width - w / 2 - m) else bx
    }

    /** A text badge (pill) at the button's top right. */
    private fun drawLabelBadge(c: Canvas, e: ControlElement, label: String, x: Float, y: Float, r: Float) {
        val br = (r * 0.36f).coerceIn(9f * density, 15f * density)
        val bx0 = x + r * 0.72f
        val by = y - r * 0.72f
        text.textSize = br * 0.95f
        val w = text.measureText(label) + br * 0.9f
        val bx = keepInside(bx0, w)
        fill.shader = null
        fill.color = Color.argb(170, 10, 12, 16)
        fill.alpha = alphaOf(e, 0.8f)
        c.drawRoundRect(bx - w / 2, by - br * 0.8f, bx + w / 2, by + br * 0.8f, br, br, fill)
        text.color = when (label) {
            "△" -> Color.parseColor("#40E2A0")
            "○" -> Color.parseColor("#FF6666")
            "✕" -> Color.parseColor("#7CB2E8")
            "□" -> Color.parseColor("#E899C9")
            else -> Color.WHITE
        }
        text.alpha = alphaOf(e, 1f)
        c.drawText(label, bx, by + text.textSize / 3, text)
    }

    private fun classicColor(i: PadInput, ps: Boolean): Int = if (ps) {
        when (i) {
            PadInput.A -> Color.parseColor("#7CB2E8")
            PadInput.B -> Color.parseColor("#FF6666")
            PadInput.X -> Color.parseColor("#E899C9")
            else -> Color.parseColor("#40E2A0")
        }
    } else {
        when (i) {
            PadInput.A -> Color.parseColor("#5DC21E")
            PadInput.B -> Color.parseColor("#E5312C")
            PadInput.X -> Color.parseColor("#2C8BE5")
            else -> Color.parseColor("#F6C41B")
        }
    }

    /** Letra do Xbox ou simbolo do PlayStation, centrado em (x, y) com raio r. */
    private fun drawClassicGlyph(c: Canvas, i: PadInput, ps: Boolean, x: Float, y: Float, r: Float, alpha: Int, nin: Boolean = false) {
        val color = if (nin) Color.parseColor("#E6E8EC") else classicColor(i, ps)
        if (!ps) {
            text.color = color
            text.alpha = alpha
            text.textSize = r * 0.9f
            c.drawText(GameActions.classicLabel(i, false, nin), x, y + text.textSize / 3, text)
            return
        }
        ring.color = color
        ring.alpha = alpha
        ring.strokeWidth = (r * 0.12f).coerceAtLeast(1.5f * density)
        val s = r * 0.42f
        when (i) {
            PadInput.A -> { // cruz
                c.drawLine(x - s, y - s, x + s, y + s, ring)
                c.drawLine(x - s, y + s, x + s, y - s, ring)
            }
            PadInput.B -> c.drawCircle(x, y, s, ring)
            PadInput.X -> c.drawRect(x - s * 0.9f, y - s * 0.9f, x + s * 0.9f, y + s * 0.9f, ring)
            else -> {
                val p = Path()
                p.moveTo(x, y - s)
                p.lineTo(x + s, y + s * 0.75f)
                p.lineTo(x - s, y + s * 0.75f)
                p.close()
                c.drawPath(p, ring)
            }
        }
    }

    /**
     * Swipe button: the icon in the middle, a short mark for each direction that does something (its hint: the
     * diamond slot, the Super Attack number...) and, under the icon, the tap's hint. While a finger is on it, the
     * chosen quarter lights up in ember.
     */
    private fun drawRadial(c: Canvas, e: ControlElement) {
        val x = cx(e)
        val y = cy(e)
        val r = radius(e)
        val down = e.id in pressed
        val dir = radialDir[e.id] ?: -1
        drawGlass(c, e, x, y, r, down && dir < 0)
        if (dir >= 0) {
            fill.shader = null
            fill.color = orange
            fill.alpha = alphaOf(e, 0.55f)
            val start = -135f + dir * 90f
            c.drawArc(x - r, y - r, x + r, y + r, start, 90f, true, fill)
        }
        // Thin ring segments between the quarters: it slides, it is not a plain button.
        ring.strokeWidth = 1.2f * density
        ring.color = glassEdge
        ring.alpha = alphaOf(e, 0.35f)
        for (i in 0 until 4) {
            val a = Math.toRadians(-45.0 + i * 90.0)
            c.drawLine(
                x + (kotlin.math.cos(a) * r * 0.58f).toFloat(), y + (kotlin.math.sin(a) * r * 0.58f).toFloat(),
                x + (kotlin.math.cos(a) * r * 0.9f).toFloat(), y + (kotlin.math.sin(a) * r * 0.9f).toFloat(), ring,
            )
        }
        for (i in 0 until 4) {
            if (GameActions.isNothing(e.radial.getOrNull(i))) continue
            val a = Math.toRadians(-90.0 + i * 90.0)
            val nx = x + (kotlin.math.cos(a) * r * 0.74f).toFloat()
            val ny = y + (kotlin.math.sin(a) * r * 0.74f).toFloat()
            val hint = e.hints.getOrNull(i).orEmpty()
            if (hint.isBlank()) {
                fill.color = if (i == dir) Color.WHITE else gold
                fill.alpha = alphaOf(e, if (i == dir) 1f else 0.7f)
                c.drawCircle(nx, ny, r * 0.075f, fill)
            } else {
                text.color = if (i == dir) Color.WHITE else hintColor(hint)
                text.alpha = alphaOf(e, if (i == dir) 1f else 0.9f)
                text.textSize = (r * 0.27f).coerceAtLeast(9f * density)
                c.drawText(hint, nx, ny + text.textSize / 3, text)
            }
        }
        val d = icon(e.icon)
        val sz = (r * 0.78f).toInt()
        val iy = y - r * 0.04f
        d.setBounds((x - sz / 2).toInt(), (iy - sz / 2).toInt(), (x + sz / 2).toInt(), (iy + sz / 2).toInt())
        d.colorFilter = PorterDuffColorFilter(if (down) Color.WHITE else gold, PorterDuff.Mode.SRC_IN)
        d.alpha = alphaOf(e, 1f)
        d.draw(c)
        val tapHint = e.hints.getOrNull(4).orEmpty()
        if (tapHint.isNotBlank() && !GameActions.isNothing(e.tap)) {
            text.color = Color.WHITE
            text.alpha = alphaOf(e, 0.85f)
            text.textSize = (r * 0.2f).coerceAtLeast(8f * density)
            c.drawText(tapHint, x + r * 0.3f, y + r * 0.34f, text)
        }
        // The pad buttons it holds (RT, R3, RB...) in the corner, as on the other buttons.
        val badgeStyle = when (style) {
            ControlStyle.XBOX -> ButtonBadge.XBOX
            ControlStyle.PLAYSTATION -> ButtonBadge.PLAYSTATION
            ControlStyle.NINTENDO -> ButtonBadge.NINTENDO
            else -> badge
        }
        if (badgeStyle != ButtonBadge.NONE && e.label.isNotBlank() && !layout.menu) drawLabelBadge(c, e, e.label, x, y, r)
    }

    /** PlayStation glyph colours for BT3 hints, the Xbox ones for the letters; ember otherwise. */
    private fun hintColor(h: String): Int = when (h) {
        "△" -> Color.parseColor("#40E2A0")
        "○" -> Color.parseColor("#FF6666")
        "✕" -> Color.parseColor("#7CB2E8")
        "□" -> Color.parseColor("#E899C9")
        "Y" -> Color.parseColor("#F6C41B")
        "B" -> Color.parseColor("#E5312C")
        "A" -> Color.parseColor("#5DC21E")
        "X" -> Color.parseColor("#2C8BE5")
        else -> gold
    }

    private fun drawStick(c: Canvas, e: ControlElement) {
        val active = stickPointer >= 0 && !editMode
        // Dynamic stick: drawn where the thumb landed; at rest a fainter disc marks its home.
        val x = if (active) stickOx else cx(e)
        val y = if (active) stickOy else cy(e)
        val r = radius(e)
        drawGlass(c, e, x, y, r, false)
        text.color = gold
        text.alpha = alphaOf(e, 0.7f)
        text.textSize = r * 0.16f
        c.drawText("▲", x, y - r * 0.72f, text)
        c.drawText("▼", x, y + r * 0.84f, text)
        c.drawText("◀", x - r * 0.78f, y + r * 0.06f, text)
        c.drawText("▶", x + r * 0.78f, y + r * 0.06f, text)
        val kx = x + (if (active) stickDx else 0f) * r * 0.55f
        val ky = y + (if (active) stickDy else 0f) * r * 0.55f
        val kr = r * 0.4f
        fill.shader = RadialGradient(kx - kr * 0.3f, ky - kr * 0.35f, kr * 1.4f,
            intArrayOf(Color.parseColor("#5A616D"), Color.parseColor("#262B33"), Color.parseColor("#14171C")),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        fill.alpha = alphaOf(e, if (active) 0.95f else 0.75f)
        c.drawCircle(kx, ky, kr, fill)
        fill.shader = null
        ring.strokeWidth = 2f * density
        ring.color = if (style == ControlStyle.DRAGON) gold else glassEdge
        ring.alpha = alphaOf(e, if (active) 0.95f else 0.55f)
        c.drawCircle(kx, ky, kr - ring.strokeWidth, ring)
    }

    /**
     * D-pad de 8 direcoes: vidro redondo com as 4 setas e 4 marcas nas
     * diagonais; as direcoes premidas (incluindo diagonais) acendem.
     */
    private fun drawDpad(c: Canvas, e: ControlElement) {
        val x = cx(e)
        val y = cy(e)
        val r = radius(e)
        val state = dpadState[e.id].orEmpty()
        drawGlass(c, e, x, y, r, false)
        // Cruz
        val arm = r * 0.30f
        fill.shader = null
        fill.color = Color.argb(80, 232, 222, 196)
        fill.alpha = alphaOf(e, 0.6f)
        c.drawRoundRect(x - arm, y - r * 0.82f, x + arm, y + r * 0.82f, arm * 0.4f, arm * 0.4f, fill)
        c.drawRoundRect(x - r * 0.82f, y - arm, x + r * 0.82f, y + arm, arm * 0.4f, arm * 0.4f, fill)
        // Direcoes premidas acendem em laranja.
        fun lit(dir: PadInput, l: Float, t: Float, rr: Float, b: Float) {
            if (dir !in state) return
            fill.color = orange
            fill.alpha = alphaOf(e, 0.75f)
            c.drawRoundRect(l, t, rr, b, arm * 0.4f, arm * 0.4f, fill)
        }
        lit(PadInput.UP, x - arm, y - r * 0.82f, x + arm, y - arm)
        lit(PadInput.DOWN, x - arm, y + arm, x + arm, y + r * 0.82f)
        lit(PadInput.LEFT, x - r * 0.82f, y - arm, x - arm, y + arm)
        lit(PadInput.RIGHT, x + arm, y - arm, x + r * 0.82f, y + arm)
        // Setas
        text.color = gold
        text.alpha = alphaOf(e, 1f)
        text.textSize = r * 0.24f
        c.drawText("▲", x, y - r * 0.5f, text)
        c.drawText("▼", x, y + r * 0.62f, text)
        c.drawText("◀", x - r * 0.56f, y + r * 0.08f, text)
        c.drawText("▶", x + r * 0.56f, y + r * 0.08f, text)
        // Diagonais (marcas que acendem quando a diagonal esta premida)
        val d = r * 0.62f
        for ((dx, dy) in listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f)) {
            val h = if (dx < 0) PadInput.LEFT else PadInput.RIGHT
            val v = if (dy < 0) PadInput.UP else PadInput.DOWN
            val on = h in state && v in state
            fill.color = if (on) yellow else glassEdge
            fill.alpha = alphaOf(e, if (on) 0.95f else 0.45f)
            c.drawCircle(x + dx * d * 0.72f, y + dy * d * 0.72f, r * (if (on) 0.07f else 0.045f), fill)
        }
        // Centro
        fill.color = Color.argb(60, 255, 255, 255)
        fill.alpha = alphaOf(e, 0.6f)
        c.drawCircle(x, y, arm * 0.7f, fill)
    }

    // ------------------------------------------------------------------ toque

    private fun hit(px: Float, py: Float): ControlElement? {
        var best: ControlElement? = null
        var bestD = Float.MAX_VALUE
        for (e in layout.elements) {
            if (!isShown(e)) continue
            val d = hypot(px - cx(e), py - cy(e))
            // Um pouco de margem fora do circulo ajuda os dedos.
            val reach = radius(e) * (if (e.kind == ElementKind.BUTTON) feel.hitScale else feel.hitScale + 0.1f)
            if (d <= reach && d / reach < bestD) {
                bestD = d / reach
                best = e
            }
        }
        return best
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (editMode) return onEditTouch(ev)
        if (HelperEngine.PAD_LOG && ev.actionMasked != MotionEvent.ACTION_MOVE) {
            android.util.Log.v(HelperEngine.PAD_TAG, "touch ${MotionEvent.actionToString(ev.actionMasked)} " +
                "pid ${ev.getPointerId(ev.actionIndex)} at %.0f,%.0f; held $pointers, pressed $pressed, stick $stickPointer"
                    .format(ev.getX(ev.actionIndex), ev.getY(ev.actionIndex)))
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = ev.actionIndex
                val px = ev.getX(i)
                val py = ev.getY(i)
                val pid = ev.getPointerId(i)
                val e = hit(px, py) ?: dynamicStickAt(px) ?: return true
                when (e.kind) {
                    ElementKind.STICK -> {
                        if (stickPointer < 0) {
                            stickPointer = pid
                            pointers[pid] = e.id
                            // Dynamic: centre where the thumb landed (no jump); fixed: the stick's own centre.
                            stickOx = if (feel.dynamicStick) px else cx(e)
                            stickOy = if (feel.dynamicStick) py else cy(e)
                            updateStick(e, px, py)
                        }
                    }
                    ElementKind.DPAD -> {
                        pointers[pid] = e.id
                        updateDpad(e, ev.getX(i), ev.getY(i))
                    }
                    ElementKind.BUTTON -> {
                        pointers[pid] = e.id
                        pressButton(e)
                    }
                    ElementKind.RADIAL -> {
                        if (e.id in pressed) return true  // already held by another finger
                        pointers[pid] = e.id
                        radialDir[e.id] = -1
                        radialOrigin[e.id] = android.graphics.PointF(px, py)
                        radialSlid.remove(e.id)
                        pressed.add(e.id)
                        if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        listener?.onSwipeStart(e)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until ev.pointerCount) {
                    val pid = ev.getPointerId(i)
                    val id = pointers[pid] ?: continue
                    val e = layout.elements.firstOrNull { it.id == id } ?: continue
                    when (e.kind) {
                        ElementKind.STICK -> updateStick(e, ev.getX(i), ev.getY(i))
                        ElementKind.DPAD -> updateDpad(e, ev.getX(i), ev.getY(i))
                        ElementKind.RADIAL -> updateRadial(e, ev.getX(i), ev.getY(i))
                        ElementKind.BUTTON -> if (feel.swipe) {
                            // Slide to another button, with hysteresis: the finger must leave this button
                            // (past 1.1 radius) and be well inside the other (under 0.8 of its radius), so a
                            // thumb rolling on a press never jumps to a neighbour by accident.
                            val x = ev.getX(i)
                            val y = ev.getY(i)
                            val other = hit(x, y)
                            if (other != null && other.id != id && other.kind == ElementKind.BUTTON &&
                                hypot(x - cx(e), y - cy(e)) > radius(e) * 1.1f &&
                                hypot(x - cx(other), y - cy(other)) < radius(other) * 0.8f
                            ) {
                                releaseButton(e)
                                pointers[pid] = other.id
                                pressButton(other)
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pid = ev.getPointerId(ev.actionIndex)
                endPointer(pid)
            }
            MotionEvent.ACTION_CANCEL -> {
                pointers.keys.toList().forEach(::endPointer)
            }
        }
        return true
    }

    private fun endPointer(pid: Int) {
        val id = pointers.remove(pid) ?: return
        val e = layout.elements.firstOrNull { it.id == id } ?: return
        when (e.kind) {
            ElementKind.STICK -> {
                stickPointer = -1
                stickDx = 0f
                stickDy = 0f
                listener?.onStick(0f, 0f)
                invalidate()
            }
            ElementKind.DPAD -> {
                dpadState.remove(e.id)
                listener?.onDpad(e, emptySet())
                invalidate()
            }
            ElementKind.BUTTON -> releaseButton(e)
            ElementKind.RADIAL -> {
                listener?.onSwipeEnd(e, e.id !in radialSlid)
                radialDir.remove(e.id)
                radialOrigin.remove(e.id)
                radialSlid.remove(e.id)
                pressed.remove(e.id)
                invalidate()
            }
        }
    }

    /**
     * Direction of the finger from where it landed: past [swipeThreshold] it picks the quarter (↑ → ↓ ←); back
     * within 60 % of that it returns to "none" (the flight button stops ascending). Directions without an action are
     * ignored, so a sloppy slide on the transform button never lands on an empty side.
     */
    private fun updateRadial(e: ControlElement, px: Float, py: Float) {
        val o = radialOrigin[e.id] ?: return
        val dx = px - o.x
        val dy = py - o.y
        val dist = hypot(dx, dy)
        val threshold = swipeThreshold(e)
        val current = radialDir[e.id] ?: -1
        val dir = when {
            dist >= threshold -> {
                val a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) // 0 = right, 90 = down
                val d = when {
                    a >= -135 && a < -45 -> 0
                    a >= -45 && a < 45 -> 1
                    a >= 45 && a < 135 -> 2
                    else -> 3
                }
                if (GameActions.isNothing(e.radial.getOrNull(d))) current else d
            }
            dist < threshold * 0.6f -> -1
            else -> current
        }
        if (dist >= threshold) radialSlid.add(e.id)
        if (dir == current) return
        radialDir[e.id] = dir
        if (haptics && dir >= 0) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        listener?.onSwipeDir(e, dir)
        invalidate()
    }

    private fun swipeThreshold(e: ControlElement) = maxOf(radius(e) * 0.38f, 14f * density)

    private fun pressButton(e: ControlElement) {
        if (!pressed.add(e.id)) return
        if (haptics) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        listener?.onPress(e)
        invalidate()
    }

    private fun releaseButton(e: ControlElement) {
        if (!pressed.remove(e.id)) return
        listener?.onRelease(e)
        invalidate()
    }

    /** Dynamic stick: a touch on free space of the stick's half of the screen grabs the stick there. */
    private fun dynamicStickAt(px: Float): ControlElement? {
        if (!feel.dynamicStick || stickPointer >= 0 || width == 0) return null
        val stick = layout.elements.firstOrNull { it.kind == ElementKind.STICK && isShown(it) } ?: return null
        val inZone = if (cx(stick) < width / 2f) px < width * 0.45f else px > width * 0.55f
        return if (inZone) stick else null
    }

    private fun updateStick(e: ControlElement, px: Float, py: Float) {
        val r = radius(e) * 0.55f
        var dx = (px - stickOx) / r
        var dy = (py - stickOy) / r
        val len = hypot(dx, dy)
        if (len > 1f) {
            dx /= len
            dy /= len
        }
        stickDx = dx
        stickDy = dy
        listener?.onStick(dx, dy)
        invalidate()
    }

    private fun updateDpad(e: ControlElement, px: Float, py: Float) {
        val dx = px - cx(e)
        val dy = py - cy(e)
        val dead = radius(e) * 0.2f
        val set = HashSet<PadInput>()
        if (hypot(dx, dy) > dead) {
            // 8 direcoes: cada setor de 45 graus.
            val a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
            if (a > -67.5 && a < 67.5) set += PadInput.RIGHT
            if (a > 112.5 || a < -112.5) set += PadInput.LEFT
            if (a > 22.5 && a < 157.5) set += PadInput.DOWN
            if (a < -22.5 && a > -157.5) set += PadInput.UP
        }
        if (dpadState[e.id] != set) {
            if (set.isNotEmpty() && haptics && dpadState[e.id].isNullOrEmpty()) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            dpadState[e.id] = set
            listener?.onDpad(e, set)
            invalidate()
        }
    }

    /** Solta tudo (ex.: o jogo foi para segundo plano). */
    fun releaseAll() {
        pointers.keys.toList().forEach(::endPointer)
        pressed.clear()
        invalidate()
    }

    // ----------------------------------------------------------------- edicao

    private fun onEditTouch(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val e = hit(ev.x, ev.y)
                selectedId = e?.id
                editListener?.onSelected(e?.id)
                if (e != null) {
                    dragId = e.id
                    dragOffX = ev.x - cx(e)
                    dragOffY = ev.y - cy(e)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val id = dragId ?: return true
                if (width == 0 || height == 0) return true
                val raw = if (layout.version < 2) ((ev.x - dragOffX) / width).coerceIn(0f, 1f)
                    else ControlScreenMap.fromPx(ev.x - dragOffX, width.toFloat(), height.toFloat())
                val nx = if (mirrored) 1f - raw else raw
                val ny = ((ev.y - dragOffY) / height).coerceIn(0f, 1f)
                layout = layout.copy(elements = layout.elements.map { if (it.id == id) it.copy(x = nx, y = ny) else it })
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragId != null) editListener?.onMoved(layout)
                dragId = null
            }
        }
        return true
    }
}
