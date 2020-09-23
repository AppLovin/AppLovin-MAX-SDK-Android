package com.applovin.enterprise.apps.demoapp.ads.applovin.banner;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;

import com.applovin.adview.AppLovinAdView;
import com.applovin.adview.AppLovinAdViewDisplayErrorCode;
import com.applovin.adview.AppLovinAdViewEventListener;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdSize;
import com.applovin.sdk.AppLovinSdkUtils;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

/**
 * Created by thomasso on 3/6/17.
 */
public final class BannerZoneActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_banner_programmatic );

        setupCallbacksRecyclerView();

        AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.BANNER, "YOUR_ZONE_ID", this );
        adView.setAdLoadListener( this );
        adView.setAdDisplayListener( this );
        adView.setAdViewEventListener( this );
        adView.setAdClickListener( this );

        adView.setId( ViewCompat.generateViewId() );

        Button loadButton = findViewById( R.id.load_button );
        loadButton.setOnClickListener( v -> {
            adView.loadNextAd();
        } );

        // Add programmatically created banner into our container
        ConstraintLayout bannerProgrammaticContentLayout = findViewById( R.id.banner_programmatic_layout );

        bannerProgrammaticContentLayout.addView( adView, new ConstraintLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx( this, 50 ) ) );

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( bannerProgrammaticContentLayout );
        constraintSet.connect( adView.getId(), ConstraintSet.BOTTOM, R.id.banner_programmatic_layout, ConstraintSet.BOTTOM, 0 );
        constraintSet.applyTo( bannerProgrammaticContentLayout );

        // Load an ad!
        adView.loadNextAd();
    }

    //region Ad Load Listener

    @Override
    public void adReceived(final AppLovinAd ad)
    {
        logCallback();
    }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback();
    }

    //endregion

    //region Ad Display Listener

    @Override
    public void adDisplayed(final AppLovinAd ad) { logCallback(); }

    @Override
    public void adHidden(final AppLovinAd ad) { logCallback(); }

    //endregion

    //region AdView Event Listener

    @Override
    public void adOpenedFullscreen(final AppLovinAd ad, final AppLovinAdView adView) { logCallback(); }

    @Override
    public void adClosedFullscreen(final AppLovinAd ad, final AppLovinAdView adView) { logCallback(); }

    @Override
    public void adLeftApplication(final AppLovinAd ad, final AppLovinAdView adView) { logCallback(); }

    @Override
    public void adFailedToDisplay(final AppLovinAd ad, final AppLovinAdView adView, final AppLovinAdViewDisplayErrorCode code) { logCallback(); }

    //endregion

    //region Ad Click Listener

    @Override
    public void adClicked(final AppLovinAd ad) { logCallback(); }

    //endregion
}
