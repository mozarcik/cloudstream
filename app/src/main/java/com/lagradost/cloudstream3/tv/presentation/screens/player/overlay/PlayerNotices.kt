package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay

/** Kind of a player notice: each kind has its own icon (mockup PlayerNotices), not only its own color. */
enum class PlayerNoticeKind { SourceFailed, SubtitleFailed }

data class PlayerNotice(
    val key: String,
    val kind: PlayerNoticeKind,
    val title: String,
    val description: String,
    val generation: Long,
)

/**
 * Stack of player notices (max 3, newest at the bottom, each shown for [PlayerNoticeLifetimeMs]).
 * Lives in the view model, so a notice sent right before a link change is not lost.
 */
@Stable
class PlayerNoticeStack {
    val items = mutableStateListOf<PlayerNotice>()
    private var generation = 0L

    /** The same notice (kind, title, description) does not duplicate; it only refreshes its timer. */
    fun push(kind: PlayerNoticeKind, title: String, description: String) {
        generation += 1
        val key = "$kind|$title|$description"
        val index = items.indexOfFirst { it.key == key }
        if (index >= 0) {
            items[index] = items[index].copy(generation = generation)
            return
        }
        items.add(PlayerNotice(key, kind, title, description, generation))
        while (items.size > PlayerNoticeMaxCount) {
            items.removeAt(0)
        }
    }

    fun expire(notice: PlayerNotice) {
        items.removeAll { it.key == notice.key && it.generation == notice.generation }
    }

    companion object {
        const val PlayerNoticeMaxCount = 3
        const val PlayerNoticeLifetimeMs = 5_000L
    }
}

/** Top-right stack (mockup PlayerNotices): 560 px wide, 96 px from the right, 72 px from the top, 12 px gaps. */
@Composable
internal fun PlayerNoticeLayer(
    stack: PlayerNoticeStack,
    modifier: Modifier = Modifier,
) {
    if (stack.items.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.PlayerNoticeGap),
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(top = TvDimens.PlayerTitleTopPadding, end = TvDimens.PlayerSidePadding),
    ) {
        stack.items.forEach { notice ->
            androidx.compose.runtime.key(notice.key) {
                LaunchedEffect(notice.generation) {
                    delay(PlayerNoticeStack.PlayerNoticeLifetimeMs)
                    stack.expire(notice)
                }
                PlayerNoticeCard(notice = notice)
            }
        }
    }
}

@Composable
private fun PlayerNoticeCard(notice: PlayerNotice) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.PlayerNoticeIconGap),
        modifier = Modifier
            .width(TvDimens.PlayerNoticeWidth)
            .background(TvColors.PlayerNoticeCard, RoundedCornerShape(TvDimens.PlayerNoticeRadius))
            .border(1.dp, TvColors.PlayerNoticeBorder, RoundedCornerShape(TvDimens.PlayerNoticeRadius))
            .padding(TvDimens.PlayerNoticePadding),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(TvDimens.PlayerNoticeIconSize),
        ) {
            when (notice.kind) {
                PlayerNoticeKind.SourceFailed -> Icon(
                    imageVector = PlayerIcons.Error,
                    contentDescription = null,
                    tint = TvColors.PlayerNoticeError,
                    modifier = Modifier.size(TvDimens.PlayerNoticeIconSize),
                )
                PlayerNoticeKind.SubtitleFailed -> {
                    Icon(
                        imageVector = PlayerIcons.Subtitles,
                        contentDescription = null,
                        tint = TvColors.PlayerNoticeWarning,
                        modifier = Modifier.size(TvDimens.PlayerNoticeIconSize),
                    )
                    Canvas(modifier = Modifier.size(TvDimens.PlayerNoticeIconSize)) {
                        drawLine(
                            color = TvColors.PlayerNoticeWarning,
                            start = Offset(size.width * 0.2f, size.height * 0.8f),
                            end = Offset(size.width * 0.8f, size.height * 0.2f),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notice.title,
                style = TvTextStyles.PlayerNoticeTitle,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notice.description,
                style = TvTextStyles.PlayerNoticeText,
                color = TvColors.PlayerValue,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
