package com.applovin.enterprise.apps.demoapp.ads.max.mrecs

import android.os.Bundle
import com.applovin.enterprise.apps.demoapp.R

import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import kotlinx.android.synthetic.main.activity_layout_editor_mrec_ad.*

/**
 * An [android.app.Activity] used to show AppLovin MAX MREC ads created in the Layout Editor.
 *
 *
 * Created by Andrew Tian on 2020-01-14.
 */
class LayoutEditorMrecAdActivity : BaseAdActivity(), MaxAdViewAdListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor_mrec_ad)
        setTitle(R.string.activity_layout_editor_mrecs)
        setupCallbacksRecyclerView()
        mrec_ad_view.setListener(this)

        // Load the first ad.
        mrec_ad_view.loadAd()
    }

    //region MAX Ad Listener
    override fun onAdLoaded(ad: MaxAd) {
        logCallback()
    }

    override fun onAdLoadFailed(adUnitId: String, errorCode: Int) {
        logCallback()
    }

    override fun onAdDisplayFailed(ad: MaxAd, errorCode: Int) {
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
    } //endregion
}