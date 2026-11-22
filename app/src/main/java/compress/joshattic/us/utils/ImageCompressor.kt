package compress.joshattic.us.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ImageCompressResult(
    val outputFile: File,
    val outputSizeBytes: Long,
    val skippedBecauseLarger: Boolean
)

object ImageCompressor {

    /**
     * Compresses an image to JPEG. Keeps the result only when smaller than the source;
     * otherwise deletes the candidate and reports [ImageCompressResult.skippedBecauseLarger].
     */
    suspend fun compress(
        context: Context,
        sourceUri: Uri,
        qualityPercent: Int,
        outputDir: File,
        outputName: String
    ): ImageCompressResult = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        val quality = qualityPercent.coerceIn(20, 95)
        val outputFile = File(outputDir, outputName)
        if (outputFile.exists()) outputFile.delete()

        val originalSize = sourceSize(context, sourceUri)
        val bitmap = decodeBitmap(context, sourceUri)
            ?: throw IllegalStateException("Unable to decode image")

        try {
            FileOutputStream(outputFile).use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (!ok) throw IllegalStateException("JPEG encode failed")
            }
        } finally {
            bitmap.recycle()
        }

        val outputSize = outputFile.length()
        if (originalSize > 0L && outputSize >= originalSize) {
            outputFile.delete()
            return@withContext ImageCompressResult(
                outputFile = outputFile,
                outputSizeBytes = originalSize,
                skippedBecauseLarger = true
            )
        }

        ImageCompressResult(
            outputFile = outputFile,
            outputSizeBytes = outputSize,
            skippedBecauseLarger = false
        )
    }

    private fun sourceSize(context: Context, uri: Uri): Long {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            return it.statSize
        }
        return 0L
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val maxSide = 4096
        var sample = 1
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxDim / sample > maxSide) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val orientation = readExifOrientation(context, uri)
        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
        ) {
            return decoded
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_UNDEFINED
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }
}
