package com.applovin.enterprise.apps.demoapp.ads

import android.os.Bundle
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdViewAdListener
import kotlinx.android.synthetic.main.activity_layout_editor_mrec_ad.*

/**
 * [android.app.Activity] used to show AppLovin MAX MREC ads created in the Layout Editor.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */
class LayoutEditorMrecAdActivity : BaseAdActivity(),
    MaxAdViewAdListener
{
    // NOTE: MRECs will be supported in Android SDK 9.12.0 & iOS SDK 6.12.0

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor_mrec_ad)
        setTitle(R.string.activity_layout_editor_mrecs)

        setupCallbacksRecyclerView()

        mrecAdView.setListener(this)

        // Load the first ad.
        mrecAdView.loadAd()
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
