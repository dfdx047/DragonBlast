package com.dfdx047.dragonblast.game

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.dfdx047.dragonblast.R
import com.dfdx047.dragonblast.data.AspectMode
import com.dfdx047.dragonblast.data.ButtonStyle
import com.dfdx047.dragonblast.data.ControlScheme
import com.dfdx047.dragonblast.data.FpsMode
import com.dfdx047.dragonblast.data.Haptics
import com.dfdx047.dragonblast.data.Orientation
import com.dfdx047.dragonblast.data.Settings
import com.dfdx047.dragonblast.data.Renderer
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.data.TouchMode
import com.dfdx047.dragonblast.data.TouchPreset
import com.dfdx047.dragonblast.data.Upscaler
import com.dfdx047.dragonblast.ui.theme.DbColors
import com.dfdx047.dragonblast.ui.theme.DragonBlastTheme
import com.dfdx047.dragonblast.ui.theme.LogoStyle
import kotlin.math.abs

/** What the in-game menu asks of the game window (org.rb2recomp.android.GameActivity). */
interface GameMenuHost {
    /** Engine setting by name, live (only settings the engine reads every frame change at once). */
    fun setCvar(name: String, value: String): Boolean
    /** The menu opened / closed: pause the game (Xbox Guide), hide the touch controls. */
    fun menuShown(open: Boolean)
    fun fpsModeChanged(mode: Int)
    fun orientationChanged(key: String)
    fun restartGame()
    fun exitToLauncher()
    fun openControlsEditor(menuLayout: Boolean)
    fun exportLogs()
    /** Lines about the running session (renderer, GPU, driver, sizes). */
    fun sessionInfo(): List<String>
}

