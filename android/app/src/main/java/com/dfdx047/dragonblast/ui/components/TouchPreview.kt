package com.dfdx047.dragonblast.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.dfdx047.dragonblast.data.ButtonStyle
import com.dfdx047.dragonblast.data.TouchPreset
import com.dfdx047.dragonblast.data.tuning
import com.dfdx047.dragonblast.ui.theme.DbColors

/**
 * A drawing of the on-screen controller over a dim fight scene: where each control sits for the chosen preset,
 * the face buttons in the chosen style and, for Pro, the touch areas and the slide paths between buttons.
 * Positions are fractions of a 16:9 screen, the same numbers the in-game controller uses.
 */
@Composable
fun TouchPreview(preset: TouchPreset, style: ButtonStyle, leftHanded: Boolean, opacity: Int, scale: Int, modifier: Modifier = Modifier) {
    val text = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        val w = size.width
        val h = size.height
        // the screen: dark arena
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF151A22), Color(0xFF1D1C1E), Color(0xFF0E0F12))),
            cornerRadius = CornerRadius(h * 0.06f),
        )
        drawRect(Color(0xFF0A0B0D), Offset(0f, h * 0.62f), Size(w, h * 0.38f), alpha = 0.6f)
        // two fighters as soft silhouettes
        drawOval(Color(0xFF2B3442), Offset(w * 0.36f, h * 0.30f), Size(w * 0.06f, h * 0.34f))
        drawOval(Color(0xFF3A2F28), Offset(w * 0.58f, h * 0.28f), Size(w * 0.06f, h * 0.36f))
        // HUD bars
        drawRoundRect(Color(0xFF8AA9C7).copy(alpha = 0.35f), Offset(w * 0.04f, h * 0.05f), Size(w * 0.34f, h * 0.025f), CornerRadius(h))
        drawRoundRect(Color(0xFFDDA05A).copy(alpha = 0.35f), Offset(w * 0.62f, h * 0.05f), Size(w * 0.34f, h * 0.025f), CornerRadius(h))

        val a = (opacity / 100f).coerceIn(0.25f, 1f)
        val s = scale / 100f
        val tune = preset.tuning()
        fun fx(x: Float) = (if (leftHanded) 1f - x else x) * w

        // ---- stick ----
        val stickC = Offset(fx(0.14f), h * 0.70f)
        val stickR = h * 0.15f * s * (tune.stickRadiusDp / 70f)
        if (preset == TouchPreset.PRO || tune.stickMode == com.dfdx047.dragonblast.data.StickMode.DYNAMIC) {
            // the zone where the stick appears under the thumb
            val zx = if (leftHanded) w * 0.55f else 0f
            drawRoundRect(
                DbColors.Steel.copy(alpha = 0.06f * a + 0.02f), Offset(zx + w * 0.01f, h * 0.30f), Size(w * 0.44f, h * 0.66f),
                CornerRadius(h * 0.05f),
            )
            drawRoundRect(
                DbColors.Steel.copy(alpha = 0.35f * a), Offset(zx + w * 0.01f, h * 0.30f), Size(w * 0.44f, h * 0.66f),
                CornerRadius(h * 0.05f), style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))),
            )
        }
        glass(stickC, stickR, a)
        drawCircle(Brush.radialGradient(listOf(DbColors.Silver.copy(alpha = 0.9f * a), DbColors.SteelDeep.copy(alpha = 0.8f * a)), stickC + Offset(stickR * 0.15f, -stickR * 0.1f), stickR * 0.42f), stickR * 0.40f, stickC + Offset(stickR * 0.15f, -stickR * 0.1f))

        // ---- face buttons (diamond) ----
        val faceC = Offset(fx(0.855f), h * 0.66f)
        val btnR = h * 0.075f * s * (if (preset == TouchPreset.CASUAL) 1.12f else 1f)
        val gap = btnR * 2.25f
        val face = listOf(
            Offset(0f, gap * 0.95f) to 0,      // bottom: A / cross / B(n)
            Offset(gap, 0f) to 1,              // right: B / circle / A(n)
            Offset(-gap, 0f) to 2,             // left: X / square / Y(n)
            Offset(0f, -gap * 0.95f) to 3,     // top: Y / triangle / X(n)
        )
        if (preset == TouchPreset.PRO) {
            // bigger touch areas and the slide paths between neighbours
            for ((o, _) in face) drawCircle(DbColors.Ember.copy(alpha = 0.10f * a), btnR * tune.buttonHitScale * 1.18f, faceC + o)
            val ring = face.map { faceC + it.first }
            val order = listOf(2, 3, 1, 0, 2)
            for (i in 0 until order.size - 1) {
                drawLine(
                    DbColors.Ember.copy(alpha = 0.45f * a), ring[order[i]], ring[order[i + 1]], strokeWidth = 1.4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f)),
                )
            }
        }
        for ((o, i) in face) faceButton(text, faceC + o, btnR, style, i, a)

        // ---- shoulders: LB/LT over the stick side, RB/RT over the buttons ----
        val shoulderY = if (preset == TouchPreset.PRO) h * 0.36f else h * 0.22f
        val sw = w * 0.085f * s
        val sh = h * 0.085f * s
        val leftX = if (preset == TouchPreset.PRO) 0.06f else 0.04f
        shoulder(text, Offset(fx(leftX) - sw / 2, shoulderY - sh * 1.25f), Size(sw, sh), "LT", a, style)
        shoulder(text, Offset(fx(leftX + 0.10f) - sw / 2, shoulderY - sh * 1.25f), Size(sw, sh), "LB", a, style)
        shoulder(text, Offset(fx(0.96f - 0.10f) - sw / 2, shoulderY - sh * 1.25f), Size(sw, sh), "RB", a, style)
        shoulder(text, Offset(fx(0.96f) - sw / 2, shoulderY - sh * 1.25f), Size(sw, sh), "RT", a, style)
        if (preset == TouchPreset.CASUAL) {
            // charge and guard held by a tap (toggle): a small ring mark
            for (c in listOf(Offset(fx(0.04f), shoulderY - sh * 0.75f), Offset(fx(0.96f), shoulderY - sh * 0.75f))) {
                drawCircle(DbColors.Ember.copy(alpha = 0.9f * a), h * 0.012f, c + Offset(0f, -sh * 0.6f), style = Stroke(1.5f))
            }
        }

        // ---- start / back and the side-menu button ----
        pill(Offset(w * 0.43f, h * 0.06f), Size(w * 0.05f, h * 0.045f), a)
        pill(Offset(w * 0.52f, h * 0.06f), Size(w * 0.05f, h * 0.045f), a)
        drawCircle(DbColors.Graphite.copy(alpha = 0.7f * a), h * 0.035f, Offset(w * 0.5f, h * 0.17f))
        for (k in -1..1) drawLine(DbColors.Silver.copy(alpha = 0.9f * a), Offset(w * 0.5f - h * 0.016f, h * 0.17f + k * h * 0.011f), Offset(w * 0.5f + h * 0.016f, h * 0.17f + k * h * 0.011f), 1.6f)
    }
}

