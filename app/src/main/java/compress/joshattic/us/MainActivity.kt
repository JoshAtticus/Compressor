package compress.joshattic.us

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import compress.joshattic.us.ui.theme.CompressorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File

import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CompressorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        if (!isTablet) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        enableEdgeToEdge()

        // Handle incoming share intent
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("video/") == true) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            
            if (uri != null) {
                viewModel.updateSelectedUri(this, uri)
            }
        }

        setContent {
            CompressorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompressorApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressorApp(viewModel: CompressorViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val window = (context as? ComponentActivity)?.window

    val clipboardManager = LocalClipboardManager.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var forceShowResult by remember { mutableStateOf(false) }
    
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

    androidx.activity.compose.PredictiveBackHandler(enabled = state.selectedUri != null) { progress ->
        try {
            progress.collect()
        } finally {
            if (state.isCompressing) {
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
            context.startActivity(Intent.createChooser(shareIntent,shareVideoTitle))
        } catch (e: Exception) {
            Toast.makeText(context, shareErrorTemplate.format(e.message), Toast.LENGTH_SHORT).show()
        }
    }

    AnimatedContent(
        targetState = state.isCompressing,
        transitionSpec = {
            if (targetState) {
                slideInVertically { h -> h } + fadeIn() togetherWith fadeOut()
            } else {
                fadeIn() togetherWith slideOutVertically { h -> h }
            }
        },
        label = "MainContent"
    ) { isCompressing ->
        if (isCompressing) {
            CompressingScreen(state = state, onCancel = { viewModel.cancelCompression() })
        } else {
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
                            IconButton(onClick = { showInfoDialog = true }) {
                                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onSurface)
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
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                                viewModel.saveToGallery(context)
                                            } else {
                                                createDocumentLauncher.launch("CompressedVideo.mp4")
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
                
                if (showInfoDialog) {
                    val infoText = "App Version: ${state.appInfoVersion}\n" +
                                   "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n" +
                                   "Supported Encoders: ${state.supportedCodecs.joinToString()}"
                                   
                    InfoDialog(
                        state = state,
                        onDismiss = { showInfoDialog = false },
                        onCopy = { 
                            clipboardManager.setText(AnnotatedString(infoText))
                        },
                        onShare = {
                             val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, infoText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        onToggleShowBitrate = { viewModel.toggleShowBitrate() },
                        onToggleBitrateUnit = { viewModel.toggleBitrateUnit() }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScreen(totalSaved: String, onPick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = onPick,
                modifier = Modifier.size(96.dp).scaleOnPress(onPick)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_video_desc), modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.select_video),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (totalSaved != "0.0 MB" && totalSaved != "0 MB") {
             Text(
                text = stringResource(R.string.total_saved, totalSaved),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
fun CompressionFailedScreen(state: CompressorUiState, onBack: () -> Unit, onSaveAnyway: () -> Unit) {
    var showReportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    if (showReportDialog) {
        val errorLogs = remember(state) {
            val sb = StringBuilder()
            sb.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
            sb.append("Android Version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})\n")
            sb.append("App Version: ${state.appInfoVersion}\n")
            sb.append("Original: ${state.originalWidth}x${state.originalHeight} @ ${state.originalFps}fps\n")
            sb.append("Target: ${state.targetResolutionHeight}p @ ${state.targetFps}fps\n")
            sb.append("Codec: ${state.videoCodec}\n")
            sb.append("Error: ${state.error ?: "File larger than original"}\n")
            if (state.errorLog != null) {
                sb.append("\nStack Trace:\n${state.errorLog}")
            }
            sb.toString()
        }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(stringResource(R.string.error_details)) },
            text = {
                Column {
                    Text(
                        errorLogs,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(errorLogs))
                        }) {
                            Text(stringResource(R.string.copy_logs))
                        }
                        
                        TextButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, errorLogs)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Text(stringResource(R.string.share))
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            uriHandler.openUri("https://github.com/JoshAtticus/Compressor/issues")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.open_issue_tracker))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.compression_failed_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val errorText = state.error ?: stringResource(R.string.compression_larger_error)
            
            Text(
                errorText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            if (state.error == null) {
                Spacer(modifier = Modifier.height(8.dp))
                 Text(
                    "${state.formattedOriginalSize} → ${state.formattedCompressedSize}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp).scaleOnPress(onBack)
            ) {
                Text(stringResource(R.string.try_again))
            }
            
            if (state.error == null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = onSaveAnyway,
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(
                        stringResource(R.string.save_anyway), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = { showReportDialog = true }
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.report_error))
            }
        }
    }
}

@Composable
fun ResultScreen(
    state: CompressorUiState, 
    onShare: () -> Unit,
    onSave: () -> Unit,
    onCompressAnother: () -> Unit,
    onBack: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.6f)) + fadeIn()
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.compression_complete),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "${state.formattedOriginalSize} → ${state.formattedCompressedSize}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        val reduction = if (state.originalSize > 0) ((state.originalSize - state.compressedSize).toFloat() / state.originalSize * 100).toInt() else 0
        if (reduction > 0) {
            Text(
                "(-$reduction%)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f).height(56.dp).scaleOnPress(onShare)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.share))
            }
            
            FilledTonalButton(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(56.dp).scaleOnPress(onSave),
                enabled = !state.saveSuccess
            ) {
                if (state.saveSuccess) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.saved))
                } else {
                    Text(stringResource(R.string.save_to_photos))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onCompressAnother) {
            Text(stringResource(R.string.compress_another_video))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(
            onClick = { uriHandler.openUri("https://buymeacoffee.com/joshatticus") }
        ) {
             Text(
                stringResource(R.string.buy_coffee),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
    }
}

@Composable
fun InfoDialog(
    state: CompressorUiState,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onToggleShowBitrate: () -> Unit,
    onToggleBitrateUnit: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                 Text(stringResource(R.string.info_title), style = MaterialTheme.typography.titleLarge)
                 Text("Compressor v${state.appInfoVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("Device", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                InfoRow("Android", android.os.Build.VERSION.RELEASE)
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.show_bitrate), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.showBitrate, 
                        onCheckedChange = { onToggleShowBitrate() }
                    )
                }
                
                AnimatedVisibility(visible = state.showBitrate) {
                     Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.bitrate_unit_mbps), 
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Switch(
                            checked = state.useMbps, 
                            onCheckedChange = { onToggleBitrateUnit() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                InfoRow("Supported Codecs", "")
                state.supportedCodecs.forEach { codec ->
                     Text(
                        "• ${codec.substringAfter("/")}", 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onShare) {
                    Text(stringResource(R.string.share))
                }
                TextButton(
                    onClick = { 
                        onCopy()
                        copied = true 
                    }
                ) {
                    Text(if (copied) stringResource(R.string.info_copied) else stringResource(R.string.info_copy_clipboard))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        if (value.isNotEmpty()) {
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ConfigScreen(
    state: CompressorUiState,
    viewModel: CompressorViewModel,
    context: android.content.Context
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Presets", "Video", "Audio")
    val haptics = LocalHapticFeedback.current

    val originalMb = state.originalSize / (1024f * 1024f)
    val actualEst = maxOf(state.targetSizeMb, state.minimumSizeMb)
    val isLarger = originalMb > 0 && actualEst > (originalMb + 0.01f)
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val useSplitLayout = maxWidth >= 600.dp 
        
        if (useSplitLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Spacer(Modifier.weight(1f))
                    NavigationRailItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Presets") }
                    )
                    Spacer(Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        label = { Text("Video") }
                    )
                    Spacer(Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text("Audio") }
                    )
                    Spacer(Modifier.weight(1f))
                }
                
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(top = 24.dp, bottom = 12.dp)
                        ) {
                            InfoCard(state)
                        }
                        
                        Box(modifier = Modifier.weight(1f)) {
                             HorizontalPager(
                                 state = pagerState,
                                 modifier = Modifier.fillMaxSize(),
                                 userScrollEnabled = false
                             ) { index ->
                                 when (index) {
                                     0 -> PresetsTab(state, viewModel)
                                     1 -> VideoOptionsTab(state, viewModel)
                                     2 -> AudioOptionsTab(state, viewModel)
                                 }
                             }
                        }
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                    startY = 0f,
                                    endY = 100f
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background.copy(alpha=0.9f))
                                .padding(24.dp)
                        ) {
                             val interactionSource = remember { MutableInteractionSource() }
                             Button(
                                onClick = { 
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.startCompression(context) 
                                },
                                enabled = !isLarger,
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .expressiveScale(interactionSource),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(stringResource(R.string.start_compression), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                 modifier = Modifier.fillMaxSize(),
                 contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 12.dp)
                    ) {
                        InfoCard(state)
                    }
                    
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(title) }
                            )
                        }
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                         HorizontalPager(
                             state = pagerState,
                             modifier = Modifier.fillMaxSize()
                         ) { index ->
                             when (index) {
                                 0 -> PresetsTab(state, viewModel)
                                 1 -> VideoOptionsTab(state, viewModel)
                                 2 -> AudioOptionsTab(state, viewModel)
                             }
                         }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                startY = 0f,
                                endY = 100f
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha=0.9f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                         val interactionSource = remember { MutableInteractionSource() }
                         Button(
                            onClick = { 
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startCompression(context) 
                            },
                            enabled = !isLarger,
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .fillMaxWidth()
                                .height(56.dp)
                                .expressiveScale(interactionSource),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.start_compression), fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(state: CompressorUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.original),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    state.formattedOriginalSize,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${state.originalWidth}x${state.originalHeight} • ${state.originalFps.toInt()}fps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (state.showBitrate) {
                    Text(
                        state.formattedOriginalBitrate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                 Text(
                    stringResource(R.string.estimated),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                AnimatedContent(
                    targetState = state.estimatedSize,
                    transitionSpec = {
                         slideInVertically { it / 2 } + fadeIn() togetherWith slideOutVertically { -it / 2 } + fadeOut()
                    },
                    label = "EstimateAnimation"
                ) { text ->
                    Text(
                        text,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val originalMb = state.originalSize / (1024f * 1024f)
                val actualEst = maxOf(state.targetSizeMb, state.minimumSizeMb)
                val pct = if (originalMb > 0) (1f - (actualEst / originalMb)) * 100f else 0f
                val pctInt = pct.toInt()

                val targetRes = if (state.targetResolutionHeight > 0) state.targetResolutionHeight else state.originalHeight
                val targetW = if (state.originalHeight > 0) (state.originalWidth.toFloat() / state.originalHeight * targetRes).toInt() else 0
                val targetFps = if (state.targetFps > 0) state.targetFps else state.originalFps.toInt()
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${targetW}x${targetRes} • ${targetFps}fps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.showBitrate) {
                        Text(
                            state.formattedBitrate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }

                    if (originalMb > 0) {
                         if (state.showBitrate) {
                             Text(
                                 " • ", 
                                 style = MaterialTheme.typography.labelSmall, 
                                 color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                             )
                         }
                         
                         val color = if (pctInt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                         val text = if (pctInt > 0) "-$pctInt%" else "+${-pctInt}%"
                         
                         Text(
                            text,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                         )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetsTab(state: CompressorUiState, viewModel: CompressorViewModel) {
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .padding(bottom = 80.dp)
    ) {
        
        Text(stringResource(R.string.quality_preset), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

        val presets = listOf(
            Triple(QualityPreset.HIGH, stringResource(R.string.preset_high), stringResource(R.string.preset_high_desc)),
            Triple(QualityPreset.MEDIUM, stringResource(R.string.preset_medium), stringResource(R.string.preset_medium_desc)),
            Triple(QualityPreset.LOW, stringResource(R.string.preset_low), stringResource(R.string.preset_low_desc))
        )
        
        presets.forEach { (preset, title, sub) ->
            val selected = state.activePreset == preset
            val isEnabled = when(preset) {
                QualityPreset.MEDIUM -> state.originalHeight >= 1080 
                QualityPreset.LOW -> state.originalHeight >= 720
                else -> true
            }

            val selectionScale by animateFloatAsState(if (selected) 1.02f else 1f, animationSpec = ExpressiveSpatialSpring)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(if (isPressed) 0.96f else 1f, animationSpec = ExpressiveSpatialSpring)
            
            OutlinedCard(
                onClick = { 
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.applyPreset(preset) 
                },
                enabled = isEnabled,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .graphicsLayer { 
                        scaleX = selectionScale * pressScale
                        scaleY = selectionScale * pressScale
                    },
                colors = if (selected) {
                    CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    CardDefaults.outlinedCardColors(
                        containerColor = if (isEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha=0.3f),
                        contentColor = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f)
                    )
                },
                border = if (selected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, if (isEnabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.38f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Medium,
                            color = if (isEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha=0.38f)
                        )
                        Text(
                            sub, 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.38f)
                        )
                    }
                    if (selected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        val sizePresets = listOf(
            10f to stringResource(R.string.size_discord),
            25f to stringResource(R.string.size_email),
            50f to stringResource(R.string.size_stories),
            100f to stringResource(R.string.size_messenger),
            500f to stringResource(R.string.size_nitro),
            512f to stringResource(R.string.size_twitter),
            2048f to stringResource(R.string.size_whatsapp),
            4096f to stringResource(R.string.size_tg_premium),
            8192f to stringResource(R.string.size_x_premium)
        ).filter { (size, _) -> 
            size < (state.originalSize.toFloat() / (1024f * 1024f))
        }

        if (sizePresets.isNotEmpty()) {
            Text(stringResource(R.string.target_size_limits), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizePresets.forEach { (size, label) ->
                    val interactionSource = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = state.targetSizeMb == size,
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setTargetSize(size) 
                        },
                        interactionSource = interactionSource,
                        label = { 
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val unitGb = stringResource(R.string.unit_gb)
                                val unitMb = stringResource(R.string.unit_mb)
                                Text(
                                    if (size >= 1024) "${(size/1024).toInt()} $unitGb" else "${size.toInt()} $unitMb", 
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .expressiveScale(interactionSource)
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun VideoOptionsTab(state: CompressorUiState, viewModel: CompressorViewModel) {
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .padding(bottom = 80.dp)
    ) {
            Text(stringResource(R.string.advanced_options), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

            var sliderValue by remember { mutableFloatStateOf(state.targetSizeMb) }
            var isUserInteracting by remember { mutableStateOf(false) }
            
            LaunchedEffect(state.targetSizeMb) {
                if (!isUserInteracting) {
                    sliderValue = state.targetSizeMb
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.target_size), style = MaterialTheme.typography.labelLarge)
                Text(
                    String.format("%.1f MB", sliderValue), 
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
             Slider(
                value = sliderValue,
                onValueChange = { 
                    isUserInteracting = true
                    sliderValue = it
                    viewModel.setTargetSize(it)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onValueChangeFinished = {
                    isUserInteracting = false
                },
                valueRange = 1f..maxOf(10f, state.targetSizeMb, (state.originalSize / (1024f*1024f))),
                steps = 0
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.slider_less_space), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.slider_balanced), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.slider_high_quality), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.encoding), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val supported = state.supportedCodecs
                
                if (supported.contains(androidx.media3.common.MimeTypes.VIDEO_AV1)) {
                    val interactionSource = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = state.videoCodec == androidx.media3.common.MimeTypes.VIDEO_AV1,
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVideoCodec(androidx.media3.common.MimeTypes.VIDEO_AV1) 
                        },
                        interactionSource = interactionSource,
                        label = { Text(stringResource(R.string.av1_high_efficiency)) },
                        modifier = Modifier.expressiveScale(interactionSource)
                    )
                }
                if (supported.contains(androidx.media3.common.MimeTypes.VIDEO_H265)) {
                    val interactionSource = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = state.videoCodec == androidx.media3.common.MimeTypes.VIDEO_H265,
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVideoCodec(androidx.media3.common.MimeTypes.VIDEO_H265) 
                        },
                        interactionSource = interactionSource,
                        label = { Text(stringResource(R.string.h265_efficient)) },
                        modifier = Modifier.expressiveScale(interactionSource)
                    )
                }
                val interactionSource = remember { MutableInteractionSource() }
                FilterChip(
                    selected = state.videoCodec == androidx.media3.common.MimeTypes.VIDEO_H264,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setVideoCodec(androidx.media3.common.MimeTypes.VIDEO_H264) 
                    },
                    interactionSource = interactionSource,
                    label = { Text(stringResource(R.string.h264_compat)) },
                    modifier = Modifier.expressiveScale(interactionSource)
                )
            }
             
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.resolution), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                
                val res4320 = stringResource(R.string.res_8k)
                val res2160 = stringResource(R.string.res_4k)
                val res1440 = stringResource(R.string.res_2k)
                val res1080 = stringResource(R.string.res_1080p)
                val res720 = stringResource(R.string.res_720p)
                val res540 = stringResource(R.string.res_540p)
                val res480 = stringResource(R.string.res_480p)

                val resThreeQuarters = stringResource(R.string.res_three_quarters)
                val resHalf = stringResource(R.string.res_half)
                val resQuarter = stringResource(R.string.res_quarter)

                val allRes = listOf(4320 to res4320, 2160 to res2160, 1440 to res1440, 1080 to res1080, 720 to res720, 540 to res540, 480 to res480)
                
                val options = remember(state.originalHeight) {
                    val standard = allRes.filter { it.first <= state.originalHeight }
                    val fractions = listOf(
                        (state.originalHeight * 0.75).toInt() to resThreeQuarters,
                        (state.originalHeight * 0.5).toInt() to resHalf,
                        (state.originalHeight * 0.25).toInt() to resQuarter
                    )
                    (standard + fractions)
                        .filter { it.first > 0 }
                        .sortedByDescending { it.first }
                }

                androidx.compose.foundation.layout.Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                     val interactionSource = remember { MutableInteractionSource() }
                     FilterChip(
                        selected = state.targetResolutionHeight == state.originalHeight || state.targetResolutionHeight == 0, 
                        onClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setResolution(state.originalHeight) 
                        }, 
                        interactionSource = interactionSource,
                        label = { Text(stringResource(R.string.original) + " • ${state.originalHeight}p") },
                        modifier = Modifier.padding(end = 8.dp).expressiveScale(interactionSource)
                    )
                    options.forEach { (res, label) ->
                         val itemInteractionSource = remember { MutableInteractionSource() }
                         FilterChip(
                            selected = state.targetResolutionHeight == res, 
                            onClick = { 
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setResolution(res) 
                            }, 
                            interactionSource = itemInteractionSource,
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 8.dp).expressiveScale(itemInteractionSource)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(R.string.framerate), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                 val iSource1 = remember { MutableInteractionSource() }
                 FilterChip(
                    selected = state.targetFps == 0,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFps(0) 
                    },
                    interactionSource = iSource1,
                    label = { Text(stringResource(R.string.original) + " • ${state.originalFps.toInt()}") },
                    modifier = Modifier.expressiveScale(iSource1)
                )
                val iSource2 = remember { MutableInteractionSource() }
                FilterChip(
                    selected = state.targetFps == 60,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFps(60) 
                    },
                    interactionSource = iSource2,
                    label = { Text(stringResource(R.string.fps_60)) },
                    enabled = state.originalFps >= 50f,
                    modifier = Modifier.expressiveScale(iSource2)
                )
                val iSource3 = remember { MutableInteractionSource() }
                FilterChip(
                    selected = state.targetFps == 30,
                    onClick = { 
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setFps(30) 
                    },
                    interactionSource = iSource3,
                    label = { Text(stringResource(R.string.fps_30)) },
                    modifier = Modifier.expressiveScale(iSource3)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AudioOptionsTab(state: CompressorUiState, viewModel: CompressorViewModel) {
    val scrollState = rememberScrollState()
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .padding(bottom = 80.dp)
    ) {
         Text(stringResource(R.string.audio_options), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
         
         Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { 
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleRemoveAudio() 
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.remove_audio), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = state.removeAudio,
                onCheckedChange = { 
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleRemoveAudio() 
                }
            )
        }

        AnimatedVisibility(visible = !state.removeAudio) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                 Text(stringResource(R.string.audio_bitrate), style = MaterialTheme.typography.labelLarge)
                 
                 Row(
                     modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                     horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                     val bitrates = listOf(0, 320000, 256000, 192000, 160000, 128000, 96000, 64000)
                     bitrates.forEach { rate ->
                         if (rate == 0 || (state.originalAudioBitrate > 0 && rate <= state.originalAudioBitrate)) {
                             val effectiveSelectedBitrate = if (state.audioBitrate == 0) state.originalAudioBitrate else state.audioBitrate
                             val chipRepresentsBitrate = if (rate == 0) state.originalAudioBitrate else rate
                             
                             val isSelected = effectiveSelectedBitrate == chipRepresentsBitrate
                             
                             val iSource = remember { MutableInteractionSource() }
                             FilterChip(
                                 selected = isSelected,
                                 onClick = { 
                                     haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                     viewModel.setAudioBitrate(rate) 
                                 },
                                 interactionSource = iSource,
                                 label = { 
                                     if (rate == 0) {
                                         Text(stringResource(R.string.original) + " • ${state.originalAudioBitrate / 1000}k")
                                     } else {
                                         Text("${rate / 1000}k") 
                                     }
                                 },
                                 modifier = Modifier.expressiveScale(iSource)
                             )
                         }
                     }
                 }

                 Spacer(modifier = Modifier.height(24.dp))

                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(stringResource(R.string.volume), style = MaterialTheme.typography.labelLarge)
                     Text(
                         "${(state.audioVolume * 100).toInt()}%", 
                         style = MaterialTheme.typography.labelMedium,
                         color = MaterialTheme.colorScheme.primary
                     )
                 }
                 
                 var sliderPosition by remember(state.audioVolume) { mutableFloatStateOf(state.audioVolume) }
                 
                 Slider(
                    value = sliderPosition,
                    onValueChange = { 
                        sliderPosition = it
                        viewModel.setAudioVolume(it)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = 0f..2f,
                    steps = 19
                )
            }
        }
    }
}

@Composable
fun CompressingScreen(
    state: CompressorUiState,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Load Thumbnail
    LaunchedEffect(state.selectedUri) {
        if (state.selectedUri == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, state.selectedUri)
                val bitmap = retriever.getFrameAtTime(0)
                thumbnail = bitmap?.asImageBitmap()
                try { retriever.release() } catch(e:Exception){}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val formattedSize = state.formattedCurrentOutputSize

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray)
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        formattedSize.forEach { char ->
                            AnimatedContent(
                                targetState = char,
                                transitionSpec = {
                                    if (char.isDigit()) {
                                        slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                                    } else {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                },
                                label = "CharAnimation"
                            ) { targetChar ->
                                Text(
                                    text = targetChar.toString(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        stringResource(R.string.compressing_video_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = state.progress,
                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        label = "ProgressAnimation"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    stringResource(R.string.cancel),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}


// Motion System - M3 Expressive - Got it to work for 1.4.0, yippee!
// Spatial (Movements, Transforms)
val ExpressiveSpatialSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = 350f
)

fun Modifier.scaleOnPress(
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveSpatialSpring,
        label = "scale"
    )
    val haptics = LocalHapticFeedback.current
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

fun Modifier.expressiveScale(interactionSource: androidx.compose.foundation.interaction.InteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveSpatialSpring,
        label = "scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
