package io.github.bulchandani.cathode

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.data.m3u.M3uIndex
import io.github.bulchandani.cathode.update.EpgRefreshWorker
import io.github.bulchandani.cathode.data.catalog.CatalogRepo
import io.github.bulchandani.cathode.data.catalog.ContentKind
import io.github.bulchandani.cathode.data.catalog.FavoritesRepo
import io.github.bulchandani.cathode.data.catalog.RecentItem
import io.github.bulchandani.cathode.data.catalog.RecentsStore
import io.github.bulchandani.cathode.data.store.CredsStore
import io.github.bulchandani.cathode.data.store.SettingsStore
import io.github.bulchandani.cathode.data.store.SourcesStore
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.ui.favorites.FavoritesScreen
import io.github.bulchandani.cathode.ui.live.LiveTvScreen
import io.github.bulchandani.cathode.ui.movies.MoviesScreen
import io.github.bulchandani.cathode.ui.player.PlayerScreen
import io.github.bulchandani.cathode.ui.recents.RecentsScreen
import io.github.bulchandani.cathode.ui.search.SearchScreen
import io.github.bulchandani.cathode.ui.series.SeriesDetailScreen
import io.github.bulchandani.cathode.ui.series.SeriesScreen
import io.github.bulchandani.cathode.ui.shell.CathodeShell
import io.github.bulchandani.cathode.ui.shell.ShellSection
import io.github.bulchandani.cathode.ui.sources.SetPinDialog
import io.github.bulchandani.cathode.ui.sources.SourceManagerScreen
import io.github.bulchandani.cathode.ui.sources.VerifyPinDialog
import io.github.bulchandani.cathode.ui.streamtester.StreamTesterScreen
import io.github.bulchandani.cathode.ui.testurl.TestUrlScreen

private sealed interface Overlay {
    data class Player(
        val url: String,
        val label: String,
        val epgChannelId: String,
        val backTo: Overlay?,
    ) : Overlay
    data class SeriesDetail(val series: XtreamSeries) : Overlay
    data object TestUrl : Overlay
    data object SourceManager : Overlay
}

private val LIVE_ID_REGEX = Regex("/live/[^/]+/[^/]+/(\\d+)\\.[^.]+$")
private val VOD_ID_REGEX = Regex("/movie/[^/]+/[^/]+/(\\d+)\\.[^.]+$")

private fun streamIdFromUrl(url: String, regex: Regex): Int =
    regex.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0

