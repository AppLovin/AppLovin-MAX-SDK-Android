package com.applovin.enterprise.apps.demoapp.ads.max

import android.os.Bundle
import android.os.Handler
import android.view.View
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R

import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxRewardedAd
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * [android.app.Activity] used to show AppLovin MAX rewarded ads.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class RewardedAdActivity : BaseAdActivity(),
        MaxRewardedAdListener, MaxAdRevenueListener {
    private lateinit var rewardedAd: MaxRewardedAd
    private var retryAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewarded_ad)
        setTitle(R.string.activity_rewarded)

        setupCallbacksRecyclerView()

        rewardedAd = MaxRewardedAd.getInstance("YOUR_AD_UNIT_ID", this)

        rewardedAd.setListener(this)
        rewardedAd.setRevenueListener(this)

        rewardedAd.loadAd()
    }

    override fun onDestroy() {
        super.onDestroy()

        rewardedAd.setListener(null)
        rewardedAd.setRevenueListener(null)
    }

    fun showAd(view: View) {
        if (rewardedAd.isReady) {
            rewardedAd.showAd()
        }
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd) {
        // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'
        logCallback()

        // Reset retry attempt
        retryAttempt = 0
    }

    override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
        logCallback()

        // Rewarded ad failed to load. We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds).

        retryAttempt++
        val delayMillis = TimeUnit.SECONDS.toMillis(2.0.pow(min(6, retryAttempt)).toLong())

        Handler().postDelayed({ rewardedAd.loadAd() }, delayMillis)
    }

    override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
        logCallback()

        // Rewarded ad failed to display. We recommend loading the next ad.
        rewardedAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd) {
        logCallback()
    }

    override fun onAdHidden(ad: MaxAd) {
        logCallback()

        // Rewarded ad is hidden. Pre-load the next ad.
        rewardedAd.loadAd()
    }

    override fun onRewardedVideoStarted(ad: MaxAd) {
        logCallback()
    }

    override fun onRewardedVideoCompleted(ad: MaxAd) {
        logCallback()
    }

    override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
        // Rewarded ad was displayed and user should receive the reward.
        logCallback()
    }

    //endregion

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd) {
        logCallback()

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}
