package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagradost.cloudstream3.tv.presentation.screens.player.core.PlayerSessionController

@Composable
internal fun PlayerRoute(
    onBackPressed: () -> Unit,
    tvPlayerScreenViewModel: TvPlayerScreenViewModel,
) {
    val context = LocalContext.current
    val seekPreferencesState = rememberTvPlayerSeekPreferencesState()
    val playerSessionController = remember(context) {
        PlayerSessionController(context)
    }
    val uiState = tvPlayerScreenViewModel.uiState.collectAsStateWithLifecycle().value
    val catalogState = tvPlayerScreenViewModel.catalogUiState.collectAsStateWithLifecycle().value
    val panelsState = tvPlayerScreenViewModel.panelsUiState.collectAsStateWithLifecycle().value

    DisposableEffect(playerSessionController) {
        onDispose {
            playerSessionController.release()
        }
    }

    val subtitleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        tvPlayerScreenViewModel.onSubtitleFileSelected(uri)
    }
    val actions = rememberPlayerScreenActions(
        onBackPressed = onBackPressed,
        tvPlayerScreenViewModel = tvPlayerScreenViewModel,
        onOpenSubtitleFilePicker = {
            subtitleFilePickerLauncher.launch(
                arrayOf(
                    "text/plain",
                    "text/str",
                    "application/octet-stream",
                    androidx.media3.common.MimeTypes.TEXT_UNKNOWN,
                    androidx.media3.common.MimeTypes.TEXT_VTT,
                    androidx.media3.common.MimeTypes.TEXT_SSA,
                    androidx.media3.common.MimeTypes.APPLICATION_TTML,
                    androidx.media3.common.MimeTypes.APPLICATION_MP4VTT,
                    androidx.media3.common.MimeTypes.APPLICATION_SUBRIP,
                )
            )
        },
    )

    PlayerScreen(
        uiState = uiState,
        catalogState = catalogState,
        panelsState = panelsState,
        seekPreferencesState = seekPreferencesState,
        actions = actions,
        panelEffects = tvPlayerScreenViewModel.panelEffects,
        playerSessionController = playerSessionController,
    )
}
