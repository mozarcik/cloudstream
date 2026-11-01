package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.MainAPI

fun MainAPI.sourceId(): String {
    val providerClass = this::class.java.name
    val providerUrl = mainUrl.trim().ifBlank { "none" }
    return "$providerClass|$providerUrl"
}

