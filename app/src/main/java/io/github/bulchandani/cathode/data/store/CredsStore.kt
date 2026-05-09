package io.github.bulchandani.cathode.data.store

import android.content.Context
import androidx.core.content.edit

/**
 * Plain SharedPreferences wrapper for Xtream credentials and the
 * last-watched stream URL. v0.6.0 will switch this to
 * EncryptedSharedPreferences (we keep the same key names so migration
 * is trivial — read once, write to encrypted, delete plain).
 */
class CredsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var host: String
        get() = prefs.getString(KEY_HOST, "") ?: ""
        set(value) = prefs.edit { putString(KEY_HOST, value) }

    var user: String
        get() = prefs.getString(KEY_USER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_USER, value) }

    var pass: String
        get() = prefs.getString(KEY_PASS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_PASS, value) }

    var lastDirectUrl: String
        get() = prefs.getString(KEY_LAST_URL, DEFAULT_DIRECT_URL) ?: DEFAULT_DIRECT_URL
        set(value) = prefs.edit { putString(KEY_LAST_URL, value) }

    var lastChannelUrl: String?
        get() = prefs.getString(KEY_LAST_CHANNEL, null)
        set(value) = prefs.edit { putString(KEY_LAST_CHANNEL, value) }

    fun hasCreds(): Boolean = host.isNotBlank() && user.isNotBlank() && pass.isNotBlank()

    companion object {
        private const val PREFS_NAME = "cathode_creds"
        private const val KEY_HOST = "host"
        private const val KEY_USER = "user"
        private const val KEY_PASS = "pass"
        private const val KEY_LAST_URL = "last_direct_url"
        private const val KEY_LAST_CHANNEL = "last_channel_url"
        const val DEFAULT_DIRECT_URL =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"
    }
}
