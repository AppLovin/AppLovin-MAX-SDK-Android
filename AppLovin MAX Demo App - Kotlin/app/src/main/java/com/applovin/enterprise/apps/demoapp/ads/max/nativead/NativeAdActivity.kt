package com.applovin.enterprise.apps.demoapp.ads.max.nativead

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class NativeAdActivity : DemoMenuActivity() {
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Templates API", Intent(this, TemplateNativeAdActivity::class.java)),
        DemoMenuItem("Manual API", Intent(this, ManualNativeAdActivity::class.java)),
        DemoMenuItem("Manual Late Binding API", Intent(this, ManualNativeLateBindingAdActivity::class.java)),
        DemoMenuItem("Recycler View Ad Placer", Intent(this, RecyclerViewNativeAdActivity::class.java))
    )
}
