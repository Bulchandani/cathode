package io.github.bulchandani.cathode.ui.log

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.log.Logger
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

/**
 * Live log viewer. Reads [Logger.rev] so the list auto-scrolls when
 * new entries land. We append-only and render newest-last to match
 * console-style reading order.
 */
@Composable
fun LogViewerScreen(onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    val rev by Logger.rev
    val entries = remember(rev) { Logger.snapshot() }
    val listState = rememberLazyListState()
    val clipboard: ClipboardManager = LocalClipboardManager.current

    LaunchedEffect(rev) {
        if (entries.isNotEmpty()) {
            listState.scrollToItem(entries.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LOG VIEWER", style = CathodeText.Display, color = PhosphorGreen)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "${entries.size} entries",
                    style = CathodeText.Caption,
                    color = PhosphorGreenDim,
                )
                Spacer(Modifier.weight(1f))
                CathodeButton(text = "COPY ALL", onClick = {
                    clipboard.setText(AnnotatedString(Logger.renderAll()))
                    Toaster.show("Log copied to clipboard")
                })
                Spacer(Modifier.width(8.dp))
                CathodeButton(text = "CLEAR", onClick = { Logger.clear() })
                Spacer(Modifier.width(8.dp))
                CathodeButton(text = "CLOSE", onClick = onExit)
            }
            Spacer(Modifier.height(16.dp))
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No log entries yet.",
                        style = CathodeText.Body,
                        color = PhosphorGreenDim,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(entries) { entry ->
                        Text(
                            text = entry.render(),
                            style = CathodeText.Data,
                            color = when (entry.level) {
                                Logger.Level.E -> AlarmRed
                                Logger.Level.W -> Amber
                                Logger.Level.I -> PhosphorGreen
                                Logger.Level.D -> OffWhite
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

