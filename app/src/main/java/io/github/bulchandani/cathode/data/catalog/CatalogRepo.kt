package io.github.bulchandani.cathode.data.catalog

import io.github.bulchandani.cathode.data.xtream.XtreamLiveStream
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.data.xtream.XtreamVodStream

/**
 * In-memory catalog cache with a 30-minute TTL per kind. Browse
 * screens populate it after fetching; later screens skip the network
 * call if the cache is fresh. [invalidate] is called when the active
 * source changes so we don't show stale data from a different
 * provider.
 */
object CatalogRepo {

    private const val TTL_MS = 30L * 60 * 1000

    @Volatile var live: List<XtreamLiveStream> = emptyList(); private set
    @Volatile var vod: List<XtreamVodStream> = emptyList(); private set
    @Volatile var series: List<XtreamSeries> = emptyList(); private set

    @Volatile private var liveAt: Long = 0L
    @Volatile private var vodAt: Long = 0L
    @Volatile private var seriesAt: Long = 0L

    fun setLive(list: List<XtreamLiveStream>) { live = list; liveAt = System.currentTimeMillis() }
    fun setVod(list: List<XtreamVodStream>) { vod = list; vodAt = System.currentTimeMillis() }
    fun setSeries(list: List<XtreamSeries>) { series = list; seriesAt = System.currentTimeMillis() }

    fun isLiveFresh(): Boolean = live.isNotEmpty() && System.currentTimeMillis() - liveAt < TTL_MS
    fun isVodFresh(): Boolean = vod.isNotEmpty() && System.currentTimeMillis() - vodAt < TTL_MS
    fun isSeriesFresh(): Boolean = series.isNotEmpty() && System.currentTimeMillis() - seriesAt < TTL_MS

    fun invalidate() {
        live = emptyList(); liveAt = 0
        vod = emptyList(); vodAt = 0
        series = emptyList(); seriesAt = 0
    }
}
