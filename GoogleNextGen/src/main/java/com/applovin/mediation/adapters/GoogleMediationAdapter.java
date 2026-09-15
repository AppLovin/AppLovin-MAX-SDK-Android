package com.applovin.mediation.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

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
import com.applovin.mediation.adapters.google.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.BaseRequestBuilder;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.AdFormat;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.Image;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd;
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedSignalRequest;
import com.google.android.libraries.ads.mobile.sdk.signal.Signal;
import com.google.android.libraries.ads.mobile.sdk.signal.SignalError;
import com.google.android.libraries.ads.mobile.sdk.signal.SignalGenerationCallback;
import com.google.android.libraries.ads.mobile.sdk.signal.SignalRequest;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.applovin.impl.sdk.utils.CollectionUtils.toBundle;
import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

public class GoogleMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxAppOpenAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final int TITLE_LABEL_TAG          = 1;
    private static final int MEDIA_VIEW_CONTAINER_TAG = 2;
    private static final int ICON_VIEW_TAG            = 3;
    private static final int BODY_VIEW_TAG            = 4;
    private static final int CALL_TO_ACTION_VIEW_TAG  = 5;
    private static final int ADVERTISER_VIEW_TAG      = 8;

    private static final String ADAPTIVE_BANNER_TYPE_INLINE = "inline";

    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private InterstitialAd interstitialAd;
    private AppOpenAd      appOpenAd;
    private RewardedAd     rewardedAd;
    private AdView         adView;
    private NativeAd       nativeAd;
    private NativeAdView   nativeAdView;

    private AppOpenAdListener  appOpenAdListener;
    private RewardedAdListener rewardedAdListener;

    // Explicit default constructor declaration
    public GoogleMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter Methods

    @SuppressLint("MissingPermission")
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        log( "Initializing Google SDK..." );

        if ( initialized.compareAndSet( false, true ) )
        {
            Context context = getContext( activity );

            // Fetch the Application ID from the AndroidManifest.xml
            String applicationId = null;
            try
            {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo( context.getPackageName(), PackageManager.GET_META_DATA );
                if ( applicationInfo.metaData != null )
                {
                    applicationId = applicationInfo.metaData.getString( "com.google.android.gms.ads.APPLICATION_ID" );
                }
            }
            catch ( PackageManager.NameNotFoundException e )
            {
                e( "Failed to load AndroidManifest metadata", e);
            }

            // Fallback to server parameters if manifest extraction failed
            if ( TextUtils.isEmpty( applicationId ) )
            {
                applicationId = parameters.getServerParameters().getString( "app_id", null );

                if ( TextUtils.isEmpty( applicationId ) )
                {
                    e( "Google Application ID not found in manifest or server parameters." );
                    initialized.set( false );
                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, "Missing Google App ID" );
                    return;
                }
            }

            InitializationConfig config = new InitializationConfig.Builder( applicationId )
                // Prevent AdMob SDK from auto-initing its adapters in AB testing environments.
                // NOTE: If MAX makes an ad request to AdMob, and the AdMob account has AL enabled (e.g. AppLovin Bidding) _and_ detects the AdMob<->AppLovin adapter, AdMob will still attempt to initialize AppLovin
                .disableMediationAdapterInitialization()
                .build();

            new Thread( () ->
            {
                if ( parameters.getServerParameters().getBoolean( "init_without_callback", false ) )
                {
                    status = InitializationStatus.DOES_NOT_APPLY;

                    MobileAds.initialize( context, config, null );

                    // Safely return the callback to MAX on the UI thread
                    AppLovinSdkUtils.runOnUiThread( () -> onCompletionListener.onCompletion( status, null ) );
                }
                else
                {
                    status = InitializationStatus.INITIALIZING;

                    MobileAds.initialize( context, config, initializationStatus ->
                    {
                        final AdapterStatus googleAdsStatus = initializationStatus.getAdapterStatusMap()
                            .get( "com.google.android.libraries.ads.mobile.sdk.MobileAds" );

                        final AdapterStatus.InitializationState googleAdsState =
                            googleAdsStatus != null ? googleAdsStatus.getInitializationState() : null;

                        log( "Initialization complete with status " + googleAdsState );

                        status = ( AdapterStatus.InitializationState.COMPLETE == googleAdsState )
                            ? InitializationStatus.INITIALIZED_SUCCESS
                            : InitializationStatus.INITIALIZED_UNKNOWN;

                        // Safely return the callback to MAX on the UI thread
                        AppLovinSdkUtils.runOnUiThread( () -> onCompletionListener.onCompletion( status, null ) );
                    } );
                }
            } ).start();

        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return String.valueOf( MobileAds.getVersion() );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log("Destroy called for adapter " + this);

        if ( interstitialAd != null )
        {
            interstitialAd.setAdEventCallback( null );
            interstitialAd = null;
        }

        if ( appOpenAd != null )
        {
            appOpenAd.setAdEventCallback( null );
            appOpenAd = null;
            appOpenAdListener = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setAdEventCallback( null );
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

        if ( nativeAdView != null )
        {
            nativeAdView.destroy();
            nativeAdView = null;
        }
    }

    //endregion

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        Context context = getContext( activity );

        SignalRequest signalRequest = createSignalRequest( parameters, context );

        MobileAds.generateSignal( signalRequest, new SignalGenerationCallback()
        {
            @Override
            public void onSuccess(@NonNull final Signal signal)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( signal.getSignalString() );
            }

            @Override
            public void onFailure(@NonNull final SignalError error)
            {
                log( "Signal collection failed with error: " + error.getMessage() );
                callback.onSignalCollectionFailed( error.getMessage() );
            }
        } );
    }

    private SignalRequest createSignalRequest(MaxAdapterSignalCollectionParameters parameters, Context context)
    {
        MaxAdFormat adFormat = parameters.getAdFormat();
        String adUnitId = parameters.getAdUnitId();
        Bundle serverParameters = parameters.getServerParameters();

        String bidderType = BundleUtils.getString( "bidder", "", serverParameters );
        String signalType = "dv360".equalsIgnoreCase( bidderType ) ? "requester_type_3" : "requester_type_2";

        if ( adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER || adFormat == MaxAdFormat.MREC )
        {
            Object isAdaptiveBannerObj = parameters.getLocalExtraParameters().get( "adaptive_banner" );
            boolean isAdaptive = isAdaptiveBannerObj instanceof String && "true".equalsIgnoreCase( ( String ) isAdaptiveBannerObj );
            AdSize adSize = toAdSize( adFormat, isAdaptive, parameters, context );

            BannerSignalRequest.Builder builder = new BannerSignalRequest.Builder( signalType )
                .setAdUnitId( adUnitId )
                .setAdSize( adSize );

            return applyCommonRequestParameters( builder, parameters, context ).build();
        }
        else if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            InterstitialSignalRequest.Builder builder = new InterstitialSignalRequest.Builder( signalType )
                .setAdUnitId( adUnitId );
            return applyCommonRequestParameters( builder, parameters, context ).build();
        }
        else if ( adFormat == MaxAdFormat.NATIVE )
        {
            NativeSignalRequest.Builder builder = new NativeSignalRequest.Builder( signalType )
                .setAdUnitId( adUnitId );
            return applyCommonRequestParameters( builder, parameters, context ).build();
        }
        else if ( adFormat == MaxAdFormat.REWARDED )
        {
            RewardedSignalRequest.Builder builder = new RewardedSignalRequest.Builder( signalType )
                .setAdUnitId( adUnitId );
            return applyCommonRequestParameters( builder, parameters, context ).build();
        }
        else if ( adFormat == MaxAdFormat.APP_OPEN )
        {
            AppOpenSignalRequest.Builder builder = new AppOpenSignalRequest.Builder( signalType )
                .setAdUnitId( adUnitId );
            return applyCommonRequestParameters( builder, parameters, context ).build();
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format for signal collection: " + adFormat );
        }
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "interstitial ad: " + placementId + "..." );

        updateMuteState( parameters.getServerParameters() );

        Context context = getContext( activity );

        AdLoadCallback<InterstitialAd> loadCallback = new AdLoadCallback<InterstitialAd>()
        {
            @Override
            public void onAdLoaded(@NonNull final InterstitialAd ad)
            {
                log( "Interstitial ad loaded: " + placementId + "..." );
                interstitialAd = ad;

                interstitialAd.setAdEventCallback( new InterstitialAdListener( placementId, listener ) );

                ResponseInfo responseInfo = interstitialAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;

                AppLovinSdkUtils.runOnUiThread( () ->
                {
                    if ( AppLovinSdkUtils.isValidString( responseId ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", responseId );
                        listener.onInterstitialAdLoaded( extraInfo );
                    }
                    else
                    {
                        listener.onInterstitialAdLoaded();
                    }
                } );
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "Interstitial ad (" + placementId + ") failed to load with error: " + adapterError );
                AppLovinSdkUtils.runOnUiThread( () -> listener.onInterstitialAdLoadFailed( adapterError ) );
            }
        };

        if ( isBiddingAd )
        {
            InterstitialAd.loadFromAdResponse( bidResponse, loadCallback );
        }
        else
        {
            AdRequest.Builder requestBuilder = new AdRequest.Builder( placementId );
            AdRequest adRequest = applyCommonRequestParameters( requestBuilder, parameters, context ).build();
            InterstitialAd.load( adRequest, loadCallback );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + placementId + "..." );

        if ( interstitialAd != null )
        {
            if ( activity == null )
            {
                log( "Interstitial ad failed to show: Activity is null" );
                listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Activity is required to show GMA Next-Gen ads" ) );
                return;
            }
            interstitialAd.show( activity );
        }
        else
        {
            log( "Interstitial ad failed to show: " + placementId );

            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                         MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onInterstitialAdDisplayFailed( error );
        }
    }

    //endregion

    //region MaxAppOpenAdapter Methods

    @Override
    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "app open ad: " + placementId + "..." );

        updateMuteState( parameters.getServerParameters() );

        Context context = getContext( activity );

        AdLoadCallback<AppOpenAd> loadCallback = new AdLoadCallback<AppOpenAd>()
        {
            @Override
            public void onAdLoaded(@NonNull final AppOpenAd ad)
            {
                log( "App open ad loaded: " + placementId );
                appOpenAd = ad;
                appOpenAdListener = new AppOpenAdListener( placementId, listener );
                appOpenAd.setAdEventCallback( appOpenAdListener );

                ResponseInfo responseInfo = appOpenAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;

                AppLovinSdkUtils.runOnUiThread( () ->
                {
                    if ( AppLovinSdkUtils.isValidString( responseId ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", responseId );
                        listener.onAppOpenAdLoaded( extraInfo );
                    }
                    else
                    {
                        listener.onAppOpenAdLoaded();
                    }
                } );
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "App open ad (" + placementId + ") failed to load with error: " + adapterError );

                AppLovinSdkUtils.runOnUiThread( () -> listener.onAppOpenAdLoadFailed( adapterError ) );
            }
        };

        if ( isBiddingAd )
        {
            AppOpenAd.loadFromAdResponse( bidResponse, loadCallback );
        }
        else
        {
            AdRequest.Builder requestBuilder = new AdRequest.Builder( placementId );
            AdRequest adRequest = applyCommonRequestParameters( requestBuilder, parameters, context ).build();
            AppOpenAd.load( adRequest, loadCallback );
        }
    }


    @Override
    public void showAppOpenAd(@NonNull final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, @NonNull final MaxAppOpenAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing app open ad: " + placementId + "..." );

        if ( appOpenAd != null )
        {
            if ( activity == null )
            {
                log( "App open ad failed to show: Activity is null" );
                listener.onAppOpenAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Activity is required to show GMA Next-Gen ads" ) );
                return;
            }

            appOpenAd.show( activity );
        }
        else
        {
            log( "App open ad failed to show: " + placementId );

            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                         MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onAppOpenAdDisplayFailed( error );
        }
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "rewarded ad: " + placementId + "..." );

        updateMuteState( parameters.getServerParameters() );

        Context context = getContext( activity );

        AdLoadCallback<RewardedAd> loadCallback = new AdLoadCallback<RewardedAd>()
        {
            @Override
            public void onAdLoaded(@NonNull final RewardedAd ad)
            {
                log( "Rewarded ad loaded: " + placementId );
                rewardedAd = ad;
                rewardedAdListener = new RewardedAdListener( placementId, listener );
                rewardedAd.setAdEventCallback( rewardedAdListener );

                ResponseInfo responseInfo = rewardedAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;

                AppLovinSdkUtils.runOnUiThread( () ->
                {
                    if ( AppLovinSdkUtils.isValidString( responseId ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", responseId );
                        listener.onRewardedAdLoaded( extraInfo );
                    }
                    else
                    {
                        listener.onRewardedAdLoaded();
                    }
                } );
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "Rewarded ad (" + placementId + ") failed to load with error: " + adapterError );

                AppLovinSdkUtils.runOnUiThread( () -> listener.onRewardedAdLoadFailed( adapterError ) );
            }
        };

        if ( isBiddingAd )
        {
            RewardedAd.loadFromAdResponse( bidResponse, loadCallback );
        }
        else
        {
            AdRequest.Builder requestBuilder = new AdRequest.Builder( placementId );
            AdRequest adRequest = applyCommonRequestParameters( requestBuilder, parameters, context ).build();
            RewardedAd.load( adRequest, loadCallback );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad: " + placementId + "..." );

        if ( rewardedAd != null )
        {
            if ( activity == null )
            {
                log( "Rewarded ad failed to show: Activity is null" );
                listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Activity is required to show GMA Next-Gen ads" ) );
                return;
            }

            configureReward( parameters );

           rewardedAd.show( activity, rewardItem -> {
                log( "Rewarded ad user earned reward: " + placementId );
                rewardedAdListener.hasGrantedReward = true;
            } );
        }
        else
        {
            log( "Rewarded ad failed to show: " + placementId );

            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                         MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onRewardedAdDisplayFailed( error );
        }
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @SuppressLint("MissingPermission")
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for placement id: " + placementId + "..." );

        Context context = getContext( activity );

        if ( isNative )
        {
            NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( parameters, adFormat, activity, listener );
            if ( isBiddingAd )
            {
                NativeAdLoader.loadFromAdResponse( bidResponse, nativeAdViewListener );
            }
            else
            {
                NativeAdRequest.Builder requestBuilder = new NativeAdRequest.Builder( placementId, Arrays.asList( NativeAd.NativeAdType.NATIVE ) )
                    .setAdChoicesPlacement( getAdChoicesPlacement( parameters ) );
                NativeAdRequest nativeAdRequest = applyCommonRequestParameters( requestBuilder, parameters, context ).build();
                NativeAdLoader.load( nativeAdRequest, nativeAdViewListener );
            }
        }
        else
        {
            adView = new AdView( context );
            final boolean isAdaptiveBanner = parameters.getServerParameters().getBoolean( "adaptive_banner", false );
            AdSize adSize = toAdSize( adFormat, isAdaptiveBanner, parameters, context );

            AdViewListener adViewListener = new AdViewListener( placementId, adFormat, adView, activity, adSize, listener );

            if ( isBiddingAd )
            {
                adView.loadFromAdResponse( bidResponse, adViewListener );
            }
            else
            {
                BannerAdRequest.Builder requestBuilder = new BannerAdRequest.Builder( placementId, adSize );
                BannerAdRequest adRequest = applyCommonRequestParameters( requestBuilder, parameters, context ).build();

                adView.loadAd( adRequest, adViewListener );
            }
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    @SuppressLint("MissingPermission")
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + " native ad for placement id: " + placementId + "..." );

        Context context = getContext( activity );

        NativeAdListener nativeAdListener = new NativeAdListener( parameters, context, listener );

        if ( isBiddingAd )
        {
            NativeAdLoader.loadFromAdResponse( bidResponse, nativeAdListener );
        }
        else
        {
            NativeAdRequest.Builder requestBuilder = new NativeAdRequest.Builder( placementId, Arrays.asList( NativeAd.NativeAdType.NATIVE ) )
                .setAdChoicesPlacement( getAdChoicesPlacement( parameters ) );
            NativeAdRequest nativeAdRequest = applyCommonRequestParameters( requestBuilder, parameters, context ).build();
            NativeAdLoader.load( nativeAdRequest, nativeAdListener );
        }
    }

    //endregion

    //region Helper Methods

    private static MaxAdapterError toMaxError(final LoadAdError googleAdsError)
    {
        LoadAdError.ErrorCode googleErrorCode = googleAdsError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( googleErrorCode )
        {
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case INVALID_REQUEST:
            case REQUEST_ID_MISMATCH:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case APP_ID_MISSING:
            case INVALID_AD_RESPONSE:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(),
            googleErrorCode.getValue(), googleAdsError.getMessage() );
    }

    private AdSize toAdSize(final MaxAdFormat adFormat,
                            final boolean isAdaptiveBanner,
                            final MaxAdapterParameters parameters,
                            final Context context)
    {
        if ( isAdaptiveBanner && isAdaptiveAdFormat( adFormat, parameters ) )
        {
            return getAdaptiveAdSize( parameters, context );
        }

        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.MEDIUM_RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private AdSize getAdaptiveAdSize(final MaxAdapterParameters parameters, final Context context)
    {
        final int bannerWidth = getAdaptiveBannerWidth( parameters, context );

        if ( isInlineAdaptiveBanner( parameters ) )
        {
            final int inlineMaxHeight = getInlineAdaptiveBannerMaxHeight( parameters );
            if ( inlineMaxHeight > 0 )
            {
                return AdSize.getInlineAdaptiveBannerAdSize( bannerWidth, inlineMaxHeight );
            }

            return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize( context, bannerWidth );
        }

        // Return anchored size by default
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize( context, bannerWidth );
    }

    private boolean isAdaptiveAdFormat(final MaxAdFormat adFormat, final MaxAdapterParameters parameters)
    {
        // Adaptive banners must be inline for MRECs
        final boolean isInlineAdaptiveMRec = ( adFormat == MaxAdFormat.MREC ) && isInlineAdaptiveBanner( parameters );
        return isInlineAdaptiveMRec || adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER;
    }

    private boolean isInlineAdaptiveBanner(final MaxAdapterParameters parameters)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        final Object adaptiveBannerType = localExtraParameters.get( "adaptive_banner_type" );
        return ( adaptiveBannerType instanceof String ) && ADAPTIVE_BANNER_TYPE_INLINE.equalsIgnoreCase( (String) adaptiveBannerType );
    }

    private int getInlineAdaptiveBannerMaxHeight(final MaxAdapterParameters parameters)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        final Object inlineMaxHeight = localExtraParameters.get( "inline_adaptive_banner_max_height" );
        return ( inlineMaxHeight instanceof Integer ) ? (int) inlineMaxHeight : 0;
    }

    private int getAdaptiveBannerWidth(final MaxAdapterParameters parameters, final Context context)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        Object widthObj = localExtraParameters.get( "adaptive_banner_width" );
        if ( widthObj instanceof Integer )
        {
            return (int) widthObj;
        }
        else if ( widthObj != null )
        {
            e( "Expected parameter \"adaptive_banner_width\" to be of type Integer, received: " + widthObj.getClass() );
        }

        int deviceWidthPx = getApplicationWindowWidth( context );
        return AppLovinSdkUtils.pxToDp( context, deviceWidthPx );
    }

    public static int getApplicationWindowWidth(final Context context)
    {
        WindowManager windowManager = (WindowManager) context.getSystemService( Context.WINDOW_SERVICE );
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics( outMetrics );
        return outMetrics.widthPixels;
    }

    private AdFormat toAdFormat(final MaxAdapterSignalCollectionParameters parameters)
    {
        MaxAdFormat adFormat = parameters.getAdFormat();
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" ) || adFormat == MaxAdFormat.NATIVE;
        if ( isNative )
        {
            return AdFormat.NATIVE;
        }
        else if ( adFormat.isAdViewAd() )
        {
            return AdFormat.BANNER;
        }
        else if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            return AdFormat.INTERSTITIAL;
        }
        else if ( adFormat == MaxAdFormat.REWARDED )
        {
            return AdFormat.REWARDED;
        }
        // NOTE: App open ads were added in AppLovin v11.5.0 and must be checked after all the other ad formats to avoid throwing an exception
        else if ( adFormat == MaxAdFormat.APP_OPEN )
        {
            return AdFormat.APP_OPEN_AD;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    @SuppressLint("ApplySharedPref")
    private <T extends BaseRequestBuilder<T>> T applyCommonRequestParameters(T builder, MaxAdapterParameters parameters, Context context)
    {
        Bundle serverParameters = parameters.getServerParameters();
        Bundle networkExtras = new Bundle( 6 );

        String bidderType = BundleUtils.getString( "bidder", "", serverParameters );
        boolean isDv360Bidding = "dv360".equalsIgnoreCase( bidderType );

        // Use "applovin" instead of mediationTag for Google's specs
        // "applovin_dv360" is for DV360_BIDDING, which is a separate bidder from regular ADMOB_BIDDING
        builder.setRequestAgent( isDv360Bidding ? "applovin_dv360" : "applovin" );

        // Use event id as AdMob's placement request id
        String eventId = BundleUtils.getString( "event_id", serverParameters );
        if ( AppLovinSdkUtils.isValidString( eventId ) )
        {
            networkExtras.putString( "placement_req_id", eventId );
        }

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null && !hasUserConsent )
        {
            networkExtras.putString( "npa", "1" ); // Non-personalized ads
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null && isDoNotSell )
        {
            networkExtras.putInt( "rdp", 1 ); // Restrict data processing - https://developers.google.com/admob/android/ccpa

            PreferenceManager.getDefaultSharedPreferences( context )
                    .edit()
                    .putInt( "gad_rdp", 1 )
                    .apply();
        }
        else
        {
            PreferenceManager.getDefaultSharedPreferences( context )
                    .edit()
                    .remove( "gad_rdp" )
                    .apply();
        }

        Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();

        Object maxContentRating = localExtraParameters.get( "google_max_ad_content_rating" );
        if ( maxContentRating instanceof String )
        {
            networkExtras.putString( "max_ad_content_rating", (String) maxContentRating );
        }

        Object contentUrlString = localExtraParameters.get( "google_content_url" );
        if ( contentUrlString instanceof String )
        {
            builder.setContentUrl( ( String ) contentUrlString );
        }

        Object neighbouringContentUrlStringsObject = localExtraParameters.get( "google_neighbouring_content_url_strings" );
        if ( neighbouringContentUrlStringsObject instanceof List )
        {
            try
            {
                builder.setNeighboringContentUrls( new HashSet<>( ( List<String> ) neighbouringContentUrlStringsObject ) );
            }
            catch ( Throwable th )
            {
                e( "Neighbouring content URL strings extra param needs to be of type List<String>.", th );
            }
        }

        // --- Google Ad Manager Specific Parameters ---

        Object publisherProvidedId = localExtraParameters.get( "ppid" );
        if ( publisherProvidedId instanceof String )
        {
            builder.setPublisherProvidedId( (String) publisherProvidedId );
        }

        Object customTargetingDataObject = localExtraParameters.get( "custom_targeting" );
        if ( customTargetingDataObject instanceof Map )
        {
            // try-catching unsafe cast to List<String> or Map<String, Object> in case an incorrect type is set.
            try
            {
                Map<String, Object> customTargetingDataMap = ( Map<String, Object> ) customTargetingDataObject;
                for ( String key : customTargetingDataMap.keySet() )
                {
                    Object value = customTargetingDataMap.get( key );
                    if ( value instanceof String )
                    {
                        builder.putCustomTargeting( key, ( String ) value );
                    }
                    else if ( value instanceof List )
                    {
                        builder.putCustomTargeting( key, ( List<String> ) value );
                    }
                    else
                    {
                        e( "Object in the map needs to be either of type String or List<String>." );
                    }
                }
            }
            catch ( Throwable th )
            {
                e( "Custom targeting extra param value needs to be of type Map<String, Object>.", th );
            }
        }

        Object googleExtraParamsObject = localExtraParameters.get( "google_extra_params" );
        if ( googleExtraParamsObject instanceof Map )
        {
            try
            {
                Map<String, ?> googleExtraParamsMap = ( Map<String, ?> ) googleExtraParamsObject;
                Bundle extraBundle = toBundle( googleExtraParamsMap );
                networkExtras.putAll( extraBundle );
                log( "Adding google_extra_params to network extras: " + extraBundle );
            }
            catch ( Throwable th )
            {
                e( "google_extra_params must be Map<String, ?>.", th );
            }
        }

        // --- Google Ad Manager Specific Parameters end ---

        builder.setGoogleExtrasBundle( networkExtras );

        return builder;
    }

    /**
     * Update the global mute state for AdMob - must be done _before_ ad load to restrict inventory which requires playing with volume.
     */
    private static void updateMuteState(final Bundle serverParameters)
    {
        if ( serverParameters.containsKey( "is_muted" ) )
        {
            MobileAds.setUserMutedApp( serverParameters.getBoolean( "is_muted" ) );
        }
    }

    private AdChoicesPlacement getAdChoicesPlacement(MaxAdapterResponseParameters parameters)
    {
        // Publishers can set via nativeAdLoader.setLocalExtraParameter( "admob_ad_choices_placement", ADCHOICES_BOTTOM_LEFT );
        final Map<String, Object> localExtraParams = parameters.getLocalExtraParameters();
        final Object adChoicesPlacementObj = localExtraParams != null ? localExtraParams.get( "admob_ad_choices_placement" ) : null;

        if ( adChoicesPlacementObj instanceof Integer )
        {
            int placement = (Integer) adChoicesPlacementObj;

            if ( placement == AdChoicesPlacement.TOP_LEFT.getValue() )
            {
                return AdChoicesPlacement.TOP_LEFT;
            }
            else if ( placement == AdChoicesPlacement.BOTTOM_LEFT.getValue() )
            {
                return AdChoicesPlacement.BOTTOM_LEFT;
            }
            else if ( placement == AdChoicesPlacement.BOTTOM_RIGHT.getValue() )
            {
                return AdChoicesPlacement.BOTTOM_RIGHT;
            }
        }

        return AdChoicesPlacement.TOP_RIGHT;
    }

    private boolean isValidAdChoicesPlacement(Object placementObj)
    {
        return ( placementObj instanceof Integer ) &&
                ( (Integer) placementObj == AdChoicesPlacement.TOP_LEFT.getValue() ||
                        (Integer) placementObj == AdChoicesPlacement.TOP_RIGHT.getValue() ||
                        (Integer) placementObj == AdChoicesPlacement.BOTTOM_LEFT.getValue() ||
                        (Integer) placementObj == AdChoicesPlacement.BOTTOM_RIGHT.getValue() );
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    //endregion

    private class InterstitialAdListener
        implements InterstitialAdEventCallback
    {
        private final String placementId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String placementId, final MaxInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Interstitial ad shown: " + placementId );
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final FullScreenContentError fullScreenContentError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                fullScreenContentError.getCode().getValue(),
                                                                fullScreenContentError.getMessage() );
            log( "Interstitial ad (" + placementId + ") failed to show with error: " + adapterError );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onInterstitialAdDisplayFailed( adapterError ) );
        }

        @Override
        public void onAdImpression()
        {
            log( "Interstitial ad impression recorded: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onInterstitialAdDisplayed() );
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onInterstitialAdClicked() );
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( "Interstitial ad hidden: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onInterstitialAdHidden() );
        }
    }

    private class AppOpenAdListener
            implements AppOpenAdEventCallback
    {
        private final String placementId;
        private final MaxAppOpenAdapterListener listener;

        AppOpenAdListener(final String placementId, final MaxAppOpenAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "App open ad shown: " + placementId );
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                fullScreenContentError.getCode().getValue(),
                                                                fullScreenContentError.getMessage() );
            log( "App open ad (" + placementId + ") failed to show with error: " + adapterError );

            AppLovinSdkUtils.runOnUiThread( () -> listener.onAppOpenAdDisplayFailed( adapterError ) );
        }

        @Override
        public void onAdImpression()
        {
            log( "App open ad impression recorded: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAppOpenAdDisplayed() );
        }

        @Override
        public void onAdClicked()
        {
            log( "App open ad clicked: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAppOpenAdClicked() );
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( "App open ad hidden: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAppOpenAdHidden() );
        }
    }

    private class RewardedAdListener
            implements RewardedAdEventCallback
    {
        private final String placementId;
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(final String placementId, final MaxRewardedAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Rewarded ad shown: " + placementId );
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull FullScreenContentError fullScreenContentError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                fullScreenContentError.getCode().getValue(),
                                                                fullScreenContentError.getMessage() );
            log( "Rewarded ad (" + placementId + ") failed to show with error: " + adapterError );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onRewardedAdDisplayFailed( adapterError ) );
        }

        @Override
        public void onAdImpression()
        {
            log( "Rewarded ad impression recorded: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onRewardedAdDisplayed() );
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onRewardedAdClicked() );
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                AppLovinSdkUtils.runOnUiThread( () -> listener.onUserRewarded( reward ) );
            }

            log( "Rewarded ad hidden: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onRewardedAdHidden() );
        }
    }

    private class AdViewListener
            implements AdLoadCallback<BannerAd>, BannerAdEventCallback
    {
        final String placementId;
        final MaxAdFormat adFormat;
        final MaxAdViewAdapterListener listener;
        final AdView adView;
        final Activity activity;
        final AdSize adSize;
        AdViewListener(final String placementId, final MaxAdFormat adFormat, final AdView adView, @Nullable final Activity activity, final AdSize adSize, final MaxAdViewAdapterListener listener)
        {
            this.placementId = placementId;
            this.adFormat = adFormat;
            this.adView = adView;
            this.activity = activity;
            this.adSize = adSize;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BannerAd bannerAd)
        {
            log( adFormat.getLabel() + " ad loaded: " + placementId );

            bannerAd.setAdEventCallback( this );

            ResponseInfo responseInfo = bannerAd.getResponseInfo();
            String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;

            AdSize adSize = bannerAd.getAdSize();

            AppLovinSdkUtils.runOnUiThread( () ->
            {
                Bundle extraInfo = new Bundle( 3 );

                if ( AppLovinSdkUtils.isValidString( responseId ) )
                {
                    extraInfo.putString( "creative_id", responseId );
                }

                if ( adSize != null )
                {
                    extraInfo.putInt( "ad_width", adSize.getWidth() );
                    extraInfo.putInt( "ad_height", adSize.getHeight() );
                }

                listener.onAdViewAdLoaded( adView, extraInfo );
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
        {
            MaxAdapterError adapterError = toMaxError( loadAdError );
            log( adFormat.getLabel() + " ad (" + placementId + ") failed to load with error code: " + adapterError );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAdViewAdLoadFailed( adapterError ) );
        }

        @Override
        public void onAdImpression()
        {
            log( adFormat.getLabel() + " ad shown: " + placementId );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAdViewAdDisplayed() );
        }

        @Override
        public void onAdClicked()
        {
            log( adFormat.getLabel() + " ad clicked" );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAdViewAdClicked() );
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( adFormat.getLabel() + " ad opened full screen" );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAdViewAdExpanded() );
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( adFormat.getLabel() + " ad closed full screen" );
            AppLovinSdkUtils.runOnUiThread( () -> listener.onAdViewAdCollapsed() );
        }
    }

    private class NativeAdViewListener
            implements NativeAdLoaderCallback, NativeAdEventCallback
    {
        final String                   placementId;
        final MaxAdFormat              adFormat;
        final Bundle                   serverParameters;
        final WeakReference<Activity>  activityRef;
        final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
        {
            this.placementId = parameters.getThirdPartyAdPlacementId();
            this.serverParameters = parameters.getServerParameters();
            this.activityRef = new WeakReference<>( activity );
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(@NonNull final NativeAd nativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded: " + placementId );

            GoogleMediationAdapter.this.nativeAd = nativeAd;

            nativeAd.setAdEventCallback( this );

            final Activity activity = activityRef.get();
            final Context context = getContext( activity );

            final MediaView mediaView = new MediaView( context );
            MediaContent mediaContent = nativeAd.getMediaContent();
            if ( mediaContent != null )
            {
                mediaView.setMediaContent( mediaContent );
            }

            final Image icon = nativeAd.getIcon();
            MaxNativeAd.MaxNativeAdImage maxNativeAdImage = null;
            if ( icon != null )
            {
                if ( icon.getDrawable() != null )
                {
                    maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( icon.getDrawable() );
                }
                else
                {
                    maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( icon.getUri() );
                }
            }

            final MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                    .setAdFormat( adFormat )
                    .setTitle( nativeAd.getHeadline() )
                    .setBody( nativeAd.getBody() )
                    .setCallToAction( nativeAd.getCallToAction() )
                    .setIcon( maxNativeAdImage )
                    .setMediaView( mediaView )
                    .build();

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final String templateName = BundleUtils.getString( "template", "", serverParameters );
                    final MaxNativeAdView maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, context );

                    nativeAdView = new NativeAdView( context );
                    nativeAdView.setIconView( maxNativeAdView.getIconImageView() );
                    nativeAdView.setHeadlineView( maxNativeAdView.getTitleTextView() );
                    nativeAdView.setBodyView( maxNativeAdView.getBodyTextView() );
                    nativeAdView.setCallToActionView( maxNativeAdView.getCallToActionButton() );

                    nativeAdView.addView( maxNativeAdView );

                    nativeAdView.registerNativeAd( nativeAd, mediaView );

                    ResponseInfo responseInfo = nativeAd.getResponseInfo();
                    String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                    if ( AppLovinSdkUtils.isValidString( responseId ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", responseId );

                        listener.onAdViewAdLoaded( nativeAdView, extraInfo );
                    }
                    else
                    {
                        listener.onAdViewAdLoaded( nativeAdView );
                    }
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
        {
            MaxAdapterError adapterError = toMaxError( loadAdError );
            log( "Native " + adFormat.getLabel() + " ad (" + placementId + ") failed to load with error: " + adapterError );
            runOnUiThread( () -> listener.onAdViewAdLoadFailed( adapterError ) );
        }

        @Override
        public void onAdImpression()
        {
            log( "Native " + adFormat.getLabel() + " ad shown" );
            runOnUiThread( () -> listener.onAdViewAdDisplayed() );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native " + adFormat.getLabel() + " ad clicked" );
            runOnUiThread( () -> listener.onAdViewAdClicked() );
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Native " + adFormat.getLabel() + " ad opened" );
            runOnUiThread( () -> listener.onAdViewAdExpanded() );
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( "Native " + adFormat.getLabel() + " ad closed" );
            runOnUiThread( () -> listener.onAdViewAdCollapsed() );
        }
    }

    private class NativeAdListener
            implements NativeAdLoaderCallback, NativeAdEventCallback
    {
        final String                     placementId;
        final Bundle                     serverParameters;
        final Context                    context;
        final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();

            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad loaded: " + placementId );

            GoogleMediationAdapter.this.nativeAd = nativeAd;

            nativeAd.setAdEventCallback( this );

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getHeadline() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                runOnUiThread( () -> listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS ) );

                return;
            }

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    View mediaView = null;
                    MediaContent mediaContent = nativeAd.getMediaContent();
                    Image image = nativeAd.getImage();
                    Drawable mainImage = null;
                    float mediaContentAspectRatio = 0.0f;

                    if ( mediaContent != null )
                    {
                        MediaView googleMediaView = new MediaView( context );
                        googleMediaView.setMediaContent( mediaContent );
                        mediaView = googleMediaView;

                        mainImage = mediaContent.getMainImage();
                        mediaContentAspectRatio = mediaContent.getAspectRatio();
                    }
                    else if ( image != null )
                    {
                        ImageView mediaImageView = new ImageView( context );
                        Drawable mediaImageDrawable = image.getDrawable();

                        if ( mediaImageDrawable != null )
                        {
                            mediaImageView.setImageDrawable( mediaImageDrawable );
                            mediaView = mediaImageView;

                            mediaContentAspectRatio = (float) mediaImageDrawable.getIntrinsicWidth() / (float) mediaImageDrawable.getIntrinsicHeight();
                        }
                    }

                    Image icon = nativeAd.getIcon();
                    MaxNativeAd.MaxNativeAdImage iconImage = null;
                    if ( icon != null )
                    {
                        if ( icon.getDrawable() != null )
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( icon.getDrawable() );
                        }
                        else
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( icon.getUri() );
                        }
                    }

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getHeadline() )
                            .setAdvertiser( nativeAd.getAdvertiser() )
                            .setBody( nativeAd.getBody() )
                            .setCallToAction( nativeAd.getCallToAction() )
                            .setIcon( iconImage )
                            .setMediaView( mediaView )
                            .setMainImage( new MaxNativeAd.MaxNativeAdImage( mainImage ) )
                            .setMediaContentAspectRatio( mediaContentAspectRatio )
                            .setStarRating( nativeAd.getStarRating() );

                    MaxNativeAd maxNativeAd = new MaxGoogleNativeAd( builder );

                    ResponseInfo responseInfo = nativeAd.getResponseInfo();
                    String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", responseId );

                    listener.onNativeAdLoaded( maxNativeAd, extraInfo );
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
        {
            MaxAdapterError adapterError = toMaxError( loadAdError );
            log( "Native ad (" + placementId + ") failed to load with error: " + adapterError );
            runOnUiThread( () -> listener.onNativeAdLoadFailed( adapterError ) );
        }

        @Override
        public void onAdImpression()
        {
            log( "Native ad shown" );
            runOnUiThread( () -> listener.onNativeAdDisplayed( null ) );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad clicked" );
            runOnUiThread( () -> listener.onNativeAdClicked() );
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Native ad opened overlay" );
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( "Native ad closed overlay" );
        }
    }

    private class MaxGoogleNativeAd
            extends MaxNativeAd
    {
        public MaxGoogleNativeAd(final Builder builder) { super( builder ); }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAd nativeAd = GoogleMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            nativeAdView = new NativeAdView( container.getContext() );

            // Native integrations
            if ( container instanceof MaxNativeAdView )
            {
                MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;

                // The Google Native Ad View needs to be wrapped around the main native ad view.
                View mainView = maxNativeAdView.getMainView();
                maxNativeAdView.removeView( mainView );
                nativeAdView.addView( mainView );
                maxNativeAdView.addView( nativeAdView );

                nativeAdView.setIconView( maxNativeAdView.getIconImageView() );
                nativeAdView.setHeadlineView( maxNativeAdView.getTitleTextView() );
                nativeAdView.setAdvertiserView( maxNativeAdView.getAdvertiserTextView() );
                nativeAdView.setBodyView( maxNativeAdView.getBodyTextView() );
                nativeAdView.setCallToActionView( maxNativeAdView.getCallToActionButton() );

                View mediaView = getMediaView();
                MediaView googleMediaView = null;

                if ( mediaView instanceof MediaView )
                {
                    googleMediaView = (MediaView) mediaView;
                }

                nativeAdView.registerNativeAd( nativeAd, googleMediaView );
            }
            // Plugins
            else
            {
                View mediaView = null;

                for ( View view : clickableViews )
                {
                    Object viewTag = view.getTag();
                    if ( viewTag == null ) continue;

                    int tag = (int) viewTag;

                    if ( tag == TITLE_LABEL_TAG )
                    {
                        nativeAdView.setHeadlineView( view );
                    }
                    else if ( tag == ICON_VIEW_TAG )
                    {
                        nativeAdView.setIconView( view );
                    }
                    else if ( tag == BODY_VIEW_TAG )
                    {
                        nativeAdView.setBodyView( view );
                    }
                    else if ( tag == CALL_TO_ACTION_VIEW_TAG )
                    {
                        nativeAdView.setCallToActionView( view );
                    }
                    else if ( tag == ADVERTISER_VIEW_TAG )
                    {
                        nativeAdView.setAdvertiserView( view );
                    }
                    else if ( tag == MEDIA_VIEW_CONTAINER_TAG )
                    {
                        mediaView = getMediaView();
                    }
                }

                //
                // Logic required for proper media view rendering in plugins (e.g. Flutter / React Native)
                //

                ViewGroup pluginContainer = ( mediaView != null ) ? (ViewGroup) mediaView.getParent() : container;
                if ( pluginContainer == null ) return true;

                // NOTE: Will be false for React Native (will extend `ReactViewGroup`), but true for Flutter
                boolean hasPluginLayout = ( pluginContainer instanceof RelativeLayout || pluginContainer instanceof FrameLayout );

                MediaView googleMediaView = null;

                // Handle re-parenting of mediaView for enabling clicks on other asset views
                if ( mediaView != null )
                {
                    // Remove mediaView from the plugin container
                    pluginContainer.removeView( mediaView );

                    if ( mediaView instanceof MediaView )
                    {
                        googleMediaView = (MediaView) mediaView;

                        // Special handling for Google MediaView on React Native
                        if ( !hasPluginLayout )
                        {
                            MediaContent googleMediaContent = googleMediaView.getMediaContent();

                            if ( googleMediaContent != null && googleMediaContent.getHasVideoContent() )
                            {
                                AutoMeasuringFrameLayout wrapper = new AutoMeasuringFrameLayout( pluginContainer.getContext() );
                                wrapper.addView( googleMediaView, new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );

                                mediaView = wrapper;
                            }
                        }
                    }

                    // Add mediaView to the NativeAdView
                    ViewGroup.LayoutParams mediaViewLayout = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    nativeAdView.addView( mediaView, mediaViewLayout );
                }
                // Insert a placeholder view for enabling clicks on other asset views
                else
                {
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    View view = new View( pluginContainer.getContext() );

                    nativeAdView.addView( view, layoutParams );
                    nativeAdView.setStoreView( view );
                }

                nativeAdView.registerNativeAd( nativeAd, googleMediaView );

                // Add the NativeAdView back to the plugin container

                if ( hasPluginLayout )
                {
                    ViewGroup.LayoutParams nativeAdViewLayout = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    pluginContainer.addView( nativeAdView, nativeAdViewLayout );
                }
                else
                {
                    nativeAdView.measure(
                            View.MeasureSpec.makeMeasureSpec( pluginContainer.getWidth(), View.MeasureSpec.EXACTLY ),
                            View.MeasureSpec.makeMeasureSpec( pluginContainer.getHeight(), View.MeasureSpec.EXACTLY ) );
                    nativeAdView.layout( 0, 0, pluginContainer.getWidth(), pluginContainer.getHeight() );
                    pluginContainer.addView( nativeAdView );
                }
            }

            return true;
        }
    }

    private static class AutoMeasuringFrameLayout
            extends FrameLayout
    {
        AutoMeasuringFrameLayout(final Context context) { super( context ); }

        @Override
        protected void onAttachedToWindow()
        {
            super.onAttachedToWindow();
            requestLayout();
        }

        @Override
        public void requestLayout()
        {
            super.requestLayout();
            post( () -> {
                measure(
                        MeasureSpec.makeMeasureSpec( getWidth(), MeasureSpec.EXACTLY ),
                        MeasureSpec.makeMeasureSpec( getHeight(), MeasureSpec.EXACTLY ) );
                layout( getLeft(), getTop(), getRight(), getBottom() );
            } );
        }
    }
}