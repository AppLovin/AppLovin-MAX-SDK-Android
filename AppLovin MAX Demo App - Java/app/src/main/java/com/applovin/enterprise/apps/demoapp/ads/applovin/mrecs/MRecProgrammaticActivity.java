package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ads.applovin.AdStatusActivity;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdkUtils;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

/**
 * Created by monica ong on 7/20/17.
 */
public final class MRecProgrammaticActivity
        extends AdStatusActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_mrec_programmatic );

        adStatusTextView = findViewById( R.id.status_label );

        // Create MRec
        AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.MREC, this );
        adView.setId( ViewCompat.generateViewId() );

        ConstraintLayout mrecConstraintLayout = findViewById( R.id.mrec_programmatic_constraint_layout );
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams( AppLovinSdkUtils.dpToPx( this, AppLovinAdSize.MREC.getWidth() ),
                                                                                        AppLovinSdkUtils.dpToPx( this, AppLovinAdSize.MREC.getHeight() ) );
        mrecConstraintLayout.addView( adView, layoutParams );

        ConstraintSet set = new ConstraintSet();
        set.clone( mrecConstraintLayout );
        set.connect( adView.getId(), ConstraintSet.TOP, mrecConstraintLayout.getId(), ConstraintSet.TOP, AppLovinSdkUtils.dpToPx( this, 80 ) );
        set.centerHorizontally( adView.getId(), mrecConstraintLayout.getId() );
        set.applyTo( mrecConstraintLayout );

        // Set up load button
        Button loadButton = findViewById( R.id.load_button );
        loadButton.setOnClickListener( v -> {
            log( "Loading ad..." );
            adView.loadNextAd();
        } );

        //
        // Optional: Set listeners
        //
        adView.setAdLoadListener( new AppLovinAdLoadListener()
        {
            @Override
            public void adReceived(final AppLovinAd ad)
            {
                log( "MRec loaded" );
            }

            @Override
            public void failedToReceiveAd(final int errorCode)
            {
                // Look at AppLovinErrorCodes.java for list of error codes
                log( "MRec failed to load with error code " + errorCode );
            }
        } );

        adView.setAdDisplayListener( new AppLovinAdDisplayListener()
        {
            @Override
            public void adDisplayed(final AppLovinAd ad)
            {
                log( "MRec Displayed" );
            }

            @Override
            public void adHidden(final AppLovinAd ad)
            {
                log( "MRec Hidden" );
            }
        } );

        adView.setAdClickListener( ad -> log( "MRec Clicked" ) );

        adView.setAdViewEventListener( new AppLovinAdViewEventListener()
        {
            @Override
            public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "MRec opened fullscreen" );
            }

            @Override
            public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "MRec closed fullscreen" );
            }

            @Override
            public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "MRec left application" );
            }

            @Override
            public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView adView, final AppLovinAdViewDisplayErrorCode code)
            {
                log( "MRec failed to display with error code " + code );
            }
        } );
    }
}
