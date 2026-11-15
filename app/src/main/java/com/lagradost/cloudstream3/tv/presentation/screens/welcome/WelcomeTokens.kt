package com.lagradost.cloudstream3.tv.presentation.screens.welcome

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.lagradost.cloudstream3.designsystem.FigtreeFontFamily
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Welcome flow sizes (mockup "Welcome": 1920x1080 px, 1 dp == 2 px). */
internal object WelcomeTokens {
    val CardWidth = 504.dp
    val PreviewWidth = 456.dp
    /** Header text of the card; the row pills of Settings add their own 24 dp text inset on top of [ListStart]. */
    val HeaderStart = 98.dp
    val ListStart = 74.dp
    val FooterStart = 78.dp
    val HeaderTop = 48.dp
    val AppIconSize = 56.dp
    val FooterHeight = 38.dp
    val FooterBottom = 32.dp
    val FooterGap = 10.dp
    val ButtonHorizontalPadding = 22.dp
    val PreviewStart = 60.dp
    val PreviewTop = 64.dp
    val PreviewBottom = 32.dp
    val StepCircle = 32.dp
    val StepGap = 18.dp
    val StepRowGap = 27.dp
    val ProgressHeight = 4.dp
    val QrSize = 150.dp
}

internal object WelcomeText {
    val Label = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em)
    val Title = TvTextStyles.SettingsDecorTitle
    val Subtitle = TvTextStyles.SettingsDecorSubtitle
    val Button = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    val PreviewHeading = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val StepTitle = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val StepTitleCurrent = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val StepSummary = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal)
    val StepNumber = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val Note = TextStyle(fontFamily = FigtreeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 17.sp)
}

internal val WelcomeButtonShape = RoundedCornerShape(percent = 50)
internal val WelcomeEllipsis = TextOverflow.Ellipsis
