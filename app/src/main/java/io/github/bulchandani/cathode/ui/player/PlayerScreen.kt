package io.github.bulchandani.cathode.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.player.AudioSyncState
import io.github.bulchandani.cathode.player.CathodePlayerFactory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_AUTO_RETRIES = 3
private val RETRY_BACKOFF_MS = longArrayOf(1_000, 3_000, 8_000)

@Composable
fun PlayerScreen(
    streamUrl: String,
    channelLabel: String = streamUrl.substringAfterLast('/').take(40),
    epgChannelId: String = "",
    onExit: () -> Unit,
    onLastChannel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val player = remember { CathodePlayerFactory.create(context) }

    var currentUrl by remember(streamUrl) { mutableStateOf(streamUrl) }
    var triedTsFallback by remember(streamUrl) { mutableStateOf(!streamUrl.endsWith(".m3u8", true)) }
    var retryAttempt by remember(streamUrl) { mutableStateOf(0) }
    var status by remember { mutableStateOf("Connecting…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var videoFormat by remember { mutableStateOf<Format?>(null) }
    var audioFormat by remember { mutableStateOf<Format?>(null) }
    var clockTick by remember { mutableStateOf(0) }

    var sleepDialogOpen by remember { mutableStateOf(false) }
    var syncDialogOpen by remember { mutableStateOf(false) }
    var sleepEndsAt by remember { mutableStateOf<Long?>(null) }
    var audioSyncMs by remember { mutableStateOf(AudioSyncState.offsetMs) }

    val (nowProgramme, nextProgramme) = remember(epgChannelId, EpgRepo.isReady()) {
        EpgRepo.nowAndNext(epgChannelId)
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nowText = nowProgramme?.let {
        "${timeFmt.format(Date(it.startMillis))}–${timeFmt.format(Date(it.stopMillis))}  ${it.title}"
    } ?: "—"
    val nextText = nextProgramme?.let {
        "${timeFmt.format(Date(it.startMillis))}  ${it.title}"
    } ?: "—"

    DisposableEffect(currentUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                status = when (state) {
                    Player.STATE_IDLE -> "Idle"
                    Player.STATE_BUFFERING -> "Buffering…"
                    Player.STATE_READY -> { retryAttempt = 0; errorMessage = null; "Playing" }
                    Player.STATE_ENDED -> "Ended"
                    else -> "Unknown"
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                val httpCode = (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode
                val codeName = error.errorCodeName
                val detail = listOfNotNull(
                    httpCode?.let { "HTTP $it" },
                    error.message?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                errorMessage = "$codeName · $detail"
                tryRecover(player)
            }
            override fun onVideoSizeChanged(size: VideoSize) { videoSize = size }
            override fun onTracksChanged(tracks: Tracks) {
                tracks.groups.forEach { group ->
                    for (i in 0 until group.length) {
                        if (!group.isTrackSelected(i)) continue
                        val fmt = group.getTrackFormat(i)
                        if (fmt.sampleMimeType?.startsWith("video/") == true) videoFormat = fmt
                        if (fmt.sampleMimeType?.startsWith("audio/") == true) audioFormat = fmt
                    }
                }
            }
            private fun tryRecover(p: ExoPlayer) {
                if (currentUrl.endsWith(".m3u8", ignoreCase = true) && !triedTsFallback) {
                    triedTsFallback = true
                    currentUrl = currentUrl.removeSuffix(".m3u8") + ".ts"
                    return
                }
                if (retryAttempt >= MAX_AUTO_RETRIES) return
                retryAttempt += 1
                p.playWhenReady = false
            }
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(currentUrl))
        player.prepare()
        player.playWhenReady = true
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(retryAttempt, currentUrl) {
        if (retryAttempt in 1..MAX_AUTO_RETRIES) {
            val wait = RETRY_BACKOFF_MS.getOrNull(retryAttempt - 1) ?: 8_000L
            status = "Retrying in ${wait / 1000}s…"
            delay(wait)
            status = "Reconnecting…"
            player.setMediaItem(MediaItem.fromUri(currentUrl))
            player.prepare()
            player.playWhenReady = true
        }
    }

    LaunchedEffect(Unit) { while (true) { clockTick++; delay(1_000) } }

    LaunchedEffect(sleepEndsAt) {
        val end = sleepEndsAt ?: return@LaunchedEffect
        while (System.currentTimeMillis() < end) delay(1000)
        player.pause()
        Toaster.show("Sleep timer reached — paused.")
    }

    BackHandler(onBack = onExit)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 5_000
                    setControllerHideOnTouch(true)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
        )

        // Top chrome — channel, clock, back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(channelLabel, style = CathodeText.Section, color = PhosphorGreen, modifier = Modifier.weight(1f))
            Text(timeFmt.format(Date()), style = CathodeText.Headline, color = Amber)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = onExit)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("✕  BACK", style = CathodeText.Section, color = PhosphorGreen)
            }
        }

        // Programme + chip strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LabeledStat("NOW", nowText)
                LabeledStat("NEXT", nextText)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OsdChip("SLEEP" + (sleepEndsAt?.let { " · ${(it - System.currentTimeMillis()) / 60_000}m" } ?: "")) { sleepDialogOpen = true }
                if (onLastChannel != null) OsdChip("LAST CHANNEL") { onLastChannel() }
                OsdChip("SYNC ${if (audioSyncMs >= 0) "+${audioSyncMs}" else audioSyncMs}ms") { syncDialogOpen = true }
                Spacer(Modifier.weight(1f))
                LabeledStat("CODEC", videoFormat?.codecs ?: videoFormat?.sampleMimeType ?: "—")
                LabeledStat("RES", if (videoSize == VideoSize.UNKNOWN) "—" else "${videoSize.width}×${videoSize.height}")
                LabeledStat("BITRATE", videoFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" } ?: "—")
            }
        }

        // Status overlay during connect/error
        val showStatus by remember { derivedStateOf { errorMessage != null || status != "Playing" } }
        if (showStatus) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = errorMessage ?: status,
                        style = CathodeText.Section,
                        color = if (errorMessage != null) AlarmRed else PhosphorGreen,
                    )
                    if (errorMessage != null) {
                        Text("URL: $currentUrl", style = CathodeText.Caption, color = OffWhite)
                        Text("Auto-retry $retryAttempt/$MAX_AUTO_RETRIES", style = CathodeText.Caption, color = PhosphorGreenDim)
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
                AudioSyncState.offsetMs = newMs
            },
            onApply = {
                Toaster.show("Audio sync ${if (audioSyncMs >= 0) "+" else ""}${audioSyncMs}ms — re-prep stream")
                player.setMediaItem(MediaItem.fromUri(currentUrl))
                player.prepare()
                player.playWhenReady = true
                syncDialogOpen = false
            },
            onDismiss = { syncDialogOpen = false },
        )
    }
}

