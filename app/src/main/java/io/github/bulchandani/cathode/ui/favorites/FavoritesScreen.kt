package io.github.bulchandani.cathode.ui.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.catalog.CatalogRepo
import io.github.bulchandani.cathode.data.catalog.ContentKind
import io.github.bulchandani.cathode.data.catalog.FavoriteItem
import io.github.bulchandani.cathode.data.catalog.FavoritesRepo
import io.github.bulchandani.cathode.data.m3u.M3uIndex
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

@Composable
fun FavoritesScreen(
    host: String,
    user: String,
    pass: String,
    onPlayLive: (streamUrl: String, label: String, epgChannelId: String) -> Unit,
    onPlayMovie: (streamUrl: String, label: String) -> Unit,
    onOpenSeries: (XtreamSeries) -> Unit,
    onExit: () -> Unit,
) {
    val items by FavoritesRepo.items

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text("FAVORITES", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.height(8.dp))
            Text(
                "Long-press a channel, movie, or series anywhere in the app to pin or un-pin it.",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            Spacer(Modifier.height(16.dp))
            if (items.isEmpty()) {
                Text("No favorites yet.", style = CathodeText.Body, color = OffWhite)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items, key = { it.kind.name + it.id }) { f ->
                        FavoriteRow(
                            item = f,
                            onPlay = {
                                when (f.kind) {
                                    ContentKind.Live -> {
                                        val ch = CatalogRepo.live.firstOrNull { it.streamId == f.id }
                                        if (ch != null) {
                                            val url = M3uIndex.urlFor(ch.streamId)
                                                ?: XtreamApi.buildLiveStreamUrl(host, user, pass, ch.streamId)
                                            val label = "%04d  %s".format(ch.streamId, ch.name)
                                            onPlayLive(url, label, ch.epgChannelId)
                                        }
                                    }
                                    ContentKind.Movie -> {
                                        val m = CatalogRepo.vod.firstOrNull { it.streamId == f.id }
                                        if (m != null) {
                                            val url = XtreamApi.buildVodUrl(host, user, pass, m.streamId, m.containerExtension)
                                            onPlayMovie(url, m.name)
                                        }
                                    }
                                    ContentKind.Series -> {
                                        val s = CatalogRepo.series.firstOrNull { it.seriesId == f.id }
                                        if (s != null) onOpenSeries(s)
                                    }
                                }
                            },
                            onUnpin = {
                                FavoritesRepo.toggle(f)
                                Toaster.show("☆ Removed: ${f.name}")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(item: FavoriteItem, onPlay: () -> Unit, onUnpin: () -> Unit) {
    CathodeBox(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        onClick = onPlay,
        onLongClick = onUnpin,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "★",
                style = CathodeText.Section,
                color = Amber,
            )
            Text(
                text = when (item.kind) {
                    ContentKind.Live -> "LIVE"
                    ContentKind.Movie -> "MOVIE"
                    ContentKind.Series -> "SERIES"
                },
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            Text(
                text = item.name.ifBlank { "${item.kind.name} ${item.id}" },
                style = CathodeText.Body,
                color = OffWhite,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                text = "long-press to un-pin",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
        }
    }
}
