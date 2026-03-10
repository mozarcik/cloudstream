package com.lagradost.cloudstream3.ui.settings

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.TvMainActivity

enum class AppLayoutMode(
    val storedValue: Int,
    @StringRes val labelResId: Int,
    val visibleInSetup: Boolean = true,
) {
    Automatic(storedValue = -1, labelResId = R.string.automatic),
    Phone(storedValue = 0, labelResId = R.string.phone_layout),
    Tv(storedValue = 1, labelResId = R.string.tv_layout),
    Emulator(storedValue = 2, labelResId = R.string.emulator_layout),
    TvMaterial(storedValue = 3, labelResId = R.string.tv_material_layout, visibleInSetup = false);

    companion object {
        fun fromStoredValue(value: Int): AppLayoutMode {
            return entries.firstOrNull { it.storedValue == value } ?: Automatic
        }
    }
}

data class AppLayoutOption(
    val value: Int,
    val label: String,
)

val AppLayoutMode.resolvedLayoutFlag: Int
    get() = when (this) {
        AppLayoutMode.Automatic -> error("Automatic layout must be resolved before reading the flag")
        AppLayoutMode.Phone -> Globals.PHONE
        AppLayoutMode.Tv -> Globals.TV
        AppLayoutMode.Emulator -> Globals.EMULATOR
        AppLayoutMode.TvMaterial -> Globals.TV_MATERIAL
    }

fun Context.selectedAppLayoutMode(): AppLayoutMode {
    val settingsManager = PreferenceManager.getDefaultSharedPreferences(this)
    val rawValue = settingsManager.getInt(
        getString(R.string.app_layout_key),
        AppLayoutMode.Automatic.storedValue,
    )
    return AppLayoutMode.fromStoredValue(rawValue)
}

fun Context.resolvedAppLayoutMode(): AppLayoutMode {
    return when (val layoutMode = selectedAppLayoutMode()) {
        AppLayoutMode.Automatic -> if (isAutoTvDevice()) AppLayoutMode.Tv else AppLayoutMode.Phone
        else -> layoutMode
    }
}

fun Context.appLayoutOptions(
    includeTvMaterial: Boolean = true,
    setupOptionsOnly: Boolean = false,
): List<AppLayoutOption> {
    return AppLayoutMode.entries
        .filterNot { !includeTvMaterial && it == AppLayoutMode.TvMaterial }
        .filterNot { setupOptionsOnly && !it.visibleInSetup }
        .map { mode ->
            AppLayoutOption(
                value = mode.storedValue,
                label = getString(mode.labelResId),
            )
        }
}

fun Context.selectedHostActivityClass(): Class<out Activity> {
    return when (resolvedAppLayoutMode()) {
        AppLayoutMode.TvMaterial -> TvMainActivity::class.java
        else -> MainActivity::class.java
    }
}

fun Context.createSelectedHostIntent(clearTask: Boolean = false): Intent {
    return Intent(this, selectedHostActivityClass()).apply {
        if (clearTask) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }
}

fun Activity.restartIntoSelectedAppLayout() {
    startActivity(createSelectedHostIntent(clearTask = true))
    finish()
}

private fun Context.isAutoTvDevice(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?
    val model = Build.MODEL.lowercase()
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        Build.MODEL.contains("AFT") ||
        model.contains("firestick") ||
        model.contains("fire tv") ||
        model.contains("chromecast")
}
