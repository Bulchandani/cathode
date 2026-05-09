package io.github.bulchandani.cathode.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.bulchandani.cathode.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES_API =
    "https://api.github.com/repos/Bulchandani/cathode/releases/latest"

data class ReleaseInfo(
    val tagName: String,
    val apkUrl: String,
    val notes: String,
)

object UpdateChecker {

    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(text)
            val tag = obj.optString("tag_name", "")
            val notes = obj.optString("body", "")
            val assets = obj.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name") == "cathode.apk") {
                    return@withContext ReleaseInfo(tag, a.optString("browser_download_url"), notes)
                }
            }
            null
        } finally {
            conn.disconnect()
        }
    }

    /** Naive semver-ish comparison: strips a leading `v`, splits on `.`, ignores suffixes after `-`. */
    fun isNewer(remote: String, local: String = BuildConfig.VERSION_NAME): Boolean {
        fun parts(v: String) = v.removePrefix("v").substringBefore('-')
            .split('.').mapNotNull { it.toIntOrNull() }
        val r = parts(remote); val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    suspend fun downloadApk(context: Context, url: String): File = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "cathode-update.apk")
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        try {
            conn.inputStream.use { input -> target.outputStream().use { out -> input.copyTo(out) } }
        } finally {
            conn.disconnect()
        }
        target
    }

    fun installApk(context: Context, apk: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
