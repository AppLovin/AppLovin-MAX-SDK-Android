package com.applovin.enterprise.apps.demoapp.ads

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils

/**
 * A [android.app.Activity] to show AppLovin MAX banner ads.
 * <p>
 * Created by Harry Arakkal on 9/17/2019
 */
class ProgrammaticBannerAdActivity : AppCompatActivity(),
        MaxAdViewAdListener
{
    private lateinit var adView: MaxAdView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programmatic_banner_ad)
        setTitle(R.string.activity_programmatic_banners)

        adView = MaxAdView("YOUR_AD_UNIT_ID", this)
        adView.setListener(this)

        val isTablet = AppLovinSdkUtils.isTablet(this)
        val heightPx = AppLovinSdkUtils.dpToPx(this, if (isTablet) 90 else 50)

        adView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
        adView.setBackgroundColor(Color.BLACK)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(adView)

        adView.loadAd()
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) {}

    override fun onAdLoadFailed(adUnitId: String?, errorCode: Int) {}

    override fun onAdHidden(ad: MaxAd?) {}

    override fun onAdDisplayFailed(ad: MaxAd?, errorCode: Int) {}

    override fun onAdDisplayed(ad: MaxAd?) {}

    override fun onAdClicked(ad: MaxAd?) {}

    override fun onAdExpanded(ad: MaxAd?) {}

    override fun onAdCollapsed(ad: MaxAd?) {}

    //endregion
}
