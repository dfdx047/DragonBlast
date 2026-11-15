package com.dfdx047.dragonblast.controls

import android.graphics.Paint
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.ControlScheme
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.ui.theme.DbColors
import com.dfdx047.dragonblast.ui.theme.DragonBlastTheme

/**
 * Physical controller remapping, one profile per fight scheme (PadRemap): a drawn controller with what every button
 * does, the list of actions, and a live test (pressed buttons light up). Tap an action, press a button: they swap.
 */
class PadRemapActivity : AppCompatActivity() {
    /** Buttons held right now (keys and axes), for the live test. */
    private val held = mutableStateOf(emptySet<PadKey>())
    private var keysHeld = emptySet<PadKey>()
    private var axesHeld = emptySet<PadKey>()
    /** The screen's handler of a newly pressed button. */
    private var onPress: ((PadKey) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPaths.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val scheme = SettingsStore(this).load().controlScheme
        setContent {
            DragonBlastTheme {
                PadRemapScreen(
                    initialProfile = PadRemap.profileOf(scheme),
                    held = held.value,
                    register = { onPress = it },
                    close = { finish() },
                )
            }
        }
    }

    private fun fromPad(e: android.view.InputEvent) =
        e.isFromSource(InputDevice.SOURCE_GAMEPAD) || e.isFromSource(InputDevice.SOURCE_JOYSTICK)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = PadKey.ofKeyCode(event.keyCode)
        // The system Back key (not a controller's) still leaves the screen.
        if (key == null || !(fromPad(event) || (event.isFromSource(InputDevice.SOURCE_DPAD) && key in DPAD))) {
            return super.dispatchKeyEvent(event)
        }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                keysHeld = keysHeld + key
                held.value = keysHeld + axesHeld
                onPress?.invoke(key)
            }
            KeyEvent.ACTION_UP -> {
                keysHeld = keysHeld - key
                held.value = keysHeld + axesHeld
            }
        }
        return true  // every controller button is the test's: none of them navigates or leaves
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.action != MotionEvent.ACTION_MOVE) {
            return super.dispatchGenericMotionEvent(event)
        }
        val now = PadKey.ofMotion(event)
        val pressed = now - axesHeld
        axesHeld = now
        held.value = keysHeld + axesHeld
        pressed.forEach { onPress?.invoke(it) }
        return true
    }

    private companion object {
        val DPAD = setOf(PadKey.UP, PadKey.DOWN, PadKey.LEFT, PadKey.RIGHT)
    }
}

