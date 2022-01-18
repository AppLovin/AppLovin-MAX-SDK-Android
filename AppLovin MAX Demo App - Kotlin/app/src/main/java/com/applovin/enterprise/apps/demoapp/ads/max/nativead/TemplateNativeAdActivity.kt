package com.applovin.enterprise.apps.demoapp.ads.max.nativead

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import java.util.*

class TemplateNativeAdActivity : BaseAdActivity(), MaxAdRevenueListener {
    // Map of ad unit IDs to native ad loaders
    private val nativeAdLoaders: Map<String, MaxNativeAdLoader> = HashMap(2)
    private var nativeAd: MaxAd? = null

    private lateinit var nativeAdLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_template)
        setTitle(R.string.activity_template_natives)

        nativeAdLayout = findViewById(R.id.native_ad_layout)
        setupCallbacksRecyclerView()
    }

    override fun onDestroy() {
        // Must destroy native ad or else there will be memory leaks.
        if (nativeAdLoaders.isNotEmpty()) {
            // Call destroy on the native ad from any native ad loader.
            val nativeAdLoader = nativeAdLoaders.entries.iterator().next().value
            nativeAdLoader.destroy(nativeAd)
        }
        super.onDestroy()
    }

    fun showAd(view: View) {
        val adUnitId = "YOUR_AD_UNIT_ID"

        val nativeAdLoader = if (nativeAdLoaders.containsKey(adUnitId)) {
            nativeAdLoaders[adUnitId]
        } else {
            MaxNativeAdLoader(adUnitId, this).also {
                it.placement = "Native Template Test Placement"
                it.setExtraParameter("test_extra_key", "test_extra_value")
                it.setRevenueListener(this)
                it.setNativeAdListener(object : MaxNativeAdListener() {
                    override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView, ad: MaxAd) {
                        // Cleanup any pre-existing native ad to prevent memory leaks.
                        if (nativeAd != null) {
                            it.destroy(nativeAd)
                        }

                        // Save ad for cleanup.
                        nativeAd = ad

                        // Add ad view to view.
                        nativeAdLayout.removeAllViews()
                        nativeAdLayout.addView(nativeAdView)
                        logCallback()
                    }

                    override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                        logCallback()
                    }

                    override fun onNativeAdClicked(ad: MaxAd) {
                        logCallback()
                    }
                })
            }
        }

        nativeAdLoader?.loadAd()
    }

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