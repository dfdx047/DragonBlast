package com.dfdx047.dragonblast.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.ui.theme.DbColors
import com.dfdx047.dragonblast.ui.theme.Rajdhani

/** Short caption under a setting plus the long explanation behind its "!" button. */
data class Help(val short: String, val long: String? = null)

@Composable
fun help(short: Int, long: Int? = null) = Help(stringResource(short), long?.let { stringResource(it) })

/** A card: a thin outline on a slightly lighter surface; the title with a small icon in a square. */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(DbColors.SlateHighest)
                            .border(1.dp, DbColors.Line, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = DbColors.Ember, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = DbColors.Silver)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

/** The small "!" next to a setting's name: opens the long explanation. */
@Composable
fun InfoButton(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .selectable(selected = false, role = Role.Button, onClick = { open = true }),
        contentAlignment = Alignment.Center,
    ) {
        // drawn, not a font glyph: a bar and a dot inside a thin ring
        androidx.compose.foundation.Canvas(Modifier.size(19.dp)) {
            val c = DbColors.Steel
            val r = size.minDimension / 2
            drawCircle(c.copy(alpha = 0.8f), r - 0.7.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.3.dp.toPx()))
            val sw = 1.8.dp.toPx()
            drawLine(c, androidx.compose.ui.geometry.Offset(r, r * 0.45f), androidx.compose.ui.geometry.Offset(r, r * 1.12f), sw, androidx.compose.ui.graphics.StrokeCap.Round)
            drawCircle(c, sw * 0.62f, androidx.compose.ui.geometry.Offset(r, r * 1.52f))
        }
    }
    if (open) InfoDialog(title, text) { open = false }
}

@Composable
fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DbColors.SlateHigh,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = DbColors.Text) },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = DbColors.TextDim,
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.got_it), color = DbColors.Ember) } },
    )
}

/** Name, "!" and the short caption: the left side of every setting row. */
@Composable
private fun SettingLabel(title: String, help: Help?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) DbColors.Text else DbColors.TextFaint,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (help?.long != null) InfoButton(title, help.long)
        }
        if (help != null && help.short.isNotEmpty()) {
            Text(help.short, style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint)
        }
    }
}

@Composable
fun SettingSwitch(title: String, help: Help? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SettingLabel(title, help, Modifier.weight(1f).padding(end = 12.dp), enabled)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DbColors.Graphite,
                checkedTrackColor = DbColors.Ember,
                checkedBorderColor = DbColors.Ember,
                uncheckedThumbColor = DbColors.TextDim,
                uncheckedTrackColor = DbColors.SlateHighest,
                uncheckedBorderColor = DbColors.Line,
            ),
        )
    }
}

/** Slider that only reports on release (settings are written on each change). */
@Composable
fun SettingSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    help: Help? = null,
    enabled: Boolean = true,
    label: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(value) }
    val shown = if (dragging) local else value
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingLabel(title, help, Modifier.weight(1f), enabled)
            Text(
                label(shown),
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) DbColors.Ember else DbColors.TextFaint,
            )
        }
        Slider(
            value = shown,
            onValueChange = {
                dragging = true
                local = it
            },
            onValueChangeFinished = {
                dragging = false
                onChange(local)
            },
            valueRange = range,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = DbColors.EmberGlow,
                activeTrackColor = DbColors.Ember,
                inactiveTrackColor = DbColors.SlateHighest,
                activeTickColor = DbColors.Graphite.copy(alpha = 0.5f),
                inactiveTickColor = DbColors.TextFaint.copy(alpha = 0.5f),
            ),
        )
    }
}

/** One option of a [ChoiceRow]; [note] is a small second line ("recommended", "experimental"). */
data class Choice<T>(val value: T, val label: String, val note: String? = null, val enabled: Boolean = true)

/**
 * A few options side by side. The chosen one gets a thin ember outline, ember text and a faint tint: easy to
 * see, nothing loud.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceRow(
    title: String?,
    options: List<Choice<T>>,
    selected: T,
    help: Help? = null,
    perRow: Int = if (options.size == 4) 4 else 3,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) SettingLabel(title, help)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = perRow,
        ) {
            options.forEach { o ->
                ChoiceChip(o, o.value == selected, Modifier.weight(1f)) { onSelect(o.value) }
            }
        }
    }
}

@Composable
private fun <T> ChoiceChip(o: Choice<T>, chosen: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (chosen) DbColors.Ember.copy(alpha = 0.08f) else DbColors.SlateHigh)
            .border(1.dp, if (chosen) DbColors.Ember.copy(alpha = 0.85f) else DbColors.Line, shape)
            .selectable(selected = chosen, enabled = o.enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            o.label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !o.enabled -> DbColors.TextFaint
                chosen -> DbColors.EmberGlow
                else -> DbColors.Text
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (o.note != null) {
            Text(
                o.note,
                style = MaterialTheme.typography.labelSmall,
                color = if (chosen) DbColors.Ember.copy(alpha = 0.8f) else DbColors.TextFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Choice in a longer list (menu). */
