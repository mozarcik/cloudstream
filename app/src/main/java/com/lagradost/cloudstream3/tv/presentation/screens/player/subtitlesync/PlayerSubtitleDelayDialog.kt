package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvButtonVariant
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusTrap
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** "Custom value…" of the sync panel (mockup PlayerErrors, third board): delay in ms, OK opens the keyboard. */
@Composable
internal fun PlayerSubtitleDelayDialog(
    initialDelayMs: Long,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialDelayMs.toString()) }
    var fieldFocused by remember { mutableStateOf(false) }
    val fieldFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        repeat(20) {
            if (fieldFocusRequester.requestFocus()) return@LaunchedEffect
            kotlinx.coroutines.delay(16)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(TvColors.DialogScrim),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Mockup PlayerErrors: 340 of 560 px wide (583 dp), top 56 of 315 px (96 dp): above the middle.
            Column(
                modifier = Modifier
                    .padding(top = TvDimens.PlayerDelayDialogTop)
                    .width(TvDimens.PlayerDelayDialogWidth)
                    .tvFocusTrap()
                    .background(TvColors.DialogSurface, TvShapes.Dialog)
                    .padding(horizontal = TvDimens.DialogHorizontalPadding, vertical = TvDimens.DialogVerticalPadding),
            ) {
                Text(
                    text = stringResource(R.string.subtitle_offset_title),
                    style = TvTextStyles.DialogTitle,
                    color = TvColors.Text,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(TvColors.PlayerFieldBackground, TvShapes.PanelRow)
                        .then(if (fieldFocused) Modifier.border(1.dp, TvColors.Focus, TvShapes.PanelRow) else Modifier)
                        .padding(horizontal = 12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { value -> text = value.filter { it.isDigit() || it == '-' }.take(8) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TvTextStyles.PlayerSyncValue.copy(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp), color = TvColors.Text),
                        cursorBrush = SolidColor(TvColors.Text),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(fieldFocusRequester)
                            .onFocusChanged { fieldFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp && event.key == Key.DirectionCenter) {
                                    keyboard?.show()
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                    Text(text = "ms", style = TvTextStyles.PlayerValue, color = TvColors.LabelPanel)
                }
                Text(
                    text = stringResource(R.string.tv_player_sync_custom_hint),
                    style = TvTextStyles.PlayerPanelMeta,
                    color = TvColors.RowSubtitle,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.DialogButtonGap, Alignment.End),
                    modifier = Modifier.fillMaxWidth().padding(top = TvDimens.DialogBodyButtonsGap),
                ) {
                    TvButton(text = stringResource(R.string.cancel), onClick = onDismiss)
                    TvButton(
                        text = stringResource(R.string.tv_player_sync_apply),
                        onClick = { text.toLongOrNull()?.let(onApply) ?: onDismiss() },
                        variant = TvButtonVariant.Default,
                    )
                }
            }
        }
    }
}
