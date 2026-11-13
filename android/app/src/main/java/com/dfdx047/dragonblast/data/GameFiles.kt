package com.dfdx047.dragonblast.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * Where things live: everything under the app's external files folder (Android/data/<package>/files), the
 * same layout RB2-Android's game shell uses (LaunchConfig): game/ (the extracted disc), saves/, cache/,
 * logs/, perf/, args.txt.
 */
object AppPaths {
    lateinit var root: File
        private set
    lateinit var internal: File
        private set

    fun init(context: Context) {
        root = context.getExternalFilesDir(null) ?: context.filesDir
        internal = context.filesDir
    }

    val game get() = File(root, "game")
    val saves get() = File(root, "saves")
    val cache get() = File(root, "cache")
    val logs get() = File(root, "logs")
    val perf get() = File(root, "perf")
    /** Downloaded / imported drivers, one folder each (meta.json + .so). */
    val driverStore get() = File(internal, "driver_store")
    /** The game shell loads a custom driver from here (LaunchConfig: drivers/custom/<lib>). */
    val activeDriver get() = File(internal, "drivers/custom")
    val tmp get() = File(internal, "tmp").also { it.mkdirs() }
}

/** The supported disc: Xbox 360, USA, Rev 1 (title 4E4D0826). */
object GameDisc {
    const val XEX_SIZE = 7_151_616L
    /** SHA-256 of the supported default.xex (RB2-Android's installer). */
    val ACCEPTED_XEX_SHA256 = setOf("d0291e647c33a5837a5d3521198ecb67a4e8d9bee80bbf691254a234e3ee989c")
    /** The original USA release (v1.0): recognised to say so clearly. */
    const val ORIGINAL_XEX_SHA256_PREFIX = "a7008a47c69d5620"
    val PACKS = listOf("data/X360/st_pack_x360.dir")
}

data class InstallState(
    val installed: Boolean,
    val xexOk: Boolean,
    val packsOk: Boolean,
    val sizeBytes: Long,
    val fileCount: Int,
) {
    val playable get() = installed && xexOk && packsOk

    companion object {
        fun scan(): InstallState {
            val game = AppPaths.game
            val xex = File(game, "default.xex")
            if (!xex.isFile) return InstallState(false, false, false, 0, 0)
            var size = 0L
            var count = 0
            game.walkTopDown().filter { it.isFile }.forEach { size += it.length(); count++ }
            val packs = GameDisc.PACKS.all { File(game, it).isFile } &&
                (File(game, "data/X360").listFiles()?.count { it.name.startsWith("st_pack_") && it.name.endsWith(".afs") } ?: 0) >= 6
            return InstallState(true, xex.length() == GameDisc.XEX_SIZE, packs, size, count)
        }
    }
}

/** Why an ISO was refused: each has its own message in the three languages. */
class IsoFailure(val reason: Reason, val detail: String = "") : Exception(detail) {
    enum class Reason {
        OPEN, NOT_XBOX, INCOMPLETE, NO_XEX, NO_SPACE, ZEROED, WRONG_VERSION, ORIGINAL_RELEASE, DAMAGED_TABLE, MOVE, IO,
    }
}

/**
 * Installs the game from the player's own disc image (port of RB2-Android's IsoInstaller): reads the XDVDFS
 * file system straight out of the picked document (plain XISO or a full XGD2 / XGD3 image), extracts every
 * file into game/ and checks it before replacing anything: default.xex must be the supported release and no
 * file may be cut short or zero-filled. Nothing is downloaded; the image never leaves the device.
 */
object IsoInstaller {
    private const val SECTOR = 2048
    private val PARTITIONS = longArrayOf(0L, 0x2080000L, 0xFD90000L, 0x18300000L)
    private val MAGIC = "MICROSOFT*XBOX*MEDIA".toByteArray(Charsets.US_ASCII)
    private const val SKIP_DIR = "\$SystemUpdate"

    private class Entry(val path: String, val sector: Long, val size: Long)

