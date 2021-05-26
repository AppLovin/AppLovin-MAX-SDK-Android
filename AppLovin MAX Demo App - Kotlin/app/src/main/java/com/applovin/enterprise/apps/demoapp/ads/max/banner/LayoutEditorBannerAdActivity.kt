package com.applovin.enterprise.apps.demoapp.ads.max.banner

import android.os.Bundle
import com.applovin.enterprise.apps.demoapp.R

import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import kotlinx.android.synthetic.main.activity_layout_editor_banner_ad.*

/**
 * [android.app.Activity] used to show AppLovin MAX banner ads created in the Layout Editor.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class LayoutEditorBannerAdActivity : BaseAdActivity(),
    MaxAdViewAdListener
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor_banner_ad)
        setTitle(R.string.activity_layout_editor_banners)

        setupCallbacksRecyclerView()

        bannerAdView.setListener(this)
        bannerAdView.loadAd()
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) { logCallback() }

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) { logCallback() }

    override fun onAdHidden(ad: MaxAd?) { logCallback() }

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) { logCallback() }

    override fun onAdDisplayed(ad: MaxAd?) { logCallback() }

    override fun onAdClicked(ad: MaxAd?) { logCallback() }

    override fun onAdExpanded(ad: MaxAd?) { logCallback() }

    override fun onAdCollapsed(ad: MaxAd?) { logCallback() }

    override fun onAdRevenuePaid(ad: MaxAd?) { logCallback() }

    //endregion
}
