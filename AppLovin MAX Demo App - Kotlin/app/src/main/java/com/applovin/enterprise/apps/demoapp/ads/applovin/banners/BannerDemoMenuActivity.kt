package com.applovin.enterprise.apps.demoapp.ads.applovin.banners

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class BannerDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Programmatic", Intent(this, BannerProgrammaticActivity::class.java)),
            DemoMenuItem("Layout Editor", Intent(this, BannerLayoutEditorActivity::class.java)),
            DemoMenuItem("Zone Integration", Intent(this, BannerZoneActivity::class.java))
    )
}
