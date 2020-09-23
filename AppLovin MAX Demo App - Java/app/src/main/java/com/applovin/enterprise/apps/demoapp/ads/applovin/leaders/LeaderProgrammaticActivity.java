package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders;

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

public class LeaderProgrammaticActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_leader_programmatic );

        setupCallbacksRecyclerView();

        AppLovinAdView adView = new AppLovinAdView( AppLovinAdSize.LEADER, this );
        adView.setAdLoadListener( this );
        adView.setAdDisplayListener( this );
        adView.setAdViewEventListener( this );
        adView.setAdClickListener( this );

        adView.setId( ViewCompat.generateViewId() );

        final Button loadButton = findViewById( R.id.load_button );
        loadButton.setOnClickListener( v -> {
            adView.loadNextAd();
        } );

        // Add programmatically created leader into our container
        ConstraintLayout leaderLayout = findViewById( R.id.leader_programmatic_layout );
        leaderLayout.addView( adView, new ConstraintLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx( this, 90 ) ) );

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone( leaderLayout );
        constraintSet.connect( adView.getId(), ConstraintSet.BOTTOM, R.id.leader_programmatic_layout, ConstraintSet.BOTTOM, 0 );
        constraintSet.applyTo( leaderLayout );

        // Load an ad!
        adView.loadNextAd();
    }

    //region AppLovin Ad Load Listener

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
