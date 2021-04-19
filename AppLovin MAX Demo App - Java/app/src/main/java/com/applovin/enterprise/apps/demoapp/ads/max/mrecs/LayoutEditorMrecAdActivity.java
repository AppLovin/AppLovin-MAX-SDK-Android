package com.applovin.enterprise.apps.demoapp.ads.max.mrecs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX MREC ads created in the Layout Editor.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */
public class LayoutEditorMrecAdActivity
        extends BaseAdActivity
        implements MaxAdViewAdListener
{
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
    public void onAdLoaded(@NonNull final MaxAd ad) { logCallback(); }

    @Override
    public void onAdLoadFailed(@NonNull final String adUnitId, final int errorCode) { logCallback(); }

    @Override
    public void onAdHidden(@NonNull final MaxAd ad) { logCallback(); }

    @Override
    public void onAdDisplayFailed(@NonNull final MaxAd ad, final int errorCode) { logCallback(); }

    @Override
    public void onAdDisplayed(@NonNull final MaxAd ad) { logCallback(); }

    @Override
    public void onAdClicked(@NonNull final MaxAd ad) { logCallback(); }

    @Override
    public void onAdExpanded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdCollapsed(final MaxAd ad) { logCallback(); }

    //endregion
}
