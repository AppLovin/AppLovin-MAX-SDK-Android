package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials;

import android.os.Bundle;
import android.widget.Button;

import com.applovin.adview.AppLovinInterstitialAd;
import com.applovin.adview.AppLovinInterstitialAdDialog;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinSdk;

/**
 * Created by suyashsaxena on 06/20/18.
 */
public class InterstitialZoneActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    private AppLovinAd                   currentAd;
    private AppLovinInterstitialAdDialog interstitialAd;
    private Button                       showButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_interstitial_manual_loading );

        setupCallbacksRecyclerView();

        interstitialAd = AppLovinInterstitialAd.create( AppLovinSdk.getInstance( this ), this );
        interstitialAd.setAdLoadListener( this );
        interstitialAd.setAdDisplayListener( this );
        interstitialAd.setAdClickListener( this );
        interstitialAd.setAdVideoPlaybackListener( this ); // This will only ever be used if you have video ads enabled.

        final Button loadButton = findViewById( R.id.loadButton );
        showButton = findViewById( R.id.showButton );

        loadButton.setOnClickListener( v -> {
            showButton.setEnabled( false );

            AppLovinSdk.getInstance( getApplicationContext() ).getAdService().loadNextAdForZoneId( "YOUR_ZONE_ID ", this );
        } );

        showButton.setOnClickListener( v -> {
            if ( currentAd != null )
            {
                interstitialAd.showAndRender( currentAd );
            }
        } );
    }

    //region Ad Load Listener

    @Override
    public void adReceived(AppLovinAd appLovinAd)
    {
        logCallback();
        currentAd = appLovinAd;
        showButton.setEnabled( true );
    }

    @Override
    public void failedToReceiveAd(int errorCode)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback();

        showButton.setEnabled( true );
    }

    //endregion

    //region Ad Display Listener

    @Override
    public void adDisplayed(AppLovinAd appLovinAd) { logCallback(); }

    @Override
    public void adHidden(AppLovinAd appLovinAd) { logCallback(); }

    //endregion

    //region Ad Click Listener

    @Override
    public void adClicked(AppLovinAd appLovinAd) { logCallback(); }

    //endregion

    //region Ad Video Playback Listener

    @Override
    public void videoPlaybackBegan(AppLovinAd appLovinAd) { logCallback(); }

    @Override
    public void videoPlaybackEnded(AppLovinAd appLovinAd, double percentViewed, boolean wasFullyViewed) { logCallback(); }

    //endregion
}
