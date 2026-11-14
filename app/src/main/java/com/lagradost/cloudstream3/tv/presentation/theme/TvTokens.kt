package com.lagradost.cloudstream3.tv.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.designsystem.CsColors
import com.lagradost.cloudstream3.designsystem.FigtreeFontFamily

/**
 * Single source of design tokens for the TV redesign (mockups "Components" row).
 *
 * Mockups are 1920x1080 px; the TV UI runs at 960x540 dp (320 dpi), so 1 dp == 2 mockup px.
 * Every value below is the mockup px value divided by 2.
 */
object TvColors {
    val Ground = CsColors.Ground
    val Card = CsColors.Card
    val Panel = Color(0xFF171A21)
    val Preview = CsColors.Preview
    val PosterPlaceholder = Color(0xFF1E222B)
    val Skeleton = Color(0xFF1C2028)
    val DialogSurface = Color(0xFF22262F)
    val Outline = CsColors.Outline

    /** White focus fill / ring and the text color used on top of it. */
    val Focus = CsColors.Selection
    val OnFocus = CsColors.OnSelection
    /** Secondary text on a focused (white) row. */
    val OnFocusSecondary = Color(0xFF4A5160)

    val Text = CsColors.Text
    val Text2 = CsColors.Text2
    val Body = CsColors.Body
    val Meta = Color(0xFFC9CED8)
    /** Label on screens (feed headers, counters). */
    val Label = Color(0xFF8E95A3)
    /** Label in Components board, side panels and panel sections. */
    val LabelPanel = CsColors.LabelPanel
    val RowSubtitle = Color(0xFF9AA1AE)
    val Disabled = Color(0xFF6B7280)
    /** Settings preview card text (mockup #8C93A1 / #5F6673). */
    val SettingsPreviewText = Color(0xFF8C93A1)
    val SettingsPreviewSubtext = Color(0xFF5F6673)
    val SettingsDivider = Color(0x14FFFFFF)
    /** Tile colors of plugins without an icon. */
    val SettingsTilePalette = listOf(
        Color(0xFF2F7D32), Color(0xFF8E1B1B), Color(0xFF5B32D6), Color(0xFF4C3FD1),
        Color(0xFF7A4FD8), Color(0xFFC81E3A), Color(0xFF1E6E8C), Color(0xFF3A5A9A),
    )
    /** Inset card inside the settings cards (account summary). */
    val DotSeparator = CsColors.DotSeparator
    val RadioBorder = Color(0xFF5A6170)
    val RadioBorderFocused = Color(0xFF4A5160)

    /** Radio / selected check / download bar ("app" blue). */
    val Selected = CsColors.Link
    val ToggleOn = Color(0xFF3F6FE0)
    val ToggleThumb = Color(0xFFFFFFFF)
    /** Source chips, progress of "Continue watching". */
    val Accent = Color(0xFFF2B880)
    /** Selected nav item: accent @22%, content #FFD9B0. */
    val AccentContainer = Color(0x38F2B880)
    val OnAccentContainer = Color(0xFFFFD9B0)
    val RailIcon = Color(0xFFC4C7C5)
    /** "Featured" eyebrow label. */
    val FeaturedEyebrow = CsColors.Info
    val FeaturedPanel = Color(0xFF14181F)
    val BodyBright = Color(0xFFDCE1E8)
    val Danger = CsColors.Danger
    /** Destructive label on the white focus fill (mockup SettingsPluginActions). */
    val DangerOnFocus = Color(0xFFC2402F)
    val ErrorText = Color(0xFFFFB0A6)
    /** "Downloaded" state of the download button / episode action. */
    val Success = CsColors.Success
    val SuccessFill = Color(0x2E9FE3B4)
    val ErrorFill = Color(0x2EFF9B8F)
    /** Details: hairline under the tabs and the separator between seasons and episodes (white 10% / 8%). */
    val DetailsDivider = Color(0x1AFFFFFF)
    /** Fallback palette of the details screens until the backdrop colors are computed (series = cool, movie = warm). */
    val DetailsSeriesPrimary = Color(0xFF8FD3E0)
    val DetailsSeriesOnPrimary = Color(0xFF0B1315)
    val DetailsSeriesBackground = Color(0xFF0B1315)
    val DetailsSeriesOnSurface = Color(0xFFEEF4F5)
    val DetailsSeriesOnSurfaceVariant = Color(0xFFA7B4B7)
    val DetailsSeriesInverseSurface = Color(0xFFF3F7F8)
    val DetailsMoviePrimary = CsColors.AccentDefault
    val DetailsMovieOnPrimary = CsColors.OnAccentDefault
    val DetailsMovieBackground = Color(0xFF120E0B)
    val DetailsMovieOnSurface = Color(0xFFF3EFEA)
    val DetailsMovieOnSurfaceVariant = Color(0xFFB9AFA6)
    val DetailsMovieInverseSurface = Color(0xFFF7F4F0)
    val DetailsSeparator = Color(0x14FFFFFF)
    /** Watched badge on an episode still (mockup rgba(8,12,14,.85)). */
    val EpisodeBadge = Color(0xD9080C0E)

