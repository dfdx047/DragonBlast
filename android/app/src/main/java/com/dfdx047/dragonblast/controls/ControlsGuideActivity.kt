package com.dfdx047.dragonblast.controls

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.ControlScheme
import com.dfdx047.dragonblast.data.Settings
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.game.TouchControlsView
import com.dfdx047.dragonblast.ui.theme.DbColors
import com.dfdx047.dragonblast.ui.theme.DragonBlastTheme

/**
 * How the controls work, per fight scheme: the player's own fight layout with a number on every button, what each
 * one does, the button combinations, and how the scheme plays on a physical controller.
 */
class ControlsGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPaths.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val settings = SettingsStore(this).load()
        setContent { DragonBlastTheme { ControlsGuide(settings) { finish() } } }
    }
}

/** Name and explanation of a layout element, by id (GameActions); helpers and macros of the editor included. */
private data class Explained(val name: Int, val desc: Int)

private val EXPLAINED = mapOf(
    "stick" to Explained(R.string.act_stick, R.string.guide_stick),
    "melee" to Explained(R.string.act_melee, R.string.act_melee_rb2),
    "ki" to Explained(R.string.act_ki, R.string.act_ki_rb2),
    "signature" to Explained(R.string.act_signature, R.string.act_signature_rb2),
    "dash" to Explained(R.string.act_dash, R.string.act_dash_rb2),
    "guard" to Explained(R.string.act_guard, R.string.act_guard_rb2),
    "ascend" to Explained(R.string.act_ascend, R.string.act_ascend_rb2),
    "descend" to Explained(R.string.act_descend, R.string.act_descend_rb2),
    "supers" to Explained(R.string.act_supers, R.string.guide_supers),
    "diamond" to Explained(R.string.act_diamond, R.string.act_diamond_rb2),
    "ultimate" to Explained(R.string.act_ultimate, R.string.act_ultimate_rb2),
    "charge" to Explained(R.string.act_charge, R.string.act_charge_rb2),
    "vanishing" to Explained(R.string.act_vanishing, R.string.guide_vanishing),
    "counter" to Explained(R.string.act_counter, R.string.guide_counter),
    "throw" to Explained(R.string.act_throw, R.string.guide_throw),
    "sway" to Explained(R.string.act_sway, R.string.guide_sway),
    "rising" to Explained(R.string.act_rising, R.string.guide_rising),
    "super1" to Explained(R.string.act_supers, R.string.guide_super_n),
    "super2" to Explained(R.string.act_supers, R.string.guide_super_n),
    "super3" to Explained(R.string.act_supers, R.string.guide_super_n),
    "super4" to Explained(R.string.act_supers, R.string.guide_super_n),
    "back" to Explained(R.string.act_back, R.string.guide_back),
    "start" to Explained(R.string.act_pause, R.string.act_pause_desc),
    GameActions.TOGGLE to Explained(R.string.act_toggle, R.string.guide_toggle),
    "bt3_square" to Explained(R.string.act_melee, R.string.act_melee_bt3),
    "bt3_triangle" to Explained(R.string.act_ki, R.string.act_ki_bt3),
    "bt3_circle" to Explained(R.string.act_guard, R.string.act_guard_bt3),
    "bt3_cross" to Explained(R.string.act_dash, R.string.act_dash_bt3),
    "bt3_r1" to Explained(R.string.act_ascend, R.string.act_ascend_bt3),
    "bt3_r2" to Explained(R.string.act_descend, R.string.act_descend_bt3),
    "bt3_l1" to Explained(R.string.act_lockon, R.string.act_lockon_bt3),
    "bt3_l2" to Explained(R.string.act_charge_blasts, R.string.act_charge_bt3),
    "bt3_r3" to Explained(R.string.act_transform, R.string.act_transform_bt3),
    "bt3_blast1" to Explained(R.string.act_blast1, R.string.guide_blast1),
    "bt3_blast2" to Explained(R.string.act_supers, R.string.guide_blast2),
    "bt3_ultimate" to Explained(R.string.act_ultimate, R.string.guide_ult_bt3),
)

