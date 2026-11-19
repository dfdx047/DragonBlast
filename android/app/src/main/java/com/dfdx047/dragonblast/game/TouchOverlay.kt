package com.dfdx047.dragonblast.game

import android.app.Activity
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.dfdx047.dragonblast.controls.ControlAction
import com.dfdx047.dragonblast.controls.ControlElement
import com.dfdx047.dragonblast.controls.ControlLayout
import com.dfdx047.dragonblast.controls.ControlLayoutStore
import com.dfdx047.dragonblast.controls.GameActions
import com.dfdx047.dragonblast.controls.LayoutKind
import com.dfdx047.dragonblast.controls.PadInput
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.Settings
import com.dfdx047.dragonblast.data.SettingsStore
import com.dfdx047.dragonblast.data.TouchMode
import org.libsdl.app.DragonBlastPad

/**
 * On-screen controls over the running game (GameActivity). The touch view feeds the [HelperEngine], which drives the
 * SDL virtual pad that the runtime sees as player 1 (SDK patch 0041).
 *
 * Visibility follows the launcher's "Show" option: off, always, or automatic (shown on a touch, hidden as soon as a
 * physical pad is used). The fight / menus layouts switch with the small ⇄ button until the game's fight state is
 * read from memory.
 */
class TouchOverlay(private val activity: Activity, private val parent: ViewGroup) : InputManager.InputDeviceListener {
    private val main = Handler(Looper.getMainLooper())
    private val engine = HelperEngine()
    private val startedAt = SystemClock.uptimeMillis()
    private lateinit var settings: Settings
    private var view: TouchControlsView? = null
    private var fight = false
    private var autoShown = true
    private var sideMenuOpen = false

    private val attachRunnable = Runnable { attachPad() }

    // Fight / menus from the game's memory: the layout follows when the state changes (twice in a row, 250 ms
    // apart). The ⇄ button still switches by hand until the next change.
    private var lastState = NativeGameState.STATE_UNKNOWN
    private var pendingState = NativeGameState.STATE_UNKNOWN
    private val statePoll = object : Runnable {
        override fun run() {
            val st = NativeGameState.fightFlag()
            if (st != -1) {
                if (st == pendingState && st != lastState) {
                    lastState = st
                    switchLayout(st == 1)
                }
                pendingState = st
            }
            main.postDelayed(this, 250)
        }
    }

    fun onCreate() {
        AppPaths.init(activity)
        settings = SettingsStore(activity).load()
        val v = TouchControlsView(activity).apply {
            applySettings(settings)
            listener = object : TouchControlsView.Listener {
                override fun onPress(element: ControlElement) {
                    if (element.id == GameActions.TOGGLE) {
                        switchLayout(!fight)
                        return
                    }
                    ensurePad()
                    engine.press(element.id, element.action)
                }

                override fun onRelease(element: ControlElement) {
                    if (element.id != GameActions.TOGGLE) engine.releaseElement(element.id)
                }

                override fun onStick(x: Float, y: Float) {
                    ensurePad()
                    engine.setStick(x, y)
                }

                override fun onDpad(element: ControlElement, inputs: Set<PadInput>) {
                    ensurePad()
                    engine.setDpad(element.id, inputs)
                }

                override fun onSwipeStart(element: ControlElement) {
                    ensurePad()
                    engine.swipeDown(element.id, element.action)
                }

                override fun onSwipeDir(element: ControlElement, dir: Int) {
                    engine.swipeDir(element.id, element.radial.getOrNull(dir))
                }

                override fun onSwipeEnd(element: ControlElement, tapped: Boolean) {
                    engine.swipeUp(element.id, if (tapped) element.tap else null)
                }
            }
        }
        view = v
        v.layout = currentLayout()
        parent.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        autoShown = !physicalPadConnected()
        (activity.getSystemService(Activity.INPUT_SERVICE) as InputManager).registerInputDeviceListener(this, main)
        // The virtual pad is attached once SDL is up (its JNI needs the native side).
        main.postDelayed(attachRunnable, 2500)
        main.postDelayed(statePoll, 3000)
        updateVisibility()
    }

