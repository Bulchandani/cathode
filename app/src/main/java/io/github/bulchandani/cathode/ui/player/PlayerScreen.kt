package io.github.bulchandani.cathode.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.player.CathodePlayerFactory

@Composable
fun PlayerScreen(streamUrl: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val player = remember { CathodePlayerFactory.create(context) }

    var status by remember { mutableStateOf("Connecting…") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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

        // Status text — top-left, phosphor green
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = errorMessage ?: status,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // BACK button — top-right, always visible (for tablets with no
        // visible system nav bar). Fire TV remote Back also works via
        // BackHandler above; this is the touch fallback.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onExit)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "✕  BACK",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
