package com.applovin.enterprise.apps.demoapp.ui.composables

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
 * Ad loader to load Max banner/leader/MREC ads using Jetpack Compose.
 */
class MaxAdLoader(adUnitId: String,
                  val adFormat: MaxAdFormat,
                  context: Context,
                  private val callbacks: BaseJetpackComposeAdActivity)
{
    var adView: MaxAdView? = null

    init {
        val adListener = object : MaxAdViewAdListener
        {
            override fun onAdLoaded(ad: MaxAd?)
            {
                callbacks.logCallback()
            }

            override fun onAdLoadFailed(adUnitId: String?, error: MaxError?)
            {
                callbacks.logCallback()
            }

            // deprecated?
            override fun onAdHidden(ad: MaxAd?)
            {
                callbacks.logCallback()
            }

            override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?)
            {
                callbacks.logCallback()
            }

            // deprecated?
            override fun onAdDisplayed(ad: MaxAd?)
            {
                callbacks.logCallback()
            }

            override fun onAdClicked(ad: MaxAd?)
            {
                callbacks.logCallback()
            }

            override fun onAdExpanded(ad: MaxAd?)
            {
                callbacks.logCallback()
            }

            override fun onAdCollapsed(ad: MaxAd?)
            {
                callbacks.logCallback()
            }
        }

        val revenueListener = object : MaxAdRevenueListener
        {
            override fun onAdRevenuePaid(ad: MaxAd?)
            {
                callbacks.logCallback()

                val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
                adjustAdRevenue.setRevenue(ad?.revenue, "USD")
                adjustAdRevenue.setAdRevenueNetwork(ad?.networkName)
                adjustAdRevenue.setAdRevenueUnit(ad?.adUnitId)
                adjustAdRevenue.setAdRevenuePlacement(ad?.placement)

                Adjust.trackAdRevenue(adjustAdRevenue)
            }
        }

        adView = MaxAdView(adUnitId, adFormat, context).apply {
            setListener(adListener)
            setRevenueListener(revenueListener)
            loadAd()
        }
    }

    fun destroy()
    {
        adView?.destroy()
    }
}

/**
 * Jetpack Compose function used to display MAX banner/leader/MREC ads.
 */
@Composable
fun MaxAdComposable(adLoader: MaxAdLoader)
{
    val adModifier = when(adLoader.adFormat) {
        MaxAdFormat.BANNER -> Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black)// Need to set the background color for MRECs to be fully functional.
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

    AndroidView(
        factory = { adLoader.adView!! },
        modifier = adModifier
    )

    DisposableEffect(adLoader.adView)
    {
        onDispose {
            // Destroy MaxAdView when composition is no longer needed.
            adLoader.destroy()
        }
    }
}