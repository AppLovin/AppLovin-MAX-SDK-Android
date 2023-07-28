package com.applovin.enterprise.apps.demoapp.ads.max.interstitial

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class InterstitialAdActivity : DemoMenuActivity() {
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Basic Integration Interstitials", Intent(this, BasicIntegrationInterstitialAdActivity::class.java)),
        DemoMenuItem("Jetpack Compose Interstitials", Intent(this, JetpackComposeInterstitialAdActivity::class.java))
    )
}