package io.github.bulchandani.cathode.data.m3u

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Maps Xtream Codes `stream_id` → actual stream URL pulled from the
 * provider's `/get.php?type=m3u_plus` playlist.
 *
 * Critically: the fetch + parse run on a **process-scoped** coroutine
 * (`ioScope`), not a composition-scoped LaunchedEffect. Live TV's
 * LaunchedEffect was being cancelled by recomposition before the
 * (multi-minute, multi-MB) load could finish, producing the
 * "coroutine scope left the composition" error and silent M3U: 0 state.
 * Now the load survives any UI lifecycle event.
 *
 * State is exposed as Compose [MutableState] so UI recomposes when
 * load progresses/completes without needing its own coroutine.
 */
object M3uIndex {

    private const val CACHE_TTL_MS = 6L * 60 * 60 * 1000
    private const val FILE_NAME = "m3u_index.tsv"
    private const val TAG = "M3U"
    private val streamIdRegex = Regex("/(\\d+)\\.[^./?]+(?:\\?.*)?$")

    /** Survives composition + Activity recreation. Backed by SupervisorJob so
     * one failure doesn't kill the scope for other future loads. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Compose-observable load state. UI just reads these. */
    val sizeState: MutableState<Int> = mutableStateOf(0)
    val errorState: MutableState<String?> = mutableStateOf(null)
    val loadingState: MutableState<Boolean> = mutableStateOf(false)
    val lastFetchAtState: MutableState<Long> = mutableStateOf(0L)

    @Volatile private var idToUrl: Map<Int, String> = emptyMap()
    @Volatile private var appContext: Context? = null
    @Volatile private var inflightJob: Job? = null
    private val refreshLock = Mutex()

    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        appContext = context.applicationContext
        if (idToUrl.isNotEmpty()) return@withContext
        val file = File(context.applicationContext.cacheDir, FILE_NAME)
        if (!file.exists()) {
            Logger.d(TAG, "init: no disk cache")
            return@withContext
        }
        try {
            val newMap = HashMap<Int, String>(8192)
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
                sizeState.value = newMap.size
                lastFetchAtState.value = ts
                Logger.i(TAG, "init: hydrated ${newMap.size} channels from disk cache")
            } else {
                Logger.d(TAG, "init: disk cache stale (age=${System.currentTimeMillis() - ts}ms)")
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "init: disk hydrate failed", t)
            errorState.value = "Disk hydrate: ${t.message}"
        }
    }

    /**
     * Trigger an M3U fetch. Returns immediately; the load runs on a
     * process-scope coroutine and updates [sizeState] / [errorState] /
     * [loadingState] as it progresses. Safe to call repeatedly — concurrent
     * calls coalesce on [refreshLock].
     */
    fun trigger(host: String, user: String, pass: String, force: Boolean = false) {
        if (host.isBlank() || user.isBlank() || pass.isBlank()) {
            Logger.w(TAG, "trigger: blank credentials, skipping")
            return
        }
        // If already loading, don't re-launch — the in-flight load will fulfill.
        val current = inflightJob
        if (current != null && current.isActive) {
            Logger.d(TAG, "trigger: load already in flight, skipping")
            return
        }
        inflightJob = ioScope.launch {
            loadInternal(host, user, pass, force)
        }
    }

    private suspend fun loadInternal(host: String, user: String, pass: String, force: Boolean) {
        if (!force && idToUrl.isNotEmpty() &&
            System.currentTimeMillis() - lastFetchAtState.value < CACHE_TTL_MS
        ) {
            Logger.d(TAG, "load: cache fresh (size=${idToUrl.size}), skipping")
            return
        }
        refreshLock.withLock {
            if (!force && idToUrl.isNotEmpty() &&
                System.currentTimeMillis() - lastFetchAtState.value < CACHE_TTL_MS
            ) return
            loadingState.value = true
            errorState.value = null
            Logger.i(TAG, "load: starting fetch from $host (force=$force)")
            val started = System.currentTimeMillis()
            try {
                val newMap = HashMap<Int, String>(8192)
                var lineCount = 0
                XtreamApi.streamM3uPlus(host, user, pass) { stream ->
                    runBlocking {
                        M3uParser.parseStreaming(stream) { ch ->
                            lineCount++
                            val match = streamIdRegex.find(ch.url) ?: return@parseStreaming
                            val id = match.groupValues[1].toIntOrNull() ?: return@parseStreaming
                            newMap[id] = XtreamApi.stripDefaultPort(ch.url)
                        }
                    }
                }
                val elapsed = System.currentTimeMillis() - started
                Logger.i(TAG, "load: parsed $lineCount EXTINF entries → ${newMap.size} indexed (${elapsed}ms)")
                idToUrl = newMap
                sizeState.value = newMap.size
                lastFetchAtState.value = System.currentTimeMillis()
                // Never null-out lastError when size==0 — caller relies on this.
                errorState.value = if (newMap.isEmpty()) {
                    "Playlist parsed $lineCount entries but indexed 0 — channel URLs may not match expected pattern (/<id>.<ext>)"
                } else null
                writeDiskCache()
            } catch (oom: OutOfMemoryError) {
                Logger.e(TAG, "load: OOM during parse: ${oom.message}")
                idToUrl = emptyMap()
                sizeState.value = 0
                errorState.value = "Out of memory: playlist too large for device heap. " +
                    "Using constructed URLs as fallback."
            } catch (t: Throwable) {
                Logger.e(TAG, "load: failed", t)
                // Even on partial failure, set a non-null error so UI can show it.
                val msg = t.message ?: t::class.simpleName ?: "Unknown error"
                errorState.value = msg
                // Don't clobber idToUrl on failure — keep whatever we had.
            } finally {
                loadingState.value = false
            }
        }
    }

    fun urlFor(streamId: Int): String? = idToUrl[streamId]
    fun isReady(): Boolean = idToUrl.isNotEmpty()
    fun size(): Int = sizeState.value
    fun lastErrorMessage(): String? = errorState.value
    fun isLoading(): Boolean = loadingState.value
    fun lastFetchAt(): Long = lastFetchAtState.value

    fun invalidate() {
        Logger.i(TAG, "invalidate")
        idToUrl = emptyMap()
        sizeState.value = 0
        lastFetchAtState.value = 0L
        errorState.value = null
        appContext?.let {
            runCatching { File(it.cacheDir, FILE_NAME).delete() }
        }
    }

    private suspend fun writeDiskCache() = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext
        try {
            File(ctx.cacheDir, FILE_NAME).bufferedWriter().use { w ->
                w.write("at=${lastFetchAtState.value}")
                w.newLine()
                idToUrl.forEach { (id, url) ->
                    w.write(id.toString())
                    w.write("\t")
                    w.write(url)
                    w.newLine()
                }
            }
            Logger.d(TAG, "writeDiskCache: ${idToUrl.size} entries written")
        } catch (t: Throwable) {
            Logger.w(TAG, "writeDiskCache failed: ${t.message}")
        }
    }
}
