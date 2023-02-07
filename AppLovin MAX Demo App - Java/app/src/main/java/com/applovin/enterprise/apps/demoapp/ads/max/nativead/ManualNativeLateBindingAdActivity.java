package com.applovin.enterprise.apps.demoapp.ads.max.nativead;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder;

import androidx.annotation.Nullable;

public class ManualNativeLateBindingAdActivity
        extends BaseAdActivity
{
    private MaxNativeAdLoader nativeAdLoader;
    private FrameLayout       nativeAdLayout;
    private Button            showAdButton;

    private MaxAd nativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_native_manual_late_binding );
        setTitle( R.string.activity_manual_native_late_binding_ad );

        nativeAdLayout = findViewById( R.id.native_ad_layout );
        showAdButton = findViewById( R.id.show_ad_button );
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

                // Save ad to be rendered later.
                nativeAd = ad;

                showAdButton.setEnabled( true );
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
            public void onNativeAdExpired(final MaxAd ad)
            {
                logAnonymousCallback();
                nativeAdLoader.loadAd();
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

    public void onLoadAdClicked(View view)
    {
        nativeAdLayout.removeAllViews();

        nativeAdLoader.loadAd();
    }

    public void onShowAdClicked(View view)
    {
        MaxNativeAdView adView = createNativeAdView();

        // Check if ad is expired before rendering
        if ( nativeAd.getNativeAd() != null && nativeAd.getNativeAd().isExpired() )
        {
            // Destroy expired ad and load a new one
            nativeAdLoader.destroy( nativeAd );
            nativeAdLoader.loadAd();

            showAdButton.setEnabled( false );
            return;
        }

        // Render the ad separately
        nativeAdLoader.render( adView, nativeAd );
        nativeAdLayout.addView( adView );
        showAdButton.setEnabled( false );
    }

    private MaxNativeAdView createNativeAdView()
    {
        MaxNativeAdViewBinder binder = new MaxNativeAdViewBinder.Builder( R.layout.native_custom_ad_view )
                .setTitleTextViewId( R.id.title_text_view )
                .setBodyTextViewId( R.id.body_text_view )
                .setAdvertiserTextViewId( R.id.advertiser_text_view )
                .setIconImageViewId( R.id.icon_image_view )
                .setMediaContentViewGroupId( R.id.media_view_container )
                .setOptionsContentViewGroupId( R.id.options_view )
                .setStarRatingContentViewGroupId( R.id.star_rating_view )
                .setCallToActionButtonId( R.id.cta_button )
                .build();
        return new MaxNativeAdView( binder, this );
    }
}
