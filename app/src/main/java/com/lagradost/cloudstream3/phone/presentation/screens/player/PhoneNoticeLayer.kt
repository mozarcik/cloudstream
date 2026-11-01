package com.lagradost.cloudstream3.phone.presentation.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerNoticeKind
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerNoticeStack
import kotlinx.coroutines.delay

/** Same notice stack as TV, drawn at phone text sizes (the TV card text is 10-11 sp, unreadable on a phone). */
@Composable
internal fun PhoneNoticeLayer(stack: PlayerNoticeStack, modifier: Modifier = Modifier) {
    if (stack.items.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(top = 16.dp, end = 76.dp),
    ) {
        stack.items.forEach { notice ->
            key(notice.key) {
                LaunchedEffect(notice.generation) {
                    delay(PlayerNoticeStack.PlayerNoticeLifetimeMs)
                    stack.expire(notice)
                }
                val accent = if (notice.kind == PlayerNoticeKind.SourceFailed) Color(0xFFFF9B8F) else Color(0xFFF4B97A)
                Column(
                    Modifier.widthIn(max = 420.dp)
                        .background(Color(0xF5171A21), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(notice.title, style = PhoneText.Button.copy(fontSize = 16.sp), color = accent)
                    if (notice.description.isNotBlank()) {
                        Text(notice.description, style = PhoneText.Row.copy(fontSize = 15.sp, lineHeight = 21.sp), color = PhoneColors.Body, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
