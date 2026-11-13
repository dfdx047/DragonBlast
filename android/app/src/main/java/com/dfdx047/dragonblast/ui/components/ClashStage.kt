package com.dfdx047.dragonblast.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Home stage: a wasteland at dusk seen from far away, no characters.
 *
 * [ready] = false (no game yet): the calm before the fight. Rocks lift off the cracked ground and hover, small
 * stones rise, a faint ki glow breathes on the ground. [ready] = true (game installed): two energy waves meet in
 * a beam clash in the middle (steel-white from the left, ember from the right), push each other back and forth,
 * and every few seconds a shockwave runs over the ground, shakes the view and breaks the floating rocks into
 * shards that fall with gravity. The change between both states is a 1.6 s fade of [intensity].
 *
 * Muted colours on purpose (graphite sky, dust, two soft lights) so the launcher stays calm. Everything that only
 * depends on the size (mountains, cracks, rock shapes) is built once in drawWithCache; the time is read only in
 * the draw phase, so the animation never recomposes. With system animations off it shows one still frame.
 */
@Composable
fun ClashStage(ready: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val context = LocalContext.current
    val animate = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val time: State<Float> = if (animate) {
        rememberInfiniteTransition(label = "stage").animateFloat(
            initialValue = 0f,
            targetValue = LOOP_S,
            animationSpec = infiniteRepeatable(tween((LOOP_S * 1000).toInt(), easing = LinearEasing), RepeatMode.Restart),
            label = "t",
        )
    } else {
        remember { mutableFloatStateOf(STILL_T) }
    }
    val intensity = animateFloatAsState(if (ready) 1f else 0f, tween(1600), label = "intensity")

    Box(modifier) {
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val scene = Scene(size)
                    onDrawBehind { scene.draw(this, time.value, intensity.value) }
                },
        )
        content()
    }
}

/** 60 s loop: every period below divides it, so the loop has no seam. */
private const val LOOP_S = 60f
private const val STILL_T = 7.3f
private const val SHOCK_PERIOD = 4f     // a shockwave every 4 s
private const val TAU = (2 * PI).toFloat()

private val SkyTop = Color(0xFF07090D)
private val SkyMid = Color(0xFF111620)
private val SkyLow = Color(0xFF231F1C)
private val Haze = Color(0xFF3A3129)
private val FarMesa = Color(0xFF141820)
private val NearMesa = Color(0xFF0E1117)
private val GroundTop = Color(0xFF17191D)
private val GroundBottom = Color(0xFF08090B)
private val RockLight = Color(0xFF3A3C40)
private val RockDark = Color(0xFF08090A)
private val Cool = Color(0xFF9CC3EA)        // left wave
private val CoolCore = Color(0xFFE6F2FF)
private val Warm = Color(0xFFE7A45C)        // right wave
private val WarmCore = Color(0xFFFFEBD0)
private val Dust = Color(0xFF8C7F70)

private class Rock(
    val shape: Path,
    val radius: Float,
    val baseX: Float,
    val groundY: Float,
    val period: Float,
    val phase: Float,
    val spin: Float,
    val drift: Float,
    val lift: Float,
    val shards: List<Shard>,
)

private class Shard(val shape: Path, val dx: Float, val dy: Float, val spin: Float)

