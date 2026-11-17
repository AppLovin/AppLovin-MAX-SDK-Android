package com.applovin.mediation.adapters;

import android.app.Activity;
import android.os.Bundle;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.helix.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.adn.sdk.publisher.AdnAdError;
import io.adn.sdk.publisher.AdnAdInfo;
import io.adn.sdk.publisher.AdnAdPlacement;
import io.adn.sdk.publisher.AdnAdRequest;
import io.adn.sdk.publisher.AdnBidTokenCallback;
import io.adn.sdk.publisher.AdnFullscreenAd;
import io.adn.sdk.publisher.AdnFullscreenAdListener;
import io.adn.sdk.publisher.AdnInitializationCallback;
import io.adn.sdk.publisher.AdnInitializationStatus;
import io.adn.sdk.publisher.AdnLoadTimeout;
import io.adn.sdk.publisher.AdnMediationType;
import io.adn.sdk.publisher.AdnSdk;

public class AdnMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private AdnFullscreenAd interstitialAd;
    private AdnFullscreenAd rewardedAd;

    public AdnMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter Methods

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            AdnSdk.setMediationType( AdnMediationType.MAX_ADS );
            AdnSdk.setTestMode( parameters.isTesting() );

            log( "Initializing Helix SDK..." );

            AdnSdk.initialize( getApplicationContext(), new AdnInitializationCallback()
            {
                @Override
                public void onCompletion(final AdnInitializationStatus adnStatus)
                {
                    if ( adnStatus == AdnInitializationStatus.Success )
                    {
                        log( "Helix SDK initialized" );
                        status = InitializationStatus.INITIALIZED_SUCCESS;
                        onCompletionListener.onCompletion( status, null );
                    }
                    else
                    {
                        log( "Helix SDK failed to initialize with error: " + adnStatus );
                        status = InitializationStatus.INITIALIZED_FAILURE;
                        onCompletionListener.onCompletion( status, adnStatus.toString() );
                    }
                }
            } );
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return AdnSdk.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log( "Destroy called for adapter " + this );

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
    }

    //endregion

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        final AdnAdPlacement adPlacement = toAdnAdPlacement( parameters.getAdFormat() );
        if ( adPlacement == null )
        {
            log( "Signal collection failed with error: unsupported ad format " + parameters.getAdFormat() );
            callback.onSignalCollectionFailed( "Unsupported ad format" );

            return;
        }

        AdnSdk.getBidToken( adPlacement, new AdnBidTokenCallback()
        {
            @Override
            public void onComplete(final String response)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( response );
            }
        } );
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad for placement id: " + placementId + "..." );

        if ( !AdnSdk.isInitialized() )
        {
            log( "Helix SDK not successfully initialized: failing interstitial ad load for placement id: " + placementId );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( activity == null )
        {
            log( "Interstitial ad failed to load for placement id: " + placementId + " with error: " + MaxAdapterError.MISSING_ACTIVITY );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );

            return;
        }

        updateMuteState( parameters );

        interstitialAd = AdnSdk.getInterstitialAdInstance( activity, new InterstitialAdListener( placementId, listener ) );
        interstitialAd.load( new AdnAdRequest.AdBidRequest( AdnAdPlacement.INTERSTITIAL, parameters.getBidResponse(), AdnLoadTimeout.MEDIATION ) );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for placement id: " + placementId + "..." );

        if ( interstitialAd == null || !interstitialAd.isReady() )
        {
            log( "Unable to show interstitial ad for placement id: " + placementId + " - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                                         MaxAdapterError.AD_NOT_READY.getMessage() ) );

            return;
        }

        interstitialAd.show();
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for placement id: " + placementId + "..." );

        if ( !AdnSdk.isInitialized() )
        {
            log( "Helix SDK not successfully initialized: failing rewarded ad load for placement id: " + placementId );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( activity == null )
        {
            log( "Rewarded ad failed to load for placement id: " + placementId + " with error: " + MaxAdapterError.MISSING_ACTIVITY );
            listener.onRewardedAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );

            return;
        }

        updateMuteState( parameters );

        rewardedAd = AdnSdk.getRewardedAdInstance( activity, new RewardedAdListener( placementId, listener ) );
        rewardedAd.load( new AdnAdRequest.AdBidRequest( AdnAdPlacement.REWARDED, parameters.getBidResponse(), AdnLoadTimeout.MEDIATION ) );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for placement id: " + placementId + "..." );

        if ( rewardedAd == null || !rewardedAd.isReady() )
        {
            log( "Unable to show rewarded ad for placement id: " + placementId + " - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );

            return;
        }

        configureReward( parameters );
        rewardedAd.show();
    }

    //endregion

    //region Helper Methods

    @Nullable
    private AdnAdPlacement toAdnAdPlacement(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            return AdnAdPlacement.INTERSTITIAL;
        }
        else if ( adFormat == MaxAdFormat.REWARDED )
        {
            return AdnAdPlacement.REWARDED;
        }
        else
        {
            return null;
        }
    }

    private static MaxAdapterError toMaxError(final AdnAdError error)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        if ( error instanceof AdnAdError.SdkNotInitialized )
        {
            adapterError = MaxAdapterError.NOT_INITIALIZED;
        }
        else if ( error instanceof AdnAdError.Timeout )
        {
            adapterError = MaxAdapterError.TIMEOUT;
        }
        else if ( error instanceof AdnAdError.BadRequest )
        {
            adapterError = MaxAdapterError.BAD_REQUEST;
        }
        else if ( error instanceof AdnAdError.ServerError )
        {
            adapterError = MaxAdapterError.SERVER_ERROR;
        }
        else if ( error instanceof AdnAdError.NoFill )
        {
            adapterError = MaxAdapterError.NO_FILL;
        }
        else if ( error instanceof AdnAdError.InternalError || error instanceof AdnAdError.SdkInitFailure )
        {
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }

        return new MaxAdapterError( adapterError, error.getErrorCode(), error.getErrorMessage() );
    }

    private void updateMuteState(final MaxAdapterResponseParameters parameters)
    {
        final Bundle serverParameters = parameters.getServerParameters();
        if ( serverParameters.containsKey( "is_muted" ) )
        {
            AdnSdk.setAdsMuted( serverParameters.getBoolean( "is_muted" ) );
        }
    }

    //endregion

    //region Ad Listeners

    private class InterstitialAdListener
            implements AdnFullscreenAdListener
    {
        private final String                         placementId;
        private final MaxInterstitialAdapterListener listener;

        private InterstitialAdListener(final String placementId, final MaxInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final AdnAdInfo adInfo)
        {
            log( "Interstitial ad loaded for placement id: " + placementId );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdLoadFailed(@NonNull final AdnAdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Interstitial ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShown(@Nullable final AdnAdInfo adInfo) { }

        @Override
        public void onAdShowFailed(@Nullable final AdnAdInfo adInfo, @NonNull final AdnAdError error)
        {
            final MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                      error.getErrorCode(),
                                                                      error.getErrorMessage() );
            log( "Interstitial ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdImpression(@Nullable final AdnAdInfo adInfo)
        {
            log( "Interstitial ad impression recorded for placement id: " + placementId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked(@Nullable final AdnAdInfo adInfo)
        {
            log( "Interstitial ad click recorded for placement id: " + placementId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed(@Nullable final AdnAdInfo adInfo)
        {
            log( "Interstitial ad hidden for placement id: " + placementId );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdRewarded(@Nullable final AdnAdInfo adInfo) { }
    }

    private class RewardedAdListener
            implements AdnFullscreenAdListener
    {
        private final String                     placementId;
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedAdListener(final String placementId, final MaxRewardedAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final AdnAdInfo adInfo)
        {
            log( "Rewarded ad loaded for placement id: " + placementId );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdLoadFailed(@NonNull final AdnAdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Rewarded ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShown(@Nullable final AdnAdInfo adInfo) { }

        @Override
        public void onAdShowFailed(@Nullable final AdnAdInfo adInfo, @NonNull final AdnAdError error)
        {
            final MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                      error.getErrorCode(),
                                                                      error.getErrorMessage() );
            log( "Rewarded ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdImpression(@Nullable final AdnAdInfo adInfo)
        {
            log( "Rewarded ad impression recorded for placement id: " + placementId );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked(@Nullable final AdnAdInfo adInfo)
        {
            log( "Rewarded ad click recorded for placement id: " + placementId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdRewarded(@Nullable final AdnAdInfo adInfo)
        {
            log( "User earned reward for placement id: " + placementId );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed(@Nullable final AdnAdInfo adInfo)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden for placement id: " + placementId );
            listener.onRewardedAdHidden();
        }
    }

    //endregion
}
