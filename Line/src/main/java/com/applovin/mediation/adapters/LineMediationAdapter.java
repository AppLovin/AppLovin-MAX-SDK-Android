package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.applovin.mediation.adapters.line.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.five_corp.ad.AdLoader;
import com.five_corp.ad.AdSlotConfig;
import com.five_corp.ad.BidData;
import com.five_corp.ad.FiveAd;
import com.five_corp.ad.FiveAdConfig;
import com.five_corp.ad.FiveAdCustomLayout;
import com.five_corp.ad.FiveAdCustomLayoutEventListener;
import com.five_corp.ad.FiveAdErrorCode;
import com.five_corp.ad.FiveAdInterstitial;
import com.five_corp.ad.FiveAdInterstitialEventListener;
import com.five_corp.ad.FiveAdNative;
import com.five_corp.ad.FiveAdNativeEventListener;
import com.five_corp.ad.FiveAdVideoReward;
import com.five_corp.ad.FiveAdVideoRewardEventListener;
import com.five_corp.ad.NeedGdprNonPersonalizedAdsTreatment;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LineMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private FiveAdInterstitial interstitialAd;
    private FiveAdVideoReward  rewardedAd;
    private FiveAdCustomLayout adView;
    private FiveAdNative       nativeAd;

    private InterstitialListener interstitialListener;
    private RewardedListener     rewardedListener;
    private AdViewListener       adViewListener;
    private NativeAdViewListener nativeAdViewListener;
    private NativeAdListener     nativeAdListener;

    public LineMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return FiveAd.getSdkSemanticVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public void onDestroy()
    {
        interstitialAd = null;
        rewardedAd = null;
        adView = null;
        nativeAd = null;

        interstitialListener = null;
        rewardedListener = null;
        adViewListener = null;
        nativeAdViewListener = null;
        nativeAdListener = null;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        final String adUnitId = parameters.getAdUnitId();
        if ( TextUtils.isEmpty( adUnitId ) )
        {
            final String errorMessage = "invalid ad unit id";
            log( "Signal collection failed with error: " + errorMessage );
            callback.onSignalCollectionFailed( errorMessage );

            return;
        }

        final Bundle credentials = BundleUtils.getBundle( "placement_ids", Bundle.EMPTY, parameters.getServerParameters() );
        final String slotId = credentials.getString( adUnitId );

        if ( TextUtils.isEmpty( slotId ) )
        {
            final String errorMessage = "invalid slot id";
            log( "Signal collection failed with error: " + errorMessage );
            callback.onSignalCollectionFailed( errorMessage );

            return;
        }

        final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
        adLoader.collectSignal( slotId, new AdLoader.CollectSignalCallback()
        {

            @Override
            public void onCollect(@NonNull final String token)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( token );
            }

            @Override
            public void onError(@NonNull final FiveAdErrorCode fiveAdErrorCode)
            {
                log( "Signal collection failed for " + slotId + " with error : " + fiveAdErrorCode );
                callback.onSignalCollectionFailed( fiveAdErrorCode.name() );
            }
        } );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + "interstitial ad for slot id: " + slotId + "..." );

        interstitialListener = new InterstitialListener( listener );

        if ( isBidding )
        {
            final BidData bidData = new BidData( bidResponse, null );
            final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
            adLoader.loadInterstitialAd( bidData, interstitialListener );
        }
        else
        {
            final AdSlotConfig slotConfig = new AdSlotConfig( slotId );
            final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
            adLoader.loadInterstitialAd( slotConfig, interstitialListener );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for slot id: " + slotId + "..." );

        if ( interstitialAd == null )
        {
            log( "Interstitial ad failed to show for slot id: " + slotId + " - no ad loaded" );

            final MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                               MaxAdapterError.AD_NOT_READY.getCode(),
                                                               MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onInterstitialAdDisplayFailed( error );
            return;
        }

        interstitialAd.setEventListener( interstitialListener );
        interstitialAd.showAd();
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + "rewarded ad for slot id: " + slotId + "..." );

        rewardedListener = new RewardedListener( listener );

        if ( isBidding )
        {
            final BidData bidData = new BidData( bidResponse, null );
            final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
            adLoader.loadRewardAd( bidData, rewardedListener );
        }
        else
        {
            final AdSlotConfig slotConfig = new AdSlotConfig( slotId );
            final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
            adLoader.loadRewardAd( slotConfig, rewardedListener );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for slot id: " + slotId + "..." );

        if ( rewardedAd == null )
        {
            log( "Rewarded ad failed to show for slot id: " + slotId + " - no ad loaded" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );

            return;
        }

        configureReward( parameters );

        rewardedAd.setEventListener( rewardedListener );
        rewardedAd.showAd();
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        final String slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for slot id: " + slotId + "..." );

        if ( isNative )
        {
            nativeAdViewListener = new NativeAdViewListener( listener, adFormat, parameters.getServerParameters() );

            if ( isBidding )
            {
                final BidData bidData = new BidData( bidResponse, null );
                final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
                adLoader.loadNativeAd( bidData, new DisplayMetrics().widthPixels, nativeAdViewListener );
            }
            else
            {
                final AdSlotConfig slotConfig = new AdSlotConfig( slotId );
                final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
                adLoader.loadNativeAd( slotConfig, new DisplayMetrics().widthPixels, nativeAdViewListener );
            }
        }
        else
        {
            adViewListener = new AdViewListener( listener, adFormat );

            if ( isBidding )
            {
                final BidData bidData = new BidData( bidResponse, null );
                final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
                adLoader.loadBannerAd( bidData, new DisplayMetrics().widthPixels, adViewListener );
            }
            else
            {
                final AdSlotConfig slotConfig = new AdSlotConfig( slotId );
                final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
                adLoader.loadBannerAd( slotConfig, new DisplayMetrics().widthPixels, adViewListener );
            }
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + "native ad for slot id: " + slotId + "..." );

        nativeAdListener = new NativeAdListener( listener, parameters.getServerParameters() );

        if ( isBidding )
        {
            final BidData bidData = new BidData( bidResponse, null );
            final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
            adLoader.loadNativeAd( bidData, nativeAdListener );
        }
        else
        {
            final AdSlotConfig slotConfig = new AdSlotConfig( slotId );
            final AdLoader adLoader = retrieveAdLoader( parameters, getContext( activity ) );
            adLoader.loadNativeAd( slotConfig, new DisplayMetrics().widthPixels, nativeAdListener );
        }
    }

    private Context getContext(@Nullable final Activity activity)
    {
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private AdLoader retrieveAdLoader(final MaxAdapterParameters parameters, final Context context)
    {
        final FiveAdConfig config = getConfigFromParameters( parameters );
        final AdLoader adLoader = AdLoader.forConfig( context, config );
        if ( adLoader == null )
        {
            throw new IllegalStateException( "Failed to retrieve ad loader for ad unit id: " + parameters.getAdUnitId() );
        }

        return adLoader;
    }

    private FiveAdConfig getConfigFromParameters(MaxAdapterParameters parameters)
    {
        final String appId = parameters.getServerParameters().getString( "app_id" );

        final FiveAdConfig config = new FiveAdConfig( appId );
        config.isTest = parameters.isTesting();

        updateMuteState( parameters.getServerParameters(), config );

        final Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            config.needGdprNonPersonalizedAdsTreatment = hasUserConsent ? NeedGdprNonPersonalizedAdsTreatment.FALSE : NeedGdprNonPersonalizedAdsTreatment.TRUE;
        }

        return config;
    }

    private static void updateMuteState(final Bundle serverParameters, final FiveAdConfig config)
    {
        if ( serverParameters.containsKey( "is_muted" ) )
        {
            config.enableSoundByDefault( !serverParameters.getBoolean( "is_muted" ) );
        }
    }

    private static MaxAdapterError toMaxError(FiveAdErrorCode lineAdsError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        String thirdPartySdkErrorMessage = "Unspecified.";
        switch ( lineAdsError )
        {
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                thirdPartySdkErrorMessage = "Please try again in a stable network environment.";
                break;
            case NO_AD:
                adapterError = MaxAdapterError.NO_FILL;
                thirdPartySdkErrorMessage = "Ad was not ready at display time. Please try again.";
                break;
            case BAD_APP_ID:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                thirdPartySdkErrorMessage = "Check if the OS type, PackageName, and issued AppID registered in FIVE Dashboard and the application settings match. Please be careful about blanks.";
                break;
            case STORAGE_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                thirdPartySdkErrorMessage = "There is a problem with the device storage. Please try again with another device.";
                break;
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                thirdPartySdkErrorMessage = "Unspecified.";
                break;
            case INVALID_STATE:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                thirdPartySdkErrorMessage = "There is a problem with the implementation. Please check the following. Whether the initialization process (FiveAd.initialize) is executed before the creation of the ad object or loadAdAsync. Are you calling loadAdAsync multiple times for one ad object?";
                break;
            case BAD_SLOT_ID:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                thirdPartySdkErrorMessage = "Make sure you are using the SlotID issued on the FIVE Dashboard.";
                break;
            case SUPPRESSED:
            case PLAYER_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                thirdPartySdkErrorMessage = "Unspecified.";
                break;
        }

        return new MaxAdapterError( adapterError, lineAdsError.ordinal(), thirdPartySdkErrorMessage );
    }

    private class InterstitialListener
            implements AdLoader.LoadInterstitialAdCallback, FiveAdInterstitialEventListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final FiveAdInterstitial ad)
        {
            log( "Interstitial ad loaded for slot id: " + ad.getSlotId() + "..." );
            interstitialAd = ad;
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onError(@NonNull final FiveAdErrorCode errorCode)
        {
            log( "Interstitial ad failed to load with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onInterstitialAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdInterstitial ad, final FiveAdErrorCode errorCode)
        {
            log( "Interstitial ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         errorCode.value,
                                                         "Please Contact Us" );
            listener.onInterstitialAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onFullScreenClose(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onFullScreenOpen(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad shown for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPlay(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad completed for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class RewardedListener
            implements AdLoader.LoadRewardAdCallback, FiveAdVideoRewardEventListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final FiveAdVideoReward ad)
        {
            log( "Rewarded ad loaded for slot id: " + ad.getSlotId() + "..." );
            rewardedAd = ad;
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onError(@NonNull final FiveAdErrorCode errorCode)
        {
            log( "Rewarded ad failed to load with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onRewardedAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdVideoReward ad, final FiveAdErrorCode errorCode)
        {
            log( "Rewarded ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         errorCode.value,
                                                         "Please Contact Us" );
            listener.onRewardedAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onFullScreenClose(final FiveAdVideoReward ad)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();

                log( "Rewarded ad user with reward: " + reward + " for slot id: " + ad.getSlotId() + "..." );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onFullScreenOpen(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad shown for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPlay(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onReward(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad granted reward for slot id: " + ad.getSlotId() );
            hasGrantedReward = true;
        }
    }

    private class AdViewListener
            implements AdLoader.LoadBannerAdCallback, FiveAdCustomLayoutEventListener
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;

        AdViewListener(final MaxAdViewAdapterListener listener, final MaxAdFormat adFormat)
        {
            this.listener = listener;
            this.adFormat = adFormat;
        }

        @Override
        public void onLoad(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad loaded for slot id: " + ad.getSlotId() + "..." );
            adView = ad;
            adView.setEventListener( adViewListener );

            // We always want to mute banners and MRECs
            adView.enableSound( false );

            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onError(final FiveAdErrorCode errorCode)
        {
            log( adFormat.getLabel() + " ad failed to load with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdCustomLayout ad, final FiveAdErrorCode errorCode)
        {
            log( adFormat.getLabel() + " ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         errorCode.value,
                                                         "Please Contact Us" );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onRemove(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onPlay(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad completed for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class NativeAdViewListener
            implements AdLoader.LoadNativeAdCallback, FiveAdNativeEventListener
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;
        private final Bundle                   serverParameters;

        NativeAdViewListener(final MaxAdViewAdapterListener listener, final MaxAdFormat adFormat, final Bundle serverParameters)
        {
            this.listener = listener;
            this.adFormat = adFormat;
            this.serverParameters = serverParameters;
        }

        @Override
        public void onLoad(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded for slot id: " + ad.getSlotId() + "..." );

            if ( nativeAd == null )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: no fill for slot id: " + ad.getSlotId() + "..." );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            nativeAd = ad;
            nativeAd.setEventListener( nativeAdViewListener );

            // We always want to mute banners and MRECs
            nativeAd.enableSound( false );

            renderCustomNativeBanner( ad.getSlotId() );
        }

        @Override
        public void onError(final FiveAdErrorCode errorCode)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to load with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdNative ad, final FiveAdErrorCode errorCode)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         errorCode.value,
                                                         "Please Contact Us" );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked for slot id: " + ad.getSlotId() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onRemove(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onPlay(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        private void renderCustomNativeBanner(final String slotId)
        {
            nativeAd.loadIconImageAsync( new FiveAdNative.LoadImageCallback()
            {
                @Override
                public void onImageLoad(final Bitmap bitmap)
                {
                    // Ensure UI rendering is done on UI thread
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
                            if ( nativeAd == null )
                            {
                                log( "Native " + adFormat.getLabel() + " ad destroyed before assets finished load for slot id: " + slotId );
                                return;
                            }

                            final MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                                    .setAdFormat( adFormat )
                                    .setTitle( nativeAd.getAdTitle() )
                                    .setBody( nativeAd.getDescriptionText() )
                                    .setCallToAction( nativeAd.getButtonText() )
                                    .setIcon( new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( getApplicationContext().getResources(), bitmap ) ) )
                                    .setMediaView( nativeAd.getAdMainView() )
                                    .build();

                            // Backend will pass down `vertical` as the template to indicate using a vertical native template
                            final String templateName = BundleUtils.getString( "template", "", serverParameters );

                            final MaxNativeAdView maxNativeAdView;
                            // Fallback case to be removed when backend sends down full template names for vertical native ads
                            if ( templateName.equals( "vertical" ) )
                            {
                                String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, verticalTemplateName, getApplicationContext() );
                            }
                            else
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
                            }

                            final List<View> clickableViews = new ArrayList<>( 5 );

                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getTitle() ) && maxNativeAdView.getTitleTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getTitleTextView() );
                            }
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getBody() ) && maxNativeAdView.getBodyTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getBodyTextView() );
                            }
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
                            {
                                clickableViews.add( maxNativeAdView.getCallToActionButton() );
                            }
                            if ( maxNativeAd.getIcon() != null && maxNativeAdView.getIconImageView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getIconImageView() );
                            }
                            final View mediaContentView = maxNativeAdView.getMediaContentViewGroup();
                            if ( maxNativeAd.getMediaView() != null && mediaContentView != null )
                            {
                                clickableViews.add( mediaContentView );
                            }

                            nativeAd.registerViews( maxNativeAdView, maxNativeAdView.getIconImageView(), clickableViews );
                            listener.onAdViewAdLoaded( maxNativeAdView );
                        }
                    } );
                }
            } );
        }
    }

    private class NativeAdListener
            implements AdLoader.LoadNativeAdCallback, FiveAdNativeEventListener
    {
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;

        NativeAdListener(final MaxNativeAdAdapterListener listener, final Bundle serverParameters)
        {
            this.listener = listener;
            this.serverParameters = serverParameters;
        }

        @Override
        public void onLoad(final FiveAdNative ad)
        {
            log( "Native ad loaded for slot id: " + ad.getSlotId() + "..." );

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( ad.getAdTitle() ) )
            {
                e( "Native ad (" + ad + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            ad.loadIconImageAsync( new FiveAdNative.LoadImageCallback()
            {
                @Override
                public void onImageLoad(final Bitmap bitmap)
                {
                    FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
                    if ( nativeAd == null )
                    {
                        log( "Native ad destroyed before assets finished load for slot id: " + ad.getSlotId() );
                        return;
                    }

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getAdTitle() )
                            .setAdvertiser( nativeAd.getAdvertiserName() )
                            .setBody( nativeAd.getDescriptionText() )
                            .setCallToAction( nativeAd.getButtonText() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( getApplicationContext().getResources(), bitmap ) ) )
                            .setMediaView( nativeAd.getAdMainView() );
                    MaxNativeAd maxNativeAd = new MaxLineNativeAd( builder );

                    listener.onNativeAdLoaded( maxNativeAd, null );
                }
            } );

            nativeAd = ad;
            nativeAd.setEventListener( nativeAdListener );

            // We always want to mute banners and MRECs
            nativeAd.enableSound( false );
        }

        @Override
        public void onError(final FiveAdErrorCode errorCode)
        {
            log( "Native ad failed to load with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onNativeAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdNative ad, final FiveAdErrorCode errorCode)
        {
            log( "Native ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
        }

        @Override
        public void onImpression(final FiveAdNative ad)
        {
            log( "Native ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onClick(final FiveAdNative ad)
        {
            log( "Native ad clicked for slot id: " + ad.getSlotId() );
            listener.onNativeAdClicked();
        }

        @Override
        public void onRemove(final FiveAdNative ad)
        {
            log( "Native ad hidden for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPlay(final FiveAdNative ad)
        {
            log( "Native ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdNative ad)
        {
            log( "Native ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdNative ad)
        {
            log( "Native ad completed for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class MaxLineNativeAd
            extends MaxNativeAd
    {
        private MaxLineNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            ImageView iconImageView = null;
            for ( final View clickableView : clickableViews )
            {
                if ( clickableView instanceof ImageView )
                {
                    iconImageView = (ImageView) clickableView;
                    break;
                }
            }

            nativeAd.registerViews( container, iconImageView, clickableViews );

            return true;
        }
    }
}
