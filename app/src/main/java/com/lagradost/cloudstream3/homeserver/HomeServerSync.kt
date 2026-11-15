package com.lagradost.cloudstream3.homeserver

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.BackupUtils
import com.lagradost.cloudstream3.utils.BackupUtils.BackupFile
import com.lagradost.cloudstream3.utils.BackupUtils.BackupVars
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.WeakReference
import java.security.MessageDigest

/**
 * Keeps the app's own backup (settings + data store: bookmarks, resume positions, ...) in sync through the
 * home server. There is no conflict resolution:
 *  - server empty            -> this device uploads its backup
 *  - server changed since we last synced -> download and apply it, never overwrite it
 *  - server unchanged, local changed     -> upload (rejected by the server if it moved in the meantime)
 * Device-specific keys (UI layout, the server settings themselves) are neither uploaded nor applied.
 */
object HomeServerSync {
    private const val PREFS = "home_server_sync_state" // not part of the backup on purpose
    private const val KEY_VERSION = "version"
    private const val KEY_HASH = "hash"
    private const val MIN_INTERVAL_MS = 20_000L

    private val EXCLUDED_KEY_PARTS = listOf("home_server_", "app_layout_key")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var installed = false
    private var started = 0
    private var lastOpenSyncMs = 0L
    private var topActivity: WeakReference<Activity>? = null

    fun install(app: Application) {
        if (installed) return
        installed = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                topActivity = WeakReference(activity)
                if (++started == 1) syncOnOpen(app)
            }

            override fun onActivityStopped(activity: Activity) {
                if (--started == 0) pushIfChanged(app)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** Called when the app comes to the foreground. */
    fun syncOnOpen(context: Context, force: Boolean = false) {
        if (!HomeServerConfig.syncEnabled(context)) return
        val now = System.currentTimeMillis()
        if (!force && now - lastOpenSyncMs < MIN_INTERVAL_MS) return
        lastOpenSyncMs = now
        scope.launch { runCatching { sync(context.applicationContext, allowPull = true) }.onFailure(::logError) }
    }

    /** Called when the app leaves the foreground: sends local changes unless the server changed meanwhile. */
    fun pushIfChanged(context: Context) {
        if (!HomeServerConfig.syncEnabled(context)) return
        scope.launch { runCatching { sync(context.applicationContext, allowPull = false) }.onFailure(::logError) }
    }

    private suspend fun sync(context: Context, allowPull: Boolean) = mutex.withLock {
        val api = HomeServerApi(HomeServerConfig.baseUrl(context) ?: return)
        val state = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastVersion = state.getInt(KEY_VERSION, -1)
        val lastHash = state.getString(KEY_HASH, null)
        val meta = api.backupMeta()
        val local = snapshot(context)

        when {
            // Nothing on the server yet: this device (the one the server was added in) provides the backup.
            meta.version == 0 -> push(api, state, local, base = 0)

            // The server changed since we last synced (or we never did): take its data, do not overwrite it.
            meta.version != lastVersion -> if (allowPull) pull(context, api, state, meta, local)

            // Server unchanged: send ours if it differs from what we last synced.
            local.hash != lastHash -> push(api, state, local, base = meta.version)
        }
    }

    private suspend fun push(api: HomeServerApi, state: android.content.SharedPreferences, local: Snapshot, base: Int) {
        val meta = api.putBackup(local.json, base) ?: return // moved on: the next open pulls it
        state.edit().putInt(KEY_VERSION, meta.version).putString(KEY_HASH, local.hash).apply()
    }

    private suspend fun pull(
        context: Context,
        api: HomeServerApi,
        state: android.content.SharedPreferences,
        meta: HomeServerApi.BackupMeta,
        before: Snapshot,
    ) {
        val json = api.backupData() ?: return
        val remote = clean(parseJson<BackupFile>(json))
        BackupUtils.restore(context, remote, restoreSettings = true, restoreDataStore = true)
        val after = snapshot(context)
        state.edit().putInt(KEY_VERSION, meta.version).putString(KEY_HASH, after.hash).apply()
        if (after.settingsHash != before.settingsHash) notifyApplied(context)
    }

    private fun notifyApplied(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, R.string.home_server_sync_applied, Toast.LENGTH_LONG).show()
            // Settings are read when screens are created, so rebuild the visible one.
            topActivity?.get()?.takeIf { !it.isFinishing }?.recreate()
        }
    }

    private class Snapshot(val json: String, val hash: String, val settingsHash: String)

    private fun snapshot(context: Context): Snapshot {
        val backup = clean(BackupUtils.getBackup(context))
        val json = backup.toJson()
        return Snapshot(json, sha256(json), sha256(backup.settings.toJson()))
    }

    /** Drops device-specific keys and sorts everything so equal data always gives equal JSON. */
    private fun clean(file: BackupFile) = BackupFile(file.datastore.clean(), file.settings.clean())

    private fun BackupVars.clean() = BackupVars(
        bool.cleanMap(), int.cleanMap(), string.cleanMap(), float.cleanMap(), long.cleanMap(),
        stringSet?.filterKeys(::isSynced)?.mapValues { it.value?.toSortedSet() }?.toSortedMap(),
    )

    private fun <T> Map<String, T>?.cleanMap(): Map<String, T>? =
        this?.filterKeys(::isSynced)?.toSortedMap()

    private fun isSynced(key: String) = EXCLUDED_KEY_PARTS.none { key.contains(it) }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray()).joinToString("") { "%02x".format(it) }
}
