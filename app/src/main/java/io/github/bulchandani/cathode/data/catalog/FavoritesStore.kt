package io.github.bulchandani.cathode.data.catalog

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class ContentKind { Live, Movie, Series }

data class FavoriteItem(
    val kind: ContentKind,
    val id: Int,
    val name: String,
)

data class RecentItem(
    val kind: ContentKind,
    val id: Int,
    val name: String,
    val streamUrl: String,
    val lastPlayedAt: Long,
)

/**
 * Process-wide reactive favorites store. Read [items] inside any
 * Composable; calls to [toggle] from anywhere in the app trigger
 * recomposition of every reader. Persists to SharedPreferences.
 */
object FavoritesRepo {
    private val _items = mutableStateOf<List<FavoriteItem>>(emptyList())
    val items: State<List<FavoriteItem>> = _items

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("cathode_favorites", Context.MODE_PRIVATE)
        _items.value = readFromPrefs()
    }

    fun isFavorite(kind: ContentKind, id: Int): Boolean =
        _items.value.any { it.kind == kind && it.id == id }

    /** Returns true if the item is now pinned, false if it was un-pinned. */
    fun toggle(item: FavoriteItem): Boolean {
        val list = _items.value.toMutableList()
        val idx = list.indexOfFirst { it.kind == item.kind && it.id == item.id }
        val pinned: Boolean
        if (idx >= 0) {
            list.removeAt(idx)
            pinned = false
        } else {
            list.add(item)
            pinned = true
        }
        _items.value = list
        writeToPrefs(list)
        return pinned
    }

    private fun readFromPrefs(): List<FavoriteItem> {
        val arr = JSONArray(prefs?.getString(KEY, "[]") ?: "[]")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            FavoriteItem(
                kind = ContentKind.valueOf(o.getString("kind")),
                id = o.getInt("id"),
                name = o.optString("name", ""),
            )
        }
    }

    private fun writeToPrefs(list: List<FavoriteItem>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(JSONObject().apply {
                put("kind", f.kind.name)
                put("id", f.id)
                put("name", f.name)
            })
        }
        prefs?.edit { putString(KEY, arr.toString()) }
    }

    private const val KEY = "list"
}

/** Old class shim — kept so existing callers compile while we migrate. */
@Deprecated("Use FavoritesRepo singleton")
class FavoritesStore(context: Context) {
    init { FavoritesRepo.init(context) }
    fun all(): List<FavoriteItem> = FavoritesRepo.items.value
    fun isFavorite(kind: ContentKind, id: Int): Boolean = FavoritesRepo.isFavorite(kind, id)
    fun toggle(item: FavoriteItem) { FavoritesRepo.toggle(item) }
}

class RecentsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("cathode_recents", Context.MODE_PRIVATE)
    private val MAX = 50

    fun all(): List<RecentItem> {
        val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RecentItem(
                kind = ContentKind.valueOf(o.getString("kind")),
                id = o.getInt("id"),
                name = o.optString("name", ""),
                streamUrl = o.optString("url", ""),
                lastPlayedAt = o.optLong("at", 0L),
            )
        }.sortedByDescending { it.lastPlayedAt }
    }

    fun touch(item: RecentItem) {
        val current = all().filterNot { it.kind == item.kind && it.id == item.id }.toMutableList()
        current.add(0, item)
        if (current.size > MAX) current.subList(MAX, current.size).clear()
        val arr = JSONArray()
        current.forEach { r ->
            arr.put(JSONObject().apply {
                put("kind", r.kind.name)
                put("id", r.id)
                put("name", r.name)
                put("url", r.streamUrl)
                put("at", r.lastPlayedAt)
            })
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    companion object { private const val KEY = "list" }
}
