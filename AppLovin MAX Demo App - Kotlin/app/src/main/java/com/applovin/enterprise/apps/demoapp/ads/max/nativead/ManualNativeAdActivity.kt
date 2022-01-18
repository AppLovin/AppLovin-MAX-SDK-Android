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
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder

class ManualNativeAdActivity : BaseAdActivity(), MaxAdRevenueListener {
    private lateinit var nativeAdLoader: MaxNativeAdLoader
    private lateinit var nativeAdView: MaxNativeAdView
    private lateinit var nativeAdLayout: FrameLayout

    private var nativeAd: MaxAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_manual)
        setTitle(R.string.activity_manual_natives)

        nativeAdLayout = findViewById(R.id.native_ad_layout)
        setupCallbacksRecyclerView()

        val binder: MaxNativeAdViewBinder = MaxNativeAdViewBinder.Builder(R.layout.native_custom_ad_view)
                .setTitleTextViewId(R.id.title_text_view)
                .setBodyTextViewId(R.id.body_text_view)
                .setAdvertiserTextViewId(R.id.advertiser_textView)
                .setIconImageViewId(R.id.icon_image_view)
                .setMediaContentViewGroupId(R.id.media_view_container)
                .setOptionsContentViewGroupId(R.id.options_view)
                .setCallToActionButtonId(R.id.cta_button)
                .build()
        nativeAdView = MaxNativeAdView(binder, this)

        val adUnitId = "YOUR_AD_UNIT_ID"

        nativeAdLoader = MaxNativeAdLoader(adUnitId, this)
        nativeAdLoader.placement = "Native Custom Test Placement"
        nativeAdLoader.setExtraParameter("test_extra_key", "test_extra_value")
        nativeAdLoader.setRevenueListener(this)
        nativeAdLoader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView, ad: MaxAd) {
                logCallback()

                // Cleanup any pre-existing native ad to prevent memory leaks.
                if (nativeAd != null) {
                    nativeAdLoader.destroy(nativeAd)
                }

                // Save ad for cleanup.
                nativeAd = ad

                // Add ad view to view.
                nativeAdLayout.removeAllViews()
                nativeAdLayout.addView(nativeAdView)
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                logCallback()
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                logCallback()
            }
        })
    }

    override fun onDestroy() {
        // Must destroy native ad or else there will be memory leaks.
        if (nativeAd != null) {
            // Call destroy on the native ad from any native ad loader.
            nativeAdLoader.destroy(nativeAd)
        }
        super.onDestroy()
    }

    fun showAd(view: View) {
        nativeAdLoader.loadAd(nativeAdView)
    }

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd?) {
        logCallback(2)

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad?.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad?.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad?.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad?.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}