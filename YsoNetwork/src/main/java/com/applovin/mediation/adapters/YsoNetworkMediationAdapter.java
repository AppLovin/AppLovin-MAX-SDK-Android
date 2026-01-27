package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.view.View;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.ysonetwork.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.ysocorp.ysonetwork.YsoNetwork;
import com.ysocorp.ysonetwork.enums.YNEnumActionError;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

/**
 * Created by Kenny Bui on 7/5/24.
 */
public class YsoNetworkMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        INITIALIZED = new AtomicBoolean();
    private static       InitializationStatus status;

    private InterstitialAdListener interstitialAdListener;
    private RewardedAdListener     rewardedAdListener;
    private AdViewLoadListener     adViewLoadListener;
    private AdViewShowListener     adViewShowListener;

    public YsoNetworkMediationAdapter(AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            log( "Initializing YSO Network" );
            status = InitializationStatus.INITIALIZING;
            try
            {
                Application application = (Application) getApplicationContext();
                YsoNetwork.initialize( application );
                if ( YsoNetwork.isInitialized() )
                {
                    log( "YSO Network successfully initialized" );
                    status = InitializationStatus.INITIALIZED_SUCCESS;
                }
                onCompletionListener.onCompletion( status, null );
            }
            catch ( Throwable th )
            {
                e( "YSO Network failed to initialize", th );
                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion( status, th.toString() );
            }
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return YsoNetwork.getSdkVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        interstitialAdListener = null;
        rewardedAdListener = null;
        adViewLoadListener = null;
        adViewShowListener = null;
    }

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        String signal = YsoNetwork.getSignal();
        callback.onSignalCollected( signal );
    }

    //endregion

    //region MaxInterstitialAdAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String key = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading interstitial ad for key: " + key + "..." );

        interstitialAdListener = new InterstitialAdListener( key, listener );
        YsoNetwork.interstitialLoad( key, bidResponse, interstitialAdListener );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String key = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for key: " + key + "..." );

        YsoNetwork.interstitialShow( key, interstitialAdListener, activity );
    }

    //endregion

    //region MaxRewardedAdAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String key = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading rewarded ad for key: " + key + "..." );

        rewardedAdListener = new RewardedAdListener( key, listener );
        YsoNetwork.rewardedLoad( key, bidResponse, rewardedAdListener );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String key = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for key: " + key + "..." );

        configureReward( parameters );

        YsoNetwork.rewardedShow( key, rewardedAdListener, activity );
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String key = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading ad view ad for key: " + key + "..." );

        adViewLoadListener = new AdViewLoadListener( key, listener, activity );
        YsoNetwork.bannerLoad( key, bidResponse, adViewLoadListener );
    }

    //endregion

    //region Helper Methods

    private static MaxAdapterError toMaxError(YNEnumActionError error)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( error )
        {
            case SdkNotInitialized:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case InvalidRequest:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case InvalidConfig:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case Timeout:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case Load:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case Server:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError, error.ordinal(), "" );
    }

    //endregion

    //region Ad Listeners

    private class InterstitialAdListener
            implements YsoNetwork.ActionLoad, YsoNetwork.ActionDisplay
    {
        private final String                         key;
        private final MaxInterstitialAdapterListener listener;

        private InterstitialAdListener(final String key, final MaxInterstitialAdapterListener listener)
        {
            this.key = key;
            this.listener = listener;
        }

        @Override
        public void onLoad(YNEnumActionError error)
        {
            if ( error == YNEnumActionError.None )
            {
                log( "Interstitial ad successfully loaded for key: " + key );
                listener.onInterstitialAdLoaded();
                return;
            }

            MaxAdapterError adapterError = toMaxError( error );
            log( "Interstitial ad failed to load for key: " + key + " and error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onDisplay(View view)
        {
            log( "Interstitial ad displayed for key: " + key );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClick()
        {
            log( "Interstitial ad clicked for key: " + key );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onClose(boolean display, boolean complete)
        {
            if ( !display )
            {
                log( "Interstitial ad failed to display for key: " + key );
                listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_DISPLAY_FAILED );
                return;
            }

            log( "Interstitial ad closed for key: " + key );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements YsoNetwork.ActionLoad, YsoNetwork.ActionDisplay
    {
        private final String                     key;
        private final MaxRewardedAdapterListener listener;

        private RewardedAdListener(final String key, final MaxRewardedAdapterListener listener)
        {
            this.key = key;
            this.listener = listener;
        }

        @Override
        public void onLoad(YNEnumActionError error)
        {
            if ( error == YNEnumActionError.None )
            {
                log( "Rewarded ad successfully loaded for key: " + key );
                listener.onRewardedAdLoaded();
                return;
            }

            MaxAdapterError adapterError = toMaxError( error );
            log( "Rewarded ad failed to load for key: " + key + " and error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onDisplay(View view)
        {
            log( "Rewarded ad displayed for key: " + key );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onClick()
        {
            log( "Rewarded ad clicked for key: " + key );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onClose(boolean display, boolean complete)
        {
            if ( !display )
            {
                log( "Rewarded ad failed to display for key: " + key );
                listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_DISPLAY_FAILED );
                return;
            }

            if ( complete || shouldAlwaysRewardUser() )
            {
                log( "User was rewarded for key: " + key );
                final MaxReward reward = getReward();
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad closed for key: " + key );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewLoadListener
            implements YsoNetwork.ActionLoad
    {
        private final String                   key;
        private final MaxAdViewAdapterListener listener;
        private final Activity                 activity;

        private AdViewLoadListener(final String key, final MaxAdViewAdapterListener listener, @Nullable final Activity activity)
        {
            this.key = key;
            this.listener = listener;
            this.activity = activity;
        }

        @Override
        public void onLoad(YNEnumActionError error)
        {
            if ( error != YNEnumActionError.None )
            {
                MaxAdapterError adapterError = toMaxError( error );
                log( "Ad view ad failed to load for key: " + key + " and error: " + adapterError );
                listener.onAdViewAdLoadFailed( adapterError );
                return;
            }

            log( "Ad view ad successfully loaded for key: " + key );
            log( "Showing ad view ad for key: " + key );
            adViewShowListener = new AdViewShowListener( key, listener );
            YsoNetwork.bannerShow( key, adViewShowListener, activity );
        }
    }

    private class AdViewShowListener
            implements YsoNetwork.ActionDisplay
    {
        private final String                   key;
        private final MaxAdViewAdapterListener listener;

        private AdViewShowListener(final String key, final MaxAdViewAdapterListener listener)
        {
            this.key = key;
            this.listener = listener;
        }

        @Override
        public void onDisplay(View view)
        {
            log( "Ad view ad displayed for key: " + key );
            // TODO: Decouple load and show logic
            listener.onAdViewAdLoaded( view );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onClick()
        {
            log( "Ad view ad clicked for key: " + key );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onClose(boolean display, boolean complete)
        {
            if ( !display )
            {
                log( "Ad view ad failed to display for key: " + key );
                return;
            }

            log( "Ad view ad closed for key: " + key );
        }
    }

    //endregion
}
