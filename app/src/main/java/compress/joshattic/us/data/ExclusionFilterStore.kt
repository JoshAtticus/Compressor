package compress.joshattic.us.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray

/**
 * Persists Free space hide filters (file IDs + folder keys) across app restarts.
 */
object ExclusionFilterStore {
    private const val PREF_IDS = "optimizer_excluded_ids_v1"
    private const val PREF_FOLDERS = "optimizer_excluded_folders_v1"

    fun loadIds(prefs: SharedPreferences): Set<Long> {
        val raw = prefs.getString(PREF_IDS, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    fun loadFolders(prefs: SharedPreferences): Set<String> {
        val raw = prefs.getString(PREF_FOLDERS, null) ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            buildSet {
                for (i in 0 until array.length()) {
                    val value = array.optString(i, "")
                    if (value.isNotBlank()) add(value)
                }
            }
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun save(prefs: SharedPreferences, ids: Set<Long>, folders: Set<String>) {
        val folderJson = JSONArray().apply {
            folders.filter { it.isNotBlank() }.forEach { put(it) }
        }
        prefs.edit {
            putString(PREF_IDS, ids.filter { it > 0L }.joinToString(","))
            putString(PREF_FOLDERS, folderJson.toString())
        }
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit {
            remove(PREF_IDS)
            remove(PREF_FOLDERS)
        }
    }
}
