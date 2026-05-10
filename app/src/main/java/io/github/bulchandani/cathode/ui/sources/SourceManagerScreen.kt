package io.github.bulchandani.cathode.ui.sources

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.store.SourcesStore
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

@Composable
fun SourceManagerScreen(
    onAddSource: () -> Unit,
    onExit: () -> Unit,
) {
    val sources by SourcesStore.sources
    val active by SourcesStore.active

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("SOURCES", style = CathodeText.Display, color = PhosphorGreen)
            Text(
                "Switch between saved Xtream providers, or add a new one.",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            Spacer(Modifier.height(8.dp))

            if (sources.isEmpty()) {
                Text("No sources saved yet.", style = CathodeText.Body, color = OffWhite)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources, key = { it.id }) { src ->
                        val isActive = src.id == active?.id
                        CathodeBox(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        src.label.ifBlank { "(unnamed)" },
                                        style = CathodeText.Body,
                                        color = if (isActive) PhosphorGreen else OffWhite,
                                    )
                                    Text(
                                        "${src.host}  ·  user: ${src.user}",
                                        style = CathodeText.Caption,
                                        color = PhosphorGreenDim,
                                        maxLines = 1,
                                    )
                                }
                                if (isActive) {
                                    Text("ACTIVE", style = CathodeText.Caption, color = Amber)
                                } else {
                                    CathodeButton(
                                        text = "SWITCH",
                                        onClick = {
                                            SourcesStore.setActive(src.id)
                                            Toaster.show("Switched to ${src.label}")
                                        },
                                    )
                                }
                                CathodeButton(
                                    text = "DELETE",
                                    onClick = {
                                        SourcesStore.delete(src.id)
                                        Toaster.show("Removed ${src.label}")
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CathodeButton(text = "ADD A SOURCE", onClick = onAddSource)
                CathodeButton(text = "BACK", onClick = onExit)
            }
        }
    }
}
