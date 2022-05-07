package com.applovin.mediation.adapters;

import android.app.Activity;
import android.os.Bundle;

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
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.nend.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

import net.nend.android.NendAdInterstitialVideo;
import net.nend.android.NendAdListener;
import net.nend.android.NendAdRewardItem;
import net.nend.android.NendAdRewardedListener;
import net.nend.android.NendAdRewardedVideo;
import net.nend.android.NendAdVideo;
import net.nend.android.NendAdVideoListener;
import net.nend.android.NendAdView;

/**
 * Created by Lorenzo Gentile on 7/1/19.
 */
public class NendMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final String KEY_API_KEY          = "api_key";
    private static final String KEY_SET_MEDIATION_ID = "set_mediation_identifier";
    private static final String KEY_USER_ID          = "user_id";

    private NendAdInterstitialVideo interstitialVideo;
    private NendAdRewardedVideo     rewardedVideo;
    private NendAdView              adView;

    // Explicit default constructor declaration
    public NendMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( net.nend.android.BuildConfig.class, "VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialVideo != null )
        {
            interstitialVideo.releaseAd();
            interstitialVideo.setAdListener( null );
            interstitialVideo = null;
        }

        if ( rewardedVideo != null )
        {
            rewardedVideo.releaseAd();
            rewardedVideo.setAdListener( null );
            rewardedVideo = null;
        }

        if ( adView != null )
        {
            adView.removeListener();
            adView = null;
        }
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final Bundle serverParams = parameters.getServerParameters();
        final int spotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        final String apiKey = serverParams.getString( KEY_API_KEY );
        log( "Loading interstitial ad for API key: " + apiKey + " and spot id: " + spotId + "..." );

        interstitialVideo = new NendAdInterstitialVideo( activity, spotId, apiKey );

        if ( serverParams.getBoolean( KEY_SET_MEDIATION_ID, false ) )
        {
            interstitialVideo.setMediationName( mediationTag() );
        }

        if ( serverParams.containsKey( KEY_USER_ID ) )
        {
            interstitialVideo.setUserId( serverParams.getString( KEY_USER_ID ) );
        }

        interstitialVideo.setAdListener( new InterstitialListener( listener ) );
        interstitialVideo.loadAd();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialVideo == null )
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.UNSPECIFIED );

            return;
        }

        if ( !interstitialVideo.isLoaded() )
        {
            log( "Interstitial ad has not loaded" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );

            return;
        }

        interstitialVideo.showAd( activity );
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final Bundle serverParams = parameters.getServerParameters();
        final int spotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        final String apiKey = serverParams.getString( KEY_API_KEY );
        log( "Loading rewarded video ad for API key: " + apiKey + " and spot id: " + spotId + "..." );

        rewardedVideo = new NendAdRewardedVideo( activity, spotId, apiKey );

        if ( serverParams.getBoolean( KEY_SET_MEDIATION_ID, false ) )
        {
            rewardedVideo.setMediationName( mediationTag() );
        }

        if ( serverParams.containsKey( KEY_USER_ID ) )
        {
            rewardedVideo.setUserId( serverParams.getString( KEY_USER_ID ) );
        }

        rewardedVideo.setAdListener( new RewardedAdListener( listener ) );
        rewardedVideo.loadAd();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedVideo == null )
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.UNSPECIFIED );

            return;
        }

        if ( !rewardedVideo.isLoaded() )
        {
            log( "Rewarded ad has not loaded" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );

            return;
        }

        configureReward( parameters );

        rewardedVideo.showAd( activity );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final int spotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        final String apiKey = parameters.getServerParameters().getString( KEY_API_KEY );
        log( "Loading ad view for API key: " + apiKey + " and spot id: " + spotId + "..." );

        adView = new NendAdView( activity, spotId, apiKey );

        adView.setListener( new AdViewListener( listener ) );
        adView.loadAd();
    }

    //region Ad Listeners

    private class InterstitialListener
            implements NendAdVideoListener
    {
        final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(NendAdVideo nendAdVideo)
        {
            log( "Interstitial loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onFailedToLoad(NendAdVideo nendAdVideo, int errorCode)
        {
            log( "Interstitial failed to load with error code: " + errorCode );
            listener.onInterstitialAdLoadFailed( toMaxError( errorCode ) );
        }

        @Override
        public void onFailedToPlay(NendAdVideo nendAdVideo)
        {
            log( "Interstitial failed to play video" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onShown(NendAdVideo nendAdVideo)
        {
            log( "Interstitial shown" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClosed(NendAdVideo nendAdVideo)
        {
            log( "Interstitial hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onStarted(NendAdVideo nendAdVideo)
        {
            log( "Interstitial video started" );
        }

        @Override
        public void onStopped(NendAdVideo nendAdVideo)
        {
            log( "Interstitial video stopped" );
        }

        @Override
        public void onCompleted(NendAdVideo nendAdVideo)
        {
            log( "Interstitial video completed" );
        }

        @Override
        public void onAdClicked(NendAdVideo nendAdVideo)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onInformationClicked(NendAdVideo nendAdVideo)
        {
            log( "Interstitial information button clicked" );
        }
    }

    private class RewardedAdListener
            implements NendAdRewardedListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward = false;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRewarded(final NendAdVideo nendAdVideo, final NendAdRewardItem nendAdRewardItem)
        {
            log( "Rewarded video granted reward" );
            hasGrantedReward = true;
        }

        @Override
        public void onLoaded(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onFailedToLoad(final NendAdVideo nendAdVideo, final int errorCode)
        {
            log( "Rewarded video failed to load with error code: " + errorCode );
            listener.onRewardedAdLoadFailed( toMaxError( errorCode ) );
        }

        @Override
        public void onFailedToPlay(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video failed to play" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onShown(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video shown" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onClosed(final NendAdVideo nendAdVideo)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded video closed" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onStarted(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video started" );
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onStopped(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video stopped" );
        }

        @Override
        public void onCompleted(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video completed" );
            listener.onRewardedAdVideoCompleted();
        }

        @Override
        public void onAdClicked(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onInformationClicked(final NendAdVideo nendAdVideo)
        {
            log( "Rewarded video information clicked" );
        }
    }

    private class AdViewListener
            implements NendAdListener
    {
        final MaxAdViewAdapterListener listener;

        AdViewListener(MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onReceiveAd(final NendAdView nendAdView)
        {
            // Nend ads auto-refresh by default so we should stop it (so MAX can control refreshing and track it).
            adView.pause();

            log( "Ad view loaded" );
            listener.onAdViewAdLoaded( nendAdView );
        }

        @Override
        public void onFailedToReceiveAd(final NendAdView nendAdView)
        {
            // Nend ads auto-refresh by default so we should stop it (so MAX can control refreshing and track it).
            adView.pause();

            log( "Ad view failed to load with nend error: " + nendAdView.getNendError() );
            NendAdView.NendError nendError = nendAdView.getNendError();
            listener.onAdViewAdLoadFailed( toMaxError( nendError ) );
        }

        @Override
        public void onClick(final NendAdView nendAdView)
        {
            log( "Ad view clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onDismissScreen(final NendAdView nendAdView)
        {
            // Callback for when the user clicks on the banner ad then returns back to the app.
        }
    }

    //endregion

    //region Error Helper Functions

    /**
     * Translated from https://github.com/fan-ADN/nendSDK-Android/wiki/Implementation-for-video-ads#acquire-error-information
     */
    private static MaxAdapterError toMaxError(final int nendError)
    {
        final MaxAdapterError adapterError;

        if ( nendError == 204 )
        {
            adapterError = MaxAdapterError.NO_FILL;
        }
        else if ( nendError == 400 )
        {
            adapterError = MaxAdapterError.BAD_REQUEST;
        }
        else if ( nendError >= 500 && nendError < 600 )
        {
            adapterError = MaxAdapterError.SERVER_ERROR;
        }
        else if ( nendError == 600 )
        {
            // From the developer: "Error in SDK"
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else if ( nendError == 601 )
        {
            // From the developer: "Ad download failed"
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else if ( nendError == 602 )
        {
            // From the developer: "Fallback fullscreen ad failed"
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else if ( nendError == 603 )
        {
            // From the developer: "Invalid network"
            adapterError = MaxAdapterError.NO_CONNECTION;
        }
        else
        {
            adapterError = MaxAdapterError.UNSPECIFIED;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), nendError, "" );
    }

    /**
     * Translated from AdMob Nend Adapter
     */
    private static MaxAdapterError toMaxError(final NendAdView.NendError nendErrorCode)
    {
        final MaxAdapterError adapterError;
        if ( nendErrorCode == NendAdView.NendError.INVALID_RESPONSE_TYPE )
        {
            // From the developer: "Invalid ad view type"
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else if ( nendErrorCode == NendAdView.NendError.FAILED_AD_DOWNLOAD )
        {
            // From the developer: "Failed ad download"
            adapterError = MaxAdapterError.BAD_REQUEST;
        }
        else if ( nendErrorCode == NendAdView.NendError.FAILED_AD_REQUEST )
        {
            // From the developer: "Failed to ad request"
            adapterError = MaxAdapterError.BAD_REQUEST;
        }
        else if ( nendErrorCode == NendAdView.NendError.AD_SIZE_TOO_LARGE )
        {
            // From the developer: "Ad size is larger than the display size"
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else if ( nendErrorCode == NendAdView.NendError.AD_SIZE_DIFFERENCES )
        {
            // From the developer: "Difference in ad sized"
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else
        {
            adapterError = MaxAdapterError.UNSPECIFIED;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), nendErrorCode.getCode(), nendErrorCode.getMessage() );
    }

    //endregion
}
