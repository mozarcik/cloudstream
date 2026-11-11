package com.lagradost.cloudstream3.tv.compat.home

import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.utils.DataStoreHelper

/** Library lists the user pinned to the navigation menu; stored per profile, none pinned by default. */
internal object PinnedLibraryLists {
    private const val KEY = "tv_pinned_library_lists"

    private val path get() = "${DataStoreHelper.currentAccount}/$KEY"

    fun read(): Set<String> = getKey<List<String>>(path)?.toSet().orEmpty()

    fun write(keys: Set<String>) = setKey(path, keys.toList())
}
