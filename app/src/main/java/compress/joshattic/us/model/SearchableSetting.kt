package compress.joshattic.us.model

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import compress.joshattic.us.R

data class SearchableSetting(
    val title: String,
    val description: String,
    val categoryName: String,
    val categoryId: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun rememberSearchableSettings(
    state: CompressorUiState,
    onNavigateToDisplay: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToVideo: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToAbout: () -> Unit
): List<SearchableSetting> {
    val displayTitle = stringResource(R.string.general_settings_title)
    val presetsTitle = stringResource(R.string.tab_presets)
    val videoTitle = stringResource(R.string.tab_video)
    val audioTitle = stringResource(R.string.tab_audio)
    val aboutTitle = stringResource(R.string.about_compressor_title)

    val categoryDisplaySubtitle = stringResource(R.string.category_display_subtitle)
    val categoryPresetsSubtitle = stringResource(R.string.category_presets_subtitle)
    val categoryVideoSubtitle = stringResource(R.string.category_video_subtitle)
    val categoryAudioSubtitle = stringResource(R.string.category_audio_subtitle)
    val versionFormat = stringResource(R.string.version_format, state.appInfoVersion)

    val autoSavePhotosTitle = stringResource(R.string.auto_save_photos_title)
    val autoSavePhotosSubtitle = stringResource(R.string.auto_save_photos_subtitle)
    val outputLocationTitle = stringResource(R.string.output_location_title)
    val outputLocationSubtitle = stringResource(R.string.output_location_subtitle)
    val outputLocationResetTitle = stringResource(R.string.output_location_reset_title)
    val outputLocationResetSubtitle = stringResource(R.string.output_location_reset_subtitle)
    val showBitrate = stringResource(R.string.show_bitrate)
    val showBitrateSubtitle = stringResource(R.string.show_bitrate_subtitle)
    val bitrateUnitMbps = stringResource(R.string.bitrate_unit_mbps)
    val bitrateUnitSubtitle = stringResource(R.string.bitrate_unit_mbps_subtitle) + " / " + stringResource(R.string.bitrate_unit_kbps_subtitle)
    val showStorageSavedTitle = stringResource(R.string.show_storage_saved_title)
    val showStorageSavedSubtitle = stringResource(R.string.show_storage_saved_subtitle)
    val showTargetSizePresetTitle = stringResource(R.string.show_target_size_preset_title)
    val showTargetSizePresetSubtitle = stringResource(R.string.show_target_size_preset_subtitle)

    val defaultVideoCodec = stringResource(R.string.default_video_codec)
    val defaultVideoCodecDesc = stringResource(R.string.default_video_codec_desc)
    val defaultResolution = stringResource(R.string.default_resolution)
    val defaultFramerate = stringResource(R.string.default_framerate)
    val defaultSizeRatio = stringResource(R.string.default_size_ratio)
    val encoding = stringResource(R.string.encoding)
    val targetSize = stringResource(R.string.target_size)
    val advancedOptions = stringResource(R.string.advanced_options)
    val resolution = stringResource(R.string.resolution)
    val framerate = stringResource(R.string.framerate)
    val resetVideoDefaults = stringResource(R.string.reset_video_defaults)

    val defaultAudioBitrate = stringResource(R.string.default_audio_bitrate)
    val defaultMuteAudio = stringResource(R.string.default_mute_audio)
    val defaultMuteAudioDesc = stringResource(R.string.default_mute_audio_desc)
    val defaultAudioVolume = stringResource(R.string.default_audio_volume)
    val audioBitrate = stringResource(R.string.audio_bitrate)
    val volume = stringResource(R.string.volume)
    val audioOptions = stringResource(R.string.audio_options)
    val removeAudio = stringResource(R.string.remove_audio)
    val resetAudioDefaults = stringResource(R.string.reset_audio_defaults)

    val headerQualityPresets = stringResource(R.string.header_quality_presets)
    val qualityPreset = stringResource(R.string.quality_preset)
    val presetHigh = stringResource(R.string.preset_high)
    val presetMedium = stringResource(R.string.preset_medium)
    val presetLow = stringResource(R.string.preset_low)
    val presetSummary = "$presetHigh • $presetMedium • $presetLow"
    val targetSizeLimits = stringResource(R.string.target_size_limits)
    val addPreset = stringResource(R.string.add_preset)
    val resetToDefault = stringResource(R.string.reset_to_default)

    val infoAppName = stringResource(R.string.info_app_name)
    val appName = stringResource(R.string.app_name)
    val infoAppVersion = stringResource(R.string.info_app_version)
    val infoDevice = stringResource(R.string.info_device)
    val infoAndroid = stringResource(R.string.info_android)
    val infoSupportedCodecs = stringResource(R.string.info_supported_codecs)
    val codecsSupportedCount = stringResource(R.string.codecs_supported_count, state.supportedCodecs.size)
    val enableAllCodecs = stringResource(R.string.enable_all_codecs)
    val headerHardwareCodecs = stringResource(R.string.header_hardware_codecs)
    val viewOnGithub = stringResource(R.string.view_on_github)
    val infoCopyClipboard = stringResource(R.string.info_copy_clipboard)
    val headerLinksActions = stringResource(R.string.header_links_actions)

    return remember(
        state.appInfoVersion,
        state.supportedCodecs.size,
        state.targetSizePresets,
        displayTitle,
        presetsTitle,
        videoTitle,
        audioTitle,
        aboutTitle
    ) {
        val settings = mutableListOf<SearchableSetting>()

        // 1. Categories
        settings.add(SearchableSetting(displayTitle, categoryDisplaySubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(presetsTitle, categoryPresetsSubtitle, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(videoTitle, categoryVideoSubtitle, videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(audioTitle, categoryAudioSubtitle, audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(aboutTitle, versionFormat, aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))

        // 2. Display / General
        settings.add(SearchableSetting(autoSavePhotosTitle, autoSavePhotosSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(outputLocationTitle, outputLocationSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(outputLocationResetTitle, outputLocationResetSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(showBitrate, showBitrateSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(bitrateUnitMbps, bitrateUnitSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(showStorageSavedTitle, showStorageSavedSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))
        settings.add(SearchableSetting(showTargetSizePresetTitle, showTargetSizePresetSubtitle, displayTitle, "display", Icons.Default.Tune, onNavigateToDisplay))

        // 3. Video
        settings.add(SearchableSetting(defaultVideoCodec, defaultVideoCodecDesc, videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(defaultResolution, "$categoryVideoSubtitle (Original, 1080p, 720p, 480p)", videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(defaultFramerate, "$categoryVideoSubtitle (Original, 60fps, 30fps)", videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(defaultSizeRatio, categoryVideoSubtitle, videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(encoding, "AV1, H.265 (HEVC), H.264 (AVC)", videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(targetSize, advancedOptions, videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(resolution, "8K, 4K, 1080p, 720p, 480p", videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(framerate, "Original, 60fps, 30fps", videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))
        settings.add(SearchableSetting(resetVideoDefaults, categoryVideoSubtitle, videoTitle, "video", Icons.Default.Movie, onNavigateToVideo))

        // 4. Audio
        settings.add(SearchableSetting(defaultAudioBitrate, "320k, 256k, 192k, 128k, 96k", audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(defaultMuteAudio, defaultMuteAudioDesc, audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(defaultAudioVolume, categoryAudioSubtitle, audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(audioBitrate, "320k, 256k, 192k, 128k, 96k, 64k", audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(volume, audioOptions, audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(removeAudio, audioOptions, audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))
        settings.add(SearchableSetting(resetAudioDefaults, categoryAudioSubtitle, audioTitle, "audio", Icons.Default.MusicNote, onNavigateToAudio))

        // 5. Presets
        settings.add(SearchableSetting(headerQualityPresets, qualityPreset, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(qualityPreset, presetSummary, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(presetHigh, headerQualityPresets, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(presetMedium, headerQualityPresets, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(presetLow, headerQualityPresets, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(targetSizeLimits, "Discord (10MB), Email (25MB), Stories (50MB), Twitter/X", presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(addPreset, targetSizeLimits, presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))
        settings.add(SearchableSetting(resetToDefault, "$headerQualityPresets / $targetSizeLimits", presetsTitle, "presets", Icons.Outlined.BookmarkBorder, onNavigateToPresets))

        state.targetSizePresets.forEach { preset ->
            settings.add(
                SearchableSetting(
                    title = preset.label,
                    description = "${preset.sizeMb.toInt()} MB",
                    categoryName = presetsTitle,
                    categoryId = "presets",
                    icon = Icons.Outlined.BookmarkBorder,
                    onClick = onNavigateToPresets
                )
            )
        }

        // 6. About
        settings.add(SearchableSetting(infoAppName, appName, aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))
        settings.add(SearchableSetting(infoAppVersion, "v${state.appInfoVersion}", aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))
        settings.add(SearchableSetting(infoDevice, "${Build.MANUFACTURER} ${Build.MODEL}", aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))
        settings.add(SearchableSetting(infoAndroid, "Android ${Build.VERSION.RELEASE}", aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))
        settings.add(SearchableSetting(infoSupportedCodecs, codecsSupportedCount, aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))
        settings.add(SearchableSetting(enableAllCodecs, headerHardwareCodecs, aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))
        settings.add(SearchableSetting(viewOnGithub, "https://github.com/JoshAtticus/Compressor", aboutTitle, "about", Icons.Default.Build, onNavigateToAbout))
        settings.add(SearchableSetting(infoCopyClipboard, headerLinksActions, aboutTitle, "about", Icons.Default.Info, onNavigateToAbout))

        settings
    }
}

fun filterSearchableSettings(
    settings: List<SearchableSetting>,
    query: String
): List<SearchableSetting> {
    if (query.isBlank()) return emptyList()
    val q = query.trim().lowercase()
    return settings.filter {
        it.title.lowercase().contains(q) ||
        it.description.lowercase().contains(q) ||
        it.categoryName.lowercase().contains(q)
    }.distinctBy { it.title + it.categoryName }
}
