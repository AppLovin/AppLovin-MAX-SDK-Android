package com.applovin.enterprise.apps.demoapp.ads;

import android.os.Bundle;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;

import androidx.annotation.Nullable;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX MREC ads created in the Layout Editor.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */
public class LayoutEditorMrecAdActivity
        extends BaseAdActivity
        implements MaxAdViewAdListener
{
    // NOTE: MRECs will be supported in Android SDK 9.12.0 & iOS SDK 6.12.0
    private MaxAdView adView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_layout_editor_mrec_ad );
        setTitle( R.string.activity_layout_editor_mrecs );

        setupCallbacksRecyclerView();

        adView = findViewById( R.id.mrec_ad_view );
        adView.setListener( this );

        // Load the first ad.
        adView.loadAd();
    }

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdLoadFailed(final String adUnitId, final int errorCode) { logCallback(); }

    @Override
    public void onAdHidden(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final int errorCode) { logCallback(); }

    @Override
    public void onAdDisplayed(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdClicked(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdExpanded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdCollapsed(final MaxAd ad) { logCallback(); }

    //endregion
}
