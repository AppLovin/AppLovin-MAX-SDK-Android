package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxAppOpenAdapter;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
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
import com.applovin.mediation.adapters.bigoads.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import sg.bigo.ads.BigoAdSdk;
import sg.bigo.ads.ConsentOptions;
import sg.bigo.ads.api.AdConfig;
import sg.bigo.ads.api.AdError;
import sg.bigo.ads.api.AdInteractionListener;
import sg.bigo.ads.api.AdLoadListener;
import sg.bigo.ads.api.AdOptionsView;
import sg.bigo.ads.api.AdSize;
import sg.bigo.ads.api.BannerAd;
import sg.bigo.ads.api.BannerAdLoader;
import sg.bigo.ads.api.BannerAdRequest;
import sg.bigo.ads.api.InterstitialAd;
import sg.bigo.ads.api.InterstitialAdLoader;
import sg.bigo.ads.api.InterstitialAdRequest;
import sg.bigo.ads.api.MediaView;
import sg.bigo.ads.api.NativeAd;
import sg.bigo.ads.api.NativeAdLoader;
import sg.bigo.ads.api.NativeAdRequest;
import sg.bigo.ads.api.RewardAdInteractionListener;
import sg.bigo.ads.api.RewardVideoAd;
import sg.bigo.ads.api.RewardVideoAdLoader;
import sg.bigo.ads.api.RewardVideoAdRequest;
import sg.bigo.ads.api.SplashAd;
import sg.bigo.ads.api.SplashAdInteractionListener;
import sg.bigo.ads.api.SplashAdLoader;
import sg.bigo.ads.api.SplashAdRequest;

import static sg.bigo.ads.api.AdError.ERROR_CODE_ACTIVITY_CREATE_ERROR;
import static sg.bigo.ads.api.AdError.ERROR_CODE_AD_EXPIRED;
import static sg.bigo.ads.api.AdError.ERROR_CODE_APP_ID_UNMATCHED;
import static sg.bigo.ads.api.AdError.ERROR_CODE_ASSETS_ERROR;
import static sg.bigo.ads.api.AdError.ERROR_CODE_FULLSCREEN_AD_FAILED_TO_OPEN;
import static sg.bigo.ads.api.AdError.ERROR_CODE_FULLSCREEN_AD_FAILED_TO_SHOW;
import static sg.bigo.ads.api.AdError.ERROR_CODE_INTERNAL_ERROR;
import static sg.bigo.ads.api.AdError.ERROR_CODE_INVALID_REQUEST;
import static sg.bigo.ads.api.AdError.ERROR_CODE_NATIVE_VIEW_MISSING;
import static sg.bigo.ads.api.AdError.ERROR_CODE_NETWORK_ERROR;
import static sg.bigo.ads.api.AdError.ERROR_CODE_NO_FILL;
import static sg.bigo.ads.api.AdError.ERROR_CODE_TIMEOUT_STRATEGY;
import static sg.bigo.ads.api.AdError.ERROR_CODE_UNINITIALIZED;
import static sg.bigo.ads.api.AdError.ERROR_CODE_VIDEO_ERROR;

