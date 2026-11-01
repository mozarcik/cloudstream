package com.lagradost.cloudstream3.phone

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lagradost.cloudstream3.CloudstreamAppRedirectHandler
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.CommonActivity.setActivityInstance
import com.lagradost.cloudstream3.network.initClient
import com.lagradost.cloudstream3.phone.presentation.PhoneApp
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneTheme
import com.lagradost.cloudstream3.tv.compat.TvPluginBootstrap
import com.lagradost.cloudstream3.utils.BackupUtils.setUpBackup
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Entry point of the Compose phone UI (the TV UI lives in `tv/`); plugin bootstrap is shared with TV. */
class PhoneMainActivity : AppCompatActivity() {
    companion object {
        private val app = Requests()
    }

    @Suppress("DEPRECATION_ERROR")
    override fun onCreate(savedInstanceState: Bundle?) {
        CommonActivity.loadThemes(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setActivityInstance(this)
        setUpBackup()
        app.initClient(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                TvPluginBootstrap.bootstrap(this@PhoneMainActivity)
            } catch (e: Exception) {
                Log.e("PhoneMainActivity", "Failed to load plugins", e)
            }
        }

        setContent { PhoneTheme { PhoneApp() } }
        handleAppIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        handleAppIntent(intent)
        super.onNewIntent(intent)
    }

    private fun handleAppIntent(intent: Intent?) {
        CloudstreamAppRedirectHandler.handle(activity = this, url = intent?.dataString)
    }
}
