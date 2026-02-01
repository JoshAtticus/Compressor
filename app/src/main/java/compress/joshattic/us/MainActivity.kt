package compress.joshattic.us

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import compress.joshattic.us.ui.theme.CompressorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File

import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CompressorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                // Using Surface to ensure correct M3 background handling
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

    val shareVideoTitle = stringResource(R.string.share_video_title)
    // We fetch the raw template string here to use in the non-composable callback callback below
    // Note: R.string.share_error is expected to have a format placeholder (e.g. %s)
    val shareErrorTemplate = stringResource(R.string.share_error)
    val deleteSuccessMsg = stringResource(R.string.delete_original_success)
    val deleteFailedMsg = stringResource(R.string.delete_original_failed)
    
    // Keep the screen on when compressing
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle back button/gesture
    BackHandler(enabled = state.selectedUri != null) {
        if (state.isCompressing) {
            viewModel.cancelCompression()
        } else {
            viewModel.reset()
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
                            state.compressedUri != null -> 2
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
                            2 -> ResultScreen(
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
fun ResultScreen(
    state: CompressorUiState, 
    onShare: () -> Unit,
    onSave: () -> Unit,
    onCompressAnother: () -> Unit,
    onBack: () -> Unit
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // Added scroll for smaller screens/landscape
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.5f, animationSpec = spring<Float>(dampingRatio = 0.6f)) + fadeIn()
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
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 80.dp) // Extra padding for the floating button
        ) {
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
                    Text(
                        state.estimatedSize,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val targetRes = if (state.targetResolutionHeight > 0) state.targetResolutionHeight else state.originalHeight
                    val targetW = if (state.originalHeight > 0) (state.originalWidth.toFloat() / state.originalHeight * targetRes).toInt() else 0
                    val targetFps = if (state.targetFps > 0) state.targetFps else state.originalFps.toInt()
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${targetW}x${targetRes} • ${targetFps}fps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )

                    if (state.showBitrate) {
                        Text(
                            state.formattedBitrate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(stringResource(R.string.quality_preset), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))

        val presets = listOf(
            Triple(QualityPreset.HIGH, stringResource(R.string.preset_high), stringResource(R.string.preset_high_desc)),
            Triple(QualityPreset.MEDIUM, stringResource(R.string.preset_medium), stringResource(R.string.preset_medium_desc)),
            Triple(QualityPreset.LOW, stringResource(R.string.preset_low), stringResource(R.string.preset_low_desc))
        ).filter { (preset, _, _) ->
            when(preset) {
                QualityPreset.MEDIUM -> state.originalHeight >= 1080 
                QualityPreset.LOW -> state.originalHeight >= 720
                else -> true
            }
        }
        
        presets.forEach { (preset, title, sub) ->
            val selected = state.activePreset == preset
            val scale by animateFloatAsState(if (selected) 1.02f else 1f, animationSpec = ExpressiveSpatialSpring)
            
            OutlinedCard(
                onClick = { viewModel.applyPreset(preset) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                colors = if (selected) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.outlinedCardColors(),
                border = if (selected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    FilterChip(
                        selected = state.targetSizeMb == size,
                        onClick = { viewModel.setTargetSize(size) },
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
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        var advancedExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { 
                    advancedExpanded = !advancedExpanded
                    if (advancedExpanded) {
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(100)
                            scrollState.animateScrollTo(
                                scrollState.value + 300,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                            )
                        }
                    }
                }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.advanced_options), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Icon(
                if (advancedExpanded) Icons.Default.Close else Icons.Default.Add, 
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = advancedExpanded,
            enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.target_size), style = MaterialTheme.typography.labelLarge)
                    Text(
                        String.format("%.1f MB", state.targetSizeMb), 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                 Slider(
                    value = state.targetSizeMb,
                    onValueChange = { viewModel.setTargetSize(it) },
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
                        FilterChip(
                            selected = state.videoCodec == androidx.media3.common.MimeTypes.VIDEO_AV1,
                            onClick = { viewModel.setVideoCodec(androidx.media3.common.MimeTypes.VIDEO_AV1) },
                            label = { Text(stringResource(R.string.av1_high_efficiency)) }
                        )
                    }
                    if (supported.contains(androidx.media3.common.MimeTypes.VIDEO_H265)) {
                        FilterChip(
                            selected = state.videoCodec == androidx.media3.common.MimeTypes.VIDEO_H265,
                            onClick = { viewModel.setVideoCodec(androidx.media3.common.MimeTypes.VIDEO_H265) },
                            label = { Text(stringResource(R.string.h265_efficient)) }
                        )
                    }
                    FilterChip(
                        selected = state.videoCodec == androidx.media3.common.MimeTypes.VIDEO_H264,
                        onClick = { viewModel.setVideoCodec(androidx.media3.common.MimeTypes.VIDEO_H264) },
                        label = { Text(stringResource(R.string.h264_compat)) }
                    )
                }
                 
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.resolution), style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Predefined resolution labels
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
                        // Standard resolutions take precedence if values are equal
                        (standard + fractions)
                            .filter { it.first > 0 }
                            .sortedByDescending { it.first }
                    }

                    androidx.compose.foundation.layout.Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                         FilterChip(
                            selected = state.targetResolutionHeight == state.originalHeight || state.targetResolutionHeight == 0, 
                            onClick = { viewModel.setResolution(state.originalHeight) }, 
                            label = { Text(stringResource(R.string.original) + " • ${state.originalHeight}p") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        options.forEach { (res, label) ->
                             FilterChip(
                                selected = state.targetResolutionHeight == res, 
                                onClick = { viewModel.setResolution(res) }, 
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.framerate), style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     FilterChip(
                        selected = state.targetFps == 0,
                        onClick = { viewModel.setFps(0) },
                        label = { Text(stringResource(R.string.original) + " • ${state.originalFps.toInt()}") }
                    )
                    FilterChip(
                        selected = state.targetFps == 60,
                        onClick = { viewModel.setFps(60) },
                        label = { Text(stringResource(R.string.fps_60)) },
                        enabled = state.originalFps >= 50f
                    )
                    FilterChip(
                        selected = state.targetFps == 30,
                        onClick = { viewModel.setFps(30) },
                        label = { Text(stringResource(R.string.fps_30)) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(stringResource(R.string.audio_options), style = MaterialTheme.typography.labelLarge)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { viewModel.toggleRemoveAudio() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.remove_audio), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.removeAudio,
                        onCheckedChange = { viewModel.toggleRemoveAudio() }
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
                                     val isSelected = state.audioBitrate == rate
                                     val isOriginalSelected = (state.audioBitrate == 0) && (rate == state.originalAudioBitrate)
                                     
                                     FilterChip(
                                         selected = isSelected || isOriginalSelected,
                                         onClick = { viewModel.setAudioBitrate(rate) },
                                         label = { 
                                             if (rate == 0) {
                                                 Text(stringResource(R.string.original) + " • ${state.originalAudioBitrate / 1000}k")
                                             } else {
                                                 Text("${rate / 1000}k") 
                                             }
                                         }
                                     )
                                 }
                             }
                         }
                    }
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
                    .padding(24.dp)
            ) {
                 Button(
                    onClick = { viewModel.startCompression(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scaleOnPress { viewModel.startCompression(context) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.start_compression), fontSize = 16.sp)
                }
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
                    Text(
                        formattedSize,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
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
                    LinearProgressIndicator(
                        progress = { state.progress },
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
// Effects (Color, Alpha)
val ExpressiveEffectsSpring = spring<Float>(
    dampingRatio = 1f,
    stiffness = 300f
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
