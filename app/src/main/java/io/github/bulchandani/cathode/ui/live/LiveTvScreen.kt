package io.github.bulchandani.cathode.ui.live

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.data.xtream.XtreamApi
import io.github.bulchandani.cathode.data.xtream.XtreamCategory
import io.github.bulchandani.cathode.data.xtream.XtreamLiveStream
import io.github.bulchandani.cathode.ui.components.CathodeBox
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.components.cathodeGlow
import io.github.bulchandani.cathode.ui.theme.AlarmRed
import io.github.bulchandani.cathode.ui.theme.Amber
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

private const val ALL_CATEGORY_ID = "__all__"

@Composable
fun LiveTvScreen(
    host: String,
    user: String,
    pass: String,
    onChannelClick: (streamUrl: String, channelLabel: String, epgChannelId: String) -> Unit,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var allChannels by remember { mutableStateOf<List<XtreamLiveStream>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf(ALL_CATEGORY_ID) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var epgReady by remember { mutableStateOf(false) }

    LaunchedEffect(host, user, pass) {
        if (host.isBlank() || user.isBlank() || pass.isBlank()) {
            error = "No credentials yet. Add them in Settings."
            loading = false
            return@LaunchedEffect
        }
        try {
            loading = true
            error = null
            categories = XtreamApi.fetchLiveCategories(host, user, pass)
            allChannels = XtreamApi.fetchLiveStreams(host, user, pass)
            loading = false
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load"
            loading = false
        }
        // Best-effort EPG load — never blocks channel rendering.
        EpgRepo.load(host, user, pass)
        epgReady = EpgRepo.isReady()
    }

    BackHandler(onBack = onExit)

    val displayChannels = if (selectedCategoryId == ALL_CATEGORY_ID) allChannels
    else allChannels.filter { it.categoryId == selectedCategoryId }

    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE TV", style = CathodeText.Display, color = PhosphorGreen)
                Spacer(Modifier.width(24.dp))
                if (!loading && error == null) {
                    Text(
                        text = "${displayChannels.size} ch",
                        style = CathodeText.Section,
                        color = Amber,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading -> Centered("Fetching channels…", color = PhosphorGreen)
                error != null -> ErrorPane(error!!, onOpenSettings = onOpenSettings)
                else -> Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CategoriesColumn(
                        categories = categories,
                        allChannels = allChannels,
                        selected = selectedCategoryId,
                        onSelect = { selectedCategoryId = it },
                        modifier = Modifier.width(280.dp).fillMaxHeight(),
                    )
                    ChannelColumn(
                        channels = displayChannels,
                        host = host,
                        user = user,
                        pass = pass,
                        onChannelClick = onChannelClick,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        CathodeScanlines()
        CathodeVignette()
    }
}

@Composable
private fun Centered(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = CathodeText.Section, color = color)
    }
}

@Composable
private fun ErrorPane(message: String, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("⚠  COULDN'T LOAD CHANNELS", style = CathodeText.Headline, color = AlarmRed)
        Text(message, style = CathodeText.Body, color = OffWhite)
        Spacer(Modifier.height(16.dp))
        io.github.bulchandani.cathode.ui.components.CathodeButton(
            text = "OPEN SETTINGS",
            onClick = onOpenSettings,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CategoriesColumn(
    categories: List<XtreamCategory>,
    allChannels: List<XtreamLiveStream>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val countByCategory = remember(allChannels) {
        allChannels.groupingBy { it.categoryId }.eachCount()
    }
    Column(modifier = modifier) {
        Text("CATEGORIES", style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                CategoryRow(
                    label = "All",
                    count = allChannels.size,
                    focused = selected == ALL_CATEGORY_ID,
                    onClick = { onSelect(ALL_CATEGORY_ID) },
                )
            }
            items(categories, key = { it.categoryId }) { c ->
                CategoryRow(
                    label = c.name,
                    count = countByCategory[c.categoryId] ?: 0,
                    focused = selected == c.categoryId,
                    onClick = { onSelect(c.categoryId) },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, count: Int, focused: Boolean, onClick: () -> Unit) {
    CathodeBox(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = CathodeText.Body,
                color = if (focused) PhosphorGreen else OffWhite,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = count.toString(),
                style = CathodeText.Caption,
                color = PhosphorGreenDim,
            )
        }
    }
}

@Composable
private fun ChannelColumn(
    channels: List<XtreamLiveStream>,
    host: String,
    user: String,
    pass: String,
    onChannelClick: (streamUrl: String, channelLabel: String, epgChannelId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("CHANNELS", style = CathodeText.Section, color = PhosphorGreen)
        Spacer(Modifier.height(8.dp))
        if (channels.isEmpty()) {
            Text(
                "No channels in this category.",
                style = CathodeText.Data,
                color = PhosphorGreenDim,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(channels, key = { it.streamId }) { ch ->
                    ChannelRow(
                        channel = ch,
                        onClick = {
                            val url = XtreamApi.buildLiveStreamUrl(host, user, pass, ch.streamId)
                            val label = "%04d  %s".format(ch.streamId, ch.name.ifBlank { "Channel ${ch.streamId}" })
                            onChannelClick(url, label, ch.epgChannelId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: XtreamLiveStream, onClick: () -> Unit) {
    val (now, _) = remember(channel.epgChannelId, EpgRepo.isReady()) {
        EpgRepo.nowAndNext(channel.epgChannelId)
    }
    CathodeBox(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DimGrey),
                contentAlignment = Alignment.Center,
            ) {
                if (channel.streamIcon.isNotBlank()) {
                    AsyncImage(
                        model = channel.streamIcon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("◉", style = CathodeText.Body, color = PhosphorGreenDim)
                }
            }
            Text(
                text = "%04d".format(channel.streamId),
                style = CathodeText.Data,
                color = Amber,
                modifier = Modifier.width(64.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name.ifBlank { "Channel ${channel.streamId}" },
                    style = CathodeText.Body,
                    color = OffWhite,
                )
                if (now != null) {
                    Text(
                        text = "Now: ${now.title}",
                        style = CathodeText.Caption,
                        color = PhosphorGreenDim,
                    )
                }
            }
        }
    }
}
