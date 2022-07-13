package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.applovin.mediation.adapters.verizonads.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.yahoo.ads.ActivityStateManager;
import com.yahoo.ads.CcpaConsent;
import com.yahoo.ads.CreativeInfo;
import com.yahoo.ads.ErrorInfo;
import com.yahoo.ads.GdprConsent;
import com.yahoo.ads.Logger;
import com.yahoo.ads.RequestMetadata;
import com.yahoo.ads.VideoPlayerView;
import com.yahoo.ads.YASAds;
import com.yahoo.ads.inlineplacement.AdSize;
import com.yahoo.ads.inlineplacement.InlineAdView;
import com.yahoo.ads.inlineplacement.InlinePlacementConfig;
import com.yahoo.ads.interstitialplacement.InterstitialAd;
import com.yahoo.ads.interstitialplacement.InterstitialPlacementConfig;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.nativeplacement.NativePlacementConfig;
import com.yahoo.ads.yahoonativecontroller.NativeComponent;
import com.yahoo.ads.yahoonativecontroller.NativeImageComponent;
import com.yahoo.ads.yahoonativecontroller.NativeTextComponent;
import com.yahoo.ads.yahoonativecontroller.NativeVideoComponent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

/**
 * Created by santoshbagadi on 2/27/19.
 */
public class VerizonAdsMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxSignalProvider /* MaxNativeAdAdapter */
{
    // Server parameters
    private static final String PARAMETER_SITE_ID = "site_id";

    // Event IDs
    private static final String VIDEO_COMPLETED_EVENT_ID = "onVideoComplete";
    private static final String AD_IMPRESSION_EVENT_ID   = "adImpression";

    public static final String[] NATIVE_AD_AD_TYPES = new String[] { "simpleImage", "simpleVideo" };

    // Ad objects
    private InterstitialAd interstitialAd;
    private InterstitialAd rewardedAd;
    private InlineAdView   inlineAdView;
    private NativeAd       nativeAd;

    public VerizonAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( !YASAds.isInitialized() )
        {
            log( "Initializing SDK..." );

            int logLevel = parameters.isTesting() ? Logger.VERBOSE : Logger.ERROR;
            YASAds.setLogLevel( logLevel );

            Application application = (Application) getContext( activity );
            String siteId = parameters.getServerParameters().getString( PARAMETER_SITE_ID );

            boolean initialized = YASAds.initialize( application, siteId );

            InitializationStatus status = initialized ? InitializationStatus.INITIALIZED_SUCCESS : InitializationStatus.INITIALIZED_FAILURE;
            onCompletionListener.onCompletion( status, null );

            // ...GDPR settings, which is part of verizon Ads SDK data, should be established after initialization and prior to making any ad requests... (https://sdk.verizonmedia.com/gdpr-coppa.html)
            updatePrivacyStates( parameters );
            updateLocationCollectionEnabled( parameters );
        }
        else
        {
            onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return YASAds.getSDKInfo().getEditionId();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log( "Destroying adapter" );

        if ( interstitialAd != null )
        {
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.destroy();
            rewardedAd = null;
        }

        if ( inlineAdView != null )
        {
            inlineAdView.destroy();
            inlineAdView = null;
        }

        nativeAd = null;
    }

    //region MAX Signal Provider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateLocationCollectionEnabled( parameters );

        String signal = YASAds.getBiddingToken( getContext( activity ) );
        if ( signal == null )
        {
            callback.onSignalCollectionFailed( "Yahoo Mobile SDK not initialized; failed to return a bid." );
            return;
        }

