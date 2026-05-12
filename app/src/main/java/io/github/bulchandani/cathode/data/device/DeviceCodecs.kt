package io.github.bulchandani.cathode.data.device

import android.media.MediaCodecInfo
import android.media.MediaCodecList

/**
 * Queries Android's MediaCodec registry to discover what this device's
 * decoders actually support. Used to surface in Settings → About →
 * Device decoders so a user staring at a "format exceeds capabilities"
 * error can see *why* — typically the channel's profile/level is above
 * what their hardware advertises.
 *
 * Note: a decoder advertising support up to Level 5.1 does not mean
 * every Level 5.1 stream will play. Real-world capability also depends
 * on bitrate, profile (Main vs Main 10), color depth, and HDR metadata.
 * This view is a best-effort baseline.
 */
object DeviceCodecs {

    data class VideoDecoder(
        val name: String,
        val mime: String,
        val maxWidth: Int,
        val maxHeight: Int,
        val maxFps: Int,
        val maxBitrateMbps: Int,
        val profileLevels: List<String>,
        val hardwareAccelerated: Boolean,
    )

    /** Friendly label for a MIME type → display string. */
    private val MIME_LABELS = mapOf(
        "video/avc" to "H.264 / AVC",
        "video/hevc" to "H.265 / HEVC",
        "video/x-vnd.on2.vp8" to "VP8",
        "video/x-vnd.on2.vp9" to "VP9",
        "video/av01" to "AV1",
        "video/mp4v-es" to "MPEG-4",
        "video/3gpp" to "H.263",
        "video/mpeg2" to "MPEG-2",
        "video/dolby-vision" to "Dolby Vision",
    )

    fun friendlyMime(mime: String): String = MIME_LABELS[mime] ?: mime