@Composable
fun <T> SettingDropdown(title: String, options: List<Pair<T, String>>, selected: T, help: Help? = null, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SettingLabel(title, help, Modifier.weight(1f).padding(end = 12.dp))
        Box {
            OutlinedButton(
                onClick = { open = true },
                border = BorderStroke(1.dp, DbColors.Line),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(options.firstOrNull { it.first == selected }?.second ?: "-", maxLines = 1, color = DbColors.Text)
                Icon(Icons.Filled.ArrowDropDown, null, tint = DbColors.TextDim)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = DbColors.SlateHigh) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                label,
                                color = if (value == selected) DbColors.EmberGlow else DbColors.Text,
                                fontWeight = if (value == selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            open = false
                            onSelect(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun Divider() = HorizontalDivider(color = DbColors.LineSoft)

enum class Level { OK, INFO, WARN, ERROR }

fun Level.color(): Color = when (this) {
    Level.OK -> DbColors.Ok
    Level.INFO -> DbColors.Steel
    Level.WARN -> DbColors.Warn
    Level.ERROR -> DbColors.Error
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** A line with a coloured dot (status, warning, error). */
@Composable
fun StatusLine(text: String, level: Level) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .background(level.color(), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = DbColors.TextDim, modifier = Modifier.weight(1f))
    }
}

/** Label / value pair (device info, versions). */
@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = DbColors.TextFaint, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = DbColors.Text, modifier = Modifier.weight(0.58f))
    }
}

/** Fills the text with a brush (logo). */
fun Modifier.gradientText(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush, blendMode = BlendMode.SrcAtop)
    }

/** "DRAGON BLAST" in brushed silver with a warm edge at the bottom. */
@Composable
fun Logo(size: Int = 30, modifier: Modifier = Modifier) {
    Text(
        "DRAGON BLAST",
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = size.sp,
        letterSpacing = (size / 7.5f).sp,
        color = Color.White,
        maxLines = 1,
        modifier = modifier.gradientText(
            Brush.verticalGradient(
                0f to Color(0xFFF4F6F8),
                0.55f to Color(0xFFC3CAD3),
                0.8f to Color(0xFF9AA4B0),
                1f to DbColors.Ember,
            ),
        ),
    )
}

fun formatBytes(b: Long): String = when {
    b >= 1L shl 30 -> String.format(java.util.Locale.ROOT, "%.2f GB", b / (1024.0 * 1024 * 1024))
    b >= 1L shl 20 -> String.format(java.util.Locale.ROOT, "%.1f MB", b / (1024.0 * 1024))
    b >= 1L shl 10 -> String.format(java.util.Locale.ROOT, "%.0f KB", b / 1024.0)
    else -> "$b B"
}

/**
 * A scrolling page of cards. On a narrow screen one column; on a wide one (landscape phones, handhelds, tablets) two
 * columns packed like masonry: each card goes under the shorter column, so cards keep their natural height.
 * [fullWidthFirst]: the first child spans every column (the Home stage).
 */
@Composable
fun CardsPage(fullWidthFirst: Boolean = false, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 720.dp && maxWidth > maxHeight) 2 else 1
        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp),
        ) {
            MasonryLayout(columns, 14.dp, fullWidthFirst, content)
        }
    }
}

@Composable
private fun MasonryLayout(columns: Int, gap: androidx.compose.ui.unit.Dp, fullWidthFirst: Boolean, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, Modifier.fillMaxWidth()) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val colWidth = (width - gapPx * (columns - 1)) / columns
        val heights = IntArray(columns)
        data class Cell(val p: androidx.compose.ui.layout.Placeable, val x: Int, val y: Int)
        val cells = ArrayList<Cell>(measurables.size)
        measurables.forEachIndexed { i, m ->
            if (i == 0 && fullWidthFirst && columns > 1) {
                val p = m.measure(androidx.compose.ui.unit.Constraints(minWidth = width, maxWidth = width))
                cells += Cell(p, 0, 0)
                val top = if (p.height > 0) p.height + gapPx else 0
                for (c in 0 until columns) heights[c] = top
            } else {
                val c = heights.indices.minBy { heights[it] }
                val p = m.measure(androidx.compose.ui.unit.Constraints(minWidth = colWidth, maxWidth = colWidth))
                cells += Cell(p, c * (colWidth + gapPx), heights[c])
                if (p.height > 0) heights[c] += p.height + gapPx
            }
        }
        val total = (heights.maxOrNull() ?: 0) - gapPx
        layout(width, total.coerceAtLeast(0)) { cells.forEach { it.p.placeRelative(it.x, it.y) } }
    }
}
