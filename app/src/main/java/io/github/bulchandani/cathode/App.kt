package io.github.bulchandani.cathode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.bulchandani.cathode.ui.hub.HubScreen
import io.github.bulchandani.cathode.ui.hub.HubTileId
import io.github.bulchandani.cathode.ui.player.PlayerScreen
import io.github.bulchandani.cathode.ui.streamtester.StreamTesterScreen

private const val TEST_HLS_URL =
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"

private sealed interface Screen {
    data object Hub : Screen
    data object StreamTester : Screen
    data class Player(val url: String, val from: Screen) : Screen
}

@Composable
fun App() {
    var current by remember { mutableStateOf<Screen>(Screen.Hub) }
    var lastUrl by remember { mutableStateOf(TEST_HLS_URL) }

    when (val screen = current) {
        Screen.Hub -> HubScreen(
            onTileClick = { tile ->
                current = when (tile) {
                    HubTileId.LiveTV -> Screen.Player(TEST_HLS_URL, Screen.Hub)
                    HubTileId.Settings -> Screen.StreamTester
                    else -> Screen.Hub
                }
            },
        )

        Screen.StreamTester -> StreamTesterScreen(
            initialUrl = lastUrl,
            onPlay = { url ->
                lastUrl = url
                current = Screen.Player(url, Screen.StreamTester)
            },
            onExit = { current = Screen.Hub },
        )

        is Screen.Player -> PlayerScreen(
            streamUrl = screen.url,
            onExit = { current = screen.from },
        )
    }
}
