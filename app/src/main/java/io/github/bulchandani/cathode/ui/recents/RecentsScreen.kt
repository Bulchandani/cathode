package io.github.bulchandani.cathode.ui.recents

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
import io.github.bulchandani.cathode.data.catalog.ContentKind
import io.github.bulchandani.cathode.data.catalog.RecentItem
import io.github.bulchandani.cathode.data.catalog.RecentsStore
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void
import java.text.DateFormat
import java.util.Date

@Composable
fun RecentsScreen(
    onPlay: (RecentItem) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { RecentsStore(context) }
    var items by remember { mutableStateOf(store.all()) }

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text("RECENTS", style = CathodeText.Display, color = PhosphorGreen)
            Spacer(Modifier.height(8.dp))
            Text("Continue watching across Live TV, Movies, and Series.", style = CathodeText.Caption, color = PhosphorGreenDim)
            Spacer(Modifier.height(16.dp))
            if (items.isEmpty()) {
                Text("Nothing watched yet — play something from Live TV, Movies, or Series and it'll appear here.",
                    style = CathodeText.Body, color = OffWhite)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items, key = { it.kind.name + it.id }) { r ->
                        CathodeBox(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            onClick = { onPlay(r) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = when (r.kind) {
                                        ContentKind.Live -> "LIVE"
                                        ContentKind.Movie -> "MOVIE"
                                        ContentKind.Series -> "EPISODE"
                                    },
                                    style = CathodeText.Caption,
                                    color = Amber,
                                )
                                Text(r.name.ifBlank { "Item ${r.id}" }, style = CathodeText.Body, color = OffWhite, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(
                                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(r.lastPlayedAt)),
                                    style = CathodeText.Caption,
                                    color = PhosphorGreenDim,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
