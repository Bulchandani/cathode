package io.github.bulchandani.cathode.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Wraps content in a CRT-style barrel-distorted viewport.
 *
 * On API 33+ (Android 13, Tiramisu) this is a real radial-distortion
 * RenderEffect shader. On older Android the wrapper is a no-op flat
 * composition; callers should always include [CathodeVignette] to
 * preserve corner-darkening on low-fidelity devices.
 */
@Composable
fun CathodeCurvature(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Box(modifier = modifier.fillMaxSize()) { content() }
        return
    }
    CathodeCurvatureTiramisu(modifier, content)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun CathodeCurvatureTiramisu(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val shader = remember { RuntimeShader(BARREL_AGSL) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .graphicsLayer {
                if (size.width == 0 || size.height == 0) return@graphicsLayer
                shader.setFloatUniform(
                    "uResolution",
                    size.width.toFloat(),
                    size.height.toFloat(),
                )
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "uContent")
                    .asComposeRenderEffect()
            },
    ) {
        content()
    }
}

// AGSL barrel distortion: ~3% radial bow at corners.
// `uResolution` is viewport size in px; `uContent` is the source bitmap shader.
private const val BARREL_AGSL = """
    uniform shader uContent;
    uniform float2 uResolution;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        float2 c = uv - float2(0.5);
        float r2 = dot(c, c);
        float k = 0.06;
        float2 distorted = c * (1.0 + k * r2) + float2(0.5);
        if (distorted.x < 0.0 || distorted.x > 1.0 ||
            distorted.y < 0.0 || distorted.y > 1.0) {
            return half4(0, 0, 0, 1);
        }
        return uContent.eval(distorted * uResolution);
    }
"""
