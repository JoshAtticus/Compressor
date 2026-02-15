package compress.joshattic.us

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodecList
import android.media.MediaExtractor
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
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.transformer.AudioEncoderSettings
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

// All sorted so nicely :D
data class CompressorUiState(
    val selectedUri: Uri? = null,
    val originalSize: Long = 0L,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val originalBitrate: Int = 0,
    val originalAudioBitrate: Int = 0,
    val originalFps: Float = 30f,
    val durationMs: Long = 0L,
    val originalDate: Long? = null,
    val originalLocation: String? = null,
    
    val isCompressing: Boolean = false,
    val progress: Float = 0f,
    val compressedUri: Uri? = null,
    val compressedSize: Long = 0L,
    val currentOutputSize: Long = 0L,
    val error: String? = null,
    val errorLog: String? = null,
    val saveSuccess: Boolean = false,
    
    // Configuration
    val activePreset: QualityPreset = QualityPreset.HIGH,
    val targetSizeMb: Float = 10f,
    val useH265: Boolean = true,
    val videoCodec: String = MimeTypes.VIDEO_H265,
    val targetResolutionHeight: Int = 0, // 0 means original
    val targetFps: Int = 0, // 0 means original
    
    val totalSavedBytes: Long = 0L,
    
    val supportedCodecs: List<String> = emptyList(),
    val appInfoVersion: String = "1.5.1,
    val showBitrate: Boolean = false,
    val useMbps: Boolean = false,
    val hasShared: Boolean = false,
    val removeAudio: Boolean = false,
    val audioBitrate: Int = 128_000,
    val audioVolume: Float = 1.0f
) {
    private val minBitrate: Long
        get() {
            val h = if (targetResolutionHeight > 0) targetResolutionHeight else originalHeight
            var base = when {
                h >= 2160 -> 4_000_000L
                h >= 1440 -> 2_500_000L
                h >= 1080 -> 1_500_000L
                h >= 720 -> 1_000_000L
                else -> 500_000L
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
            return String.format("%.1f MB", actualTarget)
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
    if (mb >= 1000) {
        return String.format("%.1f GB", mb / 1024)
    }
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
        val showBitrate = prefs.getBoolean("show_bitrate", false)
        val useMbps = prefs.getBoolean("use_mbps", false)
        _uiState.update { it.copy(totalSavedBytes = saved, showBitrate = showBitrate, useMbps = useMbps) }
        checkSupportedCodecs()
        clearCache()
    }
    
    private fun checkSupportedCodecs() {
        val supported = mutableListOf<String>()
        supported.add(MimeTypes.VIDEO_H264) // I mean this is supported on like everything ever, if not then skill issue ig?

        if (hasEncoder(MimeTypes.VIDEO_H265)) {
            supported.add(MimeTypes.VIDEO_H265)
        }
        if (hasEncoder(MimeTypes.VIDEO_AV1)) {
            supported.add(MimeTypes.VIDEO_AV1)
        }
        
        _uiState.update { 
            var newCodec = it.videoCodec
            // Fallback if H265 not supported but was default because I can't be assed to properly fix it
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (info.isSoftwareOnly) {
                        continue
                    }
                } else {
                    val name = info.name.lowercase()
                    if (name.startsWith("c2.android")) {
                        continue
                    }
                }

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
        var audioBitrate = 0
        var fps = 30f
        var duration = 0L
        var originalDate: Long? = null
        var originalLocation: String? = null
        
        try {
            audioBitrate = getAudioBitrate(context, uri)
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                size = it.statSize
            }
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            
            width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            
            val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }
            
            bitrate = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            
            // FPS extraction is flaky, sometimes in CAPTURE_FRAMERATE or needs calculation
            val fpsStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE) 
            fps = fpsStr?.toFloatOrNull() ?: 30f
            
            val dateStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DATE)
            if (dateStr != null) {
                val formats = listOf(
                    "yyyyMMdd'T'HHmmss.SSS'Z'",
                    "yyyyMMdd'T'HHmmss'Z'",
                    "yyyy-MM-dd HH:mm:ss"
                )
                for (format in formats) {
                    try {
                        val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        originalDate = sdf.parse(dateStr)?.time
                        if (originalDate != null) break
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            originalLocation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION)

            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val defaultTargetMb = if (size > 0) (size / (1024.0 * 1024.0) * 0.7).toFloat() else 10f

        val currentSavedBytes = _uiState.value.totalSavedBytes
        val showBitrate = _uiState.value.showBitrate
        val useMbps = _uiState.value.useMbps
        val supportedCodecs = _uiState.value.supportedCodecs

        _uiState.value = CompressorUiState(
            selectedUri = uri,
            originalSize = size,
            originalWidth = width,
            originalHeight = height,
            originalBitrate = bitrate,
            originalAudioBitrate = audioBitrate,
            originalFps = fps,
            durationMs = duration,
            originalDate = originalDate,
            originalLocation = originalLocation,
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
        val isVertical = current.originalHeight > current.originalWidth
        
        fun getTargetHeight(targetShortSide: Int): Int {
            if (current.originalWidth <= 0 || current.originalHeight <= 0) return current.originalHeight
            
            if (isVertical) {
                val targetWidth = minOf(targetShortSide, current.originalWidth)
                return (targetWidth.toDouble() * current.originalHeight / current.originalWidth).toInt()
            } else {
                return minOf(targetShortSide, current.originalHeight)
            }
        }
        
        when(preset) {
            QualityPreset.HIGH -> {
                 _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.HIGH,
                         targetResolutionHeight = current.originalHeight,
                         targetFps = 0,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.7).toFloat().coerceAtLeast(1f),
                         audioBitrate = 320_000,
                         removeAudio = false
                     )
                 }
            }
            QualityPreset.MEDIUM -> {
                 _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.MEDIUM,
                         targetResolutionHeight = getTargetHeight(1080),
                         targetFps = if (current.originalFps < 30) 0 else 30,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.4).toFloat().coerceAtLeast(1f),
                         audioBitrate = 192_000,
                         removeAudio = false
                     )
                 }
            }
            QualityPreset.LOW -> {
                  _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.LOW,
                         targetResolutionHeight = getTargetHeight(720),
                         targetFps = if (current.originalFps < 30) 0 else 30,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.2).toFloat().coerceAtLeast(1f),
                         audioBitrate = 128_000,
                         removeAudio = false
                     )
                 }
            }
            else -> {}
        }
    }

    fun setTargetSize(mb: Float) {
        _uiState.update { it.copy(targetSizeMb = mb, activePreset = QualityPreset.CUSTOM) }
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
        _uiState.update { 
            val newValue = !it.showBitrate
            prefs.edit().putBoolean("show_bitrate", newValue).apply()
            it.copy(showBitrate = newValue)
        }
    }

    fun toggleBitrateUnit() {
        _uiState.update { 
            val newValue = !it.useMbps
            prefs.edit().putBoolean("use_mbps", newValue).apply()
            it.copy(useMbps = newValue)
        }
    }

    fun toggleRemoveAudio() {
        _uiState.update { it.copy(removeAudio = !it.removeAudio, activePreset = QualityPreset.CUSTOM) }
    }

    fun setAudioBitrate(bitrate: Int) {
        _uiState.update { it.copy(audioBitrate = bitrate, activePreset = QualityPreset.CUSTOM) }
    }

    fun setAudioVolume(volume: Float) {
        _uiState.update { it.copy(audioVolume = volume, activePreset = QualityPreset.CUSTOM) }
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
        val current = _uiState.value
        val savedBytes = current.totalSavedBytes
        val supportedCodecs = current.supportedCodecs
        val showBitrate = current.showBitrate
        val useMbps = current.useMbps
        
        // Clear previous temp files otherwise it indefinitely duplicates compressed videos in cache
        clearCache()

        val defaultCodec = if (supportedCodecs.contains(MimeTypes.VIDEO_H265)) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264
        val useH265 = defaultCodec == MimeTypes.VIDEO_H265
        
        _uiState.value = CompressorUiState(
            totalSavedBytes = savedBytes,
            supportedCodecs = supportedCodecs,
            showBitrate = showBitrate,
            useMbps = useMbps,
            videoCodec = defaultCodec,
            useH265 = useH265
        )
    }

    fun startCompression(context: Context) {
        val currentState = _uiState.value
        val inputUri = currentState.selectedUri ?: return

        _uiState.update { it.copy(isCompressing = true, progress = 0f, currentOutputSize = 0L, error = null, errorLog = null, compressedUri = null, saveSuccess = false) }

        val outputDir = File(context.cacheDir, "compressed_videos")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "compress_${UUID.randomUUID()}.mp4")
        val outputPath = outputFile.absolutePath

        val targetBitrate = currentState.targetBitrate.toLong()

        val audioBitrateToUse = if (currentState.audioBitrate == 0) {
            val original = getAudioBitrate(context, inputUri)
            if (original > 0) original else 256_000
        } else {
             currentState.audioBitrate
        }

        val videoMimeType = currentState.videoCodec

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetBitrate.toInt())
                    .build()
            )
            .setRequestedAudioEncoderSettings(
                AudioEncoderSettings.Builder()
                    .setBitrate(audioBitrateToUse)
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
                        val isMuxerError = exportException.errorCode == ExportException.ERROR_CODE_MUXING_FAILED
                        val isHuawei = android.os.Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)

                        val errorMsg = when {
                            isMuxerError && isHuawei -> app.getString(R.string.error_huawei_muxer)
                            isCodecError -> app.getString(R.string.error_codec_unsupported)
                            else -> exportException.localizedMessage ?: app.getString(R.string.error_unknown)
                        }

                        it.copy(
                            isCompressing = false, 
                            error = errorMsg,
                            errorLog = exportException.stackTraceToString()
                        ) 
                    }
                }
            })
            .build()
        
        activeTransformer = transformer
            
        val effectsList = mutableListOf<Effect>()
        
        if (currentState.targetResolutionHeight > 0 && currentState.targetResolutionHeight != currentState.originalHeight) {
             val aspectRatio = if (currentState.originalHeight > 0) currentState.originalWidth.toFloat() / currentState.originalHeight else 16f/9f
             var width = (currentState.targetResolutionHeight * aspectRatio).toInt()
             var height = currentState.targetResolutionHeight
             
             // Ensure even dimensions for encoder compatibility
             if (width % 2 != 0) width -= 1
             if (height % 2 != 0) height -= 1
             
             if (width > 0 && height > 0) {
                 effectsList.add(Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT))
             }
        }
        
        if (currentState.activePreset != QualityPreset.HIGH && currentState.targetFps > 0) {
             effectsList.add(FrameDropEffect.createSimpleFrameDropEffect(currentState.originalFps, currentState.targetFps.toFloat()))
        }
        
        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effectsList))
            .setRemoveAudio(currentState.removeAudio)
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

    private fun getAudioBitrate(context: Context, uri: Uri): Int {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        return format.getInteger(MediaFormat.KEY_BIT_RATE)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }
        return 0
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
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    if (currentState.originalDate != null) {
                         put(MediaStore.Video.Media.DATE_TAKEN, currentState.originalDate)
                    }
                    
                    if (currentState.originalLocation != null) {
                         val matcher = java.util.regex.Pattern.compile("([+-]\\d+\\.\\d+)([+-]\\d+\\.\\d+)").matcher(currentState.originalLocation)
                         if (matcher.find()) {
                             val lat = matcher.group(1)?.toDoubleOrNull()
                             val lon = matcher.group(2)?.toDoubleOrNull()
                             if (lat != null && lon != null) {
                                  put(MediaStore.Video.Media.LATITUDE, lat)
                                  put(MediaStore.Video.Media.LONGITUDE, lon)
                             }
                         }
                    }

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

}
