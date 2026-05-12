package io.github.bulchandani.cathode.data.m3u

import android.content.Context
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Maps an Xtream Codes `stream_id` to the **actual** stream URL the
 * provider exposes via `/get.php?...&type=m3u_plus`. Source of truth
 * for live URLs (we used to *construct* URLs by guessing scheme/port/
 * path/extension, which 405-ed on some providers).
 *
 * Memory-safe for huge playlists:
 * - Streams the HTTP body straight into [M3uParser.parseStreaming]
 *   (no big response String).
 * - Disk cache is line-based TSV (`id\turl\n`) — no JSONObject tree.
 *
 * Cache: in-memory + on-disk TSV, 6h TTL. Hydrate from disk on [init],
 * refresh by [load], wipe by [invalidate] on source switch.
 */
object M3uIndex {

    private const val CACHE_TTL_MS = 6L * 60 * 60 * 1000
    private const val FILE_NAME = "m3u_index.tsv"
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
            val newMap = mutableMapOf<Int, String>()
            var ts = 0L
            file.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("at=")) {
                        ts = line.substring(3).toLongOrNull() ?: 0L
                        continue
                    }
                    val sep = line.indexOf('\t')
                    if (sep < 0) continue
                    val id = line.substring(0, sep).toIntOrNull() ?: continue
                    newMap[id] = line.substring(sep + 1)
                }
            }
            if (System.currentTimeMillis() - ts < CACHE_TTL_MS) {
                idToUrl = newMap
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
                val newMap = HashMap<Int, String>(8192)
                XtreamApi.streamM3uPlus(host, user, pass) { stream ->
                    // Block until parseStreaming finishes — we're already on an IO dispatcher
                    // inside streamM3uPlus's withContext; parseStreaming switches to Default
                    // internally for its line loop.
                    kotlinx.coroutines.runBlocking {
                        M3uParser.parseStreaming(stream) { ch ->
                            val match = streamIdRegex.find(ch.url) ?: return@parseStreaming
                            val id = match.groupValues[1].toIntOrNull() ?: return@parseStreaming
                            // Strip default `:443` / `:80` so Host header stays bare —
                            // some Xtream WAFs 405 explicit-port Host values.
                            newMap[id] = XtreamApi.stripDefaultPort(ch.url)
                        }
                    }
                }
                idToUrl = newMap
                lastFetch = System.currentTimeMillis()
                lastError = if (newMap.isEmpty()) {
                    "Playlist parsed 0 channels — response may not be M3U format"
                } else null
                writeDiskCache()
            } catch (oom: OutOfMemoryError) {
                // Playlist too big for heap. Don't crash the app — caller falls back to
                // constructed URLs (which now use .ts and work on this provider).
                idToUrl = emptyMap()
                lastError = "Out of memory: playlist too large (${oom.message ?: "no detail"}). " +
                    "Using constructed URLs instead."
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
            // Streamed TSV write — no big JSON tree, low peak memory.
            File(ctx.cacheDir, FILE_NAME).bufferedWriter().use { w ->
                w.write("at=$lastFetch")
                w.newLine()
                idToUrl.forEach { (id, url) ->
                    w.write(id.toString())
                    w.write("\t")
                    w.write(url)
                    w.newLine()
                }
            }
        } catch (_: Throwable) { /* best-effort */ }
    }
}
