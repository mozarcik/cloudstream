package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.utils.DataStoreHelper

private val RailSeparator = Color.White.copy(alpha = 0.12f)
internal val RailScrimBrush = Brush.horizontalGradient(
    0f to TvColors.Ground.copy(alpha = 0.92f),
    1f to Color.Transparent,
)

// y offsets (dp) from the Navigation board; the rail is drawn, not laid out, so they stay exact.
private val AvatarTop = 18.dp
private val HomeTop = 98.dp
private val SearchTop = 150.dp
private val DownloadsTop = 202.dp
private val UpperSeparatorTop = 260.dp
private val ListsTop = 274.dp
private val LowerSeparatorTop = 464.dp
private val SettingsTop = 480.dp
private val SeparatorWidth = 20.dp

/**
 * Collapsed navigation: icons only, drawn over the content's left edge. It never takes focus; it
 * just shows where the user is. The menu ([TvNavDrawer]) opens with Left / Back.
 */
@Composable
internal fun TvNavRail(
    selection: TvNavSelection,
    accountRefreshToken: Int,
    modifier: Modifier = Modifier,
) {
    val account = remember(accountRefreshToken) { DataStoreHelper.getCurrentAccount() }
    val fallbackDrawable = remember(accountRefreshToken) {
        account?.let { DataStoreHelper.profileImages.getOrNull(it.defaultImageIndex) }
    }
    val selectedTarget = (selection as? TvNavSelection.Screen)?.target
    val isLibrarySelected = selection is TvNavSelection.Library

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(TvDimens.NavRailScrimWidth)
            .background(RailScrimBrush),
    ) {
        Box(
            Modifier
                .offset(x = (TvDimens.RailWidth - TvDimens.NavRailAvatarSize) / 2, y = AvatarTop)
                .size(TvDimens.NavRailAvatarSize)
                .clip(CircleShape),
        ) {
            AvatarImage(customImageUrl = account?.customImage, fallbackDrawableRes = fallbackDrawable)
        }
        RailItem(Icons.Outlined.Home, HomeTop, selectedTarget == TvNavTarget.Home)
        RailItem(Icons.Outlined.Search, SearchTop, selectedTarget == TvNavTarget.Search)
        RailItem(Icons.Outlined.FileDownload, DownloadsTop, selectedTarget == TvNavTarget.Downloads)
        RailSeparator(UpperSeparatorTop)
        RailItem(Icons.Outlined.BookmarkBorder, ListsTop, isLibrarySelected)
        RailSeparator(LowerSeparatorTop)
        RailItem(Icons.Outlined.Settings, SettingsTop, selectedTarget == TvNavTarget.Settings)
    }
}

@Composable
private fun RailItem(icon: ImageVector, top: androidx.compose.ui.unit.Dp, selected: Boolean) {
    val tileStart = (TvDimens.RailWidth - TvDimens.NavRailTileSize) / 2
    Box(
        modifier = Modifier
            .offset(x = tileStart, y = top)
            .size(TvDimens.NavRailTileSize)
            .clip(CircleShape)
            .background(if (selected) TvColors.AccentContainer else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) TvColors.OnAccentContainer else TvColors.RailIcon,
            modifier = Modifier.size(TvDimens.NavRailIconSize),
        )
    }
}

@Composable
private fun RailSeparator(top: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .offset(x = (TvDimens.RailWidth - SeparatorWidth) / 2, y = top)
            .width(SeparatorWidth)
            .size(width = SeparatorWidth, height = 1.dp)
            .background(RailSeparator),
    )
}
