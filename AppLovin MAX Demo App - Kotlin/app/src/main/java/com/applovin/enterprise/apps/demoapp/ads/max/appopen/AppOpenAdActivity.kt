package com.applovin.enterprise.apps.demoapp.ads.max.appopen

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class AppOpenAdActivity : DemoMenuActivity() {
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Basic Integration", Intent(this, BasicIntegrationAppOpenAdActivity::class.java)),
        DemoMenuItem("Jetpack Compose", Intent(this, JetpackComposeAppOpenAdActivity::class.java))
    )
}