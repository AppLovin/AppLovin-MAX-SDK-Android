package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdSize
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.applovin.sdk.AppLovinSdk

class InterstitialJetpackComposeActivity : BaseJetpackComposeAdActivity(),
    AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    private var currentAd: AppLovinAd? = null
    private var isAdLoading = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val interstitialAdDialog = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(this), this)

        interstitialAdDialog.setAdLoadListener(this)
        interstitialAdDialog.setAdDisplayListener(this)
        interstitialAdDialog.setAdClickListener(this)
        interstitialAdDialog.setAdVideoPlaybackListener(this) // This will only ever be used if you have video ads enabled.

        val activity = this
        setContent {
           Box(Modifier.fillMaxSize())
           {
               Box(Modifier.align(Alignment.TopCenter))
               {
                   ListCallbacks()
               }
               Row(Modifier.align(Alignment.BottomCenter))
               {
                   // Load interstitial ad before displaying.
                   TextButton(onClick = {
                       isAdLoading.value = true
                       AppLovinSdk.getInstance(applicationContext).adService.loadNextAd(AppLovinAdSize.INTERSTITIAL, activity)
                   })
                   {
                       Text("Load")
                   }

                   // Display ad only if ad finished loading.
                   TextButton(onClick = {
                       if (!isAdLoading.value) {
                           currentAd?.let{
                               interstitialAdDialog.showAndRender(it)
                           }
                       }
                   })
                   {
                       Text("Show")
                   }
               }
           }
        }
    }

    //region Ad Load Listener

    override fun adReceived(appLovinAd: AppLovinAd)
    {
        logCallback()
        currentAd = appLovinAd
        isAdLoading.value = false

    }

    override fun failedToReceiveAd(errorCode: Int)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback()
    }

    //endregion

    //region Ad Display Listener

    override fun adDisplayed(appLovinAd: AppLovinAd)
    {
        logCallback()
    }

    override fun adHidden(appLovinAd: AppLovinAd)
    {
        logCallback()
    }

    //endregion

    //region Ad Click Listener

    override fun adClicked(appLovinAd: AppLovinAd)
    {
        logCallback()
    }

    //endregion

    //region Ad Video Playback Listener

    override fun videoPlaybackBegan(appLovinAd: AppLovinAd)
    {
        logCallback()
    }

    override fun videoPlaybackEnded(appLovinAd: AppLovinAd, percentViewed: Double, wasFullyViewed: Boolean)
    {
        logCallback()
    }

    //endregion
}