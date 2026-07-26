package compress.joshattic.us.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.ui.theme.A16BadgeAppGreen
import compress.joshattic.us.ui.theme.A16BadgeAppGreenOnBadge
import compress.joshattic.us.ui.theme.getCategoryBadgeColors
import compress.joshattic.us.utils.scaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: CompressorUiState,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDisplay: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToVideo: () -> Unit,
    onNavigateToAudio: () -> Unit
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var searchQuery by remember { mutableStateOf("") }

    data class SearchableSetting(
        val title: String,
        val description: String,
        val categoryName: String,
        val categoryId: String,
        val icon: ImageVector,
        val onClick: () -> Unit
    )

    val displayTitle = stringResource(R.string.display_settings_title)
    val presetsTitle = stringResource(R.string.tab_presets)
    val videoTitle = stringResource(R.string.tab_video)
    val audioTitle = stringResource(R.string.tab_audio)
    val aboutTitle = stringResource(R.string.about_compressor_title)

    val allSearchableSettings = listOf(
        // Categories
        SearchableSetting(
            title = displayTitle,
            description = stringResource(R.string.category_display_subtitle),
            categoryName = displayTitle,
            categoryId = "display",
            icon = Icons.Default.Tune,
            onClick = onNavigateToDisplay
        ),
        SearchableSetting(
            title = presetsTitle,
            description = stringResource(R.string.category_presets_subtitle),
            categoryName = presetsTitle,
            categoryId = "presets",
            icon = Icons.Outlined.BookmarkBorder,
            onClick = onNavigateToPresets
        ),
        SearchableSetting(
            title = videoTitle,
            description = stringResource(R.string.category_video_subtitle),
            categoryName = videoTitle,
            categoryId = "video",
            icon = Icons.Default.Movie,
            onClick = onNavigateToVideo
        ),
        SearchableSetting(
            title = audioTitle,
            description = stringResource(R.string.category_audio_subtitle),
            categoryName = audioTitle,
            categoryId = "audio",
            icon = Icons.Default.MusicNote,
            onClick = onNavigateToAudio
        ),
        SearchableSetting(
            title = aboutTitle,
            description = stringResource(R.string.version_format, state.appInfoVersion),
            categoryName = aboutTitle,
            categoryId = "about",
            icon = Icons.Default.Info,
            onClick = onNavigateToAbout
        ),

        // Individual Display Settings
        SearchableSetting(
            title = stringResource(R.string.show_bitrate),
            description = stringResource(R.string.show_bitrate_subtitle),
            categoryName = displayTitle,
            categoryId = "display",
            icon = Icons.Default.Tune,
            onClick = onNavigateToDisplay
        ),
        SearchableSetting(
            title = stringResource(R.string.bitrate_unit_mbps),
            description = stringResource(R.string.bitrate_unit_mbps_subtitle) + " / " + stringResource(R.string.bitrate_unit_kbps_subtitle),
            categoryName = displayTitle,
            categoryId = "display",
            icon = Icons.Default.Tune,
            onClick = onNavigateToDisplay
        ),
        SearchableSetting(
            title = stringResource(R.string.show_storage_saved_title),
            description = stringResource(R.string.show_storage_saved_subtitle),
            categoryName = displayTitle,
            categoryId = "display",
            icon = Icons.Default.Tune,
            onClick = onNavigateToDisplay
        ),
        SearchableSetting(
            title = stringResource(R.string.show_target_size_preset_title),
            description = stringResource(R.string.show_target_size_preset_subtitle),
            categoryName = displayTitle,
            categoryId = "display",
            icon = Icons.Default.Tune,
            onClick = onNavigateToDisplay
        ),

        // Individual About Settings
        SearchableSetting(
            title = stringResource(R.string.info_supported_codecs),
            description = stringResource(R.string.codecs_supported_count, state.supportedCodecs.size),
            categoryName = aboutTitle,
            categoryId = "about",
            icon = Icons.Default.Info,
            onClick = onNavigateToAbout
        ),
        SearchableSetting(
            title = stringResource(R.string.enable_all_codecs),
            description = stringResource(R.string.header_hardware_codecs),
            categoryName = aboutTitle,
            categoryId = "about",
            icon = Icons.Default.Info,
            onClick = onNavigateToAbout
        ),
        SearchableSetting(
            title = stringResource(R.string.view_on_github),
            description = "https://github.com/JoshAtticus/Compressor",
            categoryName = aboutTitle,
            categoryId = "about",
            icon = Icons.Default.Build,
            onClick = onNavigateToAbout
        ),
        SearchableSetting(
            title = stringResource(R.string.info_copy_clipboard),
            description = stringResource(R.string.header_links_actions),
            categoryName = aboutTitle,
            categoryId = "about",
            icon = Icons.Default.Info,
            onClick = onNavigateToAbout
        ),

        // Individual Video Settings
        SearchableSetting(
            title = stringResource(R.string.encoding),
            description = "AV1, H.265 (HEVC), H.264 (AVC)",
            categoryName = videoTitle,
            categoryId = "video",
            icon = Icons.Default.Movie,
            onClick = onNavigateToVideo
        ),
        SearchableSetting(
            title = stringResource(R.string.target_size),
            description = stringResource(R.string.advanced_options),
            categoryName = videoTitle,
            categoryId = "video",
            icon = Icons.Default.Movie,
            onClick = onNavigateToVideo
        ),
        SearchableSetting(
            title = stringResource(R.string.resolution),
            description = "8K, 4K, 1080p, 720p, 480p",
            categoryName = videoTitle,
            categoryId = "video",
            icon = Icons.Default.Movie,
            onClick = onNavigateToVideo
        ),
        SearchableSetting(
            title = stringResource(R.string.framerate),
            description = "Original, 60fps, 30fps",
            categoryName = videoTitle,
            categoryId = "video",
            icon = Icons.Default.Movie,
            onClick = onNavigateToVideo
        ),

        // Individual Audio Settings
        SearchableSetting(
            title = stringResource(R.string.audio_bitrate),
            description = "320k, 256k, 192k, 128k, 96k, 64k",
            categoryName = audioTitle,
            categoryId = "audio",
            icon = Icons.Default.MusicNote,
            onClick = onNavigateToAudio
        ),
        SearchableSetting(
            title = stringResource(R.string.volume),
            description = stringResource(R.string.audio_options),
            categoryName = audioTitle,
            categoryId = "audio",
            icon = Icons.Default.MusicNote,
            onClick = onNavigateToAudio
        ),
        SearchableSetting(
            title = stringResource(R.string.remove_audio),
            description = stringResource(R.string.audio_options),
            categoryName = audioTitle,
            categoryId = "audio",
            icon = Icons.Default.MusicNote,
            onClick = onNavigateToAudio
        ),

        // Individual Presets Settings
        SearchableSetting(
            title = stringResource(R.string.quality_preset),
            description = stringResource(R.string.preset_high) + " • " + stringResource(R.string.preset_medium) + " • " + stringResource(R.string.preset_low),
            categoryName = presetsTitle,
            categoryId = "presets",
            icon = Icons.Outlined.BookmarkBorder,
            onClick = onNavigateToPresets
        ),
        SearchableSetting(
            title = stringResource(R.string.target_size_limits),
            description = "Discord (10MB), Email (25MB), Stories (50MB), Twitter/X",
            categoryName = presetsTitle,
            categoryId = "presets",
            icon = Icons.Outlined.BookmarkBorder,
            onClick = onNavigateToPresets
        )
    )

    val searchResults = remember(searchQuery, allSearchableSettings) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allSearchableSettings.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q)
            }.distinctBy { it.title + it.categoryName }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp)) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onBack()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search bar — pill capsule matching Android 16
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.search_settings_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_icon_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_search_desc),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor      = Color.Transparent,
                    unfocusedBorderColor    = Color.Transparent,
                )
            )

            if (searchQuery.isNotBlank()) {
                // Search Results View
                if (searchResults.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column {
                            searchResults.forEachIndexed { index, setting ->
                                val (badgeBg, badgeIcon) = getCategoryBadgeColors(setting.categoryId)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { setting.onClick() }
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(badgeBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = setting.icon,
                                            contentDescription = null,
                                            tint = badgeIcon,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = setting.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${setting.categoryName} • ${setting.description}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (index < searchResults.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_settings_found),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Default View (App Card Header + Main 4 Categories)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .scaleOnPress(onNavigateToAbout),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(A16BadgeAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                tint = A16BadgeAppGreenOnBadge,
                                modifier = Modifier.size(96.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.title_compressor),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.version_format, state.appInfoVersion),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val categories = listOf(
                    SearchableSetting(
                        title = displayTitle,
                        description = stringResource(R.string.category_display_subtitle),
                        categoryName = displayTitle,
                        categoryId = "display",
                        icon = Icons.Default.Tune,
                        onClick = onNavigateToDisplay
                    ),
                    SearchableSetting(
                        title = presetsTitle,
                        description = stringResource(R.string.category_presets_subtitle),
                        categoryName = presetsTitle,
                        categoryId = "presets",
                        icon = Icons.Outlined.BookmarkBorder,
                        onClick = onNavigateToPresets
                    ),
                    SearchableSetting(
                        title = videoTitle,
                        description = stringResource(R.string.category_video_subtitle),
                        categoryName = videoTitle,
                        categoryId = "video",
                        icon = Icons.Default.Movie,
                        onClick = onNavigateToVideo
                    ),
                    SearchableSetting(
                        title = audioTitle,
                        description = stringResource(R.string.category_audio_subtitle),
                        categoryName = audioTitle,
                        categoryId = "audio",
                        icon = Icons.Default.MusicNote,
                        onClick = onNavigateToAudio
                    )
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column {
                        categories.forEachIndexed { index, category ->
                            val (badgeBg, badgeIcon) = getCategoryBadgeColors(category.categoryId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        category.onClick() 
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = badgeIcon,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = category.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (index < categories.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 80.dp, end = 20.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
