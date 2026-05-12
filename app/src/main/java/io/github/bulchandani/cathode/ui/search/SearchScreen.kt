package io.github.bulchandani.cathode.ui.search

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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.catalog.CatalogRepo
import io.github.bulchandani.cathode.data.m3u.M3uIndex
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

@Composable
fun SearchScreen(
    host: String,
    user: String,
    pass: String,
    onLiveResultClick: (streamUrl: String, label: String, epgChannelId: String) -> Unit,
    onMovieResultClick: (streamUrl: String, label: String) -> Unit,
    onSeriesResultClick: (seriesId: Int, name: String) -> Unit,
    onExit: () -> Unit,
) {
    // Voice search: if MainActivity stashed a query for us, consume it once.
    var query by remember {
        mutableStateOf(io.github.bulchandani.cathode.MainActivity.consumeVoiceQuery() ?: "")
    }
    val q = query.trim().lowercase()

    BackHandler(onBack = onExit)

    val liveResults = remember(q) {
        if (q.length < 2) emptyList() else CatalogRepo.live.filter { it.name.contains(q, true) }.take(50)
    }
    val movieResults = remember(q) {
        if (q.length < 2) emptyList() else CatalogRepo.vod.filter { it.name.contains(q, true) }.take(50)
    }
    val seriesResults = remember(q) {
        if (q.length < 2) emptyList() else CatalogRepo.series.filter { it.name.contains(q, true) }.take(50)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text("SEARCH", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.height(12.dp))
            CathodeField(
                label = "Query",
                value = query,
                onValueChange = { query = it },
                placeholder = "type at least 2 characters",
                modifier = Modifier.fillMaxWidth(0.7f),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ResultColumn(
                    title = "CHANNELS  (${liveResults.size})",
                    rows = liveResults.map { ch -> ResultRow(
                        primary = ch.name.ifBlank { "Channel ${ch.streamId}" },
                        secondary = "%04d".format(ch.streamId),
                        onClick = {
                            val resolved = M3uIndex.resolveLiveUrl(ch.streamId, host, user, pass)
                            if (resolved == null) {
                                io.github.bulchandani.cathode.ui.components.Toaster.show(
                                    "M3U still loading — try again in a moment",
                                )
                            } else {
                                if (resolved.source == "FALLBACK") {
                                    io.github.bulchandani.cathode.ui.components.Toaster.show(
                                        "Using constructed URL — channel not in M3U",
                                    )
                                }
                                val label = "%04d  %s  [%s]".format(ch.streamId, ch.name, resolved.source)
                                onLiveResultClick(resolved.url, label, ch.epgChannelId)
                            }
                        },
                    ) },
                    modifier = Modifier.weight(1f),
                )
                ResultColumn(
                    title = "MOVIES  (${movieResults.size})",
                    rows = movieResults.map { m -> ResultRow(
                        primary = m.name,
                        secondary = m.year.take(4),
                        onClick = {
                            val url = XtreamApi.buildVodUrl(host, user, pass, m.streamId, m.containerExtension)
                            onMovieResultClick(url, m.name)
                        },
                    ) },
                    modifier = Modifier.weight(1f),
                )
                ResultColumn(
                    title = "SERIES  (${seriesResults.size})",
                    rows = seriesResults.map { s -> ResultRow(
                        primary = s.name,
                        secondary = s.year.take(4),
                        onClick = { onSeriesResultClick(s.seriesId, s.name) },
                    ) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private data class ResultRow(val primary: String, val secondary: String, val onClick: () -> Unit)

@Composable
private fun ResultColumn(title: String, rows: List<ResultRow>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) {
            Text("No results", style = CathodeText.Caption, color = PhosphorGreenDim)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(rows) { r ->
                    CathodeBox(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = r.onClick,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(r.primary, style = CathodeText.Body, color = OffWhite, modifier = Modifier.weight(1f), maxLines = 1)
                            if (r.secondary.isNotEmpty()) {
                                Text(r.secondary, style = CathodeText.Caption, color = Amber)
                            }
                        }
                    }
                }
            }
        }
    }
}
