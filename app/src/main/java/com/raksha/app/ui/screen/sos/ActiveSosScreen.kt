package com.raksha.app.ui.screen.sos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raksha.app.ui.theme.ColorBackground
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorDangerSubtle
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaShapes
import com.raksha.app.ui.theme.RakshaTextStyle
import com.raksha.app.ui.theme.RakshaTypography
import com.raksha.app.viewmodel.SosViewModel

@Composable
fun ActiveSosScreen(
    sosEventId: Int,
    onAlertCancelled: () -> Unit,
    viewModel: SosViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sosEventId) {
        viewModel.loadEvent(sosEventId)
    }

    LaunchedEffect(state.isCancelled) {
        if (state.isCancelled) onAlertCancelled()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sos_border")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .drawBehind {
                drawRect(ColorDanger.copy(alpha = 0.08f))
            }
            .border(
                width = 3.dp,
                color = ColorDanger.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "ALERT ACTIVE",
                style = RakshaTypography.displayLarge.copy(color = ColorDanger),
                textAlign = TextAlign.Center
            )

            Text(
                text = formatElapsed(state.elapsedSeconds),
                style = RakshaTextStyle.mono.copy(color = ColorTextSecondary),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    icon = Icons.Filled.People,
                    label = "Contacts Alerted",
                    value = "${state.contactsAlerted}",
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    icon = Icons.Filled.LocationOn,
                    label = "Live Location",
                    value = state.currentLocation?.let {
                        "${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}"
                    } ?: "Acquiring...",
                    modifier = Modifier.weight(1f),
                    monospace = true
                )
            }

            state.event?.let { event ->
                val triggerText = if (event.triggerType == "auto") {
                    "Auto-detected · ${(event.confidenceScore * 100).toInt()}% confidence"
                } else {
                    "Manually triggered"
                }
                Text(
                    text = triggerText,
                    style = RakshaTypography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Emergency services and your contacts have been notified.\nYour location is being shared every 30 seconds.",
                style = RakshaTypography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = viewModel::cancelAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RakshaShapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = ColorDangerSubtle)
            ) {
                Text(
                    "Cancel Alert",
                    color = ColorDanger,
                    style = RakshaTypography.bodyLarge
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false
) {
    Column(
        modifier = modifier
            .background(ColorSurface, RakshaShapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = ColorDanger, modifier = Modifier.size(20.dp))
        Text(label, style = RakshaTypography.labelMedium)
        Text(
            value,
            style = if (monospace) RakshaTextStyle.mono else RakshaTypography.bodyLarge
        )
    }
}

private fun formatElapsed(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
