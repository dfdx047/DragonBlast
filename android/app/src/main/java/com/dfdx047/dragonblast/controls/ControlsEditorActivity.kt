@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.dfdx047.dragonblast.controls

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.ControlScheme
import com.dfdx047.dragonblast.data.Settings
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.game.TouchControlsView
import com.dfdx047.dragonblast.ui.theme.DragonBlastTheme
import kotlin.math.roundToInt

/**
 * Editor do layout dos controles por toque: arrastar botoes, mudar tamanho e
 * opacidade, esconder, adicionar botoes de ajuda, botoes do comando e macros
 * (varios botoes ao mesmo tempo ou uma sequencia).
 */
class ControlsEditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPaths.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val settings = SettingsStore(this).load()
        val kind = if (intent.getBooleanExtra(EXTRA_MENU, false)) LayoutKind.MENU else LayoutKind.GAMEPLAY
        setContent {
            DragonBlastTheme {
                ControlsEditor(settings, initialKind = kind) { finish() }
            }
        }
    }

    companion object {
        /** Open on the menus layout instead of the fight one. */
        const val EXTRA_MENU = "menu"
    }
}

/**
 * Editor do layout. Usado pela ControlsEditorActivity (launcher) e por cima do
 * jogo pausado ([overGame] = fundo transparente, ve-se o jogo atras).
 */
