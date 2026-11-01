package com.lagradost.cloudstream3.tv.presentation.screens.player.catalog

import com.lagradost.cloudstream3.utils.ExtractorLink
import java.net.URI

/**
 * Identity of the link that was playing, used to pick "the same mirror in the same quality" for the
 * next episode. [ExtractorLink.name] is not compared: plugins put per-episode noise into it (file
 * size, episode code, resolution). The same source can serve different mirrors from different
 * domains (Cinejoy Nebula vs Secret), so the link domain refines the match.
 */
internal data class PreferredLink(
    val source: String,
    val quality: Int,
    val domain: String?,
) {
    /** 0 = same source, quality and domain; 1 = same source and quality; null = no match. */
    fun matchRank(link: ExtractorLink): Int? {
        if (link.source != source || link.quality != quality) return null
        return if (domain != null && link.url.linkDomain() == domain) 0 else 1
    }
}

internal fun ExtractorLink.toPreferredLink() = PreferredLink(
    source = source,
    quality = quality,
    domain = url.linkDomain(),
)

/** Last two host labels, so random subdomains (`abc.example.com`) still count as the same mirror. */
internal fun String.linkDomain(): String? {
    val host = runCatching { URI(this).host }.getOrNull()?.lowercase() ?: return null
    val labels = host.split('.').filter { label -> label.isNotEmpty() }
    return labels.takeLast(2).joinToString(".").takeIf { domain -> domain.isNotEmpty() }
}
