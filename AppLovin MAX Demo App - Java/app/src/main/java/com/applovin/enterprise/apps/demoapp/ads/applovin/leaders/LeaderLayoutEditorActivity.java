package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders;

import android.os.Bundle;
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

public class LeaderLayoutEditorActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdViewEventListener, AppLovinAdClickListener
{
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_leader_layout_editor );

        setupCallbacksRecyclerView();

        AppLovinAdView adView = findViewById( R.id.ad_view );
        adView.setAdLoadListener( this );
        adView.setAdDisplayListener( this );
        adView.setAdViewEventListener( this );
        adView.setAdClickListener( this );

        Button loadButton = findViewById( R.id.load_button );
        loadButton.setOnClickListener( view -> {
            adView.loadNextAd();
        } );

        // Load an ad!
        adView.loadNextAd();

        //
        // Please note that the AppLovinAdView CAN AUTOMATICALLY invoke loadNextAd() upon inflation from layout
        // To do so, add the following attributes to the com.applovin.adview.AppLovinAdView element:
        //
        // xmlns:demo="http://schemas.applovin.com/android/1.0"
        // demo:loadAdOnCreate="true"
        //
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
