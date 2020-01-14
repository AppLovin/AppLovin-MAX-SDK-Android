package com.applovin.enterprise.apps.demoapp.ads

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils

/**
 * A [android.app.Activity] to show AppLovin MAX medium rectangle ads.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */

class ProgrammaticMRecAdActivity : BaseAdActivity(),
    MaxAdViewAdListener
{
    private lateinit var adView: MaxAdView

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programmatic_mrec_ad)
        setTitle(R.string.activity_programmatic_mrecs)

        setupCallbacksRecyclerView()

        adView = MaxAdView("YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this)
        adView.setListener(this)

        val widthPx = AppLovinSdkUtils.dpToPx(this, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(this, 250)

        adView.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
        adView.setBackgroundColor(Color.BLACK)

        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(adView)

        adView.loadAd()
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) { logCallback() }

    override fun onAdLoadFailed(adUnitId: String?, errorCode: Int) { logCallback() }

    override fun onAdHidden(ad: MaxAd?) { logCallback() }

    override fun onAdDisplayFailed(ad: MaxAd?, errorCode: Int) { logCallback() }

    override fun onAdDisplayed(ad: MaxAd?) { logCallback() }

    override fun onAdClicked(ad: MaxAd?) { logCallback() }

    override fun onAdExpanded(ad: MaxAd?) { logCallback() }

    override fun onAdCollapsed(ad: MaxAd?) { logCallback() }

    //endregion
}
