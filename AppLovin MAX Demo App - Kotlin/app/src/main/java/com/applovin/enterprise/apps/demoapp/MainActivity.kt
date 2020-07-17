package com.applovin.enterprise.apps.demoapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.applovin.enterprise.apps.demoapp.ads.InterstitialAdActivity
import com.applovin.enterprise.apps.demoapp.ads.RewardedAdActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.banners.BannerDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.eventtracking.EventTrackingActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials.InterstitialDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.leaders.LeaderDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs.MRecDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.NativeAdDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded.RewardedVideosDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.max.banner.BannerAdActivity
import com.applovin.enterprise.apps.demoapp.ads.max.mrecs.MrecAdActivity
import com.applovin.enterprise.apps.demoapp.data.main.AdType
import com.applovin.enterprise.apps.demoapp.data.main.Footer
import com.applovin.enterprise.apps.demoapp.data.main.ListItem
import com.applovin.enterprise.apps.demoapp.data.main.SectionHeader
import com.applovin.enterprise.apps.demoapp.ui.MainRecyclerViewAdapter
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.util.*

class MainActivity : AppCompatActivity(),
        MainRecyclerViewAdapter.OnMainListItemClickListener
{

    private lateinit var muteToggleMenuItem: MenuItem

    private fun generateMainListItems(): List<ListItem>
    {
        val items: MutableList<ListItem> =
                ArrayList()
        items.add(SectionHeader("APPLOVIN"))
        items.add(AdType("Interstitials", Intent(this, InterstitialDemoMenuActivity::class.java)))
        items.add(AdType("Rewarded", Intent(this, RewardedVideosDemoMenuActivity::class.java)))

        // Add "Leaders" menu item for tablets
        if (AppLovinSdkUtils.isTablet(this))
        {
            items.add(AdType("Leaders", Intent(this, LeaderDemoMenuActivity::class.java)))
        }
        // Add "Banners" menu item for phones
        else
        {
            items.add(AdType("Banners", Intent(this, BannerDemoMenuActivity::class.java)))
        }

        items.add(AdType("MRECs", Intent(this, MRecDemoMenuActivity::class.java)))
        items.add(AdType("Native Ads", Intent(this, NativeAdDemoMenuActivity::class.java)))
        items.add(AdType("Event Tracking", Intent(this, EventTrackingActivity::class.java)))
        items.add(SectionHeader("MAX"))
        items.add(AdType("Interstitials", Intent(this, InterstitialAdActivity::class.java)))
        items.add(AdType("Rewarded", Intent(this, RewardedAdActivity::class.java)))
        items.add(AdType("Banners", Intent(this, BannerAdActivity::class.java)))
        items.add(AdType("MRECs", Intent(this, MrecAdActivity::class.java)))
        items.add(AdType("Launch Mediation Debugger") { AppLovinSdk.getInstance(applicationContext).showMediationDebugger() })
        items.add(SectionHeader("SUPPORT"))
        items.add(AdType("Visit our Support Site", Intent(Intent.ACTION_VIEW, Uri.parse("https://support.applovin.com/support/home"))))
        items.add(Footer())
        return items
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val recyclerViewAdapter = MainRecyclerViewAdapter(generateMainListItems(), this, this)
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

        // Check that SDK key is present in Android Manifest
        checkSdkKey()
    }

    override fun onItemClicked(item: ListItem)
    {
        if (item is AdType && item.intent != null)
        {
            startActivity(item.intent)
        }
        else if (item is AdType && item.onTap != null)
        {
            item.onTap()
        }
    }

    private fun checkSdkKey()
    {
        val sdkKey = AppLovinSdk.getInstance(applicationContext).sdkKey
        if ("YOUR_SDK_KEY".equals(sdkKey, ignoreCase = true))
        {
            AlertDialog.Builder(this)
                    .setTitle("ERROR")
                    .setMessage("Please update your sdk key in the manifest file.")
                    .setCancelable(false)
                    .setNeutralButton("OK", null)
                    .show()
        }
    }

    // Mute Toggling

    /**
     * Toggling the sdk mute setting will affect whether your video ads begin in a muted state or not.
     */
    private fun toggleMute()
    {
        val sdk = AppLovinSdk.getInstance(applicationContext)
        sdk.settings.isMuted = !sdk.settings.isMuted
        muteToggleMenuItem.icon = getMuteIconForCurrentSdkMuteSetting()
    }

    private fun getMuteIconForCurrentSdkMuteSetting(): Drawable
    {
        val sdk = AppLovinSdk.getInstance(applicationContext)
        val drawableId = if (sdk.settings.isMuted) R.drawable.mute else R.drawable.unmute

        if (Build.VERSION.SDK_INT >= 22)
        {
            return resources.getDrawable(drawableId, theme)
        }
        else
        {
            @Suppress("DEPRECATION")
            return resources.getDrawable(drawableId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean
    {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean
    {
        muteToggleMenuItem = menu.findItem(R.id.action_toggle_mute).apply {
            icon = getMuteIconForCurrentSdkMuteSetting()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if (item.itemId == R.id.action_toggle_mute)
        {
            toggleMute()
        }

        return true
    }
}
