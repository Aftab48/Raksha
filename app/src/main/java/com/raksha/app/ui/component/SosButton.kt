package com.raksha.app.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorDangerDim
import com.raksha.app.ui.theme.ColorDangerPulse
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaTextStyle
import com.raksha.app.ui.theme.RakshaTypography
import kotlinx.coroutines.delay

/**
 * SOS button supports both:
 * - 1.5s long press for manual SOS
 * - triple tap in 1.2s for SOS + evidence streaming
 */
@Composable
fun SosButton(
    onLongHoldConfirmed: () -> Unit,
    onTripleTapDetected: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isBusy: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    val tripleTapDetector = remember { TripleTapDetector() }
    var suppressNextTap by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed, isBusy) {
        holdProgress = 0f
        if (!isPressed || isBusy) return@LaunchedEffect

        val steps = 30
        repeat(steps) { index ->
            delay(50L)
            holdProgress = ((index + 1) / steps.toFloat()).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(isBusy) {
        if (isBusy) {
            tripleTapDetector.reset()
            isPressed = false
            holdProgress = 0f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_alpha"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_alpha"
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
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
        PulseRing(scale = ring1Scale, alpha = ring1Alpha * if (isActive) 1f else 0.4f)
        PulseRing(scale = ring2Scale, alpha = ring2Alpha * if (isActive) 1f else 0.4f)
        PulseRing(scale = ring3Scale, alpha = ring3Alpha * if (isActive) 1f else 0.4f)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isBusy -> ColorDangerDim
                        isPressed -> ColorDangerDim
                        else -> ColorDanger
                    }
                )
                .semantics { contentDescription = "SOS emergency button, long press or triple tap to activate" }
                .pointerInput(isBusy) {
                    detectTapGestures(
                        onTap = {
                            if (isBusy) return@detectTapGestures
                            if (suppressNextTap) {
                                suppressNextTap = false
                                return@detectTapGestures
                            }
                            val isTripleTap = tripleTapDetector.registerTap(SystemClock.elapsedRealtime())
                            if (isTripleTap) {
                                onTripleTapDetected()
                            }
                        },
                        onPress = {
                            if (isBusy) return@detectTapGestures
                            isPressed = true
                            holdProgress = 0f
                            val startTime = System.currentTimeMillis()
                            val released = tryAwaitRelease()
                            val elapsed = System.currentTimeMillis() - startTime
                            isPressed = false
                            holdProgress = 0f

                            if (released && elapsed >= 1500L) {
                                suppressNextTap = true
                                tripleTapDetector.reset()
                                onLongHoldConfirmed()
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

        if (isPressed && !isBusy) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 92.dp)
                    .width(88.dp)
            ) {
                LinearProgressIndicator(
                    progress = { holdProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RadiusFull),
                    color = ColorDanger,
                    trackColor = ColorDangerPulse.copy(alpha = 0.25f)
                )
                Text(
                    text = "Hold...",
                    style = RakshaTypography.labelSmall,
                    color = ColorTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
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
