package io.github.bulchandani.cathode.data.m3u

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class M3uChannel(
    val name: String,
    val url: String,
    val tvgId: String,
    val tvgLogo: String,
    val groupTitle: String,
)

object M3uParser {
    private val ATTR_REGEX = Regex("""([\w-]+)=\"([^\"]*)\"""")

    /**
     * Fully buffered parse — convenient for tests and small playlists.
     * On a Fire TV Stick with a 100k-channel provider the resulting
     * `List<M3uChannel>` alone can blow the per-process heap, so prefer
     * [parseStreaming] in production.
     */
    suspend fun parse(input: InputStream): List<M3uChannel> = withContext(Dispatchers.Default) {
        val out = mutableListOf<M3uChannel>()
        parseStreamingBlocking(input) { out += it }
        out
    }

    /**
     * Streaming parse — emits each channel via [onChannel] without
     * holding the full list in memory. Caller's callback is the only
     * place the channel exists. Used by M3uIndex to keep peak memory
     * bounded on large playlists from Xtream providers.
     *
     * Runs on Dispatchers.Default for parsing CPU work.
     */
    suspend fun parseStreaming(
        input: InputStream,
        onChannel: (M3uChannel) -> Unit,
    ) = withContext(Dispatchers.Default) {
        parseStreamingBlocking(input, onChannel)
    }

    private fun parseStreamingBlocking(
        input: InputStream,
        onChannel: (M3uChannel) -> Unit,
    ) {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        var pendingExtInf: String? = null

        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim().let {
                    if (it.startsWith("﻿")) it.removePrefix("﻿") else it
                }
                if (line.isBlank()) continue
                if (line.startsWith("#EXTM3U")) continue
                if (line.startsWith("#EXTINF")) {
                    pendingExtInf = line
                    continue
                }
                if (line.startsWith("#")) continue
                // It's a URL — pair with the most recent EXTINF
                val ext = pendingExtInf ?: continue
                pendingExtInf = null

                val attrs = ATTR_REGEX.findAll(ext).associate { m ->
                    m.groupValues[1].lowercase() to m.groupValues[2]
                }
                val displayName = ext.substringAfterLast(',', "").trim()

                onChannel(
                    M3uChannel(
                        name = displayName.ifBlank { attrs["tvg-name"] ?: line.substringAfterLast('/') },
                        url = line,
                        tvgId = attrs["tvg-id"].orEmpty(),
                        tvgLogo = attrs["tvg-logo"].orEmpty(),
                        groupTitle = attrs["group-title"].orEmpty(),
                    )
                )
            }
        }
    }
}

private fun String?.orEmpty(): String = this ?: ""
