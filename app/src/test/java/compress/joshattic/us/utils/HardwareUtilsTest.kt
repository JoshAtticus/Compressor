package compress.joshattic.us.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareUtilsTest {

    @Test
    fun testCapitalizeWords_specialTokens() {
        assertEquals("SM8650", HardwareUtils.capitalizeWords("sm8650"))
        assertEquals("MT6891", HardwareUtils.capitalizeWords("mt6891"))
        assertEquals("QCOM", HardwareUtils.capitalizeWords("qcom"))
    }

    @Test
    fun testCapitalizeWords_regularWords() {
        assertEquals("Snapdragon 8 Gen 3", HardwareUtils.capitalizeWords("snapdragon 8 gen 3"))
        assertEquals("Pixel 8 Pro", HardwareUtils.capitalizeWords("pixel 8 pro"))
        assertEquals("Google Tensor G3", HardwareUtils.capitalizeWords("google tensor g3"))
    }

    @Test
    fun testCapitalizeWords_mixed() {
        assertEquals("Qualcomm SM8450 Snapdragon 8 Gen 1", HardwareUtils.capitalizeWords("qualcomm sm8450 snapdragon 8 gen 1"))
    }
}
