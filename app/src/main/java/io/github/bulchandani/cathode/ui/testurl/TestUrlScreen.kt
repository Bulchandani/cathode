package io.github.bulchandani.cathode.ui.testurl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

@Composable
fun TestUrlScreen(
    initialUrl: String,
    onPlay: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onExit: () -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("TEST URL", style = CathodeText.Display, color = PhosphorGreen)
            Text(
                "Paste any HLS / DASH / MPEG-TS URL to play it directly. " +
                    "Useful for diagnosing provider issues or playing ad-hoc streams.",
                style = CathodeText.Body,
                color = OffWhite,
            )
            Text(
                "Example: http://your-provider.com:8080/live/USER/PASS/STREAM_ID.m3u8",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            Spacer(Modifier.height(16.dp))

            CathodeField(
                label = "Stream URL",
                value = url,
                onValueChange = { url = it; onUrlChange(it) },
                placeholder = "https://…/master.m3u8",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CathodeButton(text = "PLAY", onClick = { onPlay(url.trim()) }, enabled = url.isNotBlank())
                CathodeButton(text = "BACK", onClick = onExit)
            }
        }
        CathodeScanlines()
        CathodeVignette()
    }
}
