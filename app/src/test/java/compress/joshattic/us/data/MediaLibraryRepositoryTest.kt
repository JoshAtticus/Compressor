package compress.joshattic.us.data

import compress.joshattic.us.model.MediaKind
import compress.joshattic.us.model.OptimizerQualityMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MediaLibraryRepositoryTest {

    private val repo = MediaLibraryRepository(RuntimeEnvironment.getApplication())

    @Test
    fun estimateSavings_videoAppliesEncoderCushionAndOverhead() {
        val (estimated, savings) = repo.estimateSavings(
            sizeBytes = 100_000_000L,
            kind = MediaKind.VIDEO,
            videoSizeRatio = 0.4f,
            photoQualityPercent = 70
        )
        // 1e8 * 0.4 * 0.96 = 38_400_000; minus 2% + 50KB → ~37_580_800
        assertEquals(37_580_799L, estimated)
        assertEquals(62_419_201L, savings)
    }

    @Test
    fun estimateSavings_matchesDeviceHighSampleWithinTwoMegabytes() {
        // OnePlus CPH2747 Free-space High (0.88): 927.5 MB → 767.9 MB
        val original = 972560400L
        val actualCompressed = 805229031L
        val (estimated, _) = repo.estimateSavings(
            sizeBytes = original,
            kind = MediaKind.VIDEO,
            videoSizeRatio = 0.88f,
            photoQualityPercent = 92
        )
        val err = kotlin.math.abs(estimated - actualCompressed)
        assertTrue(
            "estimate $estimated should be within 2MB of actual $actualCompressed (err=$err)",
            err < 2L * 1024L * 1024L
        )
        val rawRatioEstimate = (original * 0.88).toLong()
        assertTrue(
            "engine-budget estimate should beat raw sizeRatio",
            err < kotlin.math.abs(rawRatioEstimate - actualCompressed)
        )
    }

    @Test
    fun estimateSavings_imageUsesSoftQualityCurve() {
        val (estimated, savings) = repo.estimateSavings(
            sizeBytes = 1000L,
            kind = MediaKind.IMAGE,
            videoSizeRatio = 0.4f,
            photoQualityPercent = 70
        )
        // 0.35 + 0.7*0.60 = 0.77 → 770
        assertEquals(770L, estimated)
        assertEquals(230L, savings)
    }

    @Test
    fun freeSpaceModes_reduceResFpsOnlyDownToFloor() {
        val high = repo.videoConfigForMode(OptimizerQualityMode.HIGH)
        val medium = repo.videoConfigForMode(OptimizerQualityMode.MEDIUM)
        val low = repo.videoConfigForMode(OptimizerQualityMode.LOW)
        assertTrue(high.sizeRatio > medium.sizeRatio)
        assertTrue(medium.sizeRatio > low.sizeRatio)
        assertTrue(low.sizeRatio >= 0.50f)

        // High keeps source resolution/FPS; Medium/Low may reduce, Low stops at floor.
        assertEquals(0, high.resolutionShortSide)
        assertEquals(0, high.targetFps)
        assertEquals(MediaLibraryRepository.FREE_SPACE_MEDIUM_SHORT_SIDE, medium.resolutionShortSide)
        assertEquals(MediaLibraryRepository.FREE_SPACE_MIN_SHORT_SIDE, low.resolutionShortSide)

        assertTrue(medium.resolutionShortSide >= MediaLibraryRepository.FREE_SPACE_MIN_SHORT_SIDE)
        assertTrue(low.resolutionShortSide >= MediaLibraryRepository.FREE_SPACE_MIN_SHORT_SIDE)
        assertTrue(medium.targetFps >= MediaLibraryRepository.FREE_SPACE_MIN_FPS)
        assertTrue(low.targetFps >= MediaLibraryRepository.FREE_SPACE_MIN_FPS)
    }

    @Test
    fun resolveFolder_trimsRelativePath() {
        val (key, label) = MediaLibraryRepository.resolveFolder("DCIM/Camera/", isRelative = true)
        assertEquals("DCIM/Camera", key)
        assertEquals("Camera", label)
    }
}
