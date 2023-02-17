package com.applovin.enterprise.apps.demoapp.ads.max;

import android.os.Bundle;
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

/**
 * An {@link android.app.Activity} used to show AppLovin MAX App Open ads.
 * <p>
 * Created by avileung on 2023-02-10.
 */
public class AppOpenAdActivity
        extends BaseAdActivity
        implements MaxAdListener, MaxAdRevenueListener
{
    private MaxAppOpenAd appOpenAd;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_app_open_ad );
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

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd ad)
    {
        // App Open ad is ready to be shown. appOpenAd.isReady() will now return 'true'.
        logCallback();
    }

    @Override
    public void onAdLoadFailed(final String adUnitId, final MaxError error)
    {
        logCallback();
    }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final MaxError error)
    {
        logCallback();

        // App Open ad failed to display. We recommend loading the next ad.
        appOpenAd.loadAd();
    }

    @Override
    public void onAdDisplayed(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdClicked(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdHidden(final MaxAd ad)
    {
        logCallback();

        // App Open ad is hidden. Pre-load the next ad
        appOpenAd.loadAd();
    }

    //endregion

    //region MAX Ad Revenue Listener

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

    //endregion
}
