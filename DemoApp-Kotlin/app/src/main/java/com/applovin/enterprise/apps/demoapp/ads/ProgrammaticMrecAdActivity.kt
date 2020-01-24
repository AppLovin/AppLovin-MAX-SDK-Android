package com.applovin.enterprise.apps.demoapp.ads

import android.graphics.Color
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils
import kotlinx.android.synthetic.main.activity_programmatic_mrec_ad.*

/**
 * A [android.app.Activity] to show AppLovin MAX MREC ads.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */

class ProgrammaticMrecAdActivity : BaseAdActivity(),
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
        adView.id = ViewCompat.generateViewId()

        val widthPx = AppLovinSdkUtils.dpToPx(this, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(this, 250)

        adView.layoutParams = ConstraintLayout.LayoutParams(widthPx, heightPx)

        // Need to set the background or background color for MRECs to be fully functional.
        adView.setBackgroundColor(Color.BLACK)

        // Set up constraints
        mainConstraintLayout.id = ViewCompat.generateViewId()
        mainConstraintLayout.addView(adView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainConstraintLayout)
        constraintSet.constrainHeight(adView.id, heightPx)
        constraintSet.constrainWidth(adView.id, widthPx)

        constraintSet.connect(
            adView.id,
            ConstraintSet.LEFT,
            mainConstraintLayout.id,
            ConstraintSet.LEFT
        )
        constraintSet.connect(
            adView.id,
            ConstraintSet.RIGHT,
            mainConstraintLayout.id,
            ConstraintSet.RIGHT
        )
        constraintSet.connect(
            adView.id,
            ConstraintSet.TOP,
            mainConstraintLayout.id,
            ConstraintSet.TOP
        )
        constraintSet.applyTo(mainConstraintLayout)

        // Load the first ad.
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
