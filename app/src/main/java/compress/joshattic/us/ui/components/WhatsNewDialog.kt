package compress.joshattic.us.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compress.joshattic.us.R
import compress.joshattic.us.utils.hasVideoPermission

@Composable
fun WhatsNewDialog(
    versionName: String,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val canRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasVideoPermission(context)
    var currentPage by remember { mutableIntStateOf(0) }

    BackHandler(enabled = currentPage > 0) {
        currentPage = 0
    }

    Dialog(
        onDismissRequest = {
            if (currentPage > 0) {
                currentPage = 0
            } else {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { w -> w } + fadeIn() togetherWith slideOutHorizontally { w -> -w } + fadeOut()
                    } else {
                        slideInHorizontally { w -> -w } + fadeIn() togetherWith slideOutHorizontally { w -> w } + fadeOut()
                    }
                },
                label = "WhatsNewPages"
            ) { page ->
                when (page) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF352B4E) else Color(0xFFF3E5F5),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFD0BCFF) else Color(0xFF7E57C2),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.whats_new_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.whats_new_version, versionName),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    WhatsNewItem(
                                        icon = Icons.Default.Translate,
                                        bgColor = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE3F2FD),
                                        iconTint = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                        title = stringResource(R.string.whats_new_1_title),
                                        description = stringResource(R.string.whats_new_1_desc),
                                        onClick = { uriHandler.openUri("https://l.joshattic.us/rHkokL") }
                                    )

                                    WhatsNewItem(
                                        icon = Icons.Default.Movie,
                                        bgColor = if (isDark) Color(0xFF1B4D2E) else Color(0xFFE8F5E9),
                                        iconTint = if (isDark) Color(0xFFA5D6A7) else Color(0xFF388E3C),
                                        title = stringResource(R.string.whats_new_2_title),
                                        description = stringResource(R.string.whats_new_2_desc)
                                    )

                                    WhatsNewItem(
                                        icon = Icons.Default.Tune,
                                        bgColor = if (isDark) Color(0xFF4A2B00) else Color(0xFFFFF3E0),
                                        iconTint = if (isDark) Color(0xFFFFCC80) else Color(0xFFF57C00),
                                        title = stringResource(R.string.whats_new_3_title),
                                        description = stringResource(R.string.whats_new_3_desc)
                                    )

                                    WhatsNewItem(
                                        icon = Icons.Default.Favorite,
                                        bgColor = if (isDark) Color(0xFF4A1C1D) else Color(0xFFFFEBEE),
                                        iconTint = if (isDark) Color(0xFFEF9A9A) else Color(0xFFD32F2F),
                                        title = stringResource(R.string.whats_new_4_title),
                                        description = stringResource(R.string.whats_new_4_desc)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Seamless Pinned Bottom Bar extending edge-to-edge behind the navigation bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (canRequestPermission) {
                                            currentPage = 1
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = stringResource(if (canRequestPermission) R.string.next else R.string.whats_new_button),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        currentPage = 0
                                    },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            color = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE3F2FD),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF1976D2),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.permission_screen_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.permission_screen_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Green card: Access Gallery
                                    WhatsNewItem(
                                        icon = Icons.Default.Movie,
                                        bgColor = if (isDark) Color(0xFF1B4D2E) else Color(0xFFE8F5E9),
                                        iconTint = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32),
                                        title = stringResource(R.string.permission_card_videos_title),
                                        description = stringResource(R.string.permission_card_videos_desc)
                                    )

                                    // Red card: Storage
                                    WhatsNewItem(
                                        icon = Icons.Default.FolderOff,
                                        bgColor = if (isDark) Color(0xFF4A1C1D) else Color(0xFFFFEBEE),
                                        iconTint = if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828),
                                        title = stringResource(R.string.permission_card_storage_title),
                                        description = stringResource(R.string.permission_card_storage_desc)
                                    )

                                    // Red card: Network
                                    WhatsNewItem(
                                        icon = Icons.Default.WifiOff,
                                        bgColor = if (isDark) Color(0xFF4A1C1D) else Color(0xFFFFEBEE),
                                        iconTint = if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828),
                                        title = stringResource(R.string.permission_card_network_title),
                                        description = stringResource(R.string.permission_card_network_desc)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = stringResource(R.string.permission_screen_footer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Action buttons
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onRequestPermission()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = stringResource(R.string.grant_permission),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = stringResource(R.string.decline_permission),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsNewItem(
    icon: ImageVector,
    bgColor: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onClick()
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color = bgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