    /** Focused download row (rgba(255,255,255,.07)). */
    val FillRow = Color(0x12FFFFFF)
    val FillButton = CsColors.FillButton
    val FillButtonDisabled = Color(0x0FFFFFFF)
    val FillTabCurrent = Color(0x1AFFFFFF)
    val FillChip = Color(0x12FFFFFF)
    val BorderChip = Color(0x24FFFFFF)
    val FillIcon = Color(0x1AFFFFFF)
    val ProgressTrack = CsColors.ProgressTrack
    val Scrim = CsColors.Scrim
    val DialogScrim = Color(0x99050609)
    val FeaturedIndicator = Color(0x990A0C10)
    /** Player (mockups Player*): value text, buffered part of the timeline, option pills. */
    val PlayerValue = Color(0xFFB8BEC9)
    val SyncButtonFill = Color(0x1FFFFFFF)
    val SyncButtonBorder = Color(0x47FFFFFF)
    val SourceErrorIcon = Color(0xFFC98A82)
    val PlayerFillOption = Color(0x24FFFFFF)
    val PlayerTrack = Color(0x40FFFFFF)
    val PlayerTrackFocused = Color(0x4DFFFFFF)
    val PlayerBuffered = Color(0x73FFFFFF)
    val PlayerBufferedFocused = Color(0x80FFFFFF)
    val PlayerCard = Color(0xF5171A21)
    /** Player notices (mockup PlayerNotices): card #171A21 at 96%, white 8% border, error #FF9B8F, warning #F4B97A. */
    val PlayerNoticeCard = Color(0xF5171A21)
    val PlayerNoticeBorder = Color(0x14FFFFFF)
    val PlayerNoticeError = Color(0xFFFF9B8F)
    val PlayerNoticeWarning = Color(0xFFF4B97A)
    val PlayerSeekBadge = Color(0xBF0A0C10)
    val PlayerSubtleDivider = Color(0x14FFFFFF)
    val PlayerCueNow = Color(0x1A9CC1FF)
    val PlayerCueAction = Color(0xFF2F5FCC)
    val PlayerFieldBackground = Color(0xFF14171D)
    /** Initials tile in the sources panel. */
    val Tile = Color(0xFF262B36)
    val TileFocused = Color(0xFFD9DCE3)
}

object TvDimens {
    // Screen
    val ScreenHorizontalPadding = 48.dp
    /** Collapsed navigation rail; screens never place content under it. */
    val RailWidth = 80.dp
    /** Left edge of screen content: rail (64 dp) + 24 dp. */
    val ContentStartWithRail = 88.dp
    // Navigation menu (mockups 3, 4, 6)
    val NavMenuRowInset = 12.dp
    val NavMenuRowWidth = 256.dp
    val NavMenuSolidWidth = 300.dp
    val NavMenuFadeWidth = 500.dp
    val NavMenuPanelWidth = 280.dp
    val NavMenuProfileHeight = 40.dp
    val NavMenuAvatarSize = 32.dp
    val NavMenuRowHeight = 48.dp
    val NavMenuRowGap = 4.dp
    val NavMenuListRowHeight = 31.dp
    val NavMenuIconSize = 16.dp
    val NavMenuDotSize = 7.dp
    val NavRailDotSize = 4.dp
    val NavRailTileSize = 48.dp
    val NavRailIconSize = 16.dp
    val NavRailAvatarSize = 32.dp
    val NavRailScrimWidth = 88.dp
    // Top bar (mockup Main: logo 52 px + 28 px gap, tab padding 26 px, search icon 28 px + 10 px)
    val TopBarLogoSize = 26.dp
    val TopBarLogoGap = 14.dp
    val TopBarTabPadding = 13.dp
    val TopBarSearchIcon = 14.dp
    val TopBarSearchGap = 5.dp

    // Shapes
    val CardRadius = 7.dp
    val RowRadius = 9.dp
    val PanelRowRadius = 8.dp
    val DialogRadius = 14.dp

    // Button
    val ButtonHeight = 34.dp
    val ButtonHeightLarge = 36.dp
    val ButtonPaddingStart = 12.dp
    val ButtonPaddingEnd = 15.dp
    val ButtonIconSize = 13.dp
    val ButtonIconGap = 6.dp

    // Chip
    val ChipHeight = 28.dp
    val ChipBorderWidth = 1.dp

    // List row
    val RowHeight = 44.dp
    val RowHeightLarge = 48.dp
    val PanelRowHeight = 38.dp
    /** Download mirror rows 92 px (mockup), kept at exactly this height. */
    val PanelMirrorRowHeight = 46.dp
    /** Watch status panel icons 30 px (mockup), so the title starts 56 dp from the panel edge. */
    val PanelStatusIconSize = 15.dp
    /** Plugin delete dialog top edge (mockup: 330 px). */
    val PluginDeleteDialogTop = 165.dp
    /** Watch status panel rows 84 px, episode options panel rows 70 px. */
    val PanelStatusRowHeight = 42.dp
    val PanelMenuRowHeight = 35.dp
    val PanelStatusTopPadding = 32.dp
    val PanelListBottomPadding = 32.dp
    /** Group header in side panels: margin 22 px above, 6 px below, 19 px/700, +.12em, uppercase. */
    val PanelGroupHeaderTop = 11.dp
    val PanelGroupHeaderBottom = 3.dp
    val RowHorizontalPadding = 13.dp
    val RowGap = 11.dp
    val RowIconSize = 17.dp
    val RowTrailingIconSize = 14.dp
    val RowSubtitleTopPadding = 2.dp
    val RowSpacing = 4.dp

    // Chip
    val ChipIconSize = 11.dp

    // Poster / mini-grid (mockup 261x392, gap 32 px)
    val PosterWidth = 130.5.dp
    val PosterHeight = 196.dp
    val MiniGridGap = 16.dp
    val ShowMoreCircleSize = 36.dp
    val ShowMoreCirclePadding = 9.dp
    val ShowMoreHorizontalPadding = 10.dp
    val ShowMoreGap = 9.dp

