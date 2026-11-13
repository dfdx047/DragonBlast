package com.dfdx047.dragonblast.drivers

import android.content.Context
import android.net.Uri
import android.os.Build
import com.dfdx047.dragonblast.data.AppPaths
import com.dfdx047.dragonblast.data.GpuInfo
import com.dfdx047.dragonblast.data.GpuVendor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Where drivers come from: the GitHub repositories of the people who build them, credited by name. All of
 * them publish AdrenoTools packages (a zip with meta.json and the Vulkan .so), which the game shell loads
 * through libadrenotools; if the driver does not find the GPU the game falls back to the phone's own driver
 * (native/src/custom_driver.cpp).
 */
data class DriverSource(
    val owner: String,
    val repo: String,
    val vendor: GpuVendor,
    /** Short name shown as the title. */
    val label: String,
    /** String resource with one line about the builds. */
    val about: Int,
    /** Whether a release asset is a driver for this GPU. */
    val accepts: (name: String, gpu: GpuInfo) -> Boolean = { n, _ -> isPackage(n) },
    /** Whether the repository builds for this GPU at all. */
    val supports: (gpu: GpuInfo) -> Boolean = { true },
    val experimental: Boolean = false,
) {
    val key get() = "$owner/$repo"
    val page get() = "https://github.com/$owner/$repo/releases"
}

fun isPackage(name: String): Boolean {
    val n = name.lowercase()
    return n.endsWith(".zip") || n.endsWith(".adpkg")
}

/** Turnip zips by generation: "a8xx"/"gen8" builds only on Adreno 8xx, the rest on 6xx/7xx; OneUI only on Samsung. */
private fun turnipFor(name: String, gpu: GpuInfo): Boolean {
    val n = name.lowercase()
    if (!isPackage(n)) return false
    if ("oneui" in n && !Build.MANUFACTURER.equals("samsung", true)) return false
    val gen8Build = "a8xx" in n || "gen8" in n
    val generation = gpu.adrenoGeneration
    if (generation == 0) return true
    return if (generation >= 8) gen8Build || "qualcomm" in n || n.startsWith("v8") || n.startsWith("8") else !gen8Build
}

private fun maliModel(gpu: GpuInfo) = gpu.model
private fun maliBifrost(gpu: GpuInfo) = maliModel(gpu) in setOf(31, 51, 52, 71, 72, 76)
private fun maliValhallJm(gpu: GpuInfo) = maliModel(gpu) in setOf(57, 68, 77, 78)
private fun maliCsf(gpu: GpuInfo) = maliModel(gpu).let { it != null && (it >= 300 || it == 1) }
private fun maliUnknown(gpu: GpuInfo) = gpu.vendor != GpuVendor.MALI || gpu.model == null

