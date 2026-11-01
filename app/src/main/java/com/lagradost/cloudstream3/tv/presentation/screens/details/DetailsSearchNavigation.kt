package com.lagradost.cloudstream3.tv.presentation.screens.details

internal fun resolveDetailsSearchQuery(
    title: String,
): String? {
    return title.trim().takeIf { it.isNotEmpty() }
}