private class Mote(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

private class Scene(val size: Size) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.62f
    val cx = w * 0.5f
    val cy = h * 0.43f
    val unit = min(w, h)

    private val farRidge = ridge(Random(7), horizon - h * 0.07f, h * 0.13f, 0.035f)
    private val midRidge = ridge(Random(19), horizon - h * 0.02f, h * 0.07f, 0.06f)
    private val buttes = buttes(Random(11))
    private val cracks: List<Path> = buildCracks(Random(3))
    private val rocks: List<Rock> = buildRocks(Random(21))
    private val motes: List<Mote> = List(46) {
        val r = Random(100 + it)
        Mote(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat() * 1.2f, 0.6f + r.nextFloat() * 1.8f, r.nextFloat())
    }
    private val sparks: List<Mote> = List(34) {
        val r = Random(300 + it)
        Mote(r.nextFloat() * TAU, r.nextFloat(), 0.6f + r.nextFloat() * 1.4f, 0.8f + r.nextFloat() * 1.6f, r.nextFloat())
    }
    private val beamPath = Path()

    /** Smooth value noise in 0..1 over [0, 1] with [n] knots (cosine interpolation). */
    private fun noise(r: Random, n: Int): (Float) -> Float {
        val k = FloatArray(n + 2) { r.nextFloat() }
        return { x ->
            val f = x.coerceIn(0f, 1f) * n
            val i = f.toInt()
            val t = (1 - cos((f - i) * PI.toFloat())) / 2
            k[i] * (1 - t) + k[i + 1] * t
        }
    }

    /**
     * A far mountain line: three octaves of noise, the tops shaved flat in places (worn plateaus), closed down to
     * the horizon. [rough] = small jitter so the edge is not perfectly smooth.
     */
    private fun ridge(r: Random, base: Float, height: Float, rough: Float): Path {
        val n1 = noise(r, 5)
        val n2 = noise(r, 13)
        val n3 = noise(r, 37)
        val p = Path()
        p.moveTo(0f, horizon + 2f)
        val steps = 96
        for (i in 0..steps) {
            val f = i / steps.toFloat()
            var v = 0.62f * n1(f) + 0.28f * n2(f) + 0.10f * n3(f)
            if (v > 0.62f) v = 0.62f + (v - 0.62f) * 0.15f      // plateau
            val y = base - height * v + (r.nextFloat() - 0.5f) * h * rough * 0.1f
            p.lineTo(f * w, y)
        }
        p.lineTo(w, horizon + 2f)
        p.close()
        return p
    }

    /** Two or three tall flat-topped rock pillars near the sides (the classic wasteland buttes), never in the middle. */
    private fun buttes(r: Random): List<Path> = listOf(0.08f to 0.13f, 0.80f to 0.11f, 0.94f to 0.07f).map { (fx, fw) ->
        val p = Path()
        val x0 = w * fx
        val bw = w * fw
        val top = horizon - h * (0.16f + r.nextFloat() * 0.10f)
        val baseY = horizon + 2f
        p.moveTo(x0 - bw * 0.15f, baseY)
        // left wall, eroded
        var y = baseY
        var x = x0 - bw * 0.15f
        while (y > top) {
            y -= h * (0.015f + r.nextFloat() * 0.02f)
            x += bw * (0.01f + r.nextFloat() * 0.04f) * (if (r.nextFloat() < 0.2f) -1 else 1)
            p.lineTo(x, maxOf(y, top))
        }
        // flat, slightly uneven top
        val right = x0 + bw * 0.85f
        var tx = x
        while (tx < right) {
            tx += bw * (0.08f + r.nextFloat() * 0.1f)
            p.lineTo(minOf(tx, right), top + (r.nextFloat() - 0.3f) * h * 0.006f)
        }
        // right wall down
        y = top
        x = right
        while (y < baseY) {
            y += h * (0.015f + r.nextFloat() * 0.02f)
            x += bw * (0.01f + r.nextFloat() * 0.04f) * (if (r.nextFloat() < 0.2f) -1 else 1)
            p.lineTo(x, minOf(y, baseY))
        }
        p.close()
        p
    }

    /** Cracks fanning out towards the viewer from under the clash point (flattened by the perspective). */
    private fun buildCracks(r: Random): List<Path> = List(9) { i ->
        val p = Path()
        // 0.08 pi .. 0.92 pi: sideways to straight down the screen, never up into the horizon
        val ang = PI.toFloat() * (0.08f + 0.84f * i / 8f) + (r.nextFloat() - 0.5f) * 0.15f
        var x = cx + (r.nextFloat() - 0.5f) * unit * 0.04f
        var y = horizon + h * 0.05f
        p.moveTo(x, y)
        val segs = 4 + r.nextInt(4)
        val len = unit * (0.045f + r.nextFloat() * 0.035f)
        for (s in 0 until segs) {
            val a = ang + (r.nextFloat() - 0.5f) * 0.8f
            val grow = 1f + s * 0.25f       // segments get longer as they come closer
            x += cos(a) * len * grow
            y += kotlin.math.abs(sin(a)) * len * 0.55f * grow
            p.lineTo(x, y)
            if (s == 1 && r.nextBoolean()) {
                val b = Path()
                b.moveTo(x, y)
                b.lineTo(x + cos(a + 0.9f) * len * 0.8f, y + kotlin.math.abs(sin(a + 0.9f)) * len * 0.4f)
                p.addPath(b)
            }
        }
        p
    }

    private fun rockShape(r: Random, radius: Float, verts: Int): Path {
        val p = Path()
        for (i in 0 until verts) {
            val a = i / verts.toFloat() * TAU
            val rr = radius * (0.65f + r.nextFloat() * 0.45f)
            val x = cos(a) * rr
            val y = sin(a) * rr * 0.82f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    private fun buildRocks(r: Random): List<Rock> = List(13) { i ->
        val side = if (i % 2 == 0) -1f else 1f
        val spread = 0.06f + (i / 13f) * 0.40f + r.nextFloat() * 0.05f
        val radius = unit * (0.018f + r.nextFloat() * 0.032f) * (if (i < 4) 1.25f else 1f)
        val shards = List(5) {
            val a = it / 5f * TAU + r.nextFloat()
            Shard(rockShape(r, radius * (0.28f + r.nextFloat() * 0.18f), 5), cos(a), sin(a) - 0.6f, (r.nextFloat() - 0.5f) * 8f)
        }
        Rock(
            shape = rockShape(r, radius, 7 + r.nextInt(3)),
            radius = radius,
            baseX = cx + side * w * spread,
            groundY = horizon + h * (0.03f + r.nextFloat() * 0.16f),
            // periods that divide 60 s
            period = listOf(6f, 7.5f, 10f, 12f)[r.nextInt(4)],
            phase = r.nextFloat(),
            spin = (r.nextFloat() - 0.5f) * 1.4f,
            drift = side * (0.3f + r.nextFloat() * 0.7f),
            lift = h * (0.10f + r.nextFloat() * 0.22f),
            shards = shards,
        )
    }

    fun draw(d: DrawScope, t: Float, k: Float) = with(d) {
        // shockwave clock and the screen shake right after each wave
        val shockT = (t % SHOCK_PERIOD) / SHOCK_PERIOD
        val shake = if (k > 0.05f && shockT < 0.12f) (1f - shockT / 0.12f) * k * unit * 0.006f else 0f
        val sx = sin(t * 91f) * shake
        val sy = cos(t * 77f) * shake
        // the clash point drifts as the two waves push each other
        val push = (sin(t * TAU / 6f) * 0.6f + sin(t * TAU / 2.5f) * 0.25f + sin(t * TAU / 1.2f) * 0.15f)
        val clashX = cx + push * w * 0.06f
        val clashY = cy + sin(t * TAU / 3f) * h * 0.008f

        translate(sx, sy) {
            sky(t, k, clashX, clashY)
            ground(t, k, clashX)
            motes(t, k)
            rocks(t, k, clashX, clashY)
            if (k > 0.01f) {
                shockwave(shockT, k, clashX)
                beams(t, k, clashX, clashY)
                core(t, k, clashX, clashY)
                sparks(t, k, clashX, clashY)
            }
        }
        vignette()
    }

    private fun DrawScope.sky(t: Float, k: Float, clashX: Float, clashY: Float) {
        drawRect(Brush.verticalGradient(0f to SkyTop, 0.45f to SkyMid, 0.62f to SkyLow, 1f to SkyLow, endY = horizon))
        // dust haze on the horizon
        drawRect(
            Brush.verticalGradient(0f to Color.Transparent, 0.7f to Haze.copy(alpha = 0.35f), 1f to Color.Transparent, startY = horizon - h * 0.22f, endY = horizon + h * 0.06f),
            topLeft = Offset(0f, horizon - h * 0.22f), size = Size(w, h * 0.28f),
        )
        // calm: a low ki glow breathing on the ground; ready: the light of the clash on the clouds
        val breath = 0.5f + 0.5f * sin(t * TAU / 5f)
        val calmGlow = (1f - k) * (0.10f + 0.06f * breath)
        if (calmGlow > 0.005f) {
            drawCircle(
                Brush.radialGradient(listOf(Warm.copy(alpha = calmGlow), Color.Transparent), Offset(cx, horizon + h * 0.04f), unit * 0.55f),
                unit * 0.55f, Offset(cx, horizon + h * 0.04f),
            )
        }
        if (k > 0.01f) {
            val r = unit * 0.95f
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFBFA88A).copy(alpha = 0.16f * k), Color.Transparent), Offset(clashX, clashY), r),
                r, Offset(clashX, clashY),
            )
        }
        // aerial perspective: the far line lighter and hazier, the near one darker
        drawPath(farRidge, Brush.verticalGradient(listOf(Color(0xFF1B2029), Color(0xFF262422)), startY = horizon - h * 0.22f, endY = horizon))
        drawPath(midRidge, Brush.verticalGradient(listOf(FarMesa, Color(0xFF1C1B1B)), startY = horizon - h * 0.1f, endY = horizon))
        for (b in buttes) drawPath(b, Brush.verticalGradient(listOf(Color(0xFF15181E), NearMesa), startY = horizon - h * 0.26f, endY = horizon))
    }

    private fun DrawScope.ground(t: Float, k: Float, clashX: Float) {
        drawRect(Brush.verticalGradient(listOf(GroundTop, GroundBottom), startY = horizon, endY = h), topLeft = Offset(0f, horizon), size = Size(w, h - horizon))
        // the light pool under the clash
        if (k > 0.01f) {
            val gy = horizon + h * 0.06f
            drawOval(
                Brush.radialGradient(listOf(Color(0xFFD8B48A).copy(alpha = 0.20f * k), Color.Transparent), Offset(clashX, gy), unit * 0.5f),
                topLeft = Offset(clashX - unit * 0.55f, gy - unit * 0.10f), size = Size(unit * 1.1f, unit * 0.2f),
            )
        }
        // cracks: dark lines with a faint glow that breathes (calm) or flickers (clash)
        val glow = (1f - k) * (0.10f + 0.08f * sin(t * TAU / 5f)) + k * (0.28f + 0.12f * sin(t * 23f))
        for (c in cracks) {
            drawPath(c, Color(0xFF050607), style = Stroke(width = unit * 0.006f, cap = StrokeCap.Round))
            drawPath(c, Warm.copy(alpha = glow.coerceIn(0f, 1f)), style = Stroke(width = unit * 0.0025f, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
        }
    }

    /** Dust drifting along the ground; small stones floating up in the calm. */
    private fun DrawScope.motes(t: Float, k: Float) {
        for (m in motes) {
            val life = ((t * 0.08f * m.speed + m.phase) % 1f)
            val x = ((m.x + life * 0.25f * (if (m.x < 0.5f) -1f else 1f) * (0.3f + k)) % 1f + 1f) % 1f * w
            val rise = life * h * (0.12f + (1f - k) * 0.18f)
            val y = horizon + h * (0.02f + m.y * 0.30f) - rise
            val a = sin(life * PI.toFloat()) * (0.18f + 0.12f * k)
            drawCircle(Dust.copy(alpha = a), m.size * unit * 0.0025f, Offset(x, y))
        }
    }

    private fun DrawScope.rocks(t: Float, k: Float, clashX: Float, clashY: Float) {
        for (r in rocks) {
            val life = ((t / r.period) + r.phase) % 1f
            // calm: slow hover cycle; clash: rise -> float -> shatter at 0.72 -> shards fall
            val bob = sin((t / r.period + r.phase) * TAU) * h * 0.012f
            val riseCalm = r.lift * 0.55f * smooth(min(1f, life * 3f)) * (1f - smooth(max(0f, (life - 0.85f) / 0.15f)))
            val riseClash = r.lift * smooth(min(1f, life / 0.25f))
            val rise = riseCalm * (1f - k) + riseClash * k
            val driftX = r.drift * w * 0.05f * life * k
            val x = r.baseX + driftX
            val y = r.groundY - rise + bob
            val angle = (t * r.spin * 30f + r.phase * 360f) % 360f
            val shattered = k > 0.5f && life > 0.72f
            // contact shadow on the ground
            val shadowA = 0.35f * (1f - min(1f, rise / (r.lift + 1f)) * 0.7f)
            drawOval(Color.Black.copy(alpha = shadowA), Offset(x - r.radius * 1.1f, r.groundY + r.radius * 0.35f), Size(r.radius * 2.2f, r.radius * 0.5f))
            // light side: towards the clash, cool on the left half, warm on the right
            val light = if (x < clashX) Cool else Warm
            val lightA = 0.12f + 0.35f * k
            if (!shattered) {
                // light direction in the rock's own (rotated) frame: lit side towards the clash, or up in the calm
                val lx = if (k > 0.3f) clashX - x else 0f
                val ly = if (k > 0.3f) clashY - y else -1f
                val len = kotlin.math.sqrt(lx * lx + ly * ly).coerceAtLeast(0.001f)
                val rad = -angle * PI.toFloat() / 180f
                val dx = (lx * cos(rad) - ly * sin(rad)) / len * r.radius
                val dy = (lx * sin(rad) + ly * cos(rad)) / len * r.radius
                translate(x, y) {
                    rotate(angle, Offset.Zero) {
                        drawPath(r.shape, Brush.linearGradient(listOf(RockDark, Color(0xFF1C1E22), RockLight), Offset(-dx, -dy), Offset(dx, dy)))
                        drawPath(
                            r.shape,
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent, light.copy(alpha = lightA * 1.6f)), Offset(-dx, -dy), Offset(dx, dy)),
                            style = Stroke(width = unit * 0.003f),
                        )
                    }
                }
                // faint aura under hovering rocks in the calm
                if (k < 0.99f && rise > r.lift * 0.2f) {
                    drawCircle(
                        Brush.radialGradient(listOf(Warm.copy(alpha = 0.10f * (1f - k)), Color.Transparent), Offset(x, y + r.radius), r.radius * 2.2f),
                        r.radius * 2.2f, Offset(x, y + r.radius),
                    )
                }
            } else {
                val s = (life - 0.72f) / 0.28f        // 0..1 after the break
                val fade = (1f - s).coerceIn(0f, 1f) * k
                // the flash of the break
                if (s < 0.15f) {
                    drawCircle(
                        Brush.radialGradient(listOf(light.copy(alpha = 0.6f * (1f - s / 0.15f) * k), Color.Transparent), Offset(x, y), r.radius * 3f),
                        r.radius * 3f, Offset(x, y), blendMode = BlendMode.Plus,
                    )
                }
                for (sh in r.shards) {
                    val vx = sh.dx * unit * 0.22f
                    val vy = sh.dy * unit * 0.22f
                    val g = unit * 0.9f
                    val px = x + vx * s
                    val py = min(r.groundY + r.radius * 0.3f, y + vy * s + 0.5f * g * s * s)
                    translate(px, py) {
                        rotate(angle + sh.spin * s * 120f, Offset.Zero) {
                            drawPath(sh.shape, Color(0xFF16171A).copy(alpha = fade))
                            drawPath(sh.shape, light.copy(alpha = lightA * fade), style = Stroke(width = unit * 0.0016f))
                        }
                    }
                }
            }
        }
    }

    private fun DrawScope.shockwave(s: Float, k: Float, clashX: Float) {
        if (s > 0.55f) return
        val p = s / 0.55f
        val ease = 1f - (1f - p).pow(3)
        val a = (1f - p) * 0.45f * k
        // ring along the ground (flat ellipse) and a faint sphere in the air
        val gy = horizon + h * 0.06f
        val rx = unit * (0.05f + ease * 1.1f)
        drawOval(
            Color(0xFFE9D8C0).copy(alpha = a),
            topLeft = Offset(clashX - rx, gy - rx * 0.16f), size = Size(rx * 2f, rx * 0.32f),
            style = Stroke(width = unit * (0.012f * (1f - p) + 0.002f)), blendMode = BlendMode.Plus,
        )
        drawOval(
            Dust.copy(alpha = a * 0.5f),
            topLeft = Offset(clashX - rx * 0.9f, gy - rx * 0.12f), size = Size(rx * 1.8f, rx * 0.24f),
            style = Stroke(width = unit * 0.03f * (1f - p)),
        )
        val ra = unit * (0.04f + ease * 0.55f)
        drawCircle(Color.White.copy(alpha = a * 0.25f), ra, Offset(clashX, cy), style = Stroke(width = unit * 0.004f), blendMode = BlendMode.Plus)
    }

    /** One energy wave: an uneven tube from off-screen to the clash point, glow layers then a white core. */
    private fun DrawScope.beam(fromLeft: Boolean, t: Float, k: Float, clashX: Float, clashY: Float, color: Color, coreColor: Color) {
        val startX = if (fromLeft) -w * 0.05f else w * 1.05f
        val startY = cy + (if (fromLeft) h * 0.05f else -h * 0.035f)
        val endX = clashX
        val segs = 18
        for (layer in 0..3) {
            val width = unit * when (layer) { 0 -> 0.15f; 1 -> 0.075f; 2 -> 0.034f; else -> 0.014f }
            val alpha = k * when (layer) { 0 -> 0.08f; 1 -> 0.16f; 2 -> 0.45f; else -> 0.95f }
            val col = when (layer) { 3 -> coreColor; 2 -> lerp(color, coreColor, 0.5f); else -> color }
            beamPath.reset()
            for (i in 0..segs) {
                val f = i / segs.toFloat()
                val x = startX + (endX - startX) * f
                val y = startY + (clashY - startY) * f * f * (3 - 2 * f)
                val wob = sin(f * 14f - t * (if (fromLeft) 22f else 19f)) * width * 0.12f
                if (i == 0) beamPath.moveTo(x, y + wob) else beamPath.lineTo(x, y + wob)
            }
            drawPath(beamPath, col.copy(alpha = alpha), style = Stroke(width = width, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
        }
    }

    private fun DrawScope.beams(t: Float, k: Float, clashX: Float, clashY: Float) {
        beam(true, t, k, clashX, clashY, Cool, CoolCore)
        beam(false, t, k, clashX, clashY, Warm, WarmCore)
    }

    private fun DrawScope.core(t: Float, k: Float, clashX: Float, clashY: Float) {
        val flick = 0.85f + 0.15f * sin(t * 37f) * sin(t * 23f)
        val r = unit * 0.16f * flick
        val c = Offset(clashX, clashY)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6E8).copy(alpha = 0.55f * k), Color(0xFFD9B58C).copy(alpha = 0.18f * k), Color.Transparent), c, r * 1.8f), r * 1.8f, c, blendMode = BlendMode.Plus)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = k), Color.White.copy(alpha = 0.6f * k), Color.Transparent), c, r * 0.55f), r * 0.55f, c, blendMode = BlendMode.Plus)
        // short arcs of energy around the core
        for (i in 0 until 7) {
            val seed = floor(t * 12f + i * 3.7f)
            val rr = Random(seed.toInt() * 31 + i)
            val a0 = rr.nextFloat() * TAU
            val len = r * (0.8f + rr.nextFloat() * 1.3f)
            val mid = Offset(clashX + cos(a0) * len * 0.5f + (rr.nextFloat() - 0.5f) * r * 0.4f, clashY + sin(a0) * len * 0.5f + (rr.nextFloat() - 0.5f) * r * 0.4f)
            val end = Offset(clashX + cos(a0) * len, clashY + sin(a0) * len)
            val col = if (cos(a0) < 0) CoolCore else WarmCore
            drawLine(col.copy(alpha = 0.55f * k), c, mid, strokeWidth = unit * 0.0035f, cap = StrokeCap.Round, blendMode = BlendMode.Plus)
            drawLine(col.copy(alpha = 0.35f * k), mid, end, strokeWidth = unit * 0.0025f, cap = StrokeCap.Round, blendMode = BlendMode.Plus)
        }
    }

    private fun DrawScope.sparks(t: Float, k: Float, clashX: Float, clashY: Float) {
        for (s in sparks) {
            val life = ((t * 0.55f * s.speed + s.phase) % 1f)
            val dist = unit * (0.05f + life * 0.42f * s.speed)
            val a = s.x
            val g = life * life * unit * 0.25f
            val px = clashX + cos(a) * dist
            val py = clashY + sin(a) * dist * 0.6f + g
            val tail = Offset(px - cos(a) * unit * 0.02f, py - sin(a) * unit * 0.012f - unit * 0.01f * life)
            val col = if (cos(a) < 0) CoolCore else WarmCore
            val alpha = (1f - life) * 0.8f * k
            drawLine(col.copy(alpha = alpha), tail, Offset(px, py), strokeWidth = s.size * unit * 0.0022f, cap = StrokeCap.Round, blendMode = BlendMode.Plus)
        }
    }

    private fun DrawScope.vignette() {
        val r = max(w, h) * 0.75f
        drawRect(Brush.radialGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f), center = Offset(cx, h * 0.48f), radius = r))
        // darker bottom for the text and the button over it
        drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
    }

    private fun smooth(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c * c * (3 - 2 * c)
    }

    private fun lerp(a: Color, b: Color, f: Float) = Color(
        a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, a.alpha + (b.alpha - a.alpha) * f,
    )
}
