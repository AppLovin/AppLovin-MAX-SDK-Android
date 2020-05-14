package com.applovin.enterprise.apps.demoapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.applovin.enterprise.apps.demoapp.ads.*
import com.applovin.enterprise.apps.demoapp.data.main.AdType
import com.applovin.enterprise.apps.demoapp.data.main.ListItem
import com.applovin.enterprise.apps.demoapp.data.main.SectionHeader
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ui.MainRecyclerViewAdapter
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(),
        MainRecyclerViewAdapter.OnMainListItemClickListener
{
    private val mainListItems: List<ListItem> by lazy {
        listOf(
                SectionHeader("Ad Examples"),
                AdType("Interstitial", Intent(this, InterstitialAdActivity::class.java)),
                AdType("Rewarded", Intent(this, RewardedAdActivity::class.java)),
                AdType("Programmatic Banners", Intent(this, ProgrammaticBannerAdActivity::class.java)),
                AdType("Layout Editor Banners", Intent(this, LayoutEditorBannerAdActivity::class.java)),
                AdType("Programmatic MRECs", Intent(this, ProgrammaticMrecAdActivity::class.java)),
                AdType("Layout Editor MRECs", Intent(this, LayoutEditorMrecAdActivity::class.java))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val recyclerViewAdapter = MainRecyclerViewAdapter(mainListItems, this, this)
        val manager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(this, manager.orientation)

        mainRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = manager
            addItemDecoration(decoration)
            itemAnimator = DefaultItemAnimator()
            adapter = recyclerViewAdapter
        }

        // Initialize the AppLovin SDK
        AppLovinSdk.getInstance(this).mediationProvider = AppLovinMediationProvider.MAX
        AppLovinSdk.getInstance(this).initializeSdk {
            // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached
        }
    }

    override fun onItemClicked(item: ListItem)
    {
        if (item is AdType)
        {
            startActivity(item.intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if (item.itemId == R.id.action_mediation_debugger)
        {
            AppLovinSdk.getInstance(this).showMediationDebugger()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
