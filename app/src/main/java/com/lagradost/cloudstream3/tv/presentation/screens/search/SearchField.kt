package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Search pill. D-pad focus lands on the pill itself (white fill); OK moves focus into the text
 * field, which opens the system keyboard. [summary] ("Searching 3 sources…") sits on the right.
 */
@Composable
internal fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    pillFocusRequester: FocusRequester,
    textFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    onPillFocusChanged: (Boolean) -> Unit,
    summary: String?,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val textFocused by interactionSource.collectIsFocusedAsState()
    var pillHasFocus by remember { mutableStateOf(false) }
    val highlighted = pillHasFocus
    val view = LocalView.current
    var isImeVisible by remember { mutableStateOf(false) }
    var wasImeVisible by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            isImeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }

    // After the IME is dismissed with Back the text field keeps focus and the IME grabs D-pad
    // down (re-opening itself), so hand focus back to the pill.
    LaunchedEffect(isImeVisible) {
        if (wasImeVisible && !isImeVisible && textFocused) {
            pillFocusRequester.requestFocus()
        }
        wasImeVisible = isImeVisible
    }

    val contentColor = if (highlighted) TvColors.OnFocus else TvColors.Text
    val secondaryColor = if (highlighted) TvColors.OnFocusSecondary else TvColors.LabelPanel

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.SearchFieldGap),
        modifier = modifier
            .width(TvDimens.SearchFieldWidth)
            .height(TvDimens.SearchFieldHeight)
            .focusRequester(pillFocusRequester)
            .focusProperties { down = downFocusRequester ?: FocusRequester.Default }
            .onFocusChanged {
                pillHasFocus = it.hasFocus
                onPillFocusChanged(it.hasFocus)
            }
            .tvFocusFill(focused = highlighted, shape = TvShapes.Pill, container = TvColors.Card)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { textFocusRequester.requestFocus() }
            .padding(horizontal = TvDimens.SearchFieldHorizontalPadding),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (highlighted) TvColors.OnFocusSecondary else TvColors.Text2,
            modifier = Modifier.size(TvDimens.SearchFieldIconSize),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                textStyle = TvTextStyles.SearchField.copy(color = contentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearchAction()
                        if (downFocusRequester != null) focusManager.moveFocus(FocusDirection.Down)
                    }
                ),
                cursorBrush = SolidColor(contentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(textFocusRequester)
                    .focusProperties { down = downFocusRequester ?: FocusRequester.Default }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                        when (keyEvent.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                onSearchAction()
                                if (downFocusRequester != null) focusManager.moveFocus(FocusDirection.Down)
                                true
                            }

                            else -> false
                        }
                    },
            )
            if (value.isBlank()) {
                Text(
                    text = stringResource(R.string.tv_search_input_placeholder),
                    style = TvTextStyles.SearchField,
                    color = if (highlighted) TvColors.DotSeparator else TvColors.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (summary != null) {
            Text(
                text = summary,
                style = TvTextStyles.SearchFieldSummary,
                color = secondaryColor,
                maxLines = 1,
            )
        }
    }
}
