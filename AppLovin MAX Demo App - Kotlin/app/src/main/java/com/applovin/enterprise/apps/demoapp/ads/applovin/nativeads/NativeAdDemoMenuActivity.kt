package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuItem

class NativeAdDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Single Ad", Intent(this, NativeAdCarouselUIActivity::class.java)),
            DemoMenuItem("Multiple Ads", Intent(this, NativeAdRecyclerViewActivity::class.java))
    )
}
