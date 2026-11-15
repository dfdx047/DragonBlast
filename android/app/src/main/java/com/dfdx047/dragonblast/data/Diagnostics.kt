package com.dfdx047.dragonblast.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.dfdx047.dragonblast.BuildConfig
import com.dfdx047.dragonblast.drivers.DriverStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Is the game engine (native libraries + SDL shell) in this APK? Launcher-only builds have no engine. */
object Engine {
    fun available(context: Context): Boolean {
        if (BuildConfig.UI_ONLY) return false
        val dir = context.applicationInfo.nativeLibraryDir ?: return false
        // This CPU's runtime (librexruntime*.so or librexruntime_v80*.so) and its encrypted game (GameLib).
        val runtime = File(dir).list()?.any { it.startsWith("librexruntime" + CpuSet.suffix) &&
            (CpuSet.suffix.isNotEmpty() || !it.contains("_v80")) } ?: false
        return runtime && GameLib.inApk(context)
    }
}

object Diagnostics {
    fun deviceReport(context: Context, settings: Settings): String {
        val gpu = GpuInfo.get(context)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val install = InstallState.scan()
        return buildString {
            appendLine("Dragon Blast ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})${if (BuildConfig.UI_ONLY) " launcher-only" else ""}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("SoC: ${GpuInfo.socName()}")
            appendLine("GPU: ${gpu.renderer} | ${gpu.glVersion}")
            appendLine("Vulkan: ${gpu.vulkanVersion}")
            appendLine("RAM: ${mem.totalMem / (1024 * 1024)} MB")
            appendLine("Engine: ${if (Engine.available(context)) "yes" else "no"}")
            appendLine("Game: installed=${install.installed} xex=${install.xexOk} packs=${install.packsOk} files=${install.fileCount} size=${install.sizeBytes}")
            appendLine("Driver choice: ${DriverStore.choice(context)} active=${DriverStore.activeName(context) ?: "-"} last=${DriverStore.lastUsed().ifBlank { "-" }}")
            appendLine("Settings: $settings")
        }
    }

    /** Zips logs/, perf/, the device report and this app's logcat into the cache and returns a share intent. */
    fun exportLogs(context: Context, settings: Settings): Intent {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
        val out = File(context.cacheDir, "logs/DragonBlast-logs-$stamp.zip")
        out.parentFile?.deleteRecursively()
        out.parentFile?.mkdirs()
        ZipOutputStream(out.outputStream().buffered()).use { zip ->
            fun add(name: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            add("device.txt", deviceReport(context, settings).toByteArray())
            for (dir in listOf(AppPaths.logs, AppPaths.perf)) {
                dir.walkTopDown().filter { it.isFile && it.length() < 64L shl 20 }.forEach { f ->
                    add("${dir.name}/${f.relativeTo(dir).path}", f.readBytes())
                }
            }
            File(AppPaths.internal, "drivers/last_driver.txt").takeIf { it.isFile }?.let { add("last_driver.txt", it.readBytes()) }
            runCatching {
                val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "-t", "4000"))
                add("logcat.txt", p.inputStream.readBytes())
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", out)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dragon Blast logs $stamp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** The last lines of the game's log (logs/rb2.log), for the Debug tab. */
    fun logTail(lines: Int = 60): String {
        val f = File(AppPaths.logs, "rb2.log")
        if (!f.isFile) return ""
        return runCatching { f.readLines().takeLast(lines).joinToString("\n") }.getOrDefault("")
    }

    fun dirSize(dir: File): Long = if (!dir.exists()) 0 else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
