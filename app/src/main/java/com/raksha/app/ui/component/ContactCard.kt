package com.raksha.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.raksha.app.data.local.entity.TrustedContactEntity
import com.raksha.app.ui.theme.ColorPrimary
import com.raksha.app.ui.theme.ColorPrimarySubtle
import com.raksha.app.ui.theme.ColorSurface
import com.raksha.app.ui.theme.ColorTextPrimary
import com.raksha.app.ui.theme.ColorTextSecondary
import com.raksha.app.ui.theme.RadiusFull
import com.raksha.app.ui.theme.RakshaShapes
import com.raksha.app.ui.theme.RakshaTypography

@Composable
fun ContactCard(
    contact: TrustedContactEntity,
    onDeleteRequest: (TrustedContactEntity) -> Unit,
    modifier: Modifier = Modifier,
    deleteEnabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RakshaShapes.medium)
            .background(ColorSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = contact.name, style = RakshaTypography.bodyLarge)
            Text(text = contact.phone, style = RakshaTypography.bodyMedium.copy(color = ColorTextSecondary))
        }

        IconButton(
            onClick = { onDeleteRequest(contact) },
            enabled = deleteEnabled,
            modifier = Modifier
                .size(36.dp)
                .background(ColorPrimarySubtle, RadiusFull)
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete contact",
                tint = ColorTextPrimary
            )
        }
    }
}
