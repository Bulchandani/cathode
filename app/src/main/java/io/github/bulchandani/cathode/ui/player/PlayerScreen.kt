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
import io.github.bulchandani.cathode.player.CathodePlayerFactory
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerScreen(
    streamUrl: String,
    channelLabel: String = streamUrl.substringAfterLast('/').take(40),
    epgChannelId: String = "",
    onExit: () -> Unit,
) {
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

    val context = LocalContext.current
    val player = remember { CathodePlayerFactory.create(context) }

    var status by remember { mutableStateOf("Connecting…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var videoFormat by remember { mutableStateOf<Format?>(null) }
    var audioFormat by remember { mutableStateOf<Format?>(null) }
    var clockTick by remember { mutableStateOf(0) }
    var overlayVisible by remember { mutableStateOf(true) }

    DisposableEffect(streamUrl) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                status = when (state) {
                    Player.STATE_IDLE -> "Idle"
                    Player.STATE_BUFFERING -> "Buffering…"
                    Player.STATE_READY -> "Playing"
                    Player.STATE_ENDED -> "Ended"
                    else -> "Unknown"
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                errorMessage = error.errorCodeName + ": " + (error.message ?: "")
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
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            clockTick++
            delay(1_000)
        }
    }

    // Auto-hide overlay 5 seconds after the last interaction
    LaunchedEffect(overlayVisible) {
        if (overlayVisible) {
            delay(5_000)
            overlayVisible = false
        }
    }

    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { overlayVisible = !overlayVisible },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
        )

        if (overlayVisible) {
            // Top chrome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = channelLabel,
                    style = CathodeText.Section,
                    color = PhosphorGreen,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = clockString(clockTick),
                    style = CathodeText.Headline,
                    color = Amber,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onExit)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "✕  BACK",
                        style = CathodeText.Section,
                        color = PhosphorGreen,
                    )
                }
            }

            // Bottom chrome
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    LabeledStat("NOW", nowText)
                    LabeledStat("NEXT", nextText)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    LabeledStat("CODEC", videoFormat?.codecs ?: videoFormat?.sampleMimeType ?: "—")
                    LabeledStat("RES", if (videoSize == VideoSize.UNKNOWN) "—" else "${videoSize.width}×${videoSize.height}")
                    LabeledStat("FPS", videoFormat?.frameRate?.takeIf { it > 0 }?.let { "%.2f".format(it) } ?: "—")
                    LabeledStat("BITRATE", videoFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" } ?: "—")
                    LabeledStat("AUDIO", audioFormat?.codecs ?: audioFormat?.sampleMimeType ?: "—")
                }
            }
        }

        // Status / error always visible during connect
        val showStatus by remember {
            derivedStateOf { errorMessage != null || status != "Playing" }
        }
        if (showStatus) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = errorMessage ?: status,
                    style = CathodeText.Section,
                    color = if (errorMessage != null) AlarmRed else PhosphorGreen,
                )
            }
        }
    }
}

@Composable
private fun LabeledStat(label: String, value: String) {
    Column {
        Text(text = label, style = CathodeText.Caption, color = PhosphorGreenDim)
        Text(text = value, style = CathodeText.Data, color = OffWhite)
    }
}

private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun clockString(@Suppress("UNUSED_PARAMETER") tick: Int): String =
    clockFormat.format(Date())
