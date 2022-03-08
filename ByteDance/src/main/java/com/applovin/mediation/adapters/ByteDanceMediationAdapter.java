package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
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
import com.applovin.mediation.adapters.bytedance.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
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

    private TTFullScreenVideoAd interstitialAd;
    private TTRewardVideoAd     rewardedAd;
    private TTNativeExpressAd   expressAdViewAd;

    private InterstitialAdListener interstitialAdListener;
    private RewardedAdListener     rewardedAdListener;
    private NativeAdListener       nativeAdListener;

    private TTFeedAd nativeAd;

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

            TTAdConfig.Builder builder = new TTAdConfig.Builder();

            // Set mediation provider
            builder.data( createAdConfigData( serverParameters, true ) );

            if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
            {
                Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
                if ( hasUserConsent != null )
                {
                    builder.setGDPR( hasUserConsent ? 1 : 0 );
                }
            }

            Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
            if ( isAgeRestrictedUser != null )
            {
                builder.coppa( isAgeRestrictedUser ? 1 : 0 );
            }

            if ( AppLovinSdk.VERSION_CODE >= 91100 )
            {
                Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
                if ( isDoNotSell != null )
                {
                    builder.setCCPA( isDoNotSell ? 1 : 0 );
                }
            }

            TTAdConfig adConfig = builder.appId( appId )
                    .appName( serverParameters.getString( "app_name", "Default App Name" ) )
                    .debug( parameters.isTesting() )
                    .supportMultiProcess( false )
                    .build();

            TTAdSdk.init( getContext( activity ), adConfig, new TTAdSdk.InitCallback()
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
        return TTAdSdk.getAdManager().getSDKVersion();
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

        rewardedAdListener = null;
        rewardedAd = null;

        if ( expressAdViewAd != null )
        {
            expressAdViewAd.destroy();
            expressAdViewAd = null;
        }

        nativeAd = null;
        nativeAdListener = null;
    }

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        String signal = TTAdSdk.getAdManager().getBiddingToken();
        callback.onSignalCollected( signal );
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad for code id \"" + codeId + "\"..." );

        updateAdConfig( parameters );

        // NOTE: No privacy APIs to toggle before ad load

        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId( codeId )
                .setImageAcceptedSize( 1080, 1920 )
                .setSupportDeepLink( true )
                .setAdCount( 1 );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            adSlotBuilder.withBid( bidResponse );
        }

        interstitialAdListener = new InterstitialAdListener( codeId, listener );
        TTAdSdk.getAdManager().createAdNative( getContext( activity ) ).loadFullScreenVideoAd( adSlotBuilder.build(), interstitialAdListener );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for code id \"" + codeId + "\"..." );

        interstitialAd.setFullScreenVideoAdInteractionListener( interstitialAdListener );
        interstitialAd.showFullScreenVideoAd( activity );
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad for code id \"" + codeId + "\"..." );

        updateAdConfig( parameters );

        // NOTE: No privacy APIs to toggle before ad load

        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId( codeId )
                .setImageAcceptedSize( 1080, 1920 )
                .setSupportDeepLink( true )
                .setAdCount( 1 )
                // Rewarded Ad Params
                .setUserID( getWrappingSdk().getUserIdentifier() );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            adSlotBuilder.withBid( bidResponse );
        }

        rewardedAdListener = new RewardedAdListener( codeId, listener );
        TTAdSdk.getAdManager().createAdNative( getContext( activity ) ).loadRewardVideoAd( adSlotBuilder.build(), rewardedAdListener );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for code id \"" + codeId + "\"..." );

        // Configure userReward from server.
        configureReward( parameters );

        rewardedAd.setRewardAdInteractionListener( rewardedAdListener );
        rewardedAd.showRewardVideoAd( activity );
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        String bidResponse = parameters.getBidResponse();
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for code id \"" + codeId + "\"..." );

        updateAdConfig( parameters );

        AppLovinSdkUtils.Size adSize = adFormat.getSize();
        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId( codeId )
                .setExpressViewAcceptedSize( adSize.getWidth(), adSize.getHeight() )
                .setSupportDeepLink( true )
                .setAdCount( 1 );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            adSlotBuilder.withBid( bidResponse );
        }

        TTAdNative adViewAd = TTAdSdk.getAdManager().createAdNative( getContext( activity ) );
        if ( isNative )
        {
            NativeAdViewListener nativeListener = new NativeAdViewListener( parameters, adFormat, activity, listener );
            adViewAd.loadFeedAd( adSlotBuilder.build(), nativeListener );
        }
        else
        {
            AdViewListener adViewListener = new AdViewListener( codeId, adFormat, listener );
            adViewAd.loadBannerExpressAd( adSlotBuilder.build(), adViewListener );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    // @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String codeId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for code id \"" + codeId + "\"..." );

        updateAdConfig( parameters );

        // Minimum supported Android SDK version is 11.1.0+, previous version has `MaxNativeAdView` requiring an Activity context which might leak
        if ( AppLovinSdk.VERSION_CODE < 11010000 )
        {
            log( "Failing ad load for AppLovin SDK < 11.1.0 which requires an Activity context" );
            listener.onNativeAdLoadFailed( MaxAdapterError.UNSPECIFIED );
            return;
        }

        AdSlot.Builder adSlotBuilder = new AdSlot.Builder()
                .setCodeId( codeId )
                .setImageAcceptedSize( 640, 320 )
                .setSupportDeepLink( true )
                .setAdCount( 1 );

        if ( isBiddingAd )
        {
            adSlotBuilder.withBid( bidResponse );
        }

        nativeAdListener = new NativeAdListener( parameters, getContext( activity ), listener );
        TTAdSdk.getAdManager().createAdNative( getContext( activity ) ).loadFeedAd( adSlotBuilder.build(), nativeAdListener );
    }

    //endregion

    //region Helper Methods

    private Boolean getPrivacySetting(final String privacySetting, final MaxAdapterParameters parameters)
    {
        try
        {
            // Use reflection because compiled adapters have trouble fetching `boolean` from old SDKs and `Boolean` from new SDKs (above 9.14.0)
            Class<?> parametersClass = parameters.getClass();
            Method privacyMethod = parametersClass.getMethod( privacySetting );
            return (Boolean) privacyMethod.invoke( parameters );
        }
        catch ( Exception exception )
        {
            log( "Error getting privacy setting " + privacySetting + " with exception: ", exception );
            return ( AppLovinSdk.VERSION_CODE >= 9140000 ) ? null : false;
        }
    }

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

    private boolean isVideoMediaView(final int imageMode)
    {
        return ( imageMode == TTAdConstant.IMAGE_MODE_VIDEO ||
                imageMode == TTAdConstant.IMAGE_MODE_VIDEO_SQUARE ||
                imageMode == TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL );
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

    private void updateAdConfig(final MaxAdapterResponseParameters parameters)
    {
        TTAdConfig.Builder builder = new TTAdConfig.Builder();
        builder.data( createAdConfigData( parameters.getServerParameters(), false ) );
        TTAdSdk.updateAdConfig( builder.build() );
    }

    //endregion

    private class InterstitialAdListener
            implements TTAdNative.FullScreenVideoAdListener, TTFullScreenVideoAd.FullScreenVideoAdInteractionListener
    {
        private final String                         codeId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String codeId, final MaxInterstitialAdapterListener listener)
        {
            this.codeId = codeId;
            this.listener = listener;
        }

        @Override
        public void onFullScreenVideoAdLoad(final TTFullScreenVideoAd ad)
        {
            interstitialAd = ad;

            log( "Interstitial ad loaded: " + codeId );
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
        public void onFullScreenVideoCached()
        {
            log( "Interstitial ad cached: " + codeId );
        }

        @Override
        public void onAdShow()
        {
            log( "Interstitial ad displayed: " + codeId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdVideoBarClick()
        {
            log( "Interstitial ad clicked: " + codeId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClose()
        {
            log( "Interstitial ad hidden: " + codeId );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onVideoComplete()
        {
            log( "Interstitial ad video completed: " + codeId );
        }

        @Override
        public void onSkippedVideo()
        {
            log( "Interstitial ad skipped: " + codeId );
        }
    }

    private class RewardedAdListener
            implements TTAdNative.RewardVideoAdListener, TTRewardVideoAd.RewardAdInteractionListener
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
        public void onRewardVideoAdLoad(final TTRewardVideoAd ad)
        {
            rewardedAd = ad;

            log( "Rewarded ad loaded: " + codeId );
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
        public void onRewardVideoCached()
        {
            log( "Rewarded ad cached: " + codeId );
        }

        @Override
        public void onAdShow()
        {
            log( "Rewarded ad displayed: " + codeId );

            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onVideoError()
        {
            log( "Rewarded ad failed to display: " + codeId );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onAdVideoBarClick()
        {
            log( "Rewarded ad clicked: " + codeId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdClose()
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

        @Override
        public void onVideoComplete()
        {
            log( "Rewarded ad video completed: " + codeId );
            listener.onRewardedAdVideoCompleted();
        }

        @Override
        public void onRewardVerify(final boolean granted, final int amount, final String label, final int errorCode, final String errorMsg)
        {
            if ( granted )
            {
                log( "Rewarded user with reward: " + amount + " " + label );
                hasGrantedReward = true;
            }
            else
            {
                log( "Failed to reward user with error: " + errorCode + " " + errorMsg );
            }
        }

        @Override
        public void onSkippedVideo()
        {
            log( "Rewarded ad video skipped: " + codeId );
        }
    }

    private class AdViewListener
            implements TTAdNative.NativeExpressAdListener, TTNativeExpressAd.ExpressAdInteractionListener
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
        public void onNativeExpressAdLoad(final List<TTNativeExpressAd> ads)
        {
            if ( ads == null || ads.isEmpty() )
            {
                log( "Native " + adFormat.getLabel() + " ad (" + codeId + ") failed to load: no fill" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            // Pangle still needs an extra `render()` step to fully load an ad.
            log( adFormat.getLabel() + " ad (" + codeId + ") loaded with " + ads.size() + " ads" );

            expressAdViewAd = ads.get( 0 );
            expressAdViewAd.setExpressInteractionListener( this );

            expressAdViewAd.render();
        }

        @Override
        public void onError(final int code, final String message)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( adFormat.getLabel() + " ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onRenderSuccess(final View view, final float width, final float height)
        {
            log( adFormat.getLabel() + " ad loaded: " + codeId );
            listener.onAdViewAdLoaded( view );
        }

        @Override
        public void onRenderFail(final View view, final String message, final int code)
        {
            MaxAdapterError adapterError = toMaxError( code, message );
            log( adFormat.getLabel() + " ad (" + codeId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShow(final View view, final int type)
        {
            log( adFormat.getLabel() + " ad shown: " + codeId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(final View view, final int type)
        {
            log( adFormat.getLabel() + " ad clicked: " + codeId );
            listener.onAdViewAdClicked();
        }
    }

    private class NativeAdViewListener
            implements TTAdNative.FeedAdListener, TTNativeAd.AdInteractionListener, TTFeedAd.VideoAdListener
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
        public void onFeedAdLoad(final List<TTFeedAd> ads)
        {
            final Activity activity = activityRef.get();
            if ( activity == null )
            {
                log( "Native " + adFormat.getLabel() + " ad (" + codeId + ") failed to load: activity reference is null when ad is loaded" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                return;
            }

            if ( ads == null || ads.size() == 0 )
            {
                log( "Native " + adFormat.getLabel() + " ad (" + codeId + ") failed to load: no fill" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded: " + codeId + ". Preparing assets..." );

            final TTFeedAd nativeAdViewAd = ads.get( 0 );
            final ExecutorService executorServiceToUse;
            if ( AppLovinSdk.VERSION_CODE >= 11000000 )
            {
                executorServiceToUse = getCachingExecutorService();
            }
            else
            {
                executorServiceToUse = executor;
            }

            executorServiceToUse.execute( new Runnable()
            {
                @Override
                public void run()
                {
                    final Resources resources = activity.getResources();

                    // Create image fetching tasks to run asynchronously in the background
                    Future<Drawable> iconDrawableFuture = null;
                    if ( nativeAdViewAd.getIcon().isValid() )
                    {
                        // Pangle's image resource comes in the form of a URL which needs to be fetched in a non-blocking manner
                        log( "Adding native ad icon (" + nativeAdViewAd.getIcon().getImageUrl() + ") to queue to be fetched" );

                        final String imageUrl = nativeAdViewAd.getIcon().getImageUrl();
                        iconDrawableFuture = ( AppLovinSdk.VERSION_CODE >= 11000000 )
                                ? createDrawableFuture( imageUrl, resources )
                                : executorServiceToUse.submit( createDrawableTask( imageUrl, resources ) );
                    }

                    // Pangle's media view can be either a video or image (which they don't provide a view for)
                    Future<Drawable> imageDrawableFuture = null;
                    if ( isVideoMediaView( nativeAdViewAd.getImageMode() ) )
                    {
                        nativeAdViewAd.setVideoAdListener( NativeAdViewListener.this );
                    }
                    else if ( nativeAdViewAd.getImageList() != null && nativeAdViewAd.getImageList().size() > 0 )
                    {
                        final TTImage ttMediaImage = nativeAdViewAd.getImageList().get( 0 );
                        if ( ttMediaImage.isValid() )
                        {
                            // Pangle's image resource comes in the form of a URL which needs to be fetched in a non-blocking manner
                            log( "Adding native ad media (" + ttMediaImage.getImageUrl() + ") to queue to be fetched" );

                            final String imageUrl = ttMediaImage.getImageUrl();
                            imageDrawableFuture = ( AppLovinSdk.VERSION_CODE >= 11000000 )
                                    ? createDrawableFuture( imageUrl, resources )
                                    : executorServiceToUse.submit( createDrawableTask( imageUrl, resources ) );
                        }
                    }

                    // Execute and timeout tasks if incomplete within the given time
                    int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
                    Drawable iconDrawable = null;
                    Drawable mediaViewImageDrawable = null;
                    try
                    {
                        if ( iconDrawableFuture != null )
                        {
                            iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }

                        if ( imageDrawableFuture != null )
                        {
                            mediaViewImageDrawable = imageDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }
                    }
                    catch ( Throwable th )
                    {
                        e( "Image fetching tasks failed", th );
                    }

                    final MaxNativeAd.MaxNativeAdImage icon = iconDrawable != null ? new MaxNativeAd.MaxNativeAdImage( iconDrawable ) : null;
                    final View mediaView;
                    if ( isVideoMediaView( nativeAdViewAd.getImageMode() ) )
                    {
                        mediaView = nativeAdViewAd.getAdView();
                    }
                    else
                    {
                        mediaView = new ImageView( activity );
                        if ( mediaViewImageDrawable != null )
                        {
                            ( (ImageView) mediaView ).setImageDrawable( mediaViewImageDrawable );
                        }
                    }

                    // Create MaxNativeAd after images are loaded from remote URLs
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            log( "Creating native ad with assets" );

                            MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                                    .setAdFormat( adFormat )
                                    .setTitle( nativeAdViewAd.getTitle() )
                                    .setBody( nativeAdViewAd.getDescription() )
                                    .setCallToAction( nativeAdViewAd.getButtonText() )
                                    .setIcon( icon )
                                    .setMediaView( mediaView )
                                    .setOptionsView( nativeAdViewAd.getAdLogoView() )
                                    .build();

                            String templateName = BundleUtils.getString( "template", "", serverParameters );
                            if ( templateName.contains( "vertical" ) && AppLovinSdk.VERSION_CODE < 9140500 )
                            {
                                log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default horizontal native template will be used." );
                            }

                            MaxNativeAdView maxNativeAdView;
                            if ( AppLovinSdk.VERSION_CODE >= 11010000 )
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
                            }
                            else
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, activity );
                            }

                            List<View> clickableViews = new ArrayList<>();
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

                            nativeAdViewAd.registerViewForInteraction( maxNativeAdView, clickableViews, creativeViews, NativeAdViewListener.this );

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
        public void onAdShow(final TTNativeAd ttNativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad displayed: " + codeId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(final View view, final TTNativeAd ttNativeAd)
        {
            // This callback is never called
            log( "Native " + adFormat.getLabel() + " ad clicked: " + codeId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdCreativeClick(final View view, final TTNativeAd ttNativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad creative clicked: " + codeId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onVideoLoad(final TTFeedAd ad)
        {
            log( "Native " + adFormat.getLabel() + " ad video loaded" );
        }

        @Override
        public void onVideoError(final int errorCode, final int extraCode)
        {
            log( "Native " + adFormat.getLabel() + " ad video error: " + errorCode );
        }

        @Override
        public void onVideoAdStartPlay(final TTFeedAd ad)
        {
            log( "Native " + adFormat.getLabel() + " ad video started playing" );
        }

        @Override
        public void onVideoAdPaused(final TTFeedAd ad)
        {
            log( "Native " + adFormat.getLabel() + " ad video paused" );
        }

        @Override
        public void onVideoAdContinuePlay(final TTFeedAd ad)
        {
            log( "Native " + adFormat.getLabel() + " ad video continued" );
        }

        @Override
        public void onProgressUpdate(final long current, final long duration)
        {
            log( "Native " + adFormat.getLabel() + " ad video progress updated (" + current + ") by duration (" + duration + ")" );
        }

        @Override
        public void onVideoAdComplete(final TTFeedAd ad)
        {
            log( "Native " + adFormat.getLabel() + " ad video completed" );
        }
    }

    private class NativeAdListener
            implements TTAdNative.FeedAdListener, TTNativeAd.AdInteractionListener, TTFeedAd.VideoAdListener
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
        public void onFeedAdLoad(final List<TTFeedAd> ads)
        {
            if ( ads == null || ads.size() == 0 )
            {
                log( "Native ad (" + codeId + ") failed to load: no fill" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            log( "Native ad loaded: " + codeId + ". Preparing assets..." );

            final TTFeedAd nativeAd = ads.get( 0 );
            ByteDanceMediationAdapter.this.nativeAd = nativeAd;

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( !hasRequiredAssets( isTemplateAd, nativeAd ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
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
                    if ( nativeAd.getIcon().isValid() )
                    {
                        // Pangle's image resource comes in the form of a URL which needs to be fetched in a non-blocking manner
                        log( "Adding native ad icon (" + nativeAd.getIcon().getImageUrl() + ") to queue to be fetched" );
                        iconDrawableFuture = createDrawableFuture( nativeAd.getIcon().getImageUrl(), context.getResources() );
                    }

                    // Pangle's media view can be either a video or image (which they don't provide a view for)
                    Future<Drawable> imageDrawableFuture = null;
                    if ( isVideoMediaView( nativeAd.getImageMode() ) )
                    {
                        nativeAd.setVideoAdListener( NativeAdListener.this );
                    }
                    else if ( nativeAd.getImageList() != null && nativeAd.getImageList().size() > 0 )
                    {
                        final TTImage ttMediaImage = nativeAd.getImageList().get( 0 );
                        if ( ttMediaImage.isValid() )
                        {
                            // Pangle's image resource comes in the form of a URL which needs to be fetched in a non-blocking manner
                            log( "Adding native ad media (" + ttMediaImage.getImageUrl() + ") to queue to be fetched" );
                            imageDrawableFuture = createDrawableFuture( ttMediaImage.getImageUrl(), context.getResources() );
                        }
                    }

                    // Execute and timeout tasks if incomplete within the given time
                    int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
                    Drawable iconDrawable = null;
                    Drawable mediaViewImageDrawable = null;
                    try
                    {
                        if ( iconDrawableFuture != null )
                        {
                            iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }

                        if ( imageDrawableFuture != null )
                        {
                            mediaViewImageDrawable = imageDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }
                    }
                    catch ( Throwable th )
                    {
                        e( "Image fetching tasks failed", th );
                    }

                    final MaxNativeAd.MaxNativeAdImage icon = iconDrawable != null ? new MaxNativeAd.MaxNativeAdImage( iconDrawable ) : null;
                    final Drawable finalMediaViewImageDrawable = mediaViewImageDrawable;

                    // Create MaxNativeAd after images are loaded from remote URLs
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            final View mediaView;
                            if ( isVideoMediaView( nativeAd.getImageMode() ) )
                            {
                                mediaView = nativeAd.getAdView();
                            }
                            else if ( finalMediaViewImageDrawable != null )
                            {
                                mediaView = new ImageView( context );
                                ( (ImageView) mediaView ).setImageDrawable( finalMediaViewImageDrawable );
                            }
                            else
                            {
                                mediaView = null;
                            }

                            // Media view is required for non-template native ads.
                            if ( !isTemplateAd && mediaView == null )
                            {
                                e( "Media view asset is null for native custom ad view. Failing ad request." );
                                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                                return;
                            }

                            log( "Creating native ad with assets" );

                            MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat( MaxAdFormat.NATIVE )
                                    .setTitle( nativeAd.getTitle() )
                                    .setBody( nativeAd.getDescription() )
                                    .setCallToAction( nativeAd.getButtonText() )
                                    .setIcon( icon )
                                    .setMediaView( mediaView )
                                    .setOptionsView( nativeAd.getAdLogoView() );
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
            log( "Native ad (" + codeId + ") failed to load with error code (" + code + ") and message: " + message );

            MaxAdapterError adapterError = toMaxError( code, message );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShow(final TTNativeAd ttNativeAd)
        {
            log( "Native ad displayed: " + codeId );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked(final View view, final TTNativeAd ttNativeAd)
        {
            // This callback is never called
            log( "Native ad clicked: " + codeId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdCreativeClick(final View view, final TTNativeAd ttNativeAd)
        {
            log( "Native ad creative clicked: " + codeId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onVideoLoad(final TTFeedAd ad)
        {
            log( "Native ad video loaded" );
        }

        @Override
        public void onVideoError(final int errorCode, final int extraCode)
        {
            log( "Native ad video error: " + errorCode );
        }

        @Override
        public void onVideoAdStartPlay(final TTFeedAd ad)
        {
            log( "Native ad video started playing" );
        }

        @Override
        public void onVideoAdPaused(final TTFeedAd ad)
        {
            log( "Native ad video paused" );
        }

        @Override
        public void onVideoAdContinuePlay(final TTFeedAd ad)
        {
            log( "Native ad video continued" );
        }

        @Override
        public void onProgressUpdate(final long current, final long duration)
        {
            // Don't log - too spammy as it calls every x milliseconds
        }

        @Override
        public void onVideoAdComplete(final TTFeedAd ad)
        {
            log( "Native ad video completed" );
        }

        private boolean hasRequiredAssets(final boolean isTemplateAd, final TTFeedAd nativeAd)
        {
            if ( isTemplateAd )
            {
                return AppLovinSdkUtils.isValidString( nativeAd.getTitle() );
            }
            else
            {
                // NOTE: Media view is required and is checked separately.
                return AppLovinSdkUtils.isValidString( nativeAd.getTitle() )
                        && AppLovinSdkUtils.isValidString( nativeAd.getButtonText() );
            }
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
            TTFeedAd nativeAd = ByteDanceMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view for interaction. Native ad is null" );
                return;
            }

            List<View> clickableViews = new ArrayList<>();
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

            // CTA button is considered a creative view
            List<View> creativeViews = new ArrayList<>();
            if ( AppLovinSdkUtils.isValidString( getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
            {
                creativeViews.add( maxNativeAdView.getCallToActionButton() );
            }

            nativeAd.registerViewForInteraction( maxNativeAdView, clickableViews, creativeViews, nativeAdListener );
        }
    }
}
