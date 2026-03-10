package com.lagradost.cloudstream3.tv.compat

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.AutoDownloadMode
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.plugins.PluginManager

internal data class TvPluginBootstrapConfig(
    val autoUpdatePlugins: Boolean,
    val autoDownloadMode: AutoDownloadMode,
)

internal class TvPluginBootstrapExecutor(
    private val updateOnlinePlugins: suspend () -> Unit,
    private val loadOnlinePlugins: suspend () -> Unit,
    private val downloadMissingPlugins: suspend (AutoDownloadMode) -> Unit,
    private val loadLocalPlugins: suspend () -> Unit,
    private val onFailure: (Throwable) -> Unit,
) {
    suspend fun execute(config: TvPluginBootstrapConfig) {
        runStep {
            if (config.autoUpdatePlugins) {
                updateOnlinePlugins()
            } else {
                loadOnlinePlugins()
            }
        }

        if (config.autoDownloadMode != AutoDownloadMode.Disable) {
            runStep {
                downloadMissingPlugins(config.autoDownloadMode)
            }
        }

        runStep(loadLocalPlugins)
    }

    private suspend fun runStep(block: suspend () -> Unit) {
        try {
            block()
        } catch (throwable: Throwable) {
            onFailure(throwable)
        }
    }
}

object TvPluginBootstrap {
    private const val TAG = "TvPluginBootstrap"

    internal fun resolveConfig(
        context: Context,
        preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
    ): TvPluginBootstrapConfig {
        val autoUpdatePlugins = preferences.getBoolean(
            context.getString(R.string.auto_update_plugins_key),
            true,
        )
        val autoDownloadMode = AutoDownloadMode.getEnum(
            preferences.getInt(
                context.getString(R.string.auto_download_plugins_key),
                AutoDownloadMode.Disable.value,
            )
        ) ?: AutoDownloadMode.Disable

        return TvPluginBootstrapConfig(
            autoUpdatePlugins = autoUpdatePlugins,
            autoDownloadMode = autoDownloadMode,
        )
    }

    @Suppress("DEPRECATION_ERROR")
    suspend fun bootstrap(
        activity: Activity,
        preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity),
    ) {
        val executor = TvPluginBootstrapExecutor(
            updateOnlinePlugins = {
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_updateAllOnlinePluginsAndLoadThem(
                    activity,
                )
            },
            loadOnlinePlugins = {
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllOnlinePlugins(activity)
            },
            downloadMissingPlugins = { mode ->
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_downloadNotExistingPluginsAndLoad(
                    activity,
                    mode,
                )
            },
            loadLocalPlugins = {
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(
                    activity,
                    false,
                )
            },
            onFailure = { throwable ->
                Log.e(TAG, "TV plugin bootstrap step failed", throwable)
            },
        )

        executor.execute(resolveConfig(activity, preferences))
    }
}
