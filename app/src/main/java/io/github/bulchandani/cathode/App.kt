package io.github.bulchandani.cathode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.bulchandani.cathode.data.catalog.CatalogRepo
import io.github.bulchandani.cathode.data.catalog.ContentKind
import io.github.bulchandani.cathode.data.catalog.RecentItem
import io.github.bulchandani.cathode.data.catalog.RecentsStore
import io.github.bulchandani.cathode.data.store.CredsStore
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.ui.favorites.FavoritesScreen
import io.github.bulchandani.cathode.ui.hub.HubScreen
import io.github.bulchandani.cathode.ui.hub.HubTileId
import io.github.bulchandani.cathode.ui.live.LiveTvScreen
import io.github.bulchandani.cathode.ui.movies.MoviesScreen
import io.github.bulchandani.cathode.ui.player.PlayerScreen
import io.github.bulchandani.cathode.ui.recents.RecentsScreen
import io.github.bulchandani.cathode.ui.search.SearchScreen
import io.github.bulchandani.cathode.ui.series.SeriesDetailScreen
import io.github.bulchandani.cathode.ui.series.SeriesScreen
import io.github.bulchandani.cathode.ui.streamtester.StreamTesterScreen

private sealed interface Screen {
    data object Hub : Screen
    data object StreamTester : Screen
    data object LiveTv : Screen
    data object Movies : Screen
    data object Series : Screen
    data object Search : Screen
    data object Recents : Screen
    data object Favorites : Screen
    data class SeriesDetail(val series: XtreamSeries) : Screen
    data class Player(
        val url: String,
        val label: String,
        val epgChannelId: String,
        val from: Screen,
    ) : Screen
}

private val LIVE_ID_REGEX = Regex("/live/[^/]+/[^/]+/(\\d+)\\.[^.]+$")
private val VOD_ID_REGEX = Regex("/movie/[^/]+/[^/]+/(\\d+)\\.[^.]+$")

private fun streamIdFromUrl(url: String, regex: Regex): Int =
    regex.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0

