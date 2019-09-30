package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import kotlinx.android.synthetic.main.activity_layout_editor_banner_ad.*

/**
 * [android.app.Activity] used to show AppLovin MAX banner ads created in the Layout Editor.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
class LayoutEditorBannerAdActivity : AppCompatActivity(),
    MaxAdViewAdListener
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor_banner_ad)
        setTitle(R.string.activity_layout_editor_banners)

        bannerAdView.setListener(this)

        bannerAdView.loadAd()
    }

    //region MAX Ad Listener

    override fun onAdExpanded(ad: MaxAd?) {}

    override fun onAdCollapsed(ad: MaxAd?) {}

    override fun onAdLoaded(ad: MaxAd?) {}

    override fun onAdLoadFailed(adUnitId: String?, errorCode: Int) {}

    override fun onAdDisplayed(ad: MaxAd?) {}

    override fun onAdDisplayFailed(ad: MaxAd?, errorCode: Int) {}

    override fun onAdClicked(ad: MaxAd?) {}

    override fun onAdHidden(ad: MaxAd?) {}

    //endregion
}
