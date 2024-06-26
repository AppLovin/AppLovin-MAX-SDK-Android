package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.impl.sdk.utils.StringUtils;
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
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.bytedance.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize;
import com.bytedance.sdk.openadsdk.api.init.BiddingTokenCallback;
import com.bytedance.sdk.openadsdk.api.init.PAGConfig;
import com.bytedance.sdk.openadsdk.api.init.PAGSdk;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener;
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGVideoAdListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdLoadListener;
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener;
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

/**
 * Created by Thomas So on April 14 2020
 */
public class ByteDanceMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter,/* MaxAppOpenAdapter */ MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    //region Error Code Map
    // Pangle error codes: https://partner.oceanengine.com/union/media/union/download/detail?id=5&docId=5de8daa6b1afac0012933137&osType=android
    private static final int OK                                     = 20000;
    private static final int NO_AD                                  = 20001;
    private static final int CONTENT_TYPE                           = 40000;
    private static final int REQUEST_PB_ERROR                       = 40001;
    private static final int APP_EMPTY                              = 40002;
    private static final int WAP_EMPTY                              = 40003;
    private static final int ADSLOT_EMPTY                           = 40004;
    private static final int ADSLOT_SIZE_EMPTY                      = 40005;
    private static final int ADSLOT_ID_ERROR                        = 40006;
    private static final int ERROR_CODE_ADCOUNT_ERROR               = 40007;
    private static final int ERROR_IMAGE_SIZE                       = 40008;
    private static final int ERROR_MEDIA_ID                         = 40009;
    private static final int ERROR_MEDIA_TYPE                       = 40010;
    private static final int ERROR_AD_TYPE                          = 40011;
    private static final int ERROR_ACCESS_METHOD_PASS               = 40012;
    private static final int ERROR_SPLASH_AD_TYPE                   = 40013;
    private static final int ERROR_REDIRECT                         = 40014;
    private static final int ERROR_REQUEST_INVALID                  = 40015;
    private static final int ERROR_SLOT_ID_APP_ID_DIFFER            = 40016;
    private static final int ERROR_ACCESS_METHOD_API_SDK            = 40017;
    private static final int ERROR_PACKAGE_NAME                     = 40018;
    private static final int ERROR_ADTYPE_DIFFER                    = 40019;
    private static final int ERROR_NEW_REGISTER_LIMIT               = 40020;
    private static final int ERROR_APK_SIGN_CHECK_ERROR             = 40021;
    private static final int ERROR_ORIGIN_AD_ERROR                  = 40022;
    private static final int ERROR_UNION_OS_ERROR                   = 40023;
    private static final int ERROR_UNION_SDK_TOO_OLD                = 40024;
    private static final int ERROR_UNION_SDK_NOT_INSTALLED          = 40025;
    private static final int ERROR_TEMPLATE_METHODS                 = 40029;
    private static final int SYS_ERROR                              = 50001;
    private static final int ROR_CODE_SHOW_EVENT_ERROR              = 60001;
    private static final int ERROR_CODE_CLICK_EVENT_ERROR           = 60002;
    private static final int ERROR_VERIFY_REWARD                    = 60007;
    private static final int PARSE_FAIL                             = -1;
    private static final int NET_ERROR                              = -2;
    private static final int NO_AD_PARSE                            = -3;
    private static final int AD_DATA_ERROR                          = -4;
    private static final int BANNER_AD_LOAD_IMAGE_ERROR             = -5;
    private static final int INSERT_AD_LOAD_IMAGE_ERROR             = -6;
    private static final int SPLASH_AD_LOAD_IMAGE_ERROR             = -7;
    private static final int FREQUENT_CALL_ERROR                    = -8;
    private static final int REQUEST_BODY_ERROR                     = -9;
    private static final int SPLASH_CACHE_PARSE_ERROR               = -10;
    private static final int SPLASH_CACHE_EXPIRED_ERROR             = -11;
    private static final int SPLASH_NOT_HAVE_CACHE_ERROR            = -12;
    private static final int FAIL_PARSE_RENDERING_RESULT_DATA_ERROR = 101;
    private static final int INVALID_MAIN_TEMPLATE_ERROR            = 102;
    private static final int INVALID_TEMPLATE_DIFFERENCE_ERROR      = 103;
    private static final int ABNORMAL_MATERIAL_DATA_ERROR           = 104;
    private static final int TEMPLATE_DATA_PARSING_ERROR            = 105;
    private static final int RENDERING_ERROR                        = 106;
    private static final int RENDERING_TIMEOUT_ERROR                = 107;
    //endregion

    private static final AtomicBoolean        initialized                        = new AtomicBoolean();
    private static       InitializationStatus status;
    private static final int                  DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 10;
    private static final ExecutorService      executor                           = Executors.newCachedThreadPool();

    private PAGInterstitialAd interstitialAd;
    private PAGAppOpenAd      appOpenAd;
    private PAGRewardedAd     rewardedAd;
    private PAGBannerAd       adViewAd;
    private PAGNativeAd       nativeAd;

    private InterstitialAdListener interstitialAdListener;
    private AppOpenAdListener      appOpenAdListener;
    private RewardedAdListener     rewardedAdListener;
    private NativeAdListener       nativeAdListener;

    // Explicit default constructor declaration
    public ByteDanceMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;
            final Bundle serverParameters = parameters.getServerParameters();
            final String appId = serverParameters.getString( "app_id" );
            log( "Initializing SDK with app id: " + appId + "..." );

            PAGConfig.Builder builder = new PAGConfig.Builder();

            // Set mediation provider
            builder.setUserData( createAdConfigData( serverParameters, true ) );

            Boolean hasUserConsent = parameters.hasUserConsent();
            if ( hasUserConsent != null )
            {
                builder.setGDPRConsent( hasUserConsent ? 1 : 0 );
            }

            // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
            // https://support.google.com/googleplay/android-developer/answer/9283445?hl=en
            Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
            if ( isAgeRestrictedUser != null )
            {
                builder.setChildDirected( isAgeRestrictedUser ? 1 : 0 );
            }

            Boolean isDoNotSell = parameters.isDoNotSell();
            if ( isDoNotSell != null )
            {
                builder.setDoNotSell( isDoNotSell ? 1 : 0 );
            }

            PAGConfig adConfig = builder.appId( appId )
                    .debugLog( parameters.isTesting() )
                    .supportMultiProcess( false )
                    .build();

            PAGSdk.init( getContext( activity ), adConfig, new PAGSdk.PAGInitCallback()
            {
                @Override
                public void success()
                {
                    log( "SDK initialized" );

                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }

                @Override
                public void fail(int code, String msg)
                {
                    log( "SDK failed to initialize with code: " + code + " and message: " + msg );

                    status = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( status, msg );
                }
            } );
        }
        else
        {
            log( "attempted initialization already - marking initialization as completed" );
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return PAGSdk.getSDKVersion();
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
        interstitialAd = null;

        appOpenAdListener = null;
        appOpenAd = null;

        rewardedAdListener = null;
        rewardedAd = null;

        if ( adViewAd != null )
        {
            adViewAd.destroy();
            adViewAd = null;
        }

        nativeAd = null;
        nativeAdListener = null;
    }

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        PAGSdk.getBiddingToken( new BiddingTokenCallback()
        {
            @Override
            public void onBiddingTokenCollected(final String biddingToken)
            {
                if ( AppLovinSdkUtils.isValidString( biddingToken ) )
                {
                    log( "Signal collection successful" );
                    callback.onSignalCollected( biddingToken );
                }
                else
                {
                    log( "Failed to collect signal" );
                    callback.onSignalCollectionFailed( null );
                }
            }
        } );
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + "interstitial ad for code id \"" + codeId + "\"..." );

        PAGConfig.setUserData( createAdConfigData( parameters.getServerParameters(), false ) );

        PAGInterstitialRequest request = new PAGInterstitialRequest();

        if ( isBidding )
        {
            request.setAdString( bidResponse );
        }

        interstitialAdListener = new InterstitialAdListener( codeId, listener );
        PAGInterstitialAd.loadAd( codeId, request, interstitialAdListener );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for code id \"" + codeId + "\"..." );

        interstitialAd.setAdInteractionListener( interstitialAdListener );
        interstitialAd.show( activity );
    }

    //endregion

    //region MaxAppOpenAdapter Methods

    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + "app open ad for code id \"" + codeId + "\"..." );

        PAGConfig.setUserData( createAdConfigData( parameters.getServerParameters(), false ) );

        int appIconId = getContext( activity ).getApplicationInfo().icon;
        if ( appIconId <= 0 )
        {
            log( "App icon resource id could not be found" );
        }
        else
        {
            PAGConfig.setAppIconId( appIconId );
        }

        PAGAppOpenRequest request = new PAGAppOpenRequest();

        if ( isBidding )
        {
            request.setAdString( bidResponse );
        }

        appOpenAdListener = new AppOpenAdListener( codeId, listener );
        PAGAppOpenAd.loadAd( codeId, request, appOpenAdListener );
    }

    public void showAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Showing app open ad for code id \"" + codeId + "\"..." );

        appOpenAd.setAdInteractionListener( appOpenAdListener );
        appOpenAd.show( activity );
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + "rewarded ad for code id \"" + codeId + "\"..." );

        PAGConfig.setUserData( createAdConfigData( parameters.getServerParameters(), false ) );

        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put( "user_id", getWrappingSdk().getUserIdentifier() );

        PAGRewardedRequest request = new PAGRewardedRequest();
        request.setExtraInfo( extraInfo );

        if ( isBidding )
        {
            request.setAdString( bidResponse );
        }

        rewardedAdListener = new RewardedAdListener( codeId, listener );
        PAGRewardedAd.loadAd( codeId, request, rewardedAdListener );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for code id \"" + codeId + "\"..." );

        // Configure userReward from server.
        configureReward( parameters );

        rewardedAd.setAdInteractionListener( rewardedAdListener );
        rewardedAd.show( activity );
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        String bidResponse = parameters.getBidResponse();
        String codeId = parameters.getThirdPartyAdPlacementId();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for code id \"" + codeId + "\"..." );

        PAGConfig.setUserData( createAdConfigData( parameters.getServerParameters(), false ) );

        if ( isNative )
        {
            PAGNativeRequest request = new PAGNativeRequest();

            if ( isBidding )
            {
                request.setAdString( bidResponse );
            }

            NativeAdViewListener nativeListener = new NativeAdViewListener( parameters, adFormat, activity, listener );
            PAGNativeAd.loadAd( codeId, request, nativeListener );
        }
        else
        {
            AppLovinSdkUtils.Size adSize = adFormat.getSize();
            PAGBannerRequest request = new PAGBannerRequest( new PAGBannerSize( adSize.getWidth(), adSize.getHeight() ) );

            if ( isBidding )
            {
                request.setAdString( bidResponse );
            }

            AdViewListener adViewListener = new AdViewListener( codeId, adFormat, listener );
            PAGBannerAd.loadAd( codeId, request, adViewListener );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for code id \"" + codeId + "\"..." );

        PAGConfig.setUserData( createAdConfigData( parameters.getServerParameters(), false ) );

        // Minimum supported Android SDK version is 11.1.0+, previous version has `MaxNativeAdView` requiring an Activity context which might leak
        if ( AppLovinSdk.VERSION_CODE < 11010000 )
        {
            log( "Failing ad load for AppLovin SDK < 11.1.0 which requires an Activity context" );
            listener.onNativeAdLoadFailed( MaxAdapterError.UNSPECIFIED );
            return;
        }

        PAGNativeRequest request = new PAGNativeRequest();

        if ( isBiddingAd )
        {
            request.setAdString( bidResponse );
        }

        nativeAdListener = new NativeAdListener( parameters, getContext( activity ), listener );
        PAGNativeAd.loadAd( codeId, request, nativeAdListener );
    }

    //endregion

    //region Helper Methods

    private Callable<Drawable> createDrawableTask(final String imageUrl, final Resources resources)
    {
        return new Callable<Drawable>()
        {
            @Override
            public Drawable call() throws Exception
            {
                InputStream inputStream = new URL( imageUrl ).openStream();
                Bitmap imageData = BitmapFactory.decodeStream( inputStream );
                return new BitmapDrawable( resources, imageData );
            }
        };
    }

    private static MaxAdapterError toMaxError(final int byteDanceErrorCode, final String byteDanceErrorMessage)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( byteDanceErrorCode )
        {
            case OK: // Success
                throw new IllegalStateException( "Returned error code for success" );
            case NO_AD: // NO FILL
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case CONTENT_TYPE: // http content type error
            case REQUEST_PB_ERROR: // http request pb error
            case APP_EMPTY: // source_type =‘app’,request app can’t be empty
            case WAP_EMPTY: // source_type =‘wap’,request wap cannot be empty
            case ADSLOT_EMPTY: // Ad slot cannot be empty
            case ADSLOT_SIZE_EMPTY: // Ad slot size cannot be empty
            case ADSLOT_ID_ERROR: // Illegal ad ID
            case ERROR_CODE_ADCOUNT_ERROR: // Incorrect number of ads
            case ERROR_IMAGE_SIZE: // Image size error
            case ERROR_MEDIA_ID: // Media ID is illegal
            case ERROR_MEDIA_TYPE: // Media type is illegal
            case ERROR_AD_TYPE: // Ad type is illegal
            case ERROR_ACCESS_METHOD_PASS: // Media access type is illegal and has been deprecated
            case ERROR_SPLASH_AD_TYPE: // Code bit id is less than 900 million, but adType is not splash ad
            case ERROR_REDIRECT: // The redirect parameter is incorrect
            case ERROR_REQUEST_INVALID: // Media rectification exceeds deadline, request illegal
            case ERROR_SLOT_ID_APP_ID_DIFFER: // The relationship between slot_id and app_id is invalid.
            case ERROR_ACCESS_METHOD_API_SDK: // Media access type is not legal API / SDK
            case ERROR_PACKAGE_NAME: // Media package name is inconsistent with entry
            case ERROR_ADTYPE_DIFFER: // Media configuration ad type is inconsistent with request
            case ERROR_NEW_REGISTER_LIMIT: // The ad space registered by developers exceeds daily request limit
            case ERROR_APK_SIGN_CHECK_ERROR: // Apk signature sha1 value is inconsistent with media platform entry
            case ERROR_ORIGIN_AD_ERROR: // Whether the media request material is inconsistent with the media platform entry
            case ERROR_UNION_OS_ERROR:
            case ERROR_UNION_SDK_TOO_OLD:
            case ERROR_UNION_SDK_NOT_INSTALLED:
            case SYS_ERROR: // Server Error
            case ROR_CODE_SHOW_EVENT_ERROR: // Show event processing error
            case ERROR_CODE_CLICK_EVENT_ERROR: // Click event processing error
            case ERROR_VERIFY_REWARD: // Server abnormity or failure of rewarded video ad rewards verification
            case PARSE_FAIL: // Data parsing failed
            case NO_AD_PARSE: // Parsing data without ad
            case AD_DATA_ERROR: // Return data is missing the necessary fields
            case BANNER_AD_LOAD_IMAGE_ERROR: // bannerAd image failed to load
            case INSERT_AD_LOAD_IMAGE_ERROR: // Interstitial ad image failed to load
            case SPLASH_AD_LOAD_IMAGE_ERROR: // Splash ad image failed to load
            case FREQUENT_CALL_ERROR: // Frequent request
            case REQUEST_BODY_ERROR: // Request entity is empty
            case SPLASH_CACHE_PARSE_ERROR: // Cache parsing failed
            case SPLASH_NOT_HAVE_CACHE_ERROR: // No splash ad in the cache
            case ERROR_TEMPLATE_METHODS:
            case FAIL_PARSE_RENDERING_RESULT_DATA_ERROR:
            case INVALID_MAIN_TEMPLATE_ERROR:
            case INVALID_TEMPLATE_DIFFERENCE_ERROR:
            case ABNORMAL_MATERIAL_DATA_ERROR:
            case TEMPLATE_DATA_PARSING_ERROR:
            case RENDERING_ERROR:
            case RENDERING_TIMEOUT_ERROR:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case SPLASH_CACHE_EXPIRED_ERROR: // Cache expired
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case NET_ERROR: // Network Error
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), byteDanceErrorCode, byteDanceErrorMessage );
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private String createAdConfigData(Bundle serverParameters, Boolean isInitializing)
    {
        if ( isInitializing )
        {
            return String.format( "[{\"name\":\"mediation\",\"value\":\"MAX\"},{\"name\":\"adapter_version\",\"value\":\"%s\"}]", getAdapterVersion() );
        }
        else
        {
            return String.format( "[{\"name\":\"mediation\",\"value\":\"MAX\"},{\"name\":\"adapter_version\",\"value\":\"%s\"},{\"name\":\"hybrid_id\",\"value\":\"%s\"}]", getAdapterVersion(), BundleUtils.getString( "event_id", serverParameters ) );
        }
    }

    //endregion

    private class InterstitialAdListener
            implements PAGInterstitialAdLoadListener, PAGInterstitialAdInteractionListener
    {
        private final String                         codeId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String codeId, final MaxInterstitialAdapterListener listener)
        {
            this.codeId = codeId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final PAGInterstitialAd ad)
        {
            if ( ad == null )
            {
                log( "Interstitial ad" + "(" + codeId + ")" + " NO FILL'd" );
                listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Interstitial ad loaded: " + codeId );
            interstitialAd = ad;

            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( "Interstitial ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowed()
        {
            log( "Interstitial ad displayed: " + codeId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked: " + codeId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdDismissed()
        {
            log( "Interstitial ad hidden: " + codeId );
            listener.onInterstitialAdHidden();
        }
    }

    private class AppOpenAdListener
            implements PAGAppOpenAdLoadListener, PAGAppOpenAdInteractionListener
    {
        private final String                    codeId;
        private final MaxAppOpenAdapterListener listener;

        AppOpenAdListener(final String codeId, final MaxAppOpenAdapterListener listener)
        {
            this.codeId = codeId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final PAGAppOpenAd ad)
        {
            if ( ad == null )
            {
                log( "App open ad" + "(" + codeId + ")" + " NO FILL'd" );
                listener.onAppOpenAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "App open ad loaded: " + codeId );
            appOpenAd = ad;

            listener.onAppOpenAdLoaded();
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( "App open ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onAppOpenAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowed()
        {
            log( "App open ad displayed: " + codeId );
            listener.onAppOpenAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "App open ad clicked: " + codeId );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdDismissed()
        {
            log( "App open ad hidden: " + codeId );
            listener.onAppOpenAdHidden();
        }
    }

    private class RewardedAdListener
            implements PAGRewardedAdLoadListener, PAGRewardedAdInteractionListener
    {
        private final String                     codeId;
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(final String codeId, final MaxRewardedAdapterListener listener)
        {
            this.codeId = codeId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final PAGRewardedAd ad)
        {
            if ( ad == null )
            {
                log( "Rewarded ad" + "(" + codeId + ")" + " NO FILL'd" );
                listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Rewarded ad loaded: " + codeId );
            rewardedAd = ad;

            listener.onRewardedAdLoaded();
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( "Rewarded ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowed()
        {
            log( "Rewarded ad displayed: " + codeId );

            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked: " + codeId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onUserEarnedReward(final PAGRewardItem rewardItem)
        {
            log( "Rewarded user with reward: " + rewardItem.getRewardAmount() + " " + rewardItem.getRewardName() );
            hasGrantedReward = true;
        }

        @Override
        public void onUserEarnedRewardFail(final int code, final String message)
        {
            log( "Failed to reward user with error: " + code + " " + message );
            hasGrantedReward = false;
        }

        @Override
        public void onAdDismissed()
        {
            log( "Rewarded ad hidden: " + codeId );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            implements PAGBannerAdLoadListener, PAGBannerAdInteractionListener
    {
        private final String                   codeId;
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final String codeId, final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.codeId = codeId;
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final PAGBannerAd ad)
        {
            if ( ad == null )
            {
                log( adFormat.getLabel() + " ad" + "(" + codeId + ")" + " NO FILL'd" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( adFormat.getLabel() + " ad (" + codeId + ") loaded" );
            adViewAd = ad;

            adViewAd.setAdInteractionListener( this );
            listener.onAdViewAdLoaded( ad.getBannerView() );
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( adFormat.getLabel() + " ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowed()
        {
            log( adFormat.getLabel() + " ad displayed: " + codeId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( adFormat.getLabel() + " ad clicked: " + codeId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdDismissed()
        {
            log( adFormat.getLabel() + " ad hidden: " + codeId );
            listener.onAdViewAdHidden();
        }
    }

    private class NativeAdViewListener
            implements PAGNativeAdLoadListener, PAGNativeAdInteractionListener, PAGVideoAdListener
    {
        final String                   codeId;
        final Bundle                   serverParameters;
        final MaxAdFormat              adFormat;
        final WeakReference<Activity>  activityRef;
        final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
        {
            this.codeId = parameters.getThirdPartyAdPlacementId();
            this.serverParameters = parameters.getServerParameters();
            this.adFormat = adFormat;
            this.activityRef = new WeakReference<>( activity );
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final PAGNativeAd nativeAdViewAd)
        {
            if ( nativeAdViewAd == null )
            {
                log( "Native " + adFormat.getLabel() + "ad" + "(" + codeId + ")" + " NO FILL'd" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded: " + codeId + ". Preparing assets..." );

            final PAGNativeAdData nativeAdData = nativeAdViewAd.getNativeAdData();
            final ExecutorService executorServiceToUse;
            if ( AppLovinSdk.VERSION_CODE >= 11000000 )
            {
                executorServiceToUse = getCachingExecutorService();
            }
            else
            {
                executorServiceToUse = executor;
            }

            final Activity activity = activityRef.get();
            final Context context = getContext( activity );
            executorServiceToUse.execute( new Runnable()
            {
                @Override
                public void run()
                {
                    final Resources resources = context.getResources();

                    // Create image fetching tasks to run asynchronously in the background
                    Future<Drawable> iconDrawableFuture = null;
                    if ( nativeAdData.getIcon() != null && StringUtils.isValidString( nativeAdData.getIcon().getImageUrl() ) )
                    {
                        final String imageUrl = nativeAdData.getIcon().getImageUrl();
                        // Pangle's image resource comes in the form of a URL which needs to be fetched in a non-blocking manner
                        log( "Adding native ad icon (" + imageUrl + ") to queue to be fetched" );

                        iconDrawableFuture = ( AppLovinSdk.VERSION_CODE >= 11000000 )
                                ? createDrawableFuture( imageUrl, resources )
                                : executorServiceToUse.submit( createDrawableTask( imageUrl, resources ) );
                    }

                    // Execute and timeout tasks if incomplete within the given time
                    int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
                    Drawable iconDrawable = null;
                    try
                    {
                        if ( iconDrawableFuture != null )
                        {
                            iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }
                    }
                    catch ( Throwable th )
                    {
                        e( "Image fetching tasks failed", th );
                    }

                    final MaxNativeAd.MaxNativeAdImage icon = iconDrawable != null ? new MaxNativeAd.MaxNativeAdImage( iconDrawable ) : null;

                    // Create MaxNativeAd after images are loaded from remote URLs
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            log( "Creating native ad with assets" );

                            MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                                    .setAdFormat( adFormat )
                                    .setTitle( nativeAdData.getTitle() )
                                    .setBody( nativeAdData.getDescription() )
                                    .setCallToAction( nativeAdData.getButtonText() )
                                    .setIcon( icon )
                                    .setOptionsView( nativeAdData.getAdLogoView() )
                                    .setMediaView( nativeAdData.getMediaView() )
                                    .build();

                            String templateName = BundleUtils.getString( "template", "", serverParameters );
                            if ( templateName.contains( "vertical" ) && AppLovinSdk.VERSION_CODE < 9140500 )
                            {
                                log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default horizontal native template will be used." );
                            }

                            MaxNativeAdView maxNativeAdView;
                            if ( AppLovinSdk.VERSION_CODE >= 11010000 )
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, context );
                            }
                            else
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, activity );
                            }

                            final List<View> clickableViews = new ArrayList<>( 4 );
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getTitle() ) && maxNativeAdView.getTitleTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getTitleTextView() );
                            }
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getBody() ) && maxNativeAdView.getBodyTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getBodyTextView() );
                            }
                            if ( maxNativeAd.getIcon() != null && maxNativeAdView.getIconImageView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getIconImageView() );
                            }
                            final View mediaContentView = ( AppLovinSdk.VERSION_CODE >= 11000000 ) ? maxNativeAdView.getMediaContentViewGroup() : maxNativeAdView.getMediaContentView();
                            if ( maxNativeAd.getMediaView() != null && mediaContentView != null )
                            {
                                clickableViews.add( mediaContentView );
                            }

                            // CTA button is considered a creative view
                            List<View> creativeViews = new ArrayList<>();
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
                            {
                                creativeViews.add( maxNativeAdView.getCallToActionButton() );
                            }

                            // Here dislikeView is null since it is optional
                            nativeAdViewAd.registerViewForInteraction( maxNativeAdView, clickableViews, creativeViews, null, NativeAdViewListener.this );

                            log( "Native " + adFormat.getLabel() + " ad fully loaded: " + codeId );
                            listener.onAdViewAdLoaded( maxNativeAdView );
                        }
                    } );
                }
            } );
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( "Native " + adFormat.getLabel() + " ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowed()
        {
            log( "Native " + adFormat.getLabel() + " ad displayed: " + codeId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Native " + adFormat.getLabel() + " ad clicked: " + codeId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onVideoAdPlay()
        {
            log( "Native " + adFormat.getLabel() + " ad video loaded" );
        }

        @Override
        public void onVideoAdPaused()
        {
            log( "Native " + adFormat.getLabel() + " ad video paused" );
        }

        @Override
        public void onVideoAdComplete()
        {
            log( "Native " + adFormat.getLabel() + " ad video completed" );
        }

        @Override
        public void onVideoError()
        {
            log( "Native " + adFormat.getLabel() + " ad video error" );
        }

        @Override
        public void onAdDismissed()
        {
            // This method won't be called until we implement `dislikeView` which is optional
            log( "Native " + adFormat.getLabel() + " ad hidden: " + codeId );
            listener.onAdViewAdHidden();
        }
    }

    private class NativeAdListener
            implements PAGNativeAdLoadListener, PAGNativeAdInteractionListener, PAGVideoAdListener
    {
        final String                     codeId;
        final Bundle                     serverParameters;
        final Context                    context;
        final MaxNativeAdAdapterListener listener;

        NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.codeId = parameters.getThirdPartyAdPlacementId();
            this.serverParameters = parameters.getServerParameters();
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final PAGNativeAd ad)
        {
            if ( ad == null )
            {
                log( "Native ad" + "(" + codeId + ")" + " NO FILL'd" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Native ad loaded: " + codeId + ". Preparing assets..." );

            final PAGNativeAdData nativeAdData = ad.getNativeAdData();
            ByteDanceMediationAdapter.this.nativeAd = ad;

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAdData.getTitle() ) )
            {
                e( "Native ad (" + ad + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            final ExecutorService cachingExecutorService = getCachingExecutorService();
            cachingExecutorService.execute( new Runnable()
            {
                @Override
                public void run()
                {
                    // Create image fetching tasks to run asynchronously in the background
                    Future<Drawable> iconDrawableFuture = null;
                    if ( nativeAdData.getIcon() != null && StringUtils.isValidString( nativeAdData.getIcon().getImageUrl() ) )
                    {
                        final String imageUrl = nativeAdData.getIcon().getImageUrl();

                        // Pangle's image resource comes in the form of a URL which needs to be fetched in a non-blocking manner
                        log( "Adding native ad icon (" + imageUrl + ") to queue to be fetched" );
                        iconDrawableFuture = createDrawableFuture( imageUrl, context.getResources() );
                    }

                    // Execute and timeout tasks if incomplete within the given time
                    int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
                    Drawable iconDrawable = null;
                    try
                    {
                        if ( iconDrawableFuture != null )
                        {
                            iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }
                    }
                    catch ( Throwable th )
                    {
                        e( "Image fetching tasks failed", th );
                    }

                    final MaxNativeAd.MaxNativeAdImage icon = iconDrawable != null ? new MaxNativeAd.MaxNativeAdImage( iconDrawable ) : null;

                    // Create MaxNativeAd after images are loaded from remote URLs
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            log( "Creating native ad with assets" );

                            MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat( MaxAdFormat.NATIVE )
                                    .setTitle( nativeAdData.getTitle() )
                                    .setBody( nativeAdData.getDescription() )
                                    .setCallToAction( nativeAdData.getButtonText() )
                                    .setIcon( icon )
                                    .setOptionsView( nativeAdData.getAdLogoView() )
                                    .setMediaView( nativeAdData.getMediaView() );
                            MaxNativeAd maxNativeAd = new MaxByteDanceNativeAd( builder );

                            log( "Native ad fully loaded: " + codeId );
                            listener.onNativeAdLoaded( maxNativeAd, null );
                        }
                    } );
                }
            } );
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( "Native ad (" + codeId + ") failed to load with error: " + adapterError );

            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShowed()
        {
            log( "Native ad displayed: " + codeId );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad clicked: " + codeId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onVideoAdPlay()
        {
            log( "Native ad video started playing" );
        }

        @Override
        public void onVideoAdPaused()
        {
            log( "Native ad video paused" );
        }

        @Override
        public void onVideoAdComplete()
        {
            log( "Native ad video completed" );
        }

        @Override
        public void onVideoError()
        {
            log( "Native ad video error" );
        }

        @Override
        public void onAdDismissed()
        {
            // This method won't be called until we implement `dislikeView` which is optional
            log( "Native ad hidden: " + codeId );
        }
    }

    private class MaxByteDanceNativeAd
            extends MaxNativeAd
    {
        public MaxByteDanceNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            final List<View> clickableViews = new ArrayList<>( 4 );
            if ( AppLovinSdkUtils.isValidString( getTitle() ) && maxNativeAdView.getTitleTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getTitleTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getBody() ) && maxNativeAdView.getBodyTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getBodyTextView() );
            }
            if ( getIcon() != null && maxNativeAdView.getIconImageView() != null )
            {
                clickableViews.add( maxNativeAdView.getIconImageView() );
            }
            if ( getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null )
            {
                clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );
            }

            prepareForInteraction( clickableViews, maxNativeAdView );
        }

        // @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final PAGNativeAd nativeAd = ByteDanceMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view for interaction. Native ad is null" );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            // Here creativeViews and dislikeView are null since they are optional
            nativeAd.registerViewForInteraction( container, clickableViews, null, null, nativeAdListener );

            return true;
        }
    }
}
