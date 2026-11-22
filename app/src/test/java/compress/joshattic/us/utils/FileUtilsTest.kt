package compress.joshattic.us.utils

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FileUtilsTest {

    private val application: Application = ApplicationProvider.getApplicationContext()

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
        assertEquals("1.00 GB", formatFileSize(1024 * 1024 * 1024L))
        assertEquals("2.50 GB", formatFileSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatFileSize_matchesDeviceCompressionSample() {
        val original = 972560400L
        val compressed = 823543576L
        assertEquals("927.5 MB", formatFileSize(original))
        assertEquals("785.4 MB", formatFileSize(compressed))
        assertEquals("142.1 MB", formatFileSize(original - compressed))
    }

    @Test
    fun testQueryFileName_fileUri() {
        val testFile = File(application.cacheDir, "my_custom_video.mp4")
        testFile.writeText("sample data")
        val uri = Uri.fromFile(testFile)

        val resolved = queryFileName(application, uri)
        assertEquals("my_custom_video.mp4", resolved)
        testFile.delete()
    }

    @Test
    fun testDeleteMediaUri_fileUri() {
        val testFile = File(application.cacheDir, "delete_me.mp4")
        testFile.writeText("to be deleted")
        assertTrue(testFile.exists())

        val uri = Uri.fromFile(testFile)
        val result = deleteMediaUri(application, uri)

        assertTrue(result is DeleteMediaResult.Success)
        assertFalse(testFile.exists())
    }

    @Test
    fun testGetVideoPermission() {
        // Under Robolectric SDK 33 (Tiramisu), it should return READ_MEDIA_VIDEO
        assertEquals(android.Manifest.permission.READ_MEDIA_VIDEO, getVideoPermission())
    }
}
