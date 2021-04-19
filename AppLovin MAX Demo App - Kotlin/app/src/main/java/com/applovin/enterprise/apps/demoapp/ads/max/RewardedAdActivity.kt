package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import android.os.Handler
import android.view.View
import com.applovin.enterprise.apps.demoapp.R

import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd
import java.util.concurrent.TimeUnit

/**
 * [android.app.Activity] used to show AppLovin MAX rewarded ads.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class RewardedAdActivity : BaseAdActivity(),
    MaxRewardedAdListener
{
    private lateinit var rewardedAd: MaxRewardedAd
    private var retryAttempt = 0.0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewarded_ad)
        setTitle(R.string.activity_rewarded)

        setupCallbacksRecyclerView()

        rewardedAd = MaxRewardedAd.getInstance("YOUR_AD_UNIT_ID", this)
        rewardedAd.setListener(this)

        rewardedAd.loadAd()
    }

    fun showAd(view: View)
    {
        if (rewardedAd.isReady)
        {
            rewardedAd.showAd()
        }
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd)
    {
        // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'
        logCallback()

        // Reset retry attempt
        retryAttempt = 0.0
    }

    override fun onAdLoadFailed(adUnitId: String, errorCode: Int)
    {
        logCallback()

        // Rewarded ad failed to load. We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds).

        retryAttempt++
        val delayMillis = TimeUnit.SECONDS.toMillis(Math.pow(2.0, Math.min(6.0, retryAttempt)).toLong())

        Handler().postDelayed({ rewardedAd.loadAd() }, delayMillis)
    }

    override fun onAdDisplayFailed(ad: MaxAd, errorCode: Int)
    {
        logCallback()

        // Rewarded ad failed to display. We recommend loading the next ad.
        rewardedAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd) { logCallback() }

    override fun onAdClicked(ad: MaxAd) { logCallback() }

    override fun onAdHidden(ad: MaxAd)
    {
        logCallback()

        // Rewarded ad is hidden. Pre-load the next ad.
        rewardedAd.loadAd()
    }

    override fun onRewardedVideoStarted(ad: MaxAd) { logCallback() }

    override fun onRewardedVideoCompleted(ad: MaxAd) { logCallback() }

    override fun onUserRewarded(ad: MaxAd, reward: MaxReward?)
    {
        // Rewarded ad was displayed and user should receive the reward.
        logCallback()
    }

    //endregion
}
