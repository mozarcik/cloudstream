package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R

@Immutable
internal data class TvPlayerSeekPreferencesState(
    val seekWhenControlsHiddenMs: Long,
    val seekWhenControlsVisibleMs: Long,
)

internal interface TvPlayerPlaybackPreferencesRepository {
    fun loadSeekPreferences(): TvPlayerSeekPreferencesState
}

internal class SharedPreferencesTvPlayerPlaybackPreferencesRepository(
    private val appContext: Context,
) : TvPlayerPlaybackPreferencesRepository {
    private val preferences by lazy(LazyThreadSafetyMode.NONE) {
        PreferenceManager.getDefaultSharedPreferences(appContext)
    }

    override fun loadSeekPreferences(): TvPlayerSeekPreferencesState {
        val seekWhenControlsHiddenMs = preferences
            .getInt(appContext.getString(R.string.android_tv_interface_off_seek_key), 10)
            .toLong() * 1000L
        val seekWhenControlsVisibleMs = preferences
            .getInt(appContext.getString(R.string.android_tv_interface_on_seek_key), 30)
            .toLong() * 1000L

        return TvPlayerSeekPreferencesState(
            seekWhenControlsHiddenMs = seekWhenControlsHiddenMs,
            seekWhenControlsVisibleMs = seekWhenControlsVisibleMs,
        )
    }
}

@Composable
internal fun rememberTvPlayerPlaybackPreferencesRepository(): TvPlayerPlaybackPreferencesRepository {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        SharedPreferencesTvPlayerPlaybackPreferencesRepository(appContext)
    }
}

@Composable
internal fun rememberTvPlayerSeekPreferencesState(
    repository: TvPlayerPlaybackPreferencesRepository = rememberTvPlayerPlaybackPreferencesRepository(),
): TvPlayerSeekPreferencesState {
    return remember(repository) {
        repository.loadSeekPreferences()
    }
}
