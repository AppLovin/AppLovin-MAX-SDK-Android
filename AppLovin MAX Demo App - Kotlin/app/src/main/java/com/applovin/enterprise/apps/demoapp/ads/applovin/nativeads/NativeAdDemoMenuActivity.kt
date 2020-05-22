package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuItem

class NativeAdDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Single ad", "Programmatically loading an ad using our open-source carousel view", Intent(this, NativeAdCarouselUIActivity::class.java)),
            DemoMenuItem("Multiple ads", "Simple native ads in a RecyclerView", Intent(this, NativeAdRecyclerViewActivity::class.java))
    )
}
