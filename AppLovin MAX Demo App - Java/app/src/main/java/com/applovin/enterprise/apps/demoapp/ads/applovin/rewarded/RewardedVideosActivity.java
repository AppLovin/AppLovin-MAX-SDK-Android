package com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded;

import android.os.Bundle;
import android.widget.Button;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.sdk.AppLovinErrorCodes;

import java.util.Map;

public class RewardedVideosActivity
        extends BaseAdActivity
        implements AppLovinAdLoadListener, AppLovinAdVideoPlaybackListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdRewardListener
{
    private AppLovinIncentivizedInterstitial incentivizedInterstitial;
    private Button                           showButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_rewarded_videos );

        setupCallbacksRecyclerView();

        final Button loadButton = findViewById( R.id.loadButton );
        showButton = findViewById( R.id.showButton );

        incentivizedInterstitial = AppLovinIncentivizedInterstitial.create( getApplicationContext() );

        // You need to preload each rewarded video before it can be displayed
        loadButton.setOnClickListener( v -> {
            showButton.setEnabled( false );

            incentivizedInterstitial.preload( this );
        } );

        showButton.setOnClickListener( v -> {
            showButton.setEnabled( false );

            incentivizedInterstitial.show( this, this, this, this, this );
        } );
    }

    //region Ad Load Listener

    @Override
    public void adReceived(AppLovinAd appLovinAd)
    {
        logCallback();

        showButton.setEnabled( true );
    }

    @Override
    public void failedToReceiveAd(int errorCode)
    {
        // Look at AppLovinErrorCodes.java for list of error codes
        logCallback();

        showButton.setEnabled( true );
    }

    //endregion

    //region Ad Display Listener

    @Override
    public void adDisplayed(AppLovinAd appLovinAd) { logCallback(); }

    @Override
    public void adHidden(AppLovinAd appLovinAd) { logCallback(); }

    //endregion

    //region Ad Click Listener

    @Override
    public void adClicked(AppLovinAd appLovinAd) { logCallback(); }

    //endregion

    //region Ad Video Playback Listener

    @Override
    public void videoPlaybackBegan(AppLovinAd appLovinAd) { logCallback(); }

    @Override
    public void videoPlaybackEnded(AppLovinAd appLovinAd, double percentViewed, boolean wasFullyViewed) { logCallback(); }

    //endregion

    //region Ad Reward Listener

    @Override
    public void userRewardVerified(AppLovinAd appLovinAd, Map map)
    {
        // AppLovin servers validated the reward. Refresh user balance from your server.  We will also pass the number of coins
        // awarded and the name of the currency.  However, ideally, you should verify this with your server before granting it.
        logCallback();
    }

    @Override
    public void userOverQuota(AppLovinAd appLovinAd, Map map)
    {
        // Your user has already earned the max amount you allowed for the day at this point, so
        // don't give them any more money. By default we'll show them a alert explaining this,
        // though you can change that from the AppLovin dashboard.

        logCallback();
    }

    @Override
    public void userRewardRejected(AppLovinAd appLovinAd, Map map)
    {
        // Your user couldn't be granted a reward for this view. This could happen if you've blacklisted
        // them, for example. Don't grant them any currency. By default we'll show them an alert explaining this,
        // though you can change that from the AppLovin dashboard.

        logCallback();
    }

    @Override
    public void validationRequestFailed(AppLovinAd appLovinAd, int responseCode)
    {
        if ( responseCode == AppLovinErrorCodes.INCENTIVIZED_USER_CLOSED_VIDEO )
        {
            // Your user exited the video prematurely. It's up to you if you'd still like to grant
            // a reward in this case. Most developers choose not to. Note that this case can occur
            // after a reward was initially granted (since reward validation happens as soon as a
            // video is launched).
        }
        else if ( responseCode == AppLovinErrorCodes.INCENTIVIZED_SERVER_TIMEOUT || responseCode == AppLovinErrorCodes.INCENTIVIZED_UNKNOWN_SERVER_ERROR )
        {
            // Some server issue happened here. Don't grant a reward. By default we'll show the user
            // a alert telling them to try again later, but you can change this in the
            // AppLovin dashboard.
        }
        else if ( responseCode == AppLovinErrorCodes.INCENTIVIZED_NO_AD_PRELOADED )
        {
            // Indicates that the developer called for a rewarded video before one was available.
            // Note: This code is only possible when working with rewarded videos.
        }

        logCallback();
    }

    //endregion
}