    // Home feed (mockup HomeFeeds: header y=44 h=56, grid y=124 => gap 24 px; next header at y=996)
    val FeedSectionBottomPadding = 6.dp
    val FeedHeaderHeight = 28.dp
    val FeedHeaderGridGap = 12.dp
    val FeedHeaderTitleGap = 8.dp
    val FeedFocusedTitleGap = 7.dp
    val FeedFocusedTitleMaxWidth = 380.dp
    /** Padding above the feed header = space between the pinned header and the screen top (mockup 44 px). */
    val FeedPinnedTop = 22.dp
    // Home feed as one row (board 2): header 28 + 17 gap, posters 116x174, rows 244 apart
    val FeedRowTop = 25.dp
    val FeedRowHeaderGap = 17.dp
    val FeedRowPosterWidth = 116.dp
    val FeedRowPosterHeight = 174.dp
    val FeedRowGap = 12.dp
    val FeedRowMaxItems = 10
    val FeedRowEmptyHeight = 112.dp
    val FeedRowDescriptionWidth = 520.dp
    val FeedShowAllCircle = 40.dp

    // Home "Featured" (mockup 1728x680 px, r24, ring 4 px + offset 5 px)
    val FeaturedHeight = 340.dp
    val FeaturedRadius = 12.dp
    val FeaturedContentPadding = 32.dp
    /** Text block starts 96 px below the card top (mockup), fixed slot. */
    val FeaturedContentTop = 48.dp
    val FeaturedTextMaxWidth = 440.dp
    val FeaturedIndicatorEnd = 16.dp
    val FeaturedIndicatorBottom = 14.dp
    val FeaturedChevronWidth = 24.dp
    val FeaturedChevronHeight = 40.dp
    const val FeaturedTransitionMs = 150
    /** Debounce for swapping the Home hero when focus moves between "Continue watching" cards. */
    const val HeroSwapDebounceMs = 120L

    // "Show more" grid: same 6-column poster as the Home mini-grid (261x392 px), header like a feed header
    // (title y=44 px, grid y=124 px); the 32 dp header row also fits the Sort button.
    const val GridColumns = 7
    const val PosterAspectRatio = 2f / 3f
    const val GridSkeletonCount = 18
    val GridColumnGap = 14.dp
    val GridRowGap = MiniGridGap
    val GridHeaderTopPadding = 20.dp
    val GridHeaderHeight = 32.dp
    val GridContentTop = 76.dp
    val GridContentBottom = 24.dp
    /** Fixed slot for the focused poster title, so focus changes never move the Sort button. */
    val GridFocusedTitleSlotWidth = 300.dp
    val GridFocusedTitleSortGap = 16.dp
    /** Fits the longest label ("Sort: Alphabetical (Z to A)"). */
    /** Space under the grid header; posters scroll under the header's shadow, not right against it. */
    val GridHeaderBottomPadding = 10.dp
    val GridHeaderShadowHeight = 18.dp
    val GridSortButtonHeight = 32.dp

    // Sort panel (mockup 640 px, row 84 px)
    val SortPanelWidth = 320.dp
    val SortPanelRowHeight = 42.dp

    // Panel tile (mockup 44 px, r12)
    val PanelTileSize = 22.dp
    val PanelTileRadius = 6.dp

    // Panel
    val PanelTitleHorizontalPadding = 14.dp
    val PanelTitleBottomPadding = 10.dp
    val PanelSectionVerticalPadding = 8.dp
    val PanelFieldVerticalPadding = 8.dp
    val PanelFieldGap = 6.dp
    val PanelSupportingTopPadding = 4.dp

    // Dialog gaps
    val DialogTitleBodyGap = 8.dp
    val DialogBodyButtonsGap = 20.dp
    val DialogButtonGap = 8.dp

    // States
    val StateIconSize = 52.dp
    /** Penpot empty-state illustration: 240 px / 2 = 120 dp. */
    val StateIllustrationSize = 120.dp
    val StateTitleGap = 14.dp
    val StateMessageGap = 6.dp
    val StateActionGap = 22.dp
    val StateErrorTitleGap = 8.dp
    val StateButtonGap = 10.dp
    val StateMaxWidth = 520.dp


    // Home (1 dp = 2 mockup px): hero page, source row, featured chrome
    val HomeContentPadding = ContentStartWithRail
    val HeroHeight = 372.dp
    val HeroInfoTop = 64.dp
    val HeroInfoWidth = 640.dp
    val HeroTitleHeight = 48.dp
    val FeaturedTitleHeight = 40.dp
    val HeroSectionTitleTop = 232.dp
    val HeroCardsTop = 259.dp
    val HeroCardWidth = 176.dp
    val HeroCardHeight = 99.dp
    val HeroCardSpacing = 14.dp
    val HeroCardsInset = 8.dp
    val HeroButtonSpacing = 10.dp
    val HeroProgressWidth = 120.dp
    val HeroSectionTitleGap = 10.dp
    val HeroEyebrowGap = 7.dp
    val HeroMetaTopGap = 11.dp
    val HeroButtonsTopGap = 20.dp
    val HeroMetaGap = 9.dp
    val HeroBadgeStart = 6.dp
    val HeroBadgeBottom = 9.dp
    val HeroBadgeRadius = 4.dp
    val HeroBadgeHorizontal = 5.dp
    val HeroBadgeVertical = 2.dp
    val HeroSelectedBorder = 1.5.dp
    val HeroBackdropWidth = 700.dp
    val HeroBackdropHeight = 372.dp
    val HeroBackdropFadeTop = 230.dp
    val HeroBackdropFadeHeight = 142.dp
    val SourcesToFeaturedGap = 18.dp
    val TopBarToContentGap = 20.dp
    val FeedPinBottomPadding = 64.dp
    val SourceChipGap = 8.dp
    val SourceLabelGap = 12.dp
    val SourceChipMaxWidth = 220.dp
    val SourceRowInset = 4.dp
    val FeaturedDotGap = 5.dp
    val FeaturedDotHeight = 5.dp
    val FeaturedDotWidth = 5.dp
    val FeaturedDotActiveWidth = 14.dp
    val FeaturedIndicatorHorizontal = 8.dp
    val FeaturedIndicatorVertical = 5.dp
    val FeaturedIndicatorCounterGap = 3.dp
    val FeaturedChevronIcon = 18.dp
    val FeaturedEyebrowGap = 6.dp
    val FeaturedMetaTop = 9.dp
    val FeaturedDescriptionTop = 9.dp
    val FeaturedSupportingTop = 5.dp
    val FeaturedCtaTop = 16.dp
    val FeaturedCtaHeight = 32.dp
    val FeedSourceMaxWidth = 160.dp
    val SkeletonChipWidthA = 80.dp
    val SkeletonChipWidthB = 70.dp
    val SkeletonChipWidthC = 90.dp
    val ScrollbarGutter = 2.dp

