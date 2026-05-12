package io.github.bulchandani.cathode.ui.streamtester

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.BuildConfig
import io.github.bulchandani.cathode.data.device.DeviceCodecs
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.data.m3u.M3uIndex
import io.github.bulchandani.cathode.data.store.BufferProfile
import io.github.bulchandani.cathode.data.store.CrtMode
import io.github.bulchandani.cathode.data.store.SettingsStore
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.CathodeField
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void
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
    onOpenSourceManager: () -> Unit = {},
    onOpenPinSetup: () -> Unit = {},
    onOpenLogViewer: () -> Unit = {},
    onOpenHttpProbe: () -> Unit = {},
    onExit: () -> Unit,
) {
    var host by remember { mutableStateOf(initialHost) }
    var user by remember { mutableStateOf(initialUser) }
    var pass by remember { mutableStateOf(initialPass) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val crtMode by SettingsStore.crtMode
    val bufferProfile by SettingsStore.bufferProfile
    val channelSort by SettingsStore.channelSort
    val hasPin by SettingsStore.hasPin

    // Observe M3uIndex state directly so the MAINTENANCE row's status text
    // recomposes when the process-scope load completes.
    val m3uSize by M3uIndex.sizeState
    val m3uLoading by M3uIndex.loadingState
    val m3uError by M3uIndex.errorState
    var epgStatus by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("SETTINGS", style = CathodeText.Display, color = PhosphorGreen)

            // ---- ACCOUNT ----
            Section("ACCOUNT")
            Text(
                "Use https:// in the host if your provider runs on a secure port.",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            CathodeField(
                label = "Host",
                value = host,
                onValueChange = { host = it; onCredsChange(it, user, pass) },
                placeholder = "https://provider.example.com:443",
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
                    text = "SIGN IN",
                    onClick = {
                        scope.launch {
                            status = "Signing in…"
                            statusIsError = false
                            try {
                                val list = XtreamApi.fetchLiveStreams(host, user, pass)
                                onSaveSource(host, user, pass)
                                // Kick off M3U + EPG in the background — M3U load
                                // is now process-scoped (survives this screen).
                                M3uIndex.trigger(host, user, pass, force = true)
                                scope.launch { EpgRepo.load(host, user, pass, force = true) }
                                status = "OK · ${list.size} live channels available"
                                statusIsError = false
                            } catch (t: Throwable) {
                                status = "Error: ${t.message ?: t::class.simpleName}"
                                statusIsError = true
                            }
                        }
                    },
                    enabled = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                )
                CathodeButton(text = "MANAGE SOURCES", onClick = onOpenSourceManager)
            }
            status?.let { s ->
                Text(s, style = CathodeText.Data, color = if (statusIsError) AlarmRed else Amber)
            }

            // ---- DISPLAY ----
            Spacer(Modifier.height(8.dp))
            Section("DISPLAY")
            Text("CRT mode", style = CathodeText.Caption, color = PhosphorGreenDim)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceChip("FULL VINTAGE", selected = crtMode == CrtMode.FullVintage) {
                    SettingsStore.setCrtMode(CrtMode.FullVintage)
                }
                ChoiceChip("MODERATE", selected = crtMode == CrtMode.Moderate) {
                    SettingsStore.setCrtMode(CrtMode.Moderate)
                }
                ChoiceChip("MODERN DARK", selected = crtMode == CrtMode.ModernDark) {
                    SettingsStore.setCrtMode(CrtMode.ModernDark)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Channel sort (Live TV)", style = CathodeText.Caption, color = PhosphorGreenDim)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceChip(
                    "BY NUMBER",
                    selected = channelSort == io.github.bulchandani.cathode.data.store.ChannelSort.ByNumber,
                ) {
                    SettingsStore.setChannelSort(io.github.bulchandani.cathode.data.store.ChannelSort.ByNumber)
                }
                ChoiceChip(
                    "BY NAME",
                    selected = channelSort == io.github.bulchandani.cathode.data.store.ChannelSort.ByName,
                ) {
                    SettingsStore.setChannelSort(io.github.bulchandani.cathode.data.store.ChannelSort.ByName)
                }
            }

            // ---- PLAYBACK ----
            Spacer(Modifier.height(8.dp))
            Section("PLAYBACK")
            Text("Buffer profile", style = CathodeText.Caption, color = PhosphorGreenDim)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceChip("LOW LATENCY", selected = bufferProfile == BufferProfile.LowLatency) {
                    SettingsStore.setBufferProfile(BufferProfile.LowLatency)
                }
                ChoiceChip("DEFAULT", selected = bufferProfile == BufferProfile.Default) {
                    SettingsStore.setBufferProfile(BufferProfile.Default)
                }
                ChoiceChip("ROBUST", selected = bufferProfile == BufferProfile.Robust) {
                    SettingsStore.setBufferProfile(BufferProfile.Robust)
                }
            }
            Text(
                "Default works for most cases. Low latency cuts buffer for live sports. Robust adds headroom for flaky networks.",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )

            // ---- PARENTAL ----
            Spacer(Modifier.height(8.dp))
            Section("PARENTAL")
            Text(
                if (hasPin) "PIN is SET — you'll be asked when entering Settings."
                else "No PIN set — Settings is unlocked.",
                style = CathodeText.Caption,
                color = if (hasPin) Amber else PhosphorGreenDim,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CathodeButton(text = if (hasPin) "CHANGE PIN" else "SET PIN", onClick = onOpenPinSetup)
                if (hasPin) CathodeButton(text = "CLEAR PIN", onClick = {
                    SettingsStore.clearPin()
                    Toaster.show("PIN removed")
                })
            }

            // ---- MAINTENANCE ----
            // Catalog / EPG refreshes that used to live in the Live TV header.
            // Putting them here keeps Live TV header pure-text so D-pad
            // navigation can move straight from sidebar to content rows on
            // Fire TV (header buttons were trapping focus).
            Spacer(Modifier.height(8.dp))
            Section("MAINTENANCE")
            val m3uLabel = when {
                m3uLoading -> "M3U: loading…"
                m3uError != null && m3uSize == 0 -> "M3U: error — $m3uError"
                m3uSize == 0 -> "M3U: not loaded"
                else -> "M3U: $m3uSize channels indexed"
            }
            Text(m3uLabel, style = CathodeText.Caption, color = if (m3uSize > 0) Amber else PhosphorGreenDim)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CathodeButton(
                    text = "RETRY M3U",
                    enabled = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                    onClick = {
                        M3uIndex.trigger(host, user, pass, force = true)
                        Toaster.show("M3U refresh started…")
                    },
                )
                CathodeButton(
                    text = "REFRESH EPG",
                    enabled = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
                    onClick = {
                        scope.launch {
                            epgStatus = "Refreshing EPG…"
                            try {
                                EpgRepo.load(host, user, pass, force = true)
                                val err = EpgRepo.lastErrorMessage()
                                epgStatus = if (err != null) "EPG error: $err"
                                else "EPG refreshed (${if (EpgRepo.isReady()) "ready" else "empty"})"
                            } catch (t: Throwable) {
                                epgStatus = "EPG error: ${t.message ?: t::class.simpleName}"
                            }
                        }
                    },
                )
            }
            epgStatus?.let {
                Text(it, style = CathodeText.Caption, color = if (it.startsWith("EPG error")) AlarmRed else Amber)
            }

            // ---- TOOLS ----
            Spacer(Modifier.height(8.dp))
            Section("TOOLS")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CathodeButton(text = "TEST A DIRECT URL", onClick = onOpenUrlTester)
                CathodeButton(text = "HTTP PROBE", onClick = onOpenHttpProbe)
                CathodeButton(text = "LOG VIEWER", onClick = onOpenLogViewer)
            }

            // ---- DEVICE DECODERS ----
            // Quick summary of what this device's video decoders advertise.
            // The point: "format exceeds capabilities" errors stop being
            // mysterious — you can see right here whether you have HEVC at
            // all, whether it goes to 4K, and whether 10-bit is on the list.
            Spacer(Modifier.height(8.dp))
            Section("DEVICE DECODERS")
            val codecSummary = remember { runCatching { DeviceCodecs.summary() }.getOrNull() }
            Text(
                text = codecSummary ?: "(codec query unavailable on this device)",
                style = CathodeText.Body,
                color = OffWhite,
            )

            // ---- ABOUT ----
            Spacer(Modifier.height(8.dp))
            Section("ABOUT")
            Text("Cathode v${BuildConfig.VERSION_NAME}", style = CathodeText.Body, color = OffWhite)
            Text(
                "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})",
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun Section(label: String) {
    Text("—  $label  —", style = CathodeText.Section, color = PhosphorGreenDim)
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Three visual states: focused (D-pad), selected (already chosen), resting.
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val bg = when {
        selected -> PhosphorGreen
        focused -> PhosphorGreen.copy(alpha = 0.25f)
        else -> DimGrey
    }
    val labelColor = when {
        selected -> Void
        focused -> PhosphorGreen
        else -> PhosphorGreen
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(
                width = if (focused && !selected) 2.dp else 0.dp,
                color = if (focused && !selected) PhosphorGreen else Color.Transparent,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            // No explicit .focusable() — clickable provides one.
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = CathodeText.Caption,
            color = labelColor,
        )
    }
}