    /** Runs on a worker thread. Returns (files, bytes). */
    fun install(context: Context, uri: Uri, progress: (done: Long, total: Long, file: String) -> Unit): Pair<Int, Long> {
        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            null
        } ?: throw IsoFailure(IsoFailure.Reason.OPEN)
        pfd.use {
            FileInputStream(it.fileDescriptor).use { input ->
                try {
                    return extract(input.channel, AppPaths.game, progress)
                } catch (e: IOException) {
                    throw IsoFailure(IsoFailure.Reason.IO, e.message ?: "")
                }
            }
        }
    }

    private fun extract(ch: FileChannel, dest: File, progress: (Long, Long, String) -> Unit): Pair<Int, Long> {
        val imageSize = ch.size()
        var base = -1L
        var rootSector = 0L
        var rootSize = 0L
        for (off in PARTITIONS) {
            val b = read(ch, off + 32L * SECTOR, 28) ?: continue
            val m = ByteArray(MAGIC.size)
            b.get(m)
            if (m.contentEquals(MAGIC)) {
                base = off
                rootSector = b.int.toLong() and 0xFFFFFFFFL
                rootSize = b.int.toLong() and 0xFFFFFFFFL
                break
            }
        }
        if (base < 0) throw IsoFailure(IsoFailure.Reason.NOT_XBOX)

        val files = ArrayList<Entry>()
        val dirs = ArrayList<String>()
        walk(ch, base, rootSector, rootSize, "", files, dirs, 0)
        var total = 0L
        var hasXex = false
        for (e in files) {
            total += e.size
            if (base + e.sector * SECTOR + e.size > imageSize) throw IsoFailure(IsoFailure.Reason.INCOMPLETE, e.path)
            if (e.path == "default.xex") hasXex = true
        }
        if (!hasXex) throw IsoFailure(IsoFailure.Reason.NO_XEX)

        val parent = dest.parentFile!!
        parent.mkdirs()
        val free = parent.usableSpace
        if (free < total + (256L shl 20)) {
            throw IsoFailure(IsoFailure.Reason.NO_SPACE, String.format(java.util.Locale.ROOT, "%.1f|%.1f", total / 1e9, free / 1e9))
        }

        val tmp = File(dest.path + ".installing")
        tmp.deleteRecursively()
        tmp.mkdirs()
        dirs.forEach { File(tmp, it).mkdirs() }

        var xexSha: String? = null
        var done = 0L
        val buf = ByteBuffer.allocateDirect(4 shl 20)
        val heap = ByteArray(4 shl 20)
        try {
            for (e in files) {
                val out = File(tmp, e.path)
                val sha = if (e.path == "default.xex") MessageDigest.getInstance("SHA-256") else null
                var headZero = true
                var headChecked = false
                var tail = ByteArray(0)
                FileOutputStream(out).use { os ->
                    var pos = base + e.sector * SECTOR
                    var left = e.size
                    while (left > 0) {
                        buf.clear()
                        buf.limit(minOf(buf.capacity().toLong(), left).toInt())
                        val n = ch.read(buf, pos)
                        if (n <= 0) throw IsoFailure(IsoFailure.Reason.INCOMPLETE, e.path)
                        buf.flip()
                        buf.get(heap, 0, n)
                        os.write(heap, 0, n)
                        sha?.update(heap, 0, n)
                        if (!headChecked) {
                            headZero = allZero(heap, 0, minOf(n, SECTOR))
                            headChecked = true
                        }
                        if (left - n < SECTOR) {
                            val keep = minOf(n, SECTOR)
                            tail = heap.copyOfRange(n - keep, n)
                        }
                        pos += n
                        left -= n
                        done += n
                        progress(done, total, e.path)
                    }
                }
                // A copy that stopped part-way leaves zeros where data should be.
                if (e.size >= 65536 && (headZero || (e.size >= (16L shl 20) && allZero(tail, 0, tail.size)))) {
                    throw IsoFailure(IsoFailure.Reason.ZEROED, e.path)
                }
                if (sha != null) xexSha = sha.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (f: Exception) {
            tmp.deleteRecursively()
            throw f
        }

        val got = xexSha ?: ""
        if (got !in GameDisc.ACCEPTED_XEX_SHA256) {
            tmp.deleteRecursively()
            throw IsoFailure(
                if (got.startsWith(GameDisc.ORIGINAL_XEX_SHA256_PREFIX)) IsoFailure.Reason.ORIGINAL_RELEASE else IsoFailure.Reason.WRONG_VERSION,
                got,
            )
        }

        // Swap by renaming, so a failure never leaves the device without a working install.
        val old = File(dest.path + ".old")
        old.deleteRecursively()
        if (dest.exists() && !dest.renameTo(old)) {
            tmp.deleteRecursively()
            throw IsoFailure(IsoFailure.Reason.MOVE)
        }
        if (!tmp.renameTo(dest)) {
            if (old.exists()) old.renameTo(dest)
            tmp.deleteRecursively()
            throw IsoFailure(IsoFailure.Reason.MOVE)
        }
        old.deleteRecursively()
        return files.size to total
    }

    private fun walk(ch: FileChannel, base: Long, sector: Long, size: Long, path: String, files: MutableList<Entry>, dirs: MutableList<String>, depth: Int) {
        if (size == 0L) return
        if (depth > 32 || size > (64L shl 20)) throw IsoFailure(IsoFailure.Reason.DAMAGED_TABLE)
        val table = read(ch, base + sector * SECTOR, size.toInt()) ?: throw IsoFailure(IsoFailure.Reason.INCOMPLETE, path)
        node(ch, base, table, 0, path, files, dirs, depth, HashSet())
    }

    private fun node(ch: FileChannel, base: Long, t: ByteBuffer, o: Int, path: String, files: MutableList<Entry>, dirs: MutableList<String>, depth: Int, seen: MutableSet<Int>) {
        if (o in seen || o + 14 > t.limit()) return
        seen += o
        val left = t.getShort(o).toInt() and 0xFFFF
        val right = t.getShort(o + 2).toInt() and 0xFFFF
        val start = t.getInt(o + 4).toLong() and 0xFFFFFFFFL
        val length = t.getInt(o + 8).toLong() and 0xFFFFFFFFL
        val attr = t.get(o + 12).toInt() and 0xFF
        val nlen = t.get(o + 13).toInt() and 0xFF
        if (left == 0xFFFF && right == 0xFFFF && start == 0xFFFFFFFFL) return
        if (o + 14 + nlen > t.limit()) return
        val name = String(ByteArray(nlen) { t.get(o + 14 + it) }, Charsets.ISO_8859_1)
        if ('/' in name || '\\' in name || name == ".." || name == ".") throw IsoFailure(IsoFailure.Reason.DAMAGED_TABLE)
        if (left != 0) node(ch, base, t, left * 4, path, files, dirs, depth, seen)
        val rel = if (path.isEmpty()) name else "$path/$name"
        if (attr and 0x10 != 0) {
            if (!(path.isEmpty() && name.equals(SKIP_DIR, true))) {
                dirs += rel
                walk(ch, base, start, length, rel, files, dirs, depth + 1)
            }
        } else {
            files += Entry(rel, start, length)
        }
        if (right != 0) node(ch, base, t, right * 4, path, files, dirs, depth, seen)
    }

    private fun read(ch: FileChannel, pos: Long, len: Int): ByteBuffer? {
        val b = ByteBuffer.allocate(len).order(ByteOrder.LITTLE_ENDIAN)
        while (b.hasRemaining()) {
            if (ch.read(b, pos + b.position()) <= 0) return null
        }
        b.flip()
        return b
    }

    private fun allZero(b: ByteArray, from: Int, to: Int): Boolean {
        for (i in from until to) if (b[i].toInt() != 0) return false
        return true
    }
}
