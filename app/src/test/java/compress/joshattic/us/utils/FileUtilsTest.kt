package compress.joshattic.us.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FileUtilsTest {

    @Test
    fun testFormatFileSize_zero() {
        assertEquals("0 MB", formatFileSize(0L))
        assertEquals("0 MB", formatFileSize(-10L))
    }

    @Test
    fun testFormatFileSize_mb() {
        assertEquals("1.0 MB", formatFileSize(1024 * 1024L))
        assertEquals("500.0 MB", formatFileSize(500 * 1024 * 1024L))
        assertEquals("999.0 MB", formatFileSize(999 * 1024 * 1024L))
    }

    @Test
    fun testFormatFileSize_gb() {
        // The code uses 1000 MB as the threshold for GB display
        assertEquals("1.0 GB", formatFileSize(1024 * 1024 * 1024L))
        assertEquals("2.5 GB", formatFileSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }
}
