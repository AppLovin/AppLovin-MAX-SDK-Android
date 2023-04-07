package com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded

import android.os.Bundle
import android.widget.Button
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.sdk.*

class RewardedVideosActivity : BaseAdActivity(),
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener
{
    private var incentivizedInterstitial: AppLovinIncentivizedInterstitial? = null
    private lateinit var showButton: Button

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewarded_videos)

        setupCallbacksRecyclerView()

        // You need to preload each rewarded video before it can be displayed
        val loadButton = findViewById<Button>(R.id.loadButton)
        showButton = findViewById(R.id.showButton)

        loadButton.setOnClickListener {
            showButton.isEnabled = false

            incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(applicationContext).apply {
                preload(this@RewardedVideosActivity)
            }
        }

        showButton.setOnClickListener {
            showButton.isEnabled = false

            incentivizedInterstitial!!.show(this, this, this, this, this)
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

    //region Ad Reward Listener

    override fun userRewardVerified(appLovinAd: AppLovinAd, map: Map<String, String>)
    {
        // AppLovin servers validated the reward. Refresh user balance from your server.  We will also pass the number of coins
        // awarded and the name of the currency.  However, ideally, you should verify this with your server before granting it.

        logCallback()

        // By default we'll show a alert informing your user of the currency & amount earned.
        // If you don't want this, you can turn it off in the Manage Apps UI.
    }

    override fun userOverQuota(appLovinAd: AppLovinAd, map: Map<String, String>)
    {
        // Your user has already earned the max amount you allowed for the day at this point, so
        // don't give them any more currency. By default we'll show them a alert explaining this,
        // though you can change that from the AppLovin dashboard.

        logCallback()
    }

    override fun userRewardRejected(appLovinAd: AppLovinAd, map: Map<String, String>)
    {
        // Your user couldn't be granted a reward for this view. This could happen if you've blacklisted
        // them, for example. Don't grant them any currency. By default we'll show them an alert explaining this,
        // though you can change that from the AppLovin dashboard.

        logCallback()
    }

    override fun validationRequestFailed(appLovinAd: AppLovinAd, responseCode: Int)
    {
        when (responseCode)
        {
            AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO ->
            {
                // Your user exited the video prematurely. It's up to you if you'd still like to grant
                // a reward in this case. Most developers choose not to. Note that this case can occur
                // after a reward was initially granted (since reward validation happens as soon as a
                // video is launched).
            }
            AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT, AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR ->
            {
                // Some server issue happened here. Don't grant a reward. By default we'll show the user
                // a alert telling them to try again later, but you can change this in the
                // AppLovin dashboard.
            }
            AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED ->
            {
                // Indicates that the developer called for a rewarded video before one was available.
                // Note: This code is only possible when working with rewarded videos.
            }
        }
        logCallback()
    }

    //endregion
}
