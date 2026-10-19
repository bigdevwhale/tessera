package app.tessera.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The whole wall in one ZIP: tessera.json with every person, question and answer, and the media folder
 * beside it. Years of video are big; that's why it is a file you put somewhere yourself. Nothing leaves
 * the phone otherwise.
 */
object Backup {
    private const val VERSION = 1
    private const val DATA = "tessera.json"
    private const val MEDIA = "media/"

    fun encode(all: Everything): String = JSONObject()
        .put("app", "tessera")
        .put("version", VERSION)
        .put("people", JSONArray(all.people.map {
            JSONObject().put("id", it.id).put("name", it.name).put("birth", it.birth).put("band", it.band).put("glass", it.glass)
                .put("order", it.order).put("createdAt", it.createdAt)
        }))
        .put("questions", JSONArray(all.questions.map {
            JSONObject().put("id", it.id).put("personId", it.personId).put("key", it.key).put("text", it.text).put("cadence", it.cadence)
                .put("format", it.format).put("glass", it.glass).put("order", it.order).put("archived", it.archived).put("createdAt", it.createdAt)
        }))
        .put("answers", JSONArray(all.answers.map {
            JSONObject().put("id", it.id).put("personId", it.personId).put("questionId", it.questionId).put("day", it.day).put("at", it.at)
                .put("kind", it.kind).put("text", it.text).put("file", it.file).put("thumb", it.thumb).put("durationMs", it.durationMs)
                .put("asked", it.asked).put("starred", it.starred).put("front", it.front)
        }))
        .toString(1)

    private fun JSONObject.list(key: String) = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()

    /** Throws [IllegalArgumentException] when the text is not a Tessera backup. */
    fun decode(text: String): Everything {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "tessera") { "Not a Tessera backup" }
        return Everything(
            people = root.list("people").map {
                Person(
                    id = it.getLong("id"), name = it.optString("name"), birth = it.optString("birth"), band = it.optString("band", "ADULT"),
                    glass = it.optInt("glass"), order = it.optInt("order"), createdAt = it.optLong("createdAt"),
                )
            },
            questions = root.list("questions").map {
                Question(
                    id = it.getLong("id"), personId = it.getLong("personId"), key = it.optString("key"), text = it.optString("text"),
                    cadence = it.optString("cadence", "DAILY"), format = it.optString("format", "TEXT"), glass = it.optInt("glass"),
                    order = it.optInt("order"), archived = it.optBoolean("archived"), createdAt = it.optLong("createdAt"),
                )
            },
            answers = root.list("answers").map {
                Answer(
                    id = it.getLong("id"), personId = it.getLong("personId"), questionId = it.getLong("questionId"), day = it.getLong("day"),
                    at = it.optLong("at"), kind = it.optString("kind", "TEXT"), text = it.optString("text"), file = it.optString("file"),
                    thumb = it.optString("thumb"), durationMs = it.optLong("durationMs"), asked = it.optString("asked"),
                    starred = it.optBoolean("starred"), front = it.optBoolean("front", true),
                )
            },
        )
    }

    fun write(context: Context, uri: Uri, all: Everything) {
        val out = context.contentResolver.openOutputStream(uri) ?: error("Can't write there")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA))
            zip.write(encode(all).toByteArray())
            zip.closeEntry()
            all.answers.flatMap { it.files }.distinct().forEach { name ->
                val f = Media.file(context, name)
                if (!f.exists()) return@forEach
                zip.putNextEntry(ZipEntry(MEDIA + name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Reads a backup, putting its media in place. The caller swaps the database over. */
    fun read(context: Context, uri: Uri): Everything {
        var data: String? = null
        val input = context.contentResolver.openInputStream(uri) ?: error("Can't read that")
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                when {
                    e.name == DATA -> data = zip.readBytes().decodeToString()
                    e.name.startsWith(MEDIA) && !e.isDirectory -> {
                        // Only plain names: an entry must never write outside the media folder.
                        val name = File(e.name.removePrefix(MEDIA)).name
                        if (name.isNotEmpty() && !name.startsWith(".")) Media.file(context, name).outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry()
            }
        }
        return decode(data ?: throw IllegalArgumentException("Not a Tessera backup"))
    }
}
