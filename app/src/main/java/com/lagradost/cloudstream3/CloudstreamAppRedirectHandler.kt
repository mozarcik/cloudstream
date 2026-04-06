package com.lagradost.cloudstream3

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.APP_STRING
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.txt

object CloudstreamAppRedirectHandler {
    fun handle(activity: FragmentActivity?, url: String?): Boolean {
        if (activity == null || url.isNullOrBlank() || !url.contains(APP_STRING)) {
            return false
        }

        for (api in AccountManager.allApis) {
            if (!api.isValidRedirectUrl(url)) continue

            ioSafe {
                Log.i(MainActivity.TAG, "handleAppIntent $url")
                try {
                    val isSuccessful = api.login(url)
                    if (isSuccessful) {
                        Log.i(MainActivity.TAG, "authenticated ${api.name}")
                    } else {
                        Log.i(MainActivity.TAG, "failed to authenticate ${api.name}")
                    }
                    showToast(
                        if (isSuccessful) {
                            txt(R.string.authenticated_user, api.name)
                        } else {
                            txt(R.string.authenticated_user_fail, api.name)
                        }
                    )
                } catch (throwable: Throwable) {
                    logError(throwable)
                    showToast(
                        txt(R.string.authenticated_user_fail, api.name)
                    )
                }
            }
            return true
        }

        if (url == "$APP_STRING:") {
            ioSafe {
                @Suppress("DEPRECATION_ERROR")
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_hotReloadAllLocalPlugins(activity)
            }
            return true
        }

        if (url.startsWith(APP_STRING)) {
            showToast("Invalid Uri", Toast.LENGTH_SHORT)
            return true
        }

        return false
    }
}