@Composable
private fun OsdChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DimGrey)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = CathodeText.Caption, color = PhosphorGreen)
    }
}

@Composable
private fun LabeledStat(label: String, value: String) {
    Column {
        Text(text = label, style = CathodeText.Caption, color = PhosphorGreenDim)
        Text(text = value, style = CathodeText.Data, color = OffWhite, maxLines = 1)
    }
}

@Composable
private fun SleepTimerDialog(current: Long?, onPick: (minutes: Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismiss) {
        Text("SLEEP TIMER", style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.padding(top = 12.dp))
        listOf(0 to "Off", 15 to "15 min", 30 to "30 min", 60 to "60 min", 90 to "90 min").forEach { (m, l) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(DimGrey)
                    .clickable { onPick(m) }
                    .padding(12.dp),
            ) { Text(l, style = CathodeText.Body, color = OffWhite) }
            Spacer(Modifier.padding(top = 4.dp))
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
        Text("AUDIO SYNC", style = CathodeText.Section, color = PhosphorGreen)
        Text("Range −2000ms to +2000ms in 50ms steps. Applied on next prepare.",
            style = CathodeText.Caption, color = PhosphorGreenDim)
        Spacer(Modifier.padding(top = 12.dp))
        Text("${if (currentMs >= 0) "+" else ""}${currentMs}ms", style = CathodeText.Display, color = Amber)
        Spacer(Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-500, -50, 50, 500).forEach { step ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DimGrey)
                        .clickable {
                            val next = (currentMs + step).coerceIn(-2000, 2000)
                            onChange(next)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text("${if (step > 0) "+" else ""}${step}", style = CathodeText.Body, color = PhosphorGreen) }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DimGrey)
                    .clickable { onChange(0) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) { Text("0", style = CathodeText.Body, color = PhosphorGreenDim) }
        }
        Spacer(Modifier.padding(top = 12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(PhosphorGreen)
                .clickable(onClick = onApply)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) { Text("APPLY", style = CathodeText.Section, color = Void) }
    }
}

@Composable
private fun Dialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
        ) { content() }
    }
}
