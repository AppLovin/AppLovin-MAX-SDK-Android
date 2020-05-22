package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuItem

class InterstitialDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Basic Integration", "Quick interstitial ads integration", Intent(this, InterstitialBasicIntegrationActivity::class.java)),
            DemoMenuItem("Manually loading ad", "Use this for greater control over the ad load process", Intent(this, InterstitialManualLoadingActivity::class.java)),
            DemoMenuItem("Zone Integration", "Create different user experiences of the same ad type", Intent(this, InterstitialZoneActivity::class.java))
    )
}
