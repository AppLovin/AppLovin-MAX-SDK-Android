package com.applovin.enterprise.apps.demoapp.ads.applovin.banners

import android.os.Bundle
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import kotlinx.android.synthetic.main.activity_banner_layout_editor.*

class BannerLayoutEditorActivity : BaseAdActivity(),
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banner_layout_editor)

        setupCallbacksRecyclerView()

        ad_view.setAdLoadListener(this)
        ad_view.setAdDisplayListener(this)
        ad_view.setAdViewEventListener(this)
        ad_view.setAdClickListener(this)

        load_button.setOnClickListener { ad_view.loadNextAd() }

        // Load an ad!
        ad_view.loadNextAd()

        //
        // Please note that the AppLovinAdView CAN AUTOMATICALLY invoke loadNextAd() upon inflation from layout
        // To do so, add the following attributes to the com.applovin.adview.AppLovinAdView element:
        //
        // xmlns:demo="http://schemas.applovin.com/android/1.0"
        // demo:loadAdOnCreate="true"
        //
    }

    //region Ad Load Listener

    override fun adReceived(ad: AppLovinAd?)
    {
        logCallback()
    }

    override fun failedToReceiveAd(errorCode: Int)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback()
    }

    //endregion

    //region Ad Display Listener

    override fun adDisplayed(ad: AppLovinAd?)
    {
        logCallback()
    }

    override fun adHidden(ad: AppLovinAd?)
    {
        logCallback()
    }

    //endregion

    //region AdView Event Listener

    override fun adOpenedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?)
    {
        logCallback()
    }

    override fun adClosedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?)
    {
        logCallback()
    }

    override fun adLeftApplication(ad: AppLovinAd?, adView: AppLovinAdView?)
    {
        logCallback()
    }

    override fun adFailedToDisplay(ad: AppLovinAd?, adView: AppLovinAdView?, code: AppLovinAdViewDisplayErrorCode?)
    {
        logCallback()
    }

    //endregion

    //region Ad Click Listener

    override fun adClicked(ad: AppLovinAd?)
    {
        logCallback()
    }

    //endregion
}
