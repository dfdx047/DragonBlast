package com.dfdx047.dragonblast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.FpsMode
import com.dfdx047.dragonblast.data.FxPreset
import com.dfdx047.dragonblast.data.Renderer
import com.dfdx047.dragonblast.data.AspectMode
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.data.Upscaler
import com.dfdx047.dragonblast.ui.LauncherViewModel
import com.dfdx047.dragonblast.ui.components.CardsPage
import com.dfdx047.dragonblast.ui.components.Choice
import com.dfdx047.dragonblast.ui.components.ChoiceRow
import com.dfdx047.dragonblast.ui.components.Divider
import com.dfdx047.dragonblast.ui.components.SectionCard
import com.dfdx047.dragonblast.ui.components.SettingSlider
import com.dfdx047.dragonblast.ui.components.SettingSwitch
import com.dfdx047.dragonblast.ui.components.help

@Composable
fun GraphicsScreen(vm: LauncherViewModel) {
    val s = vm.settings
    CardsPage {
        SectionCard(stringResource(R.string.gfx_render), Icons.Outlined.Wallpaper) {
            ChoiceRow(
                title = stringResource(R.string.gfx_renderer),
                help = help(R.string.gfx_renderer_short, R.string.gfx_renderer_long),
                options = listOf(
                    Choice(Renderer.RENUT, "Renut", stringResource(R.string.gfx_renderer_renut_note)),
                    Choice(Renderer.XENIA, "Xenia", stringResource(R.string.gfx_renderer_xenia_note)),
                ),
                selected = s.renderer,
                onSelect = { r -> vm.update { it.copy(renderer = r) } },
            )
            Divider()
            // Renut: 360p to 4K; Xenia: up to the game's 720p.
            val heights = SettingsStore.heightsFor(s.renderer)
            val shown = SettingsStore.effectiveHeight(s.renderer, s.renderHeight)
            val idx = heights.indexOf(shown).let { if (it < 0) heights.indexOf(720) else it }
            SettingSlider(
                title = stringResource(R.string.gfx_resolution),
                help = help(R.string.gfx_resolution_short, R.string.gfx_resolution_long),
                value = idx.toFloat(),
                range = 0f..(heights.size - 1).toFloat(),
                steps = heights.size - 2,
                label = { v -> SettingsStore.heightLabel(heights[v.toInt().coerceIn(0, heights.size - 1)]) },
                onChange = { v -> vm.update { it.copy(renderHeight = heights[v.toInt().coerceIn(0, heights.size - 1)]) } },
            )
            Divider()
            ChoiceRow(
                title = stringResource(R.string.gfx_upscaler),
                help = help(R.string.gfx_upscaler_short, R.string.gfx_upscaler_long),
                options = listOf(
                    Choice(Upscaler.BILINEAR, stringResource(R.string.gfx_upscaler_bilinear), stringResource(R.string.recommended)),
                    Choice(Upscaler.FSR, "FSR 1", stringResource(R.string.gfx_upscaler_fsr_note)),
                    Choice(Upscaler.CAS, "AMD CAS", stringResource(R.string.gfx_upscaler_cas_note)),
                ),
                selected = s.upscaler,
                onSelect = { u -> vm.update { it.copy(upscaler = u) } },
            )
            Divider()
            ChoiceRow(
                title = stringResource(R.string.gfx_aspect),
                help = help(R.string.gfx_aspect_short, R.string.gfx_aspect_long),
                options = listOf(
                    Choice(AspectMode.ORIGINAL, stringResource(R.string.aspect_original), stringResource(R.string.aspect_original_note)),
                    Choice(AspectMode.STRETCH, stringResource(R.string.aspect_stretch), stringResource(R.string.aspect_stretch_note)),
                ),
                selected = s.aspect,
                onSelect = { a -> vm.update { it.copy(aspect = a) } },
            )
        }

        if (s.renderer == Renderer.RENUT) {
            SectionCard(stringResource(R.string.gfx_effects), Icons.Outlined.AutoAwesome, subtitle = stringResource(R.string.gfx_effects_sub)) {
                ChoiceRow(
                    title = null,
                    options = listOf(
                        Choice(FxPreset.LIGHT, stringResource(R.string.preset_light)),
                        Choice(FxPreset.BALANCED, stringResource(R.string.preset_balanced)),
                        Choice(FxPreset.QUALITY, stringResource(R.string.preset_quality)),
                    ),
                    selected = FxPreset.of(s),
                    onSelect = { p -> vm.update { it.copy(fxBloom = p!!.bloom, fxShadows = p.shadows) } },
                )
                Divider()
                SettingSwitch(
                    stringResource(R.string.fx_bloom), help(R.string.fx_bloom_short, R.string.fx_bloom_long), s.fxBloom,
                ) { v -> vm.update { it.copy(fxBloom = v) } }
                Divider()
                SettingSwitch(
                    stringResource(R.string.fx_shadows), help(R.string.fx_shadows_short, R.string.fx_shadows_long), s.fxShadows,
                ) { v -> vm.update { it.copy(fxShadows = v) } }
            }
        }

        SectionCard(stringResource(R.string.gfx_frames), Icons.Outlined.Speed) {
            // 120 FPS (frame generation) only on displays of 100 Hz or more (a 60 Hz phone like the Mi A3 has nothing
            // to show the generated frames on).
            val context = androidx.compose.ui.platform.LocalContext.current
            val high = androidx.compose.runtime.remember { org.rb2recomp.android.LaunchConfig.highRefreshDisplay(context) }
            if (high || s.fpsMode == FpsMode.SIXTY_FG) {
                ChoiceRow(
                    title = stringResource(R.string.gfx_fps),
                    help = help(R.string.gfx_fps_short, R.string.gfx_fps_long),
                    options = listOf(
                        Choice(FpsMode.SIXTY, "60 FPS", stringResource(R.string.recommended)),
                        Choice(FpsMode.SIXTY_FG, "120 FPS", stringResource(R.string.gfx_fps_fg_note)),
                    ),
                    selected = s.fpsMode,
                    onSelect = { m -> vm.update { it.copy(fpsMode = m) } },
                )
                Divider()
            }
            SettingSwitch(
                stringResource(R.string.gfx_show_fps), help(R.string.gfx_show_fps_short, R.string.gfx_show_fps_long), s.showFps,
            ) { v -> vm.update { it.copy(showFps = v) } }
        }

        SectionCard(stringResource(R.string.gfx_system), Icons.Outlined.Tune) {
            SettingSwitch(
                stringResource(R.string.gfx_pause_bg), help(R.string.gfx_pause_bg_short, R.string.gfx_pause_bg_long), s.pauseInBackground,
            ) { v -> vm.update { it.copy(pauseInBackground = v) } }
            Divider()
            SettingSwitch(
                stringResource(R.string.gfx_stereo), help(R.string.gfx_stereo_short, R.string.gfx_stereo_long), s.stereoAudio,
            ) { v -> vm.update { it.copy(stereoAudio = v) } }
        }
    }
}
