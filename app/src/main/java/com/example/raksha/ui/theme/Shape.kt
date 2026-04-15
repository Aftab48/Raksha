package com.example.raksha.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RakshaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // radius-sm: chips, tags
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),      // radius-md: cards, inputs
    large = RoundedCornerShape(16.dp),       // radius-lg: bottom sheets, modals
    extraLarge = RoundedCornerShape(24.dp)   // radius-xl: primary CTA buttons
)

val RadiusFull = RoundedCornerShape(999.dp) // Pills, FABs, toggles