object DriverCatalog {
    val SOURCES: List<DriverSource> = listOf(
        // Adreno: Turnip (Mesa) and Qualcomm packages
        DriverSource("MrPurple666", "purple-turnip", GpuVendor.ADRENO, "Mr. Purple", com.dfdx047.dragonblast.R.string.src_purple, ::turnipFor),
        DriverSource("StevenMXZ", "Adreno-Tools-Drivers", GpuVendor.ADRENO, "StevenMXZ", com.dfdx047.dragonblast.R.string.src_steven, ::turnipFor),
        DriverSource("K11MCH1", "AdrenoToolsDrivers", GpuVendor.ADRENO, "K11MCH1", com.dfdx047.dragonblast.R.string.src_k11mch1, ::turnipFor),
        DriverSource("whitebelyash", "freedreno_turnip-CI", GpuVendor.ADRENO, "whitebelyash", com.dfdx047.dragonblast.R.string.src_whitebelyash),
        DriverSource("faux123", "turnip-faux123-driver", GpuVendor.ADRENO, "faux123", com.dfdx047.dragonblast.R.string.src_faux123),
        DriverSource("bmdhacks", "armsx2-turnip", GpuVendor.ADRENO, "ARMSX2", com.dfdx047.dragonblast.R.string.src_armsx2_turnip),
        // Mali: PanVK (Mesa) and MaliSX2
        DriverSource(
            "zenithblue-oss", "panvk-kbase-android", GpuVendor.MALI, "zenithblue", com.dfdx047.dragonblast.R.string.src_zenithblue,
            // newer releases: one "-adrenotools.zip" among packages for other apps; older ones: G615-only ".adpkg.zip" builds
            accepts = { n, gpu ->
                val l = n.lowercase()
                when {
                    l.endsWith("-adrenotools.zip") -> true
                    l.endsWith(".adpkg.zip") -> "g615" !in l || gpu.model == 615 || gpu.vendor != GpuVendor.MALI
                    else -> false
                }
            },
        ),
        DriverSource("JimVulkan", "mali-panvk", GpuVendor.MALI, "JimVulkan", com.dfdx047.dragonblast.R.string.src_jimvulkan),
        DriverSource(
            "bmdhacks", "malisx2", GpuVendor.MALI, "MaliSX2 (ARMSX2)", com.dfdx047.dragonblast.R.string.src_malisx2,
            supports = { it.model in setOf(52, 76, 57, 68, 77, 78, 615, 715) || maliUnknown(it) },
            experimental = true,
        ),
        DriverSource(
            "FristOneRR", "FristOneRR-Panvk-Driver", GpuVendor.MALI, "FristOneRR", com.dfdx047.dragonblast.R.string.src_fristonerr,
            supports = { maliBifrost(it) || maliValhallJm(it) || maliUnknown(it) },
        ),
        DriverSource(
            "MineGraph-bilibili", "panvk-Android", GpuVendor.MALI, "MineGraph", com.dfdx047.dragonblast.R.string.src_minegraph,
            supports = { maliCsf(it) || maliUnknown(it) },
        ),
        DriverSource(
            "faux123", "panvk-faux123-driver-ace", GpuVendor.MALI, "faux123 PanVK", com.dfdx047.dragonblast.R.string.src_faux123_panvk,
            supports = { maliCsf(it) || maliUnknown(it) },
            experimental = true,
        ),
    )

    fun sources(vendor: GpuVendor) = SOURCES.filter { it.vendor == vendor }

    data class Asset(val name: String, val url: String, val size: Long)
    data class Release(val tag: String, val title: String, val date: String, val prerelease: Boolean, val assets: List<Asset>)
    data class Listing(val releases: List<Release>, val fetchedAt: Long)

    private const val MAX_AGE_MS = 6 * 60 * 60 * 1000L
    private fun cacheFile(s: DriverSource) = File(AppPaths.internal, "driver_lists/${s.owner}_${s.repo}.json")

    /** Up to [count] newest releases, from the 6-hour cache when it is fresh (GitHub allows 60 calls/hour without login). */
    fun releases(source: DriverSource, force: Boolean = false, count: Int = 40): Listing {
        val cache = cacheFile(source)
        if (!force && cache.isFile && System.currentTimeMillis() - cache.lastModified() < MAX_AGE_MS) {
            runCatching { return Listing(parse(JSONArray(cache.readText())), cache.lastModified()) }
        }
        return try {
            val text = Http.getText("https://api.github.com/repos/${source.owner}/${source.repo}/releases?per_page=$count")
            cache.parentFile?.mkdirs()
            cache.writeText(text)
            Listing(parse(JSONArray(text)), System.currentTimeMillis())
        } catch (e: Exception) {
            if (cache.isFile) Listing(parse(JSONArray(cache.readText())), cache.lastModified()) else throw e
        }
    }

    private fun parse(a: JSONArray): List<Release> {
        val out = ArrayList<Release>()
        for (i in 0 until a.length()) {
            val r = a.optJSONObject(i) ?: continue
            if (r.optBoolean("draft")) continue
            val assets = ArrayList<Asset>()
            r.optJSONArray("assets")?.let { list ->
                for (j in 0 until list.length()) {
                    val x = list.optJSONObject(j) ?: continue
                    assets += Asset(x.optString("name"), x.optString("browser_download_url"), x.optLong("size"))
                }
            }
            out += Release(
                tag = r.optString("tag_name"),
                title = r.optString("name").ifBlank { r.optString("tag_name") },
                date = r.optString("published_at").take(10),
                prerelease = r.optBoolean("prerelease"),
                assets = assets,
            )
        }
        return out
    }
}

