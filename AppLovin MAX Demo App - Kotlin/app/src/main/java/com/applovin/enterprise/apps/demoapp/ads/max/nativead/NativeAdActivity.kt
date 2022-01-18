package com.applovin.enterprise.apps.demoapp.ads.max.nativead

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class NativeAdActivity : DemoMenuActivity() {
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Template Native Ads", Intent(this, TemplateNativeAdActivity::class.java)),
            DemoMenuItem("Manual Native Ads", Intent(this, ManualNativeAdActivity::class.java)))
}