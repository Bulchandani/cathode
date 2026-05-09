package io.github.bulchandani.cathode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.bulchandani.cathode.ui.theme.DimGrey

private val DefaultShape = RoundedCornerShape(8.dp)

/**
 * Base Cathode container — DimGrey fill, rounded corners, dim border by default,
 * pulsing phosphor glow when focused.
 *
 * Pass `onClick` to make it interactive (D-pad SELECT / touch tap both fire).
 */
@Composable
fun CathodeBox(
    modifier: Modifier = Modifier,
    shape: Shape = DefaultShape,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val baseModifier = modifier
        .clip(shape)
        .background(DimGrey)
        .cathodeGlow(focused = focused, shape = shape)
        .onFocusChanged { focused = it.isFocused }

    val finalModifier = if (onClick != null) {
        baseModifier.focusable().clickable(onClick = onClick)
    } else {
        baseModifier
    }

    Box(modifier = finalModifier, content = content)
}
