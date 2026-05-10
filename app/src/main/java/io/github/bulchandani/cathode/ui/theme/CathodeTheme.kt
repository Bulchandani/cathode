package io.github.bulchandani.cathode.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import io.github.bulchandani.cathode.data.store.CrtMode
import io.github.bulchandani.cathode.data.store.SettingsStore
import io.github.bulchandani.cathode.ui.components.CathodeScanlines

/** Read this in any composable to branch on the active CRT mode. */
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
fun CathodeTheme(content: @Composable () -> Unit) {
    val mode = SettingsStore.crtMode.value
    CompositionLocalProvider(LocalCrtMode provides mode) {
        MaterialTheme(
            colorScheme = CathodeColorScheme,
            typography = CathodeTypography,
            content = content,
        )
    }
}

/**
 * Backwards-compat shim. Old callers still call `ScanlineOverlay()`;
 * delegates to [CathodeScanlines] which now respects [LocalCrtMode].
 */
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    CathodeScanlines(modifier = modifier)
}
