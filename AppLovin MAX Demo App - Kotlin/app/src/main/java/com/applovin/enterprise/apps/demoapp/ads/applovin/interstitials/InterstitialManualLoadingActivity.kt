package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.os.Bundle
import android.widget.Button
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.sdk.*

class InterstitialManualLoadingActivity : BaseAdActivity(),
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    private var currentAd: AppLovinAd? = null
    private lateinit var showButton: Button

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial_manual_loading)

        setupCallbacksRecyclerView()

        val sdk = AppLovinSdk.getInstance(applicationContext)
        val interstitialAdDialog = AppLovinInterstitialAd.create(sdk, this)

        interstitialAdDialog.setAdDisplayListener(this)
        interstitialAdDialog.setAdClickListener(this)
        interstitialAdDialog.setAdVideoPlaybackListener(this) // This will only ever be used if you have video ads enabled.

        val loadButton = findViewById<Button>(R.id.loadButton)
        showButton = findViewById(R.id.showButton)

        loadButton.setOnClickListener {
            showButton.isEnabled = false

            AppLovinSdk.getInstance(applicationContext).adService.loadNextAd(AppLovinAdSize.INTERSTITIAL, this)
        }

        showButton.setOnClickListener {
            currentAd?.let {
                interstitialAdDialog.showAndRender(it)
            }
        }
    }

    //region Ad Load Listener

    override fun adReceived(appLovinAd: AppLovinAd)
    {
        logCallback()
        currentAd = appLovinAd
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
