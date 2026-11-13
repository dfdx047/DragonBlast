package com.dfdx047.dragonblast.game

/**
 * What the touch controls may ask the game about (android/native/src/game_state_jni.cpp). The fight state comes from
 * Raging Blast 2's memory: the scene word (menus / fight) and the fight's pause flag.
 */
object NativeGameState {
    const val SIDE_UNKNOWN = 0
    const val SIDE_LEFT = 1
    const val SIDE_RIGHT = 2

    /** Raging Blast 2 is 3D with the camera behind player 1: "forward" is always stick up. */
    fun p1Side(): Int = SIDE_UNKNOWN

    fun syncPause(overlayOpen: Boolean, pauseQuickMenu: Boolean): Boolean = false

    const val STATE_UNKNOWN = -1
    const val STATE_MENUS = 0
    const val STATE_FIGHT = 1
    const val STATE_PAUSED = 2

    /** -1 = no game yet, 0 = menus, 1 = fight, 2 = fight with the pause menu open. */
    fun fightState(): Int = try {
        nativeFightState()
    } catch (e: UnsatisfiedLinkError) {
        STATE_UNKNOWN
    }

    @JvmStatic private external fun nativeFightState(): Int

    /** 1 = in a fight (not paused), 0 = menus or pause, -1 = unknown. */
    fun fightFlag(): Int = when (fightState()) {
        STATE_FIGHT -> 1
        STATE_UNKNOWN -> -1
        else -> 0
    }

    fun mapFlag(): Int = -1

    fun battleDebug(): String? = null

    fun dumpFight(path: String, kind: Int) {}

    fun gpuLost(): Boolean = false
}
