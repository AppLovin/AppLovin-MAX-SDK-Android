package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
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
import com.applovin.mediation.adapters.mintegral.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.MBridgeSDK;
import com.mbridge.msdk.interstitialvideo.out.InterstitialVideoListener;
import com.mbridge.msdk.interstitialvideo.out.MBBidInterstitialVideoHandler;
import com.mbridge.msdk.interstitialvideo.out.MBInterstitialVideoHandler;
import com.mbridge.msdk.mbbid.out.BidManager;
import com.mbridge.msdk.nativex.view.MBMediaView;
import com.mbridge.msdk.out.BannerAdListener;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.Frame;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.MBBidRewardVideoHandler;
import com.mbridge.msdk.out.MBConfiguration;
import com.mbridge.msdk.out.MBRewardVideoHandler;
import com.mbridge.msdk.out.MBridgeIds;
import com.mbridge.msdk.out.MBridgeSDKFactory;
import com.mbridge.msdk.out.NativeListener;
import com.mbridge.msdk.out.OnMBMediaViewListener;
import com.mbridge.msdk.out.RewardInfo;
import com.mbridge.msdk.out.RewardVideoListener;
import com.mbridge.msdk.widget.MBAdChoice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MintegralMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxSignalProvider /* MaxNativeAdAdapter */
{
    private static final MintegralMediationAdapterRouter router;
    private static final AtomicBoolean                   initialized = new AtomicBoolean();

    private static final String APP_ID_PARAMETER  = "app_id";
    private static final String APP_KEY_PARAMETER = "app_key";

    // Possible ad load error messages received from Mintegral in an email
    private final static String NOT_INITIALIZED  = "init error";
    private final static String NO_FILL_1        = "no ads available can show";
    private final static String NO_FILL_2        = "no ads available";
    private final static String NO_FILL_3        = "no server ads available";
    private final static String NO_FILL_4        = "no ads source";
    private final static String NO_FILL_5        = "load no ad";
    private final static String NETWORK_ERROR    = "network exception";
    private final static String BAD_REQUEST      = "request parameter is null";
    private final static String TIMEOUT          = "load timeout";
    private final static String UNIT_ID_EMPTY    = "UnitId is null";
    private final static String NETWORK_IO_ERROR = "Network error,I/O exception";

    // List of Mintegral error codes not defined in API, but in their docs
    //
    // http://cdn-adn.rayjump.com/cdn-adn/v2/markdown_v2/index.html?file=sdk-m_sdk-android&lang=en#faqs
    //
    private final static String EXCEPTION_RETURN_EMPTY            = "EXCEPTION_RETURN_EMPTY"; // ads no fill
    private final static String EXCEPTION_TIMEOUT                 = "EXCEPTION_TIMEOUT"; // request timeout
    private final static String EXCEPTION_IV_RECALLNET_INVALIDATE = "EXCEPTION_IV_RECALLNET_INVALIDATE"; // The network status at the time of the request is incorrect. Generally， because of the SDK initialization is not completed yet when the request has been sent.
    private final static String EXCEPTION_SIGN_ERROR              = "EXCEPTION_SIGN_ERROR"; // AppID and appKey do not match correctly
    private final static String EXCEPTION_UNIT_NOT_FOUND          = "EXCEPTION_UNIT_NOT_FOUND"; // Can not find the unitID in dashboard
    private final static String EXCEPTION_UNIT_ID_EMPTY           = "EXCEPTION_UNIT_ID_EMPTY"; // unitID is empty
    private final static String EXCEPTION_UNIT_NOT_FOUND_IN_APP   = "EXCEPTION_UNIT_NOT_FOUND_IN_APP"; // Can not find the unitID of the appID
    private final static String EXCEPTION_UNIT_ADTYPE_ERROR       = "EXCEPTION_UNIT_ADTYPE_ERROR"; // The adtype of the unitID is wrong
    private final static String EXCEPTION_APP_ID_EMPTY            = "EXCEPTION_APP_ID_EMPTY"; // appID is empty
    private final static String EXCEPTION_APP_NOT_FOUND           = "EXCEPTION_APP_NOT_FOUND"; // Can not find the appId

    private static String sSdkVersion;

    private static final int DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 5; // Mintegral ad load timeout is 10s, so this is 5s.

    // Mintegral suggested we keep a map of unit id -> handler to prevent re-creation / high error rates - https://app.asana.com/0/573104092700345/1166998599374502
    private static final Map<String, MBInterstitialVideoHandler>    mbInterstitialVideoHandlers    = new HashMap<>();
    private static final Map<String, MBBidInterstitialVideoHandler> mbBidInterstitialVideoHandlers = new HashMap<>();
    private static final Map<String, MBRewardVideoHandler>          mbRewardVideoHandlers          = new HashMap<>();
    private static final Map<String, MBBidRewardVideoHandler>       mbBidRewardVideoHandlers       = new HashMap<>();

    // Used by the mediation adapter router
    private String mbUnitId;

    // Supports video, interactive, and banner ad formats
    private MBInterstitialVideoHandler    mbInterstitialVideoHandler;
    private MBBidInterstitialVideoHandler mbBidInterstitialVideoHandler;
    private MBRewardVideoHandler          mbRewardVideoHandler;
    private MBBidRewardVideoHandler       mbBidRewardVideoHandler;
    private MBBannerView                  mbBannerView;
    private MBBidNativeHandler            mbBidNativeHandler;
    private Campaign                      nativeAdCampaign;
    private MaxNativeAdView               maxNativeAdView;
    private List<View>                    clickableViews;

    static
    {
        router = (MintegralMediationAdapterRouter) MediationAdapterRouter.getSharedInstance( MintegralMediationAdapterRouter.class );
    }

    // Explicit default constructor declaration
    public MintegralMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        MBridgeConstans.DEBUG = parameters.isTesting();

        if ( initialized.compareAndSet( false, true ) )
        {
            final String appId = parameters.getServerParameters().getString( APP_ID_PARAMETER );
            final String appKey = parameters.getServerParameters().getString( APP_KEY_PARAMETER );
            log( "Initializing Mintegral SDK with app id: " + appId + " and app key: " + appKey + "..." );

            final MBridgeSDK mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK();

            final Context context = getContext( activity );

            // Communicated over email, GDPR status can only be set before SDK initialization
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                int consent = hasUserConsent ? MBridgeConstans.IS_SWITCH_ON : MBridgeConstans.IS_SWITCH_OFF;
                mBridgeSDK.setUserPrivateInfoType( context, MBridgeConstans.AUTHORITY_ALL_INFO, consent );
                mBridgeSDK.setConsentStatus( context, consent );
            }

            // Has to be _before_ their SDK init as well
            if ( AppLovinSdk.VERSION_CODE >= 91100 )
            {
                Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
                if ( isDoNotSell != null && isDoNotSell )
                {
                    mBridgeSDK.setDoNotTrackStatus( true );
                }
            }

            // Mintegral Docs - "It is recommended to use the API in the main thread"
            final Map<String, String> map = mBridgeSDK.getMBConfigurationMap( appId, appKey );
            mBridgeSDK.init( map, context );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        // Do not use `MBConfiguration.SDK_VERSION` as this will hardcode the version when the adapter is built
        if ( sSdkVersion == null )
        {
            sSdkVersion = getVersionString( MBConfiguration.class, "SDK_VERSION" );
        }

        return sSdkVersion;
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( mbInterstitialVideoHandler != null )
        {
            mbInterstitialVideoHandler.setInterstitialVideoListener( null );
            mbInterstitialVideoHandler = null;
        }

        if ( mbBidInterstitialVideoHandler != null )
        {
            mbBidInterstitialVideoHandler.setInterstitialVideoListener( null );
            mbBidInterstitialVideoHandler = null;
        }

        if ( mbRewardVideoHandler != null )
        {
            mbRewardVideoHandler.setRewardVideoListener( null );
            mbRewardVideoHandler = null;
        }

        if ( mbBidRewardVideoHandler != null )
        {
            mbBidRewardVideoHandler.setRewardVideoListener( null );
            mbBidRewardVideoHandler = null;
        }

        if ( mbBannerView != null )
        {
            mbBannerView.release();
            mbBannerView = null;
        }

        if ( mbBidNativeHandler != null )
        {
            mbBidNativeHandler.unregisterView( maxNativeAdView, clickableViews, nativeAdCampaign );
            mbBidNativeHandler.bidRelease();
            mbBidNativeHandler.setAdListener( null );
            mbBidNativeHandler = null;
        }

        nativeAdCampaign = null;

        router.removeAdapter( this, mbUnitId );
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        final String signal = BidManager.getBuyerUid( getContext( activity ) );
        callback.onSignalCollected( signal );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        final boolean shouldUpdateMuteState = parameters.getServerParameters().containsKey( "is_muted" ); // Introduced in 9.10.0
        final int muteState = parameters.getServerParameters().getBoolean( "is_muted" ) ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.INTER_ACTIVE_VIDEO_PLAY_NOT_MUTE;

        mbUnitId = parameters.getThirdPartyAdPlacementId();
        final String placementId = BundleUtils.getString( "placement_id", parameters.getServerParameters() );

        router.addInterstitialAdapter( this, listener, mbUnitId );

        if ( !TextUtils.isEmpty( parameters.getBidResponse() ) )
        {
            log( "Loading bidding interstitial ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );

            if ( mbBidInterstitialVideoHandlers.containsKey( mbUnitId ) )
            {
                mbBidInterstitialVideoHandler = mbBidInterstitialVideoHandlers.get( mbUnitId );
            }
            else
            {
                mbBidInterstitialVideoHandler = new MBBidInterstitialVideoHandler( activity, placementId, mbUnitId );
                mbBidInterstitialVideoHandlers.put( mbUnitId, mbBidInterstitialVideoHandler );
            }

            mbBidInterstitialVideoHandler.setInterstitialVideoListener( router.getInterstitialListener() );

            if ( mbBidInterstitialVideoHandler.isBidReady() )
            {
                log( "A bidding interstitial ad is ready already" );

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( mbBidInterstitialVideoHandler.getRequestId() ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", mbBidInterstitialVideoHandler.getRequestId() );

                    router.onAdLoaded( mbUnitId, extraInfo );
                }
                else
                {
                    router.onAdLoaded( mbUnitId );
                }
            }
            else
            {
                // Update mute state if configured by backend
                if ( shouldUpdateMuteState ) mbBidInterstitialVideoHandler.playVideoMute( muteState );

                mbBidInterstitialVideoHandler.loadFromBid( parameters.getBidResponse() );
            }
        }
        else
        {
            log( "Loading mediated interstitial ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );

            if ( mbInterstitialVideoHandlers.containsKey( mbUnitId ) )
            {
                mbInterstitialVideoHandler = mbInterstitialVideoHandlers.get( mbUnitId );
            }
            else
            {
                mbInterstitialVideoHandler = new MBInterstitialVideoHandler( activity, placementId, mbUnitId );
                mbInterstitialVideoHandlers.put( mbUnitId, mbInterstitialVideoHandler );
            }

            mbInterstitialVideoHandler.setInterstitialVideoListener( router.getInterstitialListener() );

            if ( mbInterstitialVideoHandler.isReady() )
            {
                log( "A mediated interstitial ad is ready already" );

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( mbInterstitialVideoHandler.getRequestId() ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", mbInterstitialVideoHandler.getRequestId() );

                    router.onAdLoaded( mbUnitId, extraInfo );
                }
                else
                {
                    router.onAdLoaded( mbUnitId );
                }
            }
            else
            {
                // Update mute state if configured by backend
                if ( shouldUpdateMuteState ) mbInterstitialVideoHandler.playVideoMute( muteState );

                mbInterstitialVideoHandler.load();
            }
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        router.addShowingAdapter( this );

        if ( mbBidInterstitialVideoHandler != null && mbBidInterstitialVideoHandler.isBidReady() )
        {
            log( "Showing bidding interstitial..." );
            mbBidInterstitialVideoHandler.showFromBid();
        }
        else if ( mbInterstitialVideoHandler != null && mbInterstitialVideoHandler.isReady() )
        {
            log( "Showing mediated interstitial..." );
            mbInterstitialVideoHandler.show();
        }
        else
        {
            log( "Unable to show interstitial - no ad loaded..." );

            // Ad load failed
            router.onAdDisplayFailed( mbUnitId, new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        final boolean shouldUpdateMuteState = parameters.getServerParameters().containsKey( "is_muted" ); // Introduced in 9.10.0
        final int muteState = parameters.getServerParameters().getBoolean( "is_muted" ) ? MBridgeConstans.REWARD_VIDEO_PLAY_MUTE : MBridgeConstans.INTER_ACTIVE_VIDEO_PLAY_NOT_MUTE;

        mbUnitId = parameters.getThirdPartyAdPlacementId();
        final String placementId = BundleUtils.getString( "placement_id", parameters.getServerParameters() );

        router.addRewardedAdapter( this, listener, mbUnitId );

        if ( !TextUtils.isEmpty( parameters.getBidResponse() ) )
        {
            log( "Loading bidding rewarded ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );

            if ( mbBidRewardVideoHandlers.containsKey( mbUnitId ) )
            {
                mbBidRewardVideoHandler = mbBidRewardVideoHandlers.get( mbUnitId );
            }
            else
            {
                mbBidRewardVideoHandler = new MBBidRewardVideoHandler( activity, placementId, mbUnitId );
                mbBidRewardVideoHandlers.put( mbUnitId, mbBidRewardVideoHandler );
            }

            mbBidRewardVideoHandler.setRewardVideoListener( router.getRewardedListener() );

            if ( mbBidRewardVideoHandler.isBidReady() )
            {
                log( "A bidding rewarded ad is ready already" );

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( mbBidRewardVideoHandler.getRequestId() ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", mbBidRewardVideoHandler.getRequestId() );

                    router.onAdLoaded( mbUnitId, extraInfo );
                }
                else
                {
                    router.onAdLoaded( mbUnitId );
                }
            }
            else
            {
                // Update mute state if configured by backend
                if ( shouldUpdateMuteState ) mbBidRewardVideoHandler.playVideoMute( muteState );

                mbBidRewardVideoHandler.loadFromBid( parameters.getBidResponse() );
            }
        }
        else
        {
            log( "Loading mediated rewarded ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );

            if ( mbRewardVideoHandlers.containsKey( mbUnitId ) )
            {
                mbRewardVideoHandler = mbRewardVideoHandlers.get( mbUnitId );
            }
            else
            {
                mbRewardVideoHandler = new MBRewardVideoHandler( activity, placementId, mbUnitId );
                mbRewardVideoHandlers.put( mbUnitId, mbRewardVideoHandler );
            }

            mbRewardVideoHandler.setRewardVideoListener( router.getRewardedListener() );

            if ( mbRewardVideoHandler.isReady() )
            {
                log( "A mediated rewarded ad is ready already" );

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( mbRewardVideoHandler.getRequestId() ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", mbRewardVideoHandler.getRequestId() );

                    router.onAdLoaded( mbUnitId, extraInfo );
                }
                else
                {
                    router.onAdLoaded( mbUnitId );
                }
            }
            else
            {
                // Update mute state if configured by backend
                if ( shouldUpdateMuteState ) mbRewardVideoHandler.playVideoMute( muteState );

                mbRewardVideoHandler.load();
            }
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        router.addShowingAdapter( this );

        // Configure userReward from server.
        configureReward( parameters );

        final Bundle serverParameters = parameters.getServerParameters();
        final String rewardId = serverParameters.getString( "reward_id", "" );
        final String userId = serverParameters.getString( "user_id", "" );

        if ( mbBidRewardVideoHandler != null && mbBidRewardVideoHandler.isBidReady() )
        {
            log( "Showing bidding rewarded ad..." );
            mbBidRewardVideoHandler.showFromBid( rewardId, userId );
        }
        else if ( mbRewardVideoHandler != null && mbRewardVideoHandler.isReady() )
        {
            log( "Showing mediated rewarded ad..." );
            mbRewardVideoHandler.show( rewardId, userId );
        }
        else
        {
            log( "Unable to show rewarded ad - no ad loaded..." );

            // Ad load failed
            router.onAdDisplayFailed( mbUnitId, new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        BannerSize size = toBannerSize( adFormat );

        mbUnitId = parameters.getThirdPartyAdPlacementId();
        final String placementId = BundleUtils.getString( "placement_id", parameters.getServerParameters() );

        mbBannerView = new MBBannerView( getContext( activity ) );
        mbBannerView.init( size, placementId, mbUnitId );
        mbBannerView.setAllowShowCloseBtn( false );
        mbBannerView.setRefreshTime( 0 );

        mbBannerView.setBannerAdListener( new BannerAdListener()
        {
            @Override
            public void onLoadSuccessed(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad loaded for: " + mBridgeIds );

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( mbBannerView.getRequestId() ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", mbBannerView.getRequestId() );

                    listener.onAdViewAdLoaded( mbBannerView, extraInfo );
                }
                else
                {
                    listener.onAdViewAdLoaded( mbBannerView );
                }
            }

            @Override
            public void onLoadFailed(final MBridgeIds mBridgeIds, String msg)
            {
                log( "Banner ad failed to load: " + msg + " for: " + mBridgeIds );
                listener.onAdViewAdLoadFailed( toMaxError( msg ) );
            }

            @Override
            public void onLogImpression(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad displayed" );
                listener.onAdViewAdDisplayed();
            }

            @Override
            public void onClick(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad clicked" );
                listener.onAdViewAdClicked();
            }

            @Override
            public void onLeaveApp(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad will leave application" );
            }

            @Override
            public void showFullScreen(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad expanded" );
                listener.onAdViewAdExpanded();
            }

            @Override
            public void closeFullScreen(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad collapsed" );
                listener.onAdViewAdCollapsed();
            }

            @Override
            public void onCloseBanner(final MBridgeIds mBridgeIds)
            {
                log( "Banner ad closed" );
            }
        } );

        if ( !TextUtils.isEmpty( parameters.getBidResponse() ) )
        {
            log( "Loading bidding banner ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );
            mbBannerView.loadFromBid( parameters.getBidResponse() );
        }
        else
        {
            log( "Loading mediated banner ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );
            mbBannerView.load();
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        mbUnitId = parameters.getThirdPartyAdPlacementId();
        final String placementId = BundleUtils.getString( "placement_id", parameters.getServerParameters() );

        log( "Loading bidding native ad for unit id: " + mbUnitId + " and placement id: " + placementId + "..." );

        Map<String, Object> properties = MBBidNativeHandler.getNativeProperties( placementId, mbUnitId );
        properties.put( MBridgeConstans.PROPERTIES_AD_NUM, 1 ); // Only load one ad.
        properties.put( MBridgeConstans.NATIVE_VIDEO_SUPPORT, true );

        final NativeAdListener nativeAdListener = new NativeAdListener( parameters, getContext( activity ), listener );

        // Native ads do not use the handler maps, because MBNativeHandler.setAdListener fails to update the ad listener after the first assignment.
        mbBidNativeHandler = new MBBidNativeHandler( properties, getContext( activity ) );
        mbBidNativeHandler.setAdListener( nativeAdListener );
        mbBidNativeHandler.bidLoad( parameters.getBidResponse() );
    }

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

    private static MaxAdapterError toMaxError(final String mintegralError)
    {
        // Note: we are using `contains()` in some cases b/c Mintegral prepends the message with `data load failed, errorMsg is `...

        final MaxAdapterError adapterError;
        if ( NOT_INITIALIZED.equals( mintegralError ) || mintegralError.contains( EXCEPTION_IV_RECALLNET_INVALIDATE ) )
        {
            adapterError = MaxAdapterError.NOT_INITIALIZED;
        }
        else if ( mintegralError.contains( NO_FILL_1 ) || mintegralError.contains( NO_FILL_2 )
                || mintegralError.contains( NO_FILL_3 ) || mintegralError.contains( NO_FILL_4 )
                || mintegralError.contains( NO_FILL_5 ) || mintegralError.contains( EXCEPTION_RETURN_EMPTY ) )
        {
            adapterError = MaxAdapterError.NO_FILL;
        }
        else if ( NETWORK_ERROR.equalsIgnoreCase( mintegralError ) || mintegralError.contains( NETWORK_IO_ERROR ) )
        {
            adapterError = MaxAdapterError.NO_CONNECTION;
        }
        else if ( BAD_REQUEST.equalsIgnoreCase( mintegralError ) )
        {
            adapterError = MaxAdapterError.BAD_REQUEST;
        }
        else if ( TIMEOUT.equalsIgnoreCase( mintegralError ) || mintegralError.contains( EXCEPTION_TIMEOUT ) )
        {
            adapterError = MaxAdapterError.TIMEOUT;
        }
        else if ( mintegralError.contains( EXCEPTION_SIGN_ERROR ) || mintegralError.contains( EXCEPTION_UNIT_NOT_FOUND ) || mintegralError.contains( EXCEPTION_UNIT_ID_EMPTY ) || mintegralError.contains( EXCEPTION_UNIT_NOT_FOUND_IN_APP ) || mintegralError.contains( EXCEPTION_UNIT_ADTYPE_ERROR ) || mintegralError.contains( EXCEPTION_APP_ID_EMPTY ) || mintegralError.contains( EXCEPTION_APP_NOT_FOUND ) || mintegralError.contains( UNIT_ID_EMPTY ) )
        {
            adapterError = MaxAdapterError.INVALID_CONFIGURATION;
        }
        else
        {
            adapterError = MaxAdapterError.UNSPECIFIED;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), 0, mintegralError );
    }

    private Context getContext(Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private BannerSize toBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER )
        {
            // Last two parameters are for custom width and height, so we can just use 0.
            return new BannerSize( BannerSize.SMART_TYPE, 0, 0 );
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return new BannerSize( BannerSize.MEDIUM_TYPE, 0, 0 );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private static class MintegralMediationAdapterRouter
            extends MediationAdapterRouter
    {
        private final InterstitialVideoListener interstitialVideoListener = new InterstitialVideoListener()
        {
            @Override
            public void onVideoLoadSuccess(final MBridgeIds mBridgeIds)
            {
                // Ad has loaded and video has been downloaded
                log( "Interstitial successfully loaded and video has been downloaded for: " + mBridgeIds );

                String unitId = mBridgeIds.getUnitId();
                MBInterstitialVideoHandler mbInterstitialVideoHandler = MintegralMediationAdapter.mbInterstitialVideoHandlers.get( unitId );
                MBBidInterstitialVideoHandler mbBidInterstitialVideoHandler = MintegralMediationAdapter.mbBidInterstitialVideoHandlers.get( unitId );

                String requestId;
                if ( mbBidInterstitialVideoHandler != null )
                {
                    requestId = mbBidInterstitialVideoHandler.getRequestId();
                }
                else
                {
                    requestId = mbInterstitialVideoHandler.getRequestId();
                }

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( requestId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", requestId );

                    onAdLoaded( unitId, extraInfo );
                }
                else
                {
                    onAdLoaded( unitId );
                }
            }

            @Override
            public void onLoadSuccess(final MBridgeIds mBridgeIds)
            {
                // Ad has loaded but video still needs to be downloaded
                log( "Interstitial successfully loaded but video still needs to be downloaded for: " + mBridgeIds );
            }

            @Override
            public void onVideoLoadFail(final MBridgeIds mBridgeIds, String errorMsg)
            {
                log( "Interstitial failed to load: " + errorMsg + " for: " + mBridgeIds );
                onAdLoadFailed( mBridgeIds.getUnitId(), toMaxError( errorMsg ) );
            }

            @Override
            public void onAdShow(final MBridgeIds mBridgeIds)
            {
                log( "Interstitial displayed" );
                onAdDisplayed( mBridgeIds.getUnitId() );
            }

            @Override
            public void onShowFail(final MBridgeIds mBridgeIds, String errorMsg)
            {
                MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", 0, errorMsg );
                log( "Interstitial failed to show: " + adapterError );
                onAdDisplayFailed( mBridgeIds.getUnitId(), adapterError );
            }

            @Override
            public void onVideoAdClicked(final MBridgeIds mBridgeIds)
            {
                log( "Interstitial clicked" );
                onAdClicked( mBridgeIds.getUnitId() );
            }

            @Override
            public void onAdClose(final MBridgeIds mBridgeIds, RewardInfo rewardInfo)
            {
                log( "Interstitial hidden" );
                onAdHidden( mBridgeIds.getUnitId() );
            }

            @Override
            public void onVideoComplete(final MBridgeIds mBridgeIds)
            {
                log( "Interstitial video completed" );
            }

            @Override
            public void onAdCloseWithIVReward(final MBridgeIds mBridgeIds, RewardInfo rewardInfo)
            {
                log( "Interstitial with reward hidden" );
            }

            @Override
            public void onEndcardShow(final MBridgeIds mBridgeIds)
            {
                log( "Interstitial endcard shown" );
            }
        };

        private final RewardVideoListener rewardVideoListener = new RewardVideoListener()
        {
            @Override
            public void onVideoLoadSuccess(final MBridgeIds mBridgeIds)
            {
                // Ad has loaded and video has been downloaded
                log( "Rewarded ad successfully loaded and video has been downloaded for: " + mBridgeIds );

                String unitId = mBridgeIds.getUnitId();
                MBRewardVideoHandler mbRewardVideoHandler = MintegralMediationAdapter.mbRewardVideoHandlers.get( unitId );
                MBBidRewardVideoHandler mbBidRewardVideoHandler = MintegralMediationAdapter.mbBidRewardVideoHandlers.get( unitId );

                String requestId;
                if ( mbBidRewardVideoHandler != null )
                {
                    requestId = mbBidRewardVideoHandler.getRequestId();
                }
                else
                {
                    requestId = mbRewardVideoHandler.getRequestId();
                }

                // Passing extra info such as creative id supported in 9.15.0+
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( requestId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", requestId );

                    onAdLoaded( unitId, extraInfo );
                }
                else
                {
                    onAdLoaded( unitId );
                }
            }

            @Override
            public void onLoadSuccess(final MBridgeIds mBridgeIds)
            {
                // Ad has loaded but video still needs to be downloaded
                log( "Rewarded ad successfully loaded but video still needs to be downloaded for: " + mBridgeIds );
            }

            @Override
            public void onVideoLoadFail(final MBridgeIds mBridgeIds, String errorMsg)
            {
                log( "Rewarded ad failed to load: " + errorMsg + " for: " + mBridgeIds );
                onAdLoadFailed( mBridgeIds.getUnitId(), toMaxError( errorMsg ) );
            }

            @Override
            public void onAdShow(final MBridgeIds mBridgeIds)
            {
                log( "Rewarded ad displayed" );

                final String unitId = mBridgeIds.getUnitId();
                onAdDisplayed( unitId );
                onRewardedAdVideoStarted( unitId );
            }

            @Override
            public void onShowFail(final MBridgeIds mBridgeIds, String errorMsg)
            {
                MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", 0, errorMsg );
                log( "Rewarded ad failed to show: " + adapterError );
                onAdDisplayFailed( mBridgeIds.getUnitId(), adapterError );
            }

            @Override
            public void onVideoAdClicked(final MBridgeIds mBridgeIds)
            {
                log( "Rewarded ad clicked" );
                onAdClicked( mBridgeIds.getUnitId() );
            }

            @Override
            public void onAdClose(final MBridgeIds mBridgeIds, RewardInfo rewardInfo)
            {
                log( "Rewarded ad hidden" );

                final String unitId = mBridgeIds.getUnitId();
                if ( rewardInfo.isCompleteView() )
                {
                    onRewardedAdVideoCompleted( unitId );
                    onUserRewarded( unitId, getReward( unitId ) );
                }
                else if ( shouldAlwaysRewardUser( unitId ) )
                {
                    onUserRewarded( unitId, getReward( unitId ) );
                }

                onAdHidden( unitId );
            }

            @Override
            public void onVideoComplete(final MBridgeIds mBridgeIds)
            {
                log( "Rewarded ad video completed" );
            }

            @Override
            public void onEndcardShow(final MBridgeIds mBridgeIds)
            {
                log( "Rewarded ad endcard shown" );
            }
        };

        InterstitialVideoListener getInterstitialListener()
        {
            return interstitialVideoListener;
        }

        RewardVideoListener getRewardedListener()
        {
            return rewardVideoListener;
        }

        //TODO: marked for deletion, pending SDK change.
        void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener) { }
    }

    private class NativeAdListener
            implements NativeListener.NativeAdListener, OnMBMediaViewListener
    {
        private final MaxAdapterResponseParameters parameters;
        private final Context                      context;
        private final MaxNativeAdAdapterListener   listener;
        private final String                       unitId;
        private final String                       placementId;

        NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.parameters = parameters;
            this.context = context;
            this.listener = listener;

            unitId = parameters.getThirdPartyAdPlacementId();
            placementId = BundleUtils.getString( "placement_id", parameters.getServerParameters() );
        }

        //region NativeListener.NativeAdListener methods

        @Override
        public void onAdLoaded(final List<Campaign> campaigns, final int templates)
        {
            if ( campaigns == null || campaigns.isEmpty() )
            {
                log( "Native ad failed to load for unit id: " + unitId + " placement id: " + placementId + " with error: no fill" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            final Campaign campaign = campaigns.get( 0 );
            final String templateName = BundleUtils.getString( "template", "", parameters.getServerParameters() );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( campaign.getAppName() ) )
            {
                log( "Native ad failed to load for unit id: " + unitId + " placement id: " + placementId + " with error: missing required assets" );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            nativeAdCampaign = campaign;

            log( "Native ad loaded for unit id: " + unitId + " placement id: " + placementId );
            processNativeAd( campaign );
        }

        @Override
        public void onAdLoadError(final String message)
        {
            MaxAdapterError error = toMaxError( message );
            log( "Native ad failed to load for unit id: " + unitId + " placement id: " + placementId + " with error: " + error );
            listener.onNativeAdLoadFailed( error );
        }

        @Override
        public void onLoggingImpression(final int adSourceType)
        {
            log( "Native ad shown for unit id: " + unitId + " placement id: " + placementId );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClick(final Campaign campaign)
        {
            log( "Native ad clicked for unit id: " + unitId + " placement id: " + placementId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdFramesLoaded(final List<Frame> list)
        {
            log( "Native ad frames loaded for unit id: " + unitId + " placement id: " + placementId );
        }

        //endregion

        //region ONMBMediaViewListener

        @Override
        public void onEnterFullscreen()
        {
            log( "Media view entered fullscreen" );
        }

        @Override
        public void onExitFullscreen()
        {
            log( "Media view exited fullscreen" );
        }

        @Override
        public void onStartRedirection(final Campaign campaign, final String url)
        {
            log( "Media view started redirection with url: " + url );
        }

        @Override
        public void onFinishRedirection(final Campaign campaign, final String url)
        {
            log( "Media view finished redirection with url: " + url );
        }

        @Override
        public void onRedirectionFailed(final Campaign campaign, final String url)
        {
            log( "Media view redirection failed with url: " + url );
        }

        @Override
        public void onVideoAdClicked(final Campaign campaign)
        {
            log( "Media view clicked for unit id: " + unitId + " placement id: " + placementId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onVideoStart()
        {
            log( "Media view video started" );
        }

        //endregion

        //region NativeAdListener helper methods

        private void processNativeAd(final Campaign campaign)
        {
            getCachingExecutorService().submit( new Runnable()
            {
                @Override
                public void run()
                {
                    final String iconUrl = campaign.getIconUrl();
                    final String mainImageUrl = campaign.getImageUrl();
                    final Future<Drawable> iconDrawableFuture = createDrawableFuture( iconUrl, context.getResources() );

                    MaxNativeAd.MaxNativeAdImage iconImage = null;
                    Uri uri = Uri.parse( mainImageUrl );
                    final MaxNativeAd.MaxNativeAdImage mainImage = new MaxNativeAd.MaxNativeAdImage( uri );

                    try
                    {
                        final int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, parameters.getServerParameters() );
                        final Drawable iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );

                        if ( iconDrawable != null )
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( iconDrawable );
                        }
                    }
                    catch ( Throwable th )
                    {
                        log( "Failed to fetch icon image from URL: " + iconUrl, th );
                    }

                    // `iconImage` must be final to be used inside the Runnable.
                    final MaxNativeAd.MaxNativeAdImage finalIconImage = iconImage;
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            final MBMediaView mediaView = new MBMediaView( context );
                            mediaView.setNativeAd( campaign );
                            mediaView.setOnMediaViewListener( NativeAdListener.this );

                            final MBAdChoice adChoiceView = new MBAdChoice( context );
                            adChoiceView.setCampaign( campaign );

                            MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat( MaxAdFormat.NATIVE )
                                    .setTitle( campaign.getAppName() )
                                    .setBody( campaign.getAppDesc() )
                                    .setCallToAction( campaign.getAdCall() )
                                    .setIcon( finalIconImage )
                                    .setMediaView( mediaView )
                                    .setOptionsView( adChoiceView );
                            if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
                            {
                                builder.setMainImage( mainImage );
                            }

                            final MaxNativeAd maxNativeAd = new MaxMintegralNativeAd( builder );
                            listener.onNativeAdLoaded( maxNativeAd, null );
                        }
                    } );
                }
            } );
        }

        //endregion
    }

    private class MaxMintegralNativeAd
            extends MaxNativeAd
    {
        public MaxMintegralNativeAd(final Builder builder) { super( builder ); }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            final Campaign nativeAdCampaign = MintegralMediationAdapter.this.nativeAdCampaign;
            if ( nativeAdCampaign == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return;
            }

            final List<View> clickableViews = new ArrayList<>();
            if ( AppLovinSdkUtils.isValidString( getTitle() ) && maxNativeAdView.getTitleTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getTitleTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getBody() ) && maxNativeAdView.getBodyTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getBodyTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
            {
                clickableViews.add( maxNativeAdView.getCallToActionButton() );
            }
            if ( getIcon() != null && maxNativeAdView.getIconImageView() != null )
            {
                clickableViews.add( maxNativeAdView.getIconImageView() );
            }
            if ( getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null )
            {
                clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );
            }

            mbBidNativeHandler.registerView( maxNativeAdView, clickableViews, nativeAdCampaign );

            MintegralMediationAdapter.this.maxNativeAdView = maxNativeAdView;
            MintegralMediationAdapter.this.clickableViews = clickableViews;
        }
    }
}
