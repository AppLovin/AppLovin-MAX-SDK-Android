package com.applovin.enterprise.apps.demoapp.ads;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX banner ads.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class ProgrammaticBannerAdActivity
        extends AppCompatActivity
        implements MaxAdViewAdListener
{
    private MaxAdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_programmatic_banner_ad );

        adView = new MaxAdView( "YOUR_AD_UNIT_ID", this );
        adView.setListener( this );

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

    //region MAX Ad Listener

    @Override
    public void onAdExpanded(final MaxAd ad) { }

    @Override
    public void onAdCollapsed(final MaxAd ad) { }

    @Override
    public void onAdLoaded(final MaxAd ad) { }

    @Override
    public void onAdLoadFailed(final String adUnitId, final int errorCode) { }

    @Override
    public void onAdDisplayed(final MaxAd ad) { }

    @Override
    public void onAdHidden(final MaxAd ad) { }

    @Override
    public void onAdClicked(final MaxAd ad) { }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final int errorCode) { }

    //endregion
}
