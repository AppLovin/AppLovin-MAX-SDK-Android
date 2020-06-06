package com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuItem

class RewardedVideosDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Basic Integration", Intent(this, RewardedVideosActivity::class.java)),
            DemoMenuItem("Zone Integration",  Intent(this, RewardedVideosZoneActivity::class.java))
    )
}
