package io.github.bulchandani.cathode.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio offset processor. Positive `offsetMs` injects silence at the
 * head of the stream so audio plays N ms later than video; negative
 * drops the leading bytes so audio plays earlier.
 *
 * NOTE: applies on next prepare() — change `offsetMs`, then re-prep.
 * Mid-stream drift is not implemented yet; a real fix needs a per-
 * frame buffer rewrite which is its own project. v0.7.0 ships the
 * UI + initial-offset behavior; v0.8.0 will refine.
 */
class AudioSyncProcessor : BaseAudioProcessor() {

    @Volatile var offsetMs: Int = 0
    private var bytesRemainingToInject: Int = 0
    private var bytesRemainingToDrop: Int = 0
    private var primed = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        primed = false
        bytesRemainingToInject = 0
        bytesRemainingToDrop = 0
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!primed) {
            primed = true
            val bytesPerMs = inputAudioFormat.sampleRate * inputAudioFormat.channelCount * 2 / 1000
            if (offsetMs > 0) bytesRemainingToInject = offsetMs * bytesPerMs
            else if (offsetMs < 0) bytesRemainingToDrop = (-offsetMs) * bytesPerMs
        }

        // Drop initial bytes if we want audio earlier
        if (bytesRemainingToDrop > 0) {
            val dropNow = minOf(bytesRemainingToDrop, inputBuffer.remaining())
            inputBuffer.position(inputBuffer.position() + dropNow)
            bytesRemainingToDrop -= dropNow
            if (!inputBuffer.hasRemaining()) return
        }

        // Inject silence at the head if we want audio later
        if (bytesRemainingToInject > 0) {
            val out = replaceOutputBuffer(bytesRemainingToInject + inputBuffer.remaining())
                .order(ByteOrder.nativeOrder())
            repeat(bytesRemainingToInject) { out.put(0) }
            bytesRemainingToInject = 0
            out.put(inputBuffer)
            out.flip()
            return
        }

        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }
}

/** Process-wide audio sync offset; CathodePlayerFactory reads this on build. */
object AudioSyncState {
    @Volatile var offsetMs: Int = 0
}

