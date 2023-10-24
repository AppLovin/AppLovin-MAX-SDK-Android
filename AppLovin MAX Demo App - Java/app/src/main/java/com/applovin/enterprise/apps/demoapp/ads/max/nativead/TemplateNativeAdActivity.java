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
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;

import androidx.annotation.Nullable;

public class TemplateNativeAdActivity
        extends BaseAdActivity
{
    // Map of ad unit IDs to native ad loaders
    private MaxNativeAdLoader nativeAdLoader;
    private MaxAd             nativeAd;

    private FrameLayout nativeAdLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_native_template );
        setTitle( R.string.activity_template_native_ad );

        nativeAdLayout = findViewById( R.id.native_ad_layout );
        setupCallbacksRecyclerView();

        nativeAdLoader = new MaxNativeAdLoader( "YOUR_AD_UNIT_ID", this );
        nativeAdLoader.setRevenueListener( ad -> {
            logAnonymousCallback();

            AdjustAdRevenue adjustAdRevenue = new AdjustAdRevenue( AdjustConfig.AD_REVENUE_APPLOVIN_MAX );
            adjustAdRevenue.setRevenue( ad.getRevenue(), "USD" );
            adjustAdRevenue.setAdRevenueNetwork( ad.getNetworkName() );
            adjustAdRevenue.setAdRevenueUnit( ad.getAdUnitId() );
            adjustAdRevenue.setAdRevenuePlacement( ad.getPlacement() );

            Adjust.trackAdRevenue( adjustAdRevenue );
        } );
        nativeAdLoader.setNativeAdListener( new MaxNativeAdListener()
        {
            @Override
            public void onNativeAdLoaded(@Nullable final MaxNativeAdView nativeAdView, final MaxAd ad)
            {
                logAnonymousCallback();

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
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error)
            {
                logAnonymousCallback();
            }

            @Override
            public void onNativeAdClicked(final MaxAd ad)
            {
                logAnonymousCallback();
            }

            @Override
            public void onNativeAdExpired(final MaxAd nativeAd)
            {
                logAnonymousCallback();
            }
        } );
    }

    @Override
    protected void onDestroy()
    {
        // Must destroy native ad or else there will be memory leaks.
        if ( nativeAd != null )
        {
            // Call destroy on the native ad from any native ad loader.
            nativeAdLoader.destroy( nativeAd );
        }

        // Destroy the actual loader itself
        nativeAdLoader.destroy();

        super.onDestroy();
    }

    public void onShowAdClicked(View view)
    {
        nativeAdLoader.loadAd();
    }
}
