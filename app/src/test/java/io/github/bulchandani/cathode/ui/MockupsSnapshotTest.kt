package io.github.bulchandani.cathode.ui

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.components.cathodeGlow
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.CathodeTheme
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1920dp-h1080dp-television-xhdpi")
class MockupsSnapshotTest {

    @Test
    fun mockup_liveTv() = render("mockup_live_tv.png") { LiveTvMockup() }

    @Test
    fun mockup_epgGrid() = render("mockup_epg.png") { EpgGridMockup() }

    @Test
    fun mockup_movies() = render("mockup_movies.png") { MoviesMockup() }

    @Test
    fun mockup_series() = render("mockup_series.png") { SeriesMockup() }

    @Test
    fun mockup_search() = render("mockup_search.png") { SearchMockup() }

    @Test
    fun mockup_favoritesRecents() = render("mockup_favorites_recents.png") { FavoritesRecentsMockup() }

    private fun render(name: String, content: @Composable () -> Unit) {
        captureRoboImage(filePath = "build/outputs/roborazzi/$name") {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                CathodeTheme {
                    Box(modifier = Modifier.fillMaxSize().background(Void)) {
                        content()
                        CathodeScanlines()
                        CathodeVignette()
                    }
                }
            }
        }
    }
}

// ---------- Placeholder data ----------

private val Categories = listOf("All", "News", "Sports", "Movies", "Entertainment", "Kids", "Music", "Documentary")
private val LiveChannels = listOf(
    Triple(101, "BBC ONE HD", "EastEnders"),
    Triple(102, "BBC TWO HD", "Newsnight"),
    Triple(103, "ITV1 HD", "Coronation Street"),
    Triple(104, "Channel 4 HD", "Gogglebox"),
    Triple(105, "Channel 5 HD", "GBN at Five"),
    Triple(201, "Sky Sports Main", "Premier League: Liverpool v Arsenal"),
    Triple(202, "Sky Sports Football", "Bundesliga: Bayern v Dortmund"),
    Triple(203, "TNT Sports 1", "Champions League: Real v PSG"),
    Triple(301, "Sky Cinema Premiere", "Dune: Part Two"),
    Triple(302, "Film4", "Casablanca"),
    Triple(303, "TCM Movies", "The Big Sleep"),
    Triple(401, "Discovery HD", "Wheeler Dealers"),
    Triple(402, "National Geographic", "Cosmos: Possible Worlds"),
)

private val Movies = listOf(
    "Dune: Part Two" to "2024 · 12 · 2h 46m",
    "Oppenheimer" to "2023 · 15 · 3h 0m",
    "The Brutalist" to "2024 · 18 · 3h 35m",
    "Anora" to "2024 · 18 · 2h 19m",
    "Conclave" to "2024 · 12 · 2h 0m",
    "The Substance" to "2024 · 18 · 2h 21m",
    "Wicked" to "2024 · PG · 2h 40m",
    "Nosferatu" to "2024 · 15 · 2h 12m",
    "A Real Pain" to "2024 · 15 · 1h 30m",
    "Sing Sing" to "2024 · 12 · 1h 47m",
    "Emilia Pérez" to "2024 · 15 · 2h 12m",
    "Heretic" to "2024 · 15 · 1h 51m",
)

private val Series = listOf(
    "Severance" to "S2 · 10 ep",
    "Slow Horses" to "S4 · 6 ep",
    "Shogun" to "S1 · 10 ep",
    "True Detective" to "S4 · 6 ep",
    "Fallout" to "S1 · 8 ep",
    "The Bear" to "S3 · 10 ep",
    "Ripley" to "S1 · 8 ep",
    "Industry" to "S3 · 8 ep",
)

// ---------- Live TV ----------

