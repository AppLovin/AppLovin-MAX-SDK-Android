package com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded

import android.os.Bundle
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.sdk.*
import kotlinx.android.synthetic.main.activity_rewarded_videos.*

class RewardedVideosZoneActivity : AdStatusActivity()
{
    private var incentivizedInterstitial: AppLovinIncentivizedInterstitial? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewarded_videos)

        adStatusTextView = status_label

        // You need to preload each rewarded video before it can be displayed
        loadButton.setOnClickListener {
            log("Rewarded video loading...")
            showButton.isEnabled = false

            incentivizedInterstitial = AppLovinIncentivizedInterstitial.create("YOUR_ZONE_ID", AppLovinSdk.getInstance(applicationContext)).apply {
                // Set an optional user identifier used for S2S callbacks

                preload(object : AppLovinAdLoadListener
                        {
                            override fun adReceived(appLovinAd: AppLovinAd)
                            {
                                log("Rewarded video loaded.")
                                showButton.isEnabled = true
                            }

                            override fun failedToReceiveAd(errorCode: Int)
                            {
                                log("Rewarded video failed to load with error code " + errorCode)
                            }
                        })
            }
        }

        showButton.setOnClickListener {

            showButton.isEnabled = false

            //
            // OPTIONAL: Create listeners
            //

            // Reward Listener
            val adRewardListener = object : AppLovinAdRewardListener
            {
                override fun userRewardVerified(appLovinAd: AppLovinAd, map: Map<String, String>)
                {
                    // AppLovin servers validated the reward. Refresh user balance from your server.  We will also pass the number of coins
                    // awarded and the name of the currency.  However, ideally, you should verify this with your server before granting it.

                    // i.e. - "Coins", "Gold", whatever you set in the dashboard.
                    val currencyName = map["currency"] as String

                    // For example, "5" or "5.00" if you've specified an amount in the UI.
                    val amountGivenString = map["amount"] as String

                    log("Rewarded $amountGivenString $currencyName")

                    // By default we'll show a alert informing your user of the currency & amount earned.
                    // If you don't want this, you can turn it off in the Manage Apps UI.
                }

                override fun userOverQuota(appLovinAd: AppLovinAd, map: Map<String, String>)
                {
                    // Your user has already earned the max amount you allowed for the day at this point, so
                    // don't give them any more currency. By default we'll show them a alert explaining this,
                    // though you can change that from the AppLovin dashboard.

                    log("Reward validation request exceeded quota with response: " + map)
                }

                override fun userRewardRejected(appLovinAd: AppLovinAd, map: Map<String, String>)
                {
                    // Your user couldn't be granted a reward for this view. This could happen if you've blacklisted
                    // them, for example. Don't grant them any currency. By default we'll show them an alert explaining this,
                    // though you can change that from the AppLovin dashboard.

                    log("Reward validation request was rejected with response: " + map)
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
                    log("Reward validation request failed with error code: " + responseCode)
                }

                override fun userDeclinedToViewAd(appLovinAd: AppLovinAd)
                {
                    // This method will be invoked if the user selected "no" when asked if they want to view an ad.
                    // If you've disabled the pre-video prompt in the "Manage Apps" UI on our website, then this method won't be called.

                    log("User declined to view ad")
                }
            }

            // Video Playback Listener
            val adVideoPlaybackListener = object : AppLovinAdVideoPlaybackListener
            {
                override fun videoPlaybackBegan(appLovinAd: AppLovinAd)
                {
                    log("Video Started")
                }

                override fun videoPlaybackEnded(appLovinAd: AppLovinAd, percentViewed: Double, fullyWatched: Boolean)
                {
                    log("Video Ended")
                }
            }

            // Ad Display Listener
            val adDisplayListener = object : AppLovinAdDisplayListener
            {
                override fun adDisplayed(appLovinAd: AppLovinAd)
                {
                    log("Ad Displayed")
                }

                override fun adHidden(appLovinAd: AppLovinAd)
                {
                    log("Ad Dismissed")
                }
            }

            // Ad Click Listener
            val adClickListener = AppLovinAdClickListener { log("Ad Click") }

            incentivizedInterstitial!!.show(this, adRewardListener, adVideoPlaybackListener, adDisplayListener, adClickListener)
        }
    }
}
