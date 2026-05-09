package io.github.bulchandani.cathode.ui.streamtester

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.data.xtream.XtreamLiveStream
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.ScanlineOverlay
import io.github.bulchandani.cathode.ui.theme.Void
import kotlinx.coroutines.launch

@Composable
fun StreamTesterScreen(
    initialUrl: String,
    initialHost: String,
    initialUser: String,
    initialPass: String,
    onPlay: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCredsChange: (host: String, user: String, pass: String) -> Unit,
    onExit: () -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var host by remember { mutableStateOf(initialHost) }
    var user by remember { mutableStateOf(initialUser) }
    var pass by remember { mutableStateOf(initialPass) }
    var status by remember { mutableStateOf<String?>(null) }
    var channels by remember { mutableStateOf<List<XtreamLiveStream>>(emptyList()) }

    val scope = rememberCoroutineScope()

    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "SETTINGS",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = PhosphorGreen,
                )
            }

            // ---------- Xtream Codes ----------
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "XTREAM CODES",
                    style = sectionStyle(),
                    color = PhosphorGreen,
                )
            }
            item {
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        onCredsChange(it, user, pass)
                    },
                    label = { Text("Host (e.g. http://provider.com:8080)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = cathodeFieldColors(),
                )
            }
            item {
                OutlinedTextField(
                    value = user,
                    onValueChange = {
                        user = it
                        onCredsChange(host, it, pass)
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = cathodeFieldColors(),
                )
            }
            item {
                OutlinedTextField(
                    value = pass,
                    onValueChange = {
                        pass = it
                        onCredsChange(host, user, it)
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = cathodeFieldColors(),
                )
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            status = "Fetching channels…"
                            channels = emptyList()
                            try {
                                val list = XtreamApi.fetchLiveStreams(host, user, pass)
                                channels = list
                                status = "Found ${list.size} channels — tap one to play"
                            } catch (t: Throwable) {
                                status = "Error: ${t.message ?: t::class.simpleName}"
                            }
                        }
                    },
                    enabled = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                    colors = cathodeButtonColors(),
                ) {
                    Text("FETCH CHANNELS", fontWeight = FontWeight.Bold)
                }
            }
            status?.let { s ->
                item {
                    Text(
                        text = s,
                        style = monoSmall(),
                        color = if (s.startsWith("Error")) PhosphorGreenDim else OffWhite,
                    )
                }
            }
            items(channels.take(50), key = { it.streamId }) { ch ->
                Button(
                    onClick = {
                        onPlay(XtreamApi.buildLiveStreamUrl(host, user, pass, ch.streamId))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = cathodeButtonColors(),
                ) {
                    Text(
                        text = ch.name.ifBlank { "Channel ${ch.streamId}" },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ---------- Direct URL ----------
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "OR PASTE A DIRECT URL",
                    style = sectionStyle(),
                    color = PhosphorGreen,
                )
            }
            item {
                Text(
                    text = "HLS / DASH / MPEG-TS — Apple Bip-Bop default works without an account.",
                    style = monoSmall(),
                    color = OffWhite,
                )
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        onUrlChange(it)
                    },
                    label = { Text("Stream URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = cathodeFieldColors(),
                )
            }
            item {
                Button(
                    onClick = { onPlay(url.trim()) },
                    enabled = url.isNotBlank(),
                    colors = cathodeButtonColors(),
                ) {
                    Text("PLAY URL", fontWeight = FontWeight.Bold)
                }
            }
        }
        ScanlineOverlay()
    }
}

private fun sectionStyle() = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    letterSpacing = 1.sp,
)

private fun monoSmall() = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
)

@Composable
private fun cathodeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PhosphorGreen,
    unfocusedBorderColor = PhosphorGreenDim,
    focusedTextColor = OffWhite,
    unfocusedTextColor = OffWhite,
    focusedLabelColor = PhosphorGreen,
    unfocusedLabelColor = PhosphorGreenDim,
    cursorColor = PhosphorGreen,
)

@Composable
private fun cathodeButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PhosphorGreen,
    contentColor = Void,
    disabledContainerColor = PhosphorGreenDim,
    disabledContentColor = OffWhite,
)