    // Poster badge
    val BadgeRadius = 10.dp
    val BadgeMargin = 6.dp
    val BadgeHorizontalPadding = 5.dp
    val BadgeVerticalPadding = 4.dp
    val BadgeIconSize = 10.dp
    val BadgeIconGap = 4.dp
    val PosterTitleTopPadding = 10.dp
    val PosterTitleHorizontalPadding = 2.dp
    val PosterTitleAreaHeight = 40.dp

    // Selection indicators
    val RadioSize = 16.dp
    val RadioBorder = 1.5.dp
    val RadioDot = 7.dp
    val ToggleWidth = 36.dp
    val ToggleHeight = 20.dp
    val ToggleThumb = 16.dp
    val TogglePadding = 2.dp
    val CheckboxSize = 17.dp
    val CheckboxRadius = 4.dp
    val CheckboxTick = 1.5.dp

    // Settings (mockups Settings*: card 1040x904 px, preview 648 px, rows 96 px, 1 dp == 2 px)
    /** Gap between the 72 dp top bar and the cards on level 1 (mockup 8 dp under a 56 dp bar). */
    /** Levels >= 2 hide the top bar: cards start 24 dp from the top (mockup top: 48 px). */
    /** Level 1 leaves room under the cards (mockup: cards end at 960 px). */
    val SettingsCardRadius = 14.dp
    val SettingsRowChevronSize = 15.dp

    // Settings: spacing and size scale used by the level screens (value in dp == name)
    val SettingsDp0 = 0.dp
    val SettingsDp1 = 1.dp
    val SettingsDp1_5 = 1.5.dp
    val SettingsDp2 = 2.dp
    val SettingsDp3 = 3.dp
    val SettingsDp4 = 4.dp
    val SettingsDp5 = 5.dp
    val SettingsDp6 = 6.dp
    val SettingsDp7 = 7.dp
    val SettingsDp8 = 8.dp
    val SettingsDp10 = 10.dp
    val SettingsDp12 = 12.dp
    val SettingsDp14 = 14.dp
    val SettingsDp16 = 16.dp
    val SettingsDp18 = 18.dp
    val SettingsDp22 = 22.dp
    val SettingsDp24 = 24.dp
    val SettingsDp36 = 36.dp
    val SettingsDp64 = 64.dp
    val SettingsDp223 = 223.dp
    val SettingsDp257 = 257.dp
    val SettingsDebugPadding = 10.dp
    val SettingsDebugRadius = 10.dp
    val SettingsDebugHorizontalPadding = 12.dp
    val SettingsDebugVerticalPadding = 8.dp
    val SettingsDebugSpacing = 2.dp
    const val SettingsDebugAlpha = 0.88f
    const val SettingsSpinnerTrackAlpha = 0.35f
    const val SettingsStateMessageAlpha = 0.78f
    val SettingsHeaderHorizontalPadding = 14.dp
    val SettingsPreviewRowHeight = 42.dp
    val SettingsPreviewRowHorizontalPadding = 12.dp
    val SettingsPreviewHeaderBottomGap = 10.dp
    val SettingsSliderThumb = 12.dp
    val SettingsDividerGap = 14.dp
    val SettingsDividerHeight = 1.dp
    val SettingsQrSize = 200.dp
    val SettingsQrPadding = 16.dp
    val SettingsQrRadius = 12.dp
    val SettingsTextBlockRadius = 9.dp
    val SettingsTextBlockPadding = 14.dp
    val SettingsTextBlockRing = 2.dp
    val SettingsStateBoxRadius = 9.dp
    val SettingsStateBoxPadding = 14.dp
    val SettingsSkeletonHeight = 12.dp
    val SettingsSpinnerSize = 17.dp

    // Settings (system): flat 504 dp panels in a horizontal strip, values in dp (Penpot px / 2)
    val SettingsPaneWidth = 504.dp
    val SettingsPaneStartInset = 58.dp
    val SettingsItemWidth = 347.dp
    val SettingsPaneEndPadding = 62.dp
    val SettingsItemMinHeight = 60.dp
    val SettingsItemTwoLineMinHeight = 64.dp
    val SettingsItemGap = 12.dp
    val SettingsItemIconSize = 40.dp
    val SettingsItemGlyphSize = 18.dp
    val SettingsItemIconGap = 12.dp
    val SettingsItemPaddingStartIcon = 12.dp
    val SettingsItemPaddingStartText = 24.dp
    val SettingsItemPaddingEnd = 28.dp
    val SettingsItemPaddingVerticalIcon = 12.dp
    val SettingsItemPaddingVerticalText = 14.dp
    const val SettingsItemFocusScale = 1.05f
    const val SettingsItemPressedScale = 1.025f
    const val SettingsItemFocusPivotX = 0.72f
    val SettingsItemFocusElevation = 12.dp
    val SettingsItemPressedElevation = 6.dp
    const val SettingsFocusInMs = 320
    const val SettingsPressMs = 160
    const val SettingsPanelSlideMs = 450
    const val SettingsBackQueueMs = 200L
    const val SettingsPanelAlphaMs = 90
    const val SettingsPanelColorMs = 225
    const val SettingsPreviewFadeMs = 225
    const val SettingsPreviewInactiveAlpha = 0.6f
    val SettingsDecorTitleTop = 68.dp
    val SettingsCategoryHeight = 36.dp
    val SettingsTextIndent = 20.dp
    val SettingsSwitchTrackWidth = 40.dp
    val SettingsSwitchTrackHeight = 24.dp
    val SettingsControlBox = 32.dp
    val SettingsListTopPadding = 24.dp

