package com.applovin.enterprise.apps.demoapp.ads;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX rewarded ads.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class RewardedAdActivity
        extends AppCompatActivity
        implements MaxRewardedAdListener
{
    private MaxRewardedAd rewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_rewarded_ad );

        rewardedAd = MaxRewardedAd.getInstance( "YOUR_AD_UNIT_ID", this );
        rewardedAd.setListener( this );

        rewardedAd.loadAd();
    }

    public void showAd(View view)
    {
        if ( rewardedAd.isReady() )
        {
            rewardedAd.showAd();
        }
    }

    //region MAX Ad Listener

    @Override
    public void onRewardedVideoStarted(final MaxAd ad) { }

    @Override
    public void onRewardedVideoCompleted(final MaxAd ad) { }

    @Override
    public void onUserRewarded(final MaxAd ad, final MaxReward reward)
    {
        // Rewarded ad was displayed and user should receive the reward.
    }

    @Override
    public void onAdLoaded(final MaxAd ad)
    {
        // Rewarded ad is ready to be shown. rewardedAd.isReady() will now return 'true'
    }

    @Override
    public void onAdLoadFailed(final String adUnitId, final int errorCode)
    {
        // Rewarded ad failed to load. We recommend retrying in 3 seconds.
        final Handler handler = new Handler();
        handler.postDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                rewardedAd.loadAd();
            }
        }, 3000 );
    }

    @Override
    public void onAdDisplayed(final MaxAd ad) { }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final int errorCode)
    {
        // Rewarded ad failed to display. We recommend loading the next ad.
        rewardedAd.loadAd();
    }

    @Override
    public void onAdHidden(final MaxAd ad) { }

    @Override
    public void onAdClicked(final MaxAd ad) { }

    //endregion
}