@Composable
private fun PadRemapScreen(initialProfile: String, held: Set<PadKey>, register: ((PadKey) -> Unit) -> Unit, close: () -> Unit) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(initialProfile) }
    var map by remember { mutableStateOf(PadRemap.load(context, initialProfile)) }
    var listening by remember { mutableStateOf<PadKey?>(null) }
    var lastChanged by remember { mutableStateOf<PadKey?>(null) }
    val actions = PadRemap.actions(profile)

    fun commit(m: IntArray) {
        map = m
        PadRemap.save(context, profile, m)
    }

    DisposableEffect(profile) {
        register { physical ->
            val action = listening
            if (action != null) {
                commit(PadRemap.assign(map, action, physical))
                lastChanged = action
                listening = null
            }
        }
        onDispose { register {} }
    }

    Column(Modifier.fillMaxSize().background(DbColors.Ink)) {
        // Top bar: back, title, profile.
        Row(
            Modifier.fillMaxWidth().background(DbColors.Graphite).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = DbColors.Text) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.pad_title), style = MaterialTheme.typography.titleMedium, color = DbColors.Text)
                Text(
                    stringResource(R.string.pad_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = DbColors.TextFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            listOf("rb2" to "Raging Blast", "bt3" to "Tenkaichi 3").forEach { (p, label) ->
                val on = p == profile
                val shape = RoundedCornerShape(10.dp)
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) DbColors.EmberGlow else DbColors.TextDim,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(shape)
                        .background(if (on) DbColors.Ember.copy(alpha = 0.10f) else DbColors.SlateHigh)
                        .border(1.dp, if (on) DbColors.Ember else DbColors.Line, shape)
                        .clickable {
                            profile = p
                            map = PadRemap.load(context, p)
                            listening = null
                            lastChanged = null
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight * 1.6f
            val boxHeight = maxHeight
            val drawing: @Composable (Modifier) -> Unit = { m ->
                PadDrawing(profile, map, actions, held, listening, m) { physical ->
                    val a = map[physical.ordinal]
                    if (a >= 0) listening = PadKey.entries[a]
                }
            }
            val list: @Composable (Modifier) -> Unit = { m ->
                ActionList(profile, map, actions, held, listening, lastChanged, m) { listening = it }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    drawing(Modifier.weight(1.5f).fillMaxHeight())
                    list(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    drawing(Modifier.fillMaxWidth().height(boxHeight * 0.44f))
                    list(Modifier.fillMaxWidth().weight(1f))
                }
            }

            listening?.let { action ->
                ListenBanner(
                    action = actions.first { it.key == action },
                    onNone = {
                        commit(PadRemap.assign(map, action, null))
                        lastChanged = action
                        listening = null
                    },
                    onCancel = { listening = null },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }

        // Bottom bar: what this changes, presets.
        Row(
            Modifier.fillMaxWidth().background(DbColors.Graphite).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val changes = PadRemap.changes(map)
            Text(
                if (changes == 0) stringResource(R.string.pad_status_default) else stringResource(R.string.pad_status_changed, changes),
                style = MaterialTheme.typography.labelMedium,
                color = if (changes == 0) DbColors.TextFaint else DbColors.Ember,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            OutlinedButton(onClick = { commit(PadRemap.nintendo()); lastChanged = null; listening = null }) {
                Text(stringResource(R.string.pad_nintendo), color = DbColors.TextDim, maxLines = 1)
            }
            OutlinedButton(onClick = { commit(PadRemap.IDENTITY); lastChanged = null; listening = null }) {
                Text(stringResource(R.string.pad_reset), color = DbColors.Text, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ListenBanner(action: PadAction, onNone: () -> Unit, onCancel: () -> Unit, modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "listen").animateFloat(
        0.45f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "a",
    )
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .padding(top = 10.dp, start = 12.dp, end = 12.dp)
            .widthIn(max = 560.dp)
            .clip(shape)
            .background(DbColors.SlateHighest)
            .border(1.5.dp, DbColors.Ember.copy(alpha = pulse), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(action, DbColors.EmberGlow, 30)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.pad_listen), style = MaterialTheme.typography.labelMedium, color = DbColors.TextDim)
            Text(stringResource(action.name), style = MaterialTheme.typography.titleMedium, color = DbColors.Text)
        }
        TextButton(onClick = onNone) { Text(stringResource(R.string.pad_none), color = DbColors.TextDim) }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel), color = DbColors.Ember) }
    }
}

@Composable
private fun ActionIcon(action: PadAction, tint: Color, sizeDp: Int) {
    if (action.icon != null) {
        Icon(painterResource(action.icon.drawable), null, tint = tint, modifier = Modifier.size(sizeDp.dp))
    } else {
        Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
            Text(GameActions.classicLabel(action.key.padInput, false), color = tint, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ActionList(
    profile: String,
    map: IntArray,
    actions: List<PadAction>,
    held: Set<PadKey>,
    listening: PadKey?,
    lastChanged: PadKey?,
    modifier: Modifier,
    pick: (PadKey) -> Unit,
) {
    LazyColumn(modifier.padding(horizontal = 10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
        items(actions, key = { it.key.name + it.name }) { a ->
            val physical = PadRemap.physicalOf(map, a.key)
            val active = physical != null && physical in held
            val changed = physical != a.key
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(shape)
                    .background(
                        when {
                            listening == a.key -> DbColors.Ember.copy(alpha = 0.14f)
                            active -> DbColors.Steel.copy(alpha = 0.16f)
                            else -> DbColors.Slate
                        },
                    )
                    .border(1.dp, if (listening == a.key) DbColors.Ember else if (lastChanged == a.key) DbColors.EmberDeep else DbColors.LineSoft, shape)
                    .clickable { pick(a.key) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionIcon(a, if (active) DbColors.EmberGlow else DbColors.Silver, 26)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(a.name), style = MaterialTheme.typography.bodyMedium, color = DbColors.Text, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(a.desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = DbColors.TextFaint,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                val chip = RoundedCornerShape(8.dp)
                Text(
                    physical?.let { PadRemap.glyph(it, profile) } ?: "—",
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        physical == null -> DbColors.TextFaint
                        changed -> DbColors.EmberGlow
                        else -> DbColors.Text
                    },
                    modifier = Modifier
                        .widthIn(min = 52.dp)
                        .clip(chip)
                        .background(if (active) DbColors.Ember.copy(alpha = 0.35f) else DbColors.SlateHighest)
                        .border(1.dp, if (changed) DbColors.Ember.copy(alpha = 0.7f) else DbColors.Line, chip)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/** Where each input sits on the drawn controller (units of a 100 × 62 box) and which side its caption goes. */
private data class Spot(val x: Float, val y: Float, val r: Float, val kind: Int, val left: Boolean)

private const val ROUND = 0
private const val PILL = 1
private const val SQUARE = 2

private val SPOTS = mapOf(
    PadKey.LT to Spot(25f, 4f, 3f, PILL, true),
    PadKey.LB to Spot(25f, 10.5f, 3f, PILL, true),
    PadKey.RT to Spot(75f, 4f, 3f, PILL, false),
    PadKey.RB to Spot(75f, 10.5f, 3f, PILL, false),
    PadKey.BACK to Spot(43.5f, 23f, 2.2f, PILL, true),
    PadKey.START to Spot(56.5f, 23f, 2.2f, PILL, false),
    PadKey.L3 to Spot(26f, 27f, 6.4f, ROUND, true),
    PadKey.UP to Spot(38f, 34f, 2.4f, SQUARE, true),
    PadKey.LEFT to Spot(33.6f, 38.4f, 2.4f, SQUARE, true),
    PadKey.RIGHT to Spot(42.4f, 38.4f, 2.4f, SQUARE, true),
    PadKey.DOWN to Spot(38f, 42.8f, 2.4f, SQUARE, true),
    PadKey.Y to Spot(74f, 20f, 3.6f, ROUND, false),
    PadKey.X to Spot(67.5f, 26.5f, 3.6f, ROUND, false),
    PadKey.B to Spot(80.5f, 26.5f, 3.6f, ROUND, false),
    PadKey.A to Spot(74f, 33f, 3.6f, ROUND, false),
    PadKey.R3 to Spot(62f, 39f, 6.4f, ROUND, false),
)

private val DPAD_NAMES = setOf(R.string.act_up, R.string.act_down, R.string.act_left, R.string.act_right)

/** Caption order on each side, top to bottom. */
private val LEFT_ORDER = listOf(PadKey.LT, PadKey.LB, PadKey.BACK, PadKey.L3, PadKey.UP, PadKey.LEFT, PadKey.RIGHT, PadKey.DOWN)
private val RIGHT_ORDER = listOf(PadKey.RT, PadKey.RB, PadKey.Y, PadKey.START, PadKey.X, PadKey.B, PadKey.A, PadKey.R3)

@Composable
private fun PadDrawing(
    profile: String,
    map: IntArray,
    actions: List<PadAction>,
    held: Set<PadKey>,
    listening: PadKey?,
    modifier: Modifier,
    tap: (PadKey) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val font = remember { ResourcesCompat.getFont(context, R.font.rajdhani_semibold) }
    // Captions: the D-pad directions say only "D-pad" (the chip next to it shows which).
    val dpadShort = context.getString(R.string.act_dpad)
    val names = actions.associate { a ->
        a.key to if (a.name in DPAD_NAMES) dpadShort else context.getString(a.name)
    }
    val pulse by rememberInfiniteTransition(label = "pad").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "p",
    )
    // Tap areas, written while drawing (a plain holder: state written in the draw phase would redraw forever).
    val hit = remember { arrayOf<List<Pair<Rect, PadKey>>>(emptyList()) }

    Canvas(
        modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures { p -> hit[0].firstOrNull { it.first.contains(p) }?.second?.let(tap) }
            },
    ) {
        // The controller takes the middle; captions fill the sides, spread over the whole height.
        // Caption width: the longest action name plus its chip, at most a third of the width.
        val measure = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = with(density) { 13.5.dp.toPx() }; typeface = font }
        val longest = names.values.maxOfOrNull { measure.measureText(it) } ?: 0f
        val capW = (longest + with(density) { 64.dp.toPx() }).coerceIn(size.width * 0.18f, size.width * 0.32f)
        val unit = minOf((size.width - 2 * capW) / 100f, size.height * 0.86f / 62f)
        val ox = (size.width - 100f * unit) / 2f
        val oy = (size.height - 62f * unit) / 2f
        fun at(x: Float, y: Float) = Offset(ox + x * unit, oy + y * unit)

        drawBody(::at, unit)

        val hits = mutableListOf<Pair<Rect, PadKey>>()
        // Buttons.
        for ((key, s) in SPOTS) {
            val action = map[key.ordinal]
            val isListening = listening != null && action == listening.ordinal
            val pressed = key in held
            val fill = when {
                pressed -> DbColors.Ember
                isListening -> DbColors.Ember.copy(alpha = 0.25f + 0.35f * pulse)
                else -> Color(0xFF2B323D)
            }
            val c = at(s.x, s.y)
            val r = s.r * unit
            when (s.kind) {
                ROUND -> {
                    drawCircle(fill, r, c)
                    drawCircle(if (action != key.ordinal) DbColors.Ember else Color(0xFF4A5462), r, c, style = Stroke(1.2f * density.density))
                    if (s.r > 5f) drawCircle(Color(0xFF1A1F27), r * 0.62f, c)
                }
                PILL -> {
                    val w = if (s.y < 15f) 13f * unit else 5.5f * unit
                    val h = 2f * r
                    drawRoundRect(fill, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(h / 2))
                    drawRoundRect(if (action != key.ordinal) DbColors.Ember else Color(0xFF4A5462), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(h / 2), style = Stroke(1.2f * density.density))
                }
                SQUARE -> {
                    drawRoundRect(fill, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), CornerRadius(r * 0.35f))
                    if (action != key.ordinal) drawRoundRect(DbColors.Ember, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), CornerRadius(r * 0.35f), style = Stroke(1.2f * density.density))
                }
            }
            val glyph = PadRemap.glyph(key, profile)
            drawText(glyph, c.x, c.y, (if (s.kind == ROUND && s.r < 5f) 3.4f else if (s.kind == PILL && s.y > 15f) 1.9f else 2.8f) * unit, if (pressed) Color(0xFF2A1A08) else DbColors.Silver, font, center = true)
            hits += Rect(c.x - r - 4 * unit, c.y - r - 2 * unit, c.x + r + 4 * unit, c.y + r + 2 * unit) to key
        }

        // Captions with leader lines: [action name][button chip]• on the left, •[chip][name] on the right.
        val textPx = with(density) { 13.5.dp.toPx() }
        val chipPx = with(density) { 11.dp.toPx() }
        val chipH = with(density) { 20.dp.toPx() }
        val gap = with(density) { 6.dp.toPx() }
        fun side(order: List<PadKey>, left: Boolean) {
            val top = size.height * 0.07f
            val step = (size.height * 0.86f) / (order.size - 1)
            order.forEachIndexed { i, key ->
                val s = SPOTS.getValue(key)
                val y = top + i * step
                val edgeX = if (left) capW else size.width - capW
                val btn = at(s.x, s.y)
                val from = Offset(btn.x + if (left) -s.r * unit else s.r * unit, btn.y)
                val action = map[key.ordinal]
                val isListening = listening != null && action == listening.ordinal
                val pressed = key in held
                val lineColor = if (pressed || isListening) DbColors.Ember else Color(0xFF3A4350)
                drawLine(lineColor, from, Offset(edgeX, y), 1f * density.density)
                drawCircle(lineColor, 2.2f * density.density, Offset(edgeX, y))
                val changed = action != key.ordinal
                // Button chip.
                val glyph = PadRemap.glyph(key, profile)
                val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = chipPx; typeface = font }
                val chipW = maxOf(gp.measureText(glyph) + 2 * gap, chipH * 1.4f)
                val chipX = if (left) edgeX - gap - chipW else edgeX + gap
                drawRoundRect(if (pressed) DbColors.Ember else Color(0xFF242A34), Offset(chipX, y - chipH / 2), Size(chipW, chipH), CornerRadius(chipH / 2))
                drawRoundRect(if (changed) DbColors.Ember else Color(0xFF3A4350), Offset(chipX, y - chipH / 2), Size(chipW, chipH), CornerRadius(chipH / 2), style = Stroke(1f * density.density))
                drawText(glyph, chipX + chipW / 2, y, chipPx, if (pressed) Color(0xFF2A1A08) else DbColors.Silver, font, center = true)
                // Action name.
                val name = if (action < 0) "—" else names[PadKey.entries[action]] ?: ""
                val tx = if (left) chipX - gap else chipX + chipW + gap
                drawText(
                    name, tx, y, textPx,
                    when {
                        pressed -> DbColors.EmberGlow
                        isListening -> DbColors.EmberGlow
                        changed -> DbColors.Ember
                        action < 0 -> DbColors.TextFaint
                        else -> DbColors.Text
                    },
                    font, alignRight = left, maxWidth = capW - chipW - 3 * gap, centerY = true,
                )
                hits += Rect(if (left) 0f else edgeX, y - step / 2, if (left) edgeX else size.width, y + step / 2) to key
            }
        }
        side(LEFT_ORDER, true)
        side(RIGHT_ORDER, false)
        hit[0] = hits
    }
}

/** The controller's body: a rounded bar and two grips, a rim of the line colour around them. */
private fun DrawScope.drawBody(at: (Float, Float) -> Offset, unit: Float) {
    fun shape(color: Color, grow: Float) {
        val tl = at(9f - grow, 14f - grow)
        drawRoundRect(color, tl, Size((82f + 2 * grow) * unit, (30f + 2 * grow) * unit), CornerRadius((14f + grow) * unit))
        drawCircle(color, (14.5f + grow) * unit, at(24f, 45f))
        drawCircle(color, (14.5f + grow) * unit, at(76f, 45f))
    }
    shape(Color(0xFF39424F), 0.6f)
    shape(Color(0xFF1C2129), 0f)
    // Shoulder plates.
    drawRoundRect(Color(0xFF161A21), at(16f, 1f), Size(18f * unit, 13f * unit), CornerRadius(4f * unit))
    drawRoundRect(Color(0xFF161A21), at(66f, 1f), Size(18f * unit, 13f * unit), CornerRadius(4f * unit))
}

private fun DrawScope.drawText(
    text: String,
    x: Float,
    y: Float,
    sizePx: Float,
    color: Color,
    font: android.graphics.Typeface?,
    center: Boolean = false,
    alignRight: Boolean = false,
    maxWidth: Float = 0f,
    centerY: Boolean = false,
) {
    val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = sizePx
        typeface = font
        textAlign = when {
            center -> Paint.Align.CENTER
            alignRight -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }
    }
    val t = if (maxWidth > 0f) TextUtils.ellipsize(text, p, maxWidth, TextUtils.TruncateAt.END).toString() else text
    val baseline = if (center || centerY) y - (p.descent() + p.ascent()) / 2f else y
    drawIntoCanvas { it.nativeCanvas.drawText(t, x, baseline, p) }
}