    // Card focus: 4 px ring, 5 px offset, scale 1.05
    val FocusRingWidth = 2.dp
    val FocusRingOffset = 2.5.dp
    const val CardFocusScale = 1.05f

    // Progress bar (6 px)
    val ProgressHeight = 3.dp

    // Side panel (680 px) and dialog
    val PanelWidth = 340.dp
    val PanelTopPadding = 28.dp
    val PanelHorizontalPadding = 16.dp
    val DialogWidth = 400.dp
    val DialogHorizontalPadding = 26.dp
    val DialogVerticalPadding = 24.dp

    // Details (movie / series), mockup px / 2. The page is one column that is translated up by
    // DetailsHeroHeight - DetailsCollapsedTabsTop when focus moves from the hero into a tab's content.
    val DetailsHeroHeight = 385.dp
    val DetailsTextTop = 75.dp
    val DetailsTextWidth = 560.dp
    val DetailsDescriptionWidth = 430.dp
    val DetailsEyebrowGap = 6.dp
    val DetailsMetaTop = 12.dp
    /** Title line box: mockup 104 px font with line-height 1. */
    val DetailsTitleLineHeight = 42.dp
    val DetailsDescriptionTop = 12.dp
    val DetailsActionsBottom = 37.dp
    val DetailsActionGap = 8.dp
    val DetailsNoticeGap = 8.dp
    val DetailsNoticeHeight = 30.dp
    val DetailsNoticeRadius = 8.dp
    val DetailsNoticeHorizontalPadding = 12.dp
    val DetailsNoticeIconSize = 14.dp
    val DetailsNoticeIconGap = 8.dp
    val DetailsNoticeMaxWidth = 440.dp
    val DetailsStarSize = 12.dp
    val DetailsMetaGap = 7.dp
    val DetailsTabHeight = 30.dp
    val DetailsTabHorizontalPadding = 13.dp
    val DetailsTabGap = 4.dp
    val DetailsTabUnderlineHeight = 2.dp
    val DetailsTabUnderlineBottom = 1.dp
    val DetailsTabsToDivider = 10.dp
    val DetailsDividerToContent = 15.dp
    val DetailsCollapsedHeaderTop = 28.dp
    val DetailsCollapsedHeaderGap = 11.dp
    val DetailsCollapsedTabsTop = 64.dp
    val DetailsContentStart = 48.dp
    val DetailsContentEnd = 36.dp
    /** Tab content sits 40 px closer to the tabs in the top state than in the collapsed one (mockup MovieTop). */
    val DetailsTopStateContentLift = 20.dp
    // Loading placeholder blocks (mockup "Ładowanie szczegółów")
    val DetailsSkeletonEyebrowWidth = 120.dp
    val DetailsSkeletonMetaWidth = 257.dp
    val DetailsSkeletonTitleWidth = 343.dp
    val DetailsSkeletonTitleHeight = 48.dp
    val DetailsSkeletonLineHeight = 12.dp
    val DetailsSkeletonTabHeight = 15.dp
    val DetailsSkeletonTabGap = 20.dp
    val DetailsSkeletonDescriptionWidths = listOf(428.dp, 411.dp, 308.dp)
    val DetailsSkeletonActionWidths = listOf(100.dp, 110.dp, 100.dp, 110.dp)
    val DetailsSkeletonTabWidths = listOf(103.dp, 103.dp, 69.dp)
    const val DetailsDimAlpha = 0.88f
    const val DetailsDescriptionLines = 3
    const val DetailsDescriptionLinesWithNotice = 2
    // Backdrop: movie image on the right 610x540, series 680x410 (mockup 1220x1080 / 1360x820)
    val DetailsMovieBackdropStart = 350.dp
    val DetailsMovieBackdropWidth = 610.dp
    val DetailsMovieBackdropHeight = 540.dp
    val DetailsMovieFadeTop = 320.dp
    val DetailsSeriesBackdropStart = 280.dp
    val DetailsSeriesBackdropWidth = 680.dp
    val DetailsSeriesBackdropHeight = 410.dp
    val DetailsSeriesFadeTop = 280.dp
    // Cast and crew (mockup card 184x240, gap 28 px)
    val CastCardWidth = 92.dp
    val CastCardHeight = 120.dp
    val CastGap = 14.dp
    val CastNameTop = 10.dp
    val CastRingInset = 8.dp
    /** Space under the last row of the More like this grid so its focus ring keeps clear of the screen edge. */
    val SimilarGridBottomInset = 24.dp
    /** Fade under the "Similar to" header so scrolled posters are not cut in half. */
    val SimilarTopFadeHeight = 14.dp
    // "Details" tab (mockup: left column x=96, right grid x=1120 w=704)
    val InfoColumnWidth = 450.dp
    val InfoGridStart = 560.dp
    val InfoGridWidth = 352.dp
    val InfoGridColumnGap = 24.dp
    val InfoGridRowGap = 20.dp
    val InfoLabelValueGap = 4.dp
    val InfoSectionGap = 22.dp
    val InfoChipGap = 6.dp
    val InfoChipHorizontalPadding = 11.dp
    val InfoChipVerticalPadding = 5.dp
    val InfoContentTop = 10.dp
    // Episodes tab: seasons column 150 dp, row 100 dp, still 150x84.5 dp (mockup 300x200 / 300x169)
    val SeasonColumnWidth = 150.dp
    val SeasonRowHeight = 42.dp
    val SeasonRowGap = 4.dp
    val SeasonSeparatorStart = 210.dp
    val EpisodeListStart = 222.dp
    val EpisodeListEnd = 36.dp
    val EpisodeRowHeight = 100.dp
    val EpisodeRowGap = 6.dp
    val EpisodeRowRadius = 10.dp
    val EpisodeRowPadding = 8.dp
    val EpisodeRowContentGap = 16.dp
    val EpisodeStillWidth = 150.dp
    val EpisodeStillHeight = 84.5.dp
    val EpisodeStillRadius = 6.dp
    val EpisodeBadgeSize = 18.dp
    val EpisodeBadgeInset = 5.dp
    val EpisodeBadgeIcon = 11.dp
    val EpisodeMetaTop = 3.dp
    val EpisodeDescriptionTop = 5.dp
    val EpisodeActionSize = 32.dp
    val EpisodeActionIcon = 14.dp
    val EpisodeActionGap = 8.dp
    val EpisodeActionsWidth = 140.dp
    val EpisodeProgressLabelGap = 4.dp
    val EpisodeUpcomingBottom = 8.dp
    val EpisodeUpcomingHeight = 34.dp
    const val SeasonSelectDebounceMs = 150L
    val ActionRingSize = 13.dp
    val ActionRingStroke = 1.5.dp

