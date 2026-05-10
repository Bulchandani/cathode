package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.bulchandani.cathode.data.store.CrtMode
import io.github.bulchandani.cathode.ui.theme.LocalCrtMode
import kotlin.math.hypot

/** Vignette intensity follows [LocalCrtMode]; ModernDark = none. */
@Composable
fun CathodeVignette(modifier: Modifier = Modifier) {
    val mode = LocalCrtMode.current
    if (mode == CrtMode.ModernDark) return
    val outerAlpha = when (mode) {
        CrtMode.FullVintage -> 0.50f
        CrtMode.Moderate -> 0.30f
        CrtMode.ModernDark -> 0f
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = hypot(size.width / 2.0, size.height / 2.0).toFloat()
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = outerAlpha),
                ),
                center = center,
                radius = radius,
            ),
            size = size,
        )
    }
}
