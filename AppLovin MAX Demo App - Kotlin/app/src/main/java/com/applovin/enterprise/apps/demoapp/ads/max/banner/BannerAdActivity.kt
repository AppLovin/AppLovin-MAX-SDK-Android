package com.applovin.enterprise.apps.demoapp.ads.max.banner

import android.content.Intent

import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class BannerAdActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Programmatic Banners", Intent(this, ProgrammaticBannerAdActivity::class.java)),
        DemoMenuItem("Layout Editor Banners", Intent(this, LayoutEditorBannerAdActivity::class.java)))
}
