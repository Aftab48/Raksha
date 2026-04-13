package com.raksha.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raksha.app.repository.ScoredRoute
import com.raksha.app.ui.theme.*

@Composable
fun RouteCard(
    route: ScoredRoute,
    rank: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = route.safetyScore.toSafetyLevel()
    val borderColor = when (level) {
        SafetyLevel.SAFE -> ColorSafe
        SafetyLevel.MODERATE -> ColorWarning
        SafetyLevel.RISKY -> ColorDanger
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RakshaShapes.medium)
            .background(ColorSurface)
            .clickable(onClick = onSelect)
    ) {
        // Left colored border strip
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(borderColor)
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (rank == 1) "Safest Route" else route.name,
                    style = RakshaTypography.headlineMedium
                )
                SafetyScoreBadge(score = route.safetyScore)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatDuration(route.durationSeconds),
                        style = RakshaTypography.bodyMedium
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        tint = ColorTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatDistance(route.distanceMeters),
                        style = RakshaTypography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    return if (mins < 60) "$mins min" else "${mins / 60}h ${mins % 60}min"
}

private fun formatDistance(meters: Int): String =
    if (meters >= 1000) "${"%.1f".format(meters / 1000.0)} km" else "$meters m"
