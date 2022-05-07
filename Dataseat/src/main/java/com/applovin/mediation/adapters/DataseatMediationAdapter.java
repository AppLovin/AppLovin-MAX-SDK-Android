package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.dataseat.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.dataseat.sdk.AdvertListener;
import com.dataseat.sdk.DSErrorCode;
import com.dataseat.sdk.Dataseat;

import java.util.concurrent.atomic.AtomicBoolean;

public class DataseatMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter
{
    private static final AtomicBoolean initialized = new AtomicBoolean();

    // Explicit default constructor declaration
    public DataseatMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            log( "Initializing Dataseat SDK..." );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            Dataseat.getInstance( context ).initializeSDK();
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return Dataseat.version();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy() { }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String tag = parameters.getThirdPartyAdPlacementId();

        Bundle customParameters = parameters.getServerParameters().getBundle( "custom_parameters" );
        float bidFloor = (float) customParameters.getDouble( "bid_floor" );

        log( "Loading interstitial ad for tag: " + tag + " and bid floor: " + bidFloor );

        Dataseat.getInstance( activity.getApplicationContext() ).preloadInterstitial( tag, bidFloor, new Dataseat.PreloadAdvertCallback()
        {
            @Override
            public void onSuccess()
            {
                log( "Interstitial ad loaded for tag: " + tag );
                listener.onInterstitialAdLoaded();
            }

            @Override
            public void onError(final DSErrorCode dsErrorCode)
            {
                MaxAdapterError adapterError = toMaxError( dsErrorCode );
                log( "Interstitial ad failed to load for tag: " + tag + " with error: " + adapterError );
                listener.onInterstitialAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        Dataseat dataseat = Dataseat.getInstance( activity.getApplicationContext() );
        if ( dataseat.hasIntersitialAdAvailable() )
        {
            dataseat.showInterstitialAd( new InterstitialAdListener( listener ) );
        }
        else
        {
            log( "Unable to show interstitial - ad not ready." );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String tag = parameters.getThirdPartyAdPlacementId();

        Bundle customParameters = parameters.getServerParameters().getBundle( "custom_parameters" );
        float bidFloor = (float) customParameters.getDouble( "bid_floor" );
        log( "Loading rewarded ad for tag: " + tag );

        Dataseat.getInstance( activity.getApplicationContext() ).preloadRewardedVideo( tag, bidFloor, new Dataseat.PreloadAdvertCallback()
        {
            @Override
            public void onSuccess()
            {
                log( "Rewarded ad loaded for tag: " + tag );
                listener.onRewardedAdLoaded();
            }

            @Override
            public void onError(final DSErrorCode dsErrorCode)
            {
                MaxAdapterError adapterError = toMaxError( dsErrorCode );
                log( "Rewarded ad failed to load for tag: " + tag + " with error: " + adapterError );
                listener.onRewardedAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        Dataseat dataseat = Dataseat.getInstance( activity.getApplicationContext() );
        if ( dataseat.hasRewardedAdAvailable() )
        {
            dataseat.showRewardedAd( new RewardedAdListener( listener ) );
            configureReward( parameters );
        }
        else
        {
            log( "Unable to show rewarded ad - ad not ready." );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    private static MaxAdapterError toMaxError(DSErrorCode dsErrorCode)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( dsErrorCode )
        {
            case NO_BID:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case VIDEO_AD_DOWNLOAD_ERROR:
            case BID_FAILED_CONNECTION:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case BANNER_AD_LOAD_ERROR:
            case FULLSCREEN_AD_LOAD_ERROR:
            case VIDEO_AD_LOAD_ERROR:
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case FULLSCREEN_AD_DISPLAY_ERROR:
            case BANNER_AD_DISPLAY_ERROR:
            case MRAID_LOAD_ERROR:
            case HTML_LOAD_ERROR:
            case MRAID_DISPLAY_ERROR:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError, adapterError.getErrorCode(), dsErrorCode.toString() );
    }

    private class InterstitialAdListener
            implements AdvertListener
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdShown(final String tag)
        {
            log( "Interstitial ad shown for tag: " + tag );
        }

        @Override
        public void onAdFailed(final String tag, final DSErrorCode dsErrorCode)
        {
            MaxAdapterError error = new MaxAdapterError( -4205, "Ad Display Failed", dsErrorCode.ordinal(), dsErrorCode.toString() );
            log( "Interstitial ad failed to display for tag: " + tag + " with error: " + error );
            listener.onInterstitialAdDisplayFailed( error );
        }

        @Override
        public void onAdImpression(final String tag)
        {
            log( "Interstitial ad logged impression for tag: " + tag );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked(final String tag)
        {
            log( "Interstitial ad clicked for tag: " + tag );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdComplete(final String tag)
        {
            log( "Interstitial ad completed for tag: " + tag );
        }

        @Override
        public void onBannerAdExpand(final String tag)
        {
            // Not used for fullscreen ads
        }

        @Override
        public void onBannerAdContract(final String tag)
        {
            // Not used for fullscreen ads
        }

        @Override
        public void onAdDismissed(final String tag)
        {
            log( "Interstitial ad hidden for tag: " + tag );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements AdvertListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdShown(final String tag)
        {
            log( "Rewarded ad shown for tag: " + tag );
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdFailed(final String tag, final DSErrorCode dsErrorCode)
        {
            MaxAdapterError error = toMaxError( dsErrorCode );
            log( "Rewarded ad load failed for tag: " + tag + " with error: " + error );
            listener.onRewardedAdLoadFailed( error );
        }

        @Override
        public void onAdImpression(final String tag)
        {
            log( "Rewarded ad logged impression for tag: " + tag );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked(final String tag)
        {
            log( "Rewarded ad clicked for tag: " + tag );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdComplete(final String tag)
        {
            log( "Rewarded ad completed for tag: " + tag );
            hasGrantedReward = true;
            listener.onRewardedAdVideoCompleted();
        }

        @Override
        public void onAdDismissed(final String tag)
        {
            log( "Rewarded ad dismissed for tag: " + tag );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward + " for tag: " + tag );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }

        @Override
        public void onBannerAdExpand(final String tag)
        {
            // Not used for fullscreen ads
        }

        @Override
        public void onBannerAdContract(final String tag)
        {
            // Not used for fullscreen ads
        }
    }
}
