package compress.joshattic.us.viewmodel

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
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.model.QualityPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
class CompressorViewModel(application: Application) : AndroidViewModel(application) {
    private data class VideoTrackInfo(
        val mimeType: String?,
        val width: Int,
        val height: Int,
        val frameRate: Float
    )

    private data class CompressionPlan(
        val outputVideoMimeType: String,
        val outputHeight: Int,
        val outputFps: Int,
        val warnings: List<String>,
        val blockingError: String?
    )

    private val _uiState = MutableStateFlow(CompressorUiState())
    val uiState = _uiState.asStateFlow()
    
    private val prefs: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences("compressor_prefs", Context.MODE_PRIVATE)
    }

    init {
        val saved = prefs.getLong("total_saved_bytes", 0L)
        val showBitrate = prefs.getBoolean("show_bitrate", false)
        val useMbps = prefs.getBoolean("use_mbps", false)
        _uiState.update { it.copy(
            totalSavedBytes = saved, 
            showBitrate = showBitrate, 
            useMbps = useMbps
        ) }
        checkSupportedCodecs()
        clearCache()
    }
    
    private fun checkSupportedCodecs() {
        val allCodecsEnabled = prefs.getBoolean("all_codecs_enabled", false)
        val allCodecsUnlocked = prefs.getBoolean("all_codecs_unlocked", false)
        val supported = mutableListOf<String>()

        if (allCodecsEnabled) {
            supported.addAll(getDeviceEncoders())
        } else {
            supported.add(MimeTypes.VIDEO_H264)
            if (hasEncoder(MimeTypes.VIDEO_H265)) {
                supported.add(MimeTypes.VIDEO_H265)
            }
            if (hasEncoder(MimeTypes.VIDEO_AV1)) {
                supported.add(MimeTypes.VIDEO_AV1)
            }
        }
        
        _uiState.update { 
            var newCodec = it.videoCodec
            if (!supported.contains(newCodec)) {
                newCodec = when {
                    supported.contains(MimeTypes.VIDEO_H265) -> MimeTypes.VIDEO_H265
                    supported.contains(MimeTypes.VIDEO_H264) -> MimeTypes.VIDEO_H264
                    supported.isNotEmpty() -> supported.first()
                    else -> MimeTypes.VIDEO_H264
                }
            }
            it.copy(
                supportedCodecs = supported, 
                videoCodec = newCodec, 
                useH265 = newCodec == MimeTypes.VIDEO_H265,
                allCodecsEnabled = allCodecsEnabled,
                allCodecsUnlocked = allCodecsUnlocked
            ) 
        }
    }

    private fun getDeviceEncoders(): List<String> {
        val codecs = mutableSetOf<String>()
        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in list.codecInfos) {
                if (!info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.startsWith("video/", ignoreCase = true)) {
                        codecs.add(type.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val preferred = listOf(MimeTypes.VIDEO_H264, MimeTypes.VIDEO_H265, MimeTypes.VIDEO_AV1)
        return codecs.toList().sortedWith { a, b ->
            val indexA = preferred.indexOf(a)
            val indexB = preferred.indexOf(b)
            when {
                indexA != -1 && indexB != -1 -> indexA.compareTo(indexB)
                indexA != -1 -> -1
                indexB != -1 -> 1
                else -> a.compareTo(b)
            }
        }
    }

    fun isSoftwareCodec(mimeType: String): Boolean {
        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            var hasHardware = false
            var hasSoftware = false
            for (info in list.codecInfos) {
                if (!info.isEncoder) continue
                if (info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }) {
                    val isSW = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isSoftwareOnly
                    } else {
                        val name = info.name.lowercase()
                        name.startsWith("c2.android") || name.startsWith("omx.google")
                    }
                    if (isSW) {
                        hasSoftware = true
                    } else {
                        hasHardware = true
                    }
                }
            }
            return hasSoftware && !hasHardware
        } catch (e: Exception) {
            return false
        }
    }

    fun enableAllCodecsFeature() {
        prefs.edit {
            putBoolean("all_codecs_enabled", true)
            putBoolean("all_codecs_unlocked", true)
        }
        checkSupportedCodecs()
    }

    fun disableAllCodecsFeature() {
        prefs.edit {
            putBoolean("all_codecs_enabled", false)
            putBoolean("all_codecs_unlocked", false)
        }
        checkSupportedCodecs()
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
        viewModelScope.launch(Dispatchers.IO) {
            var size = 0L
            var width = 0
            var height = 0
            var bitrate = 0
            var audioBitrate = 0
            var fps = 30f
            var videoMime: String? = null
            var duration = 0L
            var originalName: String? = null

            try {
                audioBitrate = getAudioBitrate(context, uri)
                val videoInfo = getVideoTrackInfo(context, uri)
                videoMime = videoInfo?.mimeType
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

                val fpsStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                fps = fpsStr?.toFloatOrNull() ?: 0f
                if (fps <= 0f && videoInfo != null && videoInfo.frameRate > 0f) {
                    fps = videoInfo.frameRate
                }
                if (fps <= 0f) {
                    fps = 30f
                }

                val cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        originalName = cursor.getString(nameIndex)
                    }
                    cursor.close()
                }

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
                originalVideoMime = videoMime,
                durationMs = duration,
                originalName = originalName,
                targetSizeMb = defaultTargetMb,
                targetResolutionHeight = height,
                activePreset = QualityPreset.HIGH,
                totalSavedBytes = currentSavedBytes,
                showBitrate = showBitrate,
                useMbps = useMbps,
                supportedCodecs = supportedCodecs
            ).autoAdjust(defaultTargetMb)
        }
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
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.7).toFloat().coerceAtLeast(0.1f),
                         audioBitrate = 320_000,
                         removeAudio = false
                     ).autoAdjust((current.originalSize / (1024.0 * 1024.0) * 0.7).toFloat().coerceAtLeast(0.1f), lockAudioBitrate = true, allowUpward = false)
                 }
            }
            QualityPreset.MEDIUM -> {
                 _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.MEDIUM,
                         targetResolutionHeight = getTargetHeight(1080),
                         targetFps = if (current.originalFps < 30) 0 else 30,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.4).toFloat().coerceAtLeast(0.1f),
                         audioBitrate = 192_000,
                         removeAudio = false
                     ).autoAdjust((current.originalSize / (1024.0 * 1024.0) * 0.4).toFloat().coerceAtLeast(0.1f), lockAudioBitrate = true, allowUpward = false)
                 }
            }
            QualityPreset.LOW -> {
                  _uiState.update { 
                     it.copy(
                         activePreset = QualityPreset.LOW,
                         targetResolutionHeight = getTargetHeight(720),
                         targetFps = if (current.originalFps < 30) 0 else 30,
                         targetSizeMb = (current.originalSize / (1024.0 * 1024.0) * 0.2).toFloat().coerceAtLeast(0.1f),
                         audioBitrate = 128_000,
                         removeAudio = false
                     ).autoAdjust((current.originalSize / (1024.0 * 1024.0) * 0.2).toFloat().coerceAtLeast(0.1f), lockAudioBitrate = true, allowUpward = false)
                 }
            }
            else -> {}
        }
    }

    fun setTargetSize(mb: Float) {
        _uiState.update { it.copy(targetSizeMb = mb, activePreset = QualityPreset.CUSTOM).autoAdjust(mb, allowUpward = false) }
    }

    fun setVideoCodec(codec: String) {
        _uiState.update { 
            val temp = it.copy(
                videoCodec = codec, 
                useH265 = codec == MimeTypes.VIDEO_H265, 
                activePreset = QualityPreset.CUSTOM
            )
            temp.autoAdjust(temp.targetSizeMb)
        }
    }

    fun toggleShowBitrate() {
        _uiState.update { 
            val newValue = !it.showBitrate
            prefs.edit { putBoolean("show_bitrate", newValue) }
            it.copy(showBitrate = newValue)
        }
    }

    fun toggleBitrateUnit() {
        _uiState.update { 
            val newValue = !it.useMbps
            prefs.edit { putBoolean("use_mbps", newValue) }
            it.copy(useMbps = newValue)
        }
    }

    fun toggleRemoveAudio() {
        _uiState.update { 
            val temp = it.copy(removeAudio = !it.removeAudio, activePreset = QualityPreset.CUSTOM)
            if (temp.removeAudio) {
                 temp
            } else {
                 temp.autoAdjust(temp.targetSizeMb)    
            }
        }
    }

    fun setAudioBitrate(bitrate: Int) {
        _uiState.update {
            val temp = it.copy(audioBitrate = bitrate, activePreset = QualityPreset.CUSTOM)
            temp.autoAdjust(temp.targetSizeMb, lockAudioBitrate = true)
        }
    }

    fun setAudioVolume(volume: Float) {
        _uiState.update { it.copy(audioVolume = volume, activePreset = QualityPreset.CUSTOM) }
    }

    fun setResolution(height: Int) {
        _uiState.update {
            val isVertical = it.originalHeight > it.originalWidth
            val mappedHeight = if (
                isVertical &&
                it.originalWidth > 0 &&
                it.originalHeight > 0 &&
                height > 0
            ) {
                (height.toLong() * it.originalHeight / it.originalWidth).toInt()
            } else {
                height
            }
            it.copy(targetResolutionHeight = mappedHeight, activePreset = QualityPreset.CUSTOM)
        }
    }

    fun setFps(fps: Int) {
        _uiState.update { it.copy(targetFps = fps, activePreset = QualityPreset.CUSTOM) }
    }
    
    fun cancelCompression() {
        activeTransformer?.cancel()
        compressionJob?.cancel()
        _uiState.update { it.copy(isCompressing = false, progress = 0f) }
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

    fun startCompression(context: Context) = viewModelScope.launch(Dispatchers.Main) {
        val currentState = _uiState.value
        val inputUri = currentState.selectedUri ?: return@launch

        val plan = withContext(Dispatchers.IO) { buildCompressionPlan(context, currentState, inputUri) }
        if (plan.blockingError != null) {
            _uiState.update { it.copy(error = plan.blockingError, errorLog = null, isCompressing = false) }
            return@launch
        }

        _uiState.update {
            it.copy(
                isCompressing = true,
                progress = 0f,
                currentOutputSize = 0L,
                error = null,
                errorLog = null,
                compressedUri = null,
                saveSuccess = false,
                warnings = plan.warnings
            )
        }

        val outputDir = File(context.cacheDir, "compressed_videos")
        outputDir.mkdirs()
        val baseName = currentState.originalName?.substringBeforeLast(".") ?: "Compressed_${System.currentTimeMillis()}"
        val outputFile = File(outputDir, "${baseName}_Compressed.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        val outputPath = outputFile.absolutePath

        val targetBitrate = currentState.targetBitrate.toLong()

        val audioBitrateToUse = if (currentState.audioBitrate == 0) {
            if (currentState.originalAudioBitrate > 0) currentState.originalAudioBitrate else 128_000
        } else {
            currentState.audioBitrate
        }

        val videoMimeType = plan.outputVideoMimeType

        val decoderFactory = DefaultDecoderFactory.Builder(context)
            .setEnableDecoderFallback(true)
            .build()

        val cbrEncoderFactory = DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetBitrate.toInt())
                    .setBitrateMode(android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
                    .build()
            )
            .setRequestedAudioEncoderSettings(
                AudioEncoderSettings.Builder()
                    .setBitrate(audioBitrateToUse)
                    .build()
            )
            .build()

        val vbrEncoderFactory = DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetBitrate.toInt())
                    .setBitrateMode(android.media.MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                    .build()
            )
            .setRequestedAudioEncoderSettings(
                AudioEncoderSettings.Builder()
                    .setBitrate(audioBitrateToUse)
                    .build()
            )
            .build()
            
        val encoderFactory = object : androidx.media3.transformer.Codec.EncoderFactory {
            override fun createForAudioEncoding(format: androidx.media3.common.Format): androidx.media3.transformer.Codec {
                return cbrEncoderFactory.createForAudioEncoding(format)
            }

            override fun createForVideoEncoding(format: androidx.media3.common.Format): androidx.media3.transformer.Codec {
                val targetFps = if (plan.outputFps > 0) plan.outputFps.toFloat() else currentState.originalFps
                var modifiedFormatBuilder = format.buildUpon()
                if (targetFps > 0f) {
                    modifiedFormatBuilder.setFrameRate(targetFps)
                }
                if (format.colorInfo == null || !androidx.media3.common.ColorInfo.isTransferHdr(format.colorInfo)) {
                     modifiedFormatBuilder.setColorInfo(null)
                }
                val modifiedFormat = modifiedFormatBuilder.build()

                return try {
                    cbrEncoderFactory.createForVideoEncoding(modifiedFormat)
                } catch (e: Exception) {
                    vbrEncoderFactory.createForVideoEncoding(modifiedFormat)
                }
            }

            override fun audioNeedsEncoding(): Boolean = cbrEncoderFactory.audioNeedsEncoding()
            override fun videoNeedsEncoding(): Boolean = cbrEncoderFactory.videoNeedsEncoding()
        }
        
        val transformerBuilder = Transformer.Builder(context)
            .setVideoMimeType(videoMimeType)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setAssetLoaderFactory(androidx.media3.transformer.DefaultAssetLoaderFactory(context, decoderFactory, androidx.media3.common.util.Clock.DEFAULT))
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                     val finalSize = outputFile.length()
                     val savedBytes = currentState.originalSize - finalSize
                     var newTotal = _uiState.value.totalSavedBytes
                     
                     if (savedBytes > 0) {
                         newTotal += savedBytes
                         prefs.edit { putLong("total_saved_bytes", newTotal) }
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
                        val isDecoderInitError = exportException.errorCode == ExportException.ERROR_CODE_DECODER_INIT_FAILED
                        val isEncoderInitError = exportException.errorCode == ExportException.ERROR_CODE_ENCODER_INIT_FAILED
                        val isMuxerError = exportException.errorCode == ExportException.ERROR_CODE_MUXING_FAILED
                        val isHuawei = android.os.Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)

                        val errorMsg = when {
                            isMuxerError && isHuawei -> app.getString(R.string.error_huawei_muxer)
                            isDecoderInitError -> app.getString(R.string.error_decoder_config_unsupported)
                            isEncoderInitError -> app.getString(R.string.error_encoder_config_unsupported)
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

        val transformer = transformerBuilder.build()
        
        activeTransformer = transformer
            
        val effectsList = mutableListOf<Effect>()
        
           if (plan.outputHeight > 0 && plan.outputHeight != currentState.originalHeight) {
             val aspectRatio = if (currentState.originalHeight > 0) currentState.originalWidth.toFloat() / currentState.originalHeight else 16f/9f
                var width = (plan.outputHeight * aspectRatio).toInt()
                var height = plan.outputHeight
              
              if (width % 2 != 0) width -= 1
              if (height % 2 != 0) height -= 1
              
              if (width > 0 && height > 0) {
                  effectsList.add(Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT))
              }
        }
        
        if (plan.outputFps > 0 && plan.outputFps.toFloat() < currentState.originalFps) {
            effectsList.add(FrameDropEffect.createSimpleFrameDropEffect(currentState.originalFps, plan.outputFps.toFloat()))
        }
        
        val mediaItem = MediaItem.fromUri(inputUri)
        val audioProcessors: List<androidx.media3.common.audio.AudioProcessor> = if (!currentState.removeAudio) {
            listOf(androidx.media3.common.audio.SonicAudioProcessor())
        } else {
            emptyList()
        }
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(audioProcessors, effectsList))
            .setRemoveAudio(currentState.removeAudio)
            .build()

        var hdrMode = Composition.HDR_MODE_KEEP_HDR
        if (Build.MANUFACTURER.equals("Google", ignoreCase = true) && Build.MODEL.contains("Pixel 10")) {
             if (videoMimeType == MimeTypes.VIDEO_H265 || videoMimeType == MimeTypes.VIDEO_H264) {
                 if (isHdr(context, inputUri)) {
                      hdrMode = Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
                      val warningMsg = getApplication<Application>().getString(R.string.warning_hdr_tone_mapped)
                      _uiState.update { it.copy(warnings = listOf(warningMsg)) }
                 }
             }
        }

        val composition = Composition.Builder(
            listOf(EditedMediaItemSequence(editedMediaItem))
        )
        .setHdrMode(hdrMode)
        .build()

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

    private fun getVideoTrackInfo(context: Context, uri: Uri): VideoTrackInfo? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    val width = if (format.containsKey(MediaFormat.KEY_WIDTH)) format.getInteger(MediaFormat.KEY_WIDTH) else 0
                    val height = if (format.containsKey(MediaFormat.KEY_HEIGHT)) format.getInteger(MediaFormat.KEY_HEIGHT) else 0
                    var frameRate = 0f
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        try {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                        } catch (e: Exception) {
                            try {
                                frameRate = format.getFloat(MediaFormat.KEY_FRAME_RATE)
                            } catch (ignored: Exception) {}
                        }
                    }
                    return VideoTrackInfo(mime, width, height, frameRate)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }
        return null
    }

    private fun buildCompressionPlan(context: Context, state: CompressorUiState, inputUri: Uri): CompressionPlan {
        var outputMime = state.videoCodec
        var outputHeight = state.targetResolutionHeight
        var outputFps = state.targetFps
        val warnings = mutableListOf<String>()

        val sourceInfo = getVideoTrackInfo(context, inputUri)
        val sourceMime = sourceInfo?.mimeType ?: state.originalVideoMime
        val sourceWidth = sourceInfo?.width ?: 0
        val sourceHeight = sourceInfo?.height ?: 0
        val sourceFps = if ((sourceInfo?.frameRate ?: 0f) > 0f) sourceInfo!!.frameRate else state.originalFps

        if (!sourceMime.isNullOrBlank() && sourceWidth > 0 && sourceHeight > 0) {
            val decoderSupported = isCodecConfigurationSupported(
                mimeType = sourceMime,
                width = sourceWidth,
                height = sourceHeight,
                fps = sourceFps,
                encoder = false
            )
            if (!decoderSupported) {
                return CompressionPlan(
                    outputVideoMimeType = outputMime,
                    outputHeight = outputHeight,
                    outputFps = outputFps,
                    warnings = warnings,
                    blockingError = getApplication<Application>().getString(
                        R.string.error_decoder_config_unsupported_details,
                        sourceWidth,
                        sourceHeight,
                        sourceFps,
                        sourceMime.substringAfter("/")
                    )
                )
            }
        }

        val attemptedConfigs = mutableListOf<Triple<String, Int, Int>>()
        fun isCurrentOutputSupported(mime: String, height: Int, fps: Int): Boolean {
            val safeHeight = if (height > 0) height else state.originalHeight
            val safeFps = if (fps > 0) fps else state.originalFps.toInt()
            val aspectRatio = if (state.originalHeight > 0) state.originalWidth.toFloat() / state.originalHeight else 16f / 9f
            var outputWidth = (safeHeight * aspectRatio).toInt().coerceAtLeast(2)
            var outputActualHeight = safeHeight.coerceAtLeast(2)
            if (outputWidth % 2 != 0) outputWidth -= 1
            if (outputActualHeight % 2 != 0) outputActualHeight -= 1
            attemptedConfigs.add(Triple(mime, outputActualHeight, safeFps))
            return isCodecConfigurationSupported(
                mimeType = mime,
                width = outputWidth,
                height = outputActualHeight,
                fps = safeFps.toFloat(),
                encoder = true
            )
        }

        if (!isCurrentOutputSupported(outputMime, outputHeight, outputFps)) {
            if (outputMime != MimeTypes.VIDEO_H264 && isCurrentOutputSupported(MimeTypes.VIDEO_H264, outputHeight, outputFps)) {
                outputMime = MimeTypes.VIDEO_H264
                warnings.add(getApplication<Application>().getString(R.string.warning_codec_fallback_h264))
            } else {
                val fallbackHeights = listOf(1080, 720, 540, 480)
                    .filter { it in 2..state.originalHeight }
                    .ifEmpty { listOf(state.originalHeight.coerceAtLeast(2)) }
                val fallbackFps = listOf(30, 24)
                var supported = false

                for (heightCandidate in fallbackHeights) {
                    for (fpsCandidate in fallbackFps) {
                        if (isCurrentOutputSupported(MimeTypes.VIDEO_H264, heightCandidate, fpsCandidate)) {
                            outputMime = MimeTypes.VIDEO_H264
                            outputHeight = heightCandidate
                            outputFps = fpsCandidate
                            warnings.add(
                                getApplication<Application>().getString(
                                    R.string.warning_quality_fallback,
                                    outputHeight,
                                    outputFps
                                )
                            )
                            supported = true
                            break
                        }
                    }
                    if (supported) break
                }

                if (!supported) {
                    val attempted = attemptedConfigs
                        .joinToString(separator = ", ") { "${it.first.substringAfter("/")} ${it.second}p@${it.third}fps" }
                    return CompressionPlan(
                        outputVideoMimeType = outputMime,
                        outputHeight = outputHeight,
                        outputFps = outputFps,
                        warnings = warnings,
                        blockingError = getApplication<Application>().getString(
                            R.string.error_encoder_config_unsupported_details,
                            attempted
                        )
                    )
                }
            }
        }

        return CompressionPlan(
            outputVideoMimeType = outputMime,
            outputHeight = outputHeight,
            outputFps = outputFps,
            warnings = warnings,
            blockingError = null
        )
    }

    private fun isCodecConfigurationSupported(
        mimeType: String,
        width: Int,
        height: Int,
        fps: Float,
        encoder: Boolean
    ): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val safeFps = kotlin.math.ceil(if (fps > 0f) fps.toDouble() else 30.0)
            codecList.codecInfos
                .asSequence()
                .filter { it.isEncoder == encoder }
                .filter { info -> info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } }
                .any { info ->
                    try {
                        val capabilities = info.getCapabilitiesForType(mimeType)
                        val videoCaps = capabilities.videoCapabilities ?: return@any false
                        videoCaps.areSizeAndRateSupported(width, height, safeFps) ||
                            videoCaps.areSizeAndRateSupported(height, width, safeFps)
                    } catch (_: Exception) {
                        false
                    }
                }
        } catch (_: Exception) {
            false
        }
    }

    private fun isHdr(context: Context, uri: Uri): Boolean {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            if (Build.VERSION.SDK_INT >= 30) {
               val transfer = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)
               return transfer == "6" || transfer == "7"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch(e: Exception) {}
        }
        return false
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
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(compressedUri.path!!)
                if (!file.exists()) {
                    _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_file_lost)) }
                    return@launch
                }

                val targetName = if (currentState.originalName != null) {
                    val nameWithoutExt = currentState.originalName.substringBeforeLast(".")
                    "${nameWithoutExt}_Compressed.mp4"
                } else {
                    "Compressed_${System.currentTimeMillis()}.mp4"
                }

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, targetName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                    if (!containsKey(MediaStore.Video.Media.DATE_ADDED)) {
                        put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    }
                    if (!containsKey(MediaStore.Video.Media.DATE_MODIFIED)) {
                        put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
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
