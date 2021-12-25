package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs;

import android.os.Bundle;
import android.widget.Button;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdkUtils;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

public final class MRecProgrammaticActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_mrec_programmatic );

        setupCallbacksRecyclerView();

        AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.MREC, this );
        adView.setAdLoadListener( this );
        adView.setAdDisplayListener( this );
        adView.setAdViewEventListener( this );
        adView.setAdClickListener( this );

        adView.setId( ViewCompat.generateViewId() );
        final int widthPx = AppLovinSdkUtils.dpToPx( this, 300 );
        final int heightPx = AppLovinSdkUtils.dpToPx( this, 250 );

        Button loadButton = findViewById( R.id.ad_load_button );
        loadButton.setOnClickListener( v -> {
            adView.loadNextAd();
        } );

        // Add programmatically created MRec into our container and center it.
        ConstraintLayout mrecProgrammaticContentLayout = findViewById( R.id.mrec_programmatic_layout );
        mrecProgrammaticContentLayout.addView( adView, new ConstraintLayout.LayoutParams( widthPx, heightPx ) );

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( mrecProgrammaticContentLayout );
        constraintSet.constrainHeight( adView.getId(), heightPx );
        constraintSet.constrainWidth( adView.getId(), widthPx );

        constraintSet.connect( adView.getId(), ConstraintSet.LEFT, mrecProgrammaticContentLayout.getId(), ConstraintSet.LEFT );
        constraintSet.connect( adView.getId(), ConstraintSet.RIGHT, mrecProgrammaticContentLayout.getId(), ConstraintSet.RIGHT );
        constraintSet.connect( adView.getId(), ConstraintSet.TOP, mrecProgrammaticContentLayout.getId(), ConstraintSet.TOP );
        constraintSet.applyTo( mrecProgrammaticContentLayout );

        // Load an ad!
        adView.loadNextAd();
    }

    //region Ad Load Listener

    @Override
    public void adReceived(final AppLovinAd ad) { logCallback(); }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback();
    }

    //endregion

    //region Ad Display Listener

    @Override
    public void adDisplayed(final AppLovinAd ad) { logCallback(); }

    @Override
    public void adHidden(final AppLovinAd ad) { logCallback(); }

    //endregion

    //region AdView Event Listener

    @Override
    public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView adView) { logCallback(); }

    @Override
    public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView adView) { logCallback(); }

    @Override
    public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView adView) { logCallback(); }

    @Override
    public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView adView, final AppLovinAdViewDisplayErrorCode code) { logCallback(); }

    //endregion

    //region Ad Click Listener

    @Override
    public void adClicked(final AppLovinAd ad) { logCallback(); }

    //endregion
}

