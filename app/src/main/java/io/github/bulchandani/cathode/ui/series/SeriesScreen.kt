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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.bulchandani.cathode.data.catalog.CatalogRepo
import io.github.bulchandani.cathode.data.catalog.ContentKind
import io.github.bulchandani.cathode.data.catalog.FavoriteItem
import io.github.bulchandani.cathode.data.catalog.FavoritesRepo
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.data.xtream.XtreamCategory
import io.github.bulchandani.cathode.data.xtream.XtreamSeries
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

private const val ALL = "__all__"

@Composable
fun SeriesScreen(
    host: String,
    user: String,
    pass: String,
    onSeriesClick: (XtreamSeries) -> Unit,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var allSeries by remember { mutableStateOf<List<XtreamSeries>>(emptyList()) }
    var selectedCat by remember { mutableStateOf(ALL) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(host, user, pass) {
        if (host.isBlank()) { error = "No credentials. Open Settings."; loading = false; return@LaunchedEffect }
        try {
            categories = XtreamApi.fetchSeriesCategories(host, user, pass)
            allSeries = XtreamApi.fetchSeries(host, user, pass)
            CatalogRepo.setSeries(allSeries)
            loading = false
        } catch (t: Throwable) { error = t.message; loading = false }
    }

    BackHandler(onBack = onExit)

    val display = if (selectedCat == ALL) allSeries else allSeries.filter { it.categoryId == selectedCat }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SERIES", style = CathodeText.Display, color = PhosphorGreen)
                Spacer(Modifier.width(24.dp))
                if (!loading && error == null) {
                    Text("${display.size} TITLES", style = CathodeText.Section, color = Amber)
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Fetching series…", style = CathodeText.Section, color = PhosphorGreen)
                }
                error != null -> ErrorPane(error!!, onOpenSettings)
                else -> Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.width(260.dp).fillMaxHeight()) {
                        Text("CATEGORIES", style = CathodeText.Section, color = PhosphorGreen)
                        Spacer(Modifier.height(8.dp))
                        val countByCat = remember(allSeries) { allSeries.groupingBy { it.categoryId }.eachCount() }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item { CatRow("All", allSeries.size, selectedCat == ALL) { selectedCat = ALL } }
                            items(categories, key = { it.categoryId }) { c ->
                                CatRow(c.name, countByCat[c.categoryId] ?: 0, selectedCat == c.categoryId) {
                                    selectedCat = c.categoryId
                                }
                            }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(200.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(display, key = { it.seriesId }) { s ->
                            SeriesPoster(
                                s = s,
                                onClick = { onSeriesClick(s) },
                                onLongClick = {
                                    val pinned = FavoritesRepo.toggle(FavoriteItem(ContentKind.Series, s.seriesId, s.name))
                                    Toaster.show(if (pinned) "★ Pinned: ${s.name}" else "☆ Removed: ${s.name}")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    CathodeBox(modifier = Modifier.fillMaxWidth().height(40.dp), onClick = onClick) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = CathodeText.Body, color = if (selected) PhosphorGreen else OffWhite, modifier = Modifier.weight(1f))
            Text(count.toString(), style = CathodeText.Caption, color = PhosphorGreenDim)
        }
    }
}

@Composable
private fun SeriesPoster(s: XtreamSeries, onClick: () -> Unit, onLongClick: () -> Unit) {
    val isFavorite = FavoritesRepo.items.value.any { it.kind == ContentKind.Series && it.id == s.seriesId }
    CathodeBox(
        modifier = Modifier.size(width = 200.dp, height = 300.dp),
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(DimGrey),
                contentAlignment = Alignment.Center) {
                if (s.cover.isNotBlank()) {
                    AsyncImage(model = s.cover, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text("▣", style = CathodeText.Display, color = PhosphorGreenDim)
                }
                if (isFavorite) {
                    Text(
                        "★",
                        style = CathodeText.Section,
                        color = Amber,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
            }
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.name.ifBlank { "Series ${s.seriesId}" }, style = CathodeText.Caption, color = OffWhite, modifier = Modifier.weight(1f), maxLines = 1)
                Text(s.year.take(4), style = CathodeText.Caption, color = Amber)
            }
        }
    }
}

@Composable
private fun ErrorPane(message: String, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("⚠  COULDN'T LOAD SERIES", style = CathodeText.Headline, color = AlarmRed)
        Text(message, style = CathodeText.Body, color = OffWhite)
        Spacer(Modifier.height(16.dp))
        CathodeButton(text = "OPEN SETTINGS", onClick = onOpenSettings)
    }
}
