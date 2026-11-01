package com.lagradost.cloudstream3.phone.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.tv.compat.home.ratingLabelOrNull
import com.lagradost.cloudstream3.designsystem.CsProgressBar
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneOnAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText

/** Pill button: [primary] uses the title accent, otherwise a translucent fill. */
@Composable
fun CsButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
    height: Dp = if (primary) PhoneDimens.PrimaryButtonHeight else PhoneDimens.ButtonHeight,
) {
    val bg = if (primary) LocalPhoneAccent.current else PhoneColors.FillButton
    val fg = if (primary) LocalPhoneOnAccent.current else PhoneColors.Text
    Row(
        modifier = modifier
            .height(height)
            .clip(PhoneShapes.Pill)
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
        Text(label, style = PhoneText.Button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = PhoneColors.FillButton,
    size: Dp = PhoneDimens.IconButton,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(PhoneShapes.Pill)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = PhoneColors.Text, modifier = Modifier.size(22.dp))
    }
}

/** "Selected = white fill" rule shared with TV chips. */
@Composable
fun CsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = PhoneShapes.Pill
    Box(
        modifier = modifier
            .height(PhoneDimens.ChipHeight)
            .clip(shape)
            .then(
                if (selected) Modifier.background(PhoneColors.Text)
                else Modifier.border(BorderStroke(1.dp, PhoneColors.Outline), shape)
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = PhoneText.Chip,
            color = if (selected) PhoneColors.Ground else PhoneColors.Body,
            maxLines = 1,
        )
    }
}

@Composable
fun CsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.padding(start = PhoneDimens.ScreenPadding, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        action?.invoke(this)
    }
}

@Composable
fun CsKicker(text: String, modifier: Modifier = Modifier, color: Color = PhoneColors.Label) {
    Text(text.uppercase(), style = PhoneText.Kicker, color = color, modifier = modifier)
}

/** Poster with the title below it; touch-first (no focus ring / scale as on TV). */
@Composable
fun CsPosterCard(
    title: String,
    posterUri: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = PhoneDimens.PosterWidth,
    height: Dp? = PhoneDimens.PosterHeight,
    meta: String? = null,
) {
    Column(modifier = modifier.width(width).clickable(role = Role.Button, onClick = onClick)) {
        // A null height means "2:3 of the cell width" (grid cells).
        CsPosterImage(posterUri, title, if (height != null) Modifier.size(width, height) else Modifier.fillMaxWidth().aspectRatio(2f / 3f))
        Text(
            title,
            style = PhoneText.PosterTitle,
            color = PhoneColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!meta.isNullOrEmpty()) {
            Text(meta, style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CsPosterImage(uri: String, title: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(PhoneShapes.Poster).background(PhoneColors.Placeholder)) {
        if (uri.isNotBlank()) {
            AsyncImage(
                model = uri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun CsProgress(progress: Float, modifier: Modifier = Modifier) {
    CsProgressBar(
        progress = progress,
        accent = LocalPhoneAccent.current,
        track = PhoneColors.ProgressTrack,
        height = PhoneDimens.ProgressHeight,
        modifier = modifier,
    )
}

/** "2026 • ★ 7.8" line under a poster; empty when the item has neither. */
fun mediaMeta(item: com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat): String {
    val year = when (item) {
        is com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat.Movie -> item.year
        is com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat.TvSeries -> item.year
        else -> null
    }
    val rating = item.ratingLabelOrNull()
    return listOfNotNull(year?.toString(), rating?.let { "★ $it" }).joinToString(" • ")
}

/** True when the menu is permanently visible (tablet): screens then drop their hamburger button. */
val LocalPermanentMenu = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Height of the system navigation bar: added to the end of scrolling content so every item can be scrolled above it. */
@androidx.compose.runtime.Composable
fun navBarBottom(): androidx.compose.ui.unit.Dp =
    androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
