package io.github.bulchandani.cathode.ui.series

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.data.xtream.XtreamSeriesEpisode
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

@Composable
fun SeriesDetailScreen(
    host: String,
    user: String,
    pass: String,
    series: XtreamSeries,
    onEpisodeClick: (streamUrl: String, label: String) -> Unit,
    onExit: () -> Unit,
) {
    var episodes by remember { mutableStateOf<List<XtreamSeriesEpisode>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(series.seriesId) {
        try {
            episodes = XtreamApi.fetchSeriesInfo(host, user, pass, series.seriesId)
            selectedSeason = episodes.minOfOrNull { it.seasonNum } ?: 1
            loading = false
        } catch (t: Throwable) { error = t.message; loading = false }
    }

    BackHandler(onBack = onExit)

    val seasons = remember(episodes) { episodes.map { it.seasonNum }.distinct().sorted() }
    val displayed = episodes.filter { it.seasonNum == selectedSeason }

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text(series.name, style = CathodeText.Display, color = PhosphorGreen)
            if (series.plot.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(series.plot, style = CathodeText.Body, color = OffWhite, maxLines = 3)
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Fetching episodes…", style = CathodeText.Section, color = PhosphorGreen)
                }
                error != null -> Text("⚠  ${error}", style = CathodeText.Body, color = AlarmRed)
                else -> Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.width(180.dp).fillMaxHeight()) {
                        Text("SEASONS", style = CathodeText.Section, color = PhosphorGreen)
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(seasons) { s ->
                                CathodeBox(
                                    modifier = Modifier.fillMaxWidth().height(40.dp),
                                    onClick = { selectedSeason = s },
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Text(
                                            "Season $s",
                                            style = CathodeText.Body,
                                            color = if (s == selectedSeason) PhosphorGreen else OffWhite,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text("EPISODES", style = CathodeText.Section, color = PhosphorGreen)
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(displayed, key = { it.episodeId }) { ep ->
                                CathodeBox(
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    onClick = {
                                        val url = XtreamApi.buildSeriesEpisodeUrl(
                                            host, user, pass, ep.episodeId, ep.containerExtension,
                                        )
                                        val label = "S%02dE%02d  %s".format(ep.seasonNum, ep.episodeNum, ep.title)
                                        onEpisodeClick(url, label)
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            "S%02dE%02d".format(ep.seasonNum, ep.episodeNum),
                                            style = CathodeText.Data,
                                            color = Amber,
                                            modifier = Modifier.width(80.dp),
                                        )
                                        Text(
                                            ep.title.ifBlank { "Episode ${ep.episodeNum}" },
                                            style = CathodeText.Body,
                                            color = OffWhite,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        CathodeScanlines()
        CathodeVignette()
    }
}
