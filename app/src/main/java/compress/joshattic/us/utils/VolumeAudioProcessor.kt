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
        // If the encoding is not one we can process directly, output PCM 16-bit instead.
        // This handles exotic encodings like PCM_24BIT (encoding=21) that some
        // devices/Android versions produce, rather than crashing with UnhandledAudioFormatException.
        val outputEncoding = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_FLOAT -> inputAudioFormat.encoding
            else -> C.ENCODING_PCM_16BIT
        }

        // Bypass if volume is 100% and no encoding conversion is needed
        if (volume == 1f && outputEncoding == inputAudioFormat.encoding) {
            return AudioFormat.NOT_SET
        }

        return AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, outputEncoding)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) {
            return
        }

        // Output buffer size may differ from input if we're converting encoding
        // (e.g. PCM_24BIT_PACKED is 3 bytes/sample → PCM_16BIT is 2 bytes/sample)
        val outputBuffer = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> replaceOutputBuffer(remaining)
            C.ENCODING_PCM_FLOAT -> replaceOutputBuffer(remaining)
            C.ENCODING_PCM_24BIT -> {
                // 3 bytes in → 2 bytes out per sample
                val sampleCount = remaining / 3
                replaceOutputBuffer(sampleCount * 2)
            }
            else -> replaceOutputBuffer(remaining)
        }

        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> scalePcm16(inputBuffer, outputBuffer)
            C.ENCODING_PCM_FLOAT -> scalePcmFloat(inputBuffer, outputBuffer)
            C.ENCODING_PCM_24BIT -> scalePcm24Packed(inputBuffer, outputBuffer)
            else -> outputBuffer.put(inputBuffer) // passthrough for unknown
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

    /**
     * Reads PCM_24BIT_PACKED samples (3 bytes, little-endian, signed) and writes
     * volume-scaled PCM_16BIT samples (2 bytes) to [output].
     */
    private fun scalePcm24Packed(input: ByteBuffer, output: ByteBuffer) {
        while (input.remaining() >= 3) {
            val b0 = input.get().toInt() and 0xFF
            val b1 = input.get().toInt() and 0xFF
            val b2 = input.get().toInt() // sign-extended
            // Reconstruct 24-bit signed value (little-endian)
            val sample24 = (b2 shl 16) or (b1 shl 8) or b0
            // Scale down to 16-bit range and apply volume
            val sample16 = sample24 shr 8
            val scaled = (sample16 * volume).toInt()
            val clipped = max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), scaled)).toShort()
            output.putShort(clipped)
        }
    }
}
