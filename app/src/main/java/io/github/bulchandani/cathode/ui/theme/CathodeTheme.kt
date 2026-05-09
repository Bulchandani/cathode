package io.github.bulchandani.cathode.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

enum class CrtMode { FullVintage, Moderate, ModernDark }

val LocalCrtMode = staticCompositionLocalOf { CrtMode.Moderate }

private val CathodeColorScheme = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = Void,
    secondary = Amber,
    onSecondary = Void,
    background = Void,
    onBackground = OffWhite,
    surface = DimGrey,
    onSurface = OffWhite,
    error = AlarmRed,
    onError = Void,
)

@Composable
fun CathodeTheme(
    mode: CrtMode = CrtMode.Moderate,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CathodeColorScheme,
        typography = CathodeTypography,
        content = content,
    )
}

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = ScanlineWhite,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }
    }
}
