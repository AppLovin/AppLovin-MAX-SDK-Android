package com.applovin.enterprise.apps.demoapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.applovin.enterprise.apps.demoapp.ads.ProgrammaticBannerAdActivity
import com.applovin.enterprise.apps.demoapp.ads.InterstitialAdActivity
import com.applovin.enterprise.apps.demoapp.ads.LayoutEditorBannerAdActivity
import com.applovin.enterprise.apps.demoapp.ads.RewardedAdActivity
import com.applovin.enterprise.apps.demoapp.data.home.AdType
import com.applovin.enterprise.apps.demoapp.data.home.ListItem
import com.applovin.enterprise.apps.demoapp.data.home.SectionHeader
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ui.HomeRecyclerViewAdapter
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*

class HomeActivity : AppCompatActivity(),
    HomeRecyclerViewAdapter.OnHomeListItemClickListener
{
    private val homeListItems: List<ListItem> by lazy {
        listOf(
            SectionHeader("MAX Ads"),
            AdType("Interstitial", Intent(this, InterstitialAdActivity::class.java)),
            AdType("Rewarded", Intent(this, RewardedAdActivity::class.java)),
            AdType("Programmatic Banners / Leaders", Intent(this, ProgrammaticBannerAdActivity::class.java)),
            AdType("Layout Editor Banners / Leaders", Intent(this, LayoutEditorBannerAdActivity::class.java))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        val recyclerViewAdapter = HomeRecyclerViewAdapter(homeListItems, this, this)
        val manager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(this, manager.orientation)

        homeRecyclerView.apply {
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
}