    // Unavailable title (mockup state: poster 62x93 / 560 px mini => 106x159 dp)
    val UnavailableTop = 69.dp
    val UnavailablePosterWidth = 106.dp
    val UnavailablePosterHeight = 159.dp
    val UnavailablePosterGap = 31.dp
    val UnavailableTextWidth = 560.dp
    val UnavailableTitleTop = 6.dp
    val UnavailableHeadingTop = 26.dp
    val UnavailableActionsTop = 28.dp

    // Search (mockup Search: field x=96 y=124 1100x76 px, results header y=240, error row 1100x84 px)
    val SearchFieldTop = 28.dp
    val SearchFieldWidth = 524.dp
    val SearchFieldHeight = 38.dp
    val SearchFieldIconSize = 15.dp
    val SearchFieldGap = 9.dp
    val SearchFieldHorizontalPadding = 15.dp
    /** Room above/below a result row so the focus ring and scale of its posters are not clipped. */
    val SearchRowVerticalPadding = 10.dp
    val SearchErrorRowWidth = 550.dp
    val SearchErrorRowHeight = 42.dp
    val SearchErrorRowIconSize = 15.dp
    val SearchErrorRowGap = 9.dp
    val SearchErrorRowHorizontalPadding = 13.dp
    // Library (mockup Library: source row y=118 px, "LISTS FROM" label 20 px/700)
    val LibrarySourceRowTopGap = 4.dp
    val LibrarySourceRowHeight = 28.dp
    /** Library chips are 52 px in the mockup (Home: 56 px). */
    val LibraryChipHeight = 26.dp
    /** Gap from the source chips to the first list header (mockup 15 dp; later lists keep FeedPinnedTop). */
    val LibraryFirstFeedPinnedTop = 15.dp
    val LibrarySourceChipMaxWidth = 220.dp
    val LibrarySourceLabelGap = 10.dp
    val LibrarySourceChipGap = 7.dp
    val LibraryHeaderSkeletonWidth = 188.dp
    val LibraryHeaderSkeletonHeight = 17.dp
    val LibraryStateTop = 167.dp

    // Downloads (mockup Downloads: rows 150 px, thumb 208x117 px, bar 520x8 px, actions column 360 px)
    val DownloadsContentTopGap = 12.dp
    val DownloadsSectionHeaderHeight = 15.dp
    val DownloadsSectionGap = 12.dp
    val DownloadRowHeight = 75.dp
    val DownloadRowGap = 5.dp
    val DownloadRowRadius = 9.dp
    val DownloadRowHorizontalPadding = 8.dp
    val DownloadRowContentGap = 14.dp
    val DownloadRowRingWidth = 1.5.dp
    val DownloadThumbWidth = 104.dp
    val DownloadThumbHeight = 58.5.dp
    val DownloadThumbRadius = 5.dp
    val DownloadMetaTopGap = 2.dp
    val DownloadDescriptionTopGap = 4.dp
    val DownloadProgressTopGap = 7.dp
    val DownloadProgressGap = 8.dp
    val DownloadProgressWidth = 260.dp
    val DownloadProgressHeight = 4.dp
    val DownloadActionsWidth = 180.dp
    val DownloadActionsGap = 6.dp
    val DownloadActionHeight = 28.dp
    /** Empty state block sits ~50 dp above the screen centre (mockup DownloadsStates). */
    val DownloadsEmptyStateLift = 100.dp
    val DownloadsBackdropWidth = 580.dp
    val DownloadsBackdropHeight = 326.dp
    const val DownloadsBackdropAlpha = 0.35f

    /** Start / no-results text block, measured below the 72 dp top bar: title at y=441 px of the screen. */
    val SearchStateTop = 148.dp
    val SearchListBottomPadding = 28.dp
    val SearchSkeletonCount = 6
    val PosterPlaceholderPadding = 10.dp