@Composable
private fun LiveTvMockup() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "LIVE TV", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.weight(1f))
            Text(text = "21:42", style = CathodeText.Headline, color = Amber)
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Categories sidebar
            Column(
                modifier = Modifier.width(220.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("CATEGORIES", style = CathodeText.Section, color = PhosphorGreenDim)
                Spacer(Modifier.height(4.dp))
                Categories.forEachIndexed { i, cat ->
                    SidebarItem(label = cat, count = (12..120).random(), focused = i == 2)
                }
            }
            // Channel list
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("CHANNELS", style = CathodeText.Section, color = PhosphorGreen)
                Spacer(Modifier.height(8.dp))
                LiveChannels.forEachIndexed { i, (num, name, now) ->
                    ChannelMockRow(num = num, name = name, nowOn = now, focused = i == 5)
                    Spacer(Modifier.height(4.dp))
                }
            }
            // Preview pane
            Column(modifier = Modifier.width(420.dp).fillMaxHeight()) {
                Text("NOW PLAYING", style = CathodeText.Section, color = PhosphorGreen)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DimGrey)
                        .cathodeGlow(focused = false, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("◉ 201  Sky Sports Main", style = CathodeText.Section, color = PhosphorGreen)
                }
                Spacer(Modifier.height(12.dp))
                Text("Premier League: Liverpool v Arsenal", style = CathodeText.Body, color = OffWhite)
                Text("21:00 — 23:00 · Football", style = CathodeText.Caption, color = PhosphorGreenDim)
                Spacer(Modifier.height(16.dp))
                Text("UPCOMING", style = CathodeText.Section, color = PhosphorGreenDim)
                Spacer(Modifier.height(4.dp))
                EpgRow("23:00", "Premier League Reaction")
                EpgRow("00:00", "Soccer Special")
                EpgRow("01:00", "Champions League Highlights")
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, count: Int, focused: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Void else Color.Transparent)
            .cathodeGlow(focused = focused, shape = RoundedCornerShape(6.dp), blurDp = 18.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = CathodeText.Body,
                color = if (focused) PhosphorGreen else OffWhite,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = count.toString(),
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
        }
    }
}

@Composable
private fun ChannelMockRow(num: Int, name: String, nowOn: String, focused: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DimGrey)
            .cathodeGlow(focused = focused, shape = RoundedCornerShape(6.dp), blurDp = 18.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "%04d".format(num), style = CathodeText.Data, color = Amber, modifier = Modifier.width(64.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = CathodeText.Body, color = if (focused) PhosphorGreen else OffWhite)
                Text(text = "Now: $nowOn", style = CathodeText.Caption, color = PhosphorGreenDim)
            }
        }
    }
}

@Composable
private fun EpgRow(time: String, title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = time, style = CathodeText.Data, color = PhosphorGreenDim, modifier = Modifier.width(64.dp))
        Text(text = title, style = CathodeText.Data, color = OffWhite)
    }
}

// ---------- EPG Grid ----------

@Composable
private fun EpgGridMockup() {
    val timeSlots = listOf("21:00", "21:30", "22:00", "22:30", "23:00")
    val channels = LiveChannels.take(8)
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GUIDE", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.weight(1f))
            Text("21:42 · TUE 09 MAY", style = CathodeText.Headline, color = Amber)
        }
        Spacer(Modifier.height(16.dp))
        // Time header
        Row {
            Spacer(Modifier.width(280.dp))
            timeSlots.forEach { t ->
                Box(modifier = Modifier.width(280.dp).padding(start = 12.dp)) {
                    Text(t, style = CathodeText.Section, color = PhosphorGreen)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        channels.forEachIndexed { i, (num, name, _) ->
            Row(modifier = Modifier.height(72.dp)) {
                // Channel column
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DimGrey)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        Text("%04d".format(num), style = CathodeText.Caption, color = Amber)
                        Text(name, style = CathodeText.Body, color = OffWhite)
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Programs row — 5 program cells per channel, varying widths to fake realistic schedules
                EpgCell("Live show", weight = 2f, focused = i == 2)
                EpgCell("News", weight = 1f, focused = false)
                EpgCell("Documentary special", weight = 2f, focused = false)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.EpgCell(title: String, weight: Float, focused: Boolean) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DimGrey)
            .cathodeGlow(focused = focused, shape = RoundedCornerShape(6.dp), blurDp = 16.dp)
            .padding(12.dp),
    ) {
        Text(title, style = CathodeText.Data, color = if (focused) PhosphorGreen else OffWhite)
    }
}

// ---------- Movies grid ----------

@Composable
private fun MoviesMockup() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MOVIES", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.weight(1f))
            Text("${Movies.size * 87} TITLES", style = CathodeText.Section, color = Amber)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("All", "Action", "Drama", "Comedy", "Sci-Fi", "Horror", "Thriller", "Documentary").forEachIndexed { i, c ->
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (i == 0) PhosphorGreen else DimGrey)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(c, style = CathodeText.Data, color = if (i == 0) Void else OffWhite)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(6)) {
            items(Movies) { (title, meta) ->
                MoviePoster(title = title, meta = meta, focused = title == "Oppenheimer")
            }
        }
    }
}

@Composable
private fun MoviePoster(title: String, meta: String, focused: Boolean) {
    Column(modifier = Modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(310.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DimGrey)
                .cathodeGlow(focused = focused, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("◉", style = CathodeText.Display, color = PhosphorGreenDim)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = CathodeText.Body, color = if (focused) PhosphorGreen else OffWhite)
        Text(meta, style = CathodeText.Caption, color = PhosphorGreenDim)
    }
}

