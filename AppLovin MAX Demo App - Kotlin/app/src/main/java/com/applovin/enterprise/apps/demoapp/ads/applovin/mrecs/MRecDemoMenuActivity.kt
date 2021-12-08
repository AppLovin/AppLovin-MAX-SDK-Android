package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class MRecDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
        DemoMenuItem("Programmatic", Intent(this, MRecProgrammaticActivity::class.java)),
        DemoMenuItem("Layout Editor", Intent(this, MRecLayoutEditorActivity::class.java))
    )
}