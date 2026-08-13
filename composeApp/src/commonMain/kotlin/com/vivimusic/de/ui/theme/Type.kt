package com.vivimusic.de.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive type scale, ported 1:1 from ViVi Music
 * (`com.music.vivi.ui.theme.getTypography`).
 *
 * The original lets the user pick a brand font (Google Sans, Sans Flex, Outfit,
 * Plus Jakarta Sans). The desktop port uses the system font family for both the
 * brand and plain roles; the sizes, weights, line heights and letter spacing
 * are identical to the mobile app.
 */
fun appTypography(
    brandFont: FontFamily = FontFamily.Default,
    plainFont: FontFamily = FontFamily.Default,
): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = brandFont, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = plainFont, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = plainFont, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = plainFont, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = plainFont, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = plainFont, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = plainFont, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
