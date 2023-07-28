package com.applovin.enterprise.apps.demoapp.ads.max.rewarded

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * [android.app.Activity] used to show AppLovin MAX rewarded ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-19.
 */

class JetpackComposeRewardedAdActivity : BaseJetpackComposeAdActivity(),
    MaxRewardedAdListener, MaxAdRevenueListener {
    private lateinit var rewardedAd: MaxRewardedAd
    private var retryAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_rewarded)

        rewardedAd = MaxRewardedAd.getInstance("YOUR_AD_UNIT_ID", this)

        rewardedAd.setListener(this)
        rewardedAd.setRevenueListener(this)

        // Load the first ad.
        rewardedAd.loadAd()

        setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopCenter)) {
                    ListCallbacks()
                }
                // If ad is ready, show ad whenever button is tapped.
                Button(
                    onClick = {
                        if (rewardedAd.isReady) {
                            rewardedAd.showAd()
                        }
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(Color.LightGray),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "SHOW AD",
                        color = Color.Black
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Destroy rewarded ad to prevent memory leaks.
        rewardedAd.destroy()
    }


    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) {
        // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'
        logCallback()

        // Reset retry attempt
        retryAttempt = 0
    }

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
        logCallback()

        // Rewarded ad failed to load. We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds).

        retryAttempt++
        val delayMillis = TimeUnit.SECONDS.toMillis(2.0.pow(min(6, retryAttempt)).toLong())

        // Load ad after delay.
        Handler(Looper.getMainLooper()).postDelayed({ rewardedAd.loadAd() }, delayMillis)
    }

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
        logCallback()

        // Rewarded ad failed to display. We recommend loading the next ad.
        rewardedAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdHidden(ad: MaxAd?) {
        logCallback()

        // Rewarded ad is hidden. Pre-load the next ad.
        rewardedAd.loadAd()
    }

    override fun onRewardedVideoStarted(ad: MaxAd?) {
        logCallback()
    }

    override fun onRewardedVideoCompleted(ad: MaxAd?) {
        logCallback()
    }

    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?) {
        // Rewarded ad was displayed and user should receive the reward.
        logCallback()
    }

    //endregion

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd?) {
        logCallback()

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad?.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad?.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad?.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad?.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}