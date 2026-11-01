package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Form field drawn as a settings row (mockups SettingsAddRepo / SettingsLoginForm): icon, label and
 * the value (or a hint) underneath. The row itself takes focus; OK moves focus into the hidden
 * text field, which opens the system keyboard, and dismissing the keyboard hands focus back to
 * the row (same pattern as the Search field).
 */
@Composable
internal fun SettingsFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    idleHint: String,
    focusedHint: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    password: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    rowFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val textFocusRequester = remember { FocusRequester() }
    val textInteraction = remember { MutableInteractionSource() }
    val clickInteraction = remember { MutableInteractionSource() }
    var rowHasFocus by remember { mutableStateOf(false) }
    var textFocused by remember { mutableStateOf(false) }
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
    // After the IME is dismissed with Back the text field keeps focus; give it back to the row.
    LaunchedEffect(isImeVisible) {
        if (wasImeVisible && !isImeVisible && textFocused) {
            rowFocusRequester.requestFocus()
        }
        wasImeVisible = isImeVisible
    }

    val colors = MaterialTheme.colorScheme
    val titleColor = when {
        !enabled -> TvColors.Disabled
        rowHasFocus -> colors.inverseOnSurface
        else -> colors.onSurface
    }
    val subtitleColor = if (rowHasFocus) TvColors.OnFocusSecondary else TvColors.RowSubtitle

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsRowGap),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TvDimens.SettingsDp52)
            .focusRequester(rowFocusRequester)
            .onFocusChanged { rowHasFocus = it.hasFocus }
            .tvFocusFill(
                focused = rowHasFocus && enabled,
                shape = TvShapes.Row,
                focusedContainer = colors.inverseSurface,
            )
            .clickable(
                interactionSource = clickInteraction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) { textFocusRequester.requestFocus() }
            .padding(horizontal = TvDimens.SettingsRowHorizontalPadding),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(TvDimens.SettingsRowChevronSize),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = TvTextStyles.Row,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.padding(top = TvDimens.SettingsDp3)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    interactionSource = textInteraction,
                    textStyle = TvTextStyles.RowSubtitle.copy(color = subtitleColor),
                    cursorBrush = SolidColor(subtitleColor),
                    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(textFocusRequester)
                        .onFocusChanged { textFocused = it.isFocused },
                )
                if (value.isEmpty()) {
                    Text(
                        text = if (rowHasFocus) focusedHint else idleHint,
                        style = TvTextStyles.RowSubtitle,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
