package compress.joshattic.us.utils

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * An [AudioProcessor] that scales the volume of the audio.
 */
@UnstableApi
class VolumeAudioProcessor : BaseAudioProcessor() {

    private var volume = 1f

    /**
     * Sets the volume scale factor.
     *
     * @param volume The volume scale factor. 1.0 is original volume, 0.0 is silent.
     */
    fun setVolume(volume: Float) {
        this.volume = volume
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if ((inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) &&
            (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT)
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        // Bypass if volume is 100%
        if (volume == 1f) {
            return AudioFormat.NOT_SET
        }

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) {
            return
        }

        val outputBuffer = replaceOutputBuffer(remaining)

        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            scalePcm16(inputBuffer, outputBuffer)
        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            scalePcmFloat(inputBuffer, outputBuffer)
        }
        outputBuffer.flip()
    }

    private fun scalePcm16(input: ByteBuffer, output: ByteBuffer) {
        while (input.hasRemaining()) {
            val sample = input.short
            val scaled = (sample * volume).toInt()
            val clipped = max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), scaled)).toShort()
            output.putShort(clipped)
        }
    }

    private fun scalePcmFloat(input: ByteBuffer, output: ByteBuffer) {
        while (input.hasRemaining()) {
            val sample = input.float
            output.putFloat(sample * volume)
        }
    }
}
