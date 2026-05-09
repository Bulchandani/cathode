package io.github.bulchandani.cathode.ui.streamtester

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.ScanlineOverlay
import io.github.bulchandani.cathode.ui.theme.Void

@Composable
fun StreamTesterScreen(
    initialUrl: String,
    onPlay: (String) -> Unit,
    onExit: () -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }

    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "TEST STREAM",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    letterSpacing = 2.sp,
                ),
                color = PhosphorGreen,
            )

            Text(
                text = "Paste any HLS / DASH / MPEG-TS URL below.\n" +
                    "Xtream Codes example:\n" +
                    "http://your-host.com/live/USERNAME/PASSWORD/STREAM_ID.m3u8",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                ),
                color = OffWhite,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Stream URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PhosphorGreen,
                    unfocusedBorderColor = PhosphorGreenDim,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedLabelColor = PhosphorGreen,
                    unfocusedLabelColor = PhosphorGreenDim,
                    cursorColor = PhosphorGreen,
                ),
            )

            Button(
                onClick = { onPlay(url.trim()) },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhosphorGreen,
                    contentColor = Void,
                    disabledContainerColor = PhosphorGreenDim,
                    disabledContentColor = OffWhite,
                ),
            ) {
                Text(
                    text = "PLAY",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        ScanlineOverlay()
    }
}
