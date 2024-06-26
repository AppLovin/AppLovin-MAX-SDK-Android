package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.impl.sdk.utils.ImageViewUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
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
import com.applovin.mediation.adapters.moloco.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.moloco.sdk.publisher.AdLoad;
import com.moloco.sdk.publisher.Banner;
import com.moloco.sdk.publisher.BannerAdShowListener;
import com.moloco.sdk.publisher.Initialization;
import com.moloco.sdk.publisher.InterstitialAd;
import com.moloco.sdk.publisher.InterstitialAdShowListener;
import com.moloco.sdk.publisher.MediationInfo;
import com.moloco.sdk.publisher.Moloco;
import com.moloco.sdk.publisher.MolocoAd;
import com.moloco.sdk.publisher.MolocoAdError;
import com.moloco.sdk.publisher.NativeAdForMediation;
import com.moloco.sdk.publisher.RewardedInterstitialAd;
import com.moloco.sdk.publisher.RewardedInterstitialAdShowListener;
import com.moloco.sdk.publisher.init.MolocoInitParams;
import com.moloco.sdk.publisher.privacy.MolocoPrivacy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MolocoMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

    private InterstitialAd         interstitialAd;
    private RewardedInterstitialAd rewardedAd;
    private Banner                 adView;
    private NativeAdForMediation   nativeAd;

    private InterstitialAdListener interstitialAdListener;
    private RewardedAdListener     rewardedAdListener;

    public MolocoMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            initializationStatus = InitializationStatus.INITIALIZING;

            final Bundle serverParameters = parameters.getServerParameters();

            final String appKey = serverParameters.getString( "app_key" );
            final MediationInfo mediationInfo = new MediationInfo( "MAX" );

            final MolocoInitParams initParams = new MolocoInitParams( getContext( activity ), appKey, mediationInfo );
            Moloco.initialize( initParams, initStatus -> {

                if ( initStatus.getInitialization() == Initialization.SUCCESS )
                {
                    log( "Moloco SDK initialized" );
                    initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( initializationStatus, null );
                }
                else
                {
                    log( "Moloco SDK failed to initialize with error: " + initStatus.getDescription() );
                    initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( initializationStatus, initStatus.getDescription() );
                }
            } );
        }
        else
        {
            log( "Moloco SDK already initialized" );
            onCompletionListener.onCompletion( initializationStatus, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( com.moloco.sdk.BuildConfig.class, "SDK_VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public boolean isBeta()
    {
        return true;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialAd != null )
        {
            interstitialAd.destroy();
            interstitialAd = null;
            interstitialAdListener = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.destroy();
            rewardedAd = null;
            rewardedAdListener = null;
        }

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    //region MAX Signal Provider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal" );

        updatePrivacyPreferences( parameters );

        Moloco.getBidToken( (signal, errorType) -> {

            if ( errorType == null )
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( signal );
            }
            else
            {
                log( "Signal collection failed: " + errorType.getDescription() );
                callback.onSignalCollectionFailed( errorType.getDescription() );
            }
        } );
    }

    //endregion

    //region MAX Interstitial Adapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading interstitial ad: " + placementId );

        if ( activity == null )
        {
            log( "Interstitial ad load failed: Activity is null" );

            final MaxAdapterError error = new MaxAdapterError( MaxAdapterError.ERROR_CODE_MISSING_ACTIVITY, "Missing Activity" );
            listener.onInterstitialAdLoadFailed( error );

            return;
        }

        updatePrivacyPreferences( parameters );

        final Function1<InterstitialAd, Unit> createCallback = interstitialAd -> {

            if ( interstitialAd == null )
            {
                listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            }
            else
            {
                this.interstitialAd = interstitialAd;
                interstitialAdListener = new InterstitialAdListener( listener );
                interstitialAd.load( parameters.getBidResponse(), interstitialAdListener );
            }

            return Unit.INSTANCE;
        };

        Moloco.createInterstitial( placementId, createCallback );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( !interstitialAd.isLoaded() )
        {
            log( "Unable to show interstitial - ad not ready" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );

            return;
        }

        interstitialAd.show( interstitialAdListener );
    }

    //endregion

    //region MAX Rewarded Adapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded ad: " + placementId );

        if ( activity == null )
        {
            log( "Rewarded ad load failed: Activity is null" );

            final MaxAdapterError error = new MaxAdapterError( MaxAdapterError.ERROR_CODE_MISSING_ACTIVITY, "Missing Activity" );
            listener.onRewardedAdLoadFailed( error );

            return;
        }

        updatePrivacyPreferences( parameters );

        final Function1<RewardedInterstitialAd, Unit> createCallback = rewardedAd -> {

            if ( rewardedAd == null )
            {
                listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            }
            else
            {
                this.rewardedAd = rewardedAd;
                rewardedAdListener = new RewardedAdListener( listener );
                rewardedAd.load( parameters.getBidResponse(), rewardedAdListener );
            }

            return Unit.INSTANCE;
        };

        Moloco.createRewardedInterstitial( placementId, createCallback );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( !rewardedAd.isLoaded() )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_NOT_READY );

            return;
        }

        configureReward( parameters );

        rewardedAd.show( rewardedAdListener );
    }

    //endregion

    //region MAX Ad View Adapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading " + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad: " + placementId );

        if ( activity == null )
        {
            log( adFormat.getLabel() + " ad load failed: Activity is null" );

            final MaxAdapterError error = new MaxAdapterError( MaxAdapterError.ERROR_CODE_MISSING_ACTIVITY, "Missing Activity" );
            listener.onAdViewAdLoadFailed( error );

            return;
        }

        updatePrivacyPreferences( parameters );

        if ( isNative )
        {
            final Function1<NativeAdForMediation, Unit> createCallback = nativeAd -> {

                if ( nativeAd == null )
                {
                    listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
                }
                else
                {
                    this.nativeAd = nativeAd;
                    final NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( adFormat, parameters, getContext( activity ), listener );
                    nativeAd.setInteractionListener( nativeAdViewListener );
                    nativeAd.load( parameters.getBidResponse(), nativeAdViewListener );
                }

                return Unit.INSTANCE;
            };

            Moloco.createNativeAd( placementId, createCallback );
        }
        else
        {
            final Function1<Banner, Unit> createCallback = adView -> {

                if ( adView == null )
                {
                    listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
                }
                else
                {
                    this.adView = adView;
                    final AdViewAdListener adViewAdListener = new AdViewAdListener( listener );
                    adView.setAdShowListener( adViewAdListener );
                    adView.load( parameters.getBidResponse(), adViewAdListener );
                }

                return Unit.INSTANCE;
            };

            if ( adFormat == MaxAdFormat.BANNER )
            {
                Moloco.createBanner( placementId, createCallback );
            }
            else if ( adFormat == MaxAdFormat.LEADER )
            {
                Moloco.createBannerTablet( placementId, createCallback );
            }
            else if ( adFormat == MaxAdFormat.MREC )
            {
                Moloco.createMREC( placementId, createCallback );
            }
            else
            {
                throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
            }
        }
    }

    //endregion

    //region MAX Native Ad Adapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading native ad: " + placementId );

        if ( activity == null )
        {
            log( "Native ad load failed: Activity is null" );

            final MaxAdapterError error = new MaxAdapterError( MaxAdapterError.ERROR_CODE_MISSING_ACTIVITY, "Missing Activity" );
            listener.onNativeAdLoadFailed( error );

            return;
        }

        updatePrivacyPreferences( parameters );

        final Function1<NativeAdForMediation, Unit> createCallback = nativeAd -> {

            if ( nativeAd == null )
            {
                listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            }
            else
            {
                final NativeAdListener nativeAdListener = new NativeAdListener( parameters, getContext( activity ), listener );
                nativeAd.setInteractionListener( nativeAdListener );
                nativeAd.load( parameters.getBidResponse(), nativeAdListener );
            }

            return Unit.INSTANCE;
        };

        Moloco.createNativeAd( placementId, createCallback );
    }

    //region Helper Methods

    private MaxAdapterError toMaxError(final MolocoAdError molocoAdError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( molocoAdError.getErrorType() )
        {
            case UNKNOWN:
                break;
            case SDK_INIT_ERROR:
            case AD_LOAD_FAILED_SDK_NOT_INIT:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case SDK_PERSISTENT_HTTP_REQUEST_FAILED_TO_INIT:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case SDK_INVALID_CONFIGURATION:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case AD_LOAD_FAILED:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case AD_LOAD_TIMEOUT_ERROR:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case AD_LOAD_LIMIT_REACHED:
                adapterError = MaxAdapterError.AD_FREQUENCY_CAPPED;
                break;
            case AD_SHOW_ERROR:
            case AD_SHOW_ERROR_ALREADY_DISPLAYING:
                adapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
            case AD_SHOW_ERROR_NOT_LOADED:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case AD_BID_PARSE_ERROR:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case AD_SIGNAL_COLLECTION_FAILED:
                adapterError = MaxAdapterError.SIGNAL_COLLECTION_TIMEOUT;
                break;
        }

        return new MaxAdapterError( adapterError,
                                    molocoAdError.getErrorType().getErrorCode(),
                                    molocoAdError.getDescription() );
    }

    private void updatePrivacyPreferences(final MaxAdapterParameters parameters)
    {
        final Boolean hasUserConsent = parameters.hasUserConsent();
        final Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        final Boolean isDoNotSell = parameters.isDoNotSell();

        final MolocoPrivacy.PrivacySettings privacySettings = new MolocoPrivacy.PrivacySettings( hasUserConsent, isAgeRestrictedUser, isDoNotSell );
        MolocoPrivacy.setPrivacy( privacySettings );
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
            implements AdLoad.Listener, InterstitialAdShowListener
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoadSuccess(@NonNull final MolocoAd molocoAd)
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdLoadFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "Interstitial ad failed to load with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowSuccess(@NonNull final MolocoAd molocoAd)
        {
            log( "Interstitial ad displayed" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdShowFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "Interstitial ad failed to display with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked(@NonNull final MolocoAd molocoAd)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdHidden(@NonNull final MolocoAd molocoAd)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements AdLoad.Listener, RewardedInterstitialAdShowListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        public RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoadSuccess(@NonNull final MolocoAd molocoAd)
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdLoadFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "Rewarded ad failed to load with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowSuccess(@NonNull final MolocoAd molocoAd)
        {
            log( "Rewarded ad displayed" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdShowFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "Rewarded ad failed to display error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked(@NonNull final MolocoAd molocoAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onRewardedVideoStarted(@NonNull final MolocoAd molocoAd)
        {
            log( "Rewarded video started" );
        }

        @Override
        public void onRewardedVideoCompleted(@NonNull final MolocoAd molocoAd)
        {
            log( "Rewarded video completed" );
        }

        @Override
        public void onUserRewarded(@NonNull final MolocoAd molocoAd)
        {
            log( "Rewarded video granted reward" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdHidden(@NonNull final MolocoAd molocoAd)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden" );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewAdListener
            implements AdLoad.Listener, BannerAdShowListener
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewAdListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoadSuccess(@NonNull final MolocoAd molocoAd)
        {
            log( "AdView ad loaded" );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onAdLoadFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "AdView ad failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowSuccess(@NonNull final MolocoAd molocoAd)
        {
            log( "AdView ad displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdShowFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "AdView ad failed to display with error: " + adapterError );
            listener.onAdViewAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked(@NonNull final MolocoAd molocoAd)
        {
            log( "AdView ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdHidden(@NonNull final MolocoAd molocoAd) { }
    }

    private class NativeAdViewListener
            implements AdLoad.Listener, NativeAdForMediation.InteractionListener
    {
        private final MaxAdFormat              adFormat;
        private final Bundle                   serverParameters;
        private final Context                  context;
        private final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdFormat adFormat, final MaxAdapterResponseParameters parameters, final Context context, final MaxAdViewAdapterListener listener)
        {
            this.adFormat = adFormat;
            this.serverParameters = parameters.getServerParameters();
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoadSuccess(@NonNull final MolocoAd molocoAd)
        {
            if ( nativeAd == null )
            {
                e( "Native " + adFormat.getLabel() + " ad is null" );
                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded" );

            if ( TextUtils.isEmpty( nativeAd.getTitle() ) )
            {
                e( "Native " + adFormat.getLabel() + " ad (" + nativeAd + ") does not have required assets." );
                listener.onAdViewAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( adFormat )
                    .setTitle( nativeAd.getTitle() )
                    .setBody( nativeAd.getDescription() )
                    .setAdvertiser( nativeAd.getSponsorText() )
                    .setCallToAction( nativeAd.getCallToActionText() )
                    .setStarRating( nativeAd.getRating() != null ? nativeAd.getRating().doubleValue() : null );

            if ( nativeAd.getIconUri() != null )
            {
                // getIconUri() returns a Uri string without the "file://" prefix.
                final Uri iconUri = Uri.fromFile( new File( nativeAd.getIconUri() ) );
                builder.setIcon( new MaxNativeAd.MaxNativeAdImage( iconUri ) );
            }

            if ( nativeAd.getVideo() != null )
            {
                builder.setMediaView( nativeAd.getVideo() );
            }
            else if ( nativeAd.getMainImageUri() != null )
            {
                // getMainImageUri() returns a Uri string without the "file://" prefix.
                final Uri mainImageUri = Uri.fromFile( new File( nativeAd.getMainImageUri() ) );

                final ImageView imageView = new ImageView( context );
                ImageViewUtils.setImageUri( imageView, mainImageUri, null );

                builder.setMediaView( imageView );
                builder.setMainImage( new MaxNativeAd.MaxNativeAdImage( mainImageUri ) );
            }

            final MaxMolocoNativeAd maxMolocoNativeAd = new MaxMolocoNativeAd( builder );
            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final MaxNativeAdView maxNativeAdView = new MaxNativeAdView( maxMolocoNativeAd,
                                                                         AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                                         getApplicationContext() );

            maxMolocoNativeAd.prepareForInteraction( getClickableViews( maxNativeAdView ), maxNativeAdView );
            listener.onAdViewAdLoaded( maxNativeAdView );

            nativeAd.handleImpression();
        }

        @Override
        public void onAdLoadFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "Native " + adFormat.getLabel() + " ad failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onImpressionHandled()
        {
            log( "Native " + adFormat.getLabel() + " ad displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onGeneralClickHandled()
        {
            log( "Native " + adFormat.getLabel() + " ad clicked" );
            listener.onAdViewAdClicked();
        }
    }

    private class NativeAdListener
            implements AdLoad.Listener, NativeAdForMediation.InteractionListener
    {
        private final Bundle                     serverParameters;
        private final Context                    context;
        private final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = parameters.getServerParameters();
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoadSuccess(@NonNull final MolocoAd molocoAd)
        {
            if ( nativeAd == null )
            {
                e( "Native ad is null" );
                return;
            }

            log( "Native ad loaded" );

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );

            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( nativeAd.getTitle() )
                    .setBody( nativeAd.getDescription() )
                    .setAdvertiser( nativeAd.getSponsorText() )
                    .setCallToAction( nativeAd.getCallToActionText() )
                    .setStarRating( nativeAd.getRating() != null ? nativeAd.getRating().doubleValue() : null );

            if ( nativeAd.getIconUri() != null )
            {
                // getIconUri() returns a Uri string without the "file://" prefix.
                final Uri iconUri = Uri.fromFile( new File( nativeAd.getIconUri() ) );
                builder.setIcon( new MaxNativeAd.MaxNativeAdImage( iconUri ) );
            }

            if ( nativeAd.getVideo() != null )
            {
                builder.setMediaView( nativeAd.getVideo() );
            }
            else if ( nativeAd.getMainImageUri() != null )
            {
                // getMainImageUri() returns a Uri string without the "file://" prefix.
                final Uri mainImageUri = Uri.fromFile( new File( nativeAd.getMainImageUri() ) );

                final ImageView imageView = new ImageView( context );
                ImageViewUtils.setImageUri( imageView, mainImageUri, null );

                builder.setMediaView( imageView );
                builder.setMainImage( new MaxNativeAd.MaxNativeAdImage( mainImageUri ) );
            }

            final MaxNativeAd maxNativeAd = new MaxMolocoNativeAd( builder );
            listener.onNativeAdLoaded( maxNativeAd, null );
        }

        @Override
        public void onAdLoadFailed(@NonNull final MolocoAdError molocoAdError)
        {
            final MaxAdapterError adapterError = toMaxError( molocoAdError );
            log( "Native ad failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onImpressionHandled()
        {
            log( "Native ad displayed" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onGeneralClickHandled()
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }
    }

    private class MaxMolocoNativeAd
            extends MaxNativeAd
    {
        public MaxMolocoNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            prepareForInteraction( MolocoMediationAdapter.this.getClickableViews( maxNativeAdView ), maxNativeAdView );
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAdForMediation nativeAd = MolocoMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view: native ad is null." );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            for ( final View clickableView : clickableViews )
            {
                clickableView.setOnClickListener( view -> {

                    if ( view instanceof ImageView )
                    {
                        nativeAd.handleIconClick();
                    }
                    else
                    {
                        nativeAd.handleGeneralAdClick();
                    }
                } );
            }

            final View mediaView = getMediaView();
            if ( mediaView != null )
            {
                mediaView.setOnClickListener( view -> nativeAd.handleMainImageClick() );
            }

            if ( getFormat() == MaxAdFormat.NATIVE )
            {
                nativeAd.handleImpression();
            }

            return true;
        }
    }
}
