package com.raksha.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorWarning
import com.raksha.app.ui.theme.ColorWarningSubtle
import com.raksha.app.ui.theme.RakshaTextStyle
import com.raksha.app.ui.theme.RakshaTypography

@Composable
fun PanicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBusy: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "panic_pulse")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "panic_ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "panic_ring1_alpha"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "panic_ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "panic_ring2_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(120.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(ring1Scale)
                .clip(CircleShape)
                .background(ColorWarning.copy(alpha = ring1Alpha * 0.4f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(ring2Scale)
                .clip(CircleShape)
                .background(ColorWarning.copy(alpha = ring2Alpha * 0.4f))
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isBusy) ColorWarningSubtle else ColorWarning)
                .semantics { contentDescription = "Panic alert button, tap once to alert police" }
                .pointerInput(isBusy) {
                    detectTapGestures(
                        onTap = {
                            if (!isBusy) onClick()
                        }
                    )
                }
        ) {
            Text(
                text = "PANIC",
                style = RakshaTypography.labelLarge.copy(color = ColorTextPrimary),
                textAlign = TextAlign.Center
            )
        }
    }
}
