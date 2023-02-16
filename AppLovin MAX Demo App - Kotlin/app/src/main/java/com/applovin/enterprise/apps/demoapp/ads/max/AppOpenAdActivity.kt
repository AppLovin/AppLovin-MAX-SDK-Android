package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import android.view.View
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd

/**
 * [android.app.Activity] used to show AppLovin MAX App Open ads.
 * <p>
 * Created by avileung on 2023-02-10.
 */
class AppOpenAdActivity : BaseAdActivity(),
        MaxAdListener, MaxAdRevenueListener {
    private lateinit var appOpenAd: MaxAppOpenAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_open_ad)
        setTitle(R.string.activity_app_open)

        setupCallbacksRecyclerView()

        appOpenAd = MaxAppOpenAd("YOUR_AD_UNIT_ID", this)

        appOpenAd.setListener(this)
        appOpenAd.setRevenueListener(this)

        // Load the first ad.
        appOpenAd.loadAd()
    }

    fun showAd(view: View) {
        if (appOpenAd.isReady) {
            appOpenAd.showAd()
        }
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) {
        // App Open ad is ready to be shown. AppOpenAd.isReady() will now return 'true'.
        logCallback()
    }

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
        logCallback()
    }

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
        logCallback()

        // App Open ad failed to display. We recommend loading the next ad.
        appOpenAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdHidden(ad: MaxAd?) {
        logCallback()

        // App Open Ad is hidden. Pre-load the next ad
        appOpenAd.loadAd()
    }

    //endregion

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd?) {
        logCallback()

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad?.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad?.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad?.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad?.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}