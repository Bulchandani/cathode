package io.github.bulchandani.cathode.data.m3u

import android.content.Context
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Maps an Xtream Codes `stream_id` to the **actual** stream URL the
 * provider exposes via `/get.php?...&type=m3u_plus`. This is the
 * source of truth for live URLs — every other player uses it; we
 * used to *construct* URLs by guessing scheme/port/path, which is
 * exactly what was 405-ing on this user's provider (HTTPS:443 in
 * the M3U vs HTTP:80 in our build).
 *
 * Cache: in-memory + on-disk JSON, 6h TTL. Same lifecycle pattern
 * as EpgRepo: hydrate from disk on init, refresh by [load], wipe by
 * [invalidate] on source switch.
 */
object M3uIndex {

    private const val CACHE_TTL_MS = 6L * 60 * 60 * 1000
    private val streamIdRegex = Regex("/(\\d+)\\.[^./?]+(?:\\?.*)?$")

    @Volatile private var idToUrl: Map<Int, String> = emptyMap()
    @Volatile private var lastFetch: Long = 0L
    @Volatile private var lastError: String? = null
    @Volatile private var appContext: Context? = null
    private val refreshLock = Mutex()

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        appContext = context.applicationContext
        if (idToUrl.isNotEmpty()) return@withContext
        val file = File(context.applicationContext.cacheDir, FILE_NAME)
        if (!file.exists()) return@withContext
        try {
            val obj = JSONObject(file.readText())
            val ts = obj.optLong("at", 0)
            if (System.currentTimeMillis() - ts < CACHE_TTL_MS) {
                val map = obj.getJSONObject("map")
                idToUrl = buildMap {
                    map.keys().forEach { k ->
                        val id = k.toIntOrNull() ?: return@forEach
                        put(id, map.getString(k))
                    }
                }
                lastFetch = ts
            }
        } catch (t: Throwable) {
            lastError = "Disk hydrate: ${t.message}"
        }
    }

    suspend fun load(host: String, user: String, pass: String, force: Boolean = false) {
        if (!force && idToUrl.isNotEmpty() &&
            System.currentTimeMillis() - lastFetch < CACHE_TTL_MS
        ) return
        refreshLock.withLock {
            if (!force && idToUrl.isNotEmpty() &&
                System.currentTimeMillis() - lastFetch < CACHE_TTL_MS
            ) return
            try {
                val raw = XtreamApi.fetchM3uPlus(host, user, pass)
                val channels = M3uParser.parse(raw.byteInputStream())
                idToUrl = channels.mapNotNull { ch ->
                    val match = streamIdRegex.find(ch.url) ?: return@mapNotNull null
                    val id = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    id to ch.url
                }.toMap()
                lastFetch = System.currentTimeMillis()
                lastError = null
                writeDiskCache()
            } catch (t: Throwable) {
                lastError = t.message ?: t::class.simpleName
            }
        }
    }

    fun urlFor(streamId: Int): String? = idToUrl[streamId]
    fun isReady(): Boolean = idToUrl.isNotEmpty()
    fun lastFetchAt(): Long = lastFetch
    fun lastErrorMessage(): String? = lastError
    fun size(): Int = idToUrl.size

    fun invalidate() {
        idToUrl = emptyMap()
        lastFetch = 0L
        lastError = null
        appContext?.let {
            runCatching { File(it.cacheDir, FILE_NAME).delete() }
        }
    }

    private suspend fun writeDiskCache() = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext
        try {
            val obj = JSONObject()
            obj.put("at", lastFetch)
            val mapObj = JSONObject()
            idToUrl.forEach { (k, v) -> mapObj.put(k.toString(), v) }
            obj.put("map", mapObj)
            File(ctx.cacheDir, FILE_NAME).writeText(obj.toString())
        } catch (_: Throwable) { /* best-effort */ }
    }

    private const val FILE_NAME = "m3u_index.json"
}
