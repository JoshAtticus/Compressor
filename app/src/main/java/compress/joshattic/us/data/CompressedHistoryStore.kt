package compress.joshattic.us.data

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists MediaStore IDs that have been successfully compressed in Free space,
 * so the library list can mark them even after the queue is cleared.
 */
object CompressedHistoryStore {
    private const val PREF_KEY = "optimizer_compressed_history_v1"

    fun load(prefs: SharedPreferences): Set<Long> {
        val raw = prefs.getString(PREF_KEY, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    fun save(prefs: SharedPreferences, ids: Set<Long>) {
        prefs.edit {
            putString(PREF_KEY, ids.joinToString(","))
        }
    }

    fun add(prefs: SharedPreferences, vararg mediaIds: Long): Set<Long> {
        val next = load(prefs) + mediaIds.filter { it > 0L }
        save(prefs, next)
        return next
    }
}
