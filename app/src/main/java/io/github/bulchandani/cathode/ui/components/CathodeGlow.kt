package io.github.bulchandani.cathode.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bulchandani.cathode.ui.theme.PhosphorGreen
import io.github.bulchandani.cathode.ui.theme.PhosphorGreenDim

/**
 * Pulsing breathing glow + steady border for focused elements.
 *
 * Focused: blurred phosphor halo behind the element (BlurMaskFilter
 * paint, so it really blurs — Compose's built-in shadow is too dim
 * for this aesthetic); 2.0s sine alpha 0.20 ↔ 0.55; 1.5dp phosphor
 * border on the element edge.
 *
 * Unfocused: 0.5dp dim phosphor border, no halo.
 *
 * In inspection mode (Roborazzi snapshots, Android Studio Preview)
 * the infinite animation is skipped — alpha is fixed at 0.45 — so
 * snapshots render deterministically and CI doesn't hang waiting
 * for an infinite animation to settle.
 */
fun Modifier.cathodeGlow(
    focused: Boolean,
    shape: Shape,
    blurDp: Dp = 24.dp,
    cornerRadius: Dp = 8.dp,
): Modifier = composed {
    val pulse = pulseAlpha()
    val alpha = if (focused) pulse else 0f

    val borderColor = if (focused) PhosphorGreen else PhosphorGreenDim
    val borderWidth = if (focused) 1.5.dp else 0.5.dp

    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    this
        .drawBehind {
            if (alpha > 0f) {
                paint.color = PhosphorGreen.copy(alpha = alpha).toArgb()
                paint.maskFilter = BlurMaskFilter(blurDp.toPx(), BlurMaskFilter.Blur.NORMAL)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius.toPx(),
                        cornerRadius.toPx(),
                        paint,
                    )
                }
            }
        }
        .border(borderWidth, borderColor, shape)
}

@Composable
private fun pulseAlpha(): Float {
    if (LocalInspectionMode.current) return 0.45f
    val transition = rememberInfiniteTransition(label = "cathodeGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    return pulse
}
