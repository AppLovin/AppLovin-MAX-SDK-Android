package com.applovin.enterprise.apps.demoapp.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView

/**
 * View Model for Max banner/leader/MREC ads using Jetpack Compose.
 */
class MaxAdViewComposableViewModel(baseActivity: BaseJetpackComposeAdActivity) : ViewModel() {
    val adListener = object : MaxAdViewAdListener {
        override fun onAdLoaded(ad: MaxAd) {
            baseActivity.logCallback()
        }

        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
            baseActivity.logCallback()
        }

        override fun onAdHidden(ad: MaxAd) {
            baseActivity.logCallback()
        }

        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
            baseActivity.logCallback()
        }

        override fun onAdDisplayed(ad: MaxAd) {
            baseActivity.logCallback()
        }

        override fun onAdClicked(ad: MaxAd) {
            baseActivity.logCallback()
        }

        override fun onAdExpanded(ad: MaxAd) {
            baseActivity.logCallback()
        }

        override fun onAdCollapsed(ad: MaxAd) {
            baseActivity.logCallback()
        }
    }

    val revenueListener = object : MaxAdRevenueListener {
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
}

/**
 * Jetpack Compose function used to display MAX banner/leader/MREC ads.
 */
@Composable
fun MaxAdViewComposable(
    adUnitId: String,
    adFormat: MaxAdFormat,
    viewModel: MaxAdViewComposableViewModel
) {
    val context = LocalContext.current

    val adViewModifier = when (adFormat) {
        // Set background or background color for ads to be fully functional.
        MaxAdFormat.BANNER -> Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black)

        MaxAdFormat.LEADER -> Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.Black)

        MaxAdFormat.MREC -> Modifier
            .width(300.dp)
            .height(250.dp)
            .background(Color.Black)

        else -> Modifier
            .fillMaxSize()
            .background(Color.Black)
    }

    val adView = remember {
        MaxAdView(adUnitId, adFormat, context).apply {
            setListener(viewModel.adListener)
            setRevenueListener(viewModel.revenueListener)
            loadAd()
        }
    }

    AndroidView(
        factory = { adView },
        modifier = adViewModifier
    )

    DisposableEffect(adView) {
        onDispose {
            // Destroy MaxAdView when composition is no longer needed.
            adView.destroy()
        }
    }
}