@Composable
fun App() {
    val context = LocalContext.current
    val creds = remember { CredsStore(context) }
    val recents = remember { RecentsStore(context) }

    var current by remember { mutableStateOf<Screen>(Screen.Hub) }
    var directUrl by remember { mutableStateOf(creds.lastDirectUrl) }
    var host by remember { mutableStateOf(creds.host) }
    var user by remember { mutableStateOf(creds.user) }
    var pass by remember { mutableStateOf(creds.pass) }

    fun goToPlayer(target: Screen.Player, recent: RecentItem?) {
        if (recent != null) recents.touch(recent.copy(lastPlayedAt = System.currentTimeMillis()))
        current = target
    }

    when (val screen = current) {
        Screen.Hub -> HubScreen(
            onTileClick = { tile ->
                current = when (tile) {
                    HubTileId.LiveTV -> if (creds.hasCreds()) Screen.LiveTv else Screen.StreamTester
                    HubTileId.Movies -> if (creds.hasCreds()) Screen.Movies else Screen.StreamTester
                    HubTileId.Series -> if (creds.hasCreds()) Screen.Series else Screen.StreamTester
                    HubTileId.Search -> Screen.Search
                    HubTileId.Recents -> Screen.Recents
                    HubTileId.Settings -> Screen.StreamTester
                    HubTileId.Favorites -> Screen.Favorites
                }
            },
        )

        Screen.StreamTester -> StreamTesterScreen(
            initialUrl = directUrl,
            initialHost = host,
            initialUser = user,
            initialPass = pass,
            onPlay = { url ->
                directUrl = url
                creds.lastDirectUrl = url
                goToPlayer(
                    Screen.Player(
                        url = url,
                        label = url.substringAfterLast('/').take(40),
                        epgChannelId = "",
                        from = Screen.StreamTester,
                    ),
                    recent = null,
                )
            },
            onUrlChange = {
                directUrl = it
                creds.lastDirectUrl = it
            },
            onCredsChange = { h, u, p ->
                host = h; user = u; pass = p
                creds.host = h; creds.user = u; creds.pass = p
            },
            onExit = { current = Screen.Hub },
        )

        Screen.LiveTv -> LiveTvScreen(
            host = host, user = user, pass = pass,
            onChannelClick = { url, label, epgId ->
                creds.lastChannelUrl = url
                val id = streamIdFromUrl(url, LIVE_ID_REGEX)
                goToPlayer(
                    Screen.Player(url = url, label = label, epgChannelId = epgId, from = Screen.LiveTv),
                    recent = RecentItem(ContentKind.Live, id, label, url, System.currentTimeMillis()),
                )
            },
            onExit = { current = Screen.Hub },
            onOpenSettings = { current = Screen.StreamTester },
        )

        Screen.Movies -> MoviesScreen(
            host = host, user = user, pass = pass,
            onMovieClick = { url, label ->
                val id = streamIdFromUrl(url, VOD_ID_REGEX)
                goToPlayer(
                    Screen.Player(url = url, label = label, epgChannelId = "", from = Screen.Movies),
                    recent = RecentItem(ContentKind.Movie, id, label, url, System.currentTimeMillis()),
                )
            },
            onExit = { current = Screen.Hub },
            onOpenSettings = { current = Screen.StreamTester },
        )

        Screen.Series -> SeriesScreen(
            host = host, user = user, pass = pass,
            onSeriesClick = { current = Screen.SeriesDetail(it) },
            onExit = { current = Screen.Hub },
            onOpenSettings = { current = Screen.StreamTester },
        )

        is Screen.SeriesDetail -> SeriesDetailScreen(
            host = host, user = user, pass = pass,
            series = screen.series,
            onEpisodeClick = { url, label ->
                goToPlayer(
                    Screen.Player(url = url, label = label, epgChannelId = "", from = screen),
                    recent = RecentItem(ContentKind.Series, screen.series.seriesId, "${screen.series.name} · $label", url, System.currentTimeMillis()),
                )
            },
            onExit = { current = Screen.Series },
        )

        Screen.Search -> SearchScreen(
            host = host, user = user, pass = pass,
            onLiveResultClick = { url, label, epgId ->
                val id = streamIdFromUrl(url, LIVE_ID_REGEX)
                goToPlayer(
                    Screen.Player(url, label, epgId, from = Screen.Search),
                    recent = RecentItem(ContentKind.Live, id, label, url, System.currentTimeMillis()),
                )
            },
            onMovieResultClick = { url, label ->
                val id = streamIdFromUrl(url, VOD_ID_REGEX)
                goToPlayer(
                    Screen.Player(url, label, "", from = Screen.Search),
                    recent = RecentItem(ContentKind.Movie, id, label, url, System.currentTimeMillis()),
                )
            },
            onSeriesResultClick = { id, name ->
                val match = CatalogRepo.series.firstOrNull { it.seriesId == id }
                if (match != null) current = Screen.SeriesDetail(match)
            },
            onExit = { current = Screen.Hub },
        )

        Screen.Favorites -> FavoritesScreen(
            host = host, user = user, pass = pass,
            onPlayLive = { url, label, epgId ->
                val id = streamIdFromUrl(url, LIVE_ID_REGEX)
                goToPlayer(
                    Screen.Player(url, label, epgId, from = Screen.Favorites),
                    recent = RecentItem(ContentKind.Live, id, label, url, System.currentTimeMillis()),
                )
            },
            onPlayMovie = { url, label ->
                val id = streamIdFromUrl(url, VOD_ID_REGEX)
                goToPlayer(
                    Screen.Player(url, label, "", from = Screen.Favorites),
                    recent = RecentItem(ContentKind.Movie, id, label, url, System.currentTimeMillis()),
                )
            },
            onOpenSeries = { current = Screen.SeriesDetail(it) },
            onExit = { current = Screen.Hub },
        )

        Screen.Recents -> RecentsScreen(
            onPlay = { r ->
                goToPlayer(
                    Screen.Player(
                        url = r.streamUrl,
                        label = r.name,
                        epgChannelId = "",
                        from = Screen.Recents,
                    ),
                    recent = r.copy(lastPlayedAt = System.currentTimeMillis()),
                )
            },
            onExit = { current = Screen.Hub },
        )

        is Screen.Player -> PlayerScreen(
            streamUrl = screen.url,
            channelLabel = screen.label,
            epgChannelId = screen.epgChannelId,
            onExit = { current = screen.from },
        )
    }
}
