package compress.joshattic.us.utils

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.Locale

sealed class DeleteMediaResult {
    data object Success : DeleteMediaResult()
    data class RequiresUserConsent(val intentSender: IntentSender) : DeleteMediaResult()
    data class Failed(val error: String? = null) : DeleteMediaResult()
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 MB"
    val mb = size / (1024.0 * 1024.0)
    if (mb >= 1000) {
        return String.format(Locale.US, "%.1f GB", mb / 1024)
    }
    return String.format(Locale.US, "%.1f MB", mb)
}

fun getVideoPermission(): String? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_VIDEO
    } else {
        null
    }
}

fun hasVideoPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }
    val hasFull = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_MEDIA_VIDEO
    ) == PackageManager.PERMISSION_GRANTED

    val hasPartial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
    } else false

    return hasFull || hasPartial
}

/**
 * Resolves the underlying MediaStore item (Uri and original DISPLAY_NAME) for a given Uri.
 * This works for direct MediaStore URIs (from share sheet / ACTION_SEND) and also resolves
 * Photo Picker URIs (PickVisualMedia) by querying MediaStore with the video's size.
 */
fun resolveOriginalMediaStore(context: Context, pickerUri: Uri): Pair<Uri, String?>? {
    if (pickerUri.scheme != "content") return null

    val uriString = pickerUri.toString()
    if (uriString.startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString()) ||
        uriString.contains("/video/media/")
    ) {
        val name = queryFileName(context, pickerUri)
        return Pair(pickerUri, name)
    }

    if (!hasVideoPermission(context)) {
        return null
    }

    try {
        var size = 0L
        context.contentResolver.query(
            pickerUri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx != -1 && !cursor.isNull(idx)) {
                    size = cursor.getLong(idx)
                }
            }
        }
        if (size <= 0L) {
            context.contentResolver.openFileDescriptor(pickerUri, "r")?.use {
                size = it.statSize
            }
        }

        if (size > 0L) {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE
            )
            val selection = "${MediaStore.Video.Media.SIZE} = ?"
            val selectionArgs = arrayOf(size.toString())

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    if (idCol != -1) {
                        val id = cursor.getLong(idCol)
                        val name = if (nameCol != -1 && !cursor.isNull(nameCol)) cursor.getString(nameCol) else null
                        val realUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        return Pair(realUri, name)
                    }
                }
            }
        }
    } catch (_: Exception) {}

    val lastSegmentId = pickerUri.lastPathSegment?.toLongOrNull()
    if (lastSegmentId != null && lastSegmentId > 0) {
        try {
            val candidateUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, lastSegmentId)
            context.contentResolver.query(
                candidateUri,
                arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val name = if (nameCol != -1 && !cursor.isNull(nameCol)) cursor.getString(nameCol) else null
                    return Pair(candidateUri, name)
                }
            }
        } catch (_: Exception) {}
    }

    return null
}

/**
 * Robustly queries the filename of a video [Uri] across SAF, MediaStore, and file paths.
 * If the display name is missing an extension, attempts to infer one from the ContentResolver type.
 */
fun queryFileName(context: Context, uri: Uri): String? {
    var name: String? = null

    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && !cursor.isNull(idx)) {
                        name = cursor.getString(idx)
                    }
                }
            }
        } catch (_: Exception) {}

        if (name.isNullOrBlank()) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (idx != -1 && !cursor.isNull(idx)) {
                            name = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (name.isNullOrBlank()) {
            try {
                name = DocumentFile.fromSingleUri(context, uri)?.name
            } catch (_: Exception) {}
        }
    } else if (uri.scheme == "file") {
        name = uri.path?.let { File(it).name } ?: uri.lastPathSegment
    }

    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment
    }

    // Strip any directory segments if path separators were included
    name = name?.substringAfterLast('/')?.substringAfterLast('\\')

    // If name exists but lacks an extension, attempt to append one from MIME type
    if (!name.isNullOrBlank() && !name.contains(".")) {
        try {
            val mime = context.contentResolver.getType(uri)
            if (!mime.isNullOrBlank()) {
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                if (!ext.isNullOrBlank()) {
                    name = "$name.$ext"
                }
            }
        } catch (_: Exception) {}
    }

    return name?.takeIf { it.isNotBlank() }
}

/**
 * Attempts to delete a media file using the least invasive permissions possible:
 * 1. SAF document URIs (from OpenDocument) are deleted directly via DocumentsContract.deleteDocument.
 * 2. File URIs are deleted directly via java.io.File.
 * 3. MediaStore URIs on Android 11+ (API 30+) use MediaStore.createDeleteRequest, returning an IntentSender for the user to confirm without requiring broad storage permissions.
 * 4. MediaStore URIs on Android 10 (API 29) catch RecoverableSecurityException and return the intentSender.
 * 5. On older Android versions, falls back to direct ContentResolver.delete.
 */
fun deleteMediaUri(context: Context, uri: Uri): DeleteMediaResult {
    // 1. File scheme
    if (uri.scheme == "file") {
        val path = uri.path ?: return DeleteMediaResult.Failed("Invalid file path")
        val file = File(path)
        return if (file.delete()) DeleteMediaResult.Success else DeleteMediaResult.Failed("Could not delete file")
    }

    // 2. Storage Access Framework document URI
    if (DocumentsContract.isDocumentUri(context, uri)) {
        return try {
            val deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            if (deleted) {
                DeleteMediaResult.Success
            } else {
                val df = DocumentFile.fromSingleUri(context, uri)
                if (df?.delete() == true) {
                    DeleteMediaResult.Success
                } else {
                    DeleteMediaResult.Failed("Document provider declined deletion")
                }
            }
        } catch (e: Exception) {
            DeleteMediaResult.Failed(e.localizedMessage)
        }
    }

    // 3. Android 11+ (API 30+) Scoped MediaStore delete request
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            return DeleteMediaResult.RequiresUserConsent(pendingIntent.intentSender)
        } catch (_: IllegalArgumentException) {
            // Not a MediaStore URI; fall through to standard delete
        } catch (e: Exception) {
            // Fall through
        }
    }

    // 4. ContentResolver.delete with RecoverableSecurityException handling (API 29+)
    return try {
        val rows = context.contentResolver.delete(uri, null, null)
        if (rows > 0) {
            DeleteMediaResult.Success
        } else {
            DeleteMediaResult.Failed("File could not be removed")
        }
    } catch (securityException: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
            DeleteMediaResult.RequiresUserConsent(securityException.userAction.actionIntent.intentSender)
        } else {
            DeleteMediaResult.Failed(securityException.localizedMessage)
        }
    } catch (e: Exception) {
        DeleteMediaResult.Failed(e.localizedMessage)
    }
}

