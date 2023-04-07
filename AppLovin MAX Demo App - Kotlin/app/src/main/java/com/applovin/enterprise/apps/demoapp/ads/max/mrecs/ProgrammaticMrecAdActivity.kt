package com.applovin.enterprise.apps.demoapp.ads.max.mrecs

import android.graphics.Color
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.*
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils

/**
 * An [android.app.Activity] used to show AppLovin MAX MREC ads.
 *
 *
 * Created by Andrew Tian on 2020-01-14.
 */
class ProgrammaticMrecAdActivity : BaseAdActivity(), MaxAdViewAdListener, MaxAdRevenueListener {
    private lateinit var adView: MaxAdView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programmatic_mrec_ad)
        setTitle(R.string.activity_programmatic_mrecs)

        setupCallbacksRecyclerView()

        adView = MaxAdView("YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this)

        adView.setListener(this)
        adView.setRevenueListener(this)

        adView.id = ViewCompat.generateViewId()
        val widthPx = AppLovinSdkUtils.dpToPx(this, 300)
        val heightPx = AppLovinSdkUtils.dpToPx(this, 250)
        adView.layoutParams = ConstraintLayout.LayoutParams(widthPx, heightPx)

        // Need to set the background or background color for MRECs to be fully functional.
        adView.setBackgroundColor(Color.BLACK)

        // Set up constraints
        val constraintLayout = findViewById<ConstraintLayout>(R.id.main_constraint_layout)
        constraintLayout.id = ViewCompat.generateViewId()
        constraintLayout.addView(adView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.constrainHeight(adView.id, heightPx)
        constraintSet.constrainWidth(adView.id, widthPx)

        constraintSet.connect(
                adView.id,
                ConstraintSet.LEFT,
                constraintLayout.id,
                ConstraintSet.LEFT
        )
        constraintSet.connect(
                adView.id,
                ConstraintSet.RIGHT,
                constraintLayout.id,
                ConstraintSet.RIGHT
        )
        constraintSet.connect(
                adView.id,
                ConstraintSet.TOP,
                constraintLayout.id,
                ConstraintSet.TOP
        )
        constraintSet.applyTo(constraintLayout)

        // Load the first ad.
        adView.loadAd()
    }

    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd) {
        logCallback()
    }

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
        logCallback()
    }

    override fun onAdHidden(ad: MaxAd) {
        logCallback()
    }

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
        logCallback()
    }

    override fun onAdDisplayed(ad: MaxAd) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd) {
        logCallback()
    }

    override fun onAdExpanded(ad: MaxAd) {
        logCallback()
    }

    override fun onAdCollapsed(ad: MaxAd) {
        logCallback()
    }

    //endregion

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd?) {
        logCallback()

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad?.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad?.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad?.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad?.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}