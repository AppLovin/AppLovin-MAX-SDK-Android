package com.applovin.enterprise.apps.demoapp.ads;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

import androidx.annotation.Nullable;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX medium rectangle ads.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */
public class ProgrammaticMRecAdActivity
        extends BaseAdActivity
        implements MaxAdViewAdListener
{
    private MaxAdView adView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_programmatic_mrec_ad );
        setTitle( R.string.activity_programmatic_mrecs );

        setupCallbacksRecyclerView();

        adView = new MaxAdView( "YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this );
        adView.setListener( this );

        final int widthPx = AppLovinSdkUtils.dpToPx( this, 300 );
        final int heightPx = AppLovinSdkUtils.dpToPx( this, 250 );

        adView.setLayoutParams( new FrameLayout.LayoutParams( widthPx, heightPx ) );

        final ViewGroup rootView = findViewById( android.R.id.content );
        rootView.addView( adView );

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
