package com.applovin.enterprise.apps.demoapp.ads.max.nativead;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;

import java.util.HashMap;
import java.util.Map;

public class TemplateNativeAdActivity
        extends BaseAdActivity
        implements MaxAdRevenueListener
{
    // Map of ad unit IDs to native ad loaders
    private final Map<String, MaxNativeAdLoader> nativeAdLoaders = new HashMap<>( 2 );
    private       MaxAd                          nativeAd;

    private FrameLayout nativeAdLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_native_template );
        setTitle( R.string.activity_template_natives );

        nativeAdLayout = findViewById( R.id.native_ad_layout );
        setupCallbacksRecyclerView();
    }

    @Override
    protected void onDestroy()
    {
        // Must destroy native ad or else there will be memory leaks.
        if ( !nativeAdLoaders.isEmpty() )
        {
            // Call destroy on the native ad from any native ad loader.
            MaxNativeAdLoader nativeAdLoader = nativeAdLoaders.entrySet().iterator().next().getValue();
            nativeAdLoader.destroy( nativeAd );
        }

        super.onDestroy();
    }

    public void onShowAdClicked(View view)
    {
        MaxNativeAdLoader nativeAdLoader;

        String adUnitId = "YOUR_AD_UNIT_ID";
        if ( nativeAdLoaders.containsKey( adUnitId ) )
        {
            nativeAdLoader = nativeAdLoaders.get( adUnitId );
        }
        else
        {
            nativeAdLoader = new MaxNativeAdLoader( adUnitId, this );
            nativeAdLoader.setPlacement( "Native Template Test Placement" );
            nativeAdLoader.setExtraParameter( "test_extra_key", "test_extra_value" );
            nativeAdLoader.setRevenueListener( this );
            nativeAdLoader.setNativeAdListener( new MaxNativeAdListener()
            {
                @Override
                public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad)
                {
                    // Cleanup any pre-existing native ad to prevent memory leaks.
                    if ( nativeAd != null )
                    {
                        nativeAdLoader.destroy( nativeAd );
                    }

                    // Save ad for cleanup.
                    nativeAd = ad;

                    // Add ad view to view.
                    nativeAdLayout.removeAllViews();
                    nativeAdLayout.addView( nativeAdView );

                    logCallback( 2 );
                }

                @Override
                public void onNativeAdLoadFailed(final String adUnitId, final MaxError error)
                {
                    logCallback( 2 );
                }

                @Override
                public void onNativeAdClicked(final MaxAd ad)
                {
                    logCallback( 2 );
                }
            } );

            // Save the loader so we don't need to recreate more than once.
            nativeAdLoaders.put( adUnitId, nativeAdLoader );
        }

        nativeAdLoader.loadAd();
    }

    //region MAX Ad Revenue Listener

    @Override public void onAdRevenuePaid(final MaxAd ad)
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
