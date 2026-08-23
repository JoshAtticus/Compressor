package compress.joshattic.us.viewmodel

import android.app.Application
import androidx.media3.common.MimeTypes
import androidx.test.core.app.ApplicationProvider
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.model.FilenameSegment
import compress.joshattic.us.model.QualityPreset
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CompressorViewModelTest {

    private lateinit var viewModel: CompressorViewModel
    private val application: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        viewModel = CompressorViewModel(application)
    }

    @Test
    fun testSerializeSegments() {
        val segments = listOf(
            FilenameSegment.Token("original_name"),
            FilenameSegment.Text("_"),
            FilenameSegment.Token("compressed")
        )
        val serialized = viewModel.serializeSegments(segments)
        assertEquals("token:original_name|text:_|token:compressed", serialized)
    }

    @Test
    fun testDeserializeSegments() {
        val raw = "token:original_name|text:_|token:compressed"
        val segments = viewModel.deserializeSegments(raw)
        
        // deserializeSegments calls normalizeSegments, which adds empty Text segments if needed
        // Based on the code, it adds Text segments around tokens if they are not there.
        // Actually, normalizeSegments in the code:
        // result.add(FilenameSegment.Text(currentText))
        // result.add(seg)
        // ...
        // result.add(FilenameSegment.Text(currentText))
        
        assertTrue(segments.any { it is FilenameSegment.Token && it.key == "original_name" })
        assertTrue(segments.any { it is FilenameSegment.Text && it.value == "_" })
        assertTrue(segments.any { it is FilenameSegment.Token && it.key == "compressed" })
    }

    @Test
    fun testNormalizeSegments() {
        val input = listOf(
            FilenameSegment.Text("a"),
            FilenameSegment.Text("b"),
            FilenameSegment.Token("t1"),
            FilenameSegment.Text("c")
        )
        val normalized = viewModel.normalizeSegments(input)
        
        // Expected: Text("ab"), Token("t1"), Text("c")
        assertEquals(3, normalized.size)
        assertEquals("ab", (normalized[0] as FilenameSegment.Text).value)
        assertEquals("t1", (normalized[1] as FilenameSegment.Token).key)
        assertEquals("c", (normalized[2] as FilenameSegment.Text).value)
    }

    @Test
    fun testApplyPreset_High() {
        viewModel.applyPreset(QualityPreset.HIGH)
        val state = viewModel.uiState.value
        assertEquals(QualityPreset.HIGH, state.activePreset)
        assertTrue(state.targetResolutionLocked)
        assertTrue(state.targetFpsLocked)
        assertTrue(state.audioBitrateLocked)
    }

    @Test
    fun testApplyPreset_Medium() {
        viewModel.applyPreset(QualityPreset.MEDIUM)
        val state = viewModel.uiState.value
        assertEquals(QualityPreset.MEDIUM, state.activePreset)
    }

    @Test
    fun testApplyPreset_Low() {
        viewModel.applyPreset(QualityPreset.LOW)
        val state = viewModel.uiState.value
        assertEquals(QualityPreset.LOW, state.activePreset)
    }

    @Test
    fun testSetTargetSize() {
        viewModel.setTargetSize(25f)
        assertEquals(25f, viewModel.uiState.value.targetSizeMb, 0.01f)
        assertEquals(QualityPreset.CUSTOM, viewModel.uiState.value.activePreset)
    }

    @Test
    fun testSetVideoCodec() {
        viewModel.setVideoCodec(MimeTypes.VIDEO_H264)
        assertEquals(MimeTypes.VIDEO_H264, viewModel.uiState.value.videoCodec)
        assertFalse(viewModel.uiState.value.useH265)
        assertEquals(QualityPreset.CUSTOM, viewModel.uiState.value.activePreset)
    }

    @Test
    fun testCompressedOutputFileName() {
        val state = CompressorUiState(
            originalName = "myvideo.mp4",
            targetResolutionHeight = 720,
            targetFps = 30,
            videoCodec = MimeTypes.VIDEO_H264,
            filenameSegments = listOf(
                FilenameSegment.Token("original_name"),
                FilenameSegment.Text("_"),
                FilenameSegment.Token("resolution"),
                FilenameSegment.Text("_"),
                FilenameSegment.Token("compressed")
            )
        )
        val fileName = viewModel.compressedOutputFileName(state)
        assertEquals("myvideo_720p_Compressed.mp4", fileName)
    }

    @Test
    fun testCreateMediaItemSequence_withAudio() {
        val mediaItem = androidx.media3.common.MediaItem.fromUri("file:///test.mp4")
        val editedMediaItem = androidx.media3.transformer.EditedMediaItem.Builder(mediaItem).build()
        val sequence = viewModel.createMediaItemSequence(editedMediaItem, shouldIncludeAudio = true)
        assertNotNull(sequence)
        assertEquals(1, sequence.editedMediaItems.size)
    }

    @Test
    fun testCreateMediaItemSequence_withoutAudio() {
        val mediaItem = androidx.media3.common.MediaItem.fromUri("file:///test.mp4")
        val editedMediaItem = androidx.media3.transformer.EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(true)
            .build()
        val sequence = viewModel.createMediaItemSequence(editedMediaItem, shouldIncludeAudio = false)
        assertNotNull(sequence)
        assertEquals(1, sequence.editedMediaItems.size)
    }
}