@Composable
fun ControlsEditor(
    settings: Settings,
    overGame: Boolean = false,
    initialKind: LayoutKind = LayoutKind.GAMEPLAY,
    close: () -> Unit,
) {
    var kind by remember { mutableStateOf(initialKind) }
    val scheme: ControlScheme = settings.controlScheme
    var layout by remember { mutableStateOf(ControlLayoutStore.load(initialKind, scheme)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    fun update(id: String, f: (ControlElement) -> ControlElement) {
        layout = layout.copy(elements = layout.elements.map { if (it.id == id) f(it) else it })
        dirty = true
    }

    fun add(e: ControlElement) {
        layout = layout.copy(elements = layout.elements + e)
        selected = e.id
        dirty = true
    }

    Box(Modifier.fillMaxSize().background(if (overGame) Color.Black.copy(alpha = 0.35f) else Color(0xFF0E1116))) {
        Text(
            stringResource(R.string.editor_hint),
            color = Color.White.copy(alpha = 0.35f),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center),
        )
        AndroidView(
            factory = { ctx ->
                TouchControlsView(ctx).apply {
                    applySettings(settings)
                    editMode = true
                    editListener = object : TouchControlsView.EditListener {
                        override fun onSelected(id: String?) {
                            selected = id
                        }

                        override fun onMoved(moved: ControlLayout) {
                            // O View ja tem a posicao nova; guarda-a no estado do editor.
                            layout = moved
                            dirty = true
                        }
                    }
                }
            },
            update = { v ->
                v.layout = layout
                v.selectedId = selected
            },
            modifier = Modifier.fillMaxSize(),
        )
        // Barra de cima
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = close) { Icon(Icons.Filled.Close, stringResource(R.string.cancel), tint = Color.White) }
                Text(stringResource(R.string.editor_title), color = Color.White, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                // Layout da luta ou dos menus (o jogo troca sozinho entre os dois).
                listOf(
                    LayoutKind.GAMEPLAY to stringResource(R.string.editor_layout_fight),
                    LayoutKind.MENU to stringResource(R.string.editor_layout_menu),
                ).forEach { (k, label) ->
                    FilterChip(
                        selected = kind == k,
                        onClick = {
                            if (kind != k) {
                                if (dirty) ControlLayoutStore.save(layout, scheme)
                                kind = k
                                layout = ControlLayoutStore.load(k, scheme)
                                selected = null
                                dirty = false
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box {
                    TextButton(onClick = { addMenu = true }) {
                        Icon(Icons.Filled.Add, null)
                        Text(stringResource(R.string.editor_add))
                    }
                    AddMenu(addMenu, layout, scheme, onDismiss = { addMenu = false }) { e ->
                        addMenu = false
                        val existing = layout.elements.firstOrNull { it.id == e.id }
                        if (existing != null) update(e.id) { it.copy(visible = true) } else add(e)
                    }
                }
                TextButton(onClick = {
                    layout = GameActions.defaultLayout(kind, scheme)
                    selected = null
                    dirty = true
                }) {
                    Icon(Icons.Filled.RestartAlt, null)
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = {
                    ControlLayoutStore.save(layout, scheme)
                    dirty = false
                    close()
                }) {
                    Icon(Icons.Filled.Save, null)
                    Text(stringResource(R.string.save) + if (dirty) " •" else "")
                }
            }
        }

        // Painel do botao selecionado
        val sel = layout.elements.firstOrNull { it.id == selected }
        if (sel != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .widthIn(max = 560.dp)
                    .heightIn(max = 300.dp),
            ) {
                ElementPanel(
                    sel,
                    scheme,
                    onChange = { f -> update(sel.id, f) },
                    onHide = {
                        update(sel.id) { it.copy(visible = false) }
                        selected = null
                    },
                    onDelete = {
                        layout = layout.copy(elements = layout.elements.filter { it.id != sel.id })
                        selected = null
                        dirty = true
                    },
                )
            }
        }
    }
}

/** Display name of a catalog choice (GameActions.choices) or of a one-tap helper, by key. */
private fun choiceName(key: String): Int = when (key) {
    "none" -> R.string.choice_none
    "melee" -> R.string.act_melee
    "ki" -> R.string.act_ki
    "signature" -> R.string.act_signature
    "dash" -> R.string.act_dash
    "guard" -> R.string.act_guard
    "ascend" -> R.string.helper_ascend
    "descend" -> R.string.helper_descend
    "rising", "bt3_rising" -> R.string.helper_rising
    "charge" -> R.string.helper_charge
    "diamond" -> R.string.helper_diamond
    "ultimate", "bt3_ultimate" -> R.string.helper_ultimate
    "super1", "bt3_super1" -> R.string.helper_super1
    "super2", "bt3_super2" -> R.string.helper_super2
    "super3", "bt3_super3" -> R.string.helper_super3
    "super4", "bt3_super4" -> R.string.helper_super4
    "sway", "bt3_sway" -> R.string.helper_sway
    "vanishing", "bt3_vanishing" -> R.string.helper_vanishing
    "counter", "bt3_counter" -> R.string.helper_counter
    "throw" -> R.string.helper_throw
    "bt3_square" -> R.string.choice_bt3_square
    "bt3_triangle" -> R.string.choice_bt3_triangle
    "bt3_circle" -> R.string.choice_bt3_circle
    "bt3_cross" -> R.string.choice_bt3_cross
    "bt3_r1" -> R.string.choice_bt3_r1
    "bt3_r2" -> R.string.choice_bt3_r2
    "bt3_l2" -> R.string.choice_bt3_l2
    "bt3_r3" -> R.string.choice_bt3_r3
    "bt3_blast1" -> R.string.choice_bt3_blast1
    "swipe_supers", "bt3_specials" -> R.string.swipe_supers
    "swipe_transform", "bt3_transform" -> R.string.swipe_transform
    "swipe_guard" -> R.string.swipe_guard
    "swipe_flight", "bt3_flight" -> R.string.swipe_flight
    else -> R.string.editor_macro
}

@Composable
private fun AddMenu(open: Boolean, layout: ControlLayout, scheme: ControlScheme, onDismiss: () -> Unit, onAdd: (ControlElement) -> Unit) {
    val present = layout.elements.filter { it.visible }.map { it.id }.toSet()
    val helpers = GameActions.helpers(scheme)
    val swipes = GameActions.swipeHelpers(scheme)
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        Text(stringResource(R.string.editor_swipes), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
        swipes.filter { it.id !in present }.forEach { e ->
            DropdownMenuItem(
                text = { Text(stringResource(if (e.id == "bt3_circle") R.string.swipe_guard else choiceName(e.id))) },
                onClick = { onAdd(e) },
            )
        }
        HorizontalDivider()
        Text(stringResource(R.string.editor_helpers), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
        helpers.filter { it.first !in present }.forEach { (id, action, icon) ->
            DropdownMenuItem(
                text = { Text(stringResource(choiceName(id))) },
                onClick = { onAdd(ControlElement(id, ElementKind.BUTTON, action, icon, id.uppercase(), 0.5f, 0.5f, 52f)) },
            )
        }
        // Elementos escondidos voltam a aparecer.
        layout.elements.filter { e -> !e.visible && helpers.none { h -> h.first == e.id } && swipes.none { it.id == e.id } }.forEach { e ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.editor_show, e.label.ifBlank { e.id })) },
                onClick = { onAdd(e.copy(visible = true)) },
            )
        }
        HorizontalDivider()
        Text(stringResource(R.string.editor_pad_buttons), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp))
        listOf(PadInput.A, PadInput.B, PadInput.X, PadInput.Y, PadInput.LB, PadInput.RB, PadInput.LT, PadInput.RT,
            PadInput.BACK, PadInput.START, PadInput.L3, PadInput.R3).forEach { input ->
            DropdownMenuItem(
                text = { Text(GameActions.classicLabel(input, scheme == ControlScheme.TENKAICHI)) },
                onClick = {
                    val id = "pad_${input.name.lowercase()}_${System.currentTimeMillis() % 100000}"
                    onAdd(ControlElement(id, ElementKind.BUTTON, ControlAction.Press(setOf(input)), ControlIcon.MACRO,
                        GameActions.classicLabel(input, scheme == ControlScheme.TENKAICHI), 0.5f, 0.5f, 52f))
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.editor_new_macro)) },
            onClick = {
                val id = "macro_${System.currentTimeMillis() % 100000}"
                onAdd(ControlElement(id, ElementKind.BUTTON, ControlAction.Press(setOf(PadInput.X, PadInput.Y)), ControlIcon.MACRO,
                    "M", 0.5f, 0.5f, 52f))
            },
        )
    }
}

