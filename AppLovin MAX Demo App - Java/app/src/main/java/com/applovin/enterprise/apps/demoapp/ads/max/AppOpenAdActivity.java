package com.applovin.enterprise.apps.demoapp.ads.max;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAppOpenAd;

import java.util.concurrent.TimeUnit;

public class AppOpenAdActivity
        extends BaseAdActivity
        implements MaxAdListener, MaxAdRevenueListener
{
    private MaxAppOpenAd appOpenAd;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_app_open_ad_ );
        setTitle( R.string.activity_app_open );

        setupCallbacksRecyclerView();

        appOpenAd = new MaxAppOpenAd( "YOUR_AD_UNIT_ID", this );

        appOpenAd.setListener( this );
        appOpenAd.setRevenueListener( this );

        // Load the first ad.
        appOpenAd.loadAd();
    }

    public void onShowAdClicked(View view)
    {
        if ( appOpenAd.isReady() )
        {
            appOpenAd.showAd();
        }
    }

    @Override
    public void onAdLoaded(final MaxAd ad)
    {
        // Interstitial ad is ready to be shown. interstitialAd.isReady() will now return 'true'.
        logCallback();

//        // Reset retry attempt
//        retryAttempt = 0;

    }

    @Override
    public void onAdDisplayed(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdHidden(final MaxAd ad)
    {
        logCallback();

        // Interstitial Ad is hidden. Pre-load the next ad
        appOpenAd.loadAd();
    }

    @Override
    public void onAdClicked(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdLoadFailed(final String adUnitId, final MaxError error)
    {
        logCallback();

//        // Interstitial ad failed to load. We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds).
//
//        //retryAttempt++;
//        //long delayMillis = TimeUnit.SECONDS.toMillis( (long) Math.pow( 2, Math.min( 6, retryAttempt ) ) );
//
//        new Handler().postDelayed( new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                interstitialAd.loadAd();
//            }
//        }, delayMillis );
    }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final MaxError error)
    {
        logCallback();

        // Interstitial ad failed to display. We recommend loading the next ad.
        appOpenAd.loadAd();
    }

    @Override
    public void onAdRevenuePaid(final MaxAd ad)
    {
        logCallback();

        AdjustAdRevenue adjustAdRevenue = new AdjustAdRevenue( AdjustConfig.AD_REVENUE_APPLOVIN_MAX );
        adjustAdRevenue.setRevenue( ad.getRevenue(), "USD" );
        adjustAdRevenue.setAdRevenueNetwork( ad.getNetworkName() );
        adjustAdRevenue.setAdRevenueUnit( ad.getAdUnitId() );
        adjustAdRevenue.setAdRevenuePlacement( ad.getPlacement() );

        Adjust.trackAdRevenue( adjustAdRevenue );
    }
}
