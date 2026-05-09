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
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.theme.ScanlineOverlay

private data class HubTile(val title: String)

private val tiles = listOf(
    HubTile("Live TV"),
    HubTile("Movies"),
    HubTile("Series"),
    HubTile("Search"),
    HubTile("Favorites"),
    HubTile("Recents"),
    HubTile("Settings"),
)

@Composable
fun HubScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.padding(48.dp)) {
            Text(
                text = "CATHODE",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(48.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(tiles) { tile ->
                    HubCard(tile.title)
                }
            }
        }
        ScanlineOverlay()
    }
}

@Composable
private fun HubCard(title: String) {
    Card(
        onClick = { /* TODO: navigate */ },
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .height(180.dp)
            .width(260.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
