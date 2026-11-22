package compress.joshattic.us.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import compress.joshattic.us.model.OptimizerPermissionStatus

fun mediaReadPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> emptyArray()
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

fun resolveOptimizerPermissionStatus(context: Context): OptimizerPermissionStatus {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Scoped storage / legacy: MediaStore queries work without runtime grants on Q+;
        // pre-Q requires READ_EXTERNAL_STORAGE.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return OptimizerPermissionStatus.GRANTED
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return if (granted) OptimizerPermissionStatus.GRANTED else OptimizerPermissionStatus.DENIED
    }

    val video = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_VIDEO
    ) == PackageManager.PERMISSION_GRANTED
    val images = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED

    if (video && images) return OptimizerPermissionStatus.GRANTED

    val partial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        false
    }

    return when {
        partial || video || images -> OptimizerPermissionStatus.PARTIAL
        else -> OptimizerPermissionStatus.DENIED
    }
}

fun canScanMediaLibrary(status: OptimizerPermissionStatus): Boolean {
    return status == OptimizerPermissionStatus.GRANTED || status == OptimizerPermissionStatus.PARTIAL
}
