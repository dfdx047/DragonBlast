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

/** Quick presets: resolution + upscaler in one tap. */
private enum class GfxPreset(val scale: Int, val upscaler: Upscaler) {
    LIGHT(60, Upscaler.FSR), BALANCED(75, Upscaler.FSR), QUALITY(100, Upscaler.BILINEAR)
}

@Composable
fun GraphicsScreen(vm: LauncherViewModel) {
    val s = vm.settings
    val scales = SettingsStore.RENDER_SCALES
    CardsPage {
        SectionCard(stringResource(R.string.gfx_presets), Icons.Outlined.AutoAwesome, subtitle = stringResource(R.string.gfx_presets_sub)) {
            val current = GfxPreset.entries.firstOrNull { it.scale == s.renderScale && it.upscaler == s.upscaler }
            ChoiceRow(
                title = null,
                options = listOf(
                    Choice(GfxPreset.LIGHT, stringResource(R.string.preset_light), "60% · FSR"),
                    Choice(GfxPreset.BALANCED, stringResource(R.string.preset_balanced), "75% · FSR"),
                    Choice(GfxPreset.QUALITY, stringResource(R.string.preset_quality), "100%"),
                ),
                selected = current,
                onSelect = { p -> vm.update { it.copy(renderScale = p!!.scale, upscaler = p.upscaler) } },
            )
        }

        SectionCard(stringResource(R.string.gfx_render), Icons.Outlined.Wallpaper) {
            ChoiceRow(
                title = stringResource(R.string.gfx_renderer),
                help = help(R.string.gfx_renderer_short, R.string.gfx_renderer_long),
                options = listOf(
                    Choice(Renderer.XENIA, "Xenia", stringResource(R.string.gfx_renderer_xenia_note)),
                    Choice(Renderer.RENUT, "Renut", stringResource(R.string.in_development), enabled = false),
                ),
                selected = s.renderer,
                onSelect = { r -> vm.update { it.copy(renderer = r) } },
            )
            Divider()
            val idx = scales.indexOf(s.renderScale).let { if (it < 0) scales.size - 1 else it }
            SettingSlider(
                title = stringResource(R.string.gfx_resolution),
                help = help(R.string.gfx_resolution_short, R.string.gfx_resolution_long),
                value = idx.toFloat(),
                range = 0f..(scales.size - 1).toFloat(),
                steps = scales.size - 2,
                label = { v -> scales[v.toInt().coerceIn(0, scales.size - 1)].let { "$it% · ${SettingsStore.renderSize(it)}" } },
                onChange = { v -> vm.update { it.copy(renderScale = scales[v.toInt().coerceIn(0, scales.size - 1)]) } },
            )
            Divider()
            ChoiceRow(
                title = stringResource(R.string.gfx_upscaler),
                help = help(R.string.gfx_upscaler_short, R.string.gfx_upscaler_long),
                options = listOf(
                    Choice(Upscaler.BILINEAR, stringResource(R.string.gfx_upscaler_bilinear)),
                    Choice(Upscaler.FSR, "FSR 1", stringResource(R.string.recommended)),
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

        SectionCard(stringResource(R.string.gfx_frames), Icons.Outlined.Speed) {
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
