package com.applovin.enterprise.apps.demoapp.ads.applovin.banner;

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

/**
 * Created by thomasso on 3/6/17.
 */

public final class BannerZoneActivity
        extends AdStatusActivity
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_banner_programmatic );

        adStatusTextView = findViewById( R.id.status_label );

        final AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.BANNER, "YOUR_ZONE_ID", this );

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
                log( "Banner loaded" );
            }

            @Override
            public void failedToReceiveAd(final int errorCode)
            {
                // Look at AppLovinErrorCodes.java for list of error codes
                log( "Banner failed to load with error code " + errorCode );
            }
        } );

        adView.setAdDisplayListener( new AppLovinAdDisplayListener()
        {
            @Override
            public void adDisplayed(final AppLovinAd ad)
            {
                log( "Banner Displayed" );
            }

            @Override
            public void adHidden(final AppLovinAd ad)
            {
                log( "Banner Hidden" );
            }
        } );

        adView.setAdClickListener( new AppLovinAdClickListener()
        {
            @Override
            public void adClicked(final AppLovinAd ad)
            {
                log( "Banner Clicked" );
            }
        } );

        adView.setAdViewEventListener( new AppLovinAdViewEventListener()
        {
            @Override
            public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "Banner opened fullscreen" );
            }

            @Override
            public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "Banner closed fullscreen" );
            }

            @Override
            public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView adView)
            {
                log( "Banner left application" );
            }

            @Override
            public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView adView, final AppLovinAdViewDisplayErrorCode code)
            {
                log( "Banner failed to display with error code " + code );
            }
        } );

        // Add programmatically created banner into our container
        final ConstraintLayout bannerProgrammaticContentLayout = findViewById( R.id.banner_programmatic_layout );

        bannerProgrammaticContentLayout.addView( adView, new ConstraintLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx( this, 50 ) ) );

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( bannerProgrammaticContentLayout );
        constraintSet.connect( adView.getId(), ConstraintSet.BOTTOM, R.id.banner_programmatic_layout, ConstraintSet.BOTTOM, 0 );
        constraintSet.applyTo( bannerProgrammaticContentLayout );

        // Load an ad!
        adView.loadNextAd();
    }
}
