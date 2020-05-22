package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.os.Bundle
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.applovin.sdk.AppLovinSdk
import kotlinx.android.synthetic.main.activity_interstitial_basic_integration.*

class InterstitialBasicIntegrationActivity : AdStatusActivity(), AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial_basic_integration)

        adStatusTextView = status_label

        val interstitialAdDialog = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(this), this)

        showButton.setOnClickListener {
            showButton.isEnabled = false

            log("Showing...")

            //
            // Optional: Set ad load, ad display, ad click, and ad video playback callback listeners
            //
            interstitialAdDialog.setAdLoadListener(this)
            interstitialAdDialog.setAdDisplayListener(this)
            interstitialAdDialog.setAdClickListener(this)
            interstitialAdDialog.setAdVideoPlaybackListener(this) // This will only ever be used if you have video ads enabled.

            interstitialAdDialog.show()
        }
    }

    //
    // Ad Load Listener
    //
    override fun adReceived(appLovinAd: AppLovinAd)
    {
        log("Interstitial loaded")
        showButton.isEnabled = true
    }

    override fun failedToReceiveAd(errorCode: Int)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        log("Interstitial failed to load with error code " + errorCode)

        showButton.isEnabled = true
    }

    //
    // Ad Display Listener
    //
    override fun adDisplayed(appLovinAd: AppLovinAd)
    {
        log("Interstitial Displayed")
    }

    override fun adHidden(appLovinAd: AppLovinAd)
    {
        log("Interstitial Hidden")
    }

    //
    // Ad Click Listener
    //
    override fun adClicked(appLovinAd: AppLovinAd)
    {
        log("Interstitial Clicked")
    }

    //
    // Ad Video Playback Listener
    //
    override fun videoPlaybackBegan(appLovinAd: AppLovinAd)
    {
        log("Video Started")
    }

    override fun videoPlaybackEnded(appLovinAd: AppLovinAd, percentViewed: Double, wasFullyViewed: Boolean)
    {
        log("Video Ended")
    }
}
