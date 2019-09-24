package com.applovin.enterprise.apps.demoapp.ads;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.ads.MaxInterstitialAd;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX interstitial ads.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class InterstitialAdActivity
        extends AppCompatActivity
        implements MaxAdListener
{
    private MaxInterstitialAd interstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_interstitial_ad );

        interstitialAd = new MaxInterstitialAd( "YOUR_AD_UNIT_ID", this );
        interstitialAd.setListener( this );

        // Load the first ad.
        interstitialAd.loadAd();
    }

    public void onShowAdClicked(View view)
    {
        if ( interstitialAd.isReady() )
        {
            interstitialAd.showAd();
        }
    }

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd ad)
    {
        // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'.
    }

    @Override
    public void onAdLoadFailed(final String adUnitId, final int errorCode)
    {
        // Interstitial ad failed to load. We recommend retrying in 3 seconds.
        final Handler handler = new Handler();
        handler.postDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                interstitialAd.loadAd();
            }
        }, 3000 );
    }

    @Override
    public void onAdDisplayed(final MaxAd ad) { }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final int errorCode)
    {
        // Interstitial ad failed to display. We recommend loading the next ad.
        interstitialAd.loadAd();
    }

    @Override
    public void onAdClicked(final MaxAd ad) { }

    @Override
    public void onAdHidden(final MaxAd ad)
    {
        // Interstitial Ad is hidden. Pre-load the next ad
        interstitialAd.loadAd();
    }

    //endregion
}
