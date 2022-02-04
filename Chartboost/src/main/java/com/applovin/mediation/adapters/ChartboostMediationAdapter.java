package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.chartboost.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.chartboost.sdk.Banner.BannerSize;
import com.chartboost.sdk.CBLocation;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostBanner;
import com.chartboost.sdk.ChartboostBannerListener;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Events.ChartboostCacheError;
import com.chartboost.sdk.Events.ChartboostCacheEvent;
import com.chartboost.sdk.Events.ChartboostClickError;
import com.chartboost.sdk.Events.ChartboostClickEvent;
import com.chartboost.sdk.Events.ChartboostShowError;
import com.chartboost.sdk.Events.ChartboostShowEvent;
import com.chartboost.sdk.Libraries.CBLogging;
import com.chartboost.sdk.Model.CBError;
import com.chartboost.sdk.Privacy.model.CCPA;
import com.chartboost.sdk.Privacy.model.DataUseConsent;
import com.chartboost.sdk.Privacy.model.GDPR;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChartboostMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final ChartboostMediationAdapterRouter ROUTER;
    private static final AtomicBoolean                    INITIALIZED = new AtomicBoolean();

    private static InitializationStatus sStatus;

    private String mLocation;

    static
    {
        if ( AppLovinSdk.VERSION_CODE >= 90802 )
        {
            ROUTER = (ChartboostMediationAdapterRouter) MediationAdapterRouter.getSharedInstance( ChartboostMediationAdapterRouter.class );
        }
        else
        {
            ROUTER = new ChartboostMediationAdapterRouter();
        }
    }

    // Explicit default constructor declaration
    public ChartboostMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            sStatus = InitializationStatus.INITIALIZING;

            final Bundle serverParameters = parameters.getServerParameters();
            final String appId = serverParameters.getString( "app_id" );
            log( "Initializing Chartboost SDK with app id: " + appId + "..." );

            ROUTER.setOnCompletionListener( onCompletionListener );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            // We must update consent _before_ calling {@link Chartboost#startWithAppId()} `startWithAppId:appSignature:delegate`
            // (https://answers.chartboost.com/en-us/child_article/android#gdpr)
            updateConsentStatus( parameters, context );

            // NOTE: We should have autoinit server parameters AND ad response server parameters return credentials due to race condition
            String appSignature = serverParameters.getString( "app_signature" );

            // NOTE: Unlike iOS, Chartboost will call `didInitialize()` in the event of a failure.
            Chartboost.startWithAppId( context, appId, appSignature );
            Chartboost.setDelegate( ROUTER.getDelegate() );

            Chartboost.setMediation( Chartboost.CBMediation.CBMediationOther, AppLovinSdk.VERSION, getAdapterVersion() );

            // Whether or not to autocache ads - it is enabled by default and AdMob sets it to true while MoPub sets it to false
            boolean autoCacheAds = serverParameters.getBoolean( "auto_cache_ads" ); // We will default to false to match MoPub
            Chartboost.setAutoCacheAds( autoCacheAds );

            // Real test mode should be enabled from UI (https://answers.chartboost.com/en-us/articles/200780549)
            if ( parameters.isTesting() )
            {
                Chartboost.setLoggingLevel( CBLogging.Level.ALL );
            }

            if ( serverParameters.containsKey( "prefetch_video_content" ) )
            {
                boolean prefetchVideoContent = serverParameters.getBoolean( "prefetch_video_content" );
                Chartboost.setShouldPrefetchVideoContent( prefetchVideoContent );
            }
        }
        else
        {
            log( "Chartboost SDK already initialized..." );

            // Callback needs to be set each time the activity is destroyed
            if ( Chartboost.getDelegate() == null )
            {
                Chartboost.setDelegate( ROUTER.getDelegate() );
            }

            onCompletionListener.onCompletion( sStatus, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return Chartboost.getSDKVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        ROUTER.removeAdapter( this, mLocation );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        mLocation = retrieveLocation( parameters );
        log( "Loading interstitial ad for location \"" + mLocation + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );
        ROUTER.addInterstitialAdapter( this, listener, mLocation );

        if ( Chartboost.hasInterstitial( mLocation ) )
        {
            log( "Ad is available already" );
            ROUTER.onAdLoaded( mLocation );
        }
        else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            Chartboost.cacheInterstitial( mLocation );
        }
        else // Chartboost does not support showing interstitial ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing interstitial ads for devices with Android versions lower than 21" );
            ROUTER.onAdLoadFailed( parameters.getThirdPartyAdPlacementId(), MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad for location \"" + mLocation + "\"..." );

        ROUTER.addShowingAdapter( this );

        updateShowConfigurations( parameters );

        if ( Chartboost.hasInterstitial( mLocation ) )
        {
            Chartboost.showInterstitial( mLocation );
        }
        else
        {
            log( "Interstitial ad not ready" );
            ROUTER.onAdDisplayFailed( mLocation, MaxAdapterError.AD_NOT_READY );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        mLocation = retrieveLocation( parameters );
        log( "Loading rewarded ad for location \"" + mLocation + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );
        ROUTER.addRewardedAdapter( this, listener, mLocation );

        if ( Chartboost.hasRewardedVideo( mLocation ) )
        {
            log( "Ad is available already" );
            ROUTER.onAdLoaded( mLocation );
        }
        else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            Chartboost.cacheRewardedVideo( mLocation );
        }
        else // Chartboost does not support showing rewarded ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing rewarded ads for devices with Android versions lower than 21" );
            ROUTER.onAdLoadFailed( parameters.getThirdPartyAdPlacementId(), MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad for location \"" + mLocation + "\"..." );

        ROUTER.addShowingAdapter( this );

        updateShowConfigurations( parameters );

        if ( Chartboost.hasRewardedVideo( mLocation ) )
        {
            // Configure userReward from server.
            configureReward( parameters );
            Chartboost.showRewardedVideo( mLocation );
        }
        else
        {
            log( "Rewarded ad not ready" );
            ROUTER.onAdDisplayFailed( mLocation, MaxAdapterError.AD_NOT_READY );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        mLocation = retrieveLocation( parameters );
        log( "Loading " + adFormat.getLabel() + " ad for location \"" + mLocation + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );

        ChartboostBanner adView = new ChartboostBanner( activity.getApplicationContext(), mLocation, toAdSize( adFormat ), null );
        adView.setAutomaticallyRefreshesContent( false );
        ROUTER.setAdView( adView );
        ROUTER.addAdViewAdapter( this, listener, mLocation, adView );

        if ( adView.isCached() )
        {
            log( "Ad is available already" );
            ROUTER.onAdLoaded( mLocation );
            ROUTER.showAdViewDelayed();
        }
        else if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP )
        {
            // Attach the listener after we check if adView is cached, since calling `isCached` before the first cache triggers `onAdCached` with an INTERNAL error.
            adView.setListener( ROUTER.getBannerListener() );
            adView.cache();
        }
        else  // Chartboost does not support showing ad view ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing " + adFormat.getLabel() + " ads for devices with Android versions lower than 21" );
            ROUTER.onAdLoadFailed( parameters.getThirdPartyAdPlacementId(), MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    //region GDPR

    void updateConsentStatus(MaxAdapterParameters parameters, Context applicationContext)
    {
        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                DataUseConsent gdprConsent = new GDPR( hasUserConsent ? GDPR.GDPR_CONSENT.BEHAVIORAL : GDPR.GDPR_CONSENT.NON_BEHAVIORAL );
                Chartboost.addDataUseConsent( applicationContext, gdprConsent );
            }
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                DataUseConsent ccpaConsent = new CCPA( isDoNotSell ? CCPA.CCPA_CONSENT.OPT_OUT_SALE : CCPA.CCPA_CONSENT.OPT_IN_SALE );
                Chartboost.addDataUseConsent( applicationContext, ccpaConsent );
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

    //endregion

    //region Helper Methods

    private String retrieveLocation(MaxAdapterResponseParameters parameters)
    {
        if ( !TextUtils.isEmpty( parameters.getThirdPartyAdPlacementId() ) )
        {
            return parameters.getThirdPartyAdPlacementId();
        }
        else
        {
            return CBLocation.LOCATION_DEFAULT;
        }
    }

    private void updateShowConfigurations(MaxAdapterResponseParameters parameters)
    {
        final Bundle serverParameters = parameters.getServerParameters();

        if ( serverParameters.containsKey( "hide_system_ui" ) )
        {
            boolean hideSystemUi = serverParameters.getBoolean( "hide_system_ui" );
            Chartboost.setShouldHideSystemUI( hideSystemUi );
        }
    }

    private BannerSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return BannerSize.STANDARD;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return BannerSize.LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return BannerSize.MEDIUM;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    //endregion

    private static class ChartboostMediationAdapterRouter
            extends MediationAdapterRouter
    {
        private final AtomicBoolean isShowingAd = new AtomicBoolean();

        private OnCompletionListener onCompletionListener;

        private boolean hasGrantedReward;

        private ChartboostBanner adView;

        private final ChartboostDelegate chartboostDelegate = new ChartboostDelegate()
        {
            @Override
            public void didInitialize()
            {
                log( "Chartboost SDK initialized" );

                sStatus = InitializationStatus.INITIALIZED_UNKNOWN;

                if ( onCompletionListener != null )
                {
                    onCompletionListener.onCompletion( sStatus, null );
                    onCompletionListener = null;
                }
            }

            //region Interstitial Callbacks

            @Override
            public void didCacheInterstitial(String location)
            {
                log( "Interstitial loaded" );
                onAdLoaded( location );
            }

            @Override
            public void didFailToLoadInterstitial(String location, CBError.CBImpressionError error)
            {
                MaxAdapterError adapterError = toMaxError( error );

                if ( isShowingAd.compareAndSet( true, false ) )
                {
                    log( "Interstitial failed to show with error: " + error );
                    onAdDisplayFailed( location, adapterError );
                }
                else
                {
                    log( "Interstitial failed to load with error: " + error );
                    onAdLoadFailed( location, adapterError );
                }
            }

            @Override
            public void didDisplayInterstitial(String location)
            {
                log( "Interstitial shown" );
                onAdDisplayed( location );
            }

            @Override
            public void didClickInterstitial(String location)
            {
                log( "Interstitial clicked" );
                onAdClicked( location );
            }

            @Override
            public void didDismissInterstitial(String location)
            {
                isShowingAd.set( false );

                log( "Interstitial hidden" );
                onAdHidden( location );
            }

            //endregion

            //region Rewarded Callbacks

            @Override
            public void didCacheRewardedVideo(String location)
            {
                log( "Rewarded ad loaded" );
                onAdLoaded( location );
            }

            @Override
            public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error)
            {
                MaxAdapterError adapterError = toMaxError( error );

                if ( isShowingAd.compareAndSet( true, false ) )
                {
                    log( "Rewarded ad failed to show with error: " + error );
                    onAdDisplayFailed( location, adapterError );
                }
                else
                {
                    log( "Rewarded ad failed to load with error: " + error );
                    onAdLoadFailed( location, adapterError );
                }
            }

            @Override
            public void didDisplayRewardedVideo(String location)
            {
                log( "Rewarded ad shown" );

                onAdDisplayed( location );
                onRewardedAdVideoStarted( location );
            }

            @Override
            public void didClickRewardedVideo(String location)
            {
                log( "Rewarded ad clicked" );
                onAdClicked( location );
            }

            @Override
            public void didDismissRewardedVideo(String location)
            {
                isShowingAd.set( false );

                if ( hasGrantedReward || shouldAlwaysRewardUser( location ) )
                {
                    final MaxReward reward = getReward( location );
                    log( "Rewarded ad user with reward: " + reward );
                    onUserRewarded( location, reward );

                    // Clear hasGrantedReward
                    hasGrantedReward = false;
                }

                log( "Rewarded ad hidden" );
                onAdHidden( location );
            }

            // NOTE: This is ran before didDismissRewardedVideo:
            @Override
            public void didCompleteRewardedVideo(String location, int rewardAmount)
            {
                log( "Rewarded ad video completed" );
                onRewardedAdVideoCompleted( location );

                hasGrantedReward = true;
            }

            //endregion

            //region Shared Callbacks

            @Override
            public void didFailToRecordClick(String uri, CBError.CBClickError error)
            {
                log( "Failed to record click at \"" + uri + "\" because of error: " + error );
            }

            //endregion
        };

        private final ChartboostBannerListener chartboostBannerListener = new ChartboostBannerListener()
        {
            @Override
            public void onAdCached(final ChartboostCacheEvent event, final ChartboostCacheError error)
            {
                String location = adView.getLocation();
                if ( error != null )
                {
                    MaxAdapterError adapterError = toMaxError( error );

                    log( "AdView failed \"" + location + "\" to load with error: " + error.code );
                    onAdLoadFailed( location, adapterError );
                }
                else
                {
                    log( "AdView loaded: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( event.adID ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", event.adID );

                        onAdLoaded( location, extraInfo );
                    }
                    else
                    {
                        onAdLoaded( location );
                    }

                    showAdViewDelayed();
                }
            }

            @Override
            public void onAdShown(final ChartboostShowEvent event, final ChartboostShowError error)
            {
                String location = adView.getLocation();
                if ( error != null )
                {
                    MaxAdapterError adapterError = toMaxError( error );

                    log( "AdView failed \"" + location + "\" to show with error: " + error.code );
                    onAdDisplayFailed( location, adapterError );
                }
                else
                {
                    log( "AdView shown: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( event.adID ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", event.adID );

                        onAdDisplayed( location, extraInfo );
                    }
                    else
                    {
                        onAdDisplayed( location );
                    }
                }
            }

            @Override
            public void onAdClicked(final ChartboostClickEvent event, final ChartboostClickError error)
            {
                String location = adView.getLocation();
                if ( error != null )
                {
                    log( "Failed to record click on \"" + location + "\" because of error: " + error.code );
                }
                else
                {
                    log( "AdView clicked: " + location );

                    ChartboostMediationAdapterRouter.this.onAdClicked( location );
                }
            }
        };

        ChartboostDelegate getDelegate()
        {
            return chartboostDelegate;
        }

        ChartboostBannerListener getBannerListener()
        {
            return chartboostBannerListener;
        }

        void setOnCompletionListener(final OnCompletionListener onCompletionListener)
        {
            this.onCompletionListener = onCompletionListener;
        }

        void setAdView(final ChartboostBanner adView)
        {
            this.adView = adView;
        }

        void showAdViewDelayed()
        {
            // Chartboost requires manual show after caching ad views. Delay to allow enough time for attaching to parent.
            AppLovinSdkUtils.runOnUiThreadDelayed( new Runnable()
            {
                @Override public void run()
                {
                    adView.show();
                }
            }, 500 );
        }

        //region Initialization

        //TODO: marked for deletion, pending SDK change.
        void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final MaxAdapter.OnCompletionListener onCompletionListener) { }

        //endregion

        //region Overrides for Ad Show State Management

        @Override
        public void addShowingAdapter(final MaxAdapter adapter)
        {
            super.addShowingAdapter( adapter );

            // Chartboost uses the same callback for [AD LOAD FAILED] and [AD DISPLAY FAILED] callbacks
            isShowingAd.set( true );
        }

        //endregion

        //region Helper Methods

        private static MaxAdapterError toMaxError(CBError.CBImpressionError chartboostError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( chartboostError )
            {
                case INTERNAL:
                case TOO_MANY_CONNECTIONS: // Too many requests are pending for that location
                case NO_HOST_ACTIVITY:
                case ACTIVITY_MISSING_IN_MANIFEST:
                case END_POINT_DISABLED:
                case HARDWARE_ACCELERATION_DISABLED:
                case PENDING_IMPRESSION_ERROR:
                case ERROR_CREATING_VIEW:
                case ERROR_DISPLAYING_VIEW:
                case VIDEO_UNAVAILABLE:
                case VIDEO_ID_MISSING:
                case ERROR_PLAYING_VIDEO:
                case INVALID_RESPONSE:
                case EMPTY_LOCAL_VIDEO_LIST:
                case VIDEO_UNAVAILABLE_FOR_CURRENT_ORIENTATION:
                case ASSET_MISSING:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case WRONG_ORIENTATION: // Interstitial loaded with wrong orientation (from UI)
                case FIRST_SESSION_INTERSTITIALS_DISABLED: // Interstitial disabled, first session
                case INCOMPATIBLE_API_VERSION:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
                case INTERNET_UNAVAILABLE: // Network is currently unavailable
                case NETWORK_FAILURE: // Network request failed
                case INTERNET_UNAVAILABLE_AT_SHOW: // Network is unavailable while attempting to show
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case NO_AD_FOUND: //  No ad received
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case SESSION_NOT_STARTED: // Session not started
                    adapterError = MaxAdapterError.NOT_INITIALIZED;
                    break;
                case IMPRESSION_ALREADY_VISIBLE: // There is an impression already visible
                    adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                    break;
                case ASSETS_DOWNLOAD_FAILURE: // Error downloading asset
                    adapterError = MaxAdapterError.BAD_REQUEST;
                    break;
                case ERROR_LOADING_WEB_VIEW:
                case WEB_VIEW_PAGE_LOAD_TIMEOUT:
                case WEB_VIEW_CLIENT_RECEIVED_ERROR:
                    adapterError = MaxAdapterError.WEBVIEW_ERROR;
                    break;
                case ASSET_PREFETCH_IN_PROGRESS: // Video Prefetching is not finished
                    adapterError = MaxAdapterError.AD_NOT_READY;
                    break;
                case USER_CANCELLATION: // User manually cancelled the impression
                case INVALID_LOCATION: // No location detected
                    adapterError = MaxAdapterError.UNSPECIFIED;
                    break;
            }

            return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), chartboostError.ordinal(), chartboostError.name() );
        }

        private static MaxAdapterError toMaxError(ChartboostCacheError chartboostError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( chartboostError.code )
            {
                case INTERNAL:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case INTERNET_UNAVAILABLE:
                case NETWORK_FAILURE:
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case NO_AD_FOUND:
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case SESSION_NOT_STARTED:
                    adapterError = MaxAdapterError.NOT_INITIALIZED;
                    break;
                case ASSET_DOWNLOAD_FAILURE:
                    adapterError = MaxAdapterError.BAD_REQUEST;
                    break;
                case BANNER_DISABLED:
                case BANNER_VIEW_IS_DETACHED:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
            }

            return new MaxAdapterError( adapterError, chartboostError.code.getErrorCode(), chartboostError.toString() );
        }

        private static MaxAdapterError toMaxError(ChartboostShowError chartboostError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( chartboostError.code )
            {
                case INTERNAL:
                case PRESENTATION_FAILURE:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case AD_ALREADY_VISIBLE:
                    adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                    break;
                case SESSION_NOT_STARTED:
                    adapterError = MaxAdapterError.NOT_INITIALIZED;
                    break;
                case INTERNET_UNAVAILABLE:
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case NO_CACHED_AD:
                    adapterError = MaxAdapterError.AD_NOT_READY;
                    break;
                case BANNER_DISABLED:
                case BANNER_VIEW_IS_DETACHED:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
            }

            return new MaxAdapterError( adapterError, chartboostError.code.getErrorCode(), chartboostError.toString() );
        }
    }
}
