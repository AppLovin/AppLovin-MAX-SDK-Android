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
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.mediation.MaxAdFormat
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdSize

/**
 * Ad loader to load AppLovin banner/leader/MREC ads using Jetpack Compose.
 */
class AppLovinAdLoader(val adFormat: AppLovinAdSize,
                       context: Context,
                       private val callbacks: BaseJetpackComposeAdActivity)
{
    var adView: AppLovinAdView? = null

    init
    {
        val adLoadListener = object : AppLovinAdLoadListener
        {
            override fun adReceived(ad: AppLovinAd?)
            {
                callbacks.logCallback()
            }

            override fun failedToReceiveAd(errorCode: Int)
            {
                // Look at AppLovinErrorCodes.java for list of error codes
                callbacks.logCallback()
            }
        }

        val adDisplayListener = object : AppLovinAdDisplayListener
        {
            override fun adDisplayed(ad: AppLovinAd?)
            {
                callbacks.logCallback()
            }

            override fun adHidden(ad: AppLovinAd?)
            {
                callbacks.logCallback()
            }
        }

        val adViewEventListener = object : AppLovinAdViewEventListener
        {
            override fun adOpenedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?)
            {
                callbacks.logCallback()
            }

            override fun adClosedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?)
            {
                callbacks.logCallback()
            }

            override fun adLeftApplication(ad: AppLovinAd?, adView: AppLovinAdView?)
            {
                callbacks.logCallback()
            }

            override fun adFailedToDisplay(ad: AppLovinAd?, adView: AppLovinAdView?, code: AppLovinAdViewDisplayErrorCode?)
            {
                callbacks.logCallback()
            }
        }

        val adClickListener = object : AppLovinAdClickListener
        {
            override fun adClicked(ad: AppLovinAd?)
            {
                callbacks.logCallback()
            }
        }

        adView = AppLovinAdView(adFormat, context).apply {
            setAdLoadListener(adLoadListener)
            setAdDisplayListener(adDisplayListener)
            setAdViewEventListener(adViewEventListener)
            setAdClickListener(adClickListener)
        }
    }

    fun destroy()
    {
        adView?.destroy()
    }

    fun loadAd()
    {
        adView?.loadNextAd()
    }
}

/**
 * Jetpack Compose function to display AppLovin banner/leader/MREC ads.
 */
@Composable
fun AppLovinAdComposable(adLoader: AppLovinAdLoader)
{
    val adModifier = when(adLoader.adFormat) {
        AppLovinAdSize.BANNER -> Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black)
        AppLovinAdSize.LEADER -> Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.Black)
        AppLovinAdSize.MREC -> Modifier
            .width(300.dp)
            .height(250.dp)
            .background(Color.Black)
        else -> Modifier
            .fillMaxSize()
            .background(Color.Black)
    }

    AndroidView(
        factory = { adLoader.adView!! },
        update = { adLoader.adView!!.loadNextAd() }, // Load an ad once layout is inflated.
        modifier = adModifier
    )

    DisposableEffect(adLoader.adView)
    {
        onDispose {
            // Destroy adView when composition is no longer needed.
            adLoader.destroy()
        }
    }
}