package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.enterprise.apps.demoapp.ads.applovin.AdStatusActivity;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;

/**
 * Created by thomasso on 10/5/15.
 */
public class InterstitialBasicIntegrationActivity
        extends AdStatusActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    private AppLovinInterstitialAdDialog interstitialAd;
    private Button                       showButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_interstitial_basic_integration );

        adStatusTextView = findViewById( R.id.status_label );

        interstitialAd = AppLovinInterstitialAd.create( AppLovinSdk.getInstance( this ), this );

        showButton = findViewById( R.id.showButton );
        showButton.setOnClickListener( v -> {
            showButton.setEnabled( false );

            log( "Showing..." );

            //
            // Optional: Set ad load, ad display, ad click, and ad video playback callback listeners
            //
            interstitialAd.setAdLoadListener( InterstitialBasicIntegrationActivity.this );
            interstitialAd.setAdDisplayListener( InterstitialBasicIntegrationActivity.this );
            interstitialAd.setAdClickListener( InterstitialBasicIntegrationActivity.this );
            interstitialAd.setAdVideoPlaybackListener( InterstitialBasicIntegrationActivity.this ); // This will only ever be used if you have video ads enabled.

            interstitialAd.show();
        } );
    }

    //
    // Ad Load Listener
    //
    @Override
    public void adReceived(AppLovinAd appLovinAd)
    {
        log( "Interstitial loaded" );
        showButton.setEnabled( true );
    }

    @Override
    public void failedToReceiveAd(int errorCode)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        log( "Interstitial failed to load with error code " + errorCode );

        showButton.setEnabled( true );
    }

    //
    // Ad Display Listener
    //
    @Override
    public void adDisplayed(AppLovinAd appLovinAd)
    {
        log( "Interstitial Displayed" );
    }

    @Override
    public void adHidden(AppLovinAd appLovinAd)
    {
        log( "Interstitial Hidden" );
    }

    //
    // Ad Click Listener
    //
    @Override
    public void adClicked(AppLovinAd appLovinAd)
    {
        log( "Interstitial Clicked" );
    }

    //
    // Ad Video Playback Listener
    //
    @Override
    public void videoPlaybackBegan(AppLovinAd appLovinAd)
    {
        log( "Video Started" );
    }

    @Override
    public void videoPlaybackEnded(AppLovinAd appLovinAd, double percentViewed, boolean wasFullyViewed)
    {
        log( "Video Ended" );
    }
}