/** A driver in the app's store (downloaded or imported). */
data class InstalledDriver(
    val dir: File,
    val name: String,
    val version: String,
    val author: String,
    val library: File,
    val vendor: GpuVendor,
    val minApi: Int,
)

class DriverException(val reason: Reason, detail: String = "") : Exception(detail) {
    enum class Reason { NO_LIBRARY, NOT_ZIP, IO, NETWORK, MIN_API }
}

object DriverStore {
    private const val PREFS = "options"
    // Same numbers as LaunchConfig: 0 automatic, 1 phone's own, 2 bundled Turnip, 4 custom.
    const val CHOICE_AUTO = 0
    const val CHOICE_SYSTEM = 1
    const val CHOICE_CUSTOM = 4

    fun vendorOf(lib: String): GpuVendor {
        val n = lib.lowercase()
        return when {
            "panfrost" in n || "panvk" in n || "mali" in n -> GpuVendor.MALI
            "radeon" in n || "radv" in n -> GpuVendor.XCLIPSE
            else -> GpuVendor.ADRENO
        }
    }

    fun installed(): List<InstalledDriver> =
        AppPaths.driverStore.listFiles()?.filter { it.isDirectory }?.mapNotNull(::read)?.sortedByDescending { it.dir.lastModified() }.orEmpty()

    private fun read(dir: File): InstalledDriver? {
        val meta = File(dir, "meta.json").takeIf { it.isFile }?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
        val libName = meta?.optString("libraryName").orEmpty()
        val lib = File(dir, libName).takeIf { libName.isNotEmpty() && it.isFile }
            ?: dir.listFiles()?.firstOrNull { it.name.endsWith(".so") }
            ?: return null
        return InstalledDriver(
            dir = dir,
            name = meta?.optString("name").orEmpty().ifBlank { dir.name },
            version = meta?.optString("driverVersion").orEmpty(),
            author = meta?.optString("author").orEmpty(),
            library = lib,
            vendor = vendorOf(lib.name),
            minApi = meta?.optInt("minApi", 0) ?: 0,
        )
    }

    fun download(asset: DriverCatalog.Asset, onProgress: (Long, Long) -> Unit): InstalledDriver {
        val tmp = File(AppPaths.tmp, "dl_${System.currentTimeMillis()}_${asset.name}")
        try {
            Http.download(asset.url, tmp, onProgress)
        } catch (e: Exception) {
            tmp.delete()
            throw DriverException(DriverException.Reason.NETWORK, e.message ?: "")
        }
        try {
            return install(tmp, asset.name)
        } finally {
            tmp.delete()
        }
    }

    fun import(context: Context, uri: Uri, displayName: String): InstalledDriver {
        val tmp = File(AppPaths.tmp, "import_${System.currentTimeMillis()}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                ?: throw DriverException(DriverException.Reason.IO, displayName)
            return install(tmp, displayName)
        } finally {
            tmp.delete()
        }
    }

