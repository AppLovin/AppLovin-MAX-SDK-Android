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
import com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs.MRecDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.NativeAdDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded.RewardedVideosDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.max.banner.BannerAdActivity
import com.applovin.enterprise.apps.demoapp.ads.max.mrecs.MrecAdActivity
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

    private lateinit var muteToggleMenuItem: MenuItem

    private val mainListItems: List<ListItem> by lazy {
        listOf(
                SectionHeader("APPLOVIN"),
                AdType("Interstitials", Intent(this, InterstitialDemoMenuActivity::class.java)),
                AdType("Rewarded", Intent(this, RewardedVideosDemoMenuActivity::class.java)),
                AdType("Banners", Intent(this, BannerDemoMenuActivity::class.java)),
                AdType("MRECs", Intent(this, MRecDemoMenuActivity::class.java)),
                AdType("Native Ads", Intent(this, NativeAdDemoMenuActivity::class.java)),
                AdType("Event Tracking", Intent(this, EventTrackingActivity::class.java)),
                SectionHeader("MAX"),
                AdType("Interstitials", Intent(this, InterstitialAdActivity::class.java)),
                AdType("Rewarded", Intent(this, RewardedAdActivity::class.java)),
                AdType("Banners", Intent(this, BannerAdActivity::class.java)),
                AdType("MRECs", Intent(this, MrecAdActivity::class.java)),
                SectionHeader("SUPPORT"),
                AdType("Resources", Intent(Intent.ACTION_VIEW, Uri.parse("https://support.applovin.com/support/home"))),
                AdType("Contact", Intent(makeContactIntent()))

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

        // Check that SDK key is present in Android Manifest
        checkSdkKey()
    }

    private fun makeContactIntent(): Intent
    {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "text/plain"
        intent.data = Uri.parse("mailto:")
        val to = arrayOf("support@applovin.com")
        intent.putExtra(Intent.EXTRA_EMAIL, to)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Android SDK support")
        intent.putExtra(Intent.EXTRA_TEXT, "\n\n\n---\nSDK Version: ${AppLovinSdk.VERSION}")
        return Intent.createChooser(intent, "Send Email")
    }

    override fun onItemClicked(item: ListItem)
    {
        if (item is AdType)
        {
            startActivity(item.intent)
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
