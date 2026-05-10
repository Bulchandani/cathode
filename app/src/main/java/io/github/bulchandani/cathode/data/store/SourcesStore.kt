package io.github.bulchandani.cathode.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class Source(
    val id: String,
    val label: String,
    val host: String,
    val user: String,
    val pass: String,
)

/**
 * Process-wide reactive multi-source store. Replaces the single-creds
 * model from v0.6.0. CredsStore is migrated into the first Source on
 * first init.
 */
object SourcesStore {
    private val _sources = mutableStateOf<List<Source>>(emptyList())
    val sources: State<List<Source>> = _sources

    private val _activeId = mutableStateOf<String?>(null)
    val active: State<Source?> = derivedStateOf {
        val id = _activeId.value
        _sources.value.firstOrNull { it.id == id }
    }

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("cathode_sources", Context.MODE_PRIVATE)
        load()

        // One-time migration from the v0.6.0 single-creds CredsStore.
        if (_sources.value.isEmpty()) {
            val old = CredsStore(context)
            if (old.hasCreds()) {
                add(
                    Source(
                        id = "src-1",
                        label = old.host.removePrefix("http://").removePrefix("https://").take(40),
                        host = old.host,
                        user = old.user,
                        pass = old.pass,
                    ),
                )
            }
        }
    }

    fun add(source: Source) {
        _sources.value = _sources.value + source
        if (_activeId.value == null) _activeId.value = source.id
        save()
    }

    fun update(id: String, label: String) {
        _sources.value = _sources.value.map { if (it.id == id) it.copy(label = label) else it }
        save()
    }

    fun setActive(id: String) {
        if (_activeId.value == id) return
        _activeId.value = id
        // Switching providers means stale catalogs/EPG/M3U index; wipe caches.
        io.github.bulchandani.cathode.data.catalog.CatalogRepo.invalidate()
        io.github.bulchandani.cathode.data.epg.EpgRepo.invalidate()
        io.github.bulchandani.cathode.data.m3u.M3uIndex.invalidate()
        save()
    }

    fun delete(id: String) {
        _sources.value = _sources.value.filter { it.id != id }
        if (_activeId.value == id) _activeId.value = _sources.value.firstOrNull()?.id
        save()
    }

    fun nextId(): String = "src-${(_sources.value.maxOfOrNull { it.id.removePrefix("src-").toIntOrNull() ?: 0 } ?: 0) + 1}"

    private fun load() {
        val raw = prefs?.getString(KEY_SOURCES, null) ?: return
        val arr = JSONArray(raw)
        _sources.value = List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Source(
                id = o.getString("id"),
                label = o.optString("label", ""),
                host = o.getString("host"),
                user = o.getString("user"),
                pass = o.getString("pass"),
            )
        }
        _activeId.value = prefs?.getString(KEY_ACTIVE, null) ?: _sources.value.firstOrNull()?.id
    }

    private fun save() {
        val arr = JSONArray()
        _sources.value.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("label", s.label); put("host", s.host); put("user", s.user); put("pass", s.pass)
            })
        }
        prefs?.edit {
            putString(KEY_SOURCES, arr.toString())
            putString(KEY_ACTIVE, _activeId.value)
        }
    }

    private const val KEY_SOURCES = "sources"
    private const val KEY_ACTIVE = "active"
}
