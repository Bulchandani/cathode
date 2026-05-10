package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.bulchandani.cathode.data.store.CrtMode
import io.github.bulchandani.cathode.ui.theme.LocalCrtMode

/**
 * Scanline overlay. Density and alpha follow [LocalCrtMode]:
 *   FullVintage  – 1px every 3px, 12% alpha
 *   Moderate     – 1px every 4px, 6% alpha (default)
 *   ModernDark   – not drawn (clean dark UI)
 */
@Composable
fun CathodeScanlines(modifier: Modifier = Modifier) {
    val mode = LocalCrtMode.current
    if (mode == CrtMode.ModernDark) return
    val (stepPx, alpha) = when (mode) {
        CrtMode.FullVintage -> 3f to 0.12f
        CrtMode.Moderate -> 4f to 0.06f
        CrtMode.ModernDark -> return
    }
    val color = Color(0xFFE8E8E8).copy(alpha = alpha)
    Canvas(modifier = modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += stepPx
        }
    }
}
