package com.lagradost.cloudstream3.ui.player

import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.ui.result.ResultEpisode

internal fun SubtitleData.toSubtitleFetchLogPayload(): String {
    val resolvedUrl = getFixedUrl()
    val headerKeys = headers.keys.sorted()
    val resolvedFileName = name.ifBlank {
        originalName.ifBlank {
            resolvedUrl.substringAfterLast('/').ifBlank { resolvedUrl }
        }
    }

    return buildString {
        append("fileName=")
        append(resolvedFileName)
        append(" link=")
        append(resolvedUrl)
        append(" metadata={")
        append("id=")
        append(getId())
        append(", originalName=")
        append(originalName.ifBlank { "<blank>" })
        append(", nameSuffix=")
        append(nameSuffix.ifBlank { "<blank>" })
        append(", origin=")
        append(origin)
        append(", mimeType=")
        append(mimeType)
        append(", languageCode=")
        append(languageCode ?: "null")
        append(", ietfTag=")
        append(getIETF_tag() ?: "null")
        append(", headerKeys=")
        append(headerKeys)
        append(", headersCount=")
        append(headers.size)
        if (resolvedUrl != url) {
            append(", rawLink=")
            append(url)
        }
        append("}")
    }
}

internal fun ResultEpisode?.toSubtitleFetchEpisodeMetadataLog(): String {
    val episode = this
    return buildString {
        append("episodeMetadata={")
        append("id=")
        append(episode?.id ?: "null")
        append(", parentId=")
        append(episode?.parentId ?: "null")
        append(", apiName=")
        append(episode?.apiName ?: "null")
        append(", season=")
        append(episode?.season ?: "null")
        append(", episode=")
        append(episode?.episode ?: "null")
        append(", headerName=")
        append(episode?.headerName?.ifBlank { "<blank>" } ?: "null")
        append(", title=")
        append(episode?.name?.ifBlank { "<blank>" } ?: "null")
        append("}")
    }
}

internal fun AbstractSubtitleEntities.SubtitleEntity.toOnlineSubtitleMetadataLog(): String {
    val headerKeys = headers.keys.sorted()
    return buildString {
        append("providerMetadata={")
        append("idPrefix=")
        append(idPrefix)
        append(", source=")
        append(source.ifBlank { "<blank>" })
        append(", name=")
        append(name.ifBlank { "<blank>" })
        append(", lang=")
        append(lang.ifBlank { "<blank>" })
        append(", data=")
        append(data.ifBlank { "<blank>" })
        append(", type=")
        append(type)
        append(", seasonNumber=")
        append(seasonNumber ?: "null")
        append(", epNumber=")
        append(epNumber ?: "null")
        append(", year=")
        append(year ?: "null")
        append(", hearingImpaired=")
        append(isHearingImpaired)
        append(", headerKeys=")
        append(headerKeys)
        append(", headersCount=")
        append(headers.size)
        append("}")
    }
}
