package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** Level change: a plain crossfade; the page already moved when entering Settings, so no sliding here. */
private const val PanelAnimationDurationMs = TvDimens.MaxAnimationMs
private const val PreviewFadeDurationMs = TvDimens.FocusAnimationMs

@Suppress("UNUSED_PARAMETER")
fun settingsPanelTransition(direction: SettingsNavigationDirection): ContentTransform {
    return fadeIn(animationSpec = tween(PanelAnimationDurationMs)) togetherWith
        fadeOut(animationSpec = tween(PanelAnimationDurationMs))
}

fun settingsPreviewFadeTransition(): ContentTransform {
    return fadeIn(animationSpec = tween(PreviewFadeDurationMs)) togetherWith
        fadeOut(animationSpec = tween(PreviewFadeDurationMs))
}
