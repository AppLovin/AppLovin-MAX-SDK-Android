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
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.applovin.enterprise.apps.demoapp.ads.AppOpenAdActivity
import com.applovin.enterprise.apps.demoapp.ads.InterstitialAdActivity
import com.applovin.enterprise.apps.demoapp.ads.RewardedAdActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.banners.BannerDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.eventtracking.EventTrackingActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials.InterstitialDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.leaders.LeaderDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs.MRecDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded.RewardedVideosDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.max.banner.BannerAdActivity
import com.applovin.enterprise.apps.demoapp.ads.max.mrecs.MrecAdActivity
import com.applovin.enterprise.apps.demoapp.ads.max.nativead.NativeAdActivity
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem
import com.applovin.enterprise.apps.demoapp.data.main.Footer
import com.applovin.enterprise.apps.demoapp.data.main.ListItem
import com.applovin.enterprise.apps.demoapp.data.main.SectionHeader
import com.applovin.enterprise.apps.demoapp.ui.MainRecyclerViewAdapter
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkUtils
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
        items.add(DemoMenuItem("Interstitials", Intent(this, InterstitialDemoMenuActivity::class.java)))
        items.add(DemoMenuItem("Rewarded", Intent(this, RewardedVideosDemoMenuActivity::class.java)))

        // Add "Leaders" menu item for tablets
        if (AppLovinSdkUtils.isTablet(this))
        {
            items.add(DemoMenuItem("Leaders", Intent(this, LeaderDemoMenuActivity::class.java)))
        }
        // Add "Banners" menu item for phones
        else
        {
            items.add(DemoMenuItem("Banners", Intent(this, BannerDemoMenuActivity::class.java)))
        }

        items.add(DemoMenuItem("MRECs", Intent(this, MRecDemoMenuActivity::class.java)))
        items.add(DemoMenuItem("Event Tracking", Intent(this, EventTrackingActivity::class.java)))
        items.add(SectionHeader("MAX"))
        items.add(DemoMenuItem("Interstitials", Intent(this, InterstitialAdActivity::class.java)))
        items.add(DemoMenuItem("App Open Ads", Intent(this, AppOpenAdActivity::class.java)))
        items.add(DemoMenuItem("Rewarded", Intent(this, RewardedAdActivity::class.java)))
        items.add(DemoMenuItem("Banners", Intent(this, BannerAdActivity::class.java)))
        items.add(DemoMenuItem("MRECs", Intent(this, MrecAdActivity::class.java)))
        items.add(DemoMenuItem("Native Ads", Intent(this, NativeAdActivity::class.java)))
        items.add(DemoMenuItem("Launch Mediation Debugger", Runnable({ AppLovinSdk.getInstance(applicationContext).showMediationDebugger() })))
        items.add(SectionHeader("SUPPORT"))
        items.add(DemoMenuItem("Visit our Support Site", Intent(Intent.ACTION_VIEW, Uri.parse("https://support.applovin.com/support/home"))))
        items.add(Footer())
        return items
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val recyclerViewAdapter = MainRecyclerViewAdapter(generateMainListItems(), this, this)
        val manager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(this, manager.orientation)

        val recyclerView = findViewById<RecyclerView>(R.id.mainRecyclerView)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = manager
            addItemDecoration(decoration)
            itemAnimator = DefaultItemAnimator()
            adapter = recyclerViewAdapter
        }

        // Check that SDK key is present in Android Manifest
        checkSdkKey()
    }

    override fun onItemClicked(item: ListItem)
    {
        if (item is DemoMenuItem)
        {
            if (item.intent != null)
            {
                startActivity(item.intent);
            }
            else if (item.runnable != null)
            {
                item.runnable.run();
            }
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
