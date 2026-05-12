package io.github.bulchandani.cathode.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.bulchandani.cathode.data.store.SettingsStore

/**
 * ExoPlayer tuned for IPTV.
 *
 * Uses Media3's DefaultHttpDataSource (HttpURLConnection-backed) rather than
 * OkHttp. Several Xtream providers reject OkHttp's TLS fingerprint (JA3) and
 * its HTTP/2 negotiation with a 405 even when the URL, credentials, and
 * User-Agent are all correct. HttpURLConnection — the same stack TiviMate,
 * IPTV Smarters, and FFmpeg-based players use — sails past those checks.
 * User-Agent matches FFmpeg's Lavf, which is what TiviMate sends on the wire.
 *
 * Cross-protocol redirects are allowed because some providers serve the API
 * over http but redirect stream requests to https on :443.
 */
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

        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