    // Player (mockups Player*; px / 2)
    val PlayerSidePadding = 48.dp
    val PlayerControlsBottomPadding = 35.dp
    val PlayerOptionHeight = 30.dp
    val PlayerOptionPaddingStart = 9.dp
    val PlayerOptionPaddingEnd = 11.dp
    val PlayerOptionGap = 5.dp
    val PlayerOptionIconSize = 12.dp
    val PlayerOptionSpacing = 6.dp
    /** Play/Pause pill: fixed width fits both "Odtwórz" and "Wstrzymaj" (mockup 181 px for the longer one). */
    val PlayerPlayPauseWidth = 92.dp
    val PlayerTimelineHeight = 20.dp
    val PlayerTimelineTrack = 4.dp
    val PlayerTimelineTrackFocused = 6.dp
    val PlayerTimelineThumb = 10.dp
    val PlayerTimelineThumbFocused = 16.dp
    val PlayerTimelineGap = 12.dp
    val PlayerTimelineToControlsGap = 25.dp
    val PlayerGradientHeight = 300.dp
    /** Title block (mockup PlayerControls): 96 px from the left, 72 px from the top, max 1060 px wide. */
    val PlayerTitleTopPadding = 36.dp
    val PlayerTitleMaxWidth = 530.dp
    /** Top mask behind the title block: 340 px high, black 78% fading to 0. */
    val PlayerTopGradientHeight = 170.dp
    /** Timeline time labels: reserved width so the bar does not move when the duration appears. */
    val PlayerTimeLabelMinWidth = 72.dp
    /** Custom subtitle-delay dialog (mockup PlayerErrors): 583 dp wide, 96 dp from the top. */
    val PlayerDelayDialogWidth = 583.dp
    val PlayerDelayDialogTop = 96.dp
    /** Notice stack: 560 px wide, 12 px gaps, card radius 20 px, padding 24 px, icon 36 px. */
    val PlayerNoticeWidth = 280.dp
    val PlayerNoticeGap = 6.dp
    val PlayerNoticeRadius = 10.dp
    val PlayerNoticePadding = 12.dp
    val PlayerNoticeIconSize = 18.dp
    val PlayerNoticeIconGap = 9.dp
    /** Loading screen: status row top (876 px) and its height (to the skip button at 950 px). */
    val PlayerLoadingStatusTop = 438.dp
    val PlayerLoadingStatusHeight = 37.dp
    val PlayerPanelRowHeight = 42.dp
    val PlayerSourceRowHeight = 48.dp
    val PlayerPanelOptionRowHeight = 34.dp
    val PlayerPanelTopPadding = 24.dp
    val PlayerLoadingGradientHeight = 330.dp
    val PlayerSyncPanelWidth = 590.dp
    val PlayerSyncPanelPadding = 20.dp
    val PlayerSyncListWidth = 310.dp
    val PlayerCardWidth = 450.dp
    val PlayerCardRadius = 12.dp
    val PlayerCardPadding = 14.dp
    val PlayerCardPosterWidth = 160.dp
    val PlayerCardPosterHeight = 90.dp

    /** Focus/transform animations never run longer than this. */
    const val FocusAnimationMs = 120
    const val MaxAnimationMs = 150
    /** Nav drawer (handoff 2.3): expand 250 ms ease-out, collapse 200 ms ease-in. */
    const val DrawerExpandMs = 250
    const val DrawerCollapseMs = 200
}

object TvShapes {
    val Card = RoundedCornerShape(TvDimens.CardRadius)
    val Row = RoundedCornerShape(TvDimens.RowRadius)
    val PanelRow = RoundedCornerShape(TvDimens.PanelRowRadius)
    val Dialog = RoundedCornerShape(TvDimens.DialogRadius)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Text roles from the mockups (px / 2 = sp). */
@Suppress("DEPRECATION")
object TvTextStyles {
    private fun style(size: Float, weight: FontWeight, lineHeight: Float? = null) = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight?.sp ?: TextStyle.Default.lineHeight,
    )

