package app.tessera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.LruCache
import java.io.File
import java.util.UUID
import kotlin.math.max

/** Videos, voice notes and tile frames live as plain files in the app's private folder; the database keeps their names. */
object Media {
    /** Long side of a stored frame: enough for the ghost over the camera and a tile up close. */
    private const val FRAME_SIDE = 720

    fun dir(context: Context): File = File(context.filesDir, "media").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun newName(ext: String): String = UUID.randomUUID().toString().replace("-", "") + "." + ext

    fun delete(context: Context, names: List<String>) {
        names.filter { it.isNotEmpty() }.forEach {
            file(context, it).delete()
            cache.remove(it)
        }
    }

    /** Files no answer points to any more (a recording abandoned halfway, a crash mid-save). */
    fun sweep(context: Context, keep: Set<String>, olderThan: Long) {
        dir(context).listFiles()?.forEach { f ->
            if (f.name !in keep && f.lastModified() < olderThan) f.delete()
        }
    }

    /**
     * The frame a video's tile shows, taken a third of the way in (past the first blink), upright, saved as a
     * JPEG. Returns the file name and the video's length, or null when the file can't be read.
     */
    fun frame(context: Context, video: String): Pair<String, Long>? = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file(context, video).absolutePath)
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val raw = r.getFrameAtTime(ms * 1000 / 3, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: raw.width
            // Some phones hand the frame back as stored, sideways; others already turned. Turn only the sideways ones.
            val sideways = rot % 180 != 0 && raw.width == w && raw.width != raw.height
            val m = Matrix()
            if (sideways || rot == 180) m.postRotate(rot.toFloat())
            val k = FRAME_SIDE.toFloat() / max(raw.width, raw.height)
            if (k < 1f) m.postScale(k, k)
            val out = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
            val name = newName("jpg")
            file(context, name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 86, it) }
            name to ms
        } finally {
            r.release()
        }
    }.getOrNull()

    fun length(context: Context, name: String): Long = runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file(context, name).absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            r.release()
        }
    }.getOrDefault(0L)

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** A frame decoded no larger than about [side] pixels, cached: the panel draws hundreds of them small. */
    fun bitmap(context: Context, name: String, side: Int): Bitmap? {
        if (name.isEmpty()) return null
        val key = "$name@$side"
        cache.get(key)?.let { return it }
        val f = file(context, name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= side) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        cache.put(key, bmp)
        return bmp
    }
}
