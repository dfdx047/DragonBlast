package com.dfdx047.dragonblast.data

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build

enum class GpuVendor { ADRENO, MALI, XCLIPSE, POWERVR, OTHER }

/**
 * This device's GPU, from GL_RENDERER / GL_VERSION (an offscreen EGL context, ~20 ms, once; cached in
 * SharedPreferences "device" after that) and the Vulkan version the system declares.
 */
data class GpuInfo(
    val renderer: String,
    val glVersion: String,
    val vulkanVersion: String,
    val vendor: GpuVendor,
    /** "740" for an Adreno 740, "615" for a Mali-G615; null when the name has no number. */
    val model: Int?,
) {
    val adrenoGeneration: Int get() = if (vendor == GpuVendor.ADRENO) (model ?: 0) / 100 else 0

    /** The system driver's version as the GL string shows it (Mali: "r49p0", Adreno: "V@0762.10"). */
    val driverVersion: String
        get() = Regex("""v1\.(r\d+p\d+)""").find(glVersion)?.groupValues?.get(1)
            ?: Regex("""V@(\S+)""").find(glVersion)?.groupValues?.get(1)
            ?: glVersion.substringAfter("OpenGL ES ").ifBlank { "?" }

    companion object {
        @Volatile private var cached: GpuInfo? = null

        fun get(context: Context): GpuInfo {
            cached?.let { return it }
            val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)
            val stamp = Build.FINGERPRINT
            var renderer = prefs.getString("gl_renderer", null)
            var version = prefs.getString("gl_version", null)
            if (renderer == null || version == null || prefs.getString("gl_stamp", "") != stamp) {
                val (r, v) = probeGl()
                renderer = r
                version = v
                prefs.edit().putString("gl_renderer", r).putString("gl_version", v).putString("gl_stamp", stamp).apply()
            }
            val vendor = when {
                renderer.contains("Adreno", true) -> GpuVendor.ADRENO
                renderer.contains("Mali", true) || renderer.contains("Immortalis", true) -> GpuVendor.MALI
                renderer.contains("Xclipse", true) -> GpuVendor.XCLIPSE
                renderer.contains("PowerVR", true) -> GpuVendor.POWERVR
                isQualcomm() -> GpuVendor.ADRENO
                else -> GpuVendor.OTHER
            }
            val model = Regex("""(?:Adreno \(TM\) |Mali-G|Immortalis-G|Xclipse )(\d+)""").find(renderer)
                ?.groupValues?.get(1)?.toIntOrNull()
            return GpuInfo(renderer.ifBlank { "?" }, version, vulkanVersion(context), vendor, model).also { cached = it }
        }

        private fun isQualcomm(): Boolean {
            val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else ""
            return soc.equals("QTI", true) || soc.contains("qualcomm", true) || Build.HARDWARE.contains("qcom", true)
        }

        fun socName(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}" else Build.HARDWARE

        private fun vulkanVersion(context: Context): String {
            val pm = context.packageManager
            for ((code, label) in listOf(0x404000 to "1.4", 0x403000 to "1.3", 0x402000 to "1.2", 0x401000 to "1.1", 0x400000 to "1.0")) {
                if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, code)) return label
            }
            return "-"
        }

        /** GL_RENDERER and GL_VERSION from a 1x1 pbuffer context. */
        private fun probeGl(): Pair<String, String> {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return "" to ""
            val ver = IntArray(2)
            if (!EGL14.eglInitialize(display, ver, 0, ver, 1)) return "" to ""
            try {
                val configs = arrayOfNulls<EGLConfig>(1)
                val count = IntArray(1)
                val attrs = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE,
                )
                if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0) || count[0] == 0) return "" to ""
                val ctx = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(0x3098, 2, EGL14.EGL_NONE), 0)
                val surf = EGL14.eglCreatePbufferSurface(display, configs[0], intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
                EGL14.eglMakeCurrent(display, surf, surf, ctx)
                val r = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""
                val v = GLES20.glGetString(GLES20.GL_VERSION) ?: ""
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, surf)
                EGL14.eglDestroyContext(display, ctx)
                return r to v
            } catch (_: Throwable) {
                return "" to ""
            } finally {
                EGL14.eglTerminate(display)
            }
        }
    }
}
