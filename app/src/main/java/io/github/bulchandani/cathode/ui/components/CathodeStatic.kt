package io.github.bulchandani.cathode.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * 120ms procedural CRT static for screen transitions. Renders a small
 * grayscale noise bitmap each frame and stretches it to fill the
 * viewport (cheap on the GPU, looks like analog snow at full screen).
 *
 * Pre-buffer the next screen behind this overlay so the user never
 * sees a black-then-content snap.
 */
@Composable
fun CathodeStatic(
    visible: Boolean,
    durationMs: Int = 120,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(visible) {
        val start = System.nanoTime()
        val durationNanos = durationMs.toLong() * 1_000_000L
        while (System.nanoTime() - start < durationNanos) {
            withFrameNanos { tick++ }
        }
        onComplete()
    }

    val bitmap = remember { Bitmap.createBitmap(80, 45, Bitmap.Config.ARGB_8888) }
    val pixels = remember { IntArray(80 * 45) }
    val rng = remember { Random(System.nanoTime()) }

    // Re-key off `tick` so this runs every frame the static is visible.
    @Suppress("UNUSED_EXPRESSION") tick
    for (i in pixels.indices) {
        val v = rng.nextInt(0, 256)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    bitmap.setPixels(pixels, 0, 80, 0, 0, 80, 45)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawNoise(bitmap)
    }
}

private fun DrawScope.drawNoise(bitmap: Bitmap) {
    drawImage(
        image = bitmap.asImageBitmap(),
        srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
        srcSize = androidx.compose.ui.unit.IntSize(bitmap.width, bitmap.height),
        dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
        alpha = 0.85f,
    )
    // Suppress unused on Offset / Size to avoid future lint warnings if reshuffled.
    @Suppress("UNUSED_EXPRESSION") Offset.Zero
    @Suppress("UNUSED_EXPRESSION") Size.Zero
}
