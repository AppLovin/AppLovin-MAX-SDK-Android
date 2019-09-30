package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd

/**
 * [android.app.Activity] used to show AppLovin MAX rewarded ads.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class RewardedAdActivity : AppCompatActivity(),
    MaxRewardedAdListener
{
    private var rewardedAd: MaxRewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewarded_ad)
        setTitle(R.string.activity_rewarded)

        rewardedAd = MaxRewardedAd.getInstance("YOUR_AD_UNIT_ID", this)
        rewardedAd!!.setListener(this)

        rewardedAd!!.loadAd()
    }

    fun showAd(view: View)
    {
        if (rewardedAd!!.isReady)
        {
            rewardedAd!!.showAd()
        }
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?)
    {
        // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'
    }

    override fun onAdLoadFailed(adUnitId: String?, errorCode: Int)
    {
        // Rewarded ad failed to load. We recommend retrying in 3 seconds.
        Handler().postDelayed({ rewardedAd!!.loadAd() }, 3000)
    }

    override fun onAdDisplayFailed(ad: MaxAd?, errorCode: Int)
    {
        // Rewarded ad failed to display. We recommend loading the next ad.
        rewardedAd!!.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd?) {}

    override fun onAdClicked(ad: MaxAd?) {}

    override fun onAdHidden(ad: MaxAd?)
    {
        // Rewarded ad is hidden. Pre-load the next ad.
        rewardedAd!!.loadAd()
    }

    override fun onRewardedVideoStarted(ad: MaxAd?) {}

    override fun onRewardedVideoCompleted(ad: MaxAd?) {}

    override fun onUserRewarded(ad: MaxAd?, reward: MaxReward?)
    {
        // Rewarded ad was displayed and user should receive the reward.
    }

    //endregion
}
