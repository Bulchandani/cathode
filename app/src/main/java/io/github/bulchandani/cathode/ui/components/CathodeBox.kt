@file:OptIn(ExperimentalTvMaterial3Api::class)

package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import io.github.bulchandani.cathode.ui.theme.DimGrey
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen

private val DefaultShape = RoundedCornerShape(8.dp)

/**
 * Generic container used by channel rows, category rows, episode rows, etc.
 * Backed by `androidx.tv.material3.Surface` so D-pad focus + long-press +
 * SELECT all work without us touching focusable/clickable directly.
 *
 * If neither onClick nor onLongClick are provided, falls back to a plain
 * non-focusable Box (used for layout-only containers).
 */
@Composable
fun CathodeBox(
    modifier: Modifier = Modifier,
    shape: Shape = DefaultShape,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    if (onClick == null && onLongClick == null) {
        Box(modifier = modifier, content = content)
        return
    }
    Surface(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = DimGrey,
            contentColor = PhosphorGreen,
            focusedContainerColor = DimGrey,
            focusedContentColor = PhosphorGreen,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, PhosphorGreen),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
    ) {
        content()
    }
}
