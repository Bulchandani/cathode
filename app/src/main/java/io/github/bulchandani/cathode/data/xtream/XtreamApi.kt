package io.github.bulchandani.cathode.data.xtream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class XtreamCategory(
    val categoryId: String,
    val name: String,
)

data class XtreamLiveStream(
    val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String,
    val epgChannelId: String,
)

data class XtreamVodStream(
    val streamId: Int,
    val name: String,
    val categoryId: String,
    val streamIcon: String,
    val containerExtension: String,
    val rating: String,
    val year: String,
)

data class XtreamSeries(
    val seriesId: Int,
    val name: String,
    val categoryId: String,
    val cover: String,
    val plot: String,
    val rating: String,
    val year: String,
)

data class XtreamSeriesEpisode(
    val episodeId: String,
    val title: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val containerExtension: String,
    val plot: String,
)

object XtreamApi {

    /**
     * Matches TiviMate's FFmpeg-style UA. Several Xtream providers gate
     * `/get.php` and the player_api endpoints behind a UA allowlist and
     * 403/405 the default Java/HttpURLConnection signature.
     */
    private const val USER_AGENT = "Lavf/58.45.100"
    private const val MAX_REDIRECTS = 5

    fun normalizeHost(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.startsWith("http://", ignoreCase = true)) return trimmed
        if (trimmed.startsWith("https://", ignoreCase = true)) return trimmed
        // No scheme — promote to https:// for explicit secure ports, else http.
        val portMatch = Regex(":(\\d+)\\b").find(trimmed)
        val port = portMatch?.groupValues?.get(1)?.toIntOrNull()
        val scheme = when (port) {
            443, 8443 -> "https://"
            else -> "http://"
        }
        return scheme + trimmed
    }

    /**
     * Strip the default port from a URL so the Host header doesn't include
     * an explicit `:443` (https) or `:80` (http) suffix. Some Xtream WAFs
     * reject `Host: foo:443` because the cert is for `foo` and the routing
     * rule expects the bare hostname.
     */
    fun stripDefaultPort(url: String): String {
        return url
            .replace(Regex("^(https://[^/:]+):443(?=[/?]|$)"), "$1")
            .replace(Regex("^(http://[^/:]+):80(?=[/?]|$)"), "$1")
    }

    suspend fun fetchLiveCategories(host: String, user: String, pass: String): List<XtreamCategory> =
        fetchCategoriesAction(host, user, pass, "get_live_categories")

    suspend fun fetchVodCategories(host: String, user: String, pass: String): List<XtreamCategory> =
        fetchCategoriesAction(host, user, pass, "get_vod_categories")

    suspend fun fetchSeriesCategories(host: String, user: String, pass: String): List<XtreamCategory> =
        fetchCategoriesAction(host, user, pass, "get_series_categories")

    suspend fun fetchLiveStreams(host: String, user: String, pass: String): List<XtreamLiveStream> =
        withContext(Dispatchers.IO) {
            parseLiveStreams(get(host, user, pass, "get_live_streams"))
        }

    suspend fun fetchVodStreams(host: String, user: String, pass: String): List<XtreamVodStream> =
        withContext(Dispatchers.IO) {
            parseVodStreams(get(host, user, pass, "get_vod_streams"))
        }

    suspend fun fetchSeries(host: String, user: String, pass: String): List<XtreamSeries> =
        withContext(Dispatchers.IO) {
            parseSeries(get(host, user, pass, "get_series"))
        }

    suspend fun fetchSeriesInfo(
        host: String, user: String, pass: String, seriesId: Int,
    ): List<XtreamSeriesEpisode> = withContext(Dispatchers.IO) {
        parseSeriesEpisodes(
            get(host, user, pass, "get_series_info&series_id=$seriesId"),
        )
    }

    suspend fun fetchXmltv(host: String, user: String, pass: String): String =
        withContext(Dispatchers.IO) {
            val cleanHost = normalizeHost(host)
            val u = URLEncoder.encode(user, "UTF-8")
            val p = URLEncoder.encode(pass, "UTF-8")
            httpGet(URL("$cleanHost/xmltv.php?username=$u&password=$p"))
        }

    /** M3U-plus playlist with the *real* per-channel stream URLs. */
    suspend fun fetchM3uPlus(host: String, user: String, pass: String): String =
        withContext(Dispatchers.IO) {
            val cleanHost = normalizeHost(host)
            val u = URLEncoder.encode(user, "UTF-8")
            val p = URLEncoder.encode(pass, "UTF-8")
            httpGet(URL("$cleanHost/get.php?username=$u&password=$p&type=m3u_plus"))
        }

    /**
     * Stream the M3U-plus playlist. Caller consumes the [InputStream]
     * inside [block]; we manage redirects, headers, and disconnect.
     * Use this for large playlists instead of [fetchM3uPlus] — a 100k-
     * channel playlist is ~140 MB as a single String and will OOM the
     * Fire TV Stick's 256 MB per-process heap.
     */
    suspend fun streamM3uPlus(
        host: String, user: String, pass: String,
        block: (InputStream) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val cleanHost = normalizeHost(host)
        val u = URLEncoder.encode(user, "UTF-8")
        val p = URLEncoder.encode(pass, "UTF-8")
        httpStream(URL("$cleanHost/get.php?username=$u&password=$p&type=m3u_plus"), block)
    }

    /**
     * Probe helper used by the in-app HTTP probe screen. Returns the
     * full redirect chain with response code, content-type, and first
     * [bodyLimit] bytes of the final body — without materializing more
     * than that into memory. Surfaces server error messages without
     * OOMing on huge responses.
     */
    suspend fun probe(url: String, bodyLimit: Int = 800): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        var current = URL(url)
        var hops = 0
        while (true) {
            val conn = current.openConnection() as HttpURLConnection
            var advance: URL? = null
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "*/*")
                val code = conn.responseCode
                sb.append("→ GET ").append(current).append('\n')
                sb.append("← HTTP ").append(code)
                conn.contentType?.let { sb.append("  (").append(it).append(")") }
                sb.append('\n')
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    sb.append("  Location: ").append(loc ?: "(missing)").append('\n')
                    if (loc == null) return@withContext sb.toString()
                    if (++hops > MAX_REDIRECTS) {
                        sb.append("  (stopped — too many redirects)\n")
                        return@withContext sb.toString()
                    }
                    advance = URL(current, loc)
                } else {
                    val stream = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    if (stream != null) {
                        val buf = ByteArray(bodyLimit)
                        val n = stream.read(buf)
                        if (n > 0) {
                            sb.append("  body (first $n bytes): ")
                            sb.append(
                                String(buf, 0, n, Charsets.UTF_8)
                                    .replace(Regex("\\s+"), " ")
                                    .trim()
                            )
                            sb.append('\n')
                        } else {
                            sb.append("  body: (empty)\n")
                        }
                        stream.close()
                    }
                    return@withContext sb.toString()
                }
            } catch (t: Throwable) {
                sb.append("  exception: ").append(t::class.simpleName)
                    .append(": ").append(t.message).append('\n')
                return@withContext sb.toString()
            } finally {
                conn.disconnect()
            }
            // outside the try/finally so the connection is closed before we open the next one
            current = advance ?: return@withContext sb.toString()
        }
        @Suppress("UNREACHABLE_CODE") sb.toString()
    }

    // Live URL fallback uses .ts (MPEG-TS) rather than .m3u8 — TiviMate and
    // most Xtream-native players use .ts and many providers 405 the .m3u8
    // variant on live streams. This only fires when M3uIndex didn't return
    // a URL for the channel; the M3U URL (already correct format) takes
    // precedence everywhere it's available.
    fun buildLiveStreamUrl(host: String, user: String, pass: String, streamId: Int): String =
        stripDefaultPort("${normalizeHost(host)}/live/$user/$pass/$streamId.ts")

    fun buildVodUrl(host: String, user: String, pass: String, streamId: Int, ext: String): String =
        stripDefaultPort("${normalizeHost(host)}/movie/$user/$pass/$streamId.${ext.ifBlank { "mp4" }}")

    fun buildSeriesEpisodeUrl(host: String, user: String, pass: String, episodeId: String, ext: String): String =
        stripDefaultPort("${normalizeHost(host)}/series/$user/$pass/$episodeId.${ext.ifBlank { "mp4" }}")

    // ---------------- internal ----------------

    private suspend fun fetchCategoriesAction(
        host: String, user: String, pass: String, action: String,
    ): List<XtreamCategory> = withContext(Dispatchers.IO) {
        val text = get(host, user, pass, action)
        when (val token = JSONTokener(text).nextValue()) {
            is JSONArray -> List(token.length()) { i ->
                val o = token.getJSONObject(i)
                XtreamCategory(
                    categoryId = o.optString("category_id", ""),
                    name = o.optString("category_name", ""),
                )
            }
            is JSONObject -> throw IOException(translateXtreamError(token))
            else -> throw IOException("Unexpected JSON shape for $action")
        }
    }

    internal fun parseLiveStreams(text: String): List<XtreamLiveStream> {
        return when (val token = JSONTokener(text).nextValue()) {
            is JSONArray -> List(token.length()) { i ->
                val o = token.getJSONObject(i)
                XtreamLiveStream(
                    streamId = o.getInt("stream_id"),
                    name = o.optString("name", ""),
                    categoryId = o.optString("category_id", ""),
                    streamIcon = o.optString("stream_icon", ""),
                    epgChannelId = o.optString("epg_channel_id", ""),
                )
            }
            is JSONObject -> throw IOException(translateXtreamError(token))
            else -> throw IOException("Unexpected response — first 200 chars: ${text.take(200)}")
        }
    }

    private fun parseVodStreams(text: String): List<XtreamVodStream> {
        return when (val token = JSONTokener(text).nextValue()) {
            is JSONArray -> List(token.length()) { i ->
                val o = token.getJSONObject(i)
                XtreamVodStream(
                    streamId = o.getInt("stream_id"),
                    name = o.optString("name", ""),
                    categoryId = o.optString("category_id", ""),
                    streamIcon = o.optString("stream_icon", ""),
                    containerExtension = o.optString("container_extension", "mp4"),
                    rating = o.optString("rating", ""),
                    year = o.optString("year", ""),
                )
            }
            is JSONObject -> throw IOException(translateXtreamError(token))
            else -> throw IOException("Unexpected VOD response")
        }
    }

    private fun parseSeries(text: String): List<XtreamSeries> {
        return when (val token = JSONTokener(text).nextValue()) {
            is JSONArray -> List(token.length()) { i ->
                val o = token.getJSONObject(i)
                XtreamSeries(
                    seriesId = o.getInt("series_id"),
                    name = o.optString("name", ""),
                    categoryId = o.optString("category_id", ""),
                    cover = o.optString("cover", ""),
                    plot = o.optString("plot", ""),
                    rating = o.optString("rating", ""),
                    year = o.optString("year", o.optString("releaseDate", "")),
                )
            }
            is JSONObject -> throw IOException(translateXtreamError(token))
            else -> throw IOException("Unexpected series response")
        }
    }

    private fun parseSeriesEpisodes(text: String): List<XtreamSeriesEpisode> {
        // get_series_info returns { "info": {...}, "episodes": { "1": [...], "2": [...] } }
        return when (val token = JSONTokener(text).nextValue()) {
            is JSONObject -> {
                val episodesNode = token.optJSONObject("episodes") ?: return emptyList()
                val out = mutableListOf<XtreamSeriesEpisode>()
                episodesNode.keys().forEach { seasonKey ->
                    val arr = episodesNode.optJSONArray(seasonKey) ?: return@forEach
                    val seasonNum = seasonKey.toIntOrNull() ?: 0
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val info = o.optJSONObject("info") ?: JSONObject()
                        out += XtreamSeriesEpisode(
                            episodeId = o.optString("id", ""),
                            title = o.optString("title", ""),
                            episodeNum = o.optInt("episode_num", 0),
                            seasonNum = seasonNum,
                            containerExtension = o.optString("container_extension", "mp4"),
                            plot = info.optString("plot", ""),
                        )
                    }
                }
                out.sortedWith(compareBy({ it.seasonNum }, { it.episodeNum }))
            }
            else -> throw IOException("Unexpected series_info response")
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
                status.equals("Banned", true) -> "Account banned by provider"
                status.equals("Disabled", true) -> "Account disabled by provider"
                status.equals("Expired", true) -> "Account expired"
                message.isNotEmpty() -> "Provider says: $message"
                else -> "Unexpected user_info response (status=$status, auth=$auth)"
            }
        }
        val errorMsg = obj.optString("error", "")
            .ifEmpty { obj.optString("message", "") }
            .ifEmpty { obj.toString().take(200) }
        return "Server error: $errorMsg"
    }

    private fun get(host: String, user: String, pass: String, action: String): String {
        val cleanHost = normalizeHost(host)
        val u = URLEncoder.encode(user, "UTF-8")
        val p = URLEncoder.encode(pass, "UTF-8")
        return httpGet(URL("$cleanHost/player_api.php?username=$u&password=$p&action=$action"))
    }

    /**
     * Connect to [url] and hand the response body InputStream to [block].
     * Same redirect/UA/error semantics as [httpGet] but never materializes
     * the body as a String — required for large playlists.
     */
    private fun httpStream(url: URL, block: (InputStream) -> Unit) {
        var current = url
        var hops = 0
        while (true) {
            val conn = current.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 60_000
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "*/*")
                val code = conn.responseCode

                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("HTTP $code from $current with no Location header")
                    if (++hops > MAX_REDIRECTS) {
                        throw IOException("Too many redirects (>${MAX_REDIRECTS}) starting from $url")
                    }
                    current = URL(current, location)
                    continue
                }

                if (code !in 200..299) {
                    val body = runCatching {
                        (conn.errorStream ?: conn.inputStream)
                            ?.bufferedReader()?.use { it.readText() }
                            ?: ""
                    }.getOrDefault("")
                    val snippet = body.take(300).replace(Regex("\\s+"), " ").trim()
                    val tail = if (snippet.isNotEmpty()) " — $snippet" else ""
                    throw IOException("HTTP $code from $current$tail")
                }

                (conn.inputStream ?: throw IOException("Empty response from $current"))
                    .use(block)
                return
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun httpGet(url: URL): String {
        var current = url
        var hops = 0
        while (true) {
            val conn = current.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = false  // we handle redirects ourselves
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "*/*")
                val code = conn.responseCode

                // Manual redirect handling so http→https (and vice versa) works.
                // HttpURLConnection refuses cross-protocol redirects silently.
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("HTTP $code from $current with no Location header")
                    if (++hops > MAX_REDIRECTS) {
                        throw IOException("Too many redirects (>${MAX_REDIRECTS}) starting from $url")
                    }
                    current = URL(current, location)
                    continue
                }

                if (code !in 200..299) {
                    // Include up to 300 chars of the response body so providers'
                    // error messages (often plain text or HTML) surface in
                    // the UI instead of just "HTTP 405".
                    val body = runCatching {
                        (conn.errorStream ?: conn.inputStream)
                            ?.bufferedReader()?.use { it.readText() }
                            ?: ""
                    }.getOrDefault("")
                    val snippet = body.take(300).replace(Regex("\\s+"), " ").trim()
                    val tail = if (snippet.isNotEmpty()) " — $snippet" else ""
                    throw IOException("HTTP $code from $current$tail")
                }

                return (conn.inputStream ?: conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw IOException("Empty response from $current")
            } finally {
                conn.disconnect()
            }
        }
    }
}
