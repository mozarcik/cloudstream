package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val PanelAnimationDurationMs = 260
private const val PreviewFadeDurationMs = 140

fun settingsPanelTransition(direction: SettingsNavigationDirection): ContentTransform {
    return if (direction == SettingsNavigationDirection.Forward) {
        slideInHorizontally(
            animationSpec = tween(PanelAnimationDurationMs),
            initialOffsetX = { fullWidth -> fullWidth }
        ) + fadeIn(animationSpec = tween(PanelAnimationDurationMs)) togetherWith
            slideOutHorizontally(
                animationSpec = tween(PanelAnimationDurationMs),
                targetOffsetX = { fullWidth -> -fullWidth }
            ) + fadeOut(animationSpec = tween(PanelAnimationDurationMs))
    } else {
        slideInHorizontally(
            animationSpec = tween(PanelAnimationDurationMs),
            initialOffsetX = { fullWidth -> -fullWidth }
        ) + fadeIn(animationSpec = tween(PanelAnimationDurationMs)) togetherWith
            slideOutHorizontally(
                animationSpec = tween(PanelAnimationDurationMs),
                targetOffsetX = { fullWidth -> fullWidth }
            ) + fadeOut(animationSpec = tween(PanelAnimationDurationMs))
    }
}

fun settingsPreviewFadeTransition(): ContentTransform {
    return fadeIn(animationSpec = tween(PreviewFadeDurationMs)) togetherWith
        fadeOut(animationSpec = tween(PreviewFadeDurationMs))
}
