package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.mobilefuse.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.mobilefuse.sdk.AdError;
import com.mobilefuse.sdk.MobileFuse;
import com.mobilefuse.sdk.MobileFuseBannerAd;
import com.mobilefuse.sdk.MobileFuseInterstitialAd;
import com.mobilefuse.sdk.MobileFuseNativeAd;
import com.mobilefuse.sdk.MobileFuseRewardedAd;
import com.mobilefuse.sdk.MobileFuseSettings;
import com.mobilefuse.sdk.SdkInitListener;
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenProvider;
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenRequest;
import com.mobilefuse.sdk.internal.TokenGeneratorListener;
import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MobileFuseMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

    private MobileFuseInterstitialAd interstitialAd;
    private MobileFuseRewardedAd     rewardedAd;
    private MobileFuseBannerAd       adView;
    private MobileFuseNativeAd       nativeAd;

    public MobileFuseMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            log( "Initializing MobileFuse SDK" );
            initializationStatus = InitializationStatus.INITIALIZING;

            MobileFuseSettings.setTestMode( parameters.isTesting() );
            MobileFuseSettings.setSdkAdapter( "applovin_bidding", getAdapterVersion() );

            MobileFuse.init( new SdkInitListener()
            {
                @Override
                public void onInitSuccess()
                {
                    log( "MobileFuse SDK initialized" );
                    initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( initializationStatus, null );
                }

                @Override
                public void onInitError()
                {
                    log( "MobileFuse SDK failed to initialize" );
                    initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( initializationStatus, null );
                }
            } );
        }
        else
        {
            onCompletionListener.onCompletion( initializationStatus, null );
        }
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

        if ( nativeAd != null )
        {
            nativeAd.unregisterViews();
            nativeAd.setAdListener( null );
            nativeAd = null;
        }
    }

    //region MAX Signal Provider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacyPreferences( parameters );

        MobileFuseBiddingTokenRequest tokenRequest = new MobileFuseBiddingTokenRequest( MobileFuse.getPrivacyPreferences(), parameters.isTesting() );
        MobileFuseBiddingTokenProvider.getToken( tokenRequest, getContext( activity ), new TokenGeneratorListener()
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

    //endregion

    //region MAX Interstitial Adapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading interstitial ad: " + placementId );

        updatePrivacyPreferences( parameters );

        interstitialAd = new MobileFuseInterstitialAd( getContext( activity ), placementId );
        interstitialAd.setListener( new InterstitialAdListener( listener ) );
        interstitialAd.loadAdFromBiddingToken( parameters.getBidResponse() );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
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

    //endregion

    //region MAX Rewarded Adapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded ad: " + placementId );

        updatePrivacyPreferences( parameters );

        rewardedAd = new MobileFuseRewardedAd( getContext( activity ), placementId );
        rewardedAd.setListener( new RewardedAdListener( listener ) );
        rewardedAd.loadAdFromBiddingToken( parameters.getBidResponse() );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( !rewardedAd.isLoaded() )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );

            return;
        }

        configureReward( parameters );

        rewardedAd.showAd();
    }

    //endregion

    //region MAX Ad View Adapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading " + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad: " + placementId );

        updatePrivacyPreferences( parameters );

        if ( isNative )
        {
            nativeAd = new MobileFuseNativeAd( getContext( activity ), placementId );
            nativeAd.setAdListener( new NativeAdViewListener( adFormat, parameters, listener ) );
            nativeAd.loadAdFromBiddingToken( parameters.getBidResponse() );
        }
        else
        {
            adView = new MobileFuseBannerAd( getContext( activity ), placementId, toAdSize( adFormat ) );
            adView.setListener( new AdViewAdListener( listener ) );
            adView.setAutorefreshEnabled( false );
            adView.setMuted( true );
            adView.loadAdFromBiddingToken( parameters.getBidResponse() );
        }
    }

    //endregion

    //region MAX Native Ad Adapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading native ad: " + placementId );

        updatePrivacyPreferences( parameters );

        nativeAd = new MobileFuseNativeAd( getContext( activity ), placementId );
        nativeAd.setAdListener( new NativeAdListener( parameters, listener ) );
        nativeAd.loadAdFromBiddingToken( parameters.getBidResponse() );
    }

    //endregion

    //region Helper Methods

    private MaxAdapterError toMaxError(final AdError mobileFuseError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        if ( mobileFuseError == null ) return adapterError;

        switch ( mobileFuseError )
        {
            case AD_ALREADY_LOADED:
            case AD_LOAD_ERROR:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case AD_ALREADY_RENDERED:
            case AD_RUNTIME_ERROR:
                adapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
        }

        return new MaxAdapterError( adapterError,
                                    mobileFuseError.getErrorCode(),
                                    mobileFuseError.getErrorMessage() );
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

        MobileFuse.setPrivacyPreferences( privacyPreferencesBuilder.build() );
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

    private List<View> getClickableViews(final MaxNativeAdView maxNativeAdView)
    {
        final List<View> clickableViews = new ArrayList<>( 6 );
        if ( maxNativeAdView.getTitleTextView() != null ) clickableViews.add( maxNativeAdView.getTitleTextView() );
        if ( maxNativeAdView.getAdvertiserTextView() != null ) clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
        if ( maxNativeAdView.getBodyTextView() != null ) clickableViews.add( maxNativeAdView.getBodyTextView() );
        if ( maxNativeAdView.getCallToActionButton() != null ) clickableViews.add( maxNativeAdView.getCallToActionButton() );
        if ( maxNativeAdView.getIconImageView() != null ) clickableViews.add( maxNativeAdView.getIconImageView() );
        if ( maxNativeAdView.getMediaContentViewGroup() != null ) clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );

        return clickableViews;
    }

    private Context getContext(@Nullable final Activity activity)
    {
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    //endregion

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
            log( "Interstitial ad displayed" );
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
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdError(final AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );

            if ( adError == AdError.AD_ALREADY_LOADED || adError == AdError.AD_LOAD_ERROR )
            {
                log( "Interstitial ad failed to load with error (" + adapterError + ")" );
                listener.onInterstitialAdLoadFailed( adapterError );
            }
            else
            {
                log( "Interstitial ad failed to display with error (" + adapterError + ")" );
                listener.onInterstitialAdDisplayFailed( adapterError );
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
            log( "Rewarded ad displayed" );
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
            log( "Rewarded ad hidden" );

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
            MaxAdapterError adapterError = toMaxError( adError );

            if ( adError == AdError.AD_ALREADY_LOADED || adError == AdError.AD_LOAD_ERROR )
            {
                log( "Rewarded ad failed to load with error (" + adapterError + ")" );
                listener.onRewardedAdLoadFailed( adapterError );
            }
            else
            {
                log( "Rewarded ad failed to display with error (" + adapterError + ")" );
                listener.onRewardedAdDisplayFailed( adapterError );
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
            log( "AdView ad failed to load - no fill" );
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
            MaxAdapterError adapterError = toMaxError( adError );

            if ( adError == AdError.AD_ALREADY_LOADED || adError == AdError.AD_LOAD_ERROR )
            {
                log( "AdView ad failed to load with error (" + adapterError + ")" );
                listener.onAdViewAdLoadFailed( adapterError );
            }
            else
            {
                log( "AdView ad failed to display with error (" + adapterError + ")" );
                listener.onAdViewAdDisplayFailed( adapterError );
            }
        }
    }

    private class NativeAdViewListener
            implements MobileFuseNativeAd.Listener
    {
        private final MaxAdFormat              adFormat;
        private final Bundle                   serverParameters;
        private final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdFormat adFormat, final MaxAdapterResponseParameters parameters, final MaxAdViewAdapterListener listener)
        {
            this.adFormat = adFormat;
            this.serverParameters = parameters.getServerParameters();
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            if ( nativeAd == null )
            {
                e( "Native " + adFormat.getLabel() + " ad is null" );
                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded" );

            if ( !nativeAd.hasTitle() )
            {
                e( "Native " + adFormat.getLabel() + " ad (" + nativeAd + ") does not have required assets." );
                listener.onAdViewAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( adFormat )
                    .setTitle( nativeAd.getTitle() )
                    .setBody( nativeAd.getDescriptionText() )
                    .setAdvertiser( nativeAd.getSponsoredText() )
                    .setCallToAction( nativeAd.getCtaButtonText() )
                    .setIconView( nativeAd.getIconView() )
                    .setMediaView( nativeAd.getMainContentView() );

            final MaxMobileFuseNativeAd maxMobileFuseNativeAd = new MaxMobileFuseNativeAd( builder );

            MaxNativeAdView maxNativeAdView;
            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            if ( templateName.equals( "vertical" ) )
            {
                String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                maxNativeAdView = new MaxNativeAdView( maxMobileFuseNativeAd, verticalTemplateName, getApplicationContext() );
            }
            else
            {
                maxNativeAdView = new MaxNativeAdView( maxMobileFuseNativeAd,
                                                       AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                       getApplicationContext() );
            }

            maxMobileFuseNativeAd.prepareForInteraction( getClickableViews( maxNativeAdView ), maxNativeAdView );
            listener.onAdViewAdLoaded( maxNativeAdView );
        }

        @Override
        public void onAdNotFilled()
        {
            log( "Native " + adFormat.getLabel() + " ad failed to load - no fill" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onAdExpired()
        {
            log( "Native " + adFormat.getLabel() + " ad expired" );
        }

        @Override
        public void onAdRendered()
        {
            log( "Native " + adFormat.getLabel() + " ad displayed" );
            listener.onAdViewAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native " + adFormat.getLabel() + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdError(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );

            if ( adError == AdError.AD_ALREADY_LOADED || adError == AdError.AD_LOAD_ERROR )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load with error (" + adapterError + ")" );
                listener.onAdViewAdLoadFailed( adapterError );
            }
            else
            {
                log( "Native " + adFormat.getLabel() + " ad failed to display with error (" + adapterError + ")" );
                listener.onAdViewAdDisplayFailed( adapterError );
            }
        }
    }

    private class NativeAdListener
            implements MobileFuseNativeAd.Listener
    {
        private final Bundle                     serverParameters;
        private final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final MaxAdapterResponseParameters parameters, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = parameters.getServerParameters();
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            if ( nativeAd == null )
            {
                e( "Native ad is null" );
                return;
            }

            log( "Native ad loaded" );

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );

            if ( isTemplateAd && !nativeAd.hasTitle() )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( nativeAd.getTitle() )
                    .setAdvertiser( nativeAd.getSponsoredText() )
                    .setBody( nativeAd.getDescriptionText() )
                    .setCallToAction( nativeAd.getCtaButtonText() )
                    .setIcon( new MaxNativeAd.MaxNativeAdImage( nativeAd.getIconDrawable() ) )
                    .setMediaView( nativeAd.getMainContentView() );

            final MaxNativeAd maxNativeAd = new MaxMobileFuseNativeAd( builder );
            listener.onNativeAdLoaded( maxNativeAd, null );
        }

        @Override
        public void onAdNotFilled()
        {
            log( "Native ad failed to load - no fill" );
            listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onAdExpired()
        {
            log( "Native ad expired" );
        }

        @Override
        public void onAdRendered()
        {
            log( "Native ad displayed" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdError(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );

            if ( adError == AdError.AD_ALREADY_LOADED || adError == AdError.AD_LOAD_ERROR )
            {
                log( "Native ad failed to load with error (" + adapterError + ")" );
            }
            else
            {
                log( "Native ad failed to display with error (" + adapterError + ")" );
            }

            // possible display error for native ads also handled by load error callback
            listener.onNativeAdLoadFailed( adapterError );
        }
    }

    private class MaxMobileFuseNativeAd
            extends MaxNativeAd
    {
        public MaxMobileFuseNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final MobileFuseNativeAd nativeAd = MobileFuseMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view: native ad is null." );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            nativeAd.registerViewForInteraction( container, clickableViews );
            return true;
        }
    }
}
