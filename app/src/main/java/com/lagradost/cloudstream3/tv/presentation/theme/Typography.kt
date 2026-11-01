package com.lagradost.cloudstream3.tv.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.lagradost.cloudstream3.designsystem.FigtreeFontFamily

/**
 * Legacy name kept so screens that still reference it (and are not part of the foundation work)
 * keep compiling. The UI font is Figtree now.
 */
val InterFontFamily: FontFamily = FigtreeFontFamily

private val HeadingWeight = FontWeight.SemiBold
private val CtaWeight = FontWeight.Medium
private val MetaWeight = FontWeight.Normal

val Typography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = HeadingWeight,
        letterSpacing = (-0.25).sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    displaySmall = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = CtaWeight,
        letterSpacing = 0.15.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = HeadingWeight,
        letterSpacing = 0.1.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = CtaWeight,
        letterSpacing = 0.1.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = MetaWeight,
        letterSpacing = 0.1.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = CtaWeight,
        letterSpacing = 0.1.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = MetaWeight,
        letterSpacing = 0.5.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = MetaWeight,
        letterSpacing = 0.25.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = MetaWeight,
        letterSpacing = 0.2.sp,
        fontFamily = FigtreeFontFamily,
        textMotion = TextMotion.Animated
    )
)
