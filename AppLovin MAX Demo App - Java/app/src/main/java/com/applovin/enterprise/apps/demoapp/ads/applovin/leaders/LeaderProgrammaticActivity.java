package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.enterprise.apps.demoapp.ads.applovin.AdStatusActivity;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdkUtils;

public class LeaderProgrammaticActivity
        extends AdStatusActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_leader_programmatic );

        adStatusTextView = findViewById( R.id.status_label );

        final AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.LEADER, this );

        adView.setId( ViewCompat.generateViewId() );

        final Button loadButton = findViewById( R.id.load_button );
        loadButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                log( "Loading ad..." );
                adView.loadNextAd();
            }
        } );

        //
        // Optional: Set listeners
        //
        adView.setAdLoadListener( new AppLovinAdLoadListener()
        {
            @Override
            public void adReceived(final AppLovinAd ad)
            {
                log( "Leader loaded" );
            }

            @Override
            public void failedToReceiveAd(final int errorCode)
            {
                // Look at AppLovinErrorCodes.java for list of error codes
                log( "Leader failed to load with error code " + errorCode );
            }
        } );

        adView.setAdDisplayListener( new AppLovinAdDisplayListener()
        {
            @Override
            public void adDisplayed(final AppLovinAd ad)
            {
                log( "Leader Displayed" );
            }

            @Override
            public void adHidden(final AppLovinAd ad)
            {
                log( "Leader Hidden" );
            }
        } );

        adView.setAdClickListener( new AppLovinAdClickListener()
        {
            @Override
            public void adClicked(final AppLovinAd ad)
            {
                log( "Leader Clicked" );
            }
        } );

        adView.setAdViewEventListener( new AppLovinAdViewEventListener()
        {
            @Override
            public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "Leader opened fullscreen" );
            }

            @Override
            public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "Leader closed fullscreen" );
            }

            @Override
            public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "Leader left application" );
            }

            @Override
            public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView adView, final AppLovinAdViewDisplayErrorCode code)
            {
                log( "Leader failed to display with error code " + code );
            }
        } );

        // Add programmatically created leader into our container
        final ConstraintLayout leaderLayout = findViewById( R.id.leader_programmatic_layout );

        leaderLayout.addView( adView, new ConstraintLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx( this, 90 ) ) );

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( leaderLayout );
        constraintSet.connect( adView.getId(), ConstraintSet.BOTTOM, R.id.leader_programmatic_layout, ConstraintSet.BOTTOM, 0 );
        constraintSet.applyTo( leaderLayout );

        // Load an ad!
        adView.loadNextAd();
    }
}
