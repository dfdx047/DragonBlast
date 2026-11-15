package com.dfdx047.dragonblast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dfdx047.dragonblast.controls.ControlLayoutStore
import com.dfdx047.dragonblast.controls.ControlsEditorActivity
import com.dfdx047.dragonblast.controls.ControlsGuideActivity
import com.dfdx047.dragonblast.controls.PadRemap
import com.dfdx047.dragonblast.controls.PadRemapActivity
import com.dfdx047.dragonblast.controls.LayoutKind
import com.dfdx047.dragonblast.data.Settings
import com.dfdx047.dragonblast.game.TouchControlsView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.ButtonStyle
import com.dfdx047.dragonblast.data.ControlScheme
import com.dfdx047.dragonblast.data.Haptics
import com.dfdx047.dragonblast.data.StickMode
import com.dfdx047.dragonblast.data.TouchMode
import com.dfdx047.dragonblast.data.TouchPreset
import com.dfdx047.dragonblast.data.tuning
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.components.CardsPage
import com.dfdx047.dragonblast.ui.components.Choice
import com.dfdx047.dragonblast.ui.components.ChoiceRow
import com.dfdx047.dragonblast.ui.components.Divider
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.SettingSlider
import com.dfdx047.dragonblast.ui.components.SettingSwitch
import com.dfdx047.dragonblast.ui.components.help
import com.dfdx047.dragonblast.ui.theme.DbColors