private fun DrawScope.glass(c: Offset, r: Float, a: Float) {
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f * a), Color.White.copy(alpha = 0.05f * a), Color(0x22101318)), c + Offset(0f, -r * 0.3f), r * 1.3f),
        r, c,
    )
    drawCircle(DbColors.Silver.copy(alpha = 0.45f * a), r, c, style = Stroke(width = 1.4f))
}

private val XboxColors = listOf(Color(0xFF7DB85A), Color(0xFFD9655B), Color(0xFF5B8FD9), Color(0xFFD9B44A))
private val PsColors = listOf(Color(0xFF8DB4E2), Color(0xFFE07A7A), Color(0xFFD99AC5), Color(0xFF6CCFA6))

private fun DrawScope.faceButton(text: TextMeasurer, c: Offset, r: Float, style: ButtonStyle, i: Int, a: Float) {
    glass(c, r, a)
    val glyphColor: Color
    val label: String
    when (style) {
        ButtonStyle.DRAGON -> {
            glyphColor = DbColors.EmberGlow
            label = listOf("A", "B", "X", "Y")[i]
        }
        ButtonStyle.XBOX -> {
            glyphColor = XboxColors[i]
            label = listOf("A", "B", "X", "Y")[i]
        }
        ButtonStyle.NINTENDO -> {
            glyphColor = DbColors.Silver
            label = listOf("B", "A", "Y", "X")[i]
        }
        ButtonStyle.PLAYSTATION -> {
            glyphColor = PsColors[i]
            label = ""
            val g = r * 0.42f
            val sw = Stroke(width = r * 0.13f)
            when (i) {
                0 -> {
                    drawLine(glyphColor.copy(alpha = a), c + Offset(-g, -g), c + Offset(g, g), r * 0.13f)
                    drawLine(glyphColor.copy(alpha = a), c + Offset(-g, g), c + Offset(g, -g), r * 0.13f)
                }
                1 -> drawCircle(glyphColor.copy(alpha = a), g, c, style = sw)
                2 -> drawRect(glyphColor.copy(alpha = a), c - Offset(g * 0.9f, g * 0.9f), Size(g * 1.8f, g * 1.8f), style = sw)
                else -> {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(c.x, c.y - g)
                        lineTo(c.x + g, c.y + g * 0.75f)
                        lineTo(c.x - g, c.y + g * 0.75f)
                        close()
                    }
                    drawPath(p, glyphColor.copy(alpha = a), style = sw)
                }
            }
        }
    }
    if (label.isNotEmpty()) {
        val layout = text.measure(label, TextStyle(color = glyphColor.copy(alpha = a), fontSize = (r / density * 0.95f).sp, fontWeight = FontWeight.Bold))
        drawText(layout, topLeft = c - Offset(layout.size.width / 2f, layout.size.height / 2f))
    }
}

private fun DrawScope.shoulder(text: TextMeasurer, o: Offset, s: Size, label: String, a: Float, style: ButtonStyle) {
    drawRoundRect(Color.White.copy(alpha = 0.08f * a), o, s, CornerRadius(s.height * 0.4f))
    drawRoundRect(DbColors.Silver.copy(alpha = 0.4f * a), o, s, CornerRadius(s.height * 0.4f), style = Stroke(1.2f))
    val l = when (style) {
        ButtonStyle.PLAYSTATION -> mapOf("LT" to "L2", "LB" to "L1", "RB" to "R1", "RT" to "R2")[label] ?: label
        ButtonStyle.NINTENDO -> mapOf("LT" to "ZL", "LB" to "L", "RB" to "R", "RT" to "ZR")[label] ?: label
        else -> label
    }
    val layout = text.measure(l, TextStyle(color = DbColors.Silver.copy(alpha = 0.9f * a), fontSize = (s.height / density * 0.42f).sp, fontWeight = FontWeight.SemiBold))
    drawText(layout, topLeft = Offset(o.x + (s.width - layout.size.width) / 2, o.y + (s.height - layout.size.height) / 2))
}

private fun DrawScope.pill(o: Offset, s: Size, a: Float) {
    drawRoundRect(Color.White.copy(alpha = 0.08f * a), o, s, CornerRadius(s.height))
    drawRoundRect(DbColors.Silver.copy(alpha = 0.35f * a), o, s, CornerRadius(s.height), style = Stroke(1.1f))
}