    /** Unpacks an AdrenoTools zip (or a loose .so) into driver_store/<name>/. */
    private fun install(file: File, displayName: String): InstalledDriver {
        val base = displayName.removeSuffix(".zip").removeSuffix(".adpkg").replace(Regex("""[\\/:*?"<>|]"""), "_")
        var dest = File(AppPaths.driverStore, base)
        var i = 2
        while (dest.exists()) dest = File(AppPaths.driverStore, "${base}_${i++}")
        dest.mkdirs()
        try {
            if (displayName.endsWith(".so")) {
                file.copyTo(File(dest, displayName), overwrite = true)
                File(dest, "meta.json").writeText(JSONObject().put("name", base).put("libraryName", displayName).toString(2))
            } else {
                var entries = 0
                ZipInputStream(file.inputStream().buffered()).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        if (e.isDirectory) continue
                        // Loose files only (meta.json, .so): flattens folders and avoids zip-slip.
                        val name = e.name.substringAfterLast('/')
                        if (name.isEmpty() || name.startsWith(".")) continue
                        File(dest, name).outputStream().use { zip.copyTo(it) }
                        entries++
                    }
                }
                if (entries == 0) throw DriverException(DriverException.Reason.NOT_ZIP, displayName)
            }
            val d = read(dest) ?: throw DriverException(DriverException.Reason.NO_LIBRARY, displayName)
            if (d.minApi > Build.VERSION.SDK_INT) throw DriverException(DriverException.Reason.MIN_API, d.minApi.toString())
            return d
        } catch (e: DriverException) {
            dest.deleteRecursively()
            throw e
        } catch (e: java.util.zip.ZipException) {
            dest.deleteRecursively()
            throw DriverException(DriverException.Reason.NOT_ZIP, displayName)
        } catch (e: Exception) {
            dest.deleteRecursively()
            throw DriverException(DriverException.Reason.IO, e.message ?: "")
        }
    }

    fun choice(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("gpu_driver", CHOICE_AUTO)

    /** Folder name (in the store) of the driver the game will load, when the choice is Custom. */
    fun activeName(context: Context): String? {
        if (choice(context) != CHOICE_CUSTOM) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("custom_driver_store", null)
    }

    /** Copies the driver's files where the game shell looks (drivers/custom) and selects it. */
    fun activate(context: Context, d: InstalledDriver) {
        val dst = AppPaths.activeDriver
        dst.deleteRecursively()
        dst.mkdirs()
        d.dir.listFiles()?.filter { it.isFile }?.forEach { it.copyTo(File(dst, it.name), overwrite = true) }
        File(AppPaths.internal, "drivers/.failed_custom").delete()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("gpu_driver", CHOICE_CUSTOM)
            .putString("custom_driver_name", d.name + if (d.version.isNotBlank()) " ${d.version}" else "")
            .putString("custom_driver_lib", d.library.name)
            .putString("custom_driver_store", d.dir.name)
            .apply()
    }

    fun select(context: Context, choice: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("gpu_driver", choice).apply()
        if (choice != CHOICE_CUSTOM) AppPaths.activeDriver.deleteRecursively()
    }

    fun delete(context: Context, d: InstalledDriver) {
        if (activeName(context) == d.dir.name) select(context, CHOICE_AUTO)
        d.dir.deleteRecursively()
    }

    /** What the last launch actually ran on, as the game wrote it (custom_driver.cpp). */
    fun lastUsed(): String = runCatching { File(AppPaths.internal, "drivers/last_driver.txt").readLines().firstOrNull()?.trim() }.getOrNull().orEmpty()

    /** Bundled Turnip (tools/android/fetch_turnip.sh) in this APK? */
    fun hasBundledTurnip(context: Context): Boolean = runCatching { context.assets.list("drivers/turnip")?.isNotEmpty() == true }.getOrDefault(false)
}

object Http {
    private const val UA = "DragonBlast-Android"

    private fun open(url: String): HttpURLConnection {
        var u = url
        repeat(6) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", UA)
            c.setRequestProperty("Accept", "application/vnd.github+json, */*")
            val code = c.responseCode
            if (code in 300..399) {
                u = c.getHeaderField("Location") ?: throw java.io.IOException("redirect without location")
                c.disconnect()
            } else if (code !in 200..299) {
                c.disconnect()
                throw java.io.IOException("HTTP $code")
            } else {
                return c
            }
        }
        throw java.io.IOException("too many redirects")
    }

    fun getText(url: String): String = open(url).let { c -> c.inputStream.use { String(it.readBytes()) }.also { c.disconnect() } }

    fun download(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        val c = open(url)
        val total = c.contentLengthLong
        dest.parentFile?.mkdirs()
        c.inputStream.use { input ->
            dest.outputStream().use { out ->
                val buf = ByteArray(1 shl 16)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    onProgress(done, total)
                }
            }
        }
        c.disconnect()
    }
}
