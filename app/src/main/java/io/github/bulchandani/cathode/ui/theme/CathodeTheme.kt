package io.github.bulchandani.cathode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import io.github.bulchandani.cathode.ui.components.CathodeScanlines

enum class CrtMode { FullVintage, Moderate, ModernDark }

val LocalCrtMode = staticCompositionLocalOf { CrtMode.FullVintage }

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
    mode: CrtMode = CrtMode.FullVintage,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CathodeColorScheme,
        typography = CathodeTypography,
        content = content,
    )
}

/**
 * Backwards-compat shim. Phase-1 introduced [CathodeScanlines] (8% alpha,
 * full vintage). Old screens still calling `ScanlineOverlay()` get the
 * new behavior automatically until they're rewritten in Phase 2.
 */
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    CathodeScanlines(modifier = modifier)
}
