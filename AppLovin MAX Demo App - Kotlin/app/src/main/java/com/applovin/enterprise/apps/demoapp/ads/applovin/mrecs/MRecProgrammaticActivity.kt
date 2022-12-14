package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs

import android.os.Bundle
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.sdk.*

class MRecProgrammaticActivity : BaseAdActivity(),
    AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mrec_programmatic)

        setupCallbacksRecyclerView()

        val adView = AppLovinAdView(AppLovinAdSize.MREC, this)
        adView.setAdLoadListener(this)
        adView.setAdDisplayListener(this)
        adView.setAdViewEventListener(this)
        adView.setAdClickListener(this)

        adView.id = ViewCompat.generateViewId()
        val widthPx = AppLovinSdkUtils.dpToPx(this, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(this, 250)
        adView.layoutParams = ConstraintLayout.LayoutParams(widthPx, heightPx)

        val mrecLoadButton = findViewById<Button>(R.id.mrec_load_button)
        mrecLoadButton.setOnClickListener { adView.loadNextAd() }

        // Add programmatically created MRec into our container and center.
        val contentLayout = findViewById<ConstraintLayout>(R.id.mrec_programmatic_content_layout)
        contentLayout.addView(adView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(contentLayout)
        constraintSet.constrainHeight(adView.id, heightPx)
        constraintSet.constrainWidth(adView.id, widthPx)
        constraintSet.connect(adView.id, ConstraintSet.LEFT, R.id.mrec_programmatic_content_layout, ConstraintSet.LEFT, 0)
        constraintSet.connect(adView.id, ConstraintSet.RIGHT, R.id.mrec_programmatic_content_layout, ConstraintSet.RIGHT, 0)
        constraintSet.connect(adView.id, ConstraintSet.TOP, R.id.mrec_programmatic_content_layout, ConstraintSet.TOP, 0)

        constraintSet.applyTo(contentLayout)

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
