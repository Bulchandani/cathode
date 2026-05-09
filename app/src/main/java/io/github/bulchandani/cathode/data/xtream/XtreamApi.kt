package io.github.bulchandani.cathode.data.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
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

            val text = (conn.inputStream ?: conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("Empty response from $cleanHost")

            parseLiveStreams(text)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Some Xtream servers return a JSONObject (auth failure, account
     * disabled, expired, banned) where we'd expect a JSONArray of
     * streams. Parse both shapes and surface a meaningful message
     * instead of a raw `JSONException` at the call site.
     */
    internal fun parseLiveStreams(text: String): List<XtreamLiveStream> {
        val token = try {
            JSONTokener(text).nextValue()
        } catch (e: Exception) {
            throw IOException(
                "Server returned non-JSON. First 200 chars: ${text.take(200)}",
                e,
            )
        }

        return when (token) {
            is JSONArray -> List(token.length()) { i ->
                val obj = token.getJSONObject(i)
                XtreamLiveStream(
                    streamId = obj.getInt("stream_id"),
                    name = obj.optString("name", ""),
                    categoryId = obj.optString("category_id", ""),
                )
            }
            is JSONObject -> throw IOException(translateXtreamError(token))
            else -> throw IOException(
                "Unexpected JSON shape (${token::class.simpleName}). First 200 chars: ${text.take(200)}",
            )
        }
    }

    private fun translateXtreamError(obj: JSONObject): String {
        val userInfo = obj.optJSONObject("user_info")
        if (userInfo != null) {
            val auth = userInfo.optInt("auth", -1)
            val status = userInfo.optString("status", "")
            val message = userInfo.optString("message", "")

            return when {
                auth == 0 -> "Authentication failed — check username & password"
                status.equals("Banned", ignoreCase = true) -> "Account banned by provider"
                status.equals("Disabled", ignoreCase = true) -> "Account disabled by provider"
                status.equals("Expired", ignoreCase = true) -> "Account expired"
                message.isNotEmpty() -> "Provider says: $message"
                else -> "Unexpected user_info response (status=$status, auth=$auth)"
            }
        }

        val errorMsg = obj.optString("error", "")
            .ifEmpty { obj.optString("message", "") }
            .ifEmpty { obj.toString().take(200) }
        return "Server error: $errorMsg"
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
