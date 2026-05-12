package io.github.bulchandani.cathode.player

import android.content.Context
import io.github.bulchandani.cathode.data.store.BufferProfile
import io.github.bulchandani.cathode.data.store.SettingsStore
import io.github.bulchandani.cathode.log.Logger
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

/**
 * libVLC-based playback. Replaces the v0.9.x Media3/ExoPlayer stack — same
 * core that powers TiviMate, IPTV Smarters' fork, and VLC for Android.
 *
 * Why we left Media3:
 *  - libVLC is permissive about HLS / MPEG-TS quirks. Many provider URLs
 *    that Media3 rejected with IO_NETWORK_CONNECTION_FAILED or
 *    EXCEEDS_CAPABILITIES just play with libVLC.
 *  - libVLC's HW decoder falls back to its own software decoder
 *    automatically on format mismatch, so the "exceeds capabilities" pre-
 *    check we worked around with PermissiveVideoRenderer goes away.
 *  - Audio-sync, track selection, tunneling, all built-in.
 *
 * The [LibVLC] instance is a process-scope singleton (init is slow and
 * memory-hungry); the [MediaPlayer] is created per PlayerScreen and
 * released when that screen disposes.
 */
object CathodePlayer {

    private const val TAG = "VLC"

    @Volatile private var libVlc: LibVLC? = null

    fun libvlc(context: Context): LibVLC {
        libVlc?.let { return it }
        return synchronized(this) {
            libVlc ?: LibVLC(context.applicationContext, buildVlcOptions()).also {
                libVlc = it
                Logger.i(TAG, "LibVLC initialized")
            }
        }
    }

    /**
     * Args passed to LibVLC at init. These are CLI flags VLC accepts globally;
     * per-Media options (UA per request etc) are layered on top via Media.
     *
     * Notes:
     *  - `--http-user-agent`: matches what TiviMate sends; some Xtream WAFs
     *    gate by UA. Same string we used in DefaultHttpDataSource.
     *  - `--network-caching`: 2000ms baseline. We override per-stream when
     *    BufferProfile changes.
     *  - `--no-drop-late-frames` / `--no-skip-frames`: keep playback stable
     *    on bursty Wi-Fi; visual blip preferred to silent skip.
     *  - `--rtsp-tcp`: avoid UDP issues over Wi-Fi (RTSP streams).
     *  - `--avcodec-hw=any`: use whatever HW decoder is available; fall back
     *    to software if not. Replaces the PermissiveVideoRenderer hack.
     */
    private fun buildVlcOptions(): ArrayList<String> {
        val cachingMs = networkCachingMs(SettingsStore.bufferProfile.value)
        return arrayListOf(
            "--http-user-agent=Lavf/58.45.100",
            "--http-reconnect",
            "--network-caching=$cachingMs",
            "--live-caching=$cachingMs",
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--rtsp-tcp",
            "--avcodec-hw=any",
            // libVLC is chatty on stderr; pin to warnings+errors.
            "-vv",
            "--no-video-title-show",
        )
    }

    fun networkCachingMs(profile: BufferProfile): Int = when (profile) {
        BufferProfile.LowLatency -> 800
        BufferProfile.Default -> 2_000
        BufferProfile.Robust -> 5_000
    }

    /** Create a MediaPlayer attached to the shared LibVLC. Caller must
     *  release() when done. */
    fun createMediaPlayer(context: Context): MediaPlayer {
        val mp = MediaPlayer(libvlc(context))
        Logger.d(TAG, "MediaPlayer created")
        return mp
    }
}