@Composable
private fun ElementPanel(
    e: ControlElement,
    scheme: ControlScheme,
    onChange: ((ControlElement) -> ControlElement) -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(e.label.ifBlank { e.id }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onHide) { Icon(Icons.Filled.VisibilityOff, stringResource(R.string.editor_hide)) }
            val custom = e.id.startsWith("macro_") || e.id.startsWith("pad_") ||
                GameActions.helpers(scheme).any { it.first == e.id } || GameActions.swipeHelpers(scheme).any { it.id == e.id }
            if (custom) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
        }
        LabeledSlider(stringResource(R.string.editor_size), e.sizeDp, 28f..220f, "${e.sizeDp.roundToInt()} dp") { v ->
            onChange { it.copy(sizeDp = v) }
        }
        LabeledSlider(stringResource(R.string.editor_opacity), e.opacity, 0.1f..1f, "${(e.opacity * 100).roundToInt()}%") { v ->
            onChange { it.copy(opacity = v) }
        }
        if (e.kind == ElementKind.RADIAL) SwipeEditor(e, scheme, onChange)
        if (e.id.startsWith("macro_")) MacroEditor(e, onChange)
    }
}

/**
 * A swipe button's parts, each picked from the scheme's moves (GameActions.choices): what it holds while touched,
 * what a tap plays, and the move of each direction. The mark drawn on that side follows the choice.
 */
