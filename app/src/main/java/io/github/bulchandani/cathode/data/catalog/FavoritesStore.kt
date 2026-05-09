package io.github.bulchandani.cathode.data.catalog

import android.content.Context
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

class FavoritesStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("cathode_favorites", Context.MODE_PRIVATE)

    fun all(): List<FavoriteItem> {
        val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            FavoriteItem(
                kind = ContentKind.valueOf(o.getString("kind")),
                id = o.getInt("id"),
                name = o.optString("name", ""),
            )
        }
    }

    fun isFavorite(kind: ContentKind, id: Int): Boolean =
        all().any { it.kind == kind && it.id == id }

    fun toggle(item: FavoriteItem) {
        val current = all().toMutableList()
        val existing = current.indexOfFirst { it.kind == item.kind && it.id == item.id }
        if (existing >= 0) current.removeAt(existing) else current.add(item)
        save(current)
    }

    private fun save(list: List<FavoriteItem>) {
        val arr = JSONArray()
        list.forEach { f ->
            arr.put(JSONObject().apply {
                put("kind", f.kind.name)
                put("id", f.id)
                put("name", f.name)
            })
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    companion object { private const val KEY = "list" }
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
