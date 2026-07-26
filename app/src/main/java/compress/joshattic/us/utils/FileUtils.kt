package compress.joshattic.us.utils

import java.util.Locale

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 MB"
    val mb = size / (1024.0 * 1024.0)
    if (mb >= 1000) {
        return String.format(Locale.US, "%.1f GB", mb / 1024)
    }
    return String.format(Locale.US, "%.1f MB", mb)
}
