package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import com.lagradost.cloudstream3.utils.SubtitleHelper.fromTagToLanguageName
import com.lagradost.cloudstream3.utils.SubtitleHelper.languages
import java.util.Locale

internal fun buildSubtitleLanguageOptions(): List<TvPlayerSubtitleLanguageOption> {
    return languages
        .map { language ->
            TvPlayerSubtitleLanguageOption(
                tag = language.IETF_tag,
                label = language.nameNextToFlagEmoji(),
            )
        }
        .sortedBy { option ->
            option.label.substringAfter('\u00a0').lowercase(Locale.ROOT)
        }
}

internal fun subtitleLanguageLabel(
    languageTag: String,
    options: List<TvPlayerSubtitleLanguageOption>,
): String {
    val fromOptions = options.firstOrNull { option ->
        option.tag.equals(languageTag, ignoreCase = true)
    }?.label
    if (fromOptions != null) {
        return fromOptions
    }

    val fallbackLanguageName = fromTagToLanguageName(languageTag)
        ?.replaceFirstChar { firstChar ->
            if (firstChar.isLowerCase()) {
                firstChar.titlecase(Locale.getDefault())
            } else {
                firstChar.toString()
            }
        }
    return fallbackLanguageName ?: languageTag
}

internal fun createDefaultOnlineSubtitlesState(
    query: String,
    options: List<TvPlayerSubtitleLanguageOption>,
): TvPlayerOnlineSubtitlesState {
    val languageTag = getAutoSelectLanguageTagIETF().trim().ifBlank { "en" }
    return TvPlayerOnlineSubtitlesState(
        query = query,
        selectedLanguageTag = languageTag,
        selectedLanguageLabel = subtitleLanguageLabel(
            languageTag = languageTag,
            options = options,
        ),
        status = if (query.isBlank()) {
            TvPlayerOnlineSubtitlesStatus.Idle
        } else {
            TvPlayerOnlineSubtitlesStatus.Loading
        },
    )
}

internal fun onlineSubtitleDisplayName(
    entry: AbstractSubtitleEntities.SubtitleEntity,
    withLanguage: Boolean,
): String {
    if (!withLanguage || entry.lang.isBlank()) {
        return entry.name
    }
    val localizedLanguage = fromTagToLanguageName(entry.lang.trim()) ?: entry.lang
    return "$localizedLanguage ${entry.name}"
}

internal fun formatOnlineSubtitleSupportingTexts(
    subtitle: AbstractSubtitleEntities.SubtitleEntity,
): List<String> {
    val languageName = fromTagToLanguageName(subtitle.lang)?.takeIf { localized ->
        localized.isNotBlank()
    } ?: subtitle.lang.takeIf { language ->
        language.isNotBlank()
    }
    val formatHint = subtitleTypeHint(subtitle)
    return buildList {
        subtitle.source.takeIf { source -> source.isNotBlank() }?.let(::add)
        languageName?.let(::add)
        formatHint?.let(::add)
    }.distinct()
}

private fun subtitleTypeHint(
    subtitle: AbstractSubtitleEntities.SubtitleEntity,
): String? {
    fun extractType(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val sanitized = raw.substringBefore('?').substringBefore('#')
        val extension = sanitized.substringAfterLast('.', "")
        if (extension.isBlank()) return null
        return extension.lowercase(Locale.ROOT)
            .takeIf { value ->
                value in setOf("srt", "vtt", "ass", "ssa", "sub", "ttml", "smi")
            }?.uppercase(Locale.ROOT)
    }

    return extractType(subtitle.name) ?: extractType(subtitle.data)
}
