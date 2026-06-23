package dev.openhub.app.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

val BorderGradient = Brush.verticalGradient(
    0.0f to LiquidGlassBorderStart,
    0.2f to LiquidGlassBorderEnd,
    0.4f to Color.Transparent,
    0.6f to Color.Transparent,
    0.8f to LiquidGlassBorderEnd,
    1.0f to LiquidGlassBorderStart
)

val BorderGradientStrong = Brush.verticalGradient(
    0.0f to Color.White.copy(alpha = 0.5f),
    0.2f to Color.White.copy(alpha = 0.2f),
    0.4f to Color.Transparent,
    0.6f to Color.Transparent,
    0.8f to Color.White.copy(alpha = 0.2f),
    1.0f to Color.White.copy(alpha = 0.5f)
)

@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(percent = 50)
): Modifier = this
    .clip(shape)
    .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = shape, blurRadius = 4.dp) else Modifier)
    .background(LiquidGlassBackground)
    .border(1.dp, BorderGradient, shape)

@Composable
fun Modifier.liquidGlassStrong(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(percent = 50)
): Modifier = this
    .shadow(4.dp, shape, spotColor = LiquidGlassStrongShadow, ambientColor = LiquidGlassStrongShadow)
    .clip(shape)
    .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = shape, blurRadius = 50.dp) else Modifier)
    .background(LiquidGlassStrongBackground)
    .border(1.dp, BorderGradientStrong, shape)

@Composable
fun Modifier.spatialClickable(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
        label = "scale"
    )

    if (isPressed) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