@Composable
fun App() {
    val context = LocalContext.current
    val activity = context as? Activity
    val exitApp: () -> Unit = { activity?.finish() }
    val creds = remember { CredsStore(context) }
    val recents = remember { RecentsStore(context) }
    remember {
        FavoritesRepo.init(context)
        SourcesStore.init(context)
        SettingsStore.init(context)
        Unit
    }
    LaunchedEffect(Unit) {
        EpgRepo.init(context)
        M3uIndex.init(context)
        EpgRefreshWorker.schedule(context)
        if (MainActivity.pendingVoiceQuery != null) section = ShellSection.Search
    }
    val activeSource by SourcesStore.active

    val initialSection = if (activeSource != null) ShellSection.LiveTv else ShellSection.Settings
    var section by remember { mutableStateOf(initialSection) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    var lastPlayer by remember { mutableStateOf<Overlay.Player?>(null) }
    var settingsUnlocked by remember { mutableStateOf(!SettingsStore.hasPin.value) }
    var setPinOpen by remember { mutableStateOf(false) }

    var directUrl by remember { mutableStateOf(creds.lastDirectUrl) }
    val host = activeSource?.host ?: ""
    val user = activeSource?.user ?: ""
    val pass = activeSource?.pass ?: ""

    fun openPlayer(p: Overlay.Player, recent: RecentItem?) {
        if (recent != null) recents.touch(recent.copy(lastPlayedAt = System.currentTimeMillis()))
        (overlay as? Overlay.Player)?.let { lastPlayer = it }
        overlay = p
    }

    when (val o = overlay) {
        is Overlay.Player -> PlayerScreen(
            streamUrl = o.url,
            channelLabel = o.label,
            epgChannelId = o.epgChannelId,
            onExit = { overlay = o.backTo },
            onLastChannel = lastPlayer?.let { prev ->
                {
                    lastPlayer = o
                    overlay = prev
                }
            },
            onJumpToChannelNumber = { number ->
                val ch = CatalogRepo.live.firstOrNull { it.streamId == number }
                if (ch == null) {
                    io.github.bulchandani.cathode.ui.components.Toaster.show("No channel #$number")
                } else {
                    val url = io.github.bulchandani.cathode.data.m3u.M3uIndex.urlFor(ch.streamId)
                        ?: io.github.bulchandani.cathode.data.xtream.XtreamApi.buildLiveStreamUrl(host, user, pass, ch.streamId)
                    val label = "%04d  %s".format(ch.streamId, ch.name)
                    openPlayer(
                        Overlay.Player(url, label, ch.epgChannelId, backTo = o.backTo),
                        recent = RecentItem(ContentKind.Live, ch.streamId, label, url, System.currentTimeMillis()),
                    )
                }
            },
        )
        is Overlay.SeriesDetail -> SeriesDetailScreen(
            host = host, user = user, pass = pass,
            series = o.series,
            onEpisodeClick = { url, label ->
                openPlayer(
                    Overlay.Player(url, label, "", backTo = o),
                    recent = RecentItem(
                        ContentKind.Series, o.series.seriesId,
                        "${o.series.name} · $label", url, System.currentTimeMillis(),
                    ),
                )
            },
            onExit = { overlay = null },
        )
        Overlay.SourceManager -> SourceManagerScreen(
            onAddSource = { overlay = null; section = ShellSection.Settings },
            onExit = { overlay = null },
        )
        Overlay.TestUrl -> TestUrlScreen(
            initialUrl = directUrl,
            onPlay = { url ->
                directUrl = url
                creds.lastDirectUrl = url
                openPlayer(
                    Overlay.Player(url, url.substringAfterLast('/').take(40), "", backTo = null),
                    recent = null,
                )
            },
            onUrlChange = { directUrl = it; creds.lastDirectUrl = it },
            onExit = { overlay = null },
        )
        null -> CathodeShell(
            active = section,
            onSelect = { section = it },
        ) {
            when (section) {
                ShellSection.LiveTv -> LiveTvScreen(
                    host = host, user = user, pass = pass,
                    onChannelClick = { url, label, epgId ->
                        creds.lastChannelUrl = url
                        val id = streamIdFromUrl(url, LIVE_ID_REGEX)
                        openPlayer(
                            Overlay.Player(url, label, epgId, backTo = null),
                            recent = RecentItem(ContentKind.Live, id, label, url, System.currentTimeMillis()),
                        )
                    },
                    onExit = exitApp,
                    onOpenSettings = { section = ShellSection.Settings },
                )

                ShellSection.Movies -> MoviesScreen(
                    host = host, user = user, pass = pass,
                    onMovieClick = { url, label ->
                        val id = streamIdFromUrl(url, VOD_ID_REGEX)
                        openPlayer(
                            Overlay.Player(url, label, "", backTo = null),
                            recent = RecentItem(ContentKind.Movie, id, label, url, System.currentTimeMillis()),
                        )
                    },
                    onExit = exitApp,
                    onOpenSettings = { section = ShellSection.Settings },
                )

                ShellSection.Series -> SeriesScreen(
                    host = host, user = user, pass = pass,
                    onSeriesClick = { overlay = Overlay.SeriesDetail(it) },
                    onExit = exitApp,
                    onOpenSettings = { section = ShellSection.Settings },
                )

                ShellSection.Search -> SearchScreen(
                    host = host, user = user, pass = pass,
                    onLiveResultClick = { url, label, epgId ->
                        val id = streamIdFromUrl(url, LIVE_ID_REGEX)
                        openPlayer(
                            Overlay.Player(url, label, epgId, backTo = null),
                            recent = RecentItem(ContentKind.Live, id, label, url, System.currentTimeMillis()),
                        )
                    },
                    onMovieResultClick = { url, label ->
                        val id = streamIdFromUrl(url, VOD_ID_REGEX)
                        openPlayer(
                            Overlay.Player(url, label, "", backTo = null),
                            recent = RecentItem(ContentKind.Movie, id, label, url, System.currentTimeMillis()),
                        )
                    },
                    onSeriesResultClick = { id, _ ->
                        val match = CatalogRepo.series.firstOrNull { it.seriesId == id }
                        if (match != null) overlay = Overlay.SeriesDetail(match)
                    },
                    onExit = exitApp,
                )

                ShellSection.Favorites -> FavoritesScreen(
                    host = host, user = user, pass = pass,
                    onPlayLive = { url, label, epgId ->
                        val id = streamIdFromUrl(url, LIVE_ID_REGEX)
                        openPlayer(
                            Overlay.Player(url, label, epgId, backTo = null),
                            recent = RecentItem(ContentKind.Live, id, label, url, System.currentTimeMillis()),
                        )
                    },
                    onPlayMovie = { url, label ->
                        val id = streamIdFromUrl(url, VOD_ID_REGEX)
                        openPlayer(
                            Overlay.Player(url, label, "", backTo = null),
                            recent = RecentItem(ContentKind.Movie, id, label, url, System.currentTimeMillis()),
                        )
                    },
                    onOpenSeries = { overlay = Overlay.SeriesDetail(it) },
                    onExit = exitApp,
                )

                ShellSection.Recents -> RecentsScreen(
                    onPlay = { r ->
                        openPlayer(
                            Overlay.Player(r.streamUrl, r.name, "", backTo = null),
                            recent = r.copy(lastPlayedAt = System.currentTimeMillis()),
                        )
                    },
                    onExit = exitApp,
                )

                ShellSection.Settings -> {
                    if (SettingsStore.hasPin.value && !settingsUnlocked) {
                        VerifyPinDialog(
                            onPass = { settingsUnlocked = true },
                            onDismiss = { section = ShellSection.LiveTv },
                        )
                    } else {
                        StreamTesterScreen(
                            initialHost = host,
                            initialUser = user,
                            initialPass = pass,
                            onPlay = { url ->
                                openPlayer(
                                    Overlay.Player(url, url.substringAfterLast('/').take(40), "", backTo = null),
                                    recent = null,
                                )
                            },
                            onCredsChange = { _, _, _ -> /* draft only — saved via onSaveSource */ },
                            onSaveSource = { h, u, p ->
                                val existing = SourcesStore.sources.value
                                    .firstOrNull { it.host == h && it.user == u && it.pass == p }
                                if (existing != null) {
                                    SourcesStore.setActive(existing.id)
                                } else {
                                    val newId = SourcesStore.nextId()
                                    val label = h.removePrefix("http://").removePrefix("https://").take(40)
                                    SourcesStore.add(io.github.bulchandani.cathode.data.store.Source(newId, label, h, u, p))
                                    SourcesStore.setActive(newId)
                                }
                            },
                            onOpenUrlTester = { overlay = Overlay.TestUrl },
                            onOpenSourceManager = { overlay = Overlay.SourceManager },
                            onOpenPinSetup = { setPinOpen = true },
                            onExit = exitApp,
                        )
                    }
                }
            }
        }
    }

    if (setPinOpen) {
        SetPinDialog(
            onDone = { setPinOpen = false; settingsUnlocked = true },
            onDismiss = { setPinOpen = false },
        )
    }
}
