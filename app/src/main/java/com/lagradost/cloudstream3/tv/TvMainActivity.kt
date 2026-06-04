/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lagradost.cloudstream3.tv

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.CloudstreamAppRedirectHandler
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.CommonActivity.setActivityInstance
import com.lagradost.cloudstream3.network.initClient
import com.lagradost.cloudstream3.tv.compat.TvPluginBootstrap
import com.lagradost.cloudstream3.tv.presentation.TvApp
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTheme
import com.lagradost.cloudstream3.utils.BackupUtils.setUpBackup
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvMainActivity : AppCompatActivity() {
    companion object {
        // HTTP client for network requests
        private val app = Requests()
    }

    private val exitRequestHandler = TvExitRequestHandler(
        elapsedRealtime = SystemClock::elapsedRealtime,
    )
    
    @Suppress("DEPRECATION_ERROR")
    override fun onCreate(savedInstanceState: Bundle?) {
        CommonActivity.loadThemes(this)
        super.onCreate(savedInstanceState)

        setActivityInstance(this)
        setUpBackup()

        // CRITICAL: Initialize HTTP client (same as MainActivity)
        app.initClient(this)

        // Load plugins asynchronously
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                TvPluginBootstrap.bootstrap(this@TvMainActivity)
            } catch (e: Exception) {
                Log.e("TvMainActivity", "Failed to load plugins", e)
            }
        }

        setContent {
            CloudStreamTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface
                    ) {
                        TvApp(
                            onBackPressed = ::exitTvApp,
                        )
                    }
                }
            }
        }

        handleAppIntent(intent)
    }

    private fun exitTvApp() {
        when (exitRequestHandler.onExitRequested()) {
            TvExitRequestAction.ShowExitHint -> {
                CommonActivity.showToast(
                    this,
                    R.string.tv_press_back_again_to_exit,
                    Toast.LENGTH_SHORT,
                )
            }

            TvExitRequestAction.ExitNow -> finishAndRemoveTask()
        }
    }

    override fun onStop() {
        exitRequestHandler.reset()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleAppIntent(intent)
        super.onNewIntent(intent)
    }

    private fun handleAppIntent(intent: Intent?) {
        CloudstreamAppRedirectHandler.handle(
            activity = this,
            url = intent?.dataString
        )
    }
}
