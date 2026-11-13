package com.dfdx047.dragonblast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
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
import com.dfdx047.dragonblast.ui.components.Choice
import com.dfdx047.dragonblast.ui.components.ChoiceRow
import com.dfdx047.dragonblast.ui.components.Divider
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.SettingSlider
import com.dfdx047.dragonblast.ui.components.SettingSwitch
import com.dfdx047.dragonblast.ui.components.TouchPreview
import com.dfdx047.dragonblast.ui.components.help
import com.dfdx047.dragonblast.ui.theme.DbColors

@Composable
fun ControlsScreen(vm: LauncherViewModel) {
    val s = vm.settings
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
            TouchPreview(s.touchPreset, s.buttonStyle, s.leftHanded, s.touchOpacity, s.touchScale)
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
                    Choice(ControlScheme.RAGING_BLAST, "Raging Blast 2", stringResource(R.string.ctl_scheme_original)),
                    Choice(ControlScheme.TENKAICHI, "Tenkaichi 3", stringResource(R.string.experimental)),
                ),
                selected = s.controlScheme,
                onSelect = { c -> vm.update { it.copy(controlScheme = c) } },
            )
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
            SettingSwitch(stringResource(R.string.ctl_nintendo), help(R.string.ctl_nintendo_short, R.string.ctl_nintendo_long), s.nintendoLayout) { v ->
                vm.update { it.copy(nintendoLayout = v) }
            }
        }
    }
}
