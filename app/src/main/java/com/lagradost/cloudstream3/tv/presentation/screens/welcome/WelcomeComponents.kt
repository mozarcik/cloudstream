package com.lagradost.cloudstream3.tv.presentation.screens.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** Two panels side by side (mockup "Welcome"): the card with the actions and the preview with the checklist. */
@Composable
internal fun WelcomeScaffold(
    card: @Composable ColumnScope.() -> Unit,
    preview: @Composable ColumnScope.() -> Unit,
) {
    Row(Modifier.fillMaxSize().background(TvColors.Ground)) {
        Column(
            modifier = Modifier.width(WelcomeTokens.CardWidth).fillMaxHeight().background(TvColors.Card),
            content = card,
        )
        Column(
            modifier = Modifier
                .width(WelcomeTokens.PreviewWidth)
                .fillMaxHeight()
                .background(TvColors.Preview)
                .padding(
                    start = WelcomeTokens.PreviewStart,
                    end = WelcomeTokens.PreviewStart,
                    top = WelcomeTokens.PreviewTop,
                    bottom = WelcomeTokens.PreviewBottom,
                ),
            content = preview,
        )
    }
}

/** Card of a step: header, [body] (takes the free space) and the footer buttons. */
@Composable
internal fun WelcomeCardPanel(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    subtitle: String? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    progress: Float? = null,
    icon: (@Composable () -> Unit)? = null,
    /** Small text above the footer strip (only on screens whose list can scroll). */
    hint: String? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(start = WelcomeTokens.HeaderStart, end = WelcomeTokens.PreviewStart, top = WelcomeTokens.HeaderTop)) {
            icon?.let { it(); Spacer(Modifier.height(TvDimens.SettingsDp12)) }
            if (label != null) {
                Text(label, style = WelcomeText.Label, color = TvColors.Accent)
            }
            Text(title, style = WelcomeText.Title, color = TvColors.Text, maxLines = 2, overflow = WelcomeEllipsis)
            if (subtitle != null) {
                Text(subtitle, style = WelcomeText.Subtitle, color = TvColors.Text.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(TvDimens.SettingsDp12))
            if (progress != null) {
                WelcomeProgressBar(progress, Modifier.padding(top = TvDimens.SettingsDp12).width(260.dp))
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth(), content = body)
        if (footer != null) {
            // Pinned strip: the list scrolls above it, the buttons are always visible.
            Column(Modifier.fillMaxWidth().background(TvColors.Card)) {
                if (hint != null) {
                    Text(
                        hint,
                        style = WelcomeText.StepSummary,
                        color = TvColors.RowSubtitle,
                        modifier = Modifier.padding(start = WelcomeTokens.FooterStart, bottom = TvDimens.SettingsDp6),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WelcomeTokens.FooterGap),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = WelcomeTokens.FooterStart, bottom = WelcomeTokens.FooterBottom, top = TvDimens.SettingsDp2),
                    content = footer,
                )
            }
        }
    }
}

/** Explanation paragraph under the header (same inset as the header text). */
@Composable
internal fun WelcomeIntro(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TvTextStyleIntro,
        color = TvColors.Body,
        modifier = modifier.padding(start = WelcomeTokens.HeaderStart, end = WelcomeTokens.PreviewStart, top = TvDimens.SettingsDp16, bottom = TvDimens.SettingsDp12),
    )
}

private val TvTextStyleIntro = com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles.SettingsInfo

@Composable
internal fun WelcomeProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(WelcomeTokens.ProgressHeight).clip(CircleShape).background(TvColors.ProgressTrack)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(TvColors.Accent))
    }
}

/** Footer button: Back style (fill) or primary (accent); the focused one turns white like Settings rows. */
@Composable
internal fun WelcomeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background = when {
        !enabled -> TvColors.FillButtonDisabled
        focused -> TvColors.Focus
        primary -> TvColors.Accent
        else -> TvColors.FillButton
    }
    val textColor = when {
        !enabled -> TvColors.Disabled
        focused || primary -> TvColors.OnFocus
        else -> TvColors.Text
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(WelcomeTokens.FooterHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(background, WelcomeButtonShape)
            .focusProperties { canFocus = enabled }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = WelcomeTokens.ButtonHorizontalPadding),
    ) {
        Text(text, style = WelcomeText.Button, color = textColor, maxLines = 1)
    }
}

