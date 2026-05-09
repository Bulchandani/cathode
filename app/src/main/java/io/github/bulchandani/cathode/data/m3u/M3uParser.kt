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
     * Streaming M3U/M3U8 reader. Tolerates BOM, blank lines, comments,
     * and missing attributes. Each `#EXTINF` line + the URL on the
     * following non-comment line becomes one [M3uChannel].
     */
    suspend fun parse(input: InputStream): List<M3uChannel> = withContext(Dispatchers.Default) {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val out = mutableListOf<M3uChannel>()
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

                out += M3uChannel(
                    name = displayName.ifBlank { attrs["tvg-name"] ?: line.substringAfterLast('/') },
                    url = line,
                    tvgId = attrs["tvg-id"].orEmpty(),
                    tvgLogo = attrs["tvg-logo"].orEmpty(),
                    groupTitle = attrs["group-title"].orEmpty(),
                )
            }
        }

        out
    }
}

private fun String?.orEmpty(): String = this ?: ""
