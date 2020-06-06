package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import android.os.Handler
import android.view.View
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.ads.MaxInterstitialAd
import java.util.concurrent.TimeUnit

/**
 * [android.app.Activity] used to show AppLovin MAX interstitial ads.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class InterstitialAdActivity : BaseAdActivity(),
        MaxAdListener
{
    private lateinit var interstitialAd: MaxInterstitialAd
    private var retryAttempt = 0.0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial_ad)
        setTitle(R.string.activity_interstitial)

        setupCallbacksRecyclerView()

        interstitialAd = MaxInterstitialAd("YOUR_AD_UNIT_ID", this)
        interstitialAd.setListener(this)

        // Load the first ad.
        interstitialAd.loadAd()
    }

    fun showAd(view: View)
    {
        if (interstitialAd.isReady)
        {
            interstitialAd.showAd()
        }
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?)
    {
        // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'.
        logCallback()

        // Reset retry attempt
        retryAttempt = 0.0
    }

    override fun onAdLoadFailed(adUnitId: String?, errorCode: Int)
    {
        logCallback()

        // Interstitial ad failed to load. We recommend retrying with exponentially higher delays.

        retryAttempt++
        val delayMillis = TimeUnit.SECONDS.toMillis(Math.pow(2.0, retryAttempt).toLong())

        Handler().postDelayed({ interstitialAd.loadAd() }, delayMillis)
    }

    override fun onAdDisplayFailed(ad: MaxAd?, errorCode: Int)
    {
        logCallback()

        // Interstitial ad failed to display. We recommend loading the next ad.
        interstitialAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd?) { logCallback() }

    override fun onAdClicked(ad: MaxAd?) { logCallback() }

    override fun onAdHidden(ad: MaxAd?)
    {
        logCallback()

        // Interstitial Ad is hidden. Pre-load the next ad
        interstitialAd.loadAd()
    }

    //endregion
}
