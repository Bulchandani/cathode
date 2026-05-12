@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package io.github.bulchandani.cathode.player

import android.content.Context
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener

/**
 * A video renderer that bypasses Media3's strict pre-flight format
 * capability check. When Media3 evaluates a format like
 * `hvc1.2.4.l153.b0` (HEVC Main 10 Level 5.1 HDR10) and the device's
 * MediaCodecInfo only advertises Level 5.0 support, the default
 * renderer reports `FORMAT_EXCEEDS_CAPABILITIES` and the track is
 * never even attempted.
 *
 * In practice, manufacturers commonly under-declare profile/level
 * support in the API while the actual silicon handles the slightly-
 * higher format fine. TiviMate and other libVLC-based players sail
 * past these checks and play the stream. This renderer upgrades the
 * support code from `EXCEEDS_CAPABILITIES` → `HANDLED` for any video
 * MIME, forcing Media3 to call `MediaCodec.configure()` for real. If
 * the decoder really can't handle the format, configure() fails and
 * we surface a runtime error instead of a pre-check error — same end
 * for the user, but at least the borderline cases get a chance.
 */
@UnstableApi
class PermissiveVideoRenderer(
    context: Context,
    codecAdapterFactory: MediaCodecAdapter.Factory,
    mediaCodecSelector: MediaCodecSelector,
    allowedJoiningTimeMs: Long,
    enableDecoderFallback: Boolean,
    eventHandler: Handler?,
    eventListener: VideoRendererEventListener?,
    maxDroppedFramesToNotify: Int,
) : MediaCodecVideoRenderer(
    context,
    codecAdapterFactory,
    mediaCodecSelector,
    allowedJoiningTimeMs,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    maxDroppedFramesToNotify,
) {
    override fun supportsFormat(
        mediaCodecSelector: MediaCodecSelector,
        format: Format,
    ): Int {
        val original = super.supportsFormat(mediaCodecSelector, format)
        val support = RendererCapabilities.getFormatSupport(original)
        if (support != C.FORMAT_EXCEEDS_CAPABILITIES) return original

        val mime = format.sampleMimeType ?: return original
        if (!mime.startsWith("video/")) return original

        // Repack: HANDLED instead of EXCEEDS_CAPABILITIES; keep other flags as-is.
        return RendererCapabilities.create(
            C.FORMAT_HANDLED,
            RendererCapabilities.getAdaptiveSupport(original),
            RendererCapabilities.getTunnelingSupport(original),
            RendererCapabilities.getHardwareAccelerationSupport(original),
            RendererCapabilities.getDecoderSupport(original),
        )
    }
}

/**
 * Drop-in replacement for DefaultRenderersFactory that installs our
 * [PermissiveVideoRenderer] in place of the default MediaCodecVideoRenderer.
 * Audio / text / metadata renderers are untouched (the strict capability
 * check is only a problem for video on Fire TV / tablet hardware in
 * practice — audio formats don't have profile/level mismatch issues here).
 */
@UnstableApi
class PermissiveRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) {
        out.add(
            PermissiveVideoRenderer(
                context,
                codecAdapterFactory,
                mediaCodecSelector,
                allowedVideoJoiningTimeMs,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
            )
        )
        // Note: deliberately skip super.buildVideoRenderers — that would add
        // the default MediaCodecVideoRenderer alongside ours. We also skip
        // extension renderers (libvpx etc) since we don't ship the AARs.
    }
}
