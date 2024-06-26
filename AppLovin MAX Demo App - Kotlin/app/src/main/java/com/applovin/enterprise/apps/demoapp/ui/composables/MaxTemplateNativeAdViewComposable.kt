package com.applovin.enterprise.apps.demoapp.ui.composables

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView

/**
 * Ad loader to load Max Native ads with Templates API using Jetpack Compose.
 */
class MaxTemplateNativeAdViewComposableLoader(
    adUnitIdentifier: String,
    context: Context,
    baseActivity: BaseJetpackComposeAdActivity
) {
    var nativeAdView = mutableStateOf<MaxNativeAdView?>(null)
    private var nativeAd: MaxAd? = null
    private var nativeAdLoader: MaxNativeAdLoader

    init {
        nativeAdLoader = MaxNativeAdLoader(adUnitIdentifier, context)
        val adRevenueListener = object : MaxAdRevenueListener {
            override fun onAdRevenuePaid(ad: MaxAd) {
                baseActivity.logCallback()

                val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
                adjustAdRevenue.setRevenue(ad.revenue, "USD")
                adjustAdRevenue.setAdRevenueNetwork(ad.networkName)
                adjustAdRevenue.setAdRevenueUnit(ad.adUnitId)
                adjustAdRevenue.setAdRevenuePlacement(ad.placement)

                Adjust.trackAdRevenue(adjustAdRevenue)
            }
        }

        val adListener = object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(loadedNativeAdView: MaxNativeAdView?, ad: MaxAd) {
                baseActivity.logCallback()
                // Cleanup any pre-existing native ad to prevent memory leaks.
                if (nativeAd != null) {
                    nativeAdLoader.destroy(nativeAd)
                    nativeAdView.value?.let {
                        it.removeAllViews()
                        it.addView(loadedNativeAdView)
                    }
                }

                nativeAd = ad // Save ad for cleanup.
                nativeAdView.value = loadedNativeAdView
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: MaxError) {
                baseActivity.logCallback()
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                baseActivity.logCallback()
            }

            override fun onNativeAdExpired(nativeAd: MaxAd) {
                baseActivity.logCallback()
            }
        }
        nativeAdLoader.apply {
            setRevenueListener(adRevenueListener)
            setNativeAdListener(adListener)
        }
    }

    fun destroy() {
        // Must destroy native ad or else there will be memory leaks.
        if (nativeAd != null) {
            // Call destroy on the native ad from any native ad loader.
            nativeAdLoader.destroy(nativeAd)
        }

        // Destroy the actual loader itself
        nativeAdLoader.destroy()
    }

    fun loadAd() {
        nativeAdLoader.loadAd()
    }
}

/**
 * Jetpack Compose function to display MAX native ads using the Templates API.
 */
@Composable
fun MaxTemplateNativeAdViewComposable(nativeAdLoader: MaxTemplateNativeAdViewComposableLoader) {
    nativeAdLoader.nativeAdView.value?.let { adView ->
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth()
                .background(Color.Black)
        )
    }
}