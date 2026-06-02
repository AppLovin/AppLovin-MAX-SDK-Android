package com.applovin.enterprise.apps.demoapp.ads.max.banner

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.BuildConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils

/**
 * A [android.app.Activity] to show AppLovin MAX banner ads.
 * <p>
 * Created by Harry Arakkal on 9/17/2019
 */
class ProgrammaticBannerAdActivity : BaseAdActivity(),
        MaxAdViewAdListener, MaxAdRevenueListener {
    private lateinit var adView: MaxAdView
    private var testPlaceholderView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programmatic_banner_ad)
        setTitle(R.string.activity_programmatic_banners)

        setupCallbacksRecyclerView()

        adView = MaxAdView(BuildConfig.MAX_AD_UNIT_ID, this)

        adView.setListener(this)
        adView.setRevenueListener(this)

        val isTablet = AppLovinSdkUtils.isTablet(this)
        val heightPx = AppLovinSdkUtils.dpToPx(this, if (isTablet) 90 else 50)

        adView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
        // Opaque background required; use dark gray when ad may not load
        adView.setBackgroundColor(0xFF2D2D2D.toInt())

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(adView)

        adView.loadAd()
    }

    override fun onDestroy() {
        super.onDestroy()

        adView.destroy()
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd) {
        logCallback()
    }

    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
        logCallback()
        if (isTestPlaceholderMode()) {
            showTestPlaceholderBanner()
        }
    }

    private fun showTestPlaceholderBanner() {
        if (testPlaceholderView != null) return
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val placeholder = TextView(this).apply {
            tag = "test_placeholder"
            text = getString(R.string.test_placeholder_message)
            setTextColor(Color.WHITE)
            setBackgroundColor(0xFF2D2D2D.toInt())
            setPadding(
                AppLovinSdkUtils.dpToPx(this@ProgrammaticBannerAdActivity, 16),
                AppLovinSdkUtils.dpToPx(this@ProgrammaticBannerAdActivity, 8),
                AppLovinSdkUtils.dpToPx(this@ProgrammaticBannerAdActivity, 16),
                AppLovinSdkUtils.dpToPx(this@ProgrammaticBannerAdActivity, 8)
            )
            textSize = 12f
            gravity = Gravity.CENTER
        }
        val heightPx = AppLovinSdkUtils.dpToPx(this, 70)
        placeholder.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
        rootView.addView(placeholder, 0)
        testPlaceholderView = placeholder
    }

    override fun onAdHidden(ad: MaxAd) {
        logCallback()
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
        logCallback()
    }

    override fun onAdDisplayed(ad: MaxAd) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd) {
        logCallback()
    }

    override fun onAdExpanded(ad: MaxAd) {
        logCallback()
    }

    override fun onAdCollapsed(ad: MaxAd) {
        logCallback()
    }

    //endregion

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd)
    {
        logCallback()

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}