// ---------- Series grid ----------

@Composable
private fun SeriesMockup() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SERIES", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.weight(1f))
            Text("${Series.size * 41} TITLES", style = CathodeText.Section, color = Amber)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            listOf("All", "Drama", "Comedy", "Sci-Fi", "Crime", "Documentary", "Anime").forEachIndexed { i, c ->
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (i == 0) PhosphorGreen else DimGrey)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(c, style = CathodeText.Data, color = if (i == 0) Void else OffWhite)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4)) {
            items(Series) { (title, meta) ->
                SeriesPoster(title = title, meta = meta, focused = title == "Severance")
            }
        }
    }
}

@Composable
private fun SeriesPoster(title: String, meta: String, focused: Boolean) {
    Column(modifier = Modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .width(340.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DimGrey)
                .cathodeGlow(focused = focused, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▣", style = CathodeText.Display, color = PhosphorGreenDim)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = CathodeText.Body, color = if (focused) PhosphorGreen else OffWhite, modifier = Modifier.weight(1f))
            Text(meta, style = CathodeText.Caption, color = Amber)
        }
    }
}

// ---------- Search ----------

@Composable
private fun SearchMockup() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("SEARCH", style = CathodeText.Display, color = PhosphorGreen)
        Spacer(Modifier.height(16.dp))
        CathodeField(
            label = "Query",
            value = "liverpool",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SearchColumn(
                title = "CHANNELS",
                items = listOf("LFC TV HD", "Sky Sports Main", "Sky Sports Football"),
            )
            SearchColumn(
                title = "PROGRAMS",
                items = listOf("Premier League: Liverpool v Arsenal", "Liverpool U21 highlights", "Liverpool: Beyond the Game"),
            )
            SearchColumn(
                title = "MOVIES",
                items = listOf("Once Upon a Time in Liverpool", "Mr Liverpool", "The Liverpool Birmingham Story"),
            )
            SearchColumn(
                title = "SERIES",
                items = listOf("Boys from the Blackstuff", "This Is England (Liverpool ep)", "The Responder"),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SearchColumn(title: String, items: List<String>) {
    Column(modifier = Modifier.weight(1f)) {
        Text(title, style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { i, t ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DimGrey)
                    .cathodeGlow(focused = i == 0, shape = RoundedCornerShape(6.dp), blurDp = 16.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(t, style = CathodeText.Body, color = if (i == 0) PhosphorGreen else OffWhite)
            }
        }
    }
}

// ---------- Favorites + Recents ----------

@Composable
private fun FavoritesRecentsMockup() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FAVORITES", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.width(48.dp))
            Text("RECENTS", style = CathodeText.Display, color = PhosphorGreenDim)
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            // Favorites column
            Column(modifier = Modifier.weight(1f)) {
                Text("PINNED CHANNELS", style = CathodeText.Section, color = PhosphorGreen)
                Spacer(Modifier.height(12.dp))
                LiveChannels.take(7).forEachIndexed { i, (num, name, nowOn) ->
                    ChannelMockRow(num = num, name = name, nowOn = nowOn, focused = i == 0)
                    Spacer(Modifier.height(6.dp))
                }
            }
            // Recents column
            Column(modifier = Modifier.weight(1f)) {
                Text("CONTINUE WATCHING", style = CathodeText.Section, color = PhosphorGreen)
                Spacer(Modifier.height(12.dp))
                listOf(
                    Triple("Severance · S2E04", "Hello, Ms. Cobel", 0.55f),
                    Triple("Slow Horses · S4E02", "Hard Lessons", 0.30f),
                    Triple("Dune: Part Two", "Movie", 0.18f),
                    Triple("BBC ONE HD", "Live", null),
                    Triple("Shogun · S1E09", "Crimson Sky", 0.92f),
                ).forEachIndexed { i, (title, sub, prog) ->
                    RecentRow(title = title, sub = sub, progress = prog, focused = i == 0)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun RecentRow(title: String, sub: String, progress: Float?, focused: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DimGrey)
            .cathodeGlow(focused = focused, shape = RoundedCornerShape(6.dp), blurDp = 18.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row {
            Text(title, style = CathodeText.Body, color = if (focused) PhosphorGreen else OffWhite, modifier = Modifier.weight(1f))
            Text(sub, style = CathodeText.Caption, color = Amber)
        }
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(PhosphorGreenDim)) {
                Box(modifier = Modifier.fillMaxWidth(progress).height(3.dp).background(PhosphorGreen))
            }
        }
    }
}
