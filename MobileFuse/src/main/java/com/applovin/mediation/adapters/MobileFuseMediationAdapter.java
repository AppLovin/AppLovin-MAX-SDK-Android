package com.applovin.mediation.adapters;

import android.app.Activity;

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
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.mobilefuse.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.mobilefuse.sdk.AdError;
import com.mobilefuse.sdk.MobileFuse;
import com.mobilefuse.sdk.MobileFuseBannerAd;
import com.mobilefuse.sdk.MobileFuseInterstitialAd;
import com.mobilefuse.sdk.MobileFuseRewardedAd;
import com.mobilefuse.sdk.MobileFuseSettings;
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenProvider;
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenRequest;
import com.mobilefuse.sdk.internal.TokenGeneratorListener;
import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MobileFuseMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{

    private MobileFuseInterstitialAd interstitialAd;
    private MobileFuseRewardedAd     rewardedAd;
    private MobileFuseBannerAd       adView;

    public MobileFuseMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        MobileFuseSettings.setTestMode( parameters.isTesting() );
        onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_UNKNOWN, null );
    }

    @Override
    public String getSdkVersion()
    {
        return MobileFuse.getSdkVersion();
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
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setListener( null );
            rewardedAd = null;
        }

        if ( adView != null )
        {
            adView.destroy();
            adView.setListener( null );
            adView = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final @Nullable Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacyPreferences( parameters );

        MobileFuseBiddingTokenRequest tokenRequest = new MobileFuseBiddingTokenRequest( MobileFuse.getPrivacyPreferences(), parameters.isTesting() );
        MobileFuseBiddingTokenProvider.getToken( tokenRequest, getApplicationContext(), new TokenGeneratorListener()
        {
            @Override
            public void onTokenGenerated(@NonNull final String signal)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( signal );
            }

            @Override
            public void onTokenGenerationFailed(@NonNull final String errorMessage)
            {
                log( "Signal collection failed: " + errorMessage );
                callback.onSignalCollectionFailed( errorMessage );
            }
        } );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading interstitial ad: " + placementId );

        updatePrivacyPreferences( parameters );

        interstitialAd = new MobileFuseInterstitialAd( activity, placementId );
        interstitialAd.setListener( new InterstitialAdListener( listener ) );
        interstitialAd.loadAdFromBiddingToken( parameters.getBidResponse() );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( !interstitialAd.isLoaded() )
        {
            log( "Unable to show interstitial - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
            return;
        }

        interstitialAd.showAd();
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded ad: " + placementId );

        updatePrivacyPreferences( parameters );

        rewardedAd = new MobileFuseRewardedAd( activity, placementId );
        rewardedAd.setListener( new RewardedAdListener( listener ) );
        rewardedAd.loadAdFromBiddingToken( parameters.getBidResponse() );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( rewardedAd.isLoaded() )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
            return;
        }

        configureReward( parameters );

        rewardedAd.showAd();
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading " + adFormat.getLabel() + " ad: " + placementId );

        updatePrivacyPreferences( parameters );

        adView = new MobileFuseBannerAd( getApplicationContext(), placementId, toAdSize( adFormat ) );
        adView.setListener( new AdViewAdListener( listener ) );
        adView.setAutorefreshEnabled( false );
        adView.setMuted( true );
        adView.loadAdFromBiddingToken( parameters.getBidResponse() );
    }

    private MaxAdapterError toMaxError(final AdError mobileFuseError)
    {
        MaxAdapterError maxAdapterError = MaxAdapterError.UNSPECIFIED;
        if ( mobileFuseError == null ) return maxAdapterError;

        switch ( mobileFuseError )
        {
            case AD_ALREADY_LOADED:
            case AD_LOAD_ERROR:
                maxAdapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case AD_ALREADY_RENDERED:
            case AD_RUNTIME_ERROR:
                maxAdapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
        }

        return new MaxAdapterError( maxAdapterError.getCode(),
                                    maxAdapterError.getMessage(),
                                    mobileFuseError.getErrorCode(),
                                    mobileFuseError.getErrorMessage() );
    }

    private MobileFuseBannerAd.AdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return MobileFuseBannerAd.AdSize.BANNER_300x50;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return MobileFuseBannerAd.AdSize.BANNER_728x90;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return MobileFuseBannerAd.AdSize.BANNER_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private void updatePrivacyPreferences(final MaxAdapterParameters parameters)
    {
        MobileFusePrivacyPreferences.Builder privacyPreferencesBuilder = new MobileFusePrivacyPreferences.Builder();

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            privacyPreferencesBuilder.setUsPrivacyConsentString( isDoNotSell ? "1YY-" : "1YN-" );
        }
        else
        {
            privacyPreferencesBuilder.setUsPrivacyConsentString( "1---" );
        }

        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            privacyPreferencesBuilder.setSubjectToCoppa( isAgeRestrictedUser );
        }

        String consentString = parameters.getConsentString();
        if ( consentString != null )
        {
            privacyPreferencesBuilder.setIabConsentString( consentString );
        }

        MobileFuse.setPrivacyPreferences( privacyPreferencesBuilder.build() );
    }

    private class InterstitialAdListener
            implements MobileFuseInterstitialAd.Listener
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdNotFilled()
        {
            log( "Interstitial ad failed to load - no fill" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onAdExpired()
        {
            log( "Interstitial ad expired" );
        }

        @Override
        public void onAdRendered()
        {
            log( "Interstitial ad shown" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            log( "Interstitial ad closed" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdError(final AdError adError)
        {
            MaxAdapterError maxAdapterError = toMaxError( adError );

            if ( adError == AdError.AD_LOAD_ERROR || adError == AdError.AD_ALREADY_LOADED )
            {
                log( "Interstitial ad failed to load with error (" + maxAdapterError + ")" );
                listener.onInterstitialAdLoadFailed( maxAdapterError );
            }
            else if ( adError == AdError.AD_RUNTIME_ERROR || adError == AdError.AD_ALREADY_RENDERED )
            {
                log( "Interstitial ad failed to display with error (" + maxAdapterError + ")" );
                listener.onInterstitialAdDisplayFailed( maxAdapterError );
            }
        }
    }

    private class RewardedAdListener
            implements MobileFuseRewardedAd.Listener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        public RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdNotFilled()
        {
            log( "Rewarded ad failed to load - no fill" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onAdExpired()
        {
            log( "Rewarded ad expired" );
        }

        @Override
        public void onAdRendered()
        {
            log( "Rewarded ad shown" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onUserEarnedReward()
        {
            log( "Rewarded ad should grant reward" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed()
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

        @Override
        public void onAdError(final AdError adError)
        {
            MaxAdapterError maxAdapterError = toMaxError( adError );

            if ( adError == AdError.AD_LOAD_ERROR || adError == AdError.AD_ALREADY_LOADED )
            {
                log( "Rewarded ad failed to load with error (" + maxAdapterError + ")" );
                listener.onRewardedAdLoadFailed( maxAdapterError );
            }
            else if ( adError == AdError.AD_RUNTIME_ERROR || adError == AdError.AD_ALREADY_RENDERED )
            {
                log( "Rewarded ad failed to display with error (" + maxAdapterError + ")" );
                listener.onRewardedAdDisplayFailed( maxAdapterError );
            }
        }
    }

    private class AdViewAdListener
            implements MobileFuseBannerAd.Listener
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewAdListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "AdView ad loaded" );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onAdNotFilled()
        {
            log( "AdView ad failed to load: no fill" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onAdExpired()
        {
            log( "AdView ad expired" );
        }

        @Override
        public void onAdRendered()
        {
            log( "AdView ad displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "AdView ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdExpanded()
        {
            log( "AdView ad expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdCollapsed()
        {
            log( "AdView ad collapsed" );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdError(final AdError adError)
        {
            MaxAdapterError maxAdapterError = toMaxError( adError );

            if ( adError == AdError.AD_LOAD_ERROR || adError == AdError.AD_ALREADY_LOADED )
            {
                log( "AdView ad failed to load with error (" + maxAdapterError + ")" );
                listener.onAdViewAdLoadFailed( maxAdapterError );
            }
            else if ( adError == AdError.AD_RUNTIME_ERROR || adError == AdError.AD_ALREADY_RENDERED )
            {
                log( "AdView ad failed to display with error (" + maxAdapterError + ")" );
                listener.onAdViewAdDisplayFailed( maxAdapterError );
            }
        }
    }
}