@Composable
private fun SwipeEditor(e: ControlElement, scheme: ControlScheme, onChange: ((ControlElement) -> ControlElement) -> Unit) {
    val choices = GameActions.choices(scheme)
    Text(stringResource(R.string.editor_swipe_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    ChoiceRow(stringResource(R.string.editor_swipe_hold), e.action, choices) { c -> onChange { it.copy(action = c.action) } }
    ChoiceRow(stringResource(R.string.editor_swipe_tap), e.tap, choices) { c ->
        onChange { el ->
            val hints = (el.hints + List(5) { "" }).take(5).toMutableList().also { it[4] = c.hint }
            el.copy(tap = if (GameActions.isNothing(c.action)) null else c.action, hints = hints)
        }
    }
    listOf(R.string.editor_swipe_up, R.string.editor_swipe_right, R.string.editor_swipe_down, R.string.editor_swipe_left)
        .forEachIndexed { dir, title ->
            ChoiceRow(stringResource(title), e.radial.getOrNull(dir), choices) { c ->
                onChange { el ->
                    val radial = (el.radial + List(4) { GameActions.NOTHING }).take(4).toMutableList().also { it[dir] = c.action }
                    val hints = (el.hints + List(5) { "" }).take(5).toMutableList().also { it[dir] = c.hint }
                    el.copy(radial = radial, hints = hints)
                }
            }
        }
}

@Composable
private fun ChoiceRow(title: String, current: ControlAction?, choices: List<GameActions.Choice>, onPick: (GameActions.Choice) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val now = choices.firstOrNull { it.action == current || (GameActions.isNothing(current) && it.key == "none") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (now != null) stringResource(choiceName(now.key)) else stringResource(R.string.choice_custom), maxLines = 1)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                choices.forEach { c ->
                    DropdownMenuItem(
                        text = { Text((if (c.hint.isNotBlank()) c.hint + "  " else "") + stringResource(choiceName(c.key))) },
                        onClick = {
                            open = false
                            onPick(c)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, label: String, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(90.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp).padding(start = 6.dp))
    }
}

private val MACRO_INPUTS = listOf(
    PadInput.X, PadInput.Y, PadInput.A, PadInput.B, PadInput.LB, PadInput.RB, PadInput.LT, PadInput.RT,
    PadInput.UP, PadInput.DOWN, PadInput.FORWARD, PadInput.BACKWARD,
)

@Composable
private fun InputChips(selected: Set<PadInput>, onToggle: (PadInput) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MACRO_INPUTS.forEach { i ->
            FilterChip(
                selected = i in selected,
                onClick = { onToggle(i) },
                label = { Text(inputName(i)) },
            )
        }
    }
}

@Composable
private fun inputName(i: PadInput): String = when (i) {
    PadInput.X -> stringResource(R.string.input_melee)
    PadInput.Y -> stringResource(R.string.input_ki)
    PadInput.A -> stringResource(R.string.input_dash)
    PadInput.B -> stringResource(R.string.input_signature)
    PadInput.RB -> stringResource(R.string.input_guard)
    PadInput.FORWARD -> stringResource(R.string.input_forward)
    PadInput.BACKWARD -> stringResource(R.string.input_back)
    else -> GameActions.classicLabel(i, false)
}

/** Macro: combinacao (tudo ao mesmo tempo) ou sequencia de passos. */
@Composable
private fun MacroEditor(e: ControlElement, onChange: ((ControlElement) -> ControlElement) -> Unit) {
    val action = e.action
    val isSequence = action is ControlAction.Sequence
    var labelText by remember(e.id) { mutableStateOf(e.label) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.editor_label), modifier = Modifier.width(90.dp))
        listOf("M", "M1", "M2", "SP", "EX", "Z").forEach { l ->
            FilterChip(selected = labelText == l, onClick = {
                labelText = l
                onChange { it.copy(label = l) }
            }, label = { Text(l) }, modifier = Modifier.padding(end = 4.dp))
        }
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !isSequence,
            onClick = {
                if (isSequence) {
                    val all = (action as ControlAction.Sequence).steps.flatMap { it.inputs }.toSet()
                    onChange { it.copy(action = ControlAction.Press(all)) }
                }
            },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
        ) { Text(stringResource(R.string.editor_combo)) }
        SegmentedButton(
            selected = isSequence,
            onClick = {
                if (!isSequence) {
                    val inputs = (action as ControlAction.Press).inputs
                    onChange { it.copy(action = ControlAction.Sequence(listOf(MacroStep(inputs, 3)), holdLast = false)) }
                }
            },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
        ) { Text(stringResource(R.string.editor_sequence)) }
    }
    when (action) {
        is ControlAction.Press -> {
            Text(stringResource(R.string.editor_combo_help), style = MaterialTheme.typography.bodySmall)
            InputChips(action.inputs) { i ->
                val next = if (i in action.inputs) action.inputs - i else action.inputs + i
                onChange { it.copy(action = ControlAction.Press(next)) }
            }
        }
        is ControlAction.Sequence -> {
            Text(stringResource(R.string.editor_sequence_help), style = MaterialTheme.typography.bodySmall)
            action.steps.forEachIndexed { index, step ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.editor_step, index + 1), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val steps = action.steps.filterIndexed { j, _ -> j != index }
                                onChange { it.copy(action = action.copy(steps = steps.ifEmpty { listOf(MacroStep(emptySet(), 3)) })) }
                            }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                        }
                        InputChips(step.inputs) { i ->
                            val inputs = if (i in step.inputs) step.inputs - i else step.inputs + i
                            val steps = action.steps.mapIndexed { j, s -> if (j == index) s.copy(inputs = inputs) else s }
                            onChange { it.copy(action = action.copy(steps = steps)) }
                        }
                        LabeledSlider(stringResource(R.string.editor_frames), step.frames.toFloat(), 1f..60f, "${step.frames}f") { v ->
                            val steps = action.steps.mapIndexed { j, s -> if (j == index) s.copy(frames = v.roundToInt()) else s }
                            onChange { it.copy(action = action.copy(steps = steps)) }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    onChange { it.copy(action = action.copy(steps = action.steps + MacroStep(emptySet(), 3))) }
                }, enabled = action.steps.size < 16) {
                    Icon(Icons.Filled.Add, null)
                    Text(stringResource(R.string.editor_add_step))
                }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.editor_hold_last), style = MaterialTheme.typography.bodySmall)
                Switch(checked = action.holdLast, onCheckedChange = { h -> onChange { it.copy(action = action.copy(holdLast = h)) } })
            }
        }
    }
    Text(
        stringResource(R.string.editor_macro_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
