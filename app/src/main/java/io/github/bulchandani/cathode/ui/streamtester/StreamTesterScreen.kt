package io.github.bulchandani.cathode.ui.streamtester

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.BuildConfig
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.data.xtream.XtreamLiveStream
import io.github.bulchandani.cathode.update.UpdateChecker
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
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
    var statusIsError by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf<List<XtreamLiveStream>>(emptyList()) }
    var updateStatus by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "SETTINGS",
                style = CathodeText.Display,
                color = PhosphorGreen,
            )

            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.65f),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                // Left: Xtream form
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "XTREAM CODES",
                        style = CathodeText.Section,
                        color = PhosphorGreen,
                    )
                    CathodeField(
                        label = "Host",
                        value = host,
                        onValueChange = {
                            host = it
                            onCredsChange(it, user, pass)
                        },
                        placeholder = "http://provider.example.com:8080",
                    )
                    CathodeField(
                        label = "Username",
                        value = user,
                        onValueChange = {
                            user = it
                            onCredsChange(host, it, pass)
                        },
                    )
                    CathodeField(
                        label = "Password",
                        value = pass,
                        onValueChange = {
                            pass = it
                            onCredsChange(host, user, it)
                        },
                        password = true,
                    )
                    CathodeButton(
                        text = "FETCH CHANNELS",
                        onClick = {
                            scope.launch {
                                status = "Fetching channels…"
                                statusIsError = false
                                channels = emptyList()
                                try {
                                    val list = XtreamApi.fetchLiveStreams(host, user, pass)
                                    channels = list
                                    status = "Found ${list.size} channels — pick one on the right"
                                    statusIsError = false
                                } catch (t: Throwable) {
                                    status = "Error: ${t.message ?: t::class.simpleName}"
                                    statusIsError = true
                                }
                            }
                        },
                        enabled = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                    )
                    status?.let { s ->
                        Text(
                            text = s,
                            style = CathodeText.Data,
                            color = if (statusIsError) AlarmRed else Amber,
                        )
                    }
                }

                // Right: Channel list
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "CHANNELS",
                        style = CathodeText.Section,
                        color = if (channels.isEmpty()) PhosphorGreenDim else PhosphorGreen,
                    )
                    if (channels.isEmpty()) {
                        Text(
                            text = "Fetch with credentials on the left.",
                            style = CathodeText.Data,
                            color = PhosphorGreenDim,
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            items(channels.take(200), key = { it.streamId }) { ch ->
                                ChannelRow(
                                    name = ch.name.ifBlank { "Channel ${ch.streamId}" },
                                    streamId = ch.streamId,
                                    onClick = {
                                        onPlay(
                                            XtreamApi.buildLiveStreamUrl(
                                                host, user, pass, ch.streamId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "—  OR PASTE A DIRECT URL  —",
                style = CathodeText.Section,
                color = PhosphorGreenDim,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CathodeField(
                        label = "Stream URL",
                        value = url,
                        onValueChange = {
                            url = it
                            onUrlChange(it)
                        },
                        placeholder = "https://…/master.m3u8",
                    )
                }
                CathodeButton(
                    text = "PLAY",
                    onClick = { onPlay(url.trim()) },
                    enabled = url.isNotBlank(),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("—  ABOUT  —", style = CathodeText.Section, color = PhosphorGreenDim)
            Text("Cathode v${BuildConfig.VERSION_NAME}", style = CathodeText.Body, color = OffWhite)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CathodeButton(
                    text = "CHECK FOR UPDATES",
                    onClick = {
                        scope.launch {
                            updateStatus = "Checking…"
                            try {
                                val info = UpdateChecker.fetchLatest()
                                updateStatus = when {
                                    info == null -> "Couldn't reach GitHub Releases."
                                    UpdateChecker.isNewer(info.tagName) -> {
                                        val apk = UpdateChecker.downloadApk(context, info.apkUrl)
                                        UpdateChecker.installApk(context, apk)
                                        "Installing ${info.tagName}…"
                                    }
                                    else -> "You're on the latest (${info.tagName})."
                                }
                            } catch (t: Throwable) {
                                updateStatus = "Update error: ${t.message}"
                            }
                        }
                    },
                )
            }
            updateStatus?.let { Text(it, style = CathodeText.Data, color = Amber) }
        }

        CathodeScanlines()
        CathodeVignette()
    }
}

@Composable
private fun ChannelRow(name: String, streamId: Int, onClick: () -> Unit) {
    CathodeBox(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "%04d".format(streamId),
                style = CathodeText.Data,
                color = Amber,
                modifier = Modifier.width(56.dp),
            )
            Text(
                text = name,
                style = CathodeText.Body,
                color = OffWhite,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