        callback.onSignalCollected( signal );
    }

    //endregion

    //region MAX Interstitial Adapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad for placement: '" + placementId + "'..." );

        updatePrivacyStates( parameters );
        updateLocationCollectionEnabled( parameters );

        InterstitialListener interstitialListener = new InterstitialListener( listener );
        interstitialAd = new InterstitialAd( getContext( activity ), placementId, interstitialListener );

        RequestMetadata requestMetadata = createRequestMetadata( parameters.getBidResponse() );
        final InterstitialPlacementConfig config = new InterstitialPlacementConfig( placementId, requestMetadata );

        ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
        activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );

        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                interstitialAd.load( config );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: '" + parameters.getThirdPartyAdPlacementId() + "'..." );

        if ( interstitialAd == null )
        {
            log( "Unable to show interstitial - no ad loaded" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );

            return;
        }

        interstitialAd.show( getContext( activity ) );
    }

    //endregion

    //region MAX Rewarded Adapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad for placement: '" + placementId + "'..." );

        updatePrivacyStates( parameters );
        updateLocationCollectionEnabled( parameters );

        RewardedListener rewardedListener = new RewardedListener( listener );
        rewardedAd = new InterstitialAd( activity, placementId, rewardedListener );

        RequestMetadata requestMetadata = createRequestMetadata( parameters.getBidResponse() );
        final InterstitialPlacementConfig config = new InterstitialPlacementConfig( placementId, requestMetadata );

        ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
        activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );

        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                rewardedAd.load( config );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: '" + parameters.getThirdPartyAdPlacementId() + "'..." );

        if ( rewardedAd == null )
        {
            log( "Unable to show rewarded ad - no ad loaded" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );

            return;
        }

        configureReward( parameters );
        rewardedAd.show( getContext( activity ) );
    }

    //endregion

    //region MAX Ad View Adapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + adFormat.getLabel() + " for placement: '" + placementId + "'..." );

        updatePrivacyStates( parameters );
        updateLocationCollectionEnabled( parameters );

        AdSize adSize = toAdSize( adFormat );
        AdViewListener adViewListener = new AdViewListener( listener );
        inlineAdView = new InlineAdView( activity, placementId, adViewListener );

        RequestMetadata requestMetadata = createRequestMetadata( parameters.getBidResponse() );
        final InlinePlacementConfig config = new InlinePlacementConfig( placementId, requestMetadata, Collections.singletonList( adSize ) );

        ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
        activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );

        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                inlineAdView.load( config );
            }
        } );
    }

    //endregion

    //region MAX Native Ad Adapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "native ad for placement: '" + placementId + "'..." );

        updatePrivacyStates( parameters );
        updateLocationCollectionEnabled( parameters );

        // Yahoo requires us to pass them an activity for their viewability and impression tracking.
        // Note: if the provided activity is different from the final activity that the native ad view is attached,
        // Yahoo may fail to fire impression tracking. The pub would need to load and attach the native ad with the same activity.
        if ( activity == null )
        {
            e( "Native ad (" + placementId + ") failed to load: activity reference is null..." );
            listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

            return;
        }

        Context context = getContext( activity );

        NativeAdListener nativeAdListener = new NativeAdListener( parameters.getServerParameters(), activity, listener );
        nativeAd = new NativeAd( context, placementId, nativeAdListener );

        RequestMetadata requestMetadata = createRequestMetadata( bidResponse );
        final NativePlacementConfig config = new NativePlacementConfig( placementId, requestMetadata, NATIVE_AD_AD_TYPES );

        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                nativeAd.load( config );
            }
        } );
    }

    //endregion

    //region Helper Methods

    private void updatePrivacyStates(final MaxAdapterParameters parameters)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11040399 )
        {
            if ( parameters.getConsentString() != null )
            {
                YASAds.addConsent( new GdprConsent( parameters.getConsentString() ) );
            }
        }

        // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
        // https://support.google.com/googleplay/android-developer/answer/9283445?hl=en
        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null && isAgeRestrictedUser )
        {
            YASAds.applyCoppa();
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                YASAds.applyCcpa();
                YASAds.addConsent( new CcpaConsent( isDoNotSell ? "1YY-" : "1YN-" ) );
            }
            else
            {
                YASAds.addConsent( new CcpaConsent( "1---" ) );
            }
        }
    }

    private void updateLocationCollectionEnabled(final MaxAdapterParameters parameters)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11_00_00_00 )
        {
            Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
            Object isLocationCollectionEnabledObj = localExtraParameters.get( "is_location_collection_enabled" );
            if ( isLocationCollectionEnabledObj instanceof Boolean )
            {
                log( "Setting location collection: " + isLocationCollectionEnabledObj );
                YASAds.setLocationAccessMode( (boolean) isLocationCollectionEnabledObj ? YASAds.LocationAccessMode.PRECISE : YASAds.LocationAccessMode.DENIED );
            }
            else if ( isLocationCollectionEnabledObj != null )
            {
                log( "Location collection could not be set - Boolean type is required." );
            }
        }
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

    private RequestMetadata createRequestMetadata(final String bidResponse)
    {
        RequestMetadata.Builder builder = new RequestMetadata.Builder();
        builder.setMediator( mediationTag() );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            Map<String, Object> placementData = new HashMap<>( 2 );
            placementData.put( "adContent", bidResponse );
            placementData.put( "overrideWaterfallProvider", "waterfallprovider/sideloading" );

            builder.setPlacementData( placementData );
        }

        return builder.build();
    }

    private static MaxAdapterError toMaxError(final ErrorInfo verizonAdsError)
    {
        final int verizonErrorCode = verizonAdsError.getErrorCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( verizonErrorCode )
        {
            case YASAds.ERROR_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case YASAds.ERROR_AD_REQUEST_TIMED_OUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case YASAds.ERROR_AD_REQUEST_FAILED:
            case YASAds.ERROR_AD_REQUEST_FAILED_APP_IN_BACKGROUND:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), verizonErrorCode, verizonAdsError.getDescription() );
    }

    private AdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return new AdSize( 320, 50 );
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return new AdSize( 300, 250 );
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return new AdSize( 728, 90 );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    //endregion

    private class InterstitialListener
            implements InterstitialAd.InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(final InterstitialAd interstitialAd)
        {
            log( "Interstitial ad loaded" );

            VerizonAdsMediationAdapter.this.interstitialAd = interstitialAd;

            CreativeInfo creativeInfo = interstitialAd.getCreativeInfo();
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && creativeInfo != null && AppLovinSdkUtils.isValidString( creativeInfo.getCreativeId() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeInfo.getCreativeId() );

                listener.onInterstitialAdLoaded( extraInfo );
            }
            else
            {
                listener.onInterstitialAdLoaded();
            }
        }

        @Override
        public void onLoadFailed(InterstitialAd interstitialAd, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Interstitial ad (" + interstitialAd.getPlacementId() + ") load failed with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onError(final InterstitialAd interstitialAd, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Interstitial ad (" + interstitialAd.getPlacementId() + ") load failed with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onShown(final InterstitialAd interstitialAd)
        {
            log( "Interstitial ad shown" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClicked(final InterstitialAd interstitialAd)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdLeftApplication(final InterstitialAd interstitialAd)
        {
            log( "Interstitial ad left application" );
        }

        @Override
        public void onClosed(final InterstitialAd interstitialAd)
        {
            log( "Interstitial ad closed" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onEvent(final InterstitialAd interstitialAd, final String source, final String eventId, final Map<String, Object> arguments)
        {
            log( "Interstitial ad event from source: " + source + " with event ID: " + eventId + " and arguments: " + arguments );

            if ( AD_IMPRESSION_EVENT_ID.equals( eventId ) )
            {
                listener.onInterstitialAdDisplayed();
            }
        }
    }

    private class RewardedListener
            implements InterstitialAd.InterstitialAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(final InterstitialAd rewardedAd)
        {
            log( "Interstitial ad loaded" );

            VerizonAdsMediationAdapter.this.rewardedAd = rewardedAd;

            CreativeInfo creativeInfo = rewardedAd.getCreativeInfo();
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && creativeInfo != null && AppLovinSdkUtils.isValidString( creativeInfo.getCreativeId() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeInfo.getCreativeId() );

                listener.onRewardedAdLoaded( extraInfo );
            }
            else
            {
                listener.onRewardedAdLoaded();
            }
        }

        @Override
        public void onLoadFailed(final InterstitialAd rewardedAd, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Rewarded ad (" + rewardedAd.getPlacementId() + ") load failed with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onError(final InterstitialAd rewardedAd, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Rewarded ad (" + rewardedAd.getPlacementId() + ") load failed with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onShown(final InterstitialAd rewardedAd)
        {
            log( "Rewarded ad shown" );

            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onClicked(final InterstitialAd rewardedAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdLeftApplication(final InterstitialAd rewardedAd)
        {
            log( "Rewarded ad left application" );
        }

        @Override
        public void onClosed(final InterstitialAd rewardedAd)
        {
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad closed" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onEvent(final InterstitialAd rewardedAd, final String source, final String eventId, final Map<String, Object> arguments)
        {
            log( "Rewarded ad event from source: " + source + " with event ID: " + eventId + " and arguments: " + arguments );

            if ( AD_IMPRESSION_EVENT_ID.equals( eventId ) )
            {
                listener.onRewardedAdDisplayed();
            }
            else if ( VIDEO_COMPLETED_EVENT_ID.equals( eventId ) )
            {
                hasGrantedReward = true;
            }
        }
    }

    private class AdViewListener
            implements InlineAdView.InlineAdListener
    {
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(final InlineAdView inlineAdView)
        {
            log( "AdView loaded: " + inlineAdView.getPlacementId() );

            VerizonAdsMediationAdapter.this.inlineAdView = inlineAdView;

            CreativeInfo creativeInfo = inlineAdView.getCreativeInfo();
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && creativeInfo != null && AppLovinSdkUtils.isValidString( creativeInfo.getCreativeId() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeInfo.getCreativeId() );

                listener.onAdViewAdLoaded( inlineAdView, extraInfo );
            }
            else
            {
                listener.onAdViewAdLoaded( inlineAdView );
            }
        }

        @Override
        public void onLoadFailed(final InlineAdView inlineAdView, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "AdView ad (" + inlineAdView.getPlacementId() + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onError(final InlineAdView inlineAdView, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "AdView ad (" + inlineAdView.getPlacementId() + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onClicked(final InlineAdView inlineAdView)
        {
            log( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final InlineAdView inlineAdView)
        {
            log( "AdView left application" );
        }

        @Override
        public void onExpanded(final InlineAdView inlineAdView)
        {
            log( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onCollapsed(final InlineAdView inlineAdView)
        {
            log( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdRefreshed(final InlineAdView inlineAdView)
        {
            log( "AdView refreshed" );
        }

        @Override
        public void onResized(final InlineAdView inlineAdView)
        {
            log( "AdView resized" );
        }

        @Override
        public void onEvent(final InlineAdView inlineAdView, final String source, final String eventId, final Map<String, Object> arguments)
        {
            log( "AdView event from source: " + source + " with event ID: " + eventId + " and arguments: " + arguments );

            if ( AD_IMPRESSION_EVENT_ID.equals( eventId ) )
            {
                listener.onAdViewAdDisplayed();
            }
        }
    }

    private class NativeAdListener
            implements NativeAd.NativeAdListener
    {
        private final Bundle                     serverParameters;
        private final WeakReference<Activity>    activityRef;
        private final MaxNativeAdAdapterListener listener;

        private NativeAdListener(final Bundle serverParameters, final Activity activity, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = serverParameters;
            this.activityRef = new WeakReference<>( activity );
            this.listener = listener;
        }

        @Override
        public void onLoaded(final NativeAd nativeAd)
        {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    log( "Native ad loaded: " + nativeAd.getPlacementId() );

                    String title = null;
                    String body = null;
                    String advertiser = null;
                    String callToAction = null;

                    NativeTextComponent titleComponent = (NativeTextComponent) nativeAd.getComponent( "title" );
                    if ( titleComponent != null ) title = titleComponent.getText();

                    NativeTextComponent advertiserComponent = (NativeTextComponent) nativeAd.getComponent( "disclaimer" );
                    if ( advertiserComponent != null ) advertiser = advertiserComponent.getText();

                    NativeTextComponent bodyComponent = (NativeTextComponent) nativeAd.getComponent( "body" );
                    if ( bodyComponent != null ) body = bodyComponent.getText();

                    NativeTextComponent ctaComponent = (NativeTextComponent) nativeAd.getComponent( "callToAction" );
                    if ( ctaComponent != null ) callToAction = ctaComponent.getText();

                    final Activity activity = activityRef.get();
                    if ( activity == null )
                    {
                        e( "Native ad (" + nativeAd + ") failed to load: activity reference is null when ad is loaded" );
                        listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                        return;
                    }

                    // NOTE: Yahoo's SDK only returns ImageView with the image pre-cached, we cannot use the 'getUri()' getter
                    // since it is un-cached and our SDK will attempt to re-cache it, and we do not support passing ImageView for custom native
                    MaxNativeAd.MaxNativeAdImage iconImage = null;
                    NativeImageComponent iconComponent = (NativeImageComponent) nativeAd.getComponent( "iconImage" );
                    if ( iconComponent != null )
                    {
                        ImageView iconView = new ImageView( activity );
                        iconComponent.prepareView( iconView );

                        Drawable drawable = iconView.getDrawable();
                        if ( drawable != null )
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( drawable );
                        }
                    }

                    View mediaView = null;
                    float mediaViewAspectRatio = 0.0f;
                    MaxNativeAd.MaxNativeAdImage mainImage = null;
                    NativeVideoComponent nativeVideoComponent = (NativeVideoComponent) nativeAd.getComponent( "video" );
                    NativeImageComponent nativeImageComponent = (NativeImageComponent) nativeAd.getComponent( "mainImage" );

                    // If video is available, use that
                    if ( nativeVideoComponent != null )
                    {
                        mediaViewAspectRatio = (float) nativeVideoComponent.getWidth() / (float) nativeVideoComponent.getHeight();

                        mediaView = new VideoPlayerView( activity );
                        nativeVideoComponent.prepareView( (VideoPlayerView) mediaView );
                    }
                    else if ( nativeImageComponent != null )
                    {
                        mediaViewAspectRatio = (float) nativeImageComponent.getWidth() / (float) nativeImageComponent.getHeight();

                        mediaView = new ImageView( activity );
                        nativeImageComponent.prepareView( (ImageView) mediaView );

                        Drawable drawable = ( (ImageView) mediaView ).getDrawable();
                        if ( drawable != null )
                        {
                            mainImage = new MaxNativeAd.MaxNativeAdImage( drawable );
                        }
                    }

                    String templateName = BundleUtils.getString( "template", "", serverParameters );
                    boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
                    if ( isTemplateAd && TextUtils.isEmpty( title ) )
                    {
                        e( "Native ad (" + nativeAd + ") does not have required assets." );
                        listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                        return;
                    }

                    VerizonAdsMediationAdapter.this.nativeAd = nativeAd;

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( title )
                            .setBody( body )
                            .setAdvertiser( advertiser )
                            .setCallToAction( callToAction )
                            .setIcon( iconImage )
                            .setMediaView( mediaView );

                    if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
                    {
                        builder.setMainImage( mainImage );
                    }

                    if ( AppLovinSdk.VERSION_CODE >= 11_04_00_00 )
                    {
                        builder.setMediaContentAspectRatio( mediaViewAspectRatio );
                    }

                    MaxNativeAd maxNativeAd = new MaxYahooNativeAd( activity, builder );

                    CreativeInfo creativeInfo = nativeAd.getCreativeInfo();
                    Bundle extraInfo = new Bundle( 1 );
                    if ( creativeInfo != null && AppLovinSdkUtils.isValidString( creativeInfo.getCreativeId() ) )
                    {
                        extraInfo.putString( "creative_id", creativeInfo.getCreativeId() );
                    }

                    listener.onNativeAdLoaded( maxNativeAd, extraInfo );
                }
            } );
        }

        @Override
        public void onLoadFailed(final NativeAd nativeAd, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Native ad factory (" + nativeAd.getPlacementId() + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Native ad (" + nativeAd.getPlacementId() + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onClicked(final NativeAd nativeAd, final NativeComponent nativeComponent)
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdLeftApplication(final NativeAd nativeAd)
        {
            log( "Native ad left application" );
        }

        @Override
        public void onClosed(final NativeAd nativeAd)
        {
            log( "Native ad closed" );
        }

        @Override
        public void onEvent(final NativeAd nativeAd, final String source, final String eventId, final Map<String, Object> arguments)
        {
            log( "Native event from source: " + source + " with event ID: " + eventId + " and arguments: " + arguments );

            if ( AD_IMPRESSION_EVENT_ID.equals( eventId ) )
            {
                listener.onNativeAdDisplayed( null );
            }
        }
    }

    private class MaxYahooNativeAd
            extends MaxNativeAd
    {
        private final WeakReference<Activity> activityRef;

        private MaxYahooNativeAd(final Activity activity, final Builder builder)
        {
            super( builder );

            this.activityRef = new WeakReference<>( activity );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            final NativeAd nativeAd = VerizonAdsMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view for interaction. Native ad is null." );
                return;
            }

            final Activity activity = activityRef.get();
            if ( activity == null )
            {
                e( "Native ad (" + nativeAd + ") failed to prepare native view for interaction. Activity reference is null." );
                return;
            }

            //
            // Now that we have access to the native ad view's UI assets, call `prepareView()` where applicable
            //

            NativeTextComponent titleComponent = (NativeTextComponent) nativeAd.getComponent( "title" );
            NativeTextComponent disclaimerComponent = (NativeTextComponent) nativeAd.getComponent( "disclaimer" );
            NativeTextComponent bodyComponent = (NativeTextComponent) nativeAd.getComponent( "body" );
            NativeTextComponent ctaComponent = (NativeTextComponent) nativeAd.getComponent( "callToAction" );
            NativeImageComponent iconComponent = (NativeImageComponent) nativeAd.getComponent( "iconImage" );

            if ( titleComponent != null && maxNativeAdView.getTitleTextView() != null )
            {
                titleComponent.prepareView( maxNativeAdView.getTitleTextView() );
            }
            if ( disclaimerComponent != null && maxNativeAdView.getAdvertiserTextView() != null )
            {
                disclaimerComponent.prepareView( maxNativeAdView.getAdvertiserTextView() );
            }
            if ( bodyComponent != null && maxNativeAdView.getBodyTextView() != null )
            {
                bodyComponent.prepareView( maxNativeAdView.getBodyTextView() );
            }
            if ( ctaComponent != null && maxNativeAdView.getCallToActionButton() != null )
            {
                ctaComponent.prepareView( maxNativeAdView.getCallToActionButton() );
            }
            if ( iconComponent != null && maxNativeAdView.getIconImageView() != null )
            {
                iconComponent.prepareView( maxNativeAdView.getIconImageView() );
            }

            ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );

            nativeAd.registerContainerView( maxNativeAdView, activity );
        }
    }

    //endregion
}