public class BigoAdsMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxAppOpenAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final String MEDIATION_INFO;

    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private InterstitialAd interstitialAd;
    private SplashAd       appOpenAd;
    private RewardVideoAd  rewardedAd;
    private BannerAd       adViewAd;
    private NativeAd       nativeAd;

    private InterstitialAdListener interstitialAdListener;
    private AppOpenAdListener      appOpenAdListener;
    private RewardedAdListener     rewardedAdListener;
    private AdViewListener         adViewListener;
    private NativeAdViewListener   nativeAdViewListener;
    private NativeAdListener       nativeAdListener;

    static
    {
        final JSONObject mediationInfoJSON = new JSONObject();

        try
        {
            mediationInfoJSON.putOpt( "mediationName", "Max" );
            mediationInfoJSON.putOpt( "mediationVersion", AppLovinSdk.VERSION );
            mediationInfoJSON.putOpt( "adapterVersion", BuildConfig.VERSION_NAME );
        }
        catch ( Throwable th )
        {
            Log.e( "BigoAdsMediationAdapter", "Error creating mediation info JSON", th );
        }

        MEDIATION_INFO = mediationInfoJSON.toString();
    }

    public BigoAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final Bundle serverParameters = parameters.getServerParameters();
            final String appId = serverParameters.getString( "app_id" );
            log( "Initializing Bigo Ads SDK with app id: " + appId + "..." );

            final AdConfig config = new AdConfig.Builder()
                    .setAppId( appId )
                    .setDebug( parameters.isTesting() )
                    .build();

            BigoAdSdk.initialize( getApplicationContext(), config, new BigoAdSdk.InitListener()
            {
                @Override
                public void onInitialized()
                {
                    if ( BigoAdSdk.isInitialized() )
                    {
                        log( "Bigo Ads SDK initialized" );
                        status = InitializationStatus.INITIALIZED_SUCCESS;
                    }
                    else
                    {
                        log( "Bigo Ads SDK failed to initialize" );
                        status = InitializationStatus.INITIALIZED_FAILURE;
                    }

                    onCompletionListener.onCompletion( status, null );
                }
            } );
        }
        else
        {
            log( "Bigo Ads SDK attempted initialization already" );
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return BigoAdSdk.getSDKVersion();
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
            interstitialAd.destroy();
            interstitialAd = null;
            interstitialAdListener = null;
        }

        if ( appOpenAd != null )
        {
            appOpenAd.destroy();
            appOpenAd = null;
            appOpenAdListener = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.destroy();
            rewardedAd = null;
            rewardedAdListener = null;
        }

        if ( adViewAd != null )
        {
            adViewAd.destroy();
            adViewAd = null;
            adViewListener = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
            nativeAdViewListener = null;
            nativeAdListener = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserConsent( parameters );

        final String bidToken = BigoAdSdk.getBidderToken();
        callback.onSignalCollected( bidToken );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad for slot id: " + slotId + "..." );

        if ( !BigoAdSdk.isInitialized() )
        {
            log( "Bigo Ads SDK not successfully initialized: failing interstitial ad load for slot id: " + slotId );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserConsent( parameters );

        interstitialAdListener = new InterstitialAdListener( slotId, listener );
        final InterstitialAdLoader interstitialAdLoader = new InterstitialAdLoader.Builder()
                .withAdLoadListener( interstitialAdListener )
                .withExt( MEDIATION_INFO )
                .build();

        final InterstitialAdRequest interstitialAdRequest = new InterstitialAdRequest.Builder()
                .withSlotId( slotId )
                .withBid( parameters.getBidResponse() )
                .build();

        interstitialAdLoader.loadAd( interstitialAdRequest );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for slot id: " + slotId + "..." );

        if ( interstitialAd.isExpired() )
        {
            log( "Unable to show interstitial ad for slot id: " + slotId + " - ad expired" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_EXPIRED );
        }
        else
        {
            interstitialAd.setAdInteractionListener( interstitialAdListener );
            interstitialAd.show();
        }
    }

    @Override
    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading app open ad for slot id: " + slotId + "..." );

        if ( !BigoAdSdk.isInitialized() )
        {
            log( "Bigo Ads SDK not successfully initialized: failing app open ad load for slot id: " + slotId );
            listener.onAppOpenAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserConsent( parameters );

        appOpenAdListener = new AppOpenAdListener( slotId, listener );
        final SplashAdLoader splashAdLoader = new SplashAdLoader.Builder()
                .withAdLoadListener( appOpenAdListener )
                .withExt( MEDIATION_INFO )
                .build();

        final ApplicationInfo applicationInfo = getApplicationContext().getApplicationInfo();
        final int appLogo = applicationInfo.logo;
        final String appName = applicationInfo.name;
        final SplashAdRequest splashAdRequest = new SplashAdRequest.Builder()
                .withSlotId( slotId )
                .withBid( parameters.getBidResponse() )
                .withAppLogo( appLogo )
                .withAppName( appName )
                .build();

        splashAdLoader.loadAd( splashAdRequest );
    }

    @Override
    public void showAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing app open ad for slot id: " + slotId + "..." );

        if ( appOpenAd.isExpired() )
        {
            log( "Unable to show app open ad for slot id: " + slotId + " - ad expired" );
            listener.onAppOpenAdDisplayFailed( MaxAdapterError.AD_EXPIRED );
        }
        else
        {
            appOpenAd.setAdInteractionListener( appOpenAdListener );
            appOpenAd.show();
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for slot id: " + slotId + "..." );

        if ( !BigoAdSdk.isInitialized() )
        {
            log( "Bigo Ads SDK not successfully initialized: failing rewarded ad load for slot id: " + slotId );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserConsent( parameters );

        rewardedAdListener = new RewardedAdListener( slotId, listener );
        final RewardVideoAdLoader rewardVideoAdAdLoader = new RewardVideoAdLoader.Builder()
                .withAdLoadListener( rewardedAdListener )
                .withExt( MEDIATION_INFO )
                .build();

        final RewardVideoAdRequest rewardVideoAdAdRequest = new RewardVideoAdRequest.Builder()
                .withSlotId( slotId )
                .withBid( parameters.getBidResponse() )
                .build();

        rewardVideoAdAdLoader.loadAd( rewardVideoAdAdRequest );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for slot id: " + slotId + "..." );

        if ( rewardedAd.isExpired() )
        {
            log( "Unable to show rewarded ad for slot id: " + slotId + " - ad expired" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_EXPIRED );
        }
        else
        {
            configureReward( parameters );
            rewardedAd.setAdInteractionListener( rewardedAdListener );
            rewardedAd.show();
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        log( "Loading " + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for slot id: " + slotId );

        if ( !BigoAdSdk.isInitialized() )
        {
            log( "Bigo Ads SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load for slot id: " + slotId );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserConsent( parameters );

        if ( isNative )
        {
            nativeAdViewListener = new NativeAdViewListener( slotId, adFormat, parameters.getServerParameters(), listener );
            final NativeAdLoader nativeAdLoader = new NativeAdLoader.Builder()
                    .withAdLoadListener( nativeAdViewListener )
                    .withExt( MEDIATION_INFO )
                    .build();

            final NativeAdRequest nativeAdRequest = new NativeAdRequest.Builder()
                    .withSlotId( slotId )
                    .withBid( parameters.getBidResponse() )
                    .build();

            nativeAdLoader.loadAd( nativeAdRequest );
        }
        else
        {
            adViewListener = new AdViewListener( slotId, adFormat, listener );
            final BannerAdLoader bannerAdLoader = new BannerAdLoader.Builder()
                    .withAdLoadListener( adViewListener )
                    .withExt( MEDIATION_INFO )
                    .build();

            final BannerAdRequest bannerAdRequest = new BannerAdRequest.Builder()
                    .withSlotId( slotId )
                    .withBid( parameters.getBidResponse() )
                    .withAdSizes( toAdSize( adFormat ) )
                    .build();

            bannerAdLoader.loadAd( bannerAdRequest );
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading native ad for slot id: " + slotId + "..." );

        if ( !BigoAdSdk.isInitialized() )
        {
            log( "Bigo Ads SDK not successfully initialized: failing native ad load for slot id: " + slotId );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserConsent( parameters );

        nativeAdListener = new NativeAdListener( slotId, parameters.getServerParameters(), listener );
        final NativeAdLoader nativeAdLoader = new NativeAdLoader.Builder()
                .withAdLoadListener( nativeAdListener )
                .withExt( MEDIATION_INFO )
                .build();

        final NativeAdRequest nativeAdRequest = new NativeAdRequest.Builder()
                .withSlotId( slotId )
                .withBid( parameters.getBidResponse() )
                .build();

        nativeAdLoader.loadAd( nativeAdRequest );
    }

    private AdSize toAdSize(final MaxAdFormat adFormat)
    {
        // TODO: Bigo does not currently have a leader of size 728x90 but they will be adding it in a later SDK release.

        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.MEDIUM_RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(final AdError error)
    {
        final int bigoAdsErrorCode = error.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( bigoAdsErrorCode )
        {
            case ERROR_CODE_UNINITIALIZED:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case ERROR_CODE_INVALID_REQUEST:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case ERROR_CODE_NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case ERROR_CODE_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case ERROR_CODE_INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case ERROR_CODE_APP_ID_UNMATCHED:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case ERROR_CODE_AD_EXPIRED:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case ERROR_CODE_NATIVE_VIEW_MISSING:
                adapterError = MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS;
                break;
            case ERROR_CODE_ASSETS_ERROR:
            case ERROR_CODE_VIDEO_ERROR:
            case ERROR_CODE_FULLSCREEN_AD_FAILED_TO_SHOW:
            case ERROR_CODE_FULLSCREEN_AD_FAILED_TO_OPEN:
                adapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
            case ERROR_CODE_ACTIVITY_CREATE_ERROR:
                adapterError = MaxAdapterError.MISSING_ACTIVITY;
                break;
            case ERROR_CODE_TIMEOUT_STRATEGY:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
        }

        return new MaxAdapterError( adapterError, bigoAdsErrorCode, error.getMessage() );
    }

    private void updateUserConsent(final MaxAdapterParameters parameters)
    {
        final Context context = getApplicationContext();

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            BigoAdSdk.setUserConsent( context, ConsentOptions.GDPR, hasUserConsent );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            BigoAdSdk.setUserConsent( context, ConsentOptions.CCPA, !isDoNotSell );
        }
    }

    private class InterstitialAdListener
            implements AdLoadListener<InterstitialAd>, AdInteractionListener
    {
        private final String                         slotId;
        private final MaxInterstitialAdapterListener listener;

        private InterstitialAdListener(final String slotId, final MaxInterstitialAdapterListener listener)
        {
            this.slotId = slotId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull InterstitialAd ad)
        {
            log( "Interstitial ad loaded for slot id: " + slotId );
            interstitialAd = ad;
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onError(@NonNull AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Interstitial ad (" + slotId + ") failed to load with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            log( "Interstitial ad opened for slot id: " + slotId );
        }

        @Override
        public void onAdImpression()
        {
            log( "Interstitial ad impression recorded for slot id: " + slotId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdError(@NonNull final AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Interstitial ad (" + slotId + ") failed to show with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad click recorded for slot id: " + slotId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            log( "Interstitial ad hidden for slot id: " + slotId );
            listener.onInterstitialAdHidden();
        }
    }

    private class AppOpenAdListener
            implements AdLoadListener<SplashAd>, SplashAdInteractionListener
    {
        private final String                    slotId;
        private final MaxAppOpenAdapterListener listener;

        private AppOpenAdListener(final String slotId, final MaxAppOpenAdapterListener listener)
        {
            this.slotId = slotId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final SplashAd splashAd)
        {
            log( "App open ad loaded for slot id: " + slotId );
            appOpenAd = splashAd;
            listener.onAppOpenAdLoaded();
        }

        @Override
        public void onError(@NonNull AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "App open ad (" + slotId + ") failed to load with error: " + adapterError );
            listener.onAppOpenAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) app open ads will not trigger this callback.
        }

        @Override
        public void onAdImpression()
        {
            log( "App open ad impression recorded for slot id: " + slotId );
            listener.onAppOpenAdDisplayed();
        }

        @Override
        public void onAdError(@NonNull final AdError adError)
        {
            final MaxAdapterError adapterError = toMaxError( adError );
            log( "App open ad (" + slotId + ") failed to show with error: " + adapterError );
            listener.onAppOpenAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "App open ad click recorded for slot id: " + slotId );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdFinished()
        {
            // NOTE: This callback indicates the display countdown has ended. 
            log( "App open ad finished for slot id: " + slotId );
        }

        @Override
        public void onAdSkipped()
        {
            log( "App open ad skipped for slot id: " + slotId );

            // NOTE: According to Bigo, the only way to hide their app open ads is by clicking the skip button.
            listener.onAppOpenAdHidden();
        }

        @Override
        public void onAdClosed()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) app open ads will not trigger this callback.
        }
    }

    private class RewardedAdListener
            implements AdLoadListener<RewardVideoAd>, RewardAdInteractionListener
    {
        private final String                     slotId;
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedAdListener(final String slotId, final MaxRewardedAdapterListener listener)
        {
            this.slotId = slotId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull RewardVideoAd ad)
        {
            log( "Rewarded ad loaded for slot id: " + slotId );
            rewardedAd = ad;
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onError(@NonNull AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Rewarded ad (" + slotId + ") failed to load with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            log( "Rewarded ad opened for slot id: " + slotId );
        }

        @Override
        public void onAdImpression()
        {
            log( "Rewarded ad impression recorded for slot id: " + slotId );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdError(@NonNull final AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Rewarded ad (" + slotId + ") failed to show with error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad click recorded for slot id: " + slotId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdRewarded()
        {
            log( "User earned reward for slot id: " + slotId );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed()
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden for slot id: " + slotId );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            implements AdLoadListener<BannerAd>, AdInteractionListener
    {
        private final String                   slotId;
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final String slotId, final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.slotId = slotId;
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull BannerAd ad)
        {
            log( adFormat.getLabel() + " ad loaded for slot id: " + slotId );

            adViewAd = ad;
            ad.setAdInteractionListener( adViewListener );
            listener.onAdViewAdLoaded( ad.adView() );
        }

        @Override
        public void onError(@NonNull AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( adFormat.getLabel() + " ad (" + slotId + ") failed to load with error :" + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) banner ads will not trigger this callback.
        }

        @Override
        public void onAdImpression()
        {
            log( adFormat.getLabel() + " ad impression recorded for slot id: " + slotId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdError(@NonNull final AdError adError)
        {
            final MaxAdapterError adapterError = toMaxError( adError );
            log( adFormat.getLabel() + " ad (" + slotId + ") failed to show with error: " + adapterError );
            listener.onAdViewAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( adFormat.getLabel() + " ad clicked recorded for slot id: " + slotId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) banner ads will not trigger this callback.
        }
    }

    private class NativeAdViewListener
            implements AdLoadListener<NativeAd>, AdInteractionListener
    {
        private final String                   slotId;
        private final MaxAdFormat              adFormat;
        private final Bundle                   serverParameters;
        private final MaxAdViewAdapterListener listener;

        private NativeAdViewListener(final String slotId, final MaxAdFormat adFormat, final Bundle serverParameters, final MaxAdViewAdapterListener listener)
        {
            this.slotId = slotId;
            this.adFormat = adFormat;
            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull NativeAd ad)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded for slot id: " + slotId );

            if ( ad == null )
            {
                log( "Native " + adFormat.getLabel() + " ad + (" + ad + ") can 't be null." );

                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            if ( TextUtils.isEmpty( ad.getTitle() ) )
            {
                log( "Native " + adFormat.getLabel() + " ad (" + ad + ") does not have required assets." );
                listener.onAdViewAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            ad.setAdInteractionListener( nativeAdViewListener );
            BigoAdsMediationAdapter.this.nativeAd = ad;

            final Context context = getApplicationContext();
            final ImageView iconView = new ImageView( context );
            final AdOptionsView optionsView = new AdOptionsView( context );
            final MediaView mediaView = new MediaView( context );

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( adFormat )
                    .setTitle( ad.getTitle() )
                    .setAdvertiser( ad.getAdvertiser() )
                    .setBody( ad.getDescription() )
                    .setCallToAction( ad.getCallToAction() )
                    .setIconView( iconView )
                    .setOptionsView( optionsView )
                    .setMediaView( mediaView );

            final MaxBigoAdsNativeAd maxNativeAd = new MaxBigoAdsNativeAd( builder );

            MaxNativeAdView maxNativeAdView;
            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            if ( templateName.equals( "vertical" ) )
            {
                String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                maxNativeAdView = new MaxNativeAdView( maxNativeAd, verticalTemplateName, getApplicationContext() );
            }
            else
            {
                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
            }

            maxNativeAd.prepareForInteraction( maxNativeAdView.getClickableViews(), maxNativeAdView );
            listener.onAdViewAdLoaded( maxNativeAdView );
        }

        @Override
        public void onError(@NonNull AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Native " + adFormat.getLabel() + " ad (" + slotId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) native ads will not trigger this callback.
        }

        @Override
        public void onAdImpression()
        {
            log( "Native " + adFormat.getLabel() + " ad impression recorded for slot id: " + slotId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdError(@NonNull final AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Native " + adFormat.getLabel() + " ad (" + slotId + ") failed to show with error: " + adapterError );
            listener.onAdViewAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native " + adFormat.getLabel() + " ad click recorded for slot id: " + slotId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) native ads will not trigger this callback.
        }
    }

    private class NativeAdListener
            implements AdLoadListener<NativeAd>, AdInteractionListener
    {
        private final String                     slotId;
        private final Bundle                     serverParameters;
        private final MaxNativeAdAdapterListener listener;

        private NativeAdListener(final String slotId, final Bundle serverParameters, final MaxNativeAdAdapterListener listener)
        {
            this.slotId = slotId;
            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull NativeAd ad)
        {
            log( "Native ad loaded for slot id: " + slotId );

            if ( ad == null )
            {
                log( "Native ad (" + ad + ")  can't be null." );

                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            ad.setAdInteractionListener( nativeAdListener );
            BigoAdsMediationAdapter.this.nativeAd = ad;

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );

            if ( isTemplateAd && TextUtils.isEmpty( ad.getTitle() ) )
            {
                log( "Native ad (" + ad + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            final Context context = getApplicationContext();
            final ImageView iconView = new ImageView( context );
            final AdOptionsView optionsView = new AdOptionsView( context );
            final MediaView mediaView = new MediaView( context );

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( ad.getTitle() )
                    .setAdvertiser( ad.getAdvertiser() )
                    .setBody( ad.getDescription() )
                    .setCallToAction( ad.getCallToAction() )
                    .setIconView( iconView )
                    .setOptionsView( optionsView )
                    .setMediaView( mediaView );

            final MaxBigoAdsNativeAd maxNativeAd = new MaxBigoAdsNativeAd( builder );

            listener.onNativeAdLoaded( maxNativeAd, null );
        }

        @Override
        public void onError(@NonNull AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Native ad (" + slotId + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) native ads will not trigger this callback.
        }

        @Override
        public void onAdImpression()
        {
            log( "Native ad impression recorded for slot id: " + slotId );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdError(@NonNull final AdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Native ad (" + slotId + ") failed to show with error: " + adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad click recorded for slot id: " + slotId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            // NOTE: As stated in the docs(https://www.bigossp.com/guide/sdk/android/document) native ads will not trigger this callback.
        }
    }

    private class MaxBigoAdsNativeAd
            extends MaxNativeAd
    {

        public MaxBigoAdsNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAd nativeAd = BigoAdsMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;

            MediaView mediaView = null;
            if ( maxNativeAdView.getMediaContentViewGroup() != null )
            {
                mediaView = (MediaView) getMediaView();
            }

            ImageView iconView = null;
            if ( maxNativeAdView.getIconContentView() != null )
            {
                iconView = (ImageView) getIconView();
            }
            else if ( maxNativeAdView.getIconImageView() != null )
            {
                iconView = maxNativeAdView.getIconImageView();
            }

            AdOptionsView optionsView = null;
            if ( maxNativeAdView.getOptionsContentViewGroup() != null )
            {
                optionsView = (AdOptionsView) getOptionsView();
            }

            nativeAd.registerViewForInteraction( container, mediaView, iconView, optionsView, clickableViews );

            return true;
        }
    }
}
