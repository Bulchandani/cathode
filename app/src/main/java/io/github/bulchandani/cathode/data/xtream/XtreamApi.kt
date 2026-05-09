package io.github.bulchandani.cathode.data.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class XtreamLiveStream(
    val streamId: Int,
    val name: String,
    val categoryId: String,
)

object XtreamApi {

    /** Accepts `provider.com:8080`, `http://provider.com:8080`, etc. */
    fun normalizeHost(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        return when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed"
        }
    }

    suspend fun fetchLiveStreams(
        host: String,
        username: String,
        password: String,
    ): List<XtreamLiveStream> = withContext(Dispatchers.IO) {
        val cleanHost = normalizeHost(host)
        val u = URLEncoder.encode(username, "UTF-8")
        val p = URLEncoder.encode(password, "UTF-8")
        val url = URL("$cleanHost/player_api.php?username=$u&password=$p&action=get_live_streams")

        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000

            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP $code from $cleanHost")
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(text)

            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                XtreamLiveStream(
                    streamId = obj.getInt("stream_id"),
                    name = obj.optString("name", ""),
                    categoryId = obj.optString("category_id", ""),
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    fun buildLiveStreamUrl(
        host: String,
        username: String,
        password: String,
        streamId: Int,
    ): String {
        val cleanHost = normalizeHost(host)
        return "$cleanHost/live/$username/$password/$streamId.m3u8"
    }
}
