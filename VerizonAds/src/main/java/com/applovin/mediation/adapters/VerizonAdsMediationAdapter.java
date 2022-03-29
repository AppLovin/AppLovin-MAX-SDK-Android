package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
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
import com.applovin.mediation.adapters.MediationAdapterBase;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.yahoo.ads.ActivityStateManager;
import com.yahoo.ads.CcpaConsent;
import com.yahoo.ads.CreativeInfo;
import com.yahoo.ads.ErrorInfo;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    // Ad objects
    private InterstitialAd interstitialAd;
    private InterstitialAd rewardedAd;
    private InlineAdView inlineAdView;
    private NativeAd nativeAd;

    public VerizonAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region Max Adapter Methods
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( !YASAds.isInitialized() )
        {
            final Bundle serverParameters = parameters.getServerParameters();
            final String siteId = serverParameters.getString( PARAMETER_SITE_ID );
            log( "Initializing Yahoo Mobile SDK with site id: " + siteId + "..." );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Application application = ( activity != null ) ? activity.getApplication() : (Application) getApplicationContext();

            final boolean initialized = YASAds.initialize( application, siteId );

            InitializationStatus status = initialized ? InitializationStatus.INITIALIZED_SUCCESS : InitializationStatus.INITIALIZED_FAILURE;
            onCompletionListener.onCompletion( status, null );

            // ...GDPR settings, which is part of verizon Ads SDK data, should be established after initialization and prior to making any ad requests... (https://sdk.verizonmedia.com/gdpr-coppa.html)
            updateVerizonAdsSdkData( parameters );
        }
        else
        {
            onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return "1.14.0";
    }

    @Override
    public String getAdapterVersion()
    {
        return "1.14.0.7";
    }

    @Override
    public void onDestroy()
    {
        log( "Destroying Verizon Ads adapter" );

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


        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
        }
    }
    //endregion

    //region Max Interstitial Adapter Methods
    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad for placement: '" + placementId + "'..." );

        InterstitialListener interstitialListener = new InterstitialListener( listener );
        interstitialAd = new InterstitialAd(activity, placementId, interstitialListener);

        Bundle serverParameters = parameters.getServerParameters();
        RequestMetadata requestMetadata = createRequestMetadata( serverParameters, parameters.getBidResponse() );
        InterstitialPlacementConfig interstitialPlacementConfig = new InterstitialPlacementConfig(placementId, requestMetadata);

        updateVerizonAdsSdkData( parameters );

        ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
        if ( activityStateManager != null )
        {
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );
        }

        interstitialAd.load(interstitialPlacementConfig);
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: '" + parameters.getThirdPartyAdPlacementId() + "'..." );

        if ( interstitialAd == null )
        {
            log( "Unable to show interstitial - no ad loaded" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );

            return;
        }

        interstitialAd.show( activity );
    }
    //endregion

    //region Max Rewarded Adapter Methods
    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad for placement: '" + placementId + "'..." );

        final RewardedListener rewardedListener = new RewardedListener( listener );
        rewardedAd = new InterstitialAd(activity, placementId, rewardedListener);

        final Bundle serverParameters = parameters.getServerParameters();
        final RequestMetadata requestMetadata = createRequestMetadata( serverParameters, parameters.getBidResponse() );
        InterstitialPlacementConfig interstitialPlacementConfig = new InterstitialPlacementConfig(placementId, requestMetadata);

        updateVerizonAdsSdkData( parameters );

        ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
        if ( activityStateManager != null )
        {
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );
        }

        rewardedAd.load( interstitialPlacementConfig );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: '" + parameters.getThirdPartyAdPlacementId() + "'..." );

        if ( rewardedAd == null )
        {
            log( "Unable to show rewarded ad - no ad loaded" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_NOT_READY );

            return;
        }

        configureReward( parameters );
        rewardedAd.show( activity );
    }
    //endregion

    //region Max Ad View Adapter Methods
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + adFormat.getLabel() + " for placement: '" + placementId + "'..." );

        AdSize adSize = toAdSize( adFormat );
        AdViewListener adViewListener = new AdViewListener( listener );
        inlineAdView = new InlineAdView(activity, placementId, adViewListener);

        Bundle serverParameters = parameters.getServerParameters();
        RequestMetadata requestMetadata = createRequestMetadata( serverParameters, parameters.getBidResponse() );
        InlinePlacementConfig inlinePlacementConfig = new InlinePlacementConfig(placementId, requestMetadata, Collections.singletonList(adSize));

        updateVerizonAdsSdkData( parameters );

        ActivityStateManager activityStateManager = YASAds.getActivityStateManager();
        if ( activityStateManager != null )
        {
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );
        }

        inlineAdView.load( inlinePlacementConfig );
    }
    //endregion

    //region Max Native Ad Adapter Methods
    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "native ad for placement: '" + placementId + "'..." );

        Bundle serverParameters = parameters.getServerParameters();
        NativeAdListener nativeAdListener = new NativeAdListener( serverParameters, activity, listener );
        nativeAd = new NativeAd( activity, placementId, nativeAdListener );

        RequestMetadata requestMetadata = createRequestMetadata( serverParameters, bidResponse );
        NativePlacementConfig nativePlacementConfig = new NativePlacementConfig(placementId, requestMetadata, new String[] { "simpleImage", "simpleVideo" });

        updateVerizonAdsSdkData( parameters );
        AppLovinSdkUtils.runOnUiThread(() ->
            nativeAd.load( nativePlacementConfig )
        );
    }
    //endregion

    //region Max Signal Provider Methods
    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        String signal = YASAds.getBiddingToken( activity );
        if ( signal == null )
        {
            callback.onSignalCollectionFailed( "Yahoo Mobile SDK not initialized; failed to return a bid." );

            return;
        }

        callback.onSignalCollected( signal );
    }
    //endregion

    //region Helper Methods
    private void updateVerizonAdsSdkData(final MaxAdapterParameters parameters)
    {
        final int logLevel = parameters.isTesting() ? Logger.VERBOSE : Logger.ERROR;
        YASAds.setLogLevel( Logger.VERBOSE );

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null && isAgeRestrictedUser)
        {
            YASAds.applyCoppa();
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                YASAds.applyCcpa();
                YASAds.addConsent(new CcpaConsent(isDoNotSell ? "1YY-" : "1YN-"));
            }
            else
            {
                YASAds.addConsent(new CcpaConsent( "1---" ));
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

    private RequestMetadata createRequestMetadata(final Bundle serverParameters, final String bidResponse)
    {
        final RequestMetadata.Builder builder = new RequestMetadata.Builder();

        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            final Map<String, Object> placementData = new HashMap<>();
            placementData.put( "adContent", bidResponse );
            placementData.put( "overrideWaterfallProvider", "waterfallprovider/sideloading" );

            builder.setPlacementData( placementData );
        }

        builder.setMediator( mediationTag() );

        if ( serverParameters.containsKey( "user_age" ) )
        {
            builder.setUserAge( serverParameters.getInt( "user_age" ) );
        }

        if ( serverParameters.containsKey( "user_children" ) )
        {
            builder.setUserChildren( serverParameters.getInt( "user_children" ) );
        }

        if ( serverParameters.containsKey( "user_education" ) )
        {
            final String educationString = serverParameters.getString( "user_education" );
            final RequestMetadata.Education education = RequestMetadata.Education.valueOf( educationString );
            builder.setUserEducation( education );
        }

        if ( serverParameters.containsKey( "user_gender" ) )
        {
            final String genderString = serverParameters.getString( "user_gender" );
            final RequestMetadata.Gender gender = RequestMetadata.Gender.valueOf( genderString );
            builder.setUserGender( gender );
        }

        if ( serverParameters.containsKey( "user_marital_status" ) )
        {
            final String maritalStatusString = serverParameters.getString( "user_marital_status" );
            final RequestMetadata.MaritalStatus maritalStatus = RequestMetadata.MaritalStatus.valueOf( maritalStatusString );
            builder.setUserMaritalStatus( maritalStatus );
        }

        if ( serverParameters.containsKey( "dob" ) )
        {
            final Date date = new Date( serverParameters.getLong( "dob" ) );
            builder.setUserDob( date );
        }

        if ( serverParameters.containsKey( "user_state" ) )
        {
            builder.setUserState( serverParameters.getString( "user_state" ) );
        }

        if ( serverParameters.containsKey( "user_country" ) )
        {
            builder.setUserCountry( serverParameters.getString( "user_country" ) );
        }

        if ( serverParameters.containsKey( "user_postal_code" ) )
        {
            builder.setUserPostalCode( serverParameters.getString( "user_postal_code" ) );
        }

        if ( serverParameters.containsKey( "user_dma" ) )
        {
            builder.setUserDma( serverParameters.getString( "user_dma" ) );
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
    //endregion

    //region Listeners implementation
    private class InterstitialListener implements InterstitialAd.InterstitialAdListener
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
        }
    }

    private class RewardedListener implements InterstitialAd.InterstitialAdListener
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

            if ( VIDEO_COMPLETED_EVENT_ID.equals( eventId ) )
            {
                hasGrantedReward = true;
            }
        }
    }

    private class AdViewListener implements InlineAdView.InlineAdListener
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

            // Disable AdView ad refresh by setting the refresh interval to max value.
