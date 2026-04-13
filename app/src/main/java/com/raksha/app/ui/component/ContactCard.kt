package com.raksha.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raksha.app.data.local.entity.TrustedContactEntity
import com.raksha.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCard(
    contact: TrustedContactEntity,
    onDelete: (TrustedContactEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(contact)
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bg by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    ColorDanger else ColorSurface,
                label = "delete_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RakshaShapes.medium)
                    .background(bg),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete contact",
                    tint = ColorTextPrimary,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RakshaShapes.medium)
                .background(ColorSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ColorPrimarySubtle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(2).uppercase(),
                    style = RakshaTypography.labelMedium.copy(color = ColorPrimary)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = contact.name, style = RakshaTypography.bodyLarge)
                Text(text = contact.phone, style = RakshaTypography.bodyMedium)
            }
        }
    }
}
