package com.raksha.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.raksha.app.ui.theme.*

/**
 * Large pill toggle — 200dp wide.
 * Animates background color between surface/primary in 300ms.
 */
@Composable
fun ShieldToggle(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) ColorPrimary else ColorSurfaceElevated,
        animationSpec = tween(durationMillis = 300),
        label = "shield_bg"
    )
    val contentColor = if (isActive) ColorTextInverse else ColorTextSecondary
    val label = if (isActive) "Shield Active" else "Shield Off"

    Box(
        modifier = modifier
            .width(200.dp)
            .height(52.dp)
            .clip(RadiusFull)
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onToggle)
            .semantics { contentDescription = "Shield toggle, currently $label" },
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = RakshaTypography.bodyLarge.copy(color = contentColor)
            )
        }
    }
}
