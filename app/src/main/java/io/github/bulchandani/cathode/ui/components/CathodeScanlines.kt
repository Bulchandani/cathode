package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

private val ScanlineColor = Color(0xFFE8E8E8).copy(alpha = 0.08f)

/**
 * Full-vintage scanlines: 1px line every 3px @ 8% alpha.
 * Drawn full-screen, above content.
 */
@Composable
fun CathodeScanlines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 3f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = ScanlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }
    }
}
