package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.media3.common.PlaybackException
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.reporting.AppErrorReporter
import com.lagradost.cloudstream3.reporting.HandledAppErrorContext
import com.lagradost.cloudstream3.tv.presentation.common.TvErrorUiModel
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails

internal fun playerUiErrorFromRes(resId: Int): TvErrorUiModel {
    return TvErrorUiModel(message = playerString(resId))
}

internal fun playbackErrorUiModel(error: TvPlayerPlaybackErrorDetails?): TvErrorUiModel {
    return TvErrorUiModel(message = playerString(playbackErrorResId(error)))
}

internal fun reportPlayerHandledError(
    metadata: TvPlayerMetadata,
    uiMessage: String,
    eventMessage: String,
) {
    AppErrorReporter.reportHandled(
        context = HandledAppErrorContext(
            screen = "tv_player",
            uiMessage = uiMessage,
            eventMessage = eventMessage,
            providerName = metadata.apiName.takeIf { it.isNotBlank() },
            itemTitle = metadata.title.takeIf { it.isNotBlank() },
            mediaType = if (metadata.isEpisodeBased) "episode" else "video",
        )
    )
}

internal fun playbackErrorEventMessage(error: TvPlayerPlaybackErrorDetails?): String {
    if (error == null) {
        return "TV player playback failed with unknown error"
    }

    val builder = StringBuilder("TV player playback failed: ")
        .append(error.exoErrorName)
        .append(" (")
        .append(error.exoErrorCode)
        .append(')')

    error.httpCode?.let { httpCode ->
        builder.append(" http=").append(httpCode)
    }

    error.message
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { message ->
            builder.append(" message=").append(message)
        }

    return builder.toString()
}

private fun playbackErrorResId(error: TvPlayerPlaybackErrorDetails?): Int {
    return when (error?.exoErrorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> {
            R.string.source_error
        }

        PlaybackException.ERROR_CODE_REMOTE_ERROR,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> {
            R.string.remote_error
        }

        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> {
            R.string.render_error
        }

        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> {
            R.string.unsupported_error
        }

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> {
            R.string.encoding_error
        }

        else -> R.string.unexpected_error
    }
}

private fun playerString(resId: Int): String {
    return CloudStreamApp.context?.getString(resId)
        ?: when (resId) {
            R.string.error_loading_links_toast -> "Error Loading Links"
            R.string.no_links_found_toast -> "No Links Found"
            R.string.source_error -> "Source error"
            R.string.remote_error -> "Remote error"
            R.string.render_error -> "Render error"
            R.string.unsupported_error -> "Unsupported error"
            R.string.encoding_error -> "Encoding error"
            else -> "Unexpected player error"
        }
}
