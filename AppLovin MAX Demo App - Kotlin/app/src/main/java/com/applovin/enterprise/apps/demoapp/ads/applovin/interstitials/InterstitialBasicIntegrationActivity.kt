package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.os.Bundle
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.sdk.*
import kotlinx.android.synthetic.main.activity_interstitial_basic_integration.*

class InterstitialBasicIntegrationActivity : BaseAdActivity(), AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial_basic_integration)

        setupCallbacksRecyclerView()

        val interstitialAdDialog = AppLovinInterstitialAd.create(AppLovinSdk.getInstance(this), this)

        interstitialAdDialog.setAdLoadListener(this)
        interstitialAdDialog.setAdDisplayListener(this)
        interstitialAdDialog.setAdClickListener(this)
        interstitialAdDialog.setAdVideoPlaybackListener(this) // This will only ever be used if you have video ads enabled.

        showButton.setOnClickListener {
            showButton.isEnabled = false

            interstitialAdDialog.show()
        }
    }

    //region Ad Load Listener

    override fun adReceived(appLovinAd: AppLovinAd)
    {
        logCallback()
        showButton.isEnabled = true
    }

    override fun failedToReceiveAd(errorCode: Int)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback()
        showButton.isEnabled = true
    }

    //endregion

    //region Ad Display Listener

    override fun adDisplayed(appLovinAd: AppLovinAd) { logCallback() }

    override fun adHidden(appLovinAd: AppLovinAd) { logCallback() }

    //endregion

    //region Ad Click Listener

    override fun adClicked(appLovinAd: AppLovinAd) { logCallback() }

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
