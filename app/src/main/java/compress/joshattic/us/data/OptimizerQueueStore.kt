package compress.joshattic.us.data

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.MediaLibraryItem
import compress.joshattic.us.model.OptimizerQueueItem
import compress.joshattic.us.model.QueueItemStatus
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object OptimizerQueueStore {
    private const val PREF_KEY = "optimizer_queue_v1"

    fun save(prefs: SharedPreferences, queue: List<OptimizerQueueItem>) {
        val array = JSONArray()
        queue.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("status", item.status.name)
                    put("progress", item.progress.toDouble())
                    put("outputUri", item.outputUri?.toString())
                    put("outputSizeBytes", item.outputSizeBytes)
                    put("outputWidth", item.outputWidth)
                    put("outputHeight", item.outputHeight)
                    put("outputFps", item.outputFps.toDouble())
                    put("errorMessage", item.errorMessage)
                    put("originalDeleted", item.originalDeleted)
                    put("reviewed", item.reviewed)
                    put("media", mediaToJson(item.media))
                }
            )
        }
        prefs.edit { putString(PREF_KEY, array.toString()) }
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit { remove(PREF_KEY) }
    }

    fun load(prefs: SharedPreferences): List<OptimizerQueueItem> {
        val raw = prefs.getString(PREF_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val media = mediaFromJson(obj.getJSONObject("media")) ?: continue
                    var status = runCatching {
                        QueueItemStatus.valueOf(obj.getString("status"))
                    }.getOrDefault(QueueItemStatus.CANCELLED)
                    // Interrupted work cannot resume mid-encode.
                    if (status == QueueItemStatus.QUEUED || status == QueueItemStatus.RUNNING) {
                        status = QueueItemStatus.CANCELLED
                    }
                    val outputUri = obj.optString("outputUri", null)
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?.let { Uri.parse(it) }
                    val outputExists = when {
                        outputUri == null -> false
                        outputUri.scheme == "file" -> outputUri.path?.let { File(it).exists() } == true
                        else -> true
                    }
                    val resolvedStatus = if (
                        status == QueueItemStatus.DONE && !outputExists
                    ) {
                        QueueItemStatus.CANCELLED
                    } else {
                        status
                    }
                    add(
                        OptimizerQueueItem(
                            id = obj.getString("id"),
                            media = media,
                            status = resolvedStatus,
                            progress = obj.optDouble("progress", 0.0).toFloat(),
                            outputUri = outputUri.takeIf {
                                resolvedStatus == QueueItemStatus.DONE && outputExists
                            },
                            outputSizeBytes = if (resolvedStatus == QueueItemStatus.DONE && outputExists) {
                                obj.optLong("outputSizeBytes", 0L)
                            } else {
                                0L
                            },
                            outputWidth = obj.optInt("outputWidth", 0),
                            outputHeight = obj.optInt("outputHeight", 0),
                            outputFps = obj.optDouble("outputFps", 0.0).toFloat(),
                            errorMessage = obj.optString("errorMessage", null)
                                ?.takeIf { it.isNotBlank() && it != "null" },
                            originalDeleted = obj.optBoolean("originalDeleted", false),
                            reviewed = obj.optBoolean("reviewed", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mediaToJson(media: MediaLibraryItem): JSONObject {
        return JSONObject().apply {
            put("id", media.id)
            put("uri", media.uri.toString())
            put("kind", media.kind.name)
            put("displayName", media.displayName)
            put("sizeBytes", media.sizeBytes)
            put("width", media.width)
            put("height", media.height)
            put("fps", media.fps.toDouble())
            put("durationMs", media.durationMs)
            put("dateAddedSeconds", media.dateAddedSeconds)
            put("folderKey", media.folderKey)
            put("folderLabel", media.folderLabel)
            put("estimatedOutputBytes", media.estimatedOutputBytes)
            put("estimatedSavingsBytes", media.estimatedSavingsBytes)
        }
    }

    private fun mediaFromJson(obj: JSONObject): MediaLibraryItem? {
        return try {
            MediaLibraryItem(
                id = obj.getLong("id"),
                uri = Uri.parse(obj.getString("uri")),
                kind = MediaKind.valueOf(obj.getString("kind")),
                displayName = obj.getString("displayName"),
                sizeBytes = obj.getLong("sizeBytes"),
                width = obj.optInt("width", 0),
                height = obj.optInt("height", 0),
                fps = obj.optDouble("fps", 0.0).toFloat(),
                durationMs = obj.optLong("durationMs", 0L),
                dateAddedSeconds = obj.optLong("dateAddedSeconds", 0L),
                folderKey = obj.optString("folderKey", ""),
                folderLabel = obj.optString("folderLabel", ""),
                estimatedOutputBytes = obj.optLong("estimatedOutputBytes", obj.optLong("sizeBytes", 0L)),
                estimatedSavingsBytes = obj.optLong("estimatedSavingsBytes", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}
