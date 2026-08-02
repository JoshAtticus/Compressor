package compress.joshattic.us.model

import android.net.Uri
import androidx.media3.common.MimeTypes
import compress.joshattic.us.BuildConfig
import compress.joshattic.us.utils.formatFileSize
import java.util.Locale

sealed class FilenameSegment {
    data class Text(val value: String) : FilenameSegment()
    data class Token(val key: String) : FilenameSegment()
}

val defaultTargetSizePresets = listOf(
    TargetSizePreset("discord", 10f, "Discord • GitHub"),
    TargetSizePreset("email", 25f, "Email"),
    TargetSizePreset("stories", 50f, "Stories • Nitro Basic"),
    TargetSizePreset("messenger", 100f, "Messenger • BlueSky"),
    TargetSizePreset("nitro", 500f, "Nitro • Reels"),
    TargetSizePreset("twitter", 512f, "Twitter/X"),
    TargetSizePreset("whatsapp", 2048f, "WhatsApp • Telegram"),
    TargetSizePreset("tg_premium", 4096f, "TG Premium • Feed"),
    TargetSizePreset("x_premium", 8192f, "X Premium")
)

data class CompressorUiState(
    val selectedUri: Uri? = null,
    val originalSize: Long = 0L,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalBitrate: Int = 0,
    val originalAudioBitrate: Int = 0,
    val originalFps: Float = 30f,
    val originalVideoMime: String? = null,
    val durationMs: Long = 0L,
    val originalName: String? = null,
    
    val isCompressing: Boolean = false,
    val progress: Float = 0f,
    val compressedUri: Uri? = null,
    val compressedSize: Long = 0L,
    val currentOutputSize: Long = 0L,
    val error: String? = null,
    val errorLog: String? = null,
    val saveSuccess: Boolean = false,
    val isSaving: Boolean = false,
    
    // Configuration
    val activePreset: QualityPreset = QualityPreset.CUSTOM,
    val targetSizeMb: Float = 10f,
    val useH265: Boolean = true,
    val videoCodec: String = MimeTypes.VIDEO_H265,
    val targetResolutionHeight: Int = 0, // 0 means original
    val targetFps: Int = 0, // 0 means original
    
    val totalSavedBytes: Long = 0L,
    
    val supportedCodecs: List<String> = emptyList(),
    val appInfoVersion: String = BuildConfig.VERSION_NAME,
    val showWhatsNewDialog: Boolean = false,
    val showBitrate: Boolean = false,
    val useMbps: Boolean = false,
    val showStorageSaved: Boolean = true,
    val showTargetSizePreset: Boolean = true,
    /** Only meaningful on Android 10+; forced off on older versions. */
    val autoSaveToPhotos: Boolean = false,
    val customOutputTreeUri: String? = null,
    val customOutputFolderName: String? = null,
    val hasShared: Boolean = false,
    val removeAudio: Boolean = false,
    val audioBitrate: Int = 128_000,
    val audioVolume: Float = 1.0f,
    val warnings: List<String> = emptyList(),
    val allCodecsEnabled: Boolean = false,
    val allCodecsUnlocked: Boolean = false,
    val highPresetConfig: QualityPresetConfig = QualityPresetConfig(resolutionShortSide = 0, targetFps = 0, sizeRatio = 0.7f, audioBitrate = 320_000),
    val mediumPresetConfig: QualityPresetConfig = QualityPresetConfig(resolutionShortSide = 1080, targetFps = 30, sizeRatio = 0.4f, audioBitrate = 192_000),
    val lowPresetConfig: QualityPresetConfig = QualityPresetConfig(resolutionShortSide = 720, targetFps = 30, sizeRatio = 0.2f, audioBitrate = 128_000),
    val targetSizePresets: List<TargetSizePreset> = defaultTargetSizePresets,
    val defaultVideoConfig: DefaultVideoConfig = DefaultVideoConfig(),
    val defaultAudioConfig: DefaultAudioConfig = DefaultAudioConfig(),
    val filenameSegments: List<FilenameSegment> = listOf(
        FilenameSegment.Text(""),
        FilenameSegment.Token("original_name"),
        FilenameSegment.Text(""),
        FilenameSegment.Token("compressed"),
        FilenameSegment.Text("")
    )
) {
    private val minBitrate: Long
        get() {
            val h = if (targetResolutionHeight > 0) targetResolutionHeight else originalHeight
            var base = when {
                h >= 2160 -> 4_000_000L
                h >= 1440 -> 2_500_000L
                h >= 1080 -> 1_500_000L
                h >= 720 -> 1_000_000L
                h >= 480 -> 500_000L
                h >= 360 -> 350_000L
                else -> 200_000L
            }
            
            if (videoCodec == MimeTypes.VIDEO_H265) {
                base = (base * 0.7).toLong()
            } else if (videoCodec == MimeTypes.VIDEO_AV1) {
                base = (base * 0.6).toLong()
            }
            
            val fpsVal = if (targetFps > 0) targetFps.toFloat() else originalFps
            val multiplier = if (fpsVal > 45) 1.5f else 1.0f
            return (base * multiplier).toLong()
        }

    val minimumSizeMb: Float
        get() {
            if (durationMs <= 0) return 0.1f
            val seconds = durationMs / 1000f
            val audioBits = if (removeAudio) 0f else {
                val rate = if (audioBitrate == 0) 256_000f else audioBitrate.toFloat()
                rate * seconds
            }
            val minBits = minBitrate * seconds
            val totalBits = minBits + audioBits
            val minMb = (totalBits / 8f) / (1024f * 1024f)
            return minMb
        }

    val estimatedSize: String
        get() {
            val actualTarget = targetSizeMb.coerceAtLeast(minimumSizeMb)
            return String.format(Locale.US, "%.1f MB", actualTarget)
        }
    
    val targetBitrate: Int
        get() {
             val durationSec = if (durationMs > 0) durationMs / 1000.0 else 0.0
             if (durationSec <= 0) return 2_000_000
             
             val targetBits = targetSizeMb * 8 * 1024 * 1024
             
             val audioBits = if (removeAudio) 0.0 else {
                 val rate = if (audioBitrate == 0) 256_000.0 else audioBitrate.toDouble()
                 rate * durationSec
             }
             
             val overheadBits = (targetBits * 0.02) + (50 * 1024 * 8)
             
             var availableVideoBits = targetBits - audioBits - overheadBits
             
             availableVideoBits = availableVideoBits.coerceAtLeast(targetBits * 0.1) 
             
             val calculated = (availableVideoBits / durationSec).toLong()
             
             val original = if (originalBitrate > 0) originalBitrate.toLong() else Long.MAX_VALUE
             val final = calculated.coerceAtLeast(minBitrate).coerceAtMost(original)
             return final.toInt()
        }

    val formattedBitrate: String
        get() {
            if (!showBitrate) return ""
            return if (useMbps) {
                String.format(Locale.US, "%.1f Mbps", targetBitrate / 1_000_000f)
            } else {
                "${targetBitrate / 1000} kbps"
            }
        }

    val formattedOriginalBitrate: String
        get() {
            if (!showBitrate) return ""
            if (originalBitrate <= 0) return ""
            return if (useMbps) {
                String.format(Locale.US, "%.1f Mbps", originalBitrate / 1_000_000f)
            } else {
                "${originalBitrate / 1000} kbps"
            }
        }
        
    val formattedTotalSaved: String
        get() = formatFileSize(totalSavedBytes)

    val formattedOriginalSize: String
        get() = formatFileSize(originalSize)
        
    val formattedCompressedSize: String
        get() = formatFileSize(compressedSize)
        
    val formattedCurrentOutputSize: String
        get() = formatFileSize(currentOutputSize)

    fun autoAdjust(targetMb: Float, lockAudioBitrate: Boolean = false, allowUpward: Boolean = true): CompressorUiState {
        var state = this
        var attempts = 0
        val maxAttempts = 20

        // Downward adjustment (Reduce quality to fit target)
        while (state.minimumSizeMb > targetMb && attempts < maxAttempts) {
            attempts++
            
            if (!lockAudioBitrate) {
                // 1. Reduce Audio Bitrate to 128k
                if (state.audioBitrate > 128_000) {
                    state = state.copy(audioBitrate = 128_000)
                    continue
                }
            }

            // 2. Reduce Resolution
            val currentH = if (state.targetResolutionHeight > 0) state.targetResolutionHeight else state.originalHeight
            val newH = when {
                currentH > 2160 -> 2160
                currentH > 1440 -> 1440
                currentH > 1080 -> 1080
                currentH > 720 -> 720
                currentH > 480 -> 480
                currentH > 360 -> 360
                else -> 240
            }
            
            if (newH < currentH) {
                state = state.copy(targetResolutionHeight = newH)
                continue
            }

            if (!lockAudioBitrate) {
                // 3. Reduce Audio Bitrate to 96k if really needed (big gap)
                if (state.audioBitrate > 96_000 && state.minimumSizeMb > targetMb * 1.5) {
                    state = state.copy(audioBitrate = 96_000)
                    continue
                }
            }

            val effectiveFps = if (state.targetFps > 0) state.targetFps else state.originalFps.toInt()

            // 4. Reduce FPS to 30 if higher
            if (effectiveFps > 30) {
                state = state.copy(targetFps = 30)
                continue
            }
            
            // 5. Reduce FPS to 24
            if (effectiveFps > 24) { 
                 state = state.copy(targetFps = 24)
                 continue
            }
             
             break 
        }

        // Upward adjustment (Increase quality if we have headroom)
        if (allowUpward) {
            attempts = 0
            while (attempts < maxAttempts) {
                attempts++
                var changed = false
            
            // 1. Try to increase Resolution 
            val currentH = if (state.targetResolutionHeight > 0) state.targetResolutionHeight else state.originalHeight
            if (currentH < state.originalHeight) {
                val nextH = when {
                    currentH < 360 -> 360
                    currentH < 480 -> 480
                    currentH < 720 -> 720
                    currentH < 1080 -> 1080
                    currentH < 1440 -> 1440
                    currentH < 2160 -> 2160
                    else -> state.originalHeight
                }.coerceAtMost(state.originalHeight)
                
                val useOriginal = nextH >= state.originalHeight
                val testState = state.copy(targetResolutionHeight = if (useOriginal) 0 else nextH) 
                
                if (testState.minimumSizeMb <= targetMb) {
                    state = testState
                    changed = true
                    continue
                }
            }
            
            // 2. Try to increase FPS
            val currentFps = if (state.targetFps > 0) state.targetFps else state.originalFps.toInt()
            if (currentFps < state.originalFps.toInt()) {
                 val nextFps = if (currentFps < 30) 30 else state.originalFps.toInt()
                 val useOriginal = nextFps >= state.originalFps.toInt()
                 val testState = state.copy(targetFps = if (useOriginal) 0 else nextFps)
                 
                 if (testState.minimumSizeMb <= targetMb) {
                    state = testState
                    changed = true
                    continue
                 }
            }
            
             if (!lockAudioBitrate) {
                 // 3. Try to increase Audio Bitrate
                 val maxAudio = if (state.originalAudioBitrate > 0) state.originalAudioBitrate else 320_000
                 if (state.audioBitrate < maxAudio) {
                     val nextAudio = when {
                         state.audioBitrate < 96_000 -> 96_000
                         state.audioBitrate < 128_000 -> 128_000
                         state.audioBitrate < 192_000 -> 192_000
                         state.audioBitrate < 320_000 -> 320_000
                         else -> maxAudio
                     }.coerceAtMost(maxAudio)
                     
                     val testState = state.copy(audioBitrate = nextAudio)
                     if (testState.minimumSizeMb <= targetMb) {
                         state = testState
                         changed = true
                         continue
                     }
                 }
             }

            if (!changed) break
            }
        }

        return state
    }
}
