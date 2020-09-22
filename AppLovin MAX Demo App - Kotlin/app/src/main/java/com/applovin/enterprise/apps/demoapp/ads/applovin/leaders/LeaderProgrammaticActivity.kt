package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders

import android.os.Bundle
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.sdk.*
import kotlinx.android.synthetic.main.activity_leader_programmatic.*

class LeaderProgrammaticActivity : BaseAdActivity(),
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_programmatic)

        setupCallbacksRecyclerView()

        val adView = AppLovinAdView(AppLovinAdSize.LEADER, this)
        adView.setAdLoadListener(this)
        adView.setAdDisplayListener(this)
        adView.setAdViewEventListener(this)
        adView.setAdClickListener(this)

        adView.id = ViewCompat.generateViewId()
        load_button.setOnClickListener { adView.loadNextAd() }

        // Add programmatically created banner into our container
        leader_programmatic_layout.addView(adView, ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx(this, 90)))

        val constraintSet = ConstraintSet()
        constraintSet.clone(leader_programmatic_layout)
        constraintSet.connect(adView.id, ConstraintSet.BOTTOM, R.id.leader_programmatic_layout, ConstraintSet.BOTTOM, 0)
        constraintSet.applyTo(leader_programmatic_layout)

        // Load an ad!
        adView.loadNextAd()
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