package compress.joshattic.us.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Motion System - M3 Expressive
val ExpressiveSpatialSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = 350f
)

fun Modifier.scaleOnPress(
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
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

fun Modifier.expressiveScale(interactionSource: InteractionSource): Modifier = composed {
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