/** The SDL window is a plain Activity: a ComposeView needs an always-resumed lifecycle and saved-state owner. */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    init {
        savedState.performAttach()
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

// ------------------------------------------------------------------------------------------------ rows

sealed class MenuItem {
    abstract val label: String
    open val help: String? = null
    open val reopen: Boolean = false
    val selectable: Boolean get() = this !is MHeader && this !is MInfo
}

class MHeader(override val label: String) : MenuItem()
class MInfo(override val label: String) : MenuItem()

class MToggle(
    override val label: String,
    override val help: String?,
    val value: Boolean,
    override val reopen: Boolean = false,
    val onSet: (Boolean) -> Unit,
) : MenuItem()

class MChoice(
    override val label: String,
    override val help: String?,
    val options: List<String>,
    val index: Int,
    override val reopen: Boolean = false,
    val onSet: (Int) -> Unit,
) : MenuItem()

class MStep(
    override val label: String,
    override val help: String?,
    val text: String,
    override val reopen: Boolean = false,
    val onStep: (Int) -> Unit,
) : MenuItem()

class MAction(
    override val label: String,
    override val help: String?,
    val actionText: String,
    val danger: Boolean = false,
    val run: () -> Unit,
) : MenuItem()

// ------------------------------------------------------------------------------------------ navigation

/** Tab and row, and what the controller does (d-pad / left stick with repeat, A, B, LB / RB). */
class MenuController {
    var tab by mutableIntStateOf(0)
    var row by mutableIntStateOf(-1)
    var tabCount = 1
    var items: List<MenuItem> = emptyList()
    var onClose: () -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private var heldDir = 0
    private val repeat = object : Runnable {
        override fun run() {
            if (heldDir == 0) return
            direction(heldDir)
            main.postDelayed(this, REPEAT_MS)
        }
    }

    fun ensureRow() {
        if (items.getOrNull(row)?.selectable != true) row = items.indexOfFirst { it.selectable }
    }

    fun selectTab(i: Int) {
        if (i == tab) return
        tab = i
        row = -1
    }

    private fun changeTab(d: Int) = selectTab((tab + d + tabCount) % tabCount)

    private fun move(d: Int) {
        var i = row
        while (true) {
            i += d
            if (i < 0 || i >= items.size) return
            if (items[i].selectable) {
                row = i
                return
            }
        }
    }

    fun adjust(d: Int) {
        when (val it = items.getOrNull(row)) {
            is MToggle -> it.onSet(!it.value)
            is MChoice -> if (it.options.isNotEmpty()) it.onSet((it.index + d + it.options.size) % it.options.size)
            is MStep -> it.onStep(d)
            else -> Unit
        }
    }

    fun activate() {
        when (val it = items.getOrNull(row)) {
            is MAction -> it.run()
            is MToggle, is MChoice -> adjust(1)
            else -> Unit
        }
    }

    private fun direction(d: Int) = when (d) {
        UP -> move(-1)
        DOWN -> move(1)
        LEFT -> adjust(-1)
        else -> adjust(1)
    }

    fun onKeyDown(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> direction(UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> direction(DOWN)
            KeyEvent.KEYCODE_DPAD_LEFT -> direction(LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> direction(RIGHT)
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> changeTab(-1)
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> changeTab(1)
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> activate()
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> close()
            else -> return KeyEvent.isGamepadButton(code)
        }
        return true
    }

    fun close() {
        stopRepeat()
        onClose()
    }

    /** Left stick or d-pad reported as hat axes, repeating while held. */
    fun onMotion(ev: MotionEvent) {
        val hx = ev.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = ev.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val x = if (abs(hx) > 0.5f) hx else ev.getAxisValue(MotionEvent.AXIS_X)
        val y = if (abs(hy) > 0.5f) hy else ev.getAxisValue(MotionEvent.AXIS_Y)
        val d = when {
            abs(y) >= 0.6f && abs(y) >= abs(x) -> if (y < 0) UP else DOWN
            abs(x) >= 0.6f -> if (x < 0) LEFT else RIGHT
            else -> 0
        }
        if (d == heldDir) return
        stopRepeat()
        heldDir = d
        if (d != 0) {
            direction(d)
            main.postDelayed(repeat, FIRST_REPEAT_MS)
        }
    }

    fun stopRepeat() {
        heldDir = 0
        main.removeCallbacks(repeat)
    }

    private companion object {
        const val UP = 1
        const val DOWN = 2
        const val LEFT = 3
        const val RIGHT = 4
        const val FIRST_REPEAT_MS = 380L
        const val REPEAT_MS = 120L
    }
}

// --------------------------------------------------------------------------------------------- options

/** The options the menu shows and changes: the launcher's settings, live where the engine allows it. */
class MenuModel(
    private val activity: Activity,
    private val host: GameMenuHost,
    private val touch: TouchOverlay?,
    private val closeMenu: () -> Unit,
) {
    private val store = SettingsStore(activity)
    var settings by mutableStateOf(store.load())
        private set

    /** The values the running game started with: an option marked "reopen" shows its pending state. */
    private val started = settings
    private var revision by mutableIntStateOf(0)

    val tabs = listOf(
        R.string.menu_tab_game, R.string.menu_tab_picture, R.string.menu_tab_controls,
        R.string.menu_tab_system, R.string.menu_tab_debug,
    )

    private fun s(id: Int) = activity.getString(id)
    private fun s(id: Int, vararg a: Any) = activity.getString(id, *a)

    fun refresh() {
        settings = store.load()
        revision++
    }

    private fun update(change: (Settings) -> Settings) {
        val next = change(settings)
        store.save(next)
        settings = next
    }

    private fun touchChanged(change: (Settings) -> Settings) {
        update(change)
        touch?.reload()
    }

    fun items(tab: Int): List<MenuItem> {
        revision // read: actions that change state outside [settings] bump it
        return when (tab) {
            0 -> gameItems()
            1 -> pictureItems()
            2 -> controlItems()
            3 -> systemItems()
            else -> debugItems()
        }
    }

    private fun gameItems() = listOf(
        MAction(s(R.string.menu_resume), s(R.string.menu_resume_help), "▶") { closeMenu() },
        MAction(s(R.string.menu_restart), s(R.string.menu_restart_help), s(R.string.menu_go)) { host.restartGame() },
        MAction(s(R.string.menu_exit), s(R.string.menu_exit_help), s(R.string.menu_go), danger = true) {
            host.exitToLauncher()
        },
    )

    private fun pictureItems(): List<MenuItem> {
        val st = settings
        // The renderer the game is running with: the Renut changes resolution live, Xenia on reopening (<= 720p).
        val renut = started.renderer == Renderer.RENUT
        val heights = SettingsStore.heightsFor(started.renderer)
        val heightIndex = heights.indexOf(SettingsStore.effectiveHeight(started.renderer, st.renderHeight))
            .let { if (it < 0) heights.indexOf(720) else it }
        return listOf(
            MChoice(
                s(R.string.gfx_aspect), s(R.string.gfx_aspect_short),
                listOf(s(R.string.aspect_original), s(R.string.aspect_stretch)), st.aspect.ordinal,
            ) { i ->
                val a = AspectMode.entries[i]
                update { it.copy(aspect = a) }
                host.setCvar("present_letterbox", if (a == AspectMode.STRETCH) "false" else "true")
                host.setCvar("swordman_aspect", if (a == AspectMode.STRETCH) "stretch" else "original")  // Renut
            },
            MChoice(
                s(R.string.gfx_upscaler), s(R.string.gfx_upscaler_short),
                listOf(s(R.string.gfx_upscaler_bilinear), "FSR 1", "AMD CAS"), st.upscaler.ordinal,
            ) { i ->
                val u = Upscaler.entries[i]
                update { it.copy(upscaler = u) }
                host.setCvar("present_effect_live", u.code.toString())
            },
            MChoice(
                s(R.string.gfx_fps), s(R.string.gfx_fps_short),
                listOf("60 FPS", "120 FPS (" + s(R.string.gfx_fps_fg_note) + ")"), st.fpsMode.ordinal,
            ) { i ->
                val m = FpsMode.entries[i]
                update { it.copy(fpsMode = m) }
                applyFps(m)
            },
            MChoice(
                s(R.string.gfx_resolution), s(R.string.gfx_resolution_short),
                heights.map { SettingsStore.heightLabel(it) }, heightIndex, reopen = !renut,
            ) { i ->
                update { it.copy(renderHeight = heights[i]) }
                if (renut) host.setCvar("sa2_render_height_handheld", heights[i].toString())
            },
        ).let { items ->
            if (!renut) items else items + listOf(
                MToggle(s(R.string.fx_bloom), s(R.string.fx_bloom_short), st.fxBloom) { v ->
                    update { it.copy(fxBloom = v) }
                    host.setCvar("swordman_renut_skip_ps", org.rb2recomp.android.LaunchConfig.renutSkipList(v, settings.fxShadows))
                },
                MToggle(s(R.string.fx_shadows), s(R.string.fx_shadows_short), st.fxShadows) { v ->
                    update { it.copy(fxShadows = v) }
                    host.setCvar("swordman_renut_skip_ps", org.rb2recomp.android.LaunchConfig.renutSkipList(settings.fxBloom, v))
                },
            )
        } + listOf(
            MToggle(s(R.string.gfx_show_fps), s(R.string.gfx_show_fps_short), st.showFps, reopen = true) { v ->
                update { it.copy(showFps = v) }
            },
            MChoice(
                s(R.string.orientation), s(R.string.orientation_short),
                listOf(s(R.string.orientation_landscape), s(R.string.orientation_portrait), s(R.string.orientation_sensor)),
                st.orientation.ordinal,
            ) { i ->
                val o = Orientation.entries[i]
                update { it.copy(orientation = o) }
                host.orientationChanged(o.key)
            },
        )
    }

    /** Same cvars as LaunchConfig.buildArguments for the two frame-rate modes. */
    private fun applyFps(m: FpsMode) {
        val fg = m == FpsMode.SIXTY_FG
        host.setCvar("gpu_swap_fps_cap", "60")
        host.setCvar("android_affinity_mode", org.rb2recomp.android.LaunchConfig.affinityMode(activity).toString())
        host.setCvar("rb2_vsync_hz", "60")
        host.setCvar("gpu_frame_generation", fg.toString())
        host.setCvar("dbz3_frame_interpolation", fg.toString())  // Renut (its own frame generation)
        host.setCvar("gpu_vblank_hold_us", if (fg) "0" else "15000")
        host.fpsModeChanged(m.code)
    }

    private fun controlItems(): List<MenuItem> {
        val st = settings
        val list = mutableListOf<MenuItem>(
            MChoice(
                s(R.string.ctl_touch_mode), s(R.string.ctl_touch_mode_short),
                listOf(s(R.string.off), s(R.string.ctl_touch_auto), s(R.string.ctl_touch_always)), st.touchMode.ordinal,
            ) { i -> touchChanged { it.copy(touchMode = TouchMode.entries[i]) } },
        )
        if (touch != null) {
            list += MChoice(
                s(R.string.menu_layout_now), s(R.string.menu_layout_now_help),
                listOf(s(R.string.menu_layout_fight), s(R.string.menu_layout_menus)), if (touch.isFightLayout) 0 else 1,
            ) { i ->
                touch.showFightLayout(i == 0)
                revision++
            }
        }
        list += listOf(
            MChoice(
                s(R.string.ctl_scheme), s(R.string.ctl_scheme_short),
                listOf("Raging Blast 2", s(R.string.ctl_scheme_modern), "Tenkaichi 3"), st.controlScheme.ordinal,
            ) { i ->
                val scheme = ControlScheme.entries[i]
                touchChanged { it.copy(controlScheme = scheme) }
                host.setCvar("rb2_bt3_controls", (scheme == ControlScheme.TENKAICHI).toString())
            },
            MChoice(
                s(R.string.ctl_preset), s(R.string.ctl_preset_short),
                listOf("Pro", "Normal", "Casual"), st.touchPreset.ordinal,
            ) { i -> touchChanged { it.copy(touchPreset = TouchPreset.entries[i]) } },
            MChoice(
                s(R.string.ctl_style), s(R.string.ctl_style_short),
                listOf("Dragon", "Xbox", "PlayStation", "Nintendo"), st.buttonStyle.ordinal,
            ) { i -> touchChanged { it.copy(buttonStyle = ButtonStyle.entries[i]) } },
            MStep(s(R.string.ctl_opacity), s(R.string.ctl_opacity_short), "${st.touchOpacity}%") { d ->
                touchChanged { it.copy(touchOpacity = (it.touchOpacity + d * 10).coerceIn(20, 100)) }
            },
            MStep(s(R.string.ctl_size), s(R.string.ctl_size_short), "${st.touchScale}%") { d ->
                touchChanged { it.copy(touchScale = (it.touchScale + d * 10).coerceIn(60, 160)) }
            },
            MToggle(s(R.string.ctl_left), s(R.string.ctl_left_short), st.leftHanded) { v ->
                touchChanged { it.copy(leftHanded = v) }
            },
            MChoice(
                s(R.string.ctl_haptics), s(R.string.ctl_haptics_short),
                listOf(s(R.string.off), s(R.string.ctl_haptics_light), s(R.string.ctl_haptics_strong)), st.haptics.ordinal,
            ) { i -> touchChanged { it.copy(haptics = Haptics.entries[i]) } },
            MAction(s(R.string.ctl_edit), s(R.string.menu_edit_help), s(R.string.menu_open)) {
                host.openControlsEditor(touch?.isFightLayout == false)
            },
            MToggle(s(R.string.ctl_nintendo), s(R.string.ctl_nintendo_short), st.nintendoLayout, reopen = true) { v ->
                update { it.copy(nintendoLayout = v) }
            },
        )
        return list
    }

    private fun systemItems(): List<MenuItem> {
        val st = settings
        return listOf(
            MToggle(s(R.string.gfx_stereo), s(R.string.gfx_stereo_short), st.stereoAudio, reopen = true) { v ->
                update { it.copy(stereoAudio = v) }
            },
            MToggle(s(R.string.gfx_pause_bg), s(R.string.gfx_pause_bg_short), st.pauseInBackground, reopen = true) { v ->
                update { it.copy(pauseInBackground = v) }
            },
        )
    }

    private fun debugItems(): List<MenuItem> {
        val st = settings
        return buildList {
            add(MAction(s(R.string.dbg_export), s(R.string.menu_export_help), s(R.string.menu_go)) { host.exportLogs() })
            add(MToggle(s(R.string.dbg_verbose), s(R.string.dbg_verbose_short), st.verboseLog, reopen = true) { v ->
                update { it.copy(verboseLog = v) }
            })
            add(MToggle(s(R.string.dbg_gpu_stats), s(R.string.dbg_gpu_stats_short), st.gpuStats, reopen = true) { v ->
                update { it.copy(gpuStats = v) }
            })
            add(MHeader(s(R.string.menu_device_info)))
            host.sessionInfo().forEach { add(MInfo(it)) }
        }
    }

    /** True when an option that only applies on reopening differs from what the game started with. */
    fun pendingReopen(): Boolean {
        val a = started
        val b = settings
        val heightNeedsReopen = a.renderer != Renderer.RENUT &&
            SettingsStore.effectiveHeight(a.renderer, a.renderHeight) != SettingsStore.effectiveHeight(a.renderer, b.renderHeight)
        return heightNeedsReopen || a.showFps != b.showFps ||
            a.stereoAudio != b.stereoAudio || a.pauseInBackground != b.pauseInBackground ||
            a.verboseLog != b.verboseLog || a.gpuStats != b.gpuStats || a.nintendoLayout != b.nintendoLayout
    }
}

// ----------------------------------------------------------------------------------------------- view

/**
 * The in-game menu: a panel that slides in from the left over the paused game, in the launcher's look, with
 * tabs. Touch (tap rows, arrows, tabs; tap outside to close) and controller (see [MenuController]).
 */
class GameMenu(private val activity: Activity, parent: ViewGroup, host: GameMenuHost, touch: TouchOverlay?) {
    private val owner = OverlayOwner()
    private val host = host
    private val model = MenuModel(activity, host, touch) { close() }
    private val controller = MenuController()
    private val visible = MutableTransitionState(false)
    private val view = ComposeView(activity)

    var isOpen = false
        private set

    init {
        controller.onClose = { close() }
        // Compose looks the owners up from the window's root: set them on the SDL layout and the decor view too.
        for (v in listOf(view, parent, activity.window.decorView)) {
            v.setViewTreeLifecycleOwner(owner)
            v.setViewTreeSavedStateRegistryOwner(owner)
        }
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        view.setContent {
            DragonBlastTheme {
                MenuOverlay(visible, model, controller)
            }
        }
        parent.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    fun open() {
        if (isOpen) return
        isOpen = true
        model.refresh()
        controller.tab = 0
        controller.row = -1
        visible.targetState = true
        view.isFocusable = true
        host.menuShown(true)
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        controller.stopRepeat()
        visible.targetState = false
        host.menuShown(false)
    }

    /** Settings changed outside the menu (controls editor): show them. */
    fun refresh() = model.refresh()

    fun onKey(event: KeyEvent): Boolean {
        if (!isOpen) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        if (event.repeatCount > 0 && event.keyCode !in REPEATABLE) return true
        controller.onKeyDown(event.keyCode)
        return true
    }

    fun onMotion(ev: MotionEvent): Boolean {
        if (!isOpen) return false
        if (ev.actionMasked == MotionEvent.ACTION_MOVE) controller.onMotion(ev)
        return true
    }

    fun destroy() {
        controller.stopRepeat()
        owner.destroy()
    }

    private companion object {
        val REPEATABLE = setOf(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        )
    }
}

@Composable
private fun MenuOverlay(visible: MutableTransitionState<Boolean>, model: MenuModel, controller: MenuController) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.35f))))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        controller.close()
                    },
            )
        }
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
        ) {
            BoxWithConstraints(Modifier.fillMaxHeight()) {
                val landscape = maxWidth > maxHeight
                val w = if (landscape) (maxWidth * 0.46f).coerceIn(340.dp, 520.dp) else maxWidth * 0.92f
                MenuPanel(model, controller, Modifier.width(w).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun MenuPanel(model: MenuModel, controller: MenuController, modifier: Modifier) {
    controller.tabCount = model.tabs.size
    val items = model.items(controller.tab)
    SideEffect {
        controller.items = items
        controller.ensureRow()
    }
    val listState = rememberLazyListState()
    LaunchedEffect(controller.tab, controller.row) {
        if (controller.row >= 0) listState.animateScrollToItem((controller.row - 2).coerceAtLeast(0))
    }
    val shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(DbColors.SlateHigh, DbColors.Slate, DbColors.Graphite)))
            .border(1.dp, DbColors.Line, shape)
            // Swallow taps on empty panel space (the scrim behind closes the menu).
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // Header: logo, paused tag, close.
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(DbColors.Ink, DbColors.SlateHigh)))
                .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("DRAGON BLAST", style = LogoStyle.copy(fontSize = 20.sp), color = DbColors.Text, maxLines = 1)
                Text(
                    androidx.compose.ui.res.stringResource(R.string.menu_paused),
                    style = MaterialTheme.typography.labelMedium,
                    color = DbColors.Ember,
                    letterSpacing = 2.sp,
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DbColors.SlateHighest)
                    .clickable { controller.close() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = DbColors.TextDim, style = MaterialTheme.typography.titleMedium) }
        }
        // Tabs.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            model.tabs.forEachIndexed { i, t ->
                val on = i == controller.tab
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) DbColors.Ember.copy(alpha = 0.16f) else Color.Transparent)
                        .border(1.dp, if (on) DbColors.Ember else DbColors.LineSoft, RoundedCornerShape(10.dp))
                        .clickable { controller.selectTab(i) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        androidx.compose.ui.res.stringResource(t),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) DbColors.Ember else DbColors.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        ) {
            itemsIndexed(items) { i, item ->
                MenuRow(
                    item,
                    selected = i == controller.row,
                    onClick = {
                        controller.row = i
                        controller.activate()
                    },
                    onAdjust = { d ->
                        controller.row = i
                        controller.adjust(d)
                    },
                )
            }
        }
        // Footer: help of the selected row, controller hints, resume.
        val selected = items.getOrNull(controller.row)
        Column(
            Modifier
                .fillMaxWidth()
                .background(DbColors.Ink.copy(alpha = 0.8f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val reopen = androidx.compose.ui.res.stringResource(R.string.menu_reopen_hint)
            Text(
                listOfNotNull(selected?.help, if (selected?.reopen == true) reopen else null).joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = DbColors.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(34.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    androidx.compose.ui.res.stringResource(R.string.menu_hints_pad),
                    style = MaterialTheme.typography.labelSmall,
                    color = DbColors.TextFaint,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(DbColors.EmberGlow, DbColors.Ember)))
                        .clickable { controller.close() }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(
                        "▶  " + androidx.compose.ui.res.stringResource(R.string.menu_resume).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = DbColors.Ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(item: MenuItem, selected: Boolean, onClick: () -> Unit, onAdjust: (Int) -> Unit) {
    when (item) {
        is MHeader -> Text(
            item.label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = DbColors.Ember,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
        )
        is MInfo -> Text(
            item.label,
            style = MaterialTheme.typography.bodySmall,
            color = DbColors.TextDim,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
        else -> {
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(shape)
                    .background(if (selected) DbColors.SlateHighest else DbColors.Slate.copy(alpha = 0.55f))
                    .border(1.dp, if (selected) DbColors.Ember.copy(alpha = 0.8f) else Color.Transparent, shape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(width = 3.dp, height = 22.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DbColors.Ember),
                    )
                    Spacer(Modifier.width(9.dp))
                }
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item is MAction && item.danger) DbColors.Error else DbColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.reopen) {
                        Text(
                            "↻ " + androidx.compose.ui.res.stringResource(R.string.menu_reopen),
                            style = MaterialTheme.typography.labelSmall,
                            color = DbColors.Steel,
                            maxLines = 1,
                        )
                    }
                }
                when (item) {
                    is MToggle -> TogglePill(item.value)
                    is MChoice -> Stepper(item.options.getOrElse(item.index) { "-" }, onAdjust)
                    is MStep -> Stepper(item.text, onAdjust)
                    is MAction -> Text(
                        item.actionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (item.danger) DbColors.Error else DbColors.Ember,
                    )
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun TogglePill(on: Boolean) {
    Box(
        Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) DbColors.Ember else DbColors.Graphite)
            .border(1.dp, if (on) DbColors.EmberGlow else DbColors.Line, RoundedCornerShape(50))
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (on) DbColors.Ink else DbColors.TextDim),
        )
    }
}

@Composable
private fun Stepper(value: String, onAdjust: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Arrow("‹") { onAdjust(-1) }
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = DbColors.EmberGlow,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 84.dp, max = 190.dp).padding(horizontal = 4.dp),
        )
        Arrow("›") { onAdjust(1) }
    }
}

@Composable
private fun Arrow(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(DbColors.Graphite)
            .border(1.dp, DbColors.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = DbColors.Text, style = MaterialTheme.typography.titleMedium)
    }
}
