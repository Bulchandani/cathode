@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.bulchandani.cathode.ui.theme.CathodeText
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.OffWhite
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.Void

/**
 * Phosphor-styled button built on `androidx.tv.material3.Surface` so D-pad
 * focus, SELECT, and the focus highlight are all handled by the framework
 * — no hand-rolled `.focusable() + .clickable() + .onFocusChanged` stack,
 * which is what produced the 2-press SELECT bug we hit through v0.8.12.
 *
 * Visual model:
 *   - resting: thin phosphor border, Void background, PhosphorGreen text
 *   - focused: 2dp phosphor border, PhosphorGreen background, Void text
 *   - disabled: DimGrey background, OffWhite text, dim border
 */
@Composable
fun CathodeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
) {
    val shape = RoundedCornerShape(6.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Void,
            contentColor = PhosphorGreen,
            focusedContainerColor = PhosphorGreen,
            focusedContentColor = Void,
            pressedContainerColor = PhosphorGreen,
            pressedContentColor = Void,
            disabledContainerColor = DimGrey,
            disabledContentColor = OffWhite,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, PhosphorGreen),
                shape = shape,
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, PhosphorGreen),
                shape = shape,
            ),
            disabledBorder = Border(
                border = BorderStroke(1.dp, OffWhite),
                shape = shape,
            ),
        ),
        // No focus-scale animation — keeps the CRT aesthetic. tv-material3
        // defaults to 1.1x focused scale; explicitly flatten to 1.0.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            // Text auto-picks up LocalContentColor from the Surface, so it
            // flips Green→Void on focus without explicit color logic.
            Text(text = text, style = CathodeText.Section, textAlign = TextAlign.Center)
        }
    }
}
