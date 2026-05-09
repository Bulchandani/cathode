package io.github.bulchandani.cathode.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim

/**
 * Pulsing breathing glow + steady border for focused elements.
 *
 * Focused: 2.0s sine alpha 0.20 ↔ 0.55, 24dp blur shadow, 1.5dp phosphor border.
 * Unfocused: 0.5dp dim phosphor border, no glow.
 */
fun Modifier.cathodeGlow(
    focused: Boolean,
    shape: Shape,
    blurDp: Float = 24f,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "cathodeGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val targetAlpha = if (focused) pulse else 0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(200, easing = LinearEasing),
        label = "glowAlpha",
    )
    val borderColor = if (focused) PhosphorGreen else PhosphorGreenDim
    val borderWidth = if (focused) 1.5f else 0.5f

    this
        .shadow(
            elevation = if (focused) blurDp.dp else 0.dp,
            shape = shape,
            ambientColor = PhosphorGreen.copy(alpha = animatedAlpha),
            spotColor = PhosphorGreen.copy(alpha = animatedAlpha),
        )
        .border(
            width = borderWidth.dp,
            color = borderColor,
            shape = shape,
        )
}
