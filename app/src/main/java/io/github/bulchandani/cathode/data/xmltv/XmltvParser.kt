package io.github.bulchandani.cathode.data.xmltv

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class XmltvProgramme(
    val channel: String,
    val startMillis: Long,
    val stopMillis: Long,
    val title: String,
    val description: String,
)

object XmltvParser {

    /**
     * Streaming pull-parser that handles 100k+ programmes without
     * blowing memory. Returns a map of `channel-id -> sorted programmes`.
     */
    suspend fun parse(input: InputStream): Map<String, List<XmltvProgramme>> =
        withContext(Dispatchers.Default) {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)

            val result = mutableMapOf<String, MutableList<XmltvProgramme>>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "programme") {
                    val channel = parser.getAttributeValue(null, "channel") ?: ""
                    val start = parseXmltvTime(parser.getAttributeValue(null, "start"))
                    val stop = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                    var title = ""
                    var description = ""

                    while (true) {
                        val ev = parser.next()
                        if (ev == XmlPullParser.END_TAG && parser.name == "programme") break
                        if (ev == XmlPullParser.END_DOCUMENT) break
                        if (ev != XmlPullParser.START_TAG) continue
                        when (parser.name) {
                            "title" -> title = parser.nextText().orEmpty()
                            "desc" -> description = parser.nextText().orEmpty()
                            else -> skip(parser)
                        }
                    }

                    if (channel.isNotEmpty() && start > 0 && stop > 0) {
                        result.getOrPut(channel) { mutableListOf() }
                            .add(XmltvProgramme(channel, start, stop, title, description))
                    }
                }
                event = parser.next()
            }
            // sort each channel's programmes by start time
            result.mapValues { (_, v) -> v.sortedBy { it.startMillis } }
        }

    private fun skip(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    /** XMLTV time: `20260509214200 +0000`, also accepts the 14-digit form. */
    private fun parseXmltvTime(raw: String?): Long {
        if (raw.isNullOrEmpty()) return 0L
        val withTz = raw.length > 14
        val pattern = if (withTz) "yyyyMMddHHmmss Z" else "yyyyMMddHHmmss"
        val fmt = SimpleDateFormat(pattern, Locale.US)
        if (!withTz) fmt.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            fmt.parse(raw)?.time ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }
}

private fun String?.orEmpty(): String = this ?: ""
