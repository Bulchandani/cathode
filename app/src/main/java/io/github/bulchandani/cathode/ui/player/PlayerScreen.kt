@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package io.github.bulchandani.cathode.ui.player

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.data.store.SettingsStore
import io.github.bulchandani.cathode.log.Logger
import io.github.bulchandani.cathode.player.CathodePlayer
import io.github.bulchandani.cathode.ui.components.CathodeButton
import io.github.bulchandani.cathode.ui.components.Toaster
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void
import kotlinx.coroutines.delay
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "Player"
private const val MAX_AUTO_RETRIES = 3
private val RETRY_BACKOFF_MS = longArrayOf(2_000L, 4_000L, 8_000L)

@Composable
fun PlayerScreen(
    streamUrl: String,
    channelLabel: String = streamUrl.substringAfterLast('/').take(40),
    epgChannelId: String = "",
    onExit: () -> Unit,
    onLastChannel: (() -> Unit)? = null,
    onJumpToChannelNumber: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val mediaPlayer = remember { CathodePlayer.createMediaPlayer(context) }

    val urlCandidates = remember(streamUrl) { buildUrlCandidates(streamUrl) }
    var candidateIndex by remember(streamUrl) { mutableStateOf(0) }
    val currentUrl = urlCandidates.getOrElse(candidateIndex) { streamUrl }
    var retryAttempt by remember(streamUrl) { mutableStateOf(0) }
    var status by remember { mutableStateOf("Connecting…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var overlayVisible by remember { mutableStateOf(true) }
    var sleepDialogOpen by remember { mutableStateOf(false) }
    var syncDialogOpen by remember { mutableStateOf(false) }
    var numPadOpen by remember { mutableStateOf(false) }
    var sleepEndsAt by remember { mutableStateOf<Long?>(null) }

    var audioSyncMs by remember(streamUrl) {
        val saved = SettingsStore.audioSyncFor(streamUrl)
        mutableStateOf(saved)
    }

    // libVLC reports video size, codec via media tracks. Cache them so OSD
    // doesn't have to re-query.
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }

    // Keep screen awake for entire PlayerScreen lifetime.
    val hostView = LocalView.current
    DisposableEffect(Unit) {
        hostView.keepScreenOn = true
        onDispose { hostView.keepScreenOn = false }
    }

    BackHandler { onExit() }

    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var clockNow by remember { mutableStateOf(clock.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            clockNow = clock.format(Date())
            delay(15_000L)
        }
    }

    // EPG: now / next program lookup.
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val (nowProgramme, nextProgramme) = remember(epgChannelId, EpgRepo.isReady()) {
        EpgRepo.nowAndNext(epgChannelId)
    }
    val nowText = nowProgramme?.let {
        "${timeFmt.format(Date(it.startMillis))}–${timeFmt.format(Date(it.stopMillis))}  ${it.title}"
    } ?: "—"
    val nextText = nextProgramme?.let {
        "${timeFmt.format(Date(it.startMillis))}  ${it.title}"
    } ?: "—"

    // Apply audio-sync delta to the MediaPlayer. libVLC takes microseconds.
    LaunchedEffect(audioSyncMs) {
        mediaPlayer.setAudioDelay(audioSyncMs * 1000L)
    }

    // Wire the playback engine: listener for state/errors + media set.
    DisposableEffect(currentUrl) {
        Logger.i(TAG, "play: $currentUrl  (candidate ${candidateIndex + 1}/${urlCandidates.size})")
        val listener = MediaPlayer.EventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Buffering -> {
                    if (event.buffering < 100f) status = "Buffering ${event.buffering.toInt()}%"
                }
                MediaPlayer.Event.Playing -> {
                    status = "Playing"
                    retryAttempt = 0
                    errorMessage = null
                }
                MediaPlayer.Event.Paused -> status = "Paused"
                MediaPlayer.Event.Stopped -> status = "Stopped"
                MediaPlayer.Event.EndReached -> status = "Ended"
                MediaPlayer.Event.EncounteredError -> {
                    errorMessage = "Playback error on candidate ${candidateIndex + 1}"
                    Logger.w(TAG, "EncounteredError on $currentUrl")
                    // Inline recovery — advance candidate then back off.
                    if (candidateIndex < urlCandidates.size - 1) {
                        candidateIndex += 1
                    } else if (retryAttempt < MAX_AUTO_RETRIES) {
                        retryAttempt += 1
                    }
                }
                MediaPlayer.Event.Vout -> {
                    val m = mediaPlayer.media ?: return@EventListener
                    for (i in 0 until m.trackCount) {
                        val t = m.getTrack(i) ?: continue
                        if (t.type == IMedia.Track.Type.Video) {
                            (t as? IMedia.VideoTrack)?.let {
                                videoWidth = it.width
                                videoHeight = it.height
                            }
                            break
                        }
                    }
                }
            }
        }
        mediaPlayer.setEventListener(listener)

        val media = Media(CathodePlayer.libvlc(context), Uri.parse(currentUrl))
        // Per-media options. Refresh from settings on each (re)attach so a
        // BufferProfile change in Settings takes effect on the next load.
        val cachingMs = CathodePlayer.networkCachingMs(SettingsStore.bufferProfile.value)
        media.addOption(":network-caching=$cachingMs")
        media.addOption(":live-caching=$cachingMs")
        media.addOption(":http-user-agent=Lavf/58.45.100")
        media.addOption(":http-reconnect")
        mediaPlayer.media = media
        media.release()  // MediaPlayer holds its own ref
        mediaPlayer.play()

        onDispose {
            mediaPlayer.setEventListener(null)
            mediaPlayer.stop()
        }
    }

    // Final release of the MediaPlayer happens once when the screen leaves
    // composition (not on every URL change).
    DisposableEffect(Unit) {
        onDispose {
            Logger.d(TAG, "release mediaPlayer")
            mediaPlayer.release()
        }
    }

    LaunchedEffect(retryAttempt, currentUrl) {
        if (retryAttempt in 1..MAX_AUTO_RETRIES) {
            val wait = RETRY_BACKOFF_MS.getOrNull(retryAttempt - 1) ?: 8_000L
            status = "Retrying in ${wait / 1000}s…"
            delay(wait)
            status = "Reconnecting…"
            val media = Media(CathodePlayer.libvlc(context), Uri.parse(currentUrl))
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
        }
    }

    // Sleep timer.
    LaunchedEffect(sleepEndsAt) {
        val end = sleepEndsAt ?: return@LaunchedEffect
        val wait = end - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        mediaPlayer.pause()
        Toaster.show("Sleep timer reached — paused.")
    }

    // Auto-hide overlay 3s after playback steady-state.
    LaunchedEffect(status) {
        when {
            status.startsWith("Buffering") -> overlayVisible = true
            status == "Playing" -> {
                delay(3_000L)
                overlayVisible = false
            }
            errorMessage != null -> overlayVisible = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // The libVLC render surface. AndroidView wraps a VLCVideoLayout
        // which provides the SurfaceView libVLC writes frames into.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VLCVideoLayout(ctx).also { layout ->
                    mediaPlayer.attachViews(layout, null, false, false)
                }
            },
            onRelease = { mediaPlayer.detachViews() },
        )

        // Tap anywhere on the surface (D-pad SELECT on TV) to toggle the
        // overlay. Done with a thin focusable Surface from tv-material3.
        @OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
        androidx.tv.material3.Surface(
            onClick = { overlayVisible = !overlayVisible },
            modifier = Modifier.fillMaxSize(),
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(
                shape = androidx.compose.ui.graphics.RectangleShape,
            ),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            border = androidx.tv.material3.ClickableSurfaceDefaults.border(
                border = androidx.tv.material3.Border.None,
                focusedBorder = androidx.tv.material3.Border.None,
            ),
            scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
        ) { /* nothing visible — just a focus target for SELECT */ }

        // Top chrome — channel label, clock, exit.
        if (overlayVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    androidx.tv.material3.Text(
                        text = channelLabel,
                        style = CathodeText.Section,
                        color = PhosphorGreen,
                    )
                    androidx.tv.material3.Text(
                        text = "Now: $nowText",
                        style = CathodeText.Caption,
                        color = OffWhite,
                    )
                    androidx.tv.material3.Text(
                        text = "Next: $nextText",
                        style = CathodeText.Caption,
                        color = PhosphorGreenDim,
                    )
                }
                androidx.tv.material3.Text(
                    text = clockNow,
                    style = CathodeText.Display,
                    color = Amber,
                )
                Spacer(Modifier.width(12.dp))
                CathodeButton(text = "BACK", onClick = onExit)
            }
        }

        // Bottom chrome — OSD chips + stats.
        if (overlayVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OsdChip(
                        "SLEEP" + (sleepEndsAt?.let {
                            " · ${(it - System.currentTimeMillis()) / 60_000}m"
                        } ?: ""),
                    ) { sleepDialogOpen = true }
                    OsdChip("SYNC ${if (audioSyncMs >= 0) "+${audioSyncMs}" else audioSyncMs}ms") {
                        syncDialogOpen = true
                    }
                    if (onLastChannel != null) OsdChip("LAST CH", onLastChannel)
                    OsdChip("COPY URL") {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as? android.content.ClipboardManager
                        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Cathode stream", currentUrl))
                        Toaster.show("URL copied")
                    }
                    OsdChip("EXTERNAL PLAYER") {
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse(currentUrl), "video/*")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                            .onFailure { Toaster.show("No external player installed") }
                    }
                    if (onJumpToChannelNumber != null) OsdChip("CH #") { numPadOpen = true }
                    Spacer(Modifier.weight(1f))
                    LabeledStat(
                        "RES",
                        if (videoWidth == 0) "—" else "${videoWidth}×${videoHeight}",
                    )
                    LabeledStat(
                        "URL",
                        if (currentUrl.length > 60) "…" + currentUrl.takeLast(59) else currentUrl,
                    )
                }
            }
        }

        // Status / error overlay
        val showStatus by remember {
            derivedStateOf { errorMessage != null || (status != "Playing" && status != "Paused") }
        }
        if (showStatus) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    androidx.tv.material3.Text(
                        text = errorMessage ?: status,
                        style = CathodeText.Section,
                        color = if (errorMessage != null) AlarmRed else PhosphorGreen,
                    )
                    if (errorMessage != null) {
                        androidx.tv.material3.Text("URL: $currentUrl", style = CathodeText.Caption, color = OffWhite)
                        androidx.tv.material3.Text(
                            "Variant ${candidateIndex + 1}/${urlCandidates.size}  ·  Auto-retry $retryAttempt/$MAX_AUTO_RETRIES",
                            style = CathodeText.Caption,
                            color = PhosphorGreenDim,
                        )
                    }
                }
            }
        }
    }

    if (sleepDialogOpen) {
        SleepTimerDialog(
            current = sleepEndsAt,
            onPick = { minutes ->
                sleepEndsAt = if (minutes == 0) null else System.currentTimeMillis() + minutes * 60_000L
                sleepDialogOpen = false
                Toaster.show(if (minutes == 0) "Sleep timer off" else "Sleep timer ${minutes}m")
            },
            onDismiss = { sleepDialogOpen = false },
        )
    }
    if (syncDialogOpen) {
        AudioSyncDialog(
            currentMs = audioSyncMs,
            onChange = { newMs ->
                audioSyncMs = newMs
            },
            onApply = {
                syncDialogOpen = false
                SettingsStore.setAudioSync(streamUrl, audioSyncMs)
                Toaster.show("Audio sync ${if (audioSyncMs >= 0) "+" else ""}${audioSyncMs}ms — saved")
            },
            onDismiss = { syncDialogOpen = false },
        )
    }
    if (numPadOpen && onJumpToChannelNumber != null) {
        ChannelNumberDialog(
            onJump = { num ->
                numPadOpen = false
                onJumpToChannelNumber(num)
            },
            onDismiss = { numPadOpen = false },
        )
    }
}