@Composable
private fun ControlsGuide(settings: Settings, close: () -> Unit) {
    val context = LocalContext.current
    var scheme by remember { mutableStateOf(settings.controlScheme) }
    var focus by remember { mutableStateOf<String?>(null) }
    val layout = remember(scheme) { ControlLayoutStore.load(LayoutKind.GAMEPLAY, scheme) }
    val shown = layout.elements.filter { it.visible && it.kind != ElementKind.DPAD }

    Column(Modifier.fillMaxSize().background(DbColors.Ink)) {
        Row(
            Modifier.fillMaxWidth().background(DbColors.Graphite).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = DbColors.Text) }
            Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleMedium, color = DbColors.Text, modifier = Modifier.weight(1f))
            listOf(
                ControlScheme.RAGING_BLAST to "Raging Blast",
                ControlScheme.MODERN to stringResource(R.string.ctl_scheme_modern),
                ControlScheme.TENKAICHI to "Tenkaichi 3",
            ).forEach { (sc, label) ->
                val on = sc == scheme
                val shape = RoundedCornerShape(10.dp)
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) DbColors.EmberGlow else DbColors.TextDim,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(shape)
                        .background(if (on) DbColors.Ember.copy(alpha = 0.10f) else DbColors.SlateHigh)
                        .border(1.dp, if (on) DbColors.Ember else DbColors.Line, shape)
                        .clickable { scheme = sc; focus = null }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight * 1.6f
            val boxHeight = maxHeight
            val preview: @Composable (Modifier, Boolean) -> Unit = { m, hf -> Preview(settings.copy(controlScheme = scheme), layout, shown, focus, m, hf) }
            val list: @Composable (Modifier) -> Unit = { m ->
                LazyColumn(m.padding(horizontal = 10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    item {
                        Text(
                            stringResource(
                                when (scheme) {
                                    ControlScheme.RAGING_BLAST -> R.string.guide_intro_rb2
                                    ControlScheme.MODERN -> R.string.guide_intro_modern
                                    ControlScheme.TENKAICHI -> R.string.guide_intro_bt3
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = DbColors.TextDim,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    itemsIndexed(shown, key = { _, e -> e.id }) { i, e ->
                        ElementRow(i + 1, e, scheme, focus == e.id) { focus = if (focus == e.id) null else e.id }
                    }
                    item {
                        Section(
                            stringResource(R.string.guide_combos),
                            stringResource(
                                when (scheme) {
                                    ControlScheme.RAGING_BLAST -> R.string.guide_combos_rb2
                                    ControlScheme.MODERN -> R.string.guide_combos_modern
                                    ControlScheme.TENKAICHI -> R.string.guide_combos_bt3
                                },
                            ),
                        )
                    }
                    item {
                        Section(
                            stringResource(R.string.guide_pad),
                            stringResource(
                                when (scheme) {
                                    ControlScheme.TENKAICHI -> R.string.guide_pad_bt3
                                    ControlScheme.MODERN -> R.string.guide_pad_modern
                                    ControlScheme.RAGING_BLAST -> R.string.guide_pad_rb2
                                },
                            ),
                        )
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(context, PadRemapActivity::class.java)) },
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                        ) {
                            Icon(Icons.Outlined.Gamepad, null, tint = DbColors.Ember)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.ctl_pad_remap), color = DbColors.Text)
                        }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1.25f).fillMaxHeight(), contentAlignment = Alignment.Center) { preview(Modifier.padding(10.dp).fillMaxWidth(), false) }
                    list(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(boxHeight * 0.46f), contentAlignment = Alignment.Center) {
                        preview(Modifier.padding(10.dp).fillMaxHeight(), true)
                    }
                    list(Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

/** The fight layout drawn by the game's own view, a numbered badge on each element. */
@Composable
private fun Preview(s: Settings, layout: ControlLayout, shown: List<ControlElement>, focus: String?, modifier: Modifier, heightFirst: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val dm = context.resources.displayMetrics
    val longSide = maxOf(dm.widthPixels, dm.heightPixels)
    val shortSide = minOf(dm.widthPixels, dm.heightPixels).coerceAtLeast(1)
    // The touch controls cover the whole screen (on square handhelds the bars of the 16:9 picture too).
    val ratio = longSide.toFloat() / shortSide
    val font = remember { ResourcesCompat.getFont(context, R.font.rajdhani_bold) }
    val pulse by rememberInfiniteTransition(label = "guide").animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "g",
    )
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .aspectRatio(ratio, matchHeightConstraintsFirst = heightFirst)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3442), Color(0xFF1B2028), Color(0xFF15130F))))
            .border(1.dp, DbColors.Line, shape),
    ) {
        val viewRef = remember { arrayOfNulls<TouchControlsView>(1) }
        AndroidView(
            factory = { ctx -> TouchControlsView(ctx).apply { referenceWidthPx = longSide; setOnTouchListener { _, _ -> true } }.also { viewRef[0] = it } },
            update = { v ->
                v.applySettings(s)
                v.layout = layout
            },
            modifier = Modifier.matchParentSize(),
        )
        Canvas(Modifier.matchParentSize()) {
            val r = 8.5f * density.density
            shown.forEachIndexed { i, e ->
                val at = viewRef[0]?.takeIf { it.width > 0 }?.centerOf(e)
                val x = at?.x ?: (e.x * size.width)
                val y = at?.y ?: (e.y * size.height)
                val c = Offset(x, y)
                val on = focus == e.id
                if (on) drawCircle(DbColors.Ember.copy(alpha = pulse), e.sizeDp * density.density * (s.touchScale / 100f) * size.width / longSide * 0.62f, c, style = Stroke(2.5f * density.density))
                // Bottom-left of the element, away from its label chip (top-right); flipped near an edge so it stays in.
                val sx = if (x - r * 2.6f < 0f) 1f else -1f
                val sy = if (y + r * 2.6f > size.height) -1f else 1f
                val b = Offset(x + sx * r * 1.3f, y + sy * r * 1.3f)
                drawCircle(if (on) DbColors.Ember else Color(0xEE0B0D11), r, b)
                drawCircle(DbColors.Ember.copy(alpha = 0.8f), r, b, style = Stroke(1f * density.density))
                drawIntoCanvas {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = (if (on) Color(0xFF2A1A08) else DbColors.EmberGlow).toArgb()
                        textSize = 11f * density.density
                        typeface = font
                        textAlign = Paint.Align.CENTER
                    }
                    it.nativeCanvas.drawText("${i + 1}", b.x, b.y - (p.descent() + p.ascent()) / 2f, p)
                }
            }
        }
    }
}