@Composable
fun ControlsScreen(vm: LauncherViewModel) {
    val s = vm.settings
    CardsPage {
        SectionCard(stringResource(R.string.ctl_touch), Icons.Outlined.TouchApp) {
            ChoiceRow(
                title = stringResource(R.string.ctl_touch_mode),
                help = help(R.string.ctl_touch_mode_short, R.string.ctl_touch_mode_long),
                options = listOf(
                    Choice(TouchMode.OFF, stringResource(R.string.off)),
                    Choice(TouchMode.AUTO, stringResource(R.string.ctl_touch_auto)),
                    Choice(TouchMode.ALWAYS, stringResource(R.string.ctl_touch_always)),
                ),
                selected = s.touchMode,
                onSelect = { m -> vm.update { it.copy(touchMode = m) } },
            )
            ControlsPreview(s)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val context = LocalContext.current
                OutlinedButton(onClick = {
                    context.startActivity(Intent(context, ControlsEditorActivity::class.java))
                }) {
                    Icon(Icons.Outlined.Edit, null, tint = DbColors.Ember)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ctl_edit), color = DbColors.Text)
                }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(context, ControlsEditorActivity::class.java).putExtra(ControlsEditorActivity.EXTRA_MENU, true))
                }) {
                    Text(stringResource(R.string.ctl_edit_menu), color = DbColors.TextDim)
                }
            }
            ChoiceRow(
                title = stringResource(R.string.ctl_preset),
                help = help(R.string.ctl_preset_short, R.string.ctl_preset_long),
                options = listOf(
                    Choice(TouchPreset.PRO, "Pro", stringResource(R.string.ctl_preset_pro_note)),
                    Choice(TouchPreset.NORMAL, "Normal", stringResource(R.string.recommended)),
                    Choice(TouchPreset.CASUAL, "Casual", stringResource(R.string.ctl_preset_casual_note)),
                ),
                selected = s.touchPreset,
                onSelect = { p ->
                    val t = p.tuning()
                    vm.update { it.copy(touchPreset = p, stickMode = t.stickMode, swipeButtons = t.swipe) }
                },
            )
            Text(
                stringResource(
                    when (s.touchPreset) {
                        TouchPreset.PRO -> R.string.ctl_preset_pro_desc
                        TouchPreset.NORMAL -> R.string.ctl_preset_normal_desc
                        TouchPreset.CASUAL -> R.string.ctl_preset_casual_desc
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = DbColors.TextDim,
            )
            Divider()
            ChoiceRow(
                title = stringResource(R.string.ctl_style),
                help = help(R.string.ctl_style_short, R.string.ctl_style_long),
                options = listOf(
                    Choice(ButtonStyle.DRAGON, "Dragon"),
                    Choice(ButtonStyle.XBOX, "Xbox"),
                    Choice(ButtonStyle.PLAYSTATION, "PlayStation"),
                    Choice(ButtonStyle.NINTENDO, "Nintendo"),
                ),
                selected = s.buttonStyle,
                perRow = 2,
                onSelect = { b -> vm.update { it.copy(buttonStyle = b) } },
            )
        }

        SectionCard(stringResource(R.string.ctl_scheme_title), Icons.Outlined.SportsMartialArts, subtitle = stringResource(R.string.ctl_scheme_sub)) {
            ChoiceRow(
                title = stringResource(R.string.ctl_scheme),
                help = help(R.string.ctl_scheme_short, R.string.ctl_scheme_long),
                options = listOf(
                    Choice(ControlScheme.RAGING_BLAST, "Raging Blast", stringResource(R.string.ctl_scheme_original)),
                    Choice(ControlScheme.MODERN, stringResource(R.string.ctl_scheme_modern), stringResource(R.string.ctl_scheme_modern_note)),
                    Choice(ControlScheme.TENKAICHI, "Tenkaichi 3", stringResource(R.string.ctl_scheme_bt3_note)),
                ),
                selected = s.controlScheme,
                onSelect = { c -> vm.update { it.copy(controlScheme = c) } },
            )
            val context = LocalContext.current
            OutlinedButton(onClick = { context.startActivity(Intent(context, ControlsGuideActivity::class.java)) }) {
                Icon(Icons.Outlined.MenuBook, null, tint = DbColors.Ember)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ctl_guide), color = DbColors.Text)
            }
        }

        SectionCard(stringResource(R.string.ctl_fine), Icons.Outlined.Tune) {
            SettingSlider(
                stringResource(R.string.ctl_opacity), s.touchOpacity.toFloat(), 25f..100f, steps = 14,
                help = help(R.string.ctl_opacity_short),
                label = { "${it.toInt()}%" },
            ) { v -> vm.update { it.copy(touchOpacity = v.toInt()) } }
            SettingSlider(
                stringResource(R.string.ctl_size), s.touchScale.toFloat(), 70f..140f, steps = 13,
                help = help(R.string.ctl_size_short),
                label = { "${it.toInt()}%" },
            ) { v -> vm.update { it.copy(touchScale = v.toInt()) } }
            Divider()
            ChoiceRow(
                title = stringResource(R.string.ctl_stick),
                help = help(R.string.ctl_stick_short, R.string.ctl_stick_long),
                options = listOf(
                    Choice(StickMode.FIXED, stringResource(R.string.ctl_stick_fixed)),
                    Choice(StickMode.DYNAMIC, stringResource(R.string.ctl_stick_dynamic)),
                ),
                selected = s.stickMode,
                onSelect = { m -> vm.update { it.copy(stickMode = m) } },
            )
            SettingSlider(
                stringResource(R.string.ctl_deadzone), s.stickDeadZone.toFloat(), 0f..30f, steps = 5,
                help = help(R.string.ctl_deadzone_short, R.string.ctl_deadzone_long),
                label = { "${it.toInt()}%" },
            ) { v -> vm.update { it.copy(stickDeadZone = v.toInt()) } }
            Divider()
            SettingSwitch(stringResource(R.string.ctl_swipe), help(R.string.ctl_swipe_short, R.string.ctl_swipe_long), s.swipeButtons) { v ->
                vm.update { it.copy(swipeButtons = v) }
            }
            SettingSwitch(stringResource(R.string.ctl_left), help(R.string.ctl_left_short), s.leftHanded) { v ->
                vm.update { it.copy(leftHanded = v) }
            }
            Divider()
            ChoiceRow(
                title = stringResource(R.string.ctl_haptics),
                help = help(R.string.ctl_haptics_short),
                options = listOf(
                    Choice(Haptics.OFF, stringResource(R.string.off)),
                    Choice(Haptics.LIGHT, stringResource(R.string.ctl_haptics_light)),
                    Choice(Haptics.STRONG, stringResource(R.string.ctl_haptics_strong)),
                ),
                selected = s.haptics,
                onSelect = { hp -> vm.update { it.copy(haptics = hp) } },
            )
        }

        SectionCard(stringResource(R.string.ctl_pad), Icons.Outlined.Gamepad, subtitle = stringResource(R.string.ctl_pad_sub)) {
            val context = LocalContext.current
            // Re-read the profiles when the screen comes back from the remap screen.
            var resumes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(owner) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                    if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumes++
                }
                owner.lifecycle.addObserver(obs)
                onDispose { owner.lifecycle.removeObserver(obs) }
            }
            resumes.let { }
            val profile = PadRemap.profileOf(s.controlScheme)
            val changes = PadRemap.changes(PadRemap.load(context, profile))
            Text(
                stringResource(R.string.ctl_pad_text, if (profile == "bt3") "Tenkaichi 3" else "Raging Blast"),
                style = MaterialTheme.typography.bodySmall,
                color = DbColors.TextDim,
            )
            Text(
                if (changes == 0) stringResource(R.string.ctl_pad_default) else stringResource(R.string.ctl_pad_changed, changes),
                style = MaterialTheme.typography.labelMedium,
                color = if (changes == 0) DbColors.TextFaint else DbColors.Ember,
            )
            OutlinedButton(onClick = { context.startActivity(Intent(context, PadRemapActivity::class.java)) }) {
                Icon(Icons.Outlined.Gamepad, null, tint = DbColors.Ember)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ctl_pad_remap), color = DbColors.Text)
            }
        }
    }
}

/**
 * The fight layout of the chosen scheme, drawn by the same view the game uses, on a dark stand-in for the game
 * picture with the phone's landscape proportions. Not interactive.
 */
@Composable
private fun ControlsPreview(s: Settings) {
    val context = LocalContext.current
    val dm = context.resources.displayMetrics
    val longSide = maxOf(dm.widthPixels, dm.heightPixels)
    val shortSide = minOf(dm.widthPixels, dm.heightPixels).coerceAtLeast(1)
    val shape = RoundedCornerShape(14.dp)
    // Re-read the saved layout whenever the screen comes back (e.g. from the editor).
    var resumes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumes++
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(longSide.toFloat() / shortSide)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3442), Color(0xFF1B2028), Color(0xFF15130F))))
            .border(1.dp, DbColors.Line, shape),
    ) {
        AndroidView(
            factory = { ctx ->
                TouchControlsView(ctx).apply {
                    referenceWidthPx = longSide
                    setOnTouchListener { _, _ -> true }
                }
            },
            update = { v ->
                resumes.let { } // read so a resume refreshes the preview
                v.applySettings(s)
                v.layout = ControlLayoutStore.load(LayoutKind.GAMEPLAY, s.controlScheme)
            },
            modifier = Modifier.matchParentSize(),
        )
    }
}