//            inlineAdView.setRefreshInterval( Integer.MAX_VALUE );

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
        }
    }

    private class NativeAdListener implements NativeAd.NativeAdListener
    {
        private final Bundle                     serverParameters;
        private final WeakReference<Activity>    activityRef;
        private final MaxNativeAdAdapterListener listener;

        private NativeAdListener(final Bundle serverParameters, final Activity activity, final MaxNativeAdAdapterListener listener)
        {
            activityRef = new WeakReference<>( activity );

            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onLoaded(final NativeAd nativeAd)
        {
            log( "Native ad loaded: " + nativeAd.getPlacementId() );

            final Activity activity = activityRef.get();
            if ( activity == null )
            {
                log( "Native ad failed to load: activity reference is null when ad is loaded" );
                listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                nativeAd.destroy();

                return;
            }

            AppLovinSdkUtils.runOnUiThread(() -> {
                String title = null;
                String disclaimer = null;
                String body = null;
                String callToAction = null;

                NativeTextComponent titleComponent = (NativeTextComponent)nativeAd.getComponent("title");
                if (titleComponent != null) {
                    title = titleComponent.getText();
                }

                NativeTextComponent disclaimerComponent = (NativeTextComponent)nativeAd.getComponent("disclaimer");
                if (disclaimerComponent != null) {
                    disclaimer = disclaimerComponent.getText();
                }

                NativeTextComponent bodyComponent = (NativeTextComponent)nativeAd.getComponent("body");
                if (bodyComponent != null) {
                    body = bodyComponent.getText();
                }

                NativeTextComponent ctaComponent = (NativeTextComponent)nativeAd.getComponent("callToAction");
                if (ctaComponent != null) {
                    callToAction = ctaComponent.getText();
                }

                final NativeImageComponent iconComponent = (NativeImageComponent) nativeAd.getComponent( "iconImage" );
                ImageView iconView= null;
                if (iconComponent != null) {
                    iconView = new ImageView(activity);
                    iconComponent.prepareView(iconView);

                }
                final NativeVideoComponent nativeVideoComponent = (NativeVideoComponent)nativeAd.getComponent("video");
                final NativeImageComponent nativeImageComponent = (NativeImageComponent)nativeAd.getComponent("mainImage");

                View mediaView = null;

                if(nativeVideoComponent != null) {
                    mediaView = new VideoPlayerView(activity);
                    nativeVideoComponent.prepareView((VideoPlayerView)mediaView);
                } else if (nativeImageComponent != null) {
                    mediaView = new ImageView(activity);
                    nativeImageComponent.prepareView((ImageView)mediaView);
                }

                String templateName = BundleUtils.getString( "template", "", serverParameters );
                boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
                boolean missingRequiredAssets = false;
                if ( isTemplateAd && TextUtils.isEmpty( title ) )
                {
                    missingRequiredAssets = true;
                }
                else if ( TextUtils.isEmpty( title )
                        || TextUtils.isEmpty( callToAction )
                        || mediaView == null )
                {
                    missingRequiredAssets = true;
                }

                if ( missingRequiredAssets )
                {
                    e( "Custom native ad (" + nativeAd + ") does not have required assets." );
                    listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                    return;
                }

                VerizonAdsMediationAdapter.this.nativeAd = nativeAd;

                final String finalTitle = title;
                final String finalDisclaimer = disclaimer;
                final String finalBody = body;
                final String finalCallToAction = callToAction;
                final View finalMediaView = mediaView;
                final View finalIconView = iconView;

                MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( finalTitle )
                            .setAdvertiser( finalDisclaimer )
                            .setBody( finalBody )
                            .setCallToAction( finalCallToAction )
                            .setIconView( finalIconView )
                            .setMediaView( finalMediaView );
                MaxNativeAd maxNativeAd = new MaxYahooNativeAd( builder );

                CreativeInfo creativeInfo = nativeAd.getCreativeInfo();
                Bundle extraInfo = new Bundle( 1 );
                if ( creativeInfo != null && AppLovinSdkUtils.isValidString( creativeInfo.getCreativeId() ) )
                {
                    extraInfo.putString( "creative_id", creativeInfo.getCreativeId() );
                }

                listener.onNativeAdLoaded( maxNativeAd, extraInfo );
            });
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

    private class MaxYahooNativeAd extends MaxNativeAd {

        public MaxYahooNativeAd(Builder builder) {
            super(builder);
        }

        @Override
        public void prepareViewForInteraction(MaxNativeAdView maxNativeAdView) {
            final NativeTextComponent titleComponent = (NativeTextComponent)nativeAd.getComponent("title");
            final NativeTextComponent disclaimerComponent = (NativeTextComponent)nativeAd.getComponent("disclaimer");
            final NativeTextComponent bodyComponent = (NativeTextComponent)nativeAd.getComponent("body");
            final NativeTextComponent ctaComponent = (NativeTextComponent)nativeAd.getComponent("callToAction");

            if (titleComponent != null && maxNativeAdView.getTitleTextView() != null) {
                titleComponent.prepareView(maxNativeAdView.getTitleTextView());
            }
            if (disclaimerComponent != null && maxNativeAdView.getAdvertiserTextView() != null) {
                disclaimerComponent.prepareView(maxNativeAdView.getAdvertiserTextView());
            }
            if (bodyComponent != null && maxNativeAdView.getBodyTextView() != null) {
                bodyComponent.prepareView(maxNativeAdView.getBodyTextView());
            }
            if (ctaComponent != null && maxNativeAdView.getCallToActionButton() != null) {
                ctaComponent.prepareView(maxNativeAdView.getCallToActionButton());
            }

            nativeAd.registerContainerView(maxNativeAdView);
        }
    }
    //endregion
}
