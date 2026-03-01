package com.lagradost.cloudstream3.tv.compat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated host for legacy plugin settings flows.
 * Old plugins expect an AppCompatActivity and show their own DialogFragments/XML UI.
 */
class PluginSettingsHostActivity : AppCompatActivity() {
    private var hasOpenedSettings = false
    private var hasObservedLegacyUi = false
    private var finishCheckJob: Job? = null

    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?
        ) {
            hasObservedLegacyUi = true
        }

        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            hasObservedLegacyUi = true
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            scheduleFinishIfIdle()
        }

        override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
            scheduleFinishIfIdle()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        CommonActivity.loadThemes(this)
        super.onCreate(savedInstanceState)

        setContentView(
            FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        )

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)

        if (savedInstanceState == null) {
            window.decorView.post {
                openLegacySettings()
            }
        } else {
            hasOpenedSettings = true
            hasObservedLegacyUi = supportFragmentManager.fragments.isNotEmpty()
            scheduleFinishIfIdle()
        }
    }

    override fun onResume() {
        super.onResume()
        scheduleFinishIfIdle()
    }

    override fun onDestroy() {
        finishCheckJob?.cancel()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    private fun openLegacySettings() {
        val repositoryUrl = intent.getStringExtra(EXTRA_REPOSITORY_URL)
        val internalName = intent.getStringExtra(EXTRA_INTERNAL_NAME)
        if (repositoryUrl.isNullOrBlank() || internalName.isNullOrBlank()) {
            finishWithError("Missing plugin settings arguments")
            return
        }

        val plugin = ExtensionsCompat.resolveInstalledPluginInstance(
            context = this,
            repositoryUrl = repositoryUrl,
            internalName = internalName
        )
        val callback = plugin?.openSettings
        if (callback == null) {
            finishWithError(getString(R.string.plugin_settings_unavailable))
            return
        }

        hasOpenedSettings = true

        runCatching {
            callback.invoke(this)
        }.onSuccess {
            if (supportFragmentManager.fragments.isNotEmpty()) {
                hasObservedLegacyUi = true
            }
            setResult(Activity.RESULT_OK)
            scheduleFinishIfIdle(delayMillis = if (hasObservedLegacyUi) 120L else 450L)
        }.onFailure { error ->
            Log.e(TAG, "Failed to open plugin settings", error)
            finishWithError(getString(R.string.plugin_settings_open_failed))
        }
    }

    private fun scheduleFinishIfIdle(delayMillis: Long = 120L) {
        if (!hasOpenedSettings) return

        finishCheckJob?.cancel()
        finishCheckJob = lifecycleScope.launch {
            delay(delayMillis)
            if (shouldFinish()) {
                finish()
            }
        }
    }

    private fun shouldFinish(): Boolean {
        if (!hasOpenedSettings) return false

        val hasActiveFragments = supportFragmentManager.fragments.any { fragment ->
            fragment.isAdded &&
                !fragment.isRemoving &&
                !fragment.isDetached &&
                ((fragment is DialogFragment && fragment.dialog?.isShowing == true) || fragment.view != null)
        }
        if (hasActiveFragments) return false

        return hasObservedLegacyUi || supportFragmentManager.fragments.isEmpty()
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val TAG = "PluginSettingsHost"
        private const val EXTRA_REPOSITORY_URL = "extra_repository_url"
        private const val EXTRA_INTERNAL_NAME = "extra_internal_name"

        fun createIntent(
            context: Context,
            repositoryUrl: String,
            internalName: String
        ): Intent {
            return Intent(context, PluginSettingsHostActivity::class.java).apply {
                putExtra(EXTRA_REPOSITORY_URL, repositoryUrl)
                putExtra(EXTRA_INTERNAL_NAME, internalName)
            }
        }
    }
}