/** Cloud icon of the current build variant (as the legacy setup picks it); null when it cannot be loaded. */
@Composable
internal fun WelcomeAppIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember {
        val resId = when {
            BuildConfig.DEBUG -> R.drawable.cloud_2_gradient_debug
            BuildConfig.FLAVOR == "prerelease" -> R.drawable.cloud_2_gradient_beta
            else -> R.drawable.cloud_2_gradient
        }
        // Icons may crash on some odd Android versions (legacy comment): never take the screen down.
        runCatching { ContextCompat.getDrawable(context, resId)?.toBitmap(256, 256)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(WelcomeTokens.AppIconSize))
    }
}

/** Progress of the checklist: [done] steps are green, [current] is the accent one, the rest are outlined. */
data class WelcomeChecklist(
    val done: Set<Int>,
    val current: Int?,
    val summaries: List<String?> = List(5) { null },
)

/** Checklist and optional note of the preview panel; the part that is the same on every screen. */
@Composable
internal fun ColumnScope.WelcomePreview(checklist: WelcomeChecklist, note: String? = null) {
    val titles = listOf(
        R.string.tv_welcome_step_language,
        R.string.tv_welcome_step_repository,
        R.string.tv_welcome_step_extensions,
        R.string.tv_welcome_step_extension_languages,
        R.string.tv_welcome_step_media,
    )
    val defaults = listOf(
        R.string.tv_welcome_step_language_summary,
        R.string.tv_welcome_step_repository_summary,
        R.string.tv_welcome_step_extensions_summary,
        R.string.tv_welcome_step_extension_languages_summary,
        R.string.tv_welcome_step_media_summary,
    )
    Text(stringResource(R.string.tv_welcome_preview_label), style = WelcomeText.Label, color = TvColors.LabelPanel)
    Text(
        stringResource(R.string.tv_welcome_steps_heading),
        style = WelcomeText.PreviewHeading,
        color = TvColors.Text,
        modifier = Modifier.padding(top = TvDimens.SettingsDp4, bottom = TvDimens.SettingsDp24),
    )
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WelcomeTokens.StepRowGap)) {
        titles.forEachIndexed { index, title ->
            val number = index + 1
            WelcomeStepRow(
                number = number,
                title = stringResource(title),
                summary = checklist.summaries.getOrNull(index) ?: stringResource(defaults[index]),
                done = number in checklist.done,
                current = number == checklist.current,
            )
        }
    }
    if (note != null) {
        Text(note, style = WelcomeText.Note, color = TvColors.SettingsPreviewText)
    }
}

@Composable
private fun WelcomeStepRow(number: Int, title: String, summary: String, done: Boolean, current: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(WelcomeTokens.StepCircle)
                .then(
                    when {
                        done -> Modifier.background(TvColors.SuccessFill, CircleShape)
                        current -> Modifier.background(TvColors.Accent, CircleShape)
                        else -> Modifier.border(TvDimens.SettingsDp2, TvColors.RadioBorder, CircleShape)
                    }
                ),
        ) {
            if (done) {
                Icon(Icons.Default.Check, null, tint = TvColors.Success, modifier = Modifier.size(TvDimens.SettingsDp18))
            } else {
                Text(
                    number.toString(),
                    style = WelcomeText.StepNumber,
                    color = if (current) TvColors.OnFocus else TvColors.Label,
                )
            }
        }
        Column(Modifier.padding(start = WelcomeTokens.StepGap)) {
            Text(
                title,
                style = if (current) WelcomeText.StepTitleCurrent else WelcomeText.StepTitle,
                color = if (current || done) TvColors.Text else TvColors.Meta,
                maxLines = 1,
                overflow = WelcomeEllipsis,
            )
            Text(summary, style = WelcomeText.StepSummary, color = TvColors.RowSubtitle, maxLines = 2, overflow = WelcomeEllipsis)
        }
    }
}
