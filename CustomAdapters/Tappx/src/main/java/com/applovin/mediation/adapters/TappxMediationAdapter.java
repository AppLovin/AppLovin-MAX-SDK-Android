package com.applovin.mediation.adapters;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.tappx.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.tappx.sdk.android.AdRequest;
import com.tappx.sdk.android.Tappx;
import com.tappx.sdk.android.TappxAdError;
import com.tappx.sdk.android.TappxBanner;
import com.tappx.sdk.android.TappxBannerListener;
import com.tappx.sdk.android.TappxInterstitial;
import com.tappx.sdk.android.TappxInterstitialListener;
import com.tappx.sdk.android.TappxRewardedVideo;
import com.tappx.sdk.android.TappxRewardedVideoListener;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

/**
 * This is a mediation adapter for Tappx
 * <p>
 * Created by Joe Chen on January 19 2022
 */
public class TappxMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private TappxInterstitial  interstitialAd;
    private TappxRewardedVideo rewardedAd;
    private TappxBanner        adView;

    // Explicit default constructor declaration
    public TappxMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter Methods
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return Tappx.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialAd != null )
        {
            interstitialAd.setListener( null );
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setListener( null );
            rewardedAd.destroy();
            rewardedAd = null;
        }

        if ( adView != null )
        {
            adView.setListener( null );
            adView.destroy();
            adView = null;
        }
    }
    //endregion

    //region MaxInterstitialAdapter Methods
    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                final String appKey = parameters.getThirdPartyAdPlacementId();
                log( "Loading interstitial ad: " + appKey + "..." );

                interstitialAd = new TappxInterstitial( getApplicationContext(), appKey );
                interstitialAd.setListener( new InterstitialListener( listener ) );


                interstitialAd.loadAd( createAdRequest( parameters ) );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                final String appKey = parameters.getThirdPartyAdPlacementId();
                log( "Showing interstitial ad: " + appKey + "..." );

                if ( interstitialAd != null && interstitialAd.isReady() )
                {
                    interstitialAd.show();
                }
                else
                {
                    log( "Interstitial ad not ready" );
                    listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
                }
            }
        } );
    }
    //endregion

    //region MaxRewardedAdapter Methods
    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                final String appKey = parameters.getThirdPartyAdPlacementId();
                log( "Loading rewarded ad: " + appKey + "..." );

                rewardedAd = new TappxRewardedVideo( getApplicationContext(), appKey );
                rewardedAd.setListener( new RewardedAdListener( listener ) );


                rewardedAd.loadAd( createAdRequest( parameters ) );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                final String appKey = parameters.getThirdPartyAdPlacementId();
                log( "Showing rewarded ad: " + appKey + "..." );

                if ( rewardedAd != null && rewardedAd.isReady() )
                {
                    // Configure userReward from server.
                    configureReward( parameters );

                    rewardedAd.show();
                }
                else
                {
                    log( "Rewarded ad not ready" );
                    listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
                }
            }
        } );
    }
    //endregion

    //region MaxAdViewAdapter Methods
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                final String appKey = parameters.getThirdPartyAdPlacementId();
                log( "Loading " + adFormat.getLabel() + " ad: " + appKey + "..." );

                adView = new TappxBanner( getApplicationContext(), appKey );
                adView.setListener( new AdViewListener( listener ) );

                adView.setAdSize( toAdSize( adFormat ) );
                adView.setEnableAutoRefresh( false );

                adView.loadAd( createAdRequest( parameters ) );
            }
        } );
    }
    //endregion

    private AdRequest createAdRequest(MaxAdapterParameters parameters)
    {
        AdRequest adRequest = new AdRequest();
        adRequest.sdkType( "native" );
        adRequest.mediator( "applovin" );

        Bundle customParameters = parameters.getCustomParameters();
        boolean isTesting = parameters.isTesting() || customParameters.getBoolean( "is_testing" ) || customParameters.getBoolean( "test" );
        adRequest.useTestAds( isTesting );

        String endpoint = parameters.getCustomParameters().getString( "endpoint" );
        if ( !TextUtils.isEmpty( endpoint ) )
        {
            adRequest.setEndpoint( endpoint );
        }

        return adRequest;
    }

    private static MaxAdapterError toMaxError(final TappxAdError tappxAdError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( tappxAdError )
        {
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case SERVER_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case DEVELOPER_ERROR:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
        }
        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), tappxAdError.ordinal(), tappxAdError.name() );
    }

    private TappxBanner.AdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( MaxAdFormat.BANNER == adFormat )
        {
            return TappxBanner.AdSize.BANNER_320x50;
        }
        else if ( MaxAdFormat.LEADER == adFormat )
        {
            return TappxBanner.AdSize.BANNER_728x90;
        }
        else if ( MaxAdFormat.MREC == adFormat )
        {
            return TappxBanner.AdSize.BANNER_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private class InterstitialListener
            implements TappxInterstitialListener
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onInterstitialLoaded(final TappxInterstitial tappxInterstitial)
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onInterstitialLoadFailed(final TappxInterstitial tappxInterstitial, final TappxAdError tappxAdError)
        {
            MaxAdapterError adapterError = toMaxError( tappxAdError );
            log( "Interstitial failed to load: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onInterstitialShown(final TappxInterstitial tappxInterstitial)
        {
            log( "Interstitial shown" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onInterstitialClicked(final TappxInterstitial tappxInterstitial)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onInterstitialDismissed(final TappxInterstitial tappxInterstitial)
        {
            log( "Interstitial dismissed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements TappxRewardedVideoListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        public RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRewardedVideoLoaded(final TappxRewardedVideo tappxRewardedVideo)
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onRewardedVideoLoadFailed(final TappxRewardedVideo tappxRewardedVideo, final TappxAdError tappxAdError)
        {
            MaxAdapterError adapterError = toMaxError( tappxAdError );
            log( "Rewarded ad failed with error code: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onRewardedVideoStart(final TappxRewardedVideo tappxRewardedVideo)
        {
            log( "Rewarded ad started" );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onRewardedVideoPlaybackFailed(final TappxRewardedVideo tappxRewardedVideo)
        {
            log( "Rewarded ad playback failed" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onRewardedVideoClicked(final TappxRewardedVideo tappxRewardedVideo)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onRewardedVideoCompleted(final TappxRewardedVideo tappxRewardedVideo)
        {
            log( "Rewarded ad completed" );
            listener.onRewardedAdVideoCompleted();
            hasGrantedReward = true;
        }

        @Override
        public void onRewardedVideoClosed(final TappxRewardedVideo tappxRewardedVideo)
        {
            log( "Rewarded ad closed" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            implements TappxBannerListener
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewListener(MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onBannerLoaded(final TappxBanner tappxBanner)
        {
            log( "AdView loaded" );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onBannerLoadFailed(final TappxBanner tappxBanner, final TappxAdError tappxAdError)
        {
            MaxAdapterError adapterError = toMaxError( tappxAdError );
            log( "AdView failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onBannerClicked(final TappxBanner tappxBanner)
        {
            log( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onBannerExpanded(final TappxBanner tappxBanner)
        {
            log( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onBannerCollapsed(final TappxBanner tappxBanner)
        {
            log( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }
    }
}
