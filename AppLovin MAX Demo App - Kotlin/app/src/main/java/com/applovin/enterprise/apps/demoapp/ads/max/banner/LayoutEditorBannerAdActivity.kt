package com.applovin.enterprise.apps.demoapp.ads.max.banner

import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView


/**
 * [android.app.Activity] used to show AppLovin MAX banner ads created in the Layout Editor.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class LayoutEditorBannerAdActivity : BaseAdActivity(),
        MaxAdViewAdListener, MaxAdRevenueListener {
    private lateinit var adView: MaxAdView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor_banner_ad)
        setTitle(R.string.activity_layout_editor_banners)

        setupCallbacksRecyclerView()

        adView = findViewById(R.id.bannerAdView)

        adView.setListener(this)
        adView.setRevenueListener(this)

        adView.loadAd()
    }

    override fun onDestroy() {
        super.onDestroy()

        adView.destroy()
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
        logCallback()
    }

    override fun onAdHidden(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
        logCallback()
    }

    override fun onAdDisplayed(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdExpanded(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdCollapsed(ad: MaxAd?) {
        logCallback()
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
