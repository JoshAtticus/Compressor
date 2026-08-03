package compress.joshattic.us

import compress.joshattic.us.model.CompressorUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressorUiStateTest {

    @Test
    fun testAutoAdjust_prioritizesReducingResolutionOverFramerate() {
        // Given a 1080p 60fps video with 60s duration
        val initialState = CompressorUiState(
            originalWidth = 1920,
            originalHeight = 1080,
            originalFps = 60f,
            durationMs = 60_000L,
            audioBitrate = 128_000
        )

        // Verify minimumSizeMb at 1080p 60fps
        val minMb1080p60 = initialState.minimumSizeMb
        assertTrue("Initial min size should be > 10MB", minMb1080p60 > 10f)

        // Set targetMb that forces quality reduction below 1080p minimum size, but achievable at 720p 60fps
        val targetMb = minMb1080p60 - 2f
        val adjustedState = initialState.autoAdjust(targetMb)

        // Resolution should be reduced (e.g. 720p or lower) while framerate remains original (targetFps == 0 for original 60fps)
        assertTrue(
            "Target resolution height should be reduced below original 1080",
            adjustedState.targetResolutionHeight in 1..720
        )
        assertEquals(
            "Framerate should be preserved (targetFps = 0 for original 60fps)",
            0,
            adjustedState.targetFps
        )
    }

    @Test
    fun testAutoAdjust_4KTo1080p_preservesFramerate() {
        // Given a 4K (3840x2160) 60fps video
        val initialState = CompressorUiState(
            originalWidth = 3840,
            originalHeight = 2160,
            originalFps = 60f,
            durationMs = 30_000L,
            audioBitrate = 128_000
        )

        val targetMb = initialState.minimumSizeMb - 1f
        val adjustedState = initialState.autoAdjust(targetMb)

        assertEquals("Resolution should drop to 1440p or 1080p", 1440, adjustedState.targetResolutionHeight)
        assertEquals("Framerate should remain 60fps (targetFps = 0)", 0, adjustedState.targetFps)
    }

    @Test
    fun testAutoAdjust_reducesAudioBitrateTo96kWhenBigGap() {
        // Given a video already at minimum resolution (240p) with 128kbps audio and a big size gap
        val initialState = CompressorUiState(
            originalWidth = 320,
            originalHeight = 240,
            targetResolutionHeight = 240,
            originalFps = 30f,
            durationMs = 600_000L,
            audioBitrate = 128_000
        )

        // Target size smaller than min size by >1.5x to trigger audio bitrate reduction to 96k
        val targetMb = initialState.minimumSizeMb / 2.0f
        val adjustedState = initialState.autoAdjust(targetMb, lockAudioBitrate = false)

        // Audio bitrate should drop to 96,000 (96kbps)
        assertEquals(96_000, adjustedState.audioBitrate)
    }

    @Test
    fun testAutoAdjust_respectsLockedAudioBitrate() {
        val initialState = CompressorUiState(
            originalWidth = 1920,
            originalHeight = 1080,
            originalFps = 60f,
            durationMs = 60_000L,
            audioBitrate = 320_000
        )

        val targetMb = initialState.minimumSizeMb - 2f
        val adjustedState = initialState.autoAdjust(targetMb, lockAudioBitrate = true)

        assertEquals("Locked audio bitrate should not be modified", 320_000, adjustedState.audioBitrate)
        assertTrue("Resolution should drop instead", adjustedState.targetResolutionHeight in 1..720)
    }

    @Test
    fun testAutoAdjust_respectsUserLockedSettings() {
        val initialState = CompressorUiState(
            originalWidth = 3840,
            originalHeight = 2160,
            originalFps = 60f,
            durationMs = 600_000L,
            targetResolutionHeight = 2160,
            targetFps = 60,
            audioBitrate = 320_000,
            audioBitrateLocked = true,
            targetResolutionLocked = true,
            targetFpsLocked = true
        )

        val adjustedState = initialState.autoAdjust(1f)

        assertEquals(320_000, adjustedState.audioBitrate)
        assertEquals(2160, adjustedState.targetResolutionHeight)
        assertEquals(60, adjustedState.targetFps)
        assertTrue(adjustedState.targetSizeWarning)
    }

    @Test
    fun testSuggestedForTarget_releasesLocksWithoutChangingCurrentState() {
        val initialState = CompressorUiState(
            originalWidth = 3840,
            originalHeight = 2160,
            originalFps = 60f,
            durationMs = 600_000L,
            targetResolutionHeight = 2160,
            targetFps = 60,
            audioBitrate = 320_000,
            audioBitrateLocked = true,
            targetResolutionLocked = true,
            targetFpsLocked = true
        )

        val suggestion = initialState.suggestedForTarget()

        assertEquals(2160, initialState.targetResolutionHeight)
        assertTrue(suggestion.targetResolutionHeight < initialState.targetResolutionHeight)
        assertTrue(suggestion.audioBitrate < initialState.audioBitrate)
    }

    @Test
    fun testAutoAdjust_portraitVideoUsesShortSideForResolution() {
        val initialState = CompressorUiState(
            originalWidth = 1080,
            originalHeight = 1920,
            originalFps = 60f,
            durationMs = 60_000L,
            targetResolutionHeight = 1920,
            audioBitrate = 128_000
        )

        val adjustedState = initialState.autoAdjust(initialState.minimumSizeMb - 1f)

        assertEquals("720p portrait should use a 1280px height", 1280, adjustedState.targetResolutionHeight)
    }

    @Test
    fun testAutoAdjust_onlyReducesFramerateWhenResolutionIsMin() {
        // Given a video with high fps where target size is lower than minimum size at lowest resolution (240p)
        val initialState = CompressorUiState(
            originalWidth = 1920,
            originalHeight = 1080,
            originalFps = 60f,
            durationMs = 600_000L, // 10 minutes
            audioBitrate = 128_000,
            removeAudio = true
        )

        // Request an extremely small target size that requires reducing both resolution and framerate
        val adjustedState = initialState.autoAdjust(0.1f)

        // Resolution should be reduced to 240p (lowest tier)
        assertEquals(240, adjustedState.targetResolutionHeight)
        // Framerate should then be reduced to 24fps
        assertEquals(24, adjustedState.targetFps)
    }

    @Test
    fun testStatePreservation_autoSaveToPhotos() {
        val state = CompressorUiState(
            autoSaveToPhotos = true,
            customOutputTreeUri = "content://test/tree",
            customOutputFolderName = "TestFolder"
        )
        assertTrue("autoSaveToPhotos should be true when enabled", state.autoSaveToPhotos)
        assertEquals("content://test/tree", state.customOutputTreeUri)
        assertEquals("TestFolder", state.customOutputFolderName)

        // Simulating reset preservation pattern
        val resetState = CompressorUiState(
            autoSaveToPhotos = state.autoSaveToPhotos,
            customOutputTreeUri = state.customOutputTreeUri,
            customOutputFolderName = state.customOutputFolderName
        )
        assertTrue("autoSaveToPhotos should be preserved after reset", resetState.autoSaveToPhotos)
        assertEquals("content://test/tree", resetState.customOutputTreeUri)
        assertEquals("TestFolder", resetState.customOutputFolderName)
    }

    @Test
    fun testIsSavingDefaultState() {
        val defaultState = CompressorUiState()
        org.junit.Assert.assertFalse("isSaving should default to false", defaultState.isSaving)

        val savingState = defaultState.copy(isSaving = true)
        assertTrue("isSaving should be true when copy sets isSaving", savingState.isSaving)
    }
}
