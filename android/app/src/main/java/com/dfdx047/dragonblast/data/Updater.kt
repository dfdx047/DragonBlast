package com.dfdx047.dragonblast.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.content.FileProvider
import com.dfdx047.dragonblast.BuildConfig
import com.dfdx047.dragonblast.drivers.Http
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Launcher updates from the project's GitHub releases. Every release is signed with the same key and has a higher
 * versionCode, so the new APK installs over the old one (game files, saves and settings stay). The game itself is
 * decrypted again on the next Play when the update brings another build of it (GameLib "id").
 */
object Updater {
    const val REPO = "dfdx047/DragonBlast"
    const val RELEASES_URL = "https://github.com/$REPO/releases"
    private const val LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    /** Automatic checks at most this often. */
    private const val AUTO_INTERVAL_MS = 12L * 60 * 60 * 1000

    data class Release(
        val version: String,
        val title: String,
        val notes: String,
        val pageUrl: String,
        val apkName: String,
        val apkUrl: String,
        val apkSize: Long,
        /** "sha256:<hex>" from GitHub, when it gives one. */
        val digest: String?,
    )

    /** The latest release, or null when it is not newer than this build. */
    fun check(): Release? {
        val j = JSONObject(Http.getText(LATEST))
        val tag = j.getString("tag_name")
        val assets = j.getJSONArray("assets")
        var apk: JSONObject? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk")) { apk = a; break }
        }
        apk ?: return null
        val r = Release(
            version = tag.removePrefix("v"),
            title = j.optString("name", tag),
            notes = j.optString("body", ""),
            pageUrl = j.optString("html_url", RELEASES_URL),
            apkName = apk.getString("name"),
            apkUrl = apk.getString("browser_download_url"),
            apkSize = apk.optLong("size", -1),
            digest = apk.optString("digest", "").ifBlank { null },
        )
        return if (newer(r.version, BuildConfig.VERSION_NAME)) r else null
    }

    /** Whether an automatic check is due (and records it). */
    fun autoCheckDue(context: Context): Boolean {
        val p = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - p.getLong("last_check", 0) < AUTO_INTERVAL_MS) return false
        p.edit().putLong("last_check", now).apply()
        return true
    }

    /** "0.10.0" > "0.9.2"; a suffix ("-beta") sorts before the plain version. */
    fun newer(a: String, b: String): Boolean {
        fun parts(v: String): Pair<List<Int>, String> {
            val main = v.substringBefore('-')
            return main.split('.').map { it.toIntOrNull() ?: 0 } to (if ('-' in v) v.substringAfter('-') else "")
        }
        val (na, sa) = parts(a)
        val (nb, sb) = parts(b)
        for (i in 0 until maxOf(na.size, nb.size)) {
            val x = na.getOrElse(i) { 0 }
            val y = nb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return when {
            sa == sb -> false
            sa.isEmpty() -> true
            sb.isEmpty() -> false
            else -> sa > sb
        }
    }

    private fun dir(context: Context) = File(context.cacheDir, "updates")

    /** Downloads the release's APK into the cache and checks its size and SHA-256. */
    fun download(context: Context, r: Release, onProgress: (Long, Long) -> Unit): File {
        val d = dir(context)
        d.listFiles()?.forEach { it.delete() }   // older downloads
        val out = File(d, r.apkName)
        val part = File(d, r.apkName + ".part")
        Http.download(r.apkUrl, part, onProgress)
        if (r.apkSize > 0 && part.length() != r.apkSize) {
            part.delete()
            throw java.io.IOException("size ${part.length()} != ${r.apkSize}")
        }
        r.digest?.takeIf { it.startsWith("sha256:") }?.let { want ->
            val md = MessageDigest.getInstance("SHA-256")
            part.inputStream().use { s ->
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val n = s.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            val got = md.digest().joinToString("") { "%02x".format(it) }
            if (got != want.removePrefix("sha256:")) {
                part.delete()
                throw java.io.IOException("SHA-256 mismatch")
            }
        }
        if (!part.renameTo(out)) throw java.io.IOException("rename ${out.name}")
        return out
    }

    /** Android asks once whether this app may install apps; until then, its settings page. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Hands the APK to the system installer (it installs over this version: same key, higher versionCode). */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
