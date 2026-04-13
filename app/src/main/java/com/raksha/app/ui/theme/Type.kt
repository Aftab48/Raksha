package com.raksha.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Font families — loaded from assets/fonts/
val PoppinsFamily = FontFamily.Default   // Replace with actual Poppins font files
val InterFamily = FontFamily.Default     // Replace with actual Inter font files
val IbmPlexMonoFamily = FontFamily.Default // Replace with actual IBM Plex Mono font files

val RakshaTypography = Typography(
    // Display — screen hero text
    displayLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = (32 * 1.1).sp,
        color = ColorTextPrimary
    ),
    // Heading 1 — section titles
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = (24 * 1.2).sp,
        color = ColorTextPrimary
    ),
    // Heading 2 — card headers
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = (18 * 1.3).sp,
        color = ColorTextPrimary
    ),
    // Body large — primary body
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = (16 * 1.5).sp,
        color = ColorTextPrimary
    ),
    // Body medium — secondary body
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.5).sp,
        color = ColorTextSecondary
    ),
    // Label — tags, chips, status labels
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = (12 * 1.4).sp,
        color = ColorTextSecondary
    )
)

// Custom text styles not covered by Material3 Typography
object RakshaTextStyle {
    val mono = TextStyle(
        fontFamily = IbmPlexMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = (12 * 1.4).sp,
        color = ColorTextSecondary
    )
    val sos = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = (20 * 1.2).sp,
        color = ColorTextPrimary
    )
}
