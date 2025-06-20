package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.applovin.mediation.adapter.listeners.MaxAppOpenAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.vungle.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BaseAd;
import com.vungle.ads.BidTokenCallback;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.NativeAd;
import com.vungle.ads.NativeAdListener;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleAdSize;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleBannerView;
import com.vungle.ads.VungleError;
import com.vungle.ads.VunglePrivacySettings;
import com.vungle.ads.VungleWrapperFramework;
import com.vungle.ads.internal.protos.Sdk.SDKError.Reason;
import com.vungle.ads.internal.ui.view.MediaView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VungleMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, /* MaxAppOpenAdapter */ MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

    private VungleBannerView adViewAd;
    private InterstitialAd   interstitialAd;
    private RewardedAd       rewardedAd;
    private NativeAd         nativeAd;
    private InterstitialAd   appOpenAd;

    // Explicit default constructor declaration
    public VungleMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        updateUserPrivacySettings( parameters );

        if ( initialized.compareAndSet( false, true ) )
        {
            String appId = parameters.getServerParameters().getString( "app_id", null );
            log( "Initializing Vungle SDK with app id: " + appId + "..." );

            initializationStatus = InitializationStatus.INITIALIZING;

            VungleAds.setIntegrationName( VungleWrapperFramework.max, getAdapterVersion() );

            // Note: Vungle requires the Application Context
            VungleAds.init( getContext( activity ), appId, new InitializationListener()
            {
                @Override
                public void onSuccess()
                {
                    log( "Vungle SDK initialized" );

                    initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( initializationStatus, null );
                }

                @Override
                public void onError(@NonNull final VungleError vungleError)
                {
                    initialized.set( false );

                    log( "Vungle SDK failed to initialize with error: ", vungleError );

                    initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( initializationStatus, vungleError.getErrorMessage() );
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
        return getVersionString( com.vungle.ads.BuildConfig.class, "VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( adViewAd != null )
        {
            adViewAd.setAdListener( null );
            adViewAd.finishAd();
            adViewAd = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.setAdListener( null );
            nativeAd.unregisterView();
            nativeAd = null;
        }

        if ( interstitialAd != null )
        {
            interstitialAd.setAdListener( null );
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setAdListener( null );
            rewardedAd = null;
        }

        if ( appOpenAd != null )
        {
            appOpenAd.setAdListener( null );
            appOpenAd = null;
        }
    }

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserPrivacySettings( parameters );

        VungleAds.getBiddingToken( getContext( activity ), new BidTokenCallback()
        {
            @Override
            public void onBidTokenCollected(@NonNull final String bidToken)
            {
                callback.onSignalCollected( bidToken );
            }

            @Override
            public void onBidTokenError(@NonNull final String errorMessage)
            {
                log( "Signal collection failed: " + errorMessage );
                callback.onSignalCollectionFailed( errorMessage );
            }
        } );
    }

    //endregion

    //region MaxInterstitialAdapter

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        if ( shouldFailAdLoadWhenSdkNotInitialized( parameters ) && !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        interstitialAd = new InterstitialAd( getContext( activity ), placementId, new AdConfig() );
        interstitialAd.setAdListener( new InterstitialListener( listener ) );

        interstitialAd.load( bidResponse );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        if ( interstitialAd != null && interstitialAd.canPlayAd() )
        {
            log( "Showing interstitial ad for placement: " + parameters.getThirdPartyAdPlacementId() + "..." );
            interstitialAd.play( getContext( activity ) );
        }
        else
        {
            log( "Interstitial ad is not ready: " + parameters.getThirdPartyAdPlacementId() + "..." );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                                         MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    //endregion

    //region MaxAppOpenAdapter

    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, @NonNull final MaxAppOpenAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "app open ad for placement: " + placementId + "..." );

        if ( shouldFailAdLoadWhenSdkNotInitialized( parameters ) && !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing app open ad load..." );
            listener.onAppOpenAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        appOpenAd = new InterstitialAd( getContext( activity ), placementId, new AdConfig() );
        appOpenAd.setAdListener( new AppOpenAdListener( listener ) );

        appOpenAd.load( bidResponse );
    }

    public void showAppOpenAd(@NonNull final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, @NonNull final MaxAppOpenAdapterListener listener)
    {
        if ( appOpenAd != null && appOpenAd.canPlayAd() )
        {
            log( "Showing app open ad for placement: " + parameters.getThirdPartyAdPlacementId() + "..." );
            appOpenAd.play( getContext( activity ) );
        }
        else
        {
            log( "App open ad is not ready: " + parameters.getThirdPartyAdPlacementId() + "..." );
            listener.onAppOpenAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                    MaxAdapterError.AD_NOT_READY.getCode(),
                                                                    MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );

        if ( shouldFailAdLoadWhenSdkNotInitialized( parameters ) && !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        rewardedAd = new RewardedAd( getContext( activity ), placementId, new AdConfig() );
        rewardedAd.setAdListener( new RewardedListener( listener ) );

        rewardedAd.load( bidResponse );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        if ( rewardedAd != null && rewardedAd.canPlayAd() )
        {
            log( "Showing rewarded ad for placement: " + parameters.getThirdPartyAdPlacementId() + "..." );

            configureReward( parameters );
            rewardedAd.play( getContext( activity ) );
        }
        else
        {
            log( "Rewarded ad is not ready: " + parameters.getThirdPartyAdPlacementId() + "..." );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String adFormatLabel = adFormat.getLabel();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final Context context = getContext( activity );

        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        if ( shouldFailAdLoadWhenSdkNotInitialized( parameters ) && !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing " + adFormatLabel + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        if ( isNative )
        {
            final NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( parameters, adFormat, context, listener );
            nativeAd = new NativeAd( getContext( activity ), placementId );
            nativeAd.setAdListener( nativeAdViewListener );

            nativeAd.load( bidResponse );

            return;
        }

        // Check if adaptive ad view sizes should be used
        boolean isAdaptiveAdViewEnabled = isAdaptiveAdViewEnabled( parameters );
        if ( isAdaptiveAdViewEnabled && AppLovinSdk.VERSION_CODE < 13_02_00_99 )
        {
            isAdaptiveAdViewEnabled = false;
            userError( "Please update AppLovin MAX SDK to version 13.2.0 or higher in order to use Vungle adaptive ads" );
        }

        VungleAdSize adSize = toVungleAdSize( adFormat, isAdaptiveAdViewEnabled, parameters, context );
        adViewAd = new VungleBannerView( context, placementId, adSize );
        adViewAd.setAdListener( new AdViewAdListener( adFormatLabel, listener ) );

        adViewAd.load( bidResponse );
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        if ( shouldFailAdLoadWhenSdkNotInitialized( parameters ) && !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing interstitial ad load..." );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        nativeAd = new NativeAd( getContext( activity ), placementId );
        nativeAd.setAdListener( new NativeListener( parameters, getContext( activity ), listener ) );

        nativeAd.load( bidResponse );
    }

    //endregion

    //region Helper Methods

    private boolean shouldFailAdLoadWhenSdkNotInitialized(final MaxAdapterResponseParameters parameters)
    {
        return parameters.getServerParameters().getBoolean( "fail_ad_load_when_sdk_not_initialized", true );
    }

    private boolean isAdaptiveAdViewEnabled(final MaxAdapterResponseParameters parameters)
    {
        if ( !parameters.getServerParameters().getBoolean( "adaptive_banner", false ) ) return false;

        if ( VungleAds.isInline( parameters.getThirdPartyAdPlacementId() ) )
        {
            return true;
        }
        else
        {
            userError( "Please use a Vungle inline placement ID in order to use Vungle adaptive ads" );
            return false;
        }
    }

    private void updateUserPrivacySettings(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            VunglePrivacySettings.setGDPRStatus( hasUserConsent, "" );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            VunglePrivacySettings.setCCPAStatus( !isDoNotSell );
        }
    }

    private VungleAdSize toVungleAdSize(final MaxAdFormat adFormat,
                                        final boolean isAdaptiveAdViewEnabled,
                                        final MaxAdapterParameters parameters,
                                        final Context context)
    {
        if ( isAdaptiveAdViewEnabled && isAdaptiveAdViewFormat( adFormat, parameters ) )
        {
            return getAdaptiveAdSize( parameters, context );
        }

        if ( adFormat == MaxAdFormat.BANNER )
        {
            return VungleAdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return VungleAdSize.BANNER_LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return VungleAdSize.MREC;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad view ad format: " + adFormat.getLabel() );
        }
    }

    private VungleAdSize getAdaptiveAdSize(final MaxAdapterParameters parameters, final Context context)
    {
        final int adaptiveAdWidth = getAdaptiveAdViewWidth( parameters, context );

        if ( isInlineAdaptiveAdView( parameters ) )
        {
            final int inlineMaximumHeight = getInlineAdaptiveAdViewMaximumHeight( parameters );
            if ( inlineMaximumHeight > 0 )
            {
                // NOTE: Inline adaptive ad will be a fixed height equal to inlineMaximumHeight. Dynamic maximum height will be supported once the Vungle iOS SDK respects the maximum height
                return VungleAdSize.getAdSizeWithWidthAndHeight( adaptiveAdWidth, inlineMaximumHeight );
            }

            // If not specified, inline maximum height will be the device height according to current device orientation
            return VungleAdSize.getAdSizeWithWidth( context, adaptiveAdWidth );
        }

        // Return anchored size by default
        final int anchoredHeight = MaxAdFormat.BANNER.getAdaptiveSize( adaptiveAdWidth, context ).getHeight();
        return VungleAdSize.getAdSizeWithWidthAndHeight( adaptiveAdWidth, anchoredHeight );
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private List<View> getClickableViews(final MaxNativeAdView maxNativeAdView)
    {
        final List<View> clickableViews = new ArrayList<>( 7 );
        if ( maxNativeAdView.getTitleTextView() != null ) clickableViews.add( maxNativeAdView.getTitleTextView() );
        if ( maxNativeAdView.getAdvertiserTextView() != null ) clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
        if ( maxNativeAdView.getBodyTextView() != null ) clickableViews.add( maxNativeAdView.getBodyTextView() );
        if ( maxNativeAdView.getCallToActionButton() != null ) clickableViews.add( maxNativeAdView.getCallToActionButton() );
        if ( maxNativeAdView.getIconImageView() != null ) clickableViews.add( maxNativeAdView.getIconImageView() );
        if ( maxNativeAdView.getMediaContentViewGroup() != null ) clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );

        return clickableViews;
    }

    private static MaxAdapterError toMaxError(final VungleError vungleError, final boolean isAdPresentError)
    {
        final int vungleErrorCode = vungleError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( vungleErrorCode )
        {
            case Reason.SDK_NOT_INITIALIZED_VALUE:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case Reason.INVALID_APP_ID_VALUE:
            case Reason.INVALID_PLACEMENT_ID_VALUE:
            case Reason.PLACEMENT_AD_TYPE_MISMATCH_VALUE:
            case Reason.INVALID_WATERFALL_PLACEMENT_ID_VALUE:
            case Reason.BANNER_VIEW_INVALID_SIZE_VALUE:
            case Reason.AD_PUBLISHER_MISMATCH_VALUE:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case Reason.INVALID_PLAY_PARAMETER_VALUE:
                adapterError = MaxAdapterError.MISSING_ACTIVITY;
                break;
            case Reason.JSON_ENCODE_ERROR_VALUE:
            case Reason.AD_INTERNAL_INTEGRATION_ERROR_VALUE:
            case Reason.INVALID_REQUEST_BUILDER_ERROR_VALUE:
            case Reason.JSON_PARAMS_ENCODE_ERROR_VALUE:
            case Reason.GENERATE_JSON_DATA_ERROR_VALUE:
            case Reason.CONFIG_NOT_FOUND_ERROR_VALUE:
            case Reason.TEMPLATE_UNZIP_ERROR_VALUE:
            case Reason.ASSET_WRITE_ERROR_VALUE:
            case Reason.GZIP_ENCODE_ERROR_VALUE:
            case Reason.PROTOBUF_SERIALIZATION_ERROR_VALUE:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case Reason.AD_CONSUMED_VALUE:
            case Reason.AD_IS_LOADING_VALUE:
            case Reason.AD_ALREADY_LOADED_VALUE:
            case Reason.AD_IS_PLAYING_VALUE:
            case Reason.AD_ALREADY_FAILED_VALUE:
            case Reason.INVALID_BID_PAYLOAD_VALUE:
            case Reason.INVALID_JSON_BID_PAYLOAD_VALUE:
            case Reason.INVALID_GZIP_BID_PAYLOAD_VALUE:
            case Reason.AD_RESPONSE_EMPTY_VALUE:
            case Reason.AD_RESPONSE_INVALID_TEMPLATE_TYPE_VALUE:
            case Reason.STALE_CACHED_RESPONSE_VALUE:
            case Reason.API_REQUEST_ERROR_VALUE:
            case Reason.API_RESPONSE_DATA_ERROR_VALUE:
            case Reason.API_RESPONSE_DECODE_ERROR_VALUE:
            case Reason.API_FAILED_STATUS_CODE_VALUE:
            case Reason.INVALID_TEMPLATE_URL_VALUE:
            case Reason.INVALID_ASSET_URL_VALUE:
            case Reason.ASSET_REQUEST_ERROR_VALUE:
            case Reason.ASSET_RESPONSE_DATA_ERROR_VALUE:
            case Reason.INVALID_EVENT_ID_ERROR_VALUE:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case Reason.AD_NOT_LOADED_VALUE:
                adapterError = isAdPresentError ? MaxAdapterError.AD_NOT_READY : MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case Reason.INVALID_INDEX_URL_VALUE:
            case Reason.INVALID_IFA_STATUS_VALUE:
            case Reason.MRAID_BRIDGE_ERROR_VALUE:
            case Reason.CONCURRENT_PLAYBACK_UNSUPPORTED_VALUE:
            case Reason.AD_CLOSED_TEMPLATE_ERROR_VALUE:
            case Reason.AD_CLOSED_MISSING_HEARTBEAT_VALUE:
                adapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
            case Reason.PLACEMENT_SLEEP_VALUE:
            case Reason.AD_NO_FILL_VALUE:
            case Reason.AD_LOAD_TOO_FREQUENTLY_VALUE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case Reason.AD_RESPONSE_TIMED_OUT_VALUE:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case Reason.AD_RESPONSE_RETRY_AFTER_VALUE:
            case Reason.AD_LOAD_FAIL_RETRY_AFTER_VALUE:
            case Reason.AD_SERVER_ERROR_VALUE:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case Reason.AD_EXPIRED_VALUE:
            case Reason.AD_EXPIRED_ON_PLAY_VALUE:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case Reason.NATIVE_ASSET_ERROR_VALUE:
                adapterError = MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS;
                break;
            case Reason.WEB_VIEW_WEB_CONTENT_PROCESS_DID_TERMINATE_VALUE:
            case Reason.WEB_VIEW_FAILED_NAVIGATION_VALUE:
            case Reason.WEBVIEW_ERROR_VALUE:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError, vungleErrorCode, vungleError.getLocalizedMessage() );
    }

    private Bundle maybeCreateExtraInfoBundle(final BaseAd baseAd)
    {
        Bundle extraInfo = new Bundle( 3 );

        String creativeId = baseAd.getCreativeId();
        if ( AppLovinSdkUtils.isValidString( creativeId ) )
        {
            extraInfo.putString( "creative_id", creativeId );
        }

        if ( adViewAd != null )
        {
            VungleAdSize adSize = adViewAd.getAdViewSize();
            extraInfo.putInt( "ad_width", adSize.getWidth() );
            extraInfo.putInt( "ad_height", adSize.getHeight() );
        }

        return extraInfo;
    }

    //endregion

    private class InterstitialListener
            implements InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BaseAd baseAd)
        {
            log( "Interstitial ad loaded" );

            Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
            listener.onInterstitialAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError, false );
            log( "Interstitial ad failed to load with error: " + error );
            listener.onInterstitialAdLoadFailed( error );
        }

        @Override
        public void onAdStart(@NonNull final BaseAd baseAd)
        {
            log( "Interstitial ad started" );
        }

        @Override
        public void onAdImpression(@NonNull final BaseAd baseAd)
        {
            log( "Interstitial ad displayed" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(@NonNull final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         vungleError.getCode(),
                                                         vungleError.getErrorMessage() );
            log( "Interstitial ad failed to display with error: " + error );
            listener.onInterstitialAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(@NonNull final BaseAd baseAd)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdLeftApplication(@NonNull final BaseAd baseAd)
        {
            log( "Interstitial ad left application" );
        }

        @Override
        public void onAdEnd(@NonNull final BaseAd baseAd)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }
    }

    private class AppOpenAdListener
            implements InterstitialAdListener
    {
        private final MaxAppOpenAdapterListener listener;

        AppOpenAdListener(final MaxAppOpenAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BaseAd baseAd)
        {
            log( "App Open ad loaded" );

            Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
            listener.onAppOpenAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError, false );
            log( "App Open ad failed to load with error: " + error );
            listener.onAppOpenAdLoadFailed( error );
        }

        @Override
        public void onAdStart(@NonNull final BaseAd baseAd)
        {
            log( "App Open ad started" );
        }

        @Override
        public void onAdImpression(@NonNull final BaseAd baseAd)
        {
            log( "App Open ad displayed" );
            listener.onAppOpenAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(@NonNull final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         vungleError.getCode(),
                                                         vungleError.getErrorMessage() );
            log( "App Open ad failed to display with error: " + error );
            listener.onAppOpenAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(@NonNull final BaseAd baseAd)
        {
            log( "App Open ad clicked" );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdLeftApplication(@NonNull final BaseAd baseAd)
        {
            log( "App Open ad left application" );
        }

        @Override
        public void onAdEnd(@NonNull final BaseAd baseAd)
        {
            log( "App Open ad hidden" );
            listener.onAppOpenAdHidden();
        }
    }

    private class RewardedListener
            implements RewardedAdListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BaseAd baseAd)
        {
            log( "Rewarded ad loaded" );

            Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
            listener.onRewardedAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError, false );
            log( "Rewarded ad failed to load with error: " + error );
            listener.onRewardedAdLoadFailed( error );
        }

        @Override
        public void onAdStart(@NonNull final BaseAd baseAd)
        {
            log( "Rewarded ad started" );
        }

        @Override
        public void onAdImpression(@NonNull final BaseAd baseAd)
        {
            log( "Rewarded ad displayed" );

            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(@NonNull final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         vungleError.getCode(),
                                                         vungleError.getErrorMessage() );
            log( "Rewarded ad failed to display with error: " + error );
            listener.onRewardedAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(@NonNull final BaseAd baseAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdLeftApplication(@NonNull final BaseAd baseAd)
        {
            log( "Rewarded ad left application" );
        }

        @Override
        public void onAdEnd(@NonNull final BaseAd baseAd)
        {
            log( "Rewarded ad video completed" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onAdRewarded(@NonNull final BaseAd baseAd)
        {
            log( "User was rewarded" );
            hasGrantedReward = true;
        }
    }

    private class AdViewAdListener
            implements BannerAdListener
    {
        private final String                   adFormatLabel;
        private final MaxAdViewAdapterListener listener;

        AdViewAdListener(final String adFormatLabel, final MaxAdViewAdapterListener listener)
        {
            this.adFormatLabel = adFormatLabel;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd baseAd)
        {
            log( "Showing " + adFormatLabel + " ad for placement: " + baseAd.getPlacementId() + "..." );

            if ( adViewAd != null )
            {
                adViewAd.setGravity( Gravity.CENTER );
                log( adFormatLabel + " ad loaded" );

                Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
                listener.onAdViewAdLoaded( adViewAd, extraInfo );
            }
            else
            {
                MaxAdapterError error = MaxAdapterError.INVALID_LOAD_STATE;
                log( adFormatLabel + " ad failed to load: " + error );
                listener.onAdViewAdLoadFailed( error );
            }
        }

        @Override
        public void onAdFailedToLoad(@NonNull final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError, false );
            log( adFormatLabel + " ad failed to load with error: " + error );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onAdStart(@NonNull final BaseAd baseAd)
        {
            log( adFormatLabel + " ad started" );
        }

        @Override
        public void onAdImpression(@NonNull final BaseAd baseAd)
        {
            log( adFormatLabel + " ad displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(@NonNull final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         vungleError.getCode(),
                                                         vungleError.getErrorMessage() );
            log( adFormatLabel + " ad display failed with error: " + error );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(@NonNull final BaseAd baseAd)
        {
            log( adFormatLabel + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(@NonNull final BaseAd baseAd)
        {
            log( adFormatLabel + " ad left application" );
        }

        @Override
        public void onAdEnd(@NonNull final BaseAd baseAd)
        {
            log( adFormatLabel + " ad hidden" );
            listener.onAdViewAdHidden();
        }
    }

    private class NativeAdViewListener
            implements NativeAdListener
    {
        private final Context                  applicationContext;
        private final Bundle                   serverParameters;
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Context applicationContext, final MaxAdViewAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.adFormat = adFormat;
            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BaseAd ad)
        {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `ad` could be null if the user cannot get fill.
            if ( nativeAd == null || nativeAd != ad )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: no fill" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded: " + nativeAd.getPlacementId() );

            final MediaView mediaView = new MediaView( applicationContext );
            final String iconUrl = nativeAd.getAppIcon();

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( adFormat )
                    .setTitle( nativeAd.getAdTitle() )
                    .setAdvertiser( nativeAd.getAdSponsoredText() )
                    .setBody( nativeAd.getAdBodyText() )
                    .setCallToAction( nativeAd.getAdCallToActionText() )
                    .setIcon( new MaxNativeAd.MaxNativeAdImage( Uri.parse( iconUrl ) ) )
                    .setMediaContentAspectRatio( nativeAd.getMediaAspectRatio() )
                    .setMediaView( mediaView );

            final MaxVungleNativeAd maxVungleNativeAd = new MaxVungleNativeAd( builder );

            // Backend will pass down `vertical` as the template to indicate using a vertical native template
            final MaxNativeAdView maxNativeAdView;
            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            if ( templateName.contains( "vertical" ) )
            {
                if ( "vertical".equals( templateName ) )
                {
                    final String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                    maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd, verticalTemplateName, applicationContext );
                }
                else
                {
                    maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd, templateName, applicationContext );
                }
            }
            else
            {
                maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd,
                                                       AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                       applicationContext );
            }

            maxVungleNativeAd.prepareForInteraction( getClickableViews( maxNativeAdView ), maxNativeAdView );

            Bundle extraInfo = maybeCreateExtraInfoBundle( ad );
            listener.onAdViewAdLoaded( maxNativeAdView, extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            MaxAdapterError adapterError = toMaxError( vungleError, false );
            log( "Native " + adFormat.getLabel() + " ad failed to load with error " + adapterError + " with placement id: " + baseAd.getPlacementId() );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad start with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad shown with placement id: " + baseAd.getPlacementId() );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to play with error " + toMaxError( vungleError, true ) + " with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked with placement id: " + baseAd.getPlacementId() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad left application with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad end with placement id: " + baseAd.getPlacementId() );
        }
    }

    private class NativeListener
            implements NativeAdListener
    {
        private final Context                    applicationContext;
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;

        NativeListener(final MaxAdapterResponseParameters parameters, final Context applicationContext, final MaxNativeAdAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BaseAd ad)
        {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `ad` could be null if the user cannot get fill.
            if ( nativeAd == null || nativeAd != ad )
            {
                log( "Native ad failed to load: no fill" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getAdTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            log( "Native ad loaded: " + nativeAd.getPlacementId() );

            final MediaView mediaView = new MediaView( applicationContext );
            final String iconUrl = nativeAd.getAppIcon();

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( nativeAd.getAdTitle() )
                    .setAdvertiser( nativeAd.getAdSponsoredText() )
                    .setBody( nativeAd.getAdBodyText() )
                    .setCallToAction( nativeAd.getAdCallToActionText() )
                    .setIcon( new MaxNativeAd.MaxNativeAdImage( Uri.parse( iconUrl ) ) )
                    .setMediaContentAspectRatio( nativeAd.getMediaAspectRatio() )
                    .setMediaView( mediaView );

            final MaxVungleNativeAd maxVungleNativeAd = new MaxVungleNativeAd( builder );

            Bundle extraInfo = maybeCreateExtraInfoBundle( ad );
            listener.onNativeAdLoaded( maxVungleNativeAd, extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            MaxAdapterError adapterError = toMaxError( vungleError, false );
            log( "Native ad failed to load with error " + adapterError + " with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "Native ad start with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "Native ad shown with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, @NonNull final VungleError vungleError)
        {
            log( "Native ad failed to play with error " + toMaxError( vungleError, true ) + " with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "Native ad clicked with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "Native ad left application with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "Native ad end with placement id: " + baseAd.getPlacementId() );
        }
    }

    //endregion

    private class MaxVungleNativeAd
            extends MaxNativeAd
    {
        public MaxVungleNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAd nativeAd = VungleMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            if ( !nativeAd.canPlayAd() )
            {
                e( "Failed to play native ad or native ad is registered." );
                return false;
            }

            View mediaView = getMediaView();
            if ( mediaView == null )
            {
                e( "Failed to register native ad views: mediaView is null." );
                return false;
            }

            // mediaView needs to be in the clickableViews for the mediaView to be clickable even though it is only a container of the network's media view.
            clickableViews.add( mediaView );

            // Native integrations
            if ( container instanceof MaxNativeAdView )
            {
                if ( mediaView.getParent() != null )
                {
                    ( (ViewGroup) mediaView.getParent() ).removeView( mediaView );
                }

                MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;

                ViewGroup contentViewGroup = maxNativeAdView.getMediaContentViewGroup();
                if ( contentViewGroup != null )
                {
                    contentViewGroup.removeAllViews();
                    contentViewGroup.addView( mediaView );
                }

                nativeAd.registerViewForInteraction( maxNativeAdView, (MediaView) mediaView, maxNativeAdView.getIconImageView(), clickableViews );
            }
            // Plugins
            else
            {
                FrameLayout frameLayout = new FrameLayout( container.getContext() );
                container.addView( frameLayout, 0, new FrameLayout.LayoutParams( FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT ) );

                ImageView iconImageView = null;
                for ( final View clickableView : clickableViews )
                {
                    if ( clickableView instanceof ImageView )
                    {
                        iconImageView = (ImageView) clickableView;
                        break;
                    }
                }

                nativeAd.registerViewForInteraction( frameLayout, (MediaView) mediaView, iconImageView, clickableViews );
            }

            return true;
        }
    }
}