    fun listVideoDecoders(): List<VideoDecoder> {
        val out = mutableListOf<VideoDecoder>()
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in list.codecInfos) {
            if (info.isEncoder) continue
            for (mime in info.supportedTypes) {
                if (!mime.startsWith("video/")) continue
                val caps = runCatching { info.getCapabilitiesForType(mime) }.getOrNull() ?: continue
                val video = caps.videoCapabilities ?: continue
                val profileLevels = caps.profileLevels.orEmpty().map { pl ->
                    "${profileName(mime, pl.profile)} L${levelName(mime, pl.level)}"
                }.distinct()
                val hwAccelerated = isHardwareAccelerated(info)
                out += VideoDecoder(
                    name = info.name,
                    mime = mime,
                    maxWidth = video.supportedWidths.upper,
                    maxHeight = video.supportedHeights.upper,
                    maxFps = video.supportedFrameRates.upper,
                    maxBitrateMbps = (video.bitrateRange.upper / 1_000_000).coerceAtLeast(0),
                    profileLevels = profileLevels,
                    hardwareAccelerated = hwAccelerated,
                )
            }
        }
        return out.sortedWith(
            compareByDescending<VideoDecoder> { it.hardwareAccelerated }
                .thenByDescending { it.maxWidth.toLong() * it.maxHeight }
                .thenBy { it.mime },
        )
    }

    /**
     * One-line summary for the most capable HEVC and AVC decoders on
     * this device. Quick "can this device play 4K?" answer.
     */
    fun summary(): String {
        val all = listVideoDecoders()
        val hevc = all.firstOrNull { it.mime == "video/hevc" && it.hardwareAccelerated }
        val avc = all.firstOrNull { it.mime == "video/avc" && it.hardwareAccelerated }
        val av1 = all.firstOrNull { it.mime == "video/av01" && it.hardwareAccelerated }

        fun line(label: String, d: VideoDecoder?): String =
            if (d == null) "$label: not advertised"
            else "$label: ${d.maxWidth}×${d.maxHeight} @ ${d.maxFps}fps"

        return buildString {
            appendLine(line("HEVC (H.265)", hevc))
            appendLine(line("AVC (H.264)", avc))
            if (av1 != null) appendLine(line("AV1", av1))
            val any10bit = all.any { d -> d.profileLevels.any { it.contains("Main10") || it.contains("10") } }
            append("10-bit / HDR profiles: ")
            append(if (any10bit) "advertised by at least one decoder" else "not advertised")
        }.trimEnd()
    }

    private fun isHardwareAccelerated(info: MediaCodecInfo): Boolean = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            info.isHardwareAccelerated
        } else {
            // Pre-Q heuristic: hardware codecs typically don't start with "OMX.google."
            !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                !info.name.startsWith("c2.android.", ignoreCase = true)
        }
    } catch (_: Throwable) {
        false
    }

    /** Human-readable HEVC/AVC profile labels for the most common values. */
    private fun profileName(mime: String, profile: Int): String = when (mime) {
        "video/hevc" -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain -> "Main"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 -> "Main10"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 -> "Main10-HDR10"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus -> "Main10-HDR10+"
            MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill -> "MainStill"
            else -> "HEVC-Profile-$profile"
        }
        "video/avc" -> when (profile) {
            MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline -> "Baseline"
            MediaCodecInfo.CodecProfileLevel.AVCProfileMain -> "Main"
            MediaCodecInfo.CodecProfileLevel.AVCProfileExtended -> "Extended"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh -> "High"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10 -> "High10"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422 -> "High422"
            MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444 -> "High444"
            MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline -> "ConstrainedBaseline"
            MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh -> "ConstrainedHigh"
            else -> "AVC-Profile-$profile"
        }
        else -> profile.toString()
    }

    /** Translate the integer level constant into the canonical "5.1" style. */
    private fun levelName(mime: String, level: Int): String = when (mime) {
        "video/hevc" -> when (level) {
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel1 -> "1.0"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel2 -> "2.0"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel21 -> "2.1"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel3 -> "3.0"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel31 -> "3.1"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel4 -> "4.0"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel41 -> "4.1"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel5 -> "5.0"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51 -> "5.1"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel52 -> "5.2"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel6 -> "6.0"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel61 -> "6.1"
            MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel62 -> "6.2"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel41 -> "4.1-H"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel5 -> "5.0-H"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel51 -> "5.1-H"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel52 -> "5.2-H"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel6 -> "6.0-H"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel61 -> "6.1-H"
            MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62 -> "6.2-H"
            else -> level.toString()
        }
        "video/avc" -> when (level) {
            MediaCodecInfo.CodecProfileLevel.AVCLevel1 -> "1.0"
            MediaCodecInfo.CodecProfileLevel.AVCLevel11 -> "1.1"
            MediaCodecInfo.CodecProfileLevel.AVCLevel12 -> "1.2"
            MediaCodecInfo.CodecProfileLevel.AVCLevel13 -> "1.3"
            MediaCodecInfo.CodecProfileLevel.AVCLevel2 -> "2.0"
            MediaCodecInfo.CodecProfileLevel.AVCLevel21 -> "2.1"
            MediaCodecInfo.CodecProfileLevel.AVCLevel22 -> "2.2"
            MediaCodecInfo.CodecProfileLevel.AVCLevel3 -> "3.0"
            MediaCodecInfo.CodecProfileLevel.AVCLevel31 -> "3.1"
            MediaCodecInfo.CodecProfileLevel.AVCLevel32 -> "3.2"
            MediaCodecInfo.CodecProfileLevel.AVCLevel4 -> "4.0"
            MediaCodecInfo.CodecProfileLevel.AVCLevel41 -> "4.1"
            MediaCodecInfo.CodecProfileLevel.AVCLevel42 -> "4.2"
            MediaCodecInfo.CodecProfileLevel.AVCLevel5 -> "5.0"
            MediaCodecInfo.CodecProfileLevel.AVCLevel51 -> "5.1"
            MediaCodecInfo.CodecProfileLevel.AVCLevel52 -> "5.2"
            else -> level.toString()
        }
        else -> level.toString()
    }
}
