package io.github.bulchandani.cathode.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import java.security.MessageDigest

enum class CrtMode { FullVintage, Moderate, ModernDark }

enum class BufferProfile(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int,
) {
    LowLatency(5_000, 15_000, 1_000, 2_000),
    Default(15_000, 60_000, 2_500, 5_000),
    Robust(30_000, 120_000, 5_000, 10_000),
}

/** Theme + buffering + parental PIN + per-stream audio-sync, one prefs file. */
object SettingsStore {

    private val _crtMode = mutableStateOf(CrtMode.Moderate)
    val crtMode: State<CrtMode> = _crtMode

    private val _bufferProfile = mutableStateOf(BufferProfile.Default)
    val bufferProfile: State<BufferProfile> = _bufferProfile

    private val _hasPin = mutableStateOf(false)
    val hasPin: State<Boolean> = _hasPin

    private val _audioSyncByStream = mutableStateOf<Map<String, Int>>(emptyMap())
    val audioSyncByStream: State<Map<String, Int>> = _audioSyncByStream

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext
            .getSharedPreferences("cathode_settings", Context.MODE_PRIVATE)
        prefs = p

        _crtMode.value = runCatching {
            CrtMode.valueOf(p.getString(KEY_CRT, CrtMode.Moderate.name)!!)
        }.getOrDefault(CrtMode.Moderate)

        _bufferProfile.value = runCatching {
            BufferProfile.valueOf(p.getString(KEY_BUF, BufferProfile.Default.name)!!)
        }.getOrDefault(BufferProfile.Default)

        _hasPin.value = p.contains(KEY_PIN_HASH)

        // Audio sync entries persisted as "url<TAB>ms" lines.
        val raw = p.getString(KEY_SYNC_MAP, null).orEmpty()
        if (raw.isNotEmpty()) {
            _audioSyncByStream.value = raw.lineSequence().mapNotNull { entry ->
                val tab = entry.indexOf('\t')
                if (tab <= 0) return@mapNotNull null
                val key = entry.substring(0, tab)
                val ms = entry.substring(tab + 1).toIntOrNull() ?: return@mapNotNull null
                key to ms
            }.toMap()
        }
    }

    fun setCrtMode(mode: CrtMode) {
        _crtMode.value = mode
        prefs?.edit { putString(KEY_CRT, mode.name) }
    }

    fun setBufferProfile(p: BufferProfile) {
        _bufferProfile.value = p
        prefs?.edit { putString(KEY_BUF, p.name) }
    }

    fun setPin(pin: String) {
        _hasPin.value = true
        prefs?.edit { putString(KEY_PIN_HASH, hash(pin)) }
    }

    fun clearPin() {
        _hasPin.value = false
        prefs?.edit { remove(KEY_PIN_HASH) }
    }

    fun checkPin(pin: String): Boolean {
        val saved = prefs?.getString(KEY_PIN_HASH, null) ?: return true
        return hash(pin) == saved
    }

    fun setAudioSync(streamUrl: String, ms: Int) {
        val next = _audioSyncByStream.value.toMutableMap()
        if (ms == 0) next.remove(streamUrl) else next[streamUrl] = ms
        _audioSyncByStream.value = next
        prefs?.edit {
            putString(KEY_SYNC_MAP, next.entries.joinToString("\n") { "${it.key}\t${it.value}" })
        }
    }

    fun audioSyncFor(streamUrl: String): Int = _audioSyncByStream.value[streamUrl] ?: 0

    private fun hash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private const val KEY_CRT = "crt_mode"
    private const val KEY_BUF = "buf_profile"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_SYNC_MAP = "audio_sync_map"
}
