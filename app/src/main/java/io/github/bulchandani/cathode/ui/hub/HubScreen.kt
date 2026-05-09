package io.github.bulchandani.cathode.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.BuildConfig
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

enum class HubTileId(val title: String) {
    LiveTV("Live TV"),
    Movies("Movies"),
    Series("Series"),
    Search("Search"),
    Favorites("Favorites"),
    Recents("Recents"),
    Settings("Settings"),
}

@Composable
fun HubScreen(onTileClick: (HubTileId) -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(modifier = Modifier.padding(48.dp)) {
            Text(
                text = "CATHODE",
                style = CathodeText.Display,
                color = PhosphorGreen,
            )
            Spacer(Modifier.height(48.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(HubTileId.entries) { tile ->
                    HubTile(tile.title, onClick = { onTileClick(tile) })
                }
            }
        }

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = CathodeText.Caption,
            color = PhosphorGreenDim,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        )

        CathodeScanlines()
        CathodeVignette()
    }
}

@Composable
private fun HubTile(title: String, onClick: () -> Unit) {
    CathodeBox(
        modifier = Modifier
            .height(180.dp)
            .width(260.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = CathodeText.Headline,
                color = PhosphorGreen,
            )
        }
    }
}
