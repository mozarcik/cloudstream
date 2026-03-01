package com.lagradost.cloudstream3.tv.presentation.screens.player.core

import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.text.Cue
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.tv.presentation.screens.player.panels.TvPlayerPlaybackErrorDetails
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSyncController
import com.lagradost.cloudstream3.ui.player.CustomDecoder
import com.lagradost.cloudstream3.ui.player.CustomDecoder.Companion.fixSubtitleAlignment
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.UpdatedDefaultExtractorsFactory
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.applyStyle
import com.lagradost.cloudstream3.utils.ExtractorLink
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

private const val SubtitleSyncDebugTag = "TvSubtitleSync"

internal fun PlaybackException.toTvPlayerPlaybackErrorDetails(): TvPlayerPlaybackErrorDetails {
    return TvPlayerPlaybackErrorDetails(
        exoErrorCode = errorCode,
        exoErrorName = errorCodeName,
        httpCode = findHttpStatusCode(),
    )
}

private fun PlaybackException.findHttpStatusCode(): Int? {
    var current: Throwable? = this
    while (current != null) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            return current.responseCode
        }
        current = current.cause
    }
    return null
}

internal fun createTvPlayerRenderersFactory(
    context: Context,
    subtitleSyncController: TvPlayerSubtitleSyncController,
): RenderersFactory {
    val baseFactory = runCatching {
        NextRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
    }.getOrElse {
        DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
    }

    return RenderersFactory { eventHandler, videoRendererEventListener, audioRendererEventListener, textRendererOutput, metadataRendererOutput ->
        val style = CustomDecoder.style
        val deduplicatedTexts = HashSet<CharSequence>()
        val combinedTextBuffer = StringBuilder()
        val styledTextOutput = TextOutput { cueGroup ->
            val cues = cueGroup.cues.filterNotNull()
            if (cues.isEmpty()) {
                if (!subtitleSyncController.dispatchStyledCues(emptyList())) {
                    textRendererOutput.onCues(cueGroup)
                }
                return@TextOutput
            }

            val (bitmapCues, textCues) = cues.partition { cue -> cue.bitmap != null }
            val styledBitmapCues = bitmapCues.map { bitmapCue ->
                bitmapCue
                    .buildUpon()
                    .fixSubtitleAlignment()
                    .applyStyle(style)
                    .build()
            }
            val styledTextCues = textCues.groupBy { textCue ->
                textCue.lineAnchor to textCue.position.times(1000.0f).toInt()
            }.mapNotNull { (_, entries) ->
                deduplicatedTexts.clear()
                combinedTextBuffer.clear()
                var lineCount = 0
                for (entry in entries) {
                    val text = entry.text ?: continue
                    if (!deduplicatedTexts.add(text)) continue
                    if (++lineCount > 1) {
                        combinedTextBuffer.append('\n')
                    }
                    combinedTextBuffer.append(text.trim())
                }
                entries.firstOrNull()
                    ?.buildUpon()
                    ?.setText(combinedTextBuffer.toString())
                    ?.fixSubtitleAlignment()
                    ?.applyStyle(style)
                    ?.build()
            }
            val combinedCues = styledBitmapCues + styledTextCues
            if (!subtitleSyncController.dispatchStyledCues(combinedCues)) {
                textRendererOutput.onCues(
                    androidx.media3.common.text.CueGroup(
                        combinedCues,
                        cueGroup.presentationTimeUs,
                    )
                )
            }
        }
        baseFactory.createRenderers(
            eventHandler,
            videoRendererEventListener,
            audioRendererEventListener,
            styledTextOutput,
            metadataRendererOutput,
        ).map { renderer ->
            if (renderer is TextRenderer) {
                subtitleSyncDebugLog(
                    "createRenderers: replacing TextRenderer with synced decoder factory",
                )
                val syncedRenderer = TextRenderer(
                    styledTextOutput,
                    eventHandler.looper,
                    subtitleSyncController.subtitleDecoderFactory(),
                ).apply {
                    // WHY: parity z legacy playerem (CS3IPlayer) - bez tego część starszych
                    // formatów napisów nie przechodzi ścieżki dekodera kompatybilnej z offsetem.
                    @Suppress("DEPRECATION")
                    experimentalSetLegacyDecodingEnabled(true)
                }
                subtitleSyncController.attachTextRenderer(syncedRenderer)
                syncedRenderer
            } else {
                renderer
            }
        }.toTypedArray()
    }
}

