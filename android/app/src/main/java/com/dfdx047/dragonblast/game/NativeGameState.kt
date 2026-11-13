package com.dfdx047.dragonblast.game

/**
 * What the touch controls may ask the game about. Raging Blast 2 has no game-state bridge yet: every query answers
 * "unknown" and the overlay keeps the layout the player picked (fight / menus). When the fight flag is found in the
 * game's memory, [fightFlag] gets a native implementation and the layouts switch by themselves, as in Dragon Burst.
 */
object NativeGameState {
    const val SIDE_UNKNOWN = 0
    const val SIDE_LEFT = 1
    const val SIDE_RIGHT = 2

    /** Raging Blast 2 is 3D with the camera behind player 1: "forward" is always stick up. */
    fun p1Side(): Int = SIDE_UNKNOWN

    fun syncPause(overlayOpen: Boolean, pauseQuickMenu: Boolean): Boolean = false

    /** 1 = in a fight, 0 = menus, -1 = unknown. */
    fun fightFlag(): Int = -1

    fun mapFlag(): Int = -1

    fun battleDebug(): String? = null

    fun dumpFight(path: String, kind: Int) {}

    fun gpuLost(): Boolean = false
}
