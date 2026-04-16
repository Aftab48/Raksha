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
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.ColorWarning
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

    val accentColor = if (state.isPanic) ColorWarning else ColorDanger

    val infiniteTransition = rememberInfiniteTransition(label = "alert_border")
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
                drawRect(accentColor.copy(alpha = 0.08f))
            }
            .border(
                width = 3.dp,
                color = accentColor.copy(alpha = borderAlpha),
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
                text = if (state.isPanic) "PANIC ALERT ACTIVE" else "ALERT ACTIVE",
                style = RakshaTypography.displayLarge.copy(color = accentColor),
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
                if (!state.isPanic) {
                    StatusCard(
                        icon = Icons.Filled.People,
                        label = "Contacts Alerted",
                        value = "${state.contactsAlerted}",
                        modifier = Modifier.weight(1f),
                        accentColor = accentColor
                    )
                }
                StatusCard(
                    icon = Icons.Filled.LocationOn,
                    label = "Live Location",
                    value = state.currentLocation?.let {
                        "${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}"
                    } ?: "Acquiring...",
                    modifier = Modifier.weight(1f),
                    monospace = true,
                    accentColor = accentColor
                )
            }

            state.event?.let { event ->
                val triggerText = when {
                    state.isPanic -> "Panic alert — police notified, call requested"
                    event.triggerType == "auto" -> "Auto-detected: ${(event.confidenceScore * 100).toInt()}% confidence"
                    else -> "Manually triggered"
                }
                Text(
                    text = triggerText,
                    style = RakshaTypography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }

            state.latestPoliceNote?.let { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurfaceElevated, RakshaShapes.medium)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Message from police",
                        style = RakshaTypography.labelMedium.copy(color = ColorWarning)
                    )
                    Text(
                        note,
                        style = RakshaTypography.bodyMedium.copy(color = ColorTextPrimary)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = if (state.isPanic) {
                    "Police have been notified silently.\nYour location is being shared every 30 seconds."
                } else {
                    "Emergency services and your contacts have been notified.\nYour location is being shared every 30 seconds."
                },
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
    monospace: Boolean = false,
    accentColor: androidx.compose.ui.graphics.Color = ColorDanger
) {
    Column(
        modifier = modifier
            .background(ColorSurface, RakshaShapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(20.dp))
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
