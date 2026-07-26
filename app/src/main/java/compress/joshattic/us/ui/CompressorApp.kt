package compress.joshattic.us.ui

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.collect
import androidx.compose.material.icons.outlined.Settings
import compress.joshattic.us.R
import compress.joshattic.us.ui.screens.CompressionFailedScreen
import compress.joshattic.us.ui.screens.CompressingScreen
import compress.joshattic.us.ui.screens.ConfigScreen
import compress.joshattic.us.ui.screens.EmptyScreen
import compress.joshattic.us.ui.screens.ResultScreen
import compress.joshattic.us.ui.screens.settings.AboutScreen
import compress.joshattic.us.ui.screens.settings.DisplaySettingsScreen
import compress.joshattic.us.ui.screens.settings.PlaceholderSettingsScreen
import compress.joshattic.us.ui.screens.settings.SettingsScreen
import compress.joshattic.us.viewmodel.CompressorViewModel
import java.io.File

enum class SettingsDestination {
    MAIN, ABOUT, DISPLAY, PRESETS, VIDEO, AUDIO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressorApp(viewModel: CompressorViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val window = (context as? ComponentActivity)?.window

    var forceShowResult by remember { mutableStateOf(false) }
    var currentSettingsDestination by remember { mutableStateOf<SettingsDestination?>(null) }
    
    // Reset forceShowResult when we leave the result screen
    LaunchedEffect(state.compressedUri) {
        if (state.compressedUri == null) {
            forceShowResult = false
        }
    }

    val shareVideoTitle = stringResource(R.string.share_video_title)
    val shareErrorTemplate = stringResource(R.string.share_error)

    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isSettingsOpen = currentSettingsDestination != null
    PredictiveBackHandler(enabled = isSettingsOpen || state.selectedUri != null) { progress ->
        try {
            progress.collect()
        } finally {
            if (isSettingsOpen) {
                if (currentSettingsDestination == SettingsDestination.MAIN) {
                    currentSettingsDestination = null
                } else {
                    currentSettingsDestination = SettingsDestination.MAIN
                }
            } else if (state.isCompressing) {
                viewModel.cancelCompression()
            } else {
                viewModel.reset()
            }
        }
    }
    
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.updateSelectedUri(context, uri)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        if (uri != null) {
            viewModel.saveToUri(context, uri)
        }
    }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            viewModel.setCustomOutputFolder(context, uri)
        }
    }
    
    fun shareVideo(uri: Uri?) {
        if (uri == null) return
        try {
            val file = File(uri.path!!)
            val contentUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, shareVideoTitle))
        } catch (e: Exception) {
            Toast.makeText(context, shareErrorTemplate.format(e.message), Toast.LENGTH_SHORT).show()
        }
    }

    AnimatedContent(
        targetState = when {
            state.isCompressing -> "compressing"
            currentSettingsDestination != null -> "settings"
            else -> "main"
        },
        transitionSpec = {
            if (targetState == "settings" || targetState == "compressing") {
                slideInVertically { h -> h } + fadeIn() togetherWith fadeOut()
            } else {
                fadeIn() togetherWith slideOutVertically { h -> h }
            }
        },
        label = "TopLevelContent"
    ) { destination ->
        when (destination) {
            "compressing" -> {
                CompressingScreen(state = state, onCancel = { viewModel.cancelCompression() })
            }
            "settings" -> {
                AnimatedContent(
                    targetState = currentSettingsDestination,
                    transitionSpec = {
                        if (targetState != SettingsDestination.MAIN) {
                            slideInHorizontally { w -> w } + fadeIn() togetherWith slideOutHorizontally { w -> -w } + fadeOut()
                        } else {
                            slideInHorizontally { w -> -w } + fadeIn() togetherWith slideOutHorizontally { w -> w } + fadeOut()
                        }
                    },
                    label = "SettingsFlow"
                ) { settingsDest ->
                    when (settingsDest) {
                        SettingsDestination.MAIN -> SettingsScreen(
                            state = state,
                            onBack = { currentSettingsDestination = null },
                            onNavigateToAbout = { currentSettingsDestination = SettingsDestination.ABOUT },
                            onNavigateToDisplay = { currentSettingsDestination = SettingsDestination.DISPLAY },
                            onNavigateToPresets = { currentSettingsDestination = SettingsDestination.PRESETS },
                            onNavigateToVideo = { currentSettingsDestination = SettingsDestination.VIDEO },
                            onNavigateToAudio = { currentSettingsDestination = SettingsDestination.AUDIO }
                        )
                        SettingsDestination.ABOUT -> AboutScreen(
                            state = state,
                            onBack = { currentSettingsDestination = SettingsDestination.MAIN },
                            onEnableAllCodecs = { viewModel.enableAllCodecsFeature() },
                            onDisableAllCodecs = { viewModel.disableAllCodecsFeature() },
                            isSoftwareCodec = { viewModel.isSoftwareCodec(it) }
                        )
                        SettingsDestination.DISPLAY -> DisplaySettingsScreen(
                            state = state,
                            onBack = { currentSettingsDestination = SettingsDestination.MAIN },
                            onToggleAutoSaveToPhotos = { viewModel.toggleAutoSaveToPhotos() },
                            onChangeOutputLocation = {
                                val initial = state.customOutputTreeUri?.let { Uri.parse(it) }
                                openDocumentTreeLauncher.launch(initial)
                            },
                            onResetOutputLocation = { viewModel.clearCustomOutputFolder(context) },
                            onToggleShowBitrate = { viewModel.toggleShowBitrate() },
                            onToggleBitrateUnit = { viewModel.toggleBitrateUnit() },
                            onToggleShowStorageSaved = { viewModel.toggleShowStorageSaved() },
                            onToggleShowTargetSizePreset = { viewModel.toggleShowTargetSizePreset() }
                        )
                        SettingsDestination.PRESETS -> compress.joshattic.us.ui.screens.settings.PresetsSettingsScreen(
                            state = state,
                            onBack = { currentSettingsDestination = SettingsDestination.MAIN },
                            onUpdateHighPreset = { viewModel.updateHighPresetConfig(it) },
                            onUpdateMediumPreset = { viewModel.updateMediumPresetConfig(it) },
                            onUpdateLowPreset = { viewModel.updateLowPresetConfig(it) },
                            onResetQualityPresets = { viewModel.resetQualityPresets() },
                            onAddTargetSizePreset = { label, size -> viewModel.addTargetSizePreset(label, size) },
                            onUpdateTargetSizePreset = { id, label, size -> viewModel.updateTargetSizePreset(id, label, size) },
                            onDeleteTargetSizePreset = { id -> viewModel.deleteTargetSizePreset(id) },
                            onResetTargetSizePresets = { viewModel.resetTargetSizePresets() }
                        )
                        SettingsDestination.VIDEO -> compress.joshattic.us.ui.screens.settings.VideoSettingsScreen(
                            state = state,
                            onBack = { currentSettingsDestination = SettingsDestination.MAIN },
                            onUpdateVideoConfig = { viewModel.updateDefaultVideoConfig(it) },
                            onResetVideoConfig = { viewModel.resetDefaultVideoConfig() }
                        )
                        SettingsDestination.AUDIO -> compress.joshattic.us.ui.screens.settings.AudioSettingsScreen(
                            state = state,
                            onBack = { currentSettingsDestination = SettingsDestination.MAIN },
                            onUpdateAudioConfig = { viewModel.updateDefaultAudioConfig(it) },
                            onResetAudioConfig = { viewModel.resetDefaultAudioConfig() }
                        )
                        null -> {}
                    }
                }
            }
            else -> {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { 
                                Text(
                                    stringResource(R.string.title_compressor), 
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                ) 
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            actions = {
                                IconButton(onClick = { currentSettingsDestination = SettingsDestination.MAIN }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings, 
                                        contentDescription = stringResource(R.string.settings_content_desc), 
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AnimatedContent(
                            targetState = when {
                                state.selectedUri == null -> 0
                                state.compressedUri != null || state.error != null -> 2
                                else -> 1
                            },
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { w -> w } + fadeIn() togetherWith slideOutHorizontally { w -> -w } + fadeOut()
                                } else {
                                    slideInHorizontally { w -> -w } + fadeIn() togetherWith slideOutHorizontally { w -> w } + fadeOut()
                                }
                            },
                            label = "FlowContent"
                        ) { index ->
                            when(index) {
                                0 -> EmptyScreen(
                                    totalSaved = state.formattedTotalSaved,
                                    showStorageSaved = state.showStorageSaved,
                                    onPick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }
                                )
                                2 -> {
                                    if (state.error != null) {
                                         CompressionFailedScreen(
                                            state = state,
                                            onBack = { viewModel.reset() },
                                            onSaveAnyway = { /* No-op for actual errors */ }
                                        )
                                    } else if (state.compressedSize > state.originalSize && !forceShowResult) {
                                        CompressionFailedScreen(
                                            state = state,
                                            onBack = { viewModel.reset() },
                                            onSaveAnyway = { forceShowResult = true }
                                        )
                                    } else {
                                        ResultScreen(
                                            state = state,
                                            onShare = { 
                                                shareVideo(state.compressedUri) 
                                                viewModel.markAsShared()
                                            },
                                            onSave = {
                                                val hasCustomLocation = !state.customOutputTreeUri.isNullOrBlank()
                                                if (hasCustomLocation ||
                                                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
                                                ) {
                                                    viewModel.saveCompressedOutput(context)
                                                } else {
                                                    val fileName = state.compressedUri?.lastPathSegment
                                                        ?: "CompressedVideo.mp4"
                                                    createDocumentLauncher.launch(fileName)
                                                }
                                            },
                                            onCompressAnother = { viewModel.reset() },
                                            onBack = { viewModel.reset() }
                                        )
                                    }
                                }
                                else -> ConfigScreen(state, viewModel, context)
                            }
                        }
                    }
                }
            }
        }
    }
}