    /** Mockup: line-height 1, letter-spacing -0.01em. */
    val Hero = style(48f, FontWeight.ExtraBold, lineHeight = 48f).copy(
        letterSpacing = (-0.01).em,
        // Without this Compose adds the font's top/bottom padding to the first/last line (+19 px).
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    /** Feed header: title 36 px/700 and source 22 px/500; focused poster title 26/700 + meta 24/500. */
    val FeedFocusedTitle = style(13f, FontWeight.Bold)
    val FeedFocusedMeta = style(12f, FontWeight.Medium)
    val MetaLarge = style(12f, FontWeight.Medium)
    val FeaturedBody = style(12f, FontWeight.Normal, lineHeight = 18f)
    val FeaturedSupporting = style(10.5f, FontWeight.Normal)
    val HeroEyebrow = style(11f, FontWeight.Bold).copy(letterSpacing = 0.12.em)
    val HeroMeta = style(13f, FontWeight.Medium)
    val HeroSectionTitle = style(14f, FontWeight.Bold)
    val Badge = style(10f, FontWeight.Bold)
    val Tile = style(9f, FontWeight.ExtraBold)
    val Title = style(36f, FontWeight.ExtraBold)
    val Level = style(24f, FontWeight.ExtraBold)
    val PanelTitle = style(19f, FontWeight.Bold)
    /** Player panels (mockup 36 px/700), crumb 21 px/600, row title 25 px/600, meta 20 px. */
    val PlayerPanelTitle = style(18f, FontWeight.Bold)
    val PlayerPanelCrumb = style(10.5f, FontWeight.SemiBold)
    val PlayerPanelRow = style(12.5f, FontWeight.SemiBold)
    val PlayerPanelMeta = style(10f, FontWeight.Normal)
    val PlayerPanelOption = style(12f, FontWeight.Medium)
    /** Controls: time 24 px/600 (tabular), episode line 26 px/600, info line 22 px/500. */
    val PlayerTime = style(12f, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum")
    val PlayerEpisode = style(13f, FontWeight.SemiBold)
    val PlayerInfo = style(11f, FontWeight.Medium)
    /** Sync panel: delay 72 px/800, cue timestamp 18 px/500, cue text 23 px/500, action 19 px/600. */
    val PlayerSyncValue = style(36f, FontWeight.ExtraBold).copy(fontFeatureSettings = "tnum")
    val PlayerSyncTimestamp = style(9f, FontWeight.Medium).copy(fontFeatureSettings = "tnum")
    val PlayerSyncCue = style(11.5f, FontWeight.Medium, lineHeight = 15.5f)
    val PlayerSyncAction = style(9.5f, FontWeight.SemiBold)
    val Section = style(18f, FontWeight.Bold)
    /** Settings level crumb 22 px/600, section label 19 px/700 (+.12em), preview rows 25 px/20 px. */
    val SettingsDecorTitle = style(28f, FontWeight.Normal)
    val SettingsDecorSubtitle = style(14f, FontWeight.Normal)
    val SettingsItemTitle = style(14f, FontWeight.Medium).copy(letterSpacing = 0.01.em)
    val SettingsItemSummary = style(12f, FontWeight.Normal).copy(letterSpacing = 0.01.em)
    val SettingsCategory = style(11f, FontWeight.Medium).copy(letterSpacing = 0.07.em)
    val SettingsInfo = style(14f, FontWeight.Normal)
    val SettingsPreviewHeader = style(18f, FontWeight.Bold)
    val SettingsPreviewRow = style(12.5f, FontWeight.SemiBold)
    val SettingsPreviewSubtitle = style(10f, FontWeight.Normal)
    val SettingsPreviewSection = style(9f, FontWeight.Bold).copy(letterSpacing = 0.12.em)
    val SettingsTile = style(12f, FontWeight.Bold)
    val SettingsCode = style(22f, FontWeight.ExtraBold).copy(letterSpacing = 0.12.em)
    val SettingsLabelSmall = style(9.5f, FontWeight.SemiBold).copy(letterSpacing = 0.1.em)
    /** Settings rows (mockup 28 px). */
    val Row = style(14f, FontWeight.SemiBold)
    /** Rows in Components / side panels (mockup 26 px). */
    val RowPanel = style(13f, FontWeight.SemiBold)
    val ShowMoreTitle = style(14f, FontWeight.Bold)
    val StateTitle = style(22f, FontWeight.ExtraBold)
    val StateMessage = style(15f, FontWeight.Normal, lineHeight = 22.5f)
    val SearchField = style(14f, FontWeight.Medium)
    /** Title shown on a poster that has no image. */
    val PosterPlaceholderTitle = style(13f, FontWeight.SemiBold, lineHeight = 17f)
    val SearchFieldSummary = style(10.5f, FontWeight.Medium)
    val SearchErrorRow = style(12f, FontWeight.Medium)
    val HeroFeatured = style(40f, FontWeight.ExtraBold, lineHeight = 40f).copy(
        letterSpacing = (-0.01).em,
        // Without this Compose adds the font's top/bottom padding to the first/last line (+19 px).
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val HeroPlayer = style(26f, FontWeight.ExtraBold)
    val HeroSeries = style(22f, FontWeight.ExtraBold)
    val EpisodeTitle = style(15f, FontWeight.Bold)
    val DownloadTitle = style(14f, FontWeight.Bold)
    val DownloadMeta = style(10.5f, FontWeight.Medium)
    val DownloadDescription = style(10f, FontWeight.Normal)
    val PlayerOption = style(11f, FontWeight.SemiBold)
    /** Notices: title 22 px Bold, text 20 px Medium (mockup PlayerNotices). */
    val PlayerNoticeTitle = style(11f, FontWeight.Bold)
    val PlayerNoticeText = style(10f, FontWeight.Medium, lineHeight = 13f)
    val PlayerValue = style(11f, FontWeight.Medium)
    val ButtonLarge = style(13f, FontWeight.SemiBold)
    val RowSubtitle = style(11f, FontWeight.Normal)
    val Body = style(13f, FontWeight.Normal, lineHeight = 19.5f)
    val Meta = style(11f, FontWeight.Medium)
    val PanelGroupHeader = style(9.5f, FontWeight.Bold).copy(letterSpacing = 0.12.em)
    val Button = style(12f, FontWeight.SemiBold)
    /** Chips are only 26-28 dp tall: without font padding trimmed the text is clipped at the bottom. */
    val Chip = style(11f, FontWeight.Medium, lineHeight = 14f).copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val Tab = style(14f, FontWeight.SemiBold)
    val Eyebrow = style(10f, FontWeight.Bold).copy(letterSpacing = 0.12.em)
    val DialogTitle = style(20f, FontWeight.ExtraBold)
    /** Dialog body: mockup 26 px, line height 1.45. */
    val DialogMessage = style(13f, FontWeight.Normal, lineHeight = 18.85f)
    val DialogBody = style(15f, FontWeight.Normal, lineHeight = 22.5f)
    val Caption = style(9f, FontWeight.Medium)

    /** Details hero: eyebrow 22 px/600 uppercase +.06em, title 104 px/800 -.015em, meta 26 px/500. */
    val DetailsEyebrow = style(11f, FontWeight.SemiBold).copy(letterSpacing = 0.06.em)
    val DetailsTitle = style(42f, FontWeight.ExtraBold, lineHeight = 42f).copy(
        letterSpacing = (-0.015).em,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val DetailsMeta = style(13f, FontWeight.Medium)
    val DetailsNotice = style(12f, FontWeight.Medium)
    /** Cast card: name 22 px/600, role 20 px/400. */
    val CastName = style(11f, FontWeight.SemiBold)
    val CastRole = style(10f, FontWeight.Normal)
    /** "Details" tab: label 20 px/600 uppercase +.1em, value 26 px/500, story 30 px/400 lh 1.55. */
    val InfoLabel = style(10f, FontWeight.SemiBold).copy(letterSpacing = 0.1.em)
    val InfoValue = style(13f, FontWeight.Medium, lineHeight = 17.5f)
    val InfoStory = style(15f, FontWeight.Normal, lineHeight = 23.25f)
    /** Episode row: meta 22 px/500, description 22 px/400 lh 1.45. */
    val EpisodeMeta = style(11f, FontWeight.Medium)
    val EpisodeDescription = style(11f, FontWeight.Normal, lineHeight = 16f)
    val SeasonTitle = style(13.5f, FontWeight.SemiBold)
}
