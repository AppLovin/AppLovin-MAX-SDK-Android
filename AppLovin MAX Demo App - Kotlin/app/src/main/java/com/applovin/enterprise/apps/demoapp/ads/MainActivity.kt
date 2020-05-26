package com.applovin.enterprise.apps.demoapp.ads

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.applovin.enterprise.apps.demoapp.ads.applovin.banners.BannerDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.eventtracking.EventTrackingActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials.InterstitialDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.leaders.LeaderDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs.MRecDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.NativeAdDemoMenuActivity
import com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded.RewardedVideosDemoMenuActivity
import com.applovin.sdk.AppLovinSdk
import com.applovin.enterprise.apps.demoapp.kotlin.R
import kotlinx.android.synthetic.main.activity_list.*
import java.util.*


class MainActivity : DemoMenuActivity()
{
    private lateinit var muteToggleMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        AppLovinSdk.getInstance(this).initializeSdk {
            // SDK finished initialization
        }

        // Set an identifier for the current user. This identifier will be tied to various analytics events and rewarded video validation
        AppLovinSdk.getInstance(this).userIdentifier = "support@applovin.com"

        // Check that SDK key is present in Android Manifest
        checkSdkKey()
    }

    override fun setupListViewFooter()
    {
        var appVersion = ""
        try
        {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            appVersion = pInfo.versionName
        }
        catch (e: PackageManager.NameNotFoundException)
        {
            e.printStackTrace()
        }

        val versionName = Build.VERSION_CODES::class.java.fields[android.os.Build.VERSION.SDK_INT].name
        val apiLevel = Build.VERSION.SDK_INT

        val footer = TextView(applicationContext)
        footer.setTextColor(Color.GRAY)
        footer.setPadding(0, 20, 0, 0)
        footer.gravity = Gravity.CENTER
        footer.textSize = 18f
        footer.text = "\nApp Version: $appVersion\nSDK Version: ${AppLovinSdk.VERSION}\nOS Version: $versionName (API Level $apiLevel)\n"

        list_view.addFooterView(footer)
        list_view.setFooterDividersEnabled(false)
    }

    private fun makeContactIntent(): Intent
    {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "text/plain"
        intent.data = Uri.parse("mailto:" + "support@applovin.com")
        intent.putExtra(Intent.EXTRA_SUBJECT, "Android SDK support")
        intent.putExtra(Intent.EXTRA_TEXT, "\n\n\n---\nSDK Version: ${AppLovinSdk.VERSION}")
        return Intent.createChooser(intent, "Send Email")
    }

    override fun getListViewContents(): Array<DemoMenuItem>
    {
        var items = arrayOf(
                DemoMenuItem("Interstitials", Intent(this, InterstitialDemoMenuActivity::class.java)),
                DemoMenuItem("Rewarded Videos (Incentivized Ads)", Intent(this, RewardedVideosDemoMenuActivity::class.java)),
                DemoMenuItem("Native Ads", Intent(this, NativeAdDemoMenuActivity::class.java)),
                DemoMenuItem("Banners", Intent(this, BannerDemoMenuActivity::class.java)),
                DemoMenuItem("MRECs", Intent(this, MRecDemoMenuActivity::class.java)),
                DemoMenuItem("Event Tracking", Intent(this, EventTrackingActivity::class.java)),
                DemoMenuItem("Resources",  Intent(Intent.ACTION_VIEW, Uri.parse("https://support.applovin.com/support/home"))),
                DemoMenuItem("Contact", makeContactIntent())
        )
        if (resources.getBoolean(R.bool.is_tablet))
        {
            val menuItems = ArrayList<DemoMenuItem>(items.size + 1)
            menuItems.addAll(Arrays.asList(*items))
            // Add Leaders menu item below MRECs.
            menuItems.add(5, DemoMenuItem("Leaders", Intent(this, LeaderDemoMenuActivity::class.java)))
            items = menuItems.toTypedArray()
        }
        return items;
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
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
