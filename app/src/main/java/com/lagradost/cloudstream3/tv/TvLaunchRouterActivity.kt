package com.lagradost.cloudstream3.tv

import android.app.Activity
import android.os.Bundle
import com.lagradost.cloudstream3.ui.settings.createSelectedHostIntent

class TvLaunchRouterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(createSelectedHostIntent(clearTask = true))
        finish()
    }
}
