package compress.joshattic.us.utils

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import java.util.Locale

data class HardwareInfo(
    val chipset: String,
    val totalRam: String,
    val gpu: String,
    val cpuArch: String
)

data class DeviceWorkarounds(
    val isMediaTekVbrPatchActive: Boolean,
    val isPixel10HdrPatchActive: Boolean,
    val isHuaweiMuxerPatchActive: Boolean
) {
    val hasAnyActivePatch: Boolean
        get() = isMediaTekVbrPatchActive || isPixel10HdrPatchActive || isHuaweiMuxerPatchActive
}

object HardwareUtils {

    fun getHardwareInfo(context: Context): HardwareInfo {
        val chipset = getChipsetInfo()
        val ram = getTotalRam(context)
        val gpu = getGpuRenderer()
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: ""

        return HardwareInfo(
            chipset = chipset,
            totalRam = ram,
            gpu = gpu,
            cpuArch = cpuArch
        )
    }

    fun getDeviceWorkarounds(): DeviceWorkarounds {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val hardware = Build.HARDWARE.lowercase(Locale.US)
        val board = Build.BOARD.lowercase(Locale.US)
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.lowercase(Locale.US) else ""

        val isMediaTek = hardware.contains("mediatek") || board.contains("mediatek") ||
                manufacturer.contains("mediatek") || soc.contains("mediatek") ||
                soc.contains("dimensity") || hardware.matches(Regex(""".*mt\d{4}.*""")) ||
                board.matches(Regex(""".*mt\d{4}.*"""))

        val isPixel10 = manufacturer.contains("google") && model.contains("pixel 10")
        val isHuawei = manufacturer.contains("huawei")

        return DeviceWorkarounds(
            isMediaTekVbrPatchActive = isMediaTek,
            isPixel10HdrPatchActive = isPixel10,
            isHuaweiMuxerPatchActive = isHuawei
        )
    }

    private fun getChipsetInfo(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socManufacturer = Build.SOC_MANUFACTURER.trim()
            val socModel = Build.SOC_MODEL.trim()
            when {
                socManufacturer.isNotBlank() && socModel.isNotBlank() -> {
                    if (socModel.lowercase(Locale.US).contains(socManufacturer.lowercase(Locale.US))) {
                        capitalizeWords(socModel)
                    } else {
                        capitalizeWords("$socManufacturer $socModel")
                    }
                }
                socModel.isNotBlank() -> capitalizeWords(socModel)
                socManufacturer.isNotBlank() -> capitalizeWords(socManufacturer)
                else -> getLegacyChipset()
            }
        } else {
            getLegacyChipset()
        }
    }

    private fun getLegacyChipset(): String {
        val hardware = Build.HARDWARE.trim()
        val board = Build.BOARD.trim()
        return when {
            hardware.isNotBlank() && !hardware.equals("unknown", ignoreCase = true) -> capitalizeWords(hardware)
            board.isNotBlank() && !board.equals("unknown", ignoreCase = true) -> capitalizeWords(board)
            else -> "Unknown"
        }
    }

    private fun getTotalRam(context: Context): String {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val totalBytes = memInfo?.totalMem ?: 0L
            if (totalBytes <= 0L) return "Unknown"

            val gb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val roundedGb = Math.round(gb * 10.0) / 10.0
            if (roundedGb % 1.0 == 0.0) {
                "${roundedGb.toInt()} GB"
            } else {
                String.format(Locale.US, "%.1f GB", roundedGb)
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getGpuRenderer(): String {
        return try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return ""
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return ""

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            val selectedConfig = configs[0] ?: return ""

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            val eglContext = EGL14.eglCreateContext(display, selectedConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return ""

            val pbufferAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )
            val surface = EGL14.eglCreatePbufferSurface(display, selectedConfig, pbufferAttribs, 0)
            if (surface == EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroyContext(display, eglContext)
                return ""
            }

            EGL14.eglMakeCurrent(display, surface, surface, eglContext)
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: ""

            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, surface)
            EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)

            renderer.trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun capitalizeWords(str: String): String {
        return str.split(" ").joinToString(" ") { word ->
            if (word.lowercase(Locale.US) in listOf("sm", "mt", "qcom")) word.uppercase(Locale.US)
            else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }
}
