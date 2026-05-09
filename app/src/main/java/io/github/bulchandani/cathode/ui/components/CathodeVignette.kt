package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.hypot

/**
 * Radial vignette: transparent center, ~35% black at corners.
 * Used as a graceful fallback when curvature shader isn't available
 * (API < 31), and as a permanent CRT-edge darkening on top of curvature.
 */
@Composable
fun CathodeVignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = hypot(size.width / 2.0, size.height / 2.0).toFloat()
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.6f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.35f),
                ),
                center = center,
                radius = radius,
            ),
            size = size,
        )
    }
}
