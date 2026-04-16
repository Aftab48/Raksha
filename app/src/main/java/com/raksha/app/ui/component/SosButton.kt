package com.raksha.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.raksha.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * SOS button — 80dp circle with pulsing red rings.
 * Requires 1.5s long press to trigger (prevents accidental activation).
 */
@Composable
fun SosButton(
    onSosTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableStateOf(0f) }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_alpha"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_alpha"
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(120.dp)
    ) {
        // Pulse rings (always visible for effect, more prominent when active)
        PulseRing(scale = ring1Scale, alpha = ring1Alpha * if (isActive) 1f else 0.4f)
        PulseRing(scale = ring2Scale, alpha = ring2Alpha * if (isActive) 1f else 0.4f)
        PulseRing(scale = ring3Scale, alpha = ring3Alpha * if (isActive) 1f else 0.4f)

        // Main button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isPressed) ColorDangerDim else ColorDanger)
                .semantics { contentDescription = "SOS emergency button, long press to activate" }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            pressProgress = 0f
                            val startTime = System.currentTimeMillis()
                            val success = tryAwaitRelease()
                            val elapsed = System.currentTimeMillis() - startTime
                            isPressed = false
                            if (elapsed >= 1500L) {
                                onSosTriggered()
                            }
                        }
                    )
                }
        ) {
            Text(
                text = "SOS",
                style = RakshaTextStyle.sos,
                color = ColorTextPrimary
            )
        }
    }
}

@Composable
private fun PulseRing(scale: Float, alpha: Float) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(ColorDangerPulse.copy(alpha = alpha))
    )
}
