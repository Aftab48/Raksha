package com.raksha.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.raksha.app.ui.theme.*

enum class SafetyLevel { SAFE, MODERATE, RISKY }

fun Double.toSafetyLevel(): SafetyLevel = when {
    this < 0.4 -> SafetyLevel.SAFE
    this <= 0.7 -> SafetyLevel.MODERATE
    else -> SafetyLevel.RISKY
}

fun SafetyLevel.label(): String = when (this) {
    SafetyLevel.SAFE -> "Safe"
    SafetyLevel.MODERATE -> "Moderate"
    SafetyLevel.RISKY -> "Risky"
}

fun SafetyLevel.textColor(): Color = when (this) {
    SafetyLevel.SAFE -> ColorSafe
    SafetyLevel.MODERATE -> ColorWarning
    SafetyLevel.RISKY -> ColorDanger
}

fun SafetyLevel.bgColor(): Color = when (this) {
    SafetyLevel.SAFE -> ColorSafeSubtle
    SafetyLevel.MODERATE -> ColorWarningSubtle
    SafetyLevel.RISKY -> ColorDangerSubtle
}

@Composable
fun SafetyScoreBadge(
    score: Double,
    modifier: Modifier = Modifier
) {
    val level = score.toSafetyLevel()
    Text(
        text = "${level.label()} ${(score * 100).toInt()}%",
        style = RakshaTypography.labelMedium.copy(color = level.textColor()),
        modifier = modifier
            .clip(RadiusFull)
            .background(level.bgColor())
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
