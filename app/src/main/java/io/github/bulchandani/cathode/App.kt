package io.github.bulchandani.cathode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.bulchandani.cathode.data.store.CredsStore
import io.github.bulchandani.cathode.ui.hub.HubScreen
import io.github.bulchandani.cathode.ui.hub.HubTileId
import io.github.bulchandani.cathode.ui.live.LiveTvScreen
import io.github.bulchandani.cathode.ui.player.PlayerScreen
import io.github.bulchandani.cathode.ui.streamtester.StreamTesterScreen

private sealed interface Screen {
    data object Hub : Screen
    data object StreamTester : Screen
    data object LiveTv : Screen
    data class Player(val url: String, val label: String, val epgChannelId: String, val from: Screen) : Screen
}

@Composable
fun App() {
    val context = LocalContext.current
    val creds = remember { CredsStore(context) }

    var current by remember { mutableStateOf<Screen>(Screen.Hub) }
    var directUrl by remember { mutableStateOf(creds.lastDirectUrl) }
    var host by remember { mutableStateOf(creds.host) }
    var user by remember { mutableStateOf(creds.user) }
    var pass by remember { mutableStateOf(creds.pass) }

    when (val screen = current) {
        Screen.Hub -> HubScreen(
            onTileClick = { tile ->
                current = when (tile) {
                    HubTileId.LiveTV -> if (creds.hasCreds()) Screen.LiveTv else Screen.StreamTester
                    HubTileId.Settings -> Screen.StreamTester
                    else -> Screen.Hub
                }
            },
        )

        Screen.StreamTester -> StreamTesterScreen(
            initialUrl = directUrl,
            initialHost = host,
            initialUser = user,
            initialPass = pass,
            onPlay = { url ->
                directUrl = url
                creds.lastDirectUrl = url
                current = Screen.Player(
                    url = url,
                    label = url.substringAfterLast('/').take(40),
                    epgChannelId = "",
                    from = Screen.StreamTester,
                )
            },
            onUrlChange = {
                directUrl = it
                creds.lastDirectUrl = it
            },
            onCredsChange = { h, u, p ->
                host = h; user = u; pass = p
                creds.host = h; creds.user = u; creds.pass = p
            },
            onExit = { current = Screen.Hub },
        )

        Screen.LiveTv -> LiveTvScreen(
            host = host,
            user = user,
            pass = pass,
            onChannelClick = { url, label, epgId ->
                creds.lastChannelUrl = url
                current = Screen.Player(url = url, label = label, epgChannelId = epgId, from = Screen.LiveTv)
            },
            onExit = { current = Screen.Hub },
            onOpenSettings = { current = Screen.StreamTester },
        )

        is Screen.Player -> PlayerScreen(
            streamUrl = screen.url,
            channelLabel = screen.label,
            epgChannelId = screen.epgChannelId,
            onExit = { current = screen.from },
        )
    }
}
