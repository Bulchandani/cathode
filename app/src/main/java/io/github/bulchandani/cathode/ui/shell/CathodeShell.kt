@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.bulchandani.cathode.ui.shell

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.BuildConfig
import io.github.bulchandani.cathode.ui.components.CathodeScanlines
import io.github.bulchandani.cathode.ui.components.CathodeVignette
import io.github.bulchandani.cathode.ui.components.ToasterHost
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim
import io.github.bulchandani.cathode.ui.theme.Void

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
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Void)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                active = active,
                onSelect = onSelect,
                modifier = Modifier.width(240.dp).fillMaxHeight(),
            )
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
        CathodeScanlines()
        CathodeVignette()
        ToasterHost()
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
        Spacer(Modifier.height(24.dp))
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
    // tv-material3 Surface handles D-pad focus + SELECT correctly. We just
    // express the three visual states (focused, active-but-not-focused,
    // resting) via Surface's colors/border config.
    val shape = RoundedCornerShape(6.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isActive) DimGrey else Color.Transparent,
            contentColor = if (isActive) PhosphorGreen else OffWhite,
            focusedContainerColor = PhosphorGreen,
            focusedContentColor = Void,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (isActive) Border(
                border = BorderStroke(1.dp, PhosphorGreen),
                shape = shape,
            ) else Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, PhosphorGreen),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = label, style = CathodeText.Body)
        }
    }
}
