package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.ads.MaxInterstitialAd

/**
 * [android.app.Activity] used to show AppLovin MAX interstitial ads.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class InterstitialAdActivity : AppCompatActivity(),
        MaxAdListener
{
    private lateinit var interstitialAd: MaxInterstitialAd

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial_ad)
        setTitle(R.string.activity_interstitial)

        interstitialAd = MaxInterstitialAd("YOUR_AD_UNIT_AD", this)
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
    }

    override fun onAdLoadFailed(adUnitId: String?, errorCode: Int)
    {
        // Interstitial ad failed to load. We recommend retrying in 3 seconds.
        Handler().postDelayed({ interstitialAd.loadAd() }, 3000)
    }

    override fun onAdDisplayFailed(ad: MaxAd?, errorCode: Int)
    {
        // Interstitial ad failed to display. We recommend loading the next ad.
        interstitialAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd?) {}

    override fun onAdClicked(ad: MaxAd?) {}

    override fun onAdHidden(ad: MaxAd?)
    {
        // Interstitial Ad is hidden. Pre-load the next ad
        interstitialAd.loadAd()
    }

    //endregion
}
