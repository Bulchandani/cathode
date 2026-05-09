package io.github.bulchandani.cathode.ui.streamtester

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.update.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun StreamTesterScreen(
    initialHost: String,
    initialUser: String,
    initialPass: String,
    onPlay: (String) -> Unit,
    onCredsChange: (host: String, user: String, pass: String) -> Unit,
    onSaveSource: (host: String, user: String, pass: String) -> Unit = { _, _, _ -> },
    onOpenUrlTester: () -> Unit = {},
    onExit: () -> Unit,
) {
    var host by remember { mutableStateOf(initialHost) }
    var user by remember { mutableStateOf(initialUser) }
    var pass by remember { mutableStateOf(initialPass) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf<List<XtreamLiveStream>>(emptyList()) }
    var updateStatus by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("SETTINGS", style = CathodeText.Display, color = PhosphorGreen)

            Text("XTREAM CODES", style = CathodeText.Section, color = PhosphorGreen)
            CathodeField(
                label = "Host",
                value = host,
                onValueChange = { host = it; onCredsChange(it, user, pass) },
                placeholder = "http://provider.example.com:8080",
            )
            CathodeField(
                label = "Username",
                value = user,
                onValueChange = { user = it; onCredsChange(host, it, pass) },
            )
            CathodeField(
                label = "Password",
                value = pass,
                onValueChange = { pass = it; onCredsChange(host, user, it) },
                password = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                onSaveSource(host, user, pass)
                                status = "Found ${list.size} channels — saved as source"
                                statusIsError = false
                            } catch (t: Throwable) {
                                status = "Error: ${t.message ?: t::class.simpleName}"
                                statusIsError = true
                            }
                        }
                    },
                    enabled = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                )
            }
            status?.let { s ->
                Text(s, style = CathodeText.Data, color = if (statusIsError) AlarmRed else Amber)
            }

            if (channels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("CHANNELS (${channels.size})", style = CathodeText.Section, color = PhosphorGreen)
                channels.take(50).forEach { ch ->
                    CathodeBox(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        onClick = { onPlay(XtreamApi.buildLiveStreamUrl(host, user, pass, ch.streamId)) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("%04d".format(ch.streamId), style = CathodeText.Data, color = Amber)
                            Text(
                                ch.name.ifBlank { "Channel ${ch.streamId}" },
                                style = CathodeText.Body,
                                color = OffWhite,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (channels.size > 50) {
                    Text(
                        "(showing first 50 of ${channels.size} — open Live TV for the full list)",
                        style = CathodeText.Caption,
                        color = PhosphorGreenDim,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("—  TOOLS  —", style = CathodeText.Section, color = PhosphorGreenDim)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CathodeButton(text = "TEST A DIRECT URL", onClick = onOpenUrlTester)
            }

            Spacer(Modifier.height(24.dp))
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
            Spacer(Modifier.height(48.dp))
        }
    }
}
