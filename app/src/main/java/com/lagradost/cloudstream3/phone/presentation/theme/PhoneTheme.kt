package com.lagradost.cloudstream3.phone.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.designsystem.CsColors
import com.lagradost.cloudstream3.designsystem.FigtreeFontFamily

/** Phone-only colors; the palette shared with TV lives in [CsColors]. */
object PhoneColors {
    val Ground = CsColors.Ground
    val Card = CsColors.Card
    val Sheet = CsColors.Preview
    val Divider = Color(0xFF22262F)
    val Outline = CsColors.Outline
    val Text = CsColors.Text
    val Text2 = CsColors.Text2
    val Body = CsColors.Body
    val Label = CsColors.LabelPanel
    val Dot = CsColors.DotSeparator
    val FillButton = CsColors.FillButton
    val ProgressTrack = Color(0x38FFFFFF)
    val Info = CsColors.Info
    val Link = CsColors.Link
    val Danger = CsColors.Danger
    val Success = CsColors.Success
    val Placeholder = Color(0xFF1E222B)
    val Scrim = Color(0x99050609)
    val OnAccent = CsColors.OnAccentDefault
}

/** Sizes from the "Components" board, phone column (dp). */
object PhoneDimens {
    val ScreenPadding = 16.dp
    val SectionGap = 26.dp
    val ButtonHeight = 48.dp
    val PrimaryButtonHeight = 52.dp
    val IconButton = 48.dp
    val ChipHeight = 40.dp
    val RowHeight = 56.dp
    val PosterWidth = 108.dp
    val PosterHeight = 162.dp
    val PosterGap = 10.dp
    val PosterRadius = 12.dp
    val CardRadius = 20.dp
    val HeroCardWidth = 318.dp
    val HeroCardHeight = 210.dp
    val HeroRadius = 24.dp
    val ProgressHeight = 4.dp
}

object PhoneShapes {
    val Pill = RoundedCornerShape(percent = 50)
    val Poster = RoundedCornerShape(PhoneDimens.PosterRadius)
    val Card = RoundedCornerShape(PhoneDimens.CardRadius)
    val Hero = RoundedCornerShape(PhoneDimens.HeroRadius)
    val Sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

object PhoneText {
    private fun s(size: Float, w: FontWeight, lh: Float? = null) = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = w,
        fontSize = size.sp,
        lineHeight = lh?.sp ?: TextStyle.Default.lineHeight,
    )

    val Title = s(34f, FontWeight.ExtraBold)
    val Level = s(24f, FontWeight.ExtraBold)
    val Section = s(20f, FontWeight.ExtraBold)
    val Row = s(16f, FontWeight.SemiBold)
    val Body = s(15f, FontWeight.Normal, 23f)
    val Meta = s(13f, FontWeight.Medium)
    val Button = s(15f, FontWeight.Bold)
    val ButtonLarge = s(16f, FontWeight.Bold)
    val Kicker = s(11f, FontWeight.Bold).copy(letterSpacing = 0.1.sp)
    val PosterTitle = s(13f, FontWeight.SemiBold, 16f)
    val Chip = s(14f, FontWeight.SemiBold)
}

/** Accent of the current title (per-title HCT accent is applied by detail screens). */
val LocalPhoneAccent = staticCompositionLocalOf { CsColors.AccentDefault }

/** Text / icon color on top of [LocalPhoneAccent] (primary buttons). */
val LocalPhoneOnAccent = staticCompositionLocalOf { CsColors.OnAccentDefault }

@Composable
fun PhoneTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = CsColors.AccentDefault,
        onPrimary = CsColors.OnAccentDefault,
        background = PhoneColors.Ground,
        onBackground = PhoneColors.Text,
        surface = PhoneColors.Ground,
        onSurface = PhoneColors.Text,
        surfaceVariant = PhoneColors.Card,
        onSurfaceVariant = PhoneColors.Text2,
        surfaceContainer = PhoneColors.Sheet,
        surfaceContainerHigh = PhoneColors.Sheet,
        outline = PhoneColors.Outline,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
