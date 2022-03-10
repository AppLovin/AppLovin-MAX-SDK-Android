package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

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
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.Component;
import com.verizon.ads.CreativeInfo;
import com.verizon.ads.DataPrivacy;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.Logger;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.inlineplacement.AdSize;
import com.verizon.ads.inlineplacement.InlineAdFactory;
import com.verizon.ads.inlineplacement.InlineAdView;
import com.verizon.ads.interstitialplacement.InterstitialAd;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.nativeplacement.NativeAdFactory;
import com.verizon.ads.verizonnativecontroller.NativeImageComponent;
import com.verizon.ads.verizonnativecontroller.NativeVideoComponent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by santoshbagadi on 2/27/19.
 */
public class VerizonAdsMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxSignalProvider /* MaxNativeAdAdapter */
{
    private static final int DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 10;

    // Server parameters
    private static final String PARAMETER_SITE_ID = "site_id";

    // Event IDs
    private static final String VIDEO_COMPLETED_EVENT_ID = "onVideoComplete";
    private static final String AD_IMPRESSION_EVENT_ID   = "adImpression";

    // Ad objects
    private InterstitialAd interstitialAd;
    private InterstitialAd rewardedAd;
    private InlineAdView   inlineAdView;
    private NativeAd       nativeAd;

    // Factory objects require cleanup
    private InterstitialAdFactory interstitialAdFactory;
    private InlineAdFactory       inlineAdFactory;
    private NativeAdFactory       nativeAdFactory;

    public VerizonAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region Max Adapter Methods
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( !VASAds.isInitialized() )
        {
            final Bundle serverParameters = parameters.getServerParameters();
            final String siteId = serverParameters.getString( PARAMETER_SITE_ID );
            log( "Initializing Verizon Ads SDK with site id: " + siteId + "..." );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Application application = ( activity != null ) ? activity.getApplication() : (Application) getApplicationContext();

            final boolean initialized = VASAds.initialize( application, siteId );

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
        return getVersionString( com.verizon.ads.edition.BuildConfig.class, "VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return com.applovin.mediation.adapters.verizonads.BuildConfig.VERSION_NAME;
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

        if ( interstitialAdFactory != null )
        {
            interstitialAdFactory.setListener( null );
            interstitialAdFactory.destroy();
            interstitialAdFactory = null;
        }

        if ( inlineAdView != null )
        {
            inlineAdView.destroy();
            inlineAdView = null;
        }

        if ( inlineAdFactory != null )
        {
            inlineAdFactory.setListener( null );
            inlineAdFactory.destroy();
            inlineAdFactory = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
        }

        if ( nativeAdFactory != null )
        {
            nativeAdFactory.setListener( null );
            nativeAdFactory.destroy();
            nativeAdFactory = null;
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
        interstitialAdFactory = new InterstitialAdFactory( activity, placementId, interstitialListener );

        Bundle serverParameters = parameters.getServerParameters();
        RequestMetadata requestMetadata = createRequestMetadata( serverParameters, parameters.getBidResponse() );
        interstitialAdFactory.setRequestMetaData( requestMetadata );

        updateVerizonAdsSdkData( parameters );

        ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if ( activityStateManager != null )
        {
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );
        }

        interstitialAdFactory.load( interstitialListener );
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
        final InterstitialAdFactory rewardedAdFactory = new InterstitialAdFactory( activity, placementId, rewardedListener );

        final Bundle serverParameters = parameters.getServerParameters();
        final RequestMetadata requestMetadata = createRequestMetadata( serverParameters, parameters.getBidResponse() );
        rewardedAdFactory.setRequestMetaData( requestMetadata );

        updateVerizonAdsSdkData( parameters );

        ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if ( activityStateManager != null )
        {
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );
        }

        rewardedAdFactory.load( rewardedListener );
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
        inlineAdFactory = new InlineAdFactory( activity, placementId, Collections.singletonList( adSize ), adViewListener );

        Bundle serverParameters = parameters.getServerParameters();
        RequestMetadata requestMetadata = createRequestMetadata( serverParameters, parameters.getBidResponse() );
        inlineAdFactory.setRequestMetaData( requestMetadata );

        updateVerizonAdsSdkData( parameters );

        ActivityStateManager activityStateManager = VASAds.getActivityStateManager();
        if ( activityStateManager != null )
        {
            activityStateManager.setState( activity, ActivityStateManager.ActivityState.RESUMED );
        }

        inlineAdFactory.load( adViewListener );
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
        nativeAdFactory = new NativeAdFactory( activity, placementId, new String[] { "simpleImage", "simpleVideo" }, nativeAdListener );

        RequestMetadata requestMetadata = createRequestMetadata( serverParameters, bidResponse );
        nativeAdFactory.setRequestMetaData( requestMetadata );

        updateVerizonAdsSdkData( parameters );

        nativeAdFactory.load( nativeAdListener );
    }
    //endregion

