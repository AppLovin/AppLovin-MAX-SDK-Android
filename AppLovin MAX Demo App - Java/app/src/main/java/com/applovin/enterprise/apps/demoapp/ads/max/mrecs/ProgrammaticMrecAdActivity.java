package com.applovin.enterprise.apps.demoapp.ads.max.mrecs;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX MREC ads.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */
public class ProgrammaticMrecAdActivity
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
        adView.setId( ViewCompat.generateViewId() );

        final int widthPx = AppLovinSdkUtils.dpToPx( this, 300 );
        final int heightPx = AppLovinSdkUtils.dpToPx( this, 250 );

        adView.setLayoutParams( new ConstraintLayout.LayoutParams( widthPx, heightPx ) );

        // Need to set the background or background color for MRECs to be fully functional.
        adView.setBackgroundColor( Color.BLACK );

        // Set up constraints
        final ConstraintLayout constraintLayout = findViewById( R.id.main_constraint_layout );
        constraintLayout.setId( ViewCompat.generateViewId() );
        constraintLayout.addView( adView );

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( constraintLayout );
        constraintSet.constrainHeight( adView.getId(), heightPx );
        constraintSet.constrainWidth( adView.getId(), widthPx );

        constraintSet.connect( adView.getId(), ConstraintSet.LEFT, constraintLayout.getId(), ConstraintSet.LEFT );
        constraintSet.connect( adView.getId(), ConstraintSet.RIGHT, constraintLayout.getId(), ConstraintSet.RIGHT );
        constraintSet.connect( adView.getId(), ConstraintSet.TOP, constraintLayout.getId(), ConstraintSet.TOP );
        constraintSet.applyTo( constraintLayout );

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
