package io.github.bulchandani.cathode.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.BuildConfig
import io.github.bulchandani.cathode.data.epg.EpgRepo
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.components.cathodeGlow
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

enum class ShellSection(val key: String, val label: String) {
    LiveTv("live", "Live TV"),
    Movies("movies", "Movies"),
    Series("series", "Series"),
    Search("search", "Search"),
    Favorites("favorites", "Favorites"),
    Recents("recents", "Recents"),
    Settings("settings", "Settings"),
}

@Composable
fun CathodeShell(
    active: ShellSection,
    onSelect: (ShellSection) -> Unit,
    sourceLabel: String,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(active = active, onSelect = onSelect, modifier = Modifier.width(240.dp).fillMaxHeight())
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(sourceLabel = sourceLabel)
                Box(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
        CathodeScanlines()
        CathodeVignette()
    }
}

@Composable
private fun Sidebar(
    active: ShellSection,
    onSelect: (ShellSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(Color.Black.copy(alpha = 0.3f)).padding(16.dp)) {
        Text("CATHODE", style = CathodeText.Headline, color = PhosphorGreen)
        Text("v${BuildConfig.VERSION_NAME}", style = CathodeText.Caption, color = PhosphorGreenDim)
        Box(modifier = Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(ShellSection.entries) { section ->
                SidebarItem(
                    label = section.label,
                    isActive = section == active,
                    onClick = { onSelect(section) },
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) DimGrey else Color.Transparent)
            .cathodeGlow(focused = isActive, shape = RoundedCornerShape(6.dp), blurDp = 18.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = CathodeText.Body,
            color = if (isActive) PhosphorGreen else OffWhite,
        )
    }
}

@Composable
private fun TopBar(sourceLabel: String) {
    var clockTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { clockTick++; delay(1_000) }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val epgReady = EpgRepo.isReady()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (sourceLabel.isBlank()) "no source" else sourceLabel,
            style = CathodeText.Data,
            color = if (sourceLabel.isBlank()) PhosphorGreenDim else PhosphorGreen,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (epgReady) "EPG ✓" else "EPG —",
            style = CathodeText.Caption,
            color = if (epgReady) Amber else PhosphorGreenDim,
        )
        Text(timeFmt.format(Date()), style = CathodeText.Section, color = Amber)
    }
}