    //region Max Signal Provider Methods
    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        String signal = VASAds.getBiddingToken( activity );
        if ( signal == null )
        {
            callback.onSignalCollectionFailed( "VerizonAds SDK not initialized; failed to return a bid." );

            return;
        }

        callback.onSignalCollected( signal );
    }
    //endregion

    //region Helper Methods
    private void updateVerizonAdsSdkData(final MaxAdapterParameters parameters)
    {
        final int logLevel = parameters.isTesting() ? Logger.VERBOSE : Logger.ERROR;
        VASAds.setLogLevel( logLevel );

        DataPrivacy.Builder builder = new DataPrivacy.Builder();

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            builder.setCoppaApplies( isAgeRestrictedUser );
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                builder.setCcpaPrivacy( isDoNotSell ? "1YY-" : "1YN-" );
            }
            else
            {
                builder.setCcpaPrivacy( "1---" );
            }
        }

        VASAds.setDataPrivacy( builder.build() );
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

        if ( serverParameters.containsKey( "user_income" ) )
        {
            builder.setUserIncome( serverParameters.getInt( "user_income" ) );
        }

        if ( serverParameters.containsKey( "user_education" ) )
        {
            final String educationString = serverParameters.getString( "user_education" );
            final RequestMetadata.Education education = RequestMetadata.Education.valueOf( educationString );
            builder.setUserEducation( education );
        }

        if ( serverParameters.containsKey( "user_ethnicity" ) )
        {
            final String ethnicityString = serverParameters.getString( "user_ethnicity" );
            final RequestMetadata.Ethnicity ethnicity = RequestMetadata.Ethnicity.valueOf( ethnicityString );
            builder.setUserEthnicity( ethnicity );
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

        if ( serverParameters.containsKey( "user_politics" ) )
        {
            final String politicsString = serverParameters.getString( "user_politics" );
            final RequestMetadata.Politics politics = RequestMetadata.Politics.valueOf( politicsString );
            builder.setUserPolitics( politics );
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
            case VASAds.ERROR_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case VASAds.ERROR_AD_REQUEST_TIMED_OUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case VASAds.ERROR_AD_REQUEST_FAILED:
            case VASAds.ERROR_AD_REQUEST_FAILED_APP_IN_BACKGROUND:
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
    private class InterstitialListener
            implements InterstitialAdFactory.InterstitialAdFactoryListener, InterstitialAd.InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(final InterstitialAdFactory interstitialAdFactory, final InterstitialAd interstitialAd)
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
        public void onError(final InterstitialAdFactory interstitialAdFactory, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Interstitial ad (" + interstitialAdFactory.getPlacementId() + ") load failed with error: " + adapterError );
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

    private class RewardedListener
            implements InterstitialAdFactory.InterstitialAdFactoryListener, InterstitialAd.InterstitialAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(final InterstitialAdFactory rewardedAdFactory, final InterstitialAd rewardedAd)
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
        public void onError(final InterstitialAdFactory rewardedAdFactory, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Rewarded ad (" + rewardedAdFactory.getPlacementId() + ") load failed with error: " + adapterError );
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

    private class AdViewListener
            implements InlineAdFactory.InlineAdFactoryListener, InlineAdView.InlineAdListener
    {
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoaded(final InlineAdFactory inlineAdFactory, final InlineAdView inlineAdView)
        {
            log( "AdView loaded: " + inlineAdFactory.getPlacementId() );

            // Disable AdView ad refresh by setting the refresh interval to max value.
            inlineAdView.setRefreshInterval( Integer.MAX_VALUE );

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
        public void onError(final InlineAdFactory inlineAdFactory, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "AdView ad (" + inlineAdFactory.getPlacementId() + ") failed to load with error: " + adapterError );
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

    private class NativeAdListener
            implements NativeAdFactory.NativeAdFactoryListener, NativeAd.NativeAdListener
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
        public void onLoaded(final NativeAdFactory nativeAdFactory, final NativeAd nativeAd)
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

            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    String title = null;
                    String disclaimer = null;
                    String body = null;
                    String callToAction = null;
                    try
                    {
                        // Verizon's `getText()` getter returns null, so we need reflection to get the text
                        Class<?> verizonNativeTextComponentClass = Class.forName( "com.verizon.ads.verizonnativecontroller.VerizonNativeTextComponent" );
                        Field field = verizonNativeTextComponentClass.getDeclaredField( "text" );
                        field.setAccessible( true );

                        Object titleObject = verizonNativeTextComponentClass.cast( nativeAd.getComponent( "title" ) );
                        title = (String) field.get( titleObject );

                        Object disclaimerObject = verizonNativeTextComponentClass.cast( nativeAd.getComponent( "disclaimer" ) );
                        disclaimer = (String) field.get( disclaimerObject );

                        Object bodyObject = verizonNativeTextComponentClass.cast( nativeAd.getComponent( "body" ) );
                        body = (String) field.get( bodyObject );

                        Object callToActionObject = verizonNativeTextComponentClass.cast( nativeAd.getComponent( "callToAction" ) );
                        callToAction = (String) field.get( callToActionObject );
                    }
                    catch ( Exception ignored ) {}

                    final NativeImageComponent iconComponent = (NativeImageComponent) nativeAd.getComponent( "iconImage" );
                    NativeImageComponent mediaImageComponent = (NativeImageComponent) nativeAd.getComponent( "mainImage" );
                    NativeVideoComponent mediaVideoComponent = (NativeVideoComponent) nativeAd.getComponent( "video" );

                    View mediaView = null;
                    if ( mediaVideoComponent != null && mediaVideoComponent.getView( activity ) != null )
                    {
                        mediaView = mediaVideoComponent.getView( activity );
                    }
                    else if ( mediaImageComponent != null && mediaImageComponent.getView( activity ) != null )
                    {
                        mediaView = mediaImageComponent.getView( activity );
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
                    getCachingExecutorService().submit( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Uri iconUri = iconComponent.getUri();
                            Future<Drawable> iconDrawableFuture = createDrawableFuture( iconUri.toString(), activity.getResources() );
                            MaxNativeAd.MaxNativeAdImage iconImage = null;
                            try
                            {
                                int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
                                Drawable iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );

                                if ( iconDrawable != null )
                                {
                                    iconImage = new MaxNativeAd.MaxNativeAdImage( iconDrawable );
                                }
                            }
                            catch ( Throwable th )
                            {
                                log( "Failed to fetch icon image from URL: " + iconUri, th );
                            }

                            MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat( MaxAdFormat.NATIVE )
                                    .setTitle( finalTitle )
                                    .setAdvertiser( finalDisclaimer )
                                    .setBody( finalBody )
                                    .setCallToAction( finalCallToAction )
                                    .setIcon( iconImage )
                                    .setMediaView( finalMediaView );
                            MaxNativeAd maxNativeAd = new MaxVerizonNativeAd( listener, builder );

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
            } );
        }

        @Override
        public void onError(final NativeAdFactory nativeAdFactory, final ErrorInfo errorInfo)
        {
            MaxAdapterError adapterError = toMaxError( errorInfo );
            log( "Native ad factory (" + nativeAdFactory.getPlacementId() + ") failed to load with error: " + adapterError );
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
        public void onClicked(final NativeAd nativeAd, final Component component)
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

    private class MaxVerizonNativeAd
            extends MaxNativeAd
    {
        private final MaxNativeAdAdapterListener listener;

        private MaxVerizonNativeAd(final MaxNativeAdAdapterListener listener, final Builder builder)
        {
            super( builder );

            this.listener = listener;
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView nativeAdView)
        {
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view for interaction. Native ad is null" );
                return;
            }

            final View.OnClickListener clickListener = new View.OnClickListener()
            {
                @Override
                public void onClick(final View view)
                {
                    log( "Native ad clicked from click listener" );

                    nativeAd.invokeDefaultAction( view.getContext() );
                    listener.onNativeAdClicked();
                }
            };

            // Verizon's click registration methods don't work with views when recycling is involved
            // so they told us to manually invoke it as AdMob does
            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    if ( nativeAdView.getTitleTextView() != null ) nativeAdView.getTitleTextView().setOnClickListener( clickListener );
                    if ( nativeAdView.getAdvertiserTextView() != null ) nativeAdView.getAdvertiserTextView().setOnClickListener( clickListener );
                    if ( nativeAdView.getBodyTextView() != null ) nativeAdView.getBodyTextView().setOnClickListener( clickListener );
                    if ( nativeAdView.getIconImageView() != null ) nativeAdView.getIconImageView().setOnClickListener( clickListener );
                    if ( nativeAdView.getCallToActionButton() != null ) nativeAdView.getCallToActionButton().setOnClickListener( clickListener );
                }
            } );
        }
    }
    //endregion
}
