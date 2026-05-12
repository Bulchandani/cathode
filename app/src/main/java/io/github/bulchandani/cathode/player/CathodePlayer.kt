@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package io.github.bulchandani.cathode.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.github.bulchandani.cathode.data.store.SettingsStore

/**
 * ExoPlayer tuned for IPTV.
 *
 * - HTTP stack: Media3's `DefaultHttpDataSource` (HttpURLConnection-based)
 *   rather than OkHttp. Several Xtream providers reject OkHttp's TLS
 *   fingerprint (JA3) and HTTP/2 negotiation with a 405 even when URL,
 *   credentials, and User-Agent are correct.
 * - Renderers: [PermissiveRenderersFactory] swaps in a video renderer
 *   that upgrades EXCEEDS_CAPABILITIES → HANDLED, forcing Media3 to
 *   attempt decoder configure() instead of refusing pre-check. Lots of
 *   tablets handle HEVC Level 5.1 / 10-bit fine despite advertising
 *   only Level 5.0 — TiviMate and libVLC-based players reach the same
 *   playback via the same trick.
 * - Track selector: explicitly allow exceeding renderer capabilities and
 *   video constraints, so the track selector never refuses to pick a
 *   track for a capability mismatch (would otherwise undo the renderer's
 *   permissiveness).
 * - Decoder fallback: enabled so if the primary decoder fails init at
 *   runtime, Media3 tries the next candidate before erroring.
 */
@UnstableApi
object CathodePlayerFactory {

    private const val USER_AGENT = "Lavf/58.45.100"

    fun create(context: Context): ExoPlayer {
        val profile = SettingsStore.bufferProfile.value
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                profile.minBufferMs,
                profile.maxBufferMs,
                profile.playbackBufferMs,
                profile.rebufferMs,
            )
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val renderersFactory = PermissiveRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = parameters.buildUpon()
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
                .setExceedAudioConstraintsIfNecessary(true)
                .build()
        }

        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
