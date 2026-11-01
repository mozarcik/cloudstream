package com.lagradost.cloudstream3

import android.app.Activity
import android.os.Bundle
import com.lagradost.cloudstream3.ui.settings.createSelectedHostIntent

class AppIntentRouterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingIntent = intent
        val forwardedIntent = createSelectedHostIntent(clearTask = false).apply {
            action = incomingIntent?.action
            data = incomingIntent?.data
            `package` = packageName
            if (incomingIntent?.extras != null) {
                putExtras(incomingIntent.extras!!)
            }
        }

        startActivity(forwardedIntent)
        finish()
    }
}
