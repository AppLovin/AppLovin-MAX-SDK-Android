package com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded

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
import com.applovin.adview.AppLovinIncentivizedInterstitial
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdRewardListener
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.applovin.sdk.AppLovinErrorCodes

/**
 * [android.app.Activity] used to show AppLovin rewarded video ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-27.
 */

class RewardedVideosJetpackComposeActivity : BaseJetpackComposeAdActivity(),
    AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener,
    AppLovinAdVideoPlaybackListener, AppLovinAdRewardListener {
    private var incentivizedInterstitial: AppLovinIncentivizedInterstitial? = null
    private var currentAd: AppLovinAd? = null
    private var isAdLoading = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopCenter)) { ListCallbacks() }
                Row(Modifier.align(Alignment.BottomCenter))
                {
                    // You need to preload each rewarded video before it can be displayed
                    TextButton(onClick = {
                        incentivizedInterstitial =
                            AppLovinIncentivizedInterstitial.create(applicationContext).apply {
                                isAdLoading.value = true
                                preload(this@RewardedVideosJetpackComposeActivity)
                            }
                    })
                    {
                        Text("Preload")
                    }
                    // Display rewarded video once it has been loaded by clicking button.
                    TextButton(
                        onClick = {
                            if (!isAdLoading.value) {
                                incentivizedInterstitial!!.show(
                                    this@RewardedVideosJetpackComposeActivity,
                                    this@RewardedVideosJetpackComposeActivity,
                                    this@RewardedVideosJetpackComposeActivity,
                                    this@RewardedVideosJetpackComposeActivity,
                                    this@RewardedVideosJetpackComposeActivity
                                )
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

    override fun adReceived(appLovinAd: AppLovinAd) {
        logCallback()
        currentAd = appLovinAd
        isAdLoading.value = false
    }

    override fun failedToReceiveAd(errorCode: Int) {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback()
    }

    //endregion

    //region Ad Display Listener

    override fun adDisplayed(appLovinAd: AppLovinAd) {
        logCallback()
    }

    override fun adHidden(appLovinAd: AppLovinAd) {
        logCallback()
    }

    //endregion

    //region Ad Click Listener

    override fun adClicked(appLovinAd: AppLovinAd) {
        logCallback()
    }

    //endregion

    //region Ad Video Playback Listener

    override fun videoPlaybackBegan(appLovinAd: AppLovinAd) {
        logCallback()
    }

    override fun videoPlaybackEnded(
        appLovinAd: AppLovinAd,
        percentViewed: Double,
        wasFullyViewed: Boolean
    ) {
        logCallback()
    }

    //endregion

    //region Ad Reward Listener

    override fun userRewardVerified(appLovinAd: AppLovinAd, map: Map<String, String>) {
        // AppLovin servers validated the reward. Refresh user balance from your server.  We will also pass the number of coins
        // awarded and the name of the currency.  However, ideally, you should verify this with your server before granting it.

        logCallback()

        // By default we'll show a alert informing your user of the currency & amount earned.
        // If you don't want this, you can turn it off in the Manage Apps UI.
    }

    override fun userOverQuota(appLovinAd: AppLovinAd, map: Map<String, String>) {
        // Your user has already earned the max amount you allowed for the day at this point, so
        // don't give them any more currency. By default we'll show them a alert explaining this,
        // though you can change that from the AppLovin dashboard.

        logCallback()
    }

    override fun userRewardRejected(appLovinAd: AppLovinAd, map: Map<String, String>) {
        // Your user couldn't be granted a reward for this view. This could happen if you've blacklisted
        // them, for example. Don't grant them any currency. By default we'll show them an alert explaining this,
        // though you can change that from the AppLovin dashboard.

        logCallback()
    }

    override fun validationRequestFailed(appLovinAd: AppLovinAd, responseCode: Int) {
        when (responseCode) {
            AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO -> {
                // Your user exited the video prematurely. It's up to you if you'd still like to grant
                // a reward in this case. Most developers choose not to. Note that this case can occur
                // after a reward was initially granted (since reward validation happens as soon as a
                // video is launched).
            }

            AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT, AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR -> {
                // Some server issue happened here. Don't grant a reward. By default we'll show the user
                // a alert telling them to try again later, but you can change this in the
                // AppLovin dashboard.
            }

            AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED -> {
                // Indicates that the developer called for a rewarded video before one was available.
                // Note: This code is only possible when working with rewarded videos.
            }
        }
        logCallback()
    }

    //endregion
}