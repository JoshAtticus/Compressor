package compress.joshattic.us

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.FrameDropEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.common.Effect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID

enum class QualityPreset {
    HIGH, MEDIUM, LOW, CUSTOM
}

data class CompressorUiState(
    val selectedUri: Uri? = null,
    val originalSize: Long = 0L,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalBitrate: Int = 0,
    val originalFps: Float = 30f,
    val durationMs: Long = 0L,
    
    val isCompressing: Boolean = false,
    val progress: Float = 0f,
    val compressedUri: Uri? = null,
    val compressedSize: Long = 0L,
    val currentOutputSize: Long = 0L,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    
    // Configuration
    val activePreset: QualityPreset = QualityPreset.HIGH,
    val targetSizeMb: Float = 10f,
    val useH265: Boolean = true, // Legacy, use videoCodec instead if possible, but keeping for compatibility if referenced elsewhere used as bool
    val videoCodec: String = MimeTypes.VIDEO_H265,
    val targetResolutionHeight: Int = 0, // 0 means original
    val targetFps: Int = 0, // 0 means original
    
    val totalSavedBytes: Long = 0L,
    
    val supportedCodecs: List<String> = emptyList(),
    val appInfoVersion: String = "1.3.0",
    val showBitrate: Boolean = false,
    val useMbps: Boolean = false,
    val hasShared: Boolean = false
) {
    private val minBitrate: Long
        get() {
            val h = if (targetResolutionHeight > 0) targetResolutionHeight else originalHeight
            var base = when {
                h >= 2160 -> 4_000_000L // 4K needs reliable bitrate
                h >= 1440 -> 2_500_000L
                h >= 1080 -> 1_500_000L
                h >= 720 -> 1_000_000L
                else -> 500_000L
            }
            
            // Adjust for Codec efficiency
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
            // Duration * Bitrate / 8 bits / 1024 / 1024
            val seconds = durationMs / 1000f
            val minBits = minBitrate * seconds
            val minMb = (minBits / 8f) / (1024f * 1024f)
            return minMb
        }

    val estimatedSize: String
        get() {
            val actualTarget = targetSizeMb.coerceAtLeast(minimumSizeMb)
            return String.format("%.1f MB", actualTarget)
        }
    
    val targetBitrate: Int
        get() {
             val durationSec = if (durationMs > 0) durationMs / 1000.0 else 0.0
             if (durationSec <= 0) return 2_000_000
             
             // Same logic as startCompression
             val safetyMarginMb = (targetSizeMb - 2.5f).coerceAtLeast(targetSizeMb * 0.8f)
             val targetBits = safetyMarginMb * 8 * 1024 * 1024
             val calculated = (targetBits / durationSec).toLong()
             
             // Apply min/max guardrails
             val original = if (originalBitrate > 0) originalBitrate.toLong() else Long.MAX_VALUE
             val final = calculated.coerceAtLeast(minBitrate).coerceAtMost(original)
             return final.toInt()
        }

    val formattedBitrate: String
        get() {
            if (!showBitrate) return ""
            return if (useMbps) {
                String.format("%.1f Mbps", targetBitrate / 1_000_000f)
            } else {
                "${targetBitrate / 1000} kbps"
            }
        }

    val formattedOriginalBitrate: String
        get() {
            if (!showBitrate) return ""
            if (originalBitrate <= 0) return ""
            return if (useMbps) {
                String.format("%.1f Mbps", originalBitrate / 1_000_000f)
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
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 MB"
    val mb = size / (1024.0 * 1024.0)
    return String.format("%.1f MB", mb)
}

@OptIn(UnstableApi::class)
class CompressorViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CompressorUiState())
    val uiState = _uiState.asStateFlow()
    
    private val prefs: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences("compressor_prefs", Context.MODE_PRIVATE)
    }

    init {
        val saved = prefs.getLong("total_saved_bytes", 0L)
        _uiState.update { it.copy(totalSavedBytes = saved) }
        checkSupportedCodecs()
        clearCache()
    }
    
    private fun checkSupportedCodecs() {
        val supported = mutableListOf<String>()
        supported.add(MimeTypes.VIDEO_H264) // Always supported on Android 5.0+ 

        if (hasEncoder(MimeTypes.VIDEO_H265)) {
            supported.add(MimeTypes.VIDEO_H265)
        }
        if (hasEncoder(MimeTypes.VIDEO_AV1)) {
            supported.add(MimeTypes.VIDEO_AV1)
        }
        
        _uiState.update { 
            var newCodec = it.videoCodec
            // Fallback if H265 not supported but was default
            if (newCodec == MimeTypes.VIDEO_H265 && !supported.contains(MimeTypes.VIDEO_H265)) {
                newCodec = MimeTypes.VIDEO_H264
            }
            it.copy(supportedCodecs = supported, videoCodec = newCodec, useH265 = newCodec == MimeTypes.VIDEO_H265) 
        }
    }

    private fun hasEncoder(mimeType: String): Boolean {
        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in list.codecInfos) {
                if (!info.isEncoder) continue
                if (info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }) {
                    return true
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private var compressionJob: Job? = null
    private var activeTransformer: Transformer? = null

    fun updateSelectedUri(context: Context, uri: Uri) {
        var size = 0L
        var width = 0
        var height = 0
        var bitrate = 0
        var fps = 30f
        var duration = 0L
        
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                size = it.statSize
            }
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            
            width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            bitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            
            // FPS extraction is flaky, sometimes in CAPTURE_FRAMERATE or needs calculation
            val fpsStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE) 
            fps = fpsStr?.toFloatOrNull() ?: 30f
            
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Calculate a reasonable default target (e.g., 70% of original)
        val defaultTargetMb = if (size > 0) (size / (1024.0 * 1024.0) * 0.7).toFloat() else 10f

        // Keep current saved bytes and preferences
        val currentSavedBytes = _uiState.value.totalSavedBytes
        val showBitrate = _uiState.value.showBitrate
        val useMbps = _uiState.value.useMbps
        val supportedCodecs = _uiState.value.supportedCodecs

        // Reset state
        _uiState.value = CompressorUiState(
            selectedUri = uri,
            originalSize = size,
            originalWidth = width,
            originalHeight = height,
            originalBitrate = bitrate,
            originalFps = fps,
            durationMs = duration,
            targetSizeMb = defaultTargetMb,
            targetResolutionHeight = height,
            activePreset = QualityPreset.HIGH,
            totalSavedBytes = currentSavedBytes,
            showBitrate = showBitrate,
            useMbps = useMbps,
            supportedCodecs = supportedCodecs
        )
    }
    
    fun markAsShared() {
        _uiState.update { it.copy(hasShared = true) }
    }
    
    fun applyPreset(preset: QualityPreset) {
        if (preset == QualityPreset.CUSTOM) {
             _uiState.update { it.copy(activePreset = QualityPreset.CUSTOM) }
             return
        }
        
        val current = _uiState.value
        
        when(preset) {
            QualityPreset.HIGH -> {
                // High Quality: Optimized Bitrate, Original Res, Original FPS
                 _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.HIGH,
                         targetResolutionHeight = current.originalHeight,
                         targetFps = 0,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.7).toFloat().coerceAtLeast(1f)
                     ) 
                 }
            }
            QualityPreset.MEDIUM -> {
                // Medium: 1080p, 30fps (unless original is lower)
                 _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.MEDIUM,
                         targetResolutionHeight = minOf(1080, current.originalHeight),
                         targetFps = if (current.originalFps < 30) 0 else 30,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.4).toFloat().coerceAtLeast(1f)
                     ) 
                 }
            }
            QualityPreset.LOW -> {
                // Low: 720p, 30fps (unless original is lower)
                  _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.LOW,
                         targetResolutionHeight = minOf(720, current.originalHeight),
                         targetFps = if (current.originalFps < 30) 0 else 30,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.2).toFloat().coerceAtLeast(1f)
                     ) 
                 }
            }
            else -> {}
        }
    }

    fun setTargetSize(mb: Float) {
        _uiState.update { it.copy(targetSizeMb = mb, activePreset = QualityPreset.CUSTOM) }
    }

    // Deprecated but kept for compatibility calls if any exist
    fun setUseH265(enable: Boolean) {
        val codec = if (enable) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264
        setVideoCodec(codec)
    }

    fun setVideoCodec(codec: String) {
        _uiState.update { 
            it.copy(
                videoCodec = codec, 
                useH265 = codec == MimeTypes.VIDEO_H265, 
                activePreset = QualityPreset.CUSTOM
            ) 
        }
    }
    fun toggleShowBitrate() {
        _uiState.update { it.copy(showBitrate = !it.showBitrate) }
    }

    fun toggleBitrateUnit() {
        _uiState.update { it.copy(useMbps = !it.useMbps) }
    }    
    fun setResolution(height: Int) {
        _uiState.update { it.copy(targetResolutionHeight = height, activePreset = QualityPreset.CUSTOM) }
    }

    fun setFps(fps: Int) {
        _uiState.update { it.copy(targetFps = fps, activePreset = QualityPreset.CUSTOM) }
    }
    
    fun cancelCompression() {
        activeTransformer?.cancel()
        compressionJob?.cancel()
        _uiState.update { it.copy(isCompressing = false, progress = 0f) }
    }
    
    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
    
    private fun clearCache() {
        try {
            val context = getApplication<Application>()
            val outputDir = File(context.cacheDir, "compressed_videos")
            if (outputDir.exists()) {
                outputDir.listFiles()?.forEach { 
                    try { it.delete() } catch(e: Exception) {} 
                }
            }
        } catch(e: Exception) {
             e.printStackTrace()
        }
    }

    fun reset() {
        // Keep persistent data
        val current = _uiState.value
        val savedBytes = current.totalSavedBytes
        val supportedCodecs = current.supportedCodecs
        val showBitrate = current.showBitrate
        val useMbps = current.useMbps
        
        // Clear previous temp files to free up space
        clearCache()
        
        // Reset state but keep preserved values
        _uiState.value = CompressorUiState(
            totalSavedBytes = savedBytes,
            supportedCodecs = supportedCodecs,
            showBitrate = showBitrate,
            useMbps = useMbps
        )
    }

    fun startCompression(context: Context) {
        val currentState = _uiState.value
        val inputUri = currentState.selectedUri ?: return

        _uiState.update { it.copy(isCompressing = true, progress = 0f, currentOutputSize = 0L, error = null, compressedUri = null, saveSuccess = false) }

        val outputDir = File(context.cacheDir, "compressed_videos")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "compress_${UUID.randomUUID()}.mp4")
        val outputPath = outputFile.absolutePath

        // 1. Calculate Bitrate
        val targetBitrate = currentState.targetBitrate.toLong() // Use the one calculated in UI state with guardrails

        // 2. Encoder setup
        val videoMimeType = currentState.videoCodec

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetBitrate.toInt())
                    .build()
            )
            .build()
        
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(videoMimeType)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                     val finalSize = outputFile.length()
                     val savedBytes = currentState.originalSize - finalSize
                     var newTotal = _uiState.value.totalSavedBytes
                     
                     if (savedBytes > 0) {
                         newTotal += savedBytes
                         prefs.edit().putLong("total_saved_bytes", newTotal).apply()
                     }

                     _uiState.update { 
                         it.copy(
                             isCompressing = false, 
                             progress = 1f, 
                             compressedUri = Uri.fromFile(outputFile),
                             compressedSize = finalSize,
                             totalSavedBytes = newTotal
                         ) 
                     }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    val app = getApplication<Application>()
                    _uiState.update { 
                        val isCodecError = exportException.errorCode == ExportException.ERROR_CODE_DECODER_INIT_FAILED ||
                                           exportException.errorCode == ExportException.ERROR_CODE_ENCODER_INIT_FAILED

                        val errorMsg = if (isCodecError) {
                            app.getString(R.string.error_codec_unsupported)
                        } else {
                            exportException.localizedMessage ?: app.getString(R.string.error_unknown)
                        }

                        it.copy(
                            isCompressing = false, 
                            error = errorMsg
                        ) 
                    }
                }
            })
            .build()
        
        activeTransformer = transformer
            
        // 3. Effects (Resolution & FPS)
        val effectsList = mutableListOf<Effect>()
        
        // Resolution
        if (currentState.targetResolutionHeight > 0 && currentState.targetResolutionHeight != currentState.originalHeight) {
             effectsList.add(Presentation.createForHeight(currentState.targetResolutionHeight))
        }
        
        // FPS
        if (currentState.activePreset != QualityPreset.HIGH && currentState.targetFps > 0) {
            // Only apply framerate change if not High Quality (keeps original) and target is set
             effectsList.add(FrameDropEffect.createSimpleFrameDropEffect(currentState.originalFps, currentState.targetFps.toFloat()))
        }
        
        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effectsList))
            .build()

        val composition = Composition.Builder(
            listOf(EditedMediaItemSequence(editedMediaItem))
        ).build()

        transformer.start(composition, outputPath)
        
        compressionJob = viewModelScope.launch {
            while (_uiState.value.isCompressing) {
                val progressHolder = androidx.media3.transformer.ProgressHolder()
                val state = transformer.getProgress(progressHolder)
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    val currentSize = if(outputFile.exists()) outputFile.length() else 0L
                    _uiState.update { it.copy(progress = progressHolder.progress / 100f, currentOutputSize = currentSize) }
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    fun saveToUri(context: Context, targetUri: Uri) {
        val currentState = _uiState.value
        val compressedUri = currentState.compressedUri ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(compressedUri.path!!)
                if (!file.exists()) {
                    _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_file_lost)) }
                    return@launch
                }
                
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                 _uiState.update { it.copy(saveSuccess = true) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_save_failed, e.message)) }
            }
        }
    }

    fun saveToGallery(context: Context) {
        val currentState = _uiState.value
        val compressedUri = currentState.compressedUri ?: return
        
        viewModelScope.launch {
            try {
                val file = File(compressedUri.path!!)
                if (!file.exists()) {
                    _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_file_lost)) }
                    return@launch
                }

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "Compressed_${System.currentTimeMillis()}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    // Add date metadata
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Compressor")
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val itemUri = context.contentResolver.insert(collection, values)
                
                if (itemUri != null) {
                    context.contentResolver.openOutputStream(itemUri).use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out!!)
                        }
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        context.contentResolver.update(itemUri, values, null, null)
                    }
                    
                    _uiState.update { it.copy(saveSuccess = true) }
                } else {
                     _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_gallery_entry)) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_save_failed, e.message)) }
            }
        }
    }

    fun deleteOriginal(context: Context, launcher: androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>) {
        val uri = _uiState.value.selectedUri ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                 val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(pi.intentSender).build()
                 launcher.launch(intentSenderRequest)
            } else {
                context.contentResolver.delete(uri, null, null)
                // If successful we assume it's gone. The Activity listener will actually reset logic on success, 
                // but here we can optimistically say it worked if no exception
            }
        } catch (e: SecurityException) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                 val recoverableSecurityException = e as? android.app.RecoverableSecurityException
                 if (recoverableSecurityException != null) {
                     val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(recoverableSecurityException.userAction.actionIntent.intentSender).build()
                     launcher.launch(intentSenderRequest)
                     return
                 }
             }
             _uiState.update { it.copy(error = "Cannot delete: Permission denied") }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
             _uiState.update { it.copy(error = "Cannot delete: Invalid file type (Photo Picker)") }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(error = "Delete failed: ${e.localizedMessage}") }
        }
    }
}
