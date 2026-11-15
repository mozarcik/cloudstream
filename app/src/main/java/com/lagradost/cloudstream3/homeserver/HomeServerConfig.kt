package com.lagradost.cloudstream3.homeserver

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/** Settings of the optional home server. An empty URL disables every feature of this package. */
object HomeServerConfig {
    private const val KEY_URL = "home_server_url"
    private const val KEY_AUTO_UPLOAD = "home_server_auto_upload"
    private const val KEY_DELETE_AFTER = "home_server_delete_after_upload"
    private const val KEY_SYNC = "home_server_sync"

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun baseUrl(context: Context): String? =
        prefs(context).getString(KEY_URL, null)?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }

    fun setBaseUrl(context: Context, url: String) =
        prefs(context).edit { putString(KEY_URL, url.trim()) }

    fun isEnabled(context: Context) = baseUrl(context) != null

    fun autoUpload(context: Context) = isEnabled(context) && prefs(context).getBoolean(KEY_AUTO_UPLOAD, false)

    fun setAutoUpload(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_AUTO_UPLOAD, value) }

    fun deleteAfterUpload(context: Context) = prefs(context).getBoolean(KEY_DELETE_AFTER, false)

    fun setDeleteAfterUpload(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_DELETE_AFTER, value) }

    /** Settings and history sync (on by default once an address is set). */
    fun syncEnabled(context: Context) = isEnabled(context) && prefs(context).getBoolean(KEY_SYNC, true)

    fun syncSwitch(context: Context) = prefs(context).getBoolean(KEY_SYNC, true)

    fun setSync(context: Context, value: Boolean) =
        prefs(context).edit { putBoolean(KEY_SYNC, value) }
}
