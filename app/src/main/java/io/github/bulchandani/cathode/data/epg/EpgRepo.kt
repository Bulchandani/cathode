package io.github.bulchandani.cathode.data.epg

import android.content.Context
import io.github.bulchandani.cathode.data.xmltv.XmltvParser
import io.github.bulchandani.cathode.data.xmltv.XmltvProgramme
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * EPG cache. Memory-resident parsed map plus a disk-backed copy of
 * the raw XMLTV so that app restart within the 6h TTL doesn't have
 * to re-fetch from the provider (just re-parse). [invalidate] is
 * called on source switch so a fresh provider doesn't see stale EPG.
 */
object EpgRepo {

    const val CACHE_TTL_MS = 6L * 60 * 60 * 1000

    @Volatile private var cache: Map<String, List<XmltvProgramme>> = emptyMap()
    @Volatile private var lastFetch: Long = 0L
    @Volatile private var lastError: String? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var diskHydrated = false
    private val refreshLock = Mutex()

    /**
     * Async hydrate from disk on first app launch. Safe to call multiple
     * times; first caller does the work. The on-disk file is
     * `cacheDir/epg.xml` plus a small `cacheDir/epg.meta` with the
     * fetch timestamp.
     */
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        appContext = context.applicationContext
        if (diskHydrated) return@withContext
        diskHydrated = true
        val xmlFile = File(context.applicationContext.cacheDir, FILE_XML)
        val metaFile = File(context.applicationContext.cacheDir, FILE_META)
        if (!xmlFile.exists() || !metaFile.exists()) return@withContext
        val cachedAt = metaFile.readText().trim().toLongOrNull() ?: return@withContext
        if (System.currentTimeMillis() - cachedAt >= CACHE_TTL_MS) return@withContext
        try {
            cache = XmltvParser.parse(xmlFile.inputStream())
            lastFetch = cachedAt
        } catch (t: Throwable) {
            lastError = "Disk hydrate: ${t.message}"
        }
    }

    suspend fun load(host: String, user: String, pass: String, force: Boolean = false) {
        if (!force && cache.isNotEmpty() &&
            System.currentTimeMillis() - lastFetch < CACHE_TTL_MS
        ) return

        refreshLock.withLock {
            if (!force && cache.isNotEmpty() &&
                System.currentTimeMillis() - lastFetch < CACHE_TTL_MS
            ) return

            try {
                val xml = XtreamApi.fetchXmltv(host, user, pass)
                cache = XmltvParser.parse(xml.byteInputStream())
                val now = System.currentTimeMillis()
                lastFetch = now
                lastError = null
                writeDiskCache(xml, now)
            } catch (t: Throwable) {
                lastError = t.message ?: t::class.simpleName
            }
        }
    }

    private suspend fun writeDiskCache(xml: String, ts: Long) = withContext(Dispatchers.IO) {
        val ctx = appContext ?: return@withContext
        try {
            File(ctx.cacheDir, FILE_XML).writeText(xml)
            File(ctx.cacheDir, FILE_META).writeText(ts.toString())
        } catch (_: Throwable) { /* best-effort */ }
    }

    fun invalidate() {
        cache = emptyMap()
        lastFetch = 0L
        lastError = null
        appContext?.let { ctx ->
            runCatching {
                File(ctx.cacheDir, FILE_XML).delete()
                File(ctx.cacheDir, FILE_META).delete()
            }
        }
    }

    fun isReady(): Boolean = cache.isNotEmpty()
    fun lastFetchAt(): Long = lastFetch
    fun lastErrorMessage(): String? = lastError

    fun nowAndNext(epgChannelId: String): Pair<XmltvProgramme?, XmltvProgramme?> {
        if (epgChannelId.isBlank()) return null to null
        val list = cache[epgChannelId] ?: return null to null
        val now = System.currentTimeMillis()
        val current = list.firstOrNull { now in it.startMillis until it.stopMillis }
        val next = list.firstOrNull { it.startMillis > now }
        return current to next
    }

    fun upcoming(epgChannelId: String, count: Int = 6): List<XmltvProgramme> {
        if (epgChannelId.isBlank()) return emptyList()
        val list = cache[epgChannelId] ?: return emptyList()
        val now = System.currentTimeMillis()
        return list.asSequence().filter { it.stopMillis > now }.take(count).toList()
    }

    private const val FILE_XML = "epg.xml"
    private const val FILE_META = "epg.meta"
}
