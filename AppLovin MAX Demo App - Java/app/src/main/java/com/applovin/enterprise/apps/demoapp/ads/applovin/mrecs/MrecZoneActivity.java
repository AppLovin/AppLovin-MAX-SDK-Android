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

public final class MrecZoneActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_mrec_programmatic );

        setupCallbacksRecyclerView();

        AppLovinAdView mrecAdView = new AppLovinAdView( AppLovinAdSize.MREC, "YOUR_ZONE_ID", this );
        mrecAdView.setAdLoadListener( this );
        mrecAdView.setAdDisplayListener( this );
        mrecAdView.setAdViewEventListener( this );
        mrecAdView.setAdClickListener( this );

        mrecAdView.setId( ViewCompat.generateViewId() );
        final int widthPx = AppLovinSdkUtils.dpToPx( this, 300 );
        final int heightPx = AppLovinSdkUtils.dpToPx( this, 250 );

        Button loadButton = findViewById( R.id.MREC_load_button );
        loadButton.setOnClickListener( v -> {
            mrecAdView.loadNextAd();
        } );


        // Add programmatically created MREC into our container and center it.
        ConstraintLayout MRECProgrammaticContentLayout = findViewById( R.id.mrec_programmatic_layout );
        MRECProgrammaticContentLayout.addView( mrecAdView, new ConstraintLayout.LayoutParams( widthPx, heightPx ) );

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( MRECProgrammaticContentLayout );
        constraintSet.constrainHeight( mrecAdView.getId(), heightPx );
        constraintSet.constrainWidth( mrecAdView.getId(), widthPx );

        constraintSet.connect( mrecAdView.getId(), ConstraintSet.LEFT, MRECProgrammaticContentLayout.getId(), ConstraintSet.LEFT );
        constraintSet.connect( mrecAdView.getId(), ConstraintSet.RIGHT, MRECProgrammaticContentLayout.getId(), ConstraintSet.RIGHT );
        constraintSet.connect( mrecAdView.getId(), ConstraintSet.TOP, MRECProgrammaticContentLayout.getId(), ConstraintSet.TOP );
        constraintSet.applyTo( MRECProgrammaticContentLayout );

        // Load an ad!
        mrecAdView.loadNextAd();
    }

    //region Ad Load Listener

    @Override
    public void adReceived(final AppLovinAd ad)
    {
        logCallback();
    }

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
    public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView mrecAdView) { logCallback(); }

    @Override
    public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView mrecAdView) { logCallback(); }

    @Override
    public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView mrecAdView) { logCallback(); }

    @Override
    public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView mrecAdView, final AppLovinAdViewDisplayErrorCode code) { logCallback(); }

    //endregion

    //region Ad Click Listener

    @Override
    public void adClicked(final AppLovinAd ad) { logCallback(); }

    //endregion
}

