package com.applovin.enterprise.apps.demoapp.ads.max.banner;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX banner ads.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class ProgrammaticBannerAdActivity
        extends BaseAdActivity
        implements MaxAdViewAdListener, MaxAdRevenueListener
{
    private MaxAdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_programmatic_banner_ad );
        setTitle( R.string.activity_programmatic_banners );

        setupCallbacksRecyclerView();

        adView = new MaxAdView( "YOUR_AD_UNIT_ID", this );

        adView.setListener( this );
        adView.setRevenueListener( this );

        // Set the height of the banner ad based on the device type.
        final boolean isTablet = AppLovinSdkUtils.isTablet( this );
        final int heightPx = AppLovinSdkUtils.dpToPx( this, isTablet ? 90 : 50 );
        // Banner width must match the screen to be fully functional.
        adView.setLayoutParams( new FrameLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, heightPx ) );

        // Need to set the background or background color for banners to be fully functional.
        adView.setBackgroundColor( Color.BLACK );

        final ViewGroup rootView = (ViewGroup) findViewById( android.R.id.content );
        rootView.addView( adView );

        // Load the first ad.
        adView.loadAd();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        adView.destroy();
    }

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdLoadFailed(final String adUnitId, final MaxError maxError) { logCallback(); }

    @Override
    public void onAdHidden(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final MaxError maxError) { logCallback(); }

    @Override
    public void onAdDisplayed(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdClicked(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdExpanded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdCollapsed(final MaxAd ad) { logCallback(); }

    //endregion

    //region MAX Ad Revenue Listener

    @Override
    public void onAdRevenuePaid(final MaxAd maxAd)
    {
        logCallback();

        AdjustAdRevenue adjustAdRevenue = new AdjustAdRevenue( AdjustConfig.AD_REVENUE_APPLOVIN_MAX );
        adjustAdRevenue.setRevenue( maxAd.getRevenue(), "USD" );
        adjustAdRevenue.setAdRevenueNetwork( maxAd.getNetworkName() );
        adjustAdRevenue.setAdRevenueUnit( maxAd.getAdUnitId() );
        adjustAdRevenue.setAdRevenuePlacement( maxAd.getPlacement() );

        Adjust.trackAdRevenue( adjustAdRevenue );
    }

    //endregion
}
