package io.github.bulchandani.cathode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.bulchandani.cathode.ui.hub.HubScreen
import io.github.bulchandani.cathode.ui.hub.HubTileId
import io.github.bulchandani.cathode.ui.player.PlayerScreen

/**
 * Public HLS test stream — Apple Bip-Bop. Stable, well-known, multi-bitrate.
 * Replace per-tile routing with real flows (Xtream onboarding, M3U import)
 * in subsequent commits.
 */
private const val TEST_HLS_URL =
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"

private sealed interface Screen {
    data object Hub : Screen
    data class Player(val url: String) : Screen
}

@Composable
fun App() {
    var current by remember { mutableStateOf<Screen>(Screen.Hub) }

    when (val screen = current) {
        Screen.Hub -> HubScreen(
            onTileClick = { tile ->
                when (tile) {
                    HubTileId.LiveTV -> current = Screen.Player(TEST_HLS_URL)
                    else -> Unit // wired in later commits
                }
            },
        )
        is Screen.Player -> PlayerScreen(
            streamUrl = screen.url,
            onExit = { current = Screen.Hub },
        )
    }
}
