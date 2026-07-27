package compress.joshattic.us.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.ui.components.InfoCard
import compress.joshattic.us.ui.tabs.AudioOptionsTab
import compress.joshattic.us.ui.tabs.PresetsTab
import compress.joshattic.us.ui.tabs.VideoOptionsTab
import compress.joshattic.us.utils.expressiveScale
import compress.joshattic.us.viewmodel.CompressorViewModel
import kotlinx.coroutines.launch

@Composable
fun ConfigScreen(
    state: CompressorUiState,
    viewModel: CompressorViewModel,
    context: Context
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabs = listOf(stringResource(R.string.tab_presets), stringResource(R.string.tab_video), stringResource(R.string.tab_audio))
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
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_presets), fontWeight = FontWeight.Bold) }
                    )
                    Spacer(Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_video), fontWeight = FontWeight.Bold) }
                    )
                    Spacer(Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_audio), fontWeight = FontWeight.Bold) }
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
                                Brush.verticalGradient(
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
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(stringResource(R.string.start_compression), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val selected = pagerState.currentPage == index
                                val tabIcon = when (index) {
                                    0 -> Icons.Outlined.BookmarkBorder
                                    1 -> Icons.Default.Movie
                                    else -> Icons.Default.MusicNote
                                }

                                Surface(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape,
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = tabIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
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
                            Brush.verticalGradient(
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
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(stringResource(R.string.start_compression), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
