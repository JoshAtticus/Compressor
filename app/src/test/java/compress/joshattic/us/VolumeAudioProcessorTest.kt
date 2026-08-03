package compress.joshattic.us

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import compress.joshattic.us.utils.VolumeAudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class VolumeAudioProcessorTest {

    @Test
    fun testVolumeScalingPcm16() {
        val processor = VolumeAudioProcessor()
        processor.setVolume(0.5f)
        
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(format)
        
        assertEquals(format.sampleRate, outputFormat.sampleRate)
        assertEquals(format.channelCount, outputFormat.channelCount)
        assertEquals(C.ENCODING_PCM_16BIT, outputFormat.encoding)
        
        processor.flush()
        
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.putShort(1000)
        input.putShort(-2000)
        input.flip()
        
        processor.queueInput(input)
        val output = processor.output
        
        assertEquals(500.toShort(), output.short)
        assertEquals((-1000).toShort(), output.short)
    }

    @Test
    fun testVolumeClippingPcm16() {
        val processor = VolumeAudioProcessor()
        processor.setVolume(2.0f)
        
        val format = AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()
        
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        input.putShort(20000)
        input.putShort(-20000)
        input.flip()
        
        processor.queueInput(input)
        val output = processor.output
        
        assertEquals(Short.MAX_VALUE, output.short)
        assertEquals(Short.MIN_VALUE, output.short)
    }

    @Test
    fun testVolumeScalingPcmFloat() {
        val processor = VolumeAudioProcessor()
        processor.setVolume(0.5f)
        
        val format = AudioFormat(44100, 1, C.ENCODING_PCM_FLOAT)
        processor.configure(format)
        processor.flush()
        
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        input.putFloat(1.0f)
        input.putFloat(-0.5f)
        input.flip()
        
        processor.queueInput(input)
        val output = processor.output
        
        assertEquals(0.5f, output.float, 0.0001f)
        assertEquals(-0.25f, output.float, 0.0001f)
    }

    @Test
    fun testBypassWhenVolumeIsOne() {
        val processor = VolumeAudioProcessor()
        processor.setVolume(1.0f)
        
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(format)
        
        assertEquals(AudioFormat.NOT_SET, outputFormat)
        assertTrue(!processor.isActive)
    }
}
