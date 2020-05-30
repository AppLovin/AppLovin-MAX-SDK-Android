package com.applovin.enterprise.apps.demoapp.ads.max.mrecs

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuItem

class MrecAdActivity : DemoMenuActivity() {
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Programmatic MRECs", Intent(this, ProgrammaticMrecAdActivity::class.java)),
        DemoMenuItem("Layout Editor MRECs", Intent(this, LayoutEditorMrecAdActivity::class.java)))
}