    private fun currentLayout(): ControlLayout =
        ControlLayoutStore.load(if (fight) LayoutKind.GAMEPLAY else LayoutKind.MENU, settings.controlScheme)

    private fun switchLayout(toFight: Boolean) {
        fight = toFight
        view?.let {
            it.releaseAll()
            it.layout = currentLayout()
        }
    }

    /** Fight layout on screen (true) or the menu layout. */
    val isFightLayout: Boolean get() = fight

    fun showFightLayout(toFight: Boolean) {
        if (toFight != fight) switchLayout(toFight)
    }

    /** Settings or saved layouts changed (in-game menu, editor): read them again. */
    fun reload() {
        settings = SettingsStore(activity).load()
        view?.let {
            it.releaseAll()
            it.applySettings(settings)
            it.layout = currentLayout()
        }
        updateVisibility()
    }

    private fun ensurePad() {
        if (!DragonBlastPad.isAttached() && SystemClock.uptimeMillis() - startedAt > 1500) attachPad()
    }

    private fun attachPad() {
        if (DragonBlastPad.attach()) engine.start()
    }

    /** The side menu opened / closed: controls hide and release while it is open. */
    fun setSideMenuOpen(open: Boolean) {
        sideMenuOpen = open
        updateVisibility()
    }

    /** A touch with the controls hidden in automatic mode shows them and is consumed. */
    fun onScreenTouch(ev: MotionEvent): Boolean {
        if (ev.actionMasked != MotionEvent.ACTION_DOWN) return false
        if (settings.touchMode != TouchMode.AUTO || sideMenuOpen || autoShown) return false
        autoShown = true
        ensurePad()
        updateVisibility()
        return true
    }

    /** A real pad button or a deliberate stick move hides the controls in automatic mode. */
    fun onPhysicalInput(event: InputEvent) {
        if (settings.touchMode != TouchMode.AUTO || !autoShown) return
        val dev = event.device ?: return
        if (dev.isVirtual) return
        val s = event.source
        val pad = s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (!pad) return
        if (event is MotionEvent && STICK_AXES.none { kotlin.math.abs(event.getAxisValue(it)) > 0.5f }) return
        if (event is KeyEvent && (event.action != KeyEvent.ACTION_DOWN || event.keyCode == KeyEvent.KEYCODE_BACK)) return
        autoShown = false
        view?.releaseAll()
        updateVisibility()
    }

    fun onPause() {
        view?.releaseAll()
    }

    fun onDestroy() {
        main.removeCallbacks(attachRunnable)
        main.removeCallbacks(statePoll)
        (activity.getSystemService(Activity.INPUT_SERVICE) as InputManager).unregisterInputDeviceListener(this)
        engine.release()
        DragonBlastPad.detach()
    }

    private fun physicalPadConnected(): Boolean =
        InputDevice.getDeviceIds().any { id ->
            val d = InputDevice.getDevice(id) ?: return@any false
            !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                d.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
        }

    private fun updateVisibility() {
        val v = view ?: return
        val show = !sideMenuOpen && when (settings.touchMode) {
            TouchMode.ALWAYS -> true
            TouchMode.OFF -> false
            TouchMode.AUTO -> autoShown
        }
        if (!show) v.releaseAll()
        v.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        if (settings.touchMode == TouchMode.AUTO && physicalPadConnected()) autoShown = false
        updateVisibility()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        if (settings.touchMode == TouchMode.AUTO && !physicalPadConnected()) autoShown = true
        updateVisibility()
    }

    override fun onInputDeviceChanged(deviceId: Int) = updateVisibility()

    private companion object {
        val STICK_AXES = intArrayOf(
            MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y, MotionEvent.AXIS_LTRIGGER,
            MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS, MotionEvent.AXIS_BRAKE,
        )
    }
}
