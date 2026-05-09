package io.github.bulchandani.cathode.data.catalog

import io.github.bulchandani.cathode.data.xtream.XtreamLiveStream
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.data.xtream.XtreamVodStream

/**
 * In-memory catalog cache. LiveTvScreen / MoviesScreen / SeriesScreen
 * populate it after they fetch; SearchScreen reads from it. v0.6.0
 * promotes this to a Room-backed cache for offline use.
 */
object CatalogRepo {
    @Volatile var live: List<XtreamLiveStream> = emptyList(); private set
    @Volatile var vod: List<XtreamVodStream> = emptyList(); private set
    @Volatile var series: List<XtreamSeries> = emptyList(); private set

    fun setLive(list: List<XtreamLiveStream>) { live = list }
    fun setVod(list: List<XtreamVodStream>) { vod = list }
    fun setSeries(list: List<XtreamSeries>) { series = list }
}
