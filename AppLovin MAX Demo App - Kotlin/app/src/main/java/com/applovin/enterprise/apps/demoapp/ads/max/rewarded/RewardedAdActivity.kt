package com.applovin.enterprise.apps.demoapp.ads.max.rewarded

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class RewardedAdActivity : DemoMenuActivity() {
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Basic Integration Rewarded", Intent(this, BasicIntegrationRewardedAdActivity::class.java)),
        DemoMenuItem("Jetpack Compose Rewarded", Intent(this, JetpackComposeRewardedAdActivity::class.java))
    )
}