internal fun buildPlayerMediaSource(
    context: Context,
    link: ExtractorLink,
    subtitle: SubtitleData?,
    audioTracks: List<com.lagradost.cloudstream3.AudioFile>,
): MediaSource {
    val extractorFactory = UpdatedDefaultExtractorsFactory()
        .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_MERGE_FRAGMENTED_SIDX)
    val videoDataSourceFactory = createDataSourceFactory(
        context = context,
        link = link,
    )
    val videoMediaSourceFactory = DefaultMediaSourceFactory(videoDataSourceFactory, extractorFactory)
        // WHY: parity z legacy subtitle sync.
        // Przy wartości `true` Media3 parsuje napisy podczas ekstrakcji i przekazuje CuesWithTiming,
        // omijając CustomSubtitleDecoderFactory (brak listy cue do panelu synchronizacji).
        .experimentalParseSubtitlesDuringExtraction(false)

    val videoMediaItem = MediaItem.Builder()
        .setUri(link.url)
        .build()
    val videoMediaSource = videoMediaSourceFactory.createMediaSource(videoMediaItem)
    val subtitleConfiguration = subtitle?.let { subtitleData ->
        MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleData.getFixedUrl()))
            .setMimeType(subtitleData.mimeType)
            .setLabel(subtitleData.name)
            .setLanguage(subtitleData.languageCode)
            .setId(subtitleData.getId())
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }
    val subtitleMediaSource = if (subtitle != null && subtitleConfiguration != null) {
        val subtitleDataSourceFactory = createDataSourceFactory(
            context = context,
            link = link,
            globalExtraHeaders = subtitle.headers,
        )
        subtitleSyncDebugLog(
            "buildPlayerMediaSource: creating subtitle source id=${subtitle.getId()} uri=${subtitle.getFixedUrl()}",
        )
        SingleSampleMediaSource.Factory(subtitleDataSourceFactory)
            .createMediaSource(subtitleConfiguration, C.TIME_UNSET)
    } else {
        null
    }

    val externalAudioSources = audioTracks.mapIndexedNotNull { index, audioTrack ->
        runCatching {
            val audioDataSourceFactory = createDataSourceFactory(
                context = context,
                link = link,
                globalExtraHeaders = audioTrack.headers.orEmpty(),
            )
            val audioMediaItem = MediaItem.Builder()
                .setUri(audioTrack.url)
                .setMimeType(MimeTypes.AUDIO_UNKNOWN)
                .setMediaId("external_audio_$index")
                .build()
            DefaultMediaSourceFactory(audioDataSourceFactory, extractorFactory)
                .createMediaSource(audioMediaItem)
        }.getOrNull()
    }

    val allSources = ArrayList<MediaSource>(externalAudioSources.size + 2)
    allSources.add(videoMediaSource)
    subtitleMediaSource?.let(allSources::add)
    allSources.addAll(externalAudioSources)
    return if (allSources.size == 1) {
        videoMediaSource
    } else {
        MergingMediaSource(*allSources.toTypedArray())
    }
}

private fun createHttpDataSourceFactory(
    link: ExtractorLink,
    extraHeaders: Map<String, String> = emptyMap(),
): HttpDataSource.Factory {
    val provider = APIHolder.getApiFromNameNull(link.source)
    val interceptor = provider?.getVideoInterceptor(link)
    val client = if (interceptor == null) {
        app.baseClient
    } else {
        app.baseClient.newBuilder()
            .addInterceptor(interceptor)
            .build()
    }

    val userAgent = extraHeaders.entries.firstOrNull { (key, _) ->
        key.equals("User-Agent", ignoreCase = true)
    }?.value ?: link.headers.entries.firstOrNull { (key, _) ->
        key.equals("User-Agent", ignoreCase = true)
    }?.value ?: USER_AGENT

    val refererMap = if (link.referer.isBlank()) {
        emptyMap()
    } else {
        mapOf("referer" to link.referer)
    }

    val headers = mapOf(
        "accept" to "*/*",
        "sec-ch-ua" to "\"Chromium\";v=\"91\", \" Not;A Brand\";v=\"99\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-fetch-user" to "?1",
        "sec-fetch-mode" to "navigate",
        "sec-fetch-dest" to "video",
    ) + refererMap + link.getAllHeaders() + extraHeaders

    return OkHttpDataSource.Factory(client)
        .setUserAgent(userAgent)
        .apply {
            setDefaultRequestProperties(headers)
        }
}

private fun createDataSourceFactory(
    context: Context,
    link: ExtractorLink,
    globalExtraHeaders: Map<String, String> = emptyMap(),
    perUriExtraHeaders: Map<String, Map<String, String>> = emptyMap(),
): DataSource.Factory {
    val defaultFactory = DefaultDataSource.Factory(
        context,
        createHttpDataSourceFactory(
            link = link,
            extraHeaders = globalExtraHeaders,
        )
    )
    if (perUriExtraHeaders.isEmpty()) {
        return defaultFactory
    }

    return ResolvingDataSource.Factory(defaultFactory) { dataSpec ->
        val extraHeaders = perUriExtraHeaders[dataSpec.uri.toString()]
        if (extraHeaders.isNullOrEmpty()) {
            dataSpec
        } else {
            dataSpec.withAdditionalHeaders(extraHeaders)
        }
    }
}

internal fun seekPlayerBy(player: ExoPlayer, deltaMs: Long) {
    val current = player.currentPosition.coerceAtLeast(0L)
    seekPlayerTo(player = player, positionMs = current + deltaMs)
}

internal fun seekPlayerTo(player: ExoPlayer, positionMs: Long) {
    val target = positionMs.coerceAtLeast(0L)
    val duration = player.duration
    val clamped = if (duration == C.TIME_UNSET || duration < 0L) {
        target
    } else {
        target.coerceAtMost(duration)
    }
    player.seekTo(clamped)
}

internal fun subtitleSyncDebugLog(message: String) {
    Log.i(SubtitleSyncDebugTag, message)
}
