package io.github.bulchandani.cathode.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.bulchandani.cathode.data.store.SettingsStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * ExoPlayer tuned for IPTV. Uses an OkHttp-backed data source (better
 * redirect / cookie / HTTP-2 handling than HttpURLConnection, which
 * is what some Xtream providers reject with 405). Sets a permissive
 * User-Agent and forwards generous timeouts.
 */
object CathodePlayerFactory {

    private const val USER_AGENT = "VLC/3.0.20 LibVLC/3.0.20"

    private val sharedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

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

        val httpDataSourceFactory = OkHttpDataSource.Factory(sharedClient)
            .setUserAgent(USER_AGENT)

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
