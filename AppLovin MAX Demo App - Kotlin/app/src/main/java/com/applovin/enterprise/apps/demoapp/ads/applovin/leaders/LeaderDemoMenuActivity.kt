package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders

import android.content.Intent
import com.applovin.enterprise.apps.demoapp.ads.DemoMenuActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem

class LeaderDemoMenuActivity : DemoMenuActivity()
{
    override fun getListViewContents(): Array<DemoMenuItem> = arrayOf(
            DemoMenuItem("Programmatic", Intent(this, LeaderProgrammaticActivity::class.java)),
            DemoMenuItem("Layout Editor",  Intent(this, LeaderLayoutEditorActivity::class.java))
    )
}
