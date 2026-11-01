package com.lagradost.cloudstream3.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Colors shared by the TV and phone UI (mockups "Components" board). Device specific tokens stay in
 * `TvColors` / `PhoneColors`; the TV object aliases the values below so both UIs cannot drift.
 */
object CsColors {
    val Ground = Color(0xFF0E1015)
    val Card = Color(0xFF1B1F27)
    val Preview = Color(0xFF13161C)
    val Outline = Color(0xFF2A2F3A)
    val Selection = Color(0xFFF1F3F7)
    val OnSelection = Color(0xFF0E1015)
    val Text = Color(0xFFF1F3F7)
    val Text2 = Color(0xFFA9B0BD)
    val Body = Color(0xFFD5D9E1)
    val LabelPanel = Color(0xFF8790A0)
    val Link = Color(0xFF9CC1FF)
    val Info = Color(0xFF8FD3E0)
    val AccentDefault = Color(0xFFF4B97A)
    val OnAccentDefault = Color(0xFF2A1A0C)
    val DotSeparator = Color(0xFF5A6170)
    val Danger = Color(0xFFFF9B8F)
    val Success = Color(0xFF9FE3B4)
    val FillButton = Color(0x21FFFFFF)
    val ProgressTrack = Color(0x38FFFFFF)
    val Scrim = Color(0xA3050609)
}