@Composable
private fun OsdChip(label: String, onClick: () -> Unit) {
    CathodeButton(
        text = label,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp, vertical = 8.dp,
        ),
    )
}

@Composable
private fun LabeledStat(label: String, value: String) {
    Column {
        androidx.tv.material3.Text(text = label, style = CathodeText.Caption, color = PhosphorGreenDim)
        androidx.tv.material3.Text(text = value, style = CathodeText.Data, color = OffWhite, maxLines = 1)
    }
}

@Composable
private fun SleepTimerDialog(current: Long?, onPick: (minutes: Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismiss) {
        androidx.tv.material3.Text("SLEEP TIMER", style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.padding(top = 12.dp))
        listOf(0 to "OFF", 15 to "15 MIN", 30 to "30 MIN", 60 to "60 MIN", 90 to "90 MIN").forEach { (m, l) ->
            CathodeButton(
                text = l,
                onClick = { onPick(m) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun AudioSyncDialog(
    currentMs: Int,
    onChange: (Int) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismiss) {
        androidx.tv.material3.Text("AUDIO SYNC", style = CathodeText.Section, color = PhosphorGreen)
        androidx.tv.material3.Text(
            "Range −2000ms to +2000ms in 50ms steps. Applied immediately.",
            style = CathodeText.Caption,
            color = PhosphorGreenDim,
        )
        Spacer(Modifier.padding(top = 12.dp))
        androidx.tv.material3.Text(
            "${if (currentMs >= 0) "+" else ""}${currentMs}ms",
            style = CathodeText.Display,
            color = Amber,
        )
        Spacer(Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-500, -50, 50, 500).forEach { step ->
                CathodeButton(
                    text = "${if (step > 0) "+" else ""}$step",
                    onClick = {
                        val next = (currentMs + step).coerceIn(-2000, 2000)
                        onChange(next)
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 10.dp,
                    ),
                )
            }
            CathodeButton(
                text = "0",
                onClick = { onChange(0) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp, vertical = 10.dp,
                ),
            )
        }
        Spacer(Modifier.padding(top = 12.dp))
        CathodeButton(
            text = "APPLY",
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChannelNumberDialog(onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var digits by remember { mutableStateOf("") }
    Dialog(onDismiss) {
        androidx.tv.material3.Text("JUMP TO CHANNEL", style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.padding(top = 12.dp))
        androidx.tv.material3.Text(
            digits.ifEmpty { "—" },
            style = CathodeText.Display,
            color = Amber,
        )
        Spacer(Modifier.padding(top = 12.dp))
        // 3x4 number pad: 1 2 3 / 4 5 6 / 7 8 9 / DEL 0 GO
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("⌫", "0", "GO"),
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    CathodeButton(
                        text = key,
                        onClick = {
                            when (key) {
                                "⌫" -> if (digits.isNotEmpty()) digits = digits.dropLast(1)
                                "GO" -> digits.toIntOrNull()?.let(onJump)
                                else -> if (digits.length < 4) digits += key
                            }
                        },
                        modifier = Modifier.size(width = 80.dp, height = 56.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    )
                }
            }
            Spacer(Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * Variants we try in order when a stream URL is rejected. Original URL
 * is always first — M3U-derived URLs come in verbatim and the provider's
 * own URL is the most trustworthy starting point.
 *
 * For LIVE URLs we synthesize a `.ts` variant explicitly — most Xtream
 * providers prefer that form. If the M3U gave us an extensionless URL,
 * both `.ts` and `.m3u8` candidates are added. Query strings (?token=…)
 * are preserved across variants.
 */
private fun buildUrlCandidates(streamUrl: String): List<String> {
    val out = mutableListOf(streamUrl)
    val isLive = streamUrl.contains("/live/")
    val q = streamUrl.indexOf('?').let { if (it >= 0) streamUrl.substring(it) else "" }
    val noQuery = if (q.isNotEmpty()) streamUrl.removeSuffix(q) else streamUrl

    fun addCandidate(s: String) {
        val full = s + q
        if (full !in out) out += full
    }

    when {
        noQuery.endsWith(".m3u8", ignoreCase = true) -> {
            addCandidate(noQuery.removeSuffix(".m3u8") + ".ts")
        }
        noQuery.endsWith(".ts", ignoreCase = true) -> {
            addCandidate(noQuery.removeSuffix(".ts") + ".m3u8")
        }
        else -> {
            if (isLive) {
                addCandidate("$noQuery.ts")
                addCandidate("$noQuery.m3u8")
            }
        }
    }

    val noExt = noQuery.replace(Regex("""\.(m3u8|ts)$""", RegexOption.IGNORE_CASE), "")
    if (noExt != noQuery) addCandidate(noExt)

    if (isLive) {
        val hlsSwap = streamUrl.replace("/live/", "/hls/")
        if (hlsSwap !in out) out += hlsSwap
    }

    return out.distinct()
}

@Composable
private fun Dialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DimGrey)
                .padding(24.dp),
        ) { content() }
    }
}
