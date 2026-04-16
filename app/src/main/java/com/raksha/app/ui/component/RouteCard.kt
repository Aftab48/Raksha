package com.raksha.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.raksha.app.repository.ScoredRoute
import com.raksha.app.ui.theme.ColorDanger
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorSafe
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorSurfaceElevated
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.ColorWarning
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaShapes
import com.raksha.app.ui.theme.RakshaTypography

@Composable
fun RouteCard(
    route: ScoredRoute,
    rank: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val level = route.safetyScore.toSafetyLevel()
    val borderColor = when (level) {
        SafetyLevel.SAFE -> ColorSafe
        SafetyLevel.MODERATE -> ColorWarning
        SafetyLevel.RISKY -> ColorDanger
    }

    val cardBackground = if (isSelected) ColorSurfaceElevated else ColorSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RakshaShapes.medium)
            .background(cardBackground)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) ColorPrimary else cardBackground,
                shape = RakshaShapes.medium
            )
            .clickable(onClick = onSelect)
            .semantics {
                role = Role.Button
                selected = isSelected
                contentDescription = if (isSelected) {
                    "Route option selected"
                } else {
                    "Route option"
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
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

                if (isSelected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = ColorPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Selected",
                            style = RakshaTypography.labelMedium.copy(color = ColorPrimary)
                        )
                    }
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
                            style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary)
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
                            style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .fillMaxHeight()
                .background(borderColor, RadiusFull)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    return if (mins < 60) "$mins min" else "${mins / 60}h ${mins % 60}min"
}

private fun formatDistance(meters: Int): String =
    if (meters >= 1000) "${"%.1f".format(meters / 1000.0)} km" else "$meters m"
