package io.github.bulchandani.cathode.data.epg

import io.github.bulchandani.cathode.data.xmltv.XmltvParser
import io.github.bulchandani.cathode.data.xmltv.XmltvProgramme
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-singleton in-memory EPG cache. Refreshes on demand or when
 * older than [CACHE_TTL_MS]. Disk persistence + WorkManager refresh
 * lands in v0.5.0 (when we add favorites/recents persistence anyway).
 */
object EpgRepo {

    private const val CACHE_TTL_MS = 6L * 60 * 60 * 1000

    @Volatile private var cache: Map<String, List<XmltvProgramme>> = emptyMap()
    @Volatile private var lastFetch: Long = 0L
    @Volatile private var lastError: String? = null
    private val refreshLock = Mutex()

    suspend fun load(host: String, user: String, pass: String, force: Boolean = false) {
        if (!force && cache.isNotEmpty() &&
            System.currentTimeMillis() - lastFetch < CACHE_TTL_MS
        ) return

        refreshLock.withLock {
            // Re-check after acquiring the lock — another coroutine may have refreshed.
            if (!force && cache.isNotEmpty() &&
                System.currentTimeMillis() - lastFetch < CACHE_TTL_MS
            ) return

            try {
                val xml = XtreamApi.fetchXmltv(host, user, pass)
                cache = XmltvParser.parse(xml.byteInputStream())
                lastFetch = System.currentTimeMillis()
                lastError = null
            } catch (t: Throwable) {
                lastError = t.message ?: t::class.simpleName
            }
        }
    }

    fun isReady(): Boolean = cache.isNotEmpty()
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
        return list.asSequence()
            .filter { it.stopMillis > now }
            .take(count)
            .toList()
    }
}
