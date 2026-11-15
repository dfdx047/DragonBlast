package com.dfdx047.dragonblast.data

import android.content.Context
import org.json.JSONObject
import java.io.DataInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Why the game code could not be unpacked; each reason has its own message. */
class GameLibException(val reason: Reason, val detail: String = "") : Exception(detail) {
    enum class Reason { NO_ENGINE, NO_XEX, UNSUPPORTED_VERSION, IO }
}

/**
 * The game code (libgame.so: the recompiled default.xex, the hooks, the Renut and its translated shaders) is game
 * data, so the APK only carries it encrypted (assets/libgame.zdat, tools/android/pack_libgame.py). The key is the
 * SHA-256 of the PE image of the player's own default.xex (libxeximage.so); the decrypted libgame.so goes to the
 * app's private files folder and GameActivity loads it from there. Done before the first game start and again when
 * an update brings another libgame (libgame.json "id").
 *
 * Universal APK: the Armv8.2 set (libgame.zdat) or the Armv8.0 one (libgame_v80.zdat, linked to
 * librexruntime_v80*.so), picked by CpuSet; each has its own "id", so switching sets decrypts again.
 */
object GameLib {
    private val ASSET get() = CpuSet.libgame + ".zdat"
    private val META get() = CpuSet.libgame + ".json"

    @JvmStatic
    fun file(context: Context) = File(context.filesDir, "libgame.so")
    private fun stamp(context: Context) = File(context.filesDir, "libgame.id")

    private class Meta(val titleId: String, val keySha256: String, val id: String, val size: Long)

    private fun meta(context: Context): Meta = context.assets.open(META).use { s ->
        val j = JSONObject(s.readBytes().decodeToString())
        Meta(j.getString("title_id"), j.getString("key_sha256"), j.getString("id"), j.getLong("size"))
    }

    /** This APK carries the encrypted game for this CPU. */
    fun inApk(context: Context): Boolean = try {
        context.assets.open(META).close()
        true
    } catch (_: Exception) {
        false
    }

    /** The decrypted libgame.so is there and is this APK's. */
    @JvmStatic
    fun ready(context: Context): Boolean = try {
        val m = meta(context)
        file(context).length() == m.size && stamp(context).readText() == m.id
    } catch (_: Exception) {
        false
    }

    @JvmStatic
    private external fun nativeXexImage(path: String, titleOut: IntArray, errorOut: Array<String?>): ByteArray?

    /**
     * Reads the installed default.xex, derives the key and decrypts libgame.so into the private files folder.
     * [onProgress] gets (bytes done, total).
     */
    fun install(context: Context, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        if (!inApk(context)) throw GameLibException(GameLibException.Reason.NO_ENGINE)
        val m = meta(context)
        if (ready(context)) return
        val xex = File(AppPaths.game, "default.xex")
        if (!xex.isFile) throw GameLibException(GameLibException.Reason.NO_XEX)
        System.loadLibrary("xeximage")
        val title = IntArray(1)
        val error = arrayOfNulls<String>(1)
        val pe = nativeXexImage(xex.absolutePath, title, error)
        val sha256 = { b: ByteArray -> MessageDigest.getInstance("SHA-256").digest(b) }
        val key = pe?.let(sha256)
        if (key == null || sha256(key).hex() != m.keySha256) {
            throw GameLibException(
                GameLibException.Reason.UNSUPPORTED_VERSION,
                "title ID %08X%s; expected %s".format(title[0], error[0]?.let { " ($it)" } ?: "", m.titleId),
            )
        }

        val out = file(context)
        val part = File(out.parentFile, out.name + ".part")
        val started = System.currentTimeMillis()
        stamp(context).delete()
        try {
            context.assets.open(ASSET).use { raw ->
                val input = DataInputStream(raw.buffered(1 shl 20))
                val header = ByteArray(24).also { input.readFully(it) }
                val hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                if (hb.getInt(0) != 0x3154445A) throw GameLibException(GameLibException.Reason.IO, ASSET)  // "ZDT1"
                val chunk = hb.getInt(4)
                val size = hb.getLong(8)
                val nonce = ByteArray(12).also { System.arraycopy(header, 16, it, 0, 8) }
                val spec = SecretKeySpec(key, "AES")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val buf = ByteArray(chunk + 16)
                part.outputStream().buffered(1 shl 20).use { o ->
                    var done = 0L
                    var i = 0
                    while (done < size) {
                        val n = minOf(chunk.toLong(), size - done).toInt()
                        input.readFully(buf, 0, n + 16)
                        ByteBuffer.wrap(nonce, 8, 4).order(ByteOrder.BIG_ENDIAN).putInt(i)
                        cipher.init(Cipher.DECRYPT_MODE, spec, GCMParameterSpec(128, nonce))
                        cipher.updateAAD(header)
                        o.write(cipher.doFinal(buf, 0, n + 16))
                        done += n
                        i++
                        onProgress(done, size)
                    }
                }
            }
            if (!part.renameTo(out)) throw GameLibException(GameLibException.Reason.IO, out.name)
            stamp(context).writeText(m.id)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw GameLibException(GameLibException.Reason.UNSUPPORTED_VERSION, "$ASSET: ${e.message}")
        } finally {
            part.delete()
        }
        val line = "libgame.so ready (${CpuSet.describe()}, ${out.length() / (1 shl 20)} MB, id ${m.id}, " +
            "title ${m.titleId}, ${System.currentTimeMillis() - started} ms)"
        android.util.Log.i("DragonBlast", line)
        try {
            File(AppPaths.logs.apply { mkdirs() }, "update.log").appendText(
                "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())} $line\n",
            )
        } catch (_: Exception) {
        }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
}