@Composable
private fun ElementRow(n: Int, e: ControlElement, scheme: ControlScheme, focused: Boolean, onClick: () -> Unit) {
    val ex = EXPLAINED[e.id]
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(if (focused) DbColors.Ember.copy(alpha = 0.12f) else DbColors.Slate)
            .border(1.dp, if (focused) DbColors.Ember else DbColors.LineSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (focused) DbColors.Ember else DbColors.SlateHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.labelMedium, color = if (focused) Color(0xFF2A1A08) else DbColors.EmberGlow)
        }
        Spacer(Modifier.width(10.dp))
        if (e.kind != ElementKind.STICK) {
            Icon(painterResource(e.icon.drawable), null, tint = DbColors.Silver, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (ex != null) stringResource(ex.name) else e.label.ifBlank { stringResource(R.string.guide_custom) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = DbColors.Text,
                    fontWeight = FontWeight.SemiBold,
                )
                val tag = buttonTag(e, scheme)
                if (tag.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = DbColors.TextDim,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(DbColors.SlateHighest).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                stringResource(ex?.desc ?: R.string.guide_custom_desc),
                style = MaterialTheme.typography.bodySmall,
                color = DbColors.TextFaint,
            )
        }
    }
}

/** The pad button(s) an element sends, in the scheme's symbols (BT3: PlayStation). */
private fun buttonTag(e: ControlElement, scheme: ControlScheme): String {
    if (e.kind == ElementKind.STICK) return ""
    if (e.kind == ElementKind.RADIAL) return "RS"
    val inputs = (e.action as? ControlAction.Press)?.inputs ?: return e.label
    if (inputs.isEmpty()) return ""
    val ps = scheme == ControlScheme.TENKAICHI
    return inputs.sortedBy { it.ordinal }.joinToString("+") { GameActions.classicLabel(it, ps) }
}

@Composable
private fun Section(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = DbColors.Ember)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim)
    }
}
