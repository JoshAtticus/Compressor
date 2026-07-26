package compress.joshattic.us.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compress.joshattic.us.R
import compress.joshattic.us.model.CompressorUiState
import compress.joshattic.us.utils.scaleOnPress

@Composable
fun ResultScreen(
    state: CompressorUiState, 
    onShare: () -> Unit,
    onSave: () -> Unit,
    onCompressAnother: () -> Unit,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
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

        if (state.warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            var showWarningDialog by remember { mutableStateOf(false) }
            
            OutlinedButton(
                onClick = { showWarningDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                 Icon(Icons.Outlined.Warning, contentDescription = null)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("${state.warnings.size} Warning${if (state.warnings.size > 1) "s" else ""} - Tap for Details")
            }
            
            if (showWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showWarningDialog = false },
                    icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text(stringResource(R.string.warning_details)) },
                    text = {
                        Column {
                            state.warnings.forEach { warning ->
                                Text("• $warning", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showWarningDialog = false }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                )
            }
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
