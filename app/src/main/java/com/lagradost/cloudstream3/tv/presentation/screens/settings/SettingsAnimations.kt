package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Animation duration for settings navigation transitions.
 */
const val SETTINGS_ANIMATION_DURATION_MS = 300

/**
 * Creates a horizontal slide transition for navigation.
 * Used when navigating deeper into settings or going back.
 * 
 * @param forward true for forward navigation (right to left), false for back (left to right)
 */
fun settingsSlideTransition(forward: Boolean): ContentTransform {
    return if (forward) {
        slideInHorizontally(
            animationSpec = tween(SETTINGS_ANIMATION_DURATION_MS),
            initialOffsetX = { fullWidth -> fullWidth }
        ) togetherWith slideOutHorizontally(
            animationSpec = tween(SETTINGS_ANIMATION_DURATION_MS),
            targetOffsetX = { fullWidth -> -fullWidth }
        )
    } else {
        slideInHorizontally(
            animationSpec = tween(SETTINGS_ANIMATION_DURATION_MS),
            initialOffsetX = { fullWidth -> -fullWidth }
        ) togetherWith slideOutHorizontally(
            animationSpec = tween(SETTINGS_ANIMATION_DURATION_MS),
            targetOffsetX = { fullWidth -> fullWidth }
        )
    }
}

/**
 * Creates a fade transition for preview panel selection changes.
 */
fun settingsFadeTransition(): ContentTransform {
    return fadeIn(animationSpec = tween(SETTINGS_ANIMATION_DURATION_MS)) togetherWith 
           fadeOut(animationSpec = tween(SETTINGS_ANIMATION_DURATION_MS))
}
