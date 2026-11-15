package com.dfdx047.dragonblast.data

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Which of the universal APK's two native sets this CPU runs (GameLib, GameActivity).
 *
 * The Armv8.2 set (-march=armv8.2-a) uses inline LSE atomics, RDM and CRC32: on a CPU without them (Armv8.0, e.g.
 * the Snapdragon 665 of the Mi A3, Cortex-A73/A53) the game dies with SIGILL. Those get the Armv8.0 set ("_v80"),
 * whose atomics are __aarch64_* calls that use LSE at run time where it exists.
 *
 * Reads AT_HWCAP from /proc/self/auxv (what bionic's getauxval returns); failing that, the Features line of
 * /proc/cpuinfo. When in doubt, Armv8.0 (runs everywhere).
 */
object CpuSet {
    private const val AT_HWCAP = 16L
    private const val HWCAP_CRC32 = 1L shl 7
    private const val HWCAP_ATOMICS = 1L shl 8
    private const val HWCAP_ASIMDRDM = 1L shl 12
    private const val NEEDED = HWCAP_CRC32 or HWCAP_ATOMICS or HWCAP_ASIMDRDM

    private val hwcap: Long? by lazy {
        try {
            val b = ByteBuffer.wrap(File("/proc/self/auxv").readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            var found: Long? = null
            while (b.remaining() >= 16) {
                val type = b.long
                val value = b.long
                if (type == AT_HWCAP) { found = value; break }
                if (type == 0L) break
            }
            found
        } catch (_: Exception) {
            null
        }
    }

    private val cpuinfoFeatures: Set<String>? by lazy {
        try {
            File("/proc/cpuinfo").useLines { lines ->
                lines.firstOrNull { it.startsWith("Features") }?.substringAfter(':')?.trim()?.split(Regex("\\s+"))?.toSet()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Test switch: an empty files/force_armv80 runs the Armv8.0 set on any CPU (both sets run on Armv8.2). */
    private val forced80: Boolean by lazy {
        runCatching {
            File(android.os.Environment.getExternalStorageDirectory(),
                "Android/data/${com.dfdx047.dragonblast.BuildConfig.APPLICATION_ID}/files/force_armv80").isFile
        }.getOrDefault(false)
    }

    /** The CPU has what the Armv8.2 set uses (LSE, RDM, CRC32). */
    @JvmStatic
    val armv82: Boolean by lazy {
        if (forced80) return@lazy false
        hwcap?.let { it and NEEDED == NEEDED }
            ?: cpuinfoFeatures?.containsAll(listOf("atomics", "asimdrdm", "crc32"))
            ?: false
    }

    /** "" for the Armv8.2 set, "_v80" for the Armv8.0 one: the suffix of its runtime, GPU plugin and libgame. */
    @JvmStatic
    val suffix: String get() = if (armv82) "" else "_v80"

    /** Name of this set's libgame in the assets (no extension). */
    val libgame: String get() = "libgame$suffix"

    fun describe(): String = (if (armv82) "Armv8.2" else "Armv8.0") + (if (forced80) " (forced)" else "") +
        (hwcap?.let { " hwcap %x".format(it) } ?: " cpuinfo")
}
