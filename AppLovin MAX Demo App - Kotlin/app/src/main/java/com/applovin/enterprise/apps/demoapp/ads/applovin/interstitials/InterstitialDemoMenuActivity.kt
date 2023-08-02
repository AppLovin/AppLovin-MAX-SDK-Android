package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class InterstitialDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Basic Integration", Intent(this, InterstitialBasicIntegrationActivity::class.java)),
            DemoMenuItem("Manually Loading Ad", Intent(this, InterstitialManualLoadingActivity::class.java)),
            DemoMenuItem("Zone Integration",  Intent(this, InterstitialZoneActivity::class.java))
    )
}
