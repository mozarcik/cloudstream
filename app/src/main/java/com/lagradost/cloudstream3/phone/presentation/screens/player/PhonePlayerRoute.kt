package com.lagradost.cloudstream3.phone.presentation.screens.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MimeTypes
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController
import com.lagradost.cloudstream3.tv.presentation.screens.player.rememberPlayerScreenActions
import com.lagradost.cloudstream3.tv.presentation.screens.player.rememberTvPlayerSeekPreferencesState

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Phone player entry: the same view model, session controller and playback effects as TV,
 * a touch UI on top. Landscape and immersive while it is on screen.
 */
@Composable
fun PhonePlayerRoute(onBackPressed: () -> Unit, viewModel: TvPlayerScreenViewModel) {
    val context = LocalContext.current
    val seekPreferences = rememberTvPlayerSeekPreferencesState()
    val session = remember(context) { PlayerSessionController(context, handleAudioFocus = true) }
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val catalogState = viewModel.catalogUiState.collectAsStateWithLifecycle().value
    val panelsState = viewModel.panelsUiState.collectAsStateWithLifecycle().value

    DisposableEffect(session) { onDispose { session.release() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) session.pause() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Full screen like any video player: system bars hidden (a swipe from the edge shows them briefly),
    // video under the camera cutout, orientation locked to landscape while the player is open.
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(context, view) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousOrientation = activity?.requestedOrientation
        val previousCutout = window?.attributes?.layoutInDisplayCutoutMode
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.let { w ->
            WindowCompat.setDecorFitsSystemWindows(w, false)
            w.attributes = w.attributes.apply { layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        fun hideBars() { controller?.hide(WindowInsetsCompat.Type.systemBars()) }
        hideBars()
        // The system shows the bars again after a dialog, the app switcher or a permission prompt: hide them again.
        val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (hasFocus) hideBars() }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        val resumeObserver = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) hideBars() }
        lifecycleOwner.lifecycle.addObserver(resumeObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(resumeObserver)
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            // Give brightness back to the system (the brightness swipe overrides it for this window).
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; previousCutout?.let { layoutInDisplayCutoutMode = it } } }
            controller?.show(WindowInsetsCompat.Type.systemBars())
            previousOrientation?.let { activity.requestedOrientation = it }
        }
    }

    val subtitleFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        viewModel.onSubtitleFileSelected(uri)
    }
    val actions = rememberPlayerScreenActions(
        onBackPressed = onBackPressed,
        tvPlayerScreenViewModel = viewModel,
        onOpenSubtitleFilePicker = {
            subtitleFilePicker.launch(
                arrayOf(
                    "text/plain", "text/str", "application/octet-stream", MimeTypes.TEXT_UNKNOWN, MimeTypes.TEXT_VTT,
                    MimeTypes.TEXT_SSA, MimeTypes.APPLICATION_TTML, MimeTypes.APPLICATION_MP4VTT, MimeTypes.APPLICATION_SUBRIP,
                )
            )
        },
    )

    PhonePlayerScreen(
        uiState = uiState,
        catalogState = catalogState,
        panelsState = panelsState,
        seekPreferences = seekPreferences,
        actions = actions,
        panelEffects = viewModel.panelEffects,
        notices = viewModel.notices,
        session = session,
    )
}
