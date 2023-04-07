package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.ironsource.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.ISDemandOnlyBannerLayout;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyBannerListener;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.ironsource.mediationsdk.utils.IronSourceUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IronSourceMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final IronSourceRouter ROUTER      = new IronSourceRouter();
    private static final AtomicBoolean    INITIALIZED = new AtomicBoolean();

    private String                   mRouterPlacementIdentifier;
    private ISDemandOnlyBannerLayout adView;

    private static final Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks()
    {
        @Override
        public void onActivityResumed(final Activity activity)
        {
            // Do not forward ironSource Activity lifecycle events
            if ( !activity.getClass().getName().startsWith( "com.ironsource.sdk" ) )
            {
                IronSource.onResume( activity );
            }
        }

        @Override
        public void onActivityPaused(final Activity activity)
        {
            // Do not forward ironSource Activity lifecycle events
            if ( !activity.getClass().getName().startsWith( "com.ironsource.sdk" ) )
            {
                IronSource.onPause( activity );
            }
        }

        @Override
        public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) { }

        @Override
        public void onActivityStarted(final Activity activity) { }

        @Override
        public void onActivityStopped(final Activity activity) { }

        @Override
        public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) { }

        @Override
        public void onActivityDestroyed(final Activity activity) { }
    };

    public IronSourceMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String appKey = parameters.getServerParameters().getString( "app_key" );
            log( "Initializing IronSource SDK with app key: " + appKey + "..." );

            if ( parameters.getServerParameters().getBoolean( "set_mediation_identifier" ) )
            {
                IronSource.setMediationType( mediationTag() );
            }

            setPrivacySettings( parameters );

            if ( AppLovinSdk.VERSION_CODE >= 91100 )
            {
                Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
                if ( isDoNotSell != null )
                {
                    // NOTE: `setMetaData` must be called _before_ initializing their SDK
                    IronSource.setMetaData( "do_not_sell", Boolean.toString( isDoNotSell ) );
                }
            }

            Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
            if ( isAgeRestrictedUser != null )
            {
                IronSource.setMetaData( "is_child_directed", Boolean.toString( isAgeRestrictedUser ) );
            }

            IronSource.setAdaptersDebug( parameters.isTesting() );
            IronSource.setISDemandOnlyInterstitialListener( ROUTER );
            IronSource.setISDemandOnlyRewardedVideoListener( ROUTER );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Application application = ( activity != null ) ? activity.getApplication() : (Application) getApplicationContext();

            IronSource.initISDemandOnly( application, appKey, getAdFormatsToInitialize( parameters ) );

            application.registerActivityLifecycleCallbacks( activityLifecycleCallbacks );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return IronSourceUtils.getSDKVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        ROUTER.removeAdapter( this, mRouterPlacementIdentifier );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        setPrivacySettings( parameters );

        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Loading ironSource interstitial for instance ID: " + instanceId );

        // Create a format specific router identifier to ensure that the router can distinguish between them.
        mRouterPlacementIdentifier = IronSourceRouter.getInterstitialRouterIdentifier( instanceId );
        ROUTER.addInterstitialAdapter( this, listener, mRouterPlacementIdentifier );

        if ( IronSource.isISDemandOnlyInterstitialReady( instanceId ) )
        {
            log( "Ad is available already for instance ID: " + instanceId );
            ROUTER.onAdLoaded( mRouterPlacementIdentifier );
        }
        else
        {
            IronSource.loadISDemandOnlyInterstitial( activity, instanceId );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Showing ironSource interstitial for instance ID: " + instanceId );

        ROUTER.addShowingAdapter( this );

        if ( IronSource.isISDemandOnlyInterstitialReady( instanceId ) )
        {
            IronSource.showISDemandOnlyInterstitial( instanceId );
        }
        else
        {
            log( "Unable to show ironSource interstitial - no ad loaded for instance ID: " + instanceId );
            ROUTER.onAdDisplayFailed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ), new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        setPrivacySettings( parameters );

        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Loading ironSource rewarded for instance ID: " + instanceId );

        // Create a format specific router identifier to ensure that the router can distinguish between them.
        mRouterPlacementIdentifier = IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId );
        ROUTER.addRewardedAdapter( this, listener, mRouterPlacementIdentifier );

        if ( IronSource.isISDemandOnlyRewardedVideoAvailable( instanceId ) )
        {
            log( "Ad is available already for instance ID: " + instanceId );
            ROUTER.onAdLoaded( mRouterPlacementIdentifier );
        }
        else
        {
            IronSource.loadISDemandOnlyRewardedVideo( activity, instanceId );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Showing ironSource rewarded for instance ID: " + instanceId );

        ROUTER.addShowingAdapter( this );

        if ( IronSource.isISDemandOnlyRewardedVideoAvailable( instanceId ) )
        {
            // Configure userReward from server.
            configureReward( parameters );
            IronSource.showISDemandOnlyRewardedVideo( instanceId );
        }
        else
        {
            log( "Unable to show ironSource rewarded - no ad loaded..." );
            ROUTER.onAdDisplayFailed( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ), new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        setPrivacySettings( parameters );

        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Loading " + adFormat.getLabel() + " ad for instance ID: " + instanceId );

        adView = IronSource.createBannerForDemandOnly( activity, toISBannerSize( adFormat ) );
        adView.setBannerDemandOnlyListener( new AdViewListener( listener ) );

        IronSource.loadISDemandOnlyBanner( activity, adView, instanceId );
    }

    private void setPrivacySettings(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
        if ( hasUserConsent != null )
        {
            IronSource.setConsent( hasUserConsent );
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

    private IronSource.AD_UNIT[] getAdFormatsToInitialize(final MaxAdapterInitializationParameters parameters)
    {
        List<String> adFormats = parameters.getServerParameters().getStringArrayList( "init_ad_formats" );
        if ( adFormats == null || adFormats.isEmpty() )
        {
            // Default to initialize all ad formats if backend doesn't send down which ones to initialize
            return new IronSource.AD_UNIT[] { IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO, IronSource.AD_UNIT.BANNER };
        }

        List<IronSource.AD_UNIT> adFormatsToInitialize = new ArrayList<>();
        if ( adFormats.contains( "inter" ) )
        {
            adFormatsToInitialize.add( IronSource.AD_UNIT.INTERSTITIAL );
        }

        if ( adFormats.contains( "rewarded" ) )
        {
            adFormatsToInitialize.add( IronSource.AD_UNIT.REWARDED_VIDEO );
        }

        if ( adFormats.contains( "banner" ) )
        {
            adFormatsToInitialize.add( IronSource.AD_UNIT.BANNER );
        }

        return adFormatsToInitialize.toArray( new IronSource.AD_UNIT[adFormatsToInitialize.size()] );
    }

    private ISBannerSize toISBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return ISBannerSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return ISBannerSize.LARGE; // Note: LARGE is 320x90 - leaders weren't supported at the time of implementation.
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return ISBannerSize.RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private static class IronSourceRouter
            extends MediationAdapterRouter
            implements ISDemandOnlyInterstitialListener, ISDemandOnlyRewardedVideoListener
    {
        private boolean hasGrantedReward;

        void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener) { }

        @Override
        public void onInterstitialAdReady(final String instanceId)
        {
            log( "Interstitial loaded for instance ID: " + instanceId );
            onAdLoaded( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onInterstitialAdLoadFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            log( "Interstitial ad failed to load for instance ID: " + instanceId + " with error: " + ironSourceError );
            onAdLoadFailed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ), toMaxError( ironSourceError ) );
        }

        @Override
        public void onInterstitialAdOpened(final String instanceId)
        {
            log( "Interstitial ad displayed for instance ID: " + instanceId );
            onAdDisplayed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onInterstitialAdClosed(final String instanceId)
        {
            log( "Interstitial ad closed for instance ID: " + instanceId );
            onAdHidden( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onInterstitialAdShowFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage() );
            log( "Interstitial ad failed to show for instance ID: " + instanceId + " with error: " + adapterError );
            onAdDisplayFailed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ), adapterError );
        }

        @Override
        public void onInterstitialAdClicked(final String instanceId)
        {
            log( "Interstitial ad clicked for instance ID: " + instanceId );
            onAdClicked( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onRewardedVideoAdOpened(final String instanceId)
        {
            log( "Rewarded ad shown for instance ID: " + instanceId );

            final String routerPlacementId = IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId );
            onAdDisplayed( routerPlacementId );
            onRewardedAdVideoStarted( routerPlacementId );
        }

        @Override
        public void onRewardedVideoAdClosed(final String instanceId)
        {
            final String routerPlacementId = IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId );
            if ( hasGrantedReward || shouldAlwaysRewardUser( routerPlacementId ) )
            {
                MaxReward reward = getReward( routerPlacementId );

                log( "Rewarded  ad rewarded user with reward: " + reward + " for instance ID: " + instanceId );
                onUserRewarded( routerPlacementId, reward );

                // clear hasGrantedReward
                hasGrantedReward = false;
            }

            log( "Rewarded ad hidden for instance ID: " + instanceId );
            onAdHidden( routerPlacementId );
        }

        @Override
        public void onRewardedVideoAdLoadSuccess(final String instanceId)
        {
            log( "Rewarded ad loaded for instance ID: " + instanceId );
            onAdLoaded( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ) );
        }

        @Override
        public void onRewardedVideoAdLoadFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            log( "Rewarded ad failed to load for instance ID: " + instanceId );
            onAdLoadFailed( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ), toMaxError( ironSourceError ) );
        }

        @Override
        public void onRewardedVideoAdRewarded(final String instanceId)
        {
            log( "Rewarded ad granted reward for instance ID: " + instanceId );
            onRewardedAdVideoCompleted( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ) );

            hasGrantedReward = true;
        }

        @Override
        public void onRewardedVideoAdShowFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage() );
            log( "Rewarded ad failed to show for instance ID: " + instanceId + " with error: " + adapterError );
            onAdDisplayFailed( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ), adapterError );
        }

        @Override
        public void onRewardedVideoAdClicked(final String instanceId)
        {
            log( "Rewarded ad clicked for instance ID: " + instanceId );
            onAdClicked( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ) );
        }

        private static MaxAdapterError toMaxError(final IronSourceError ironSourceError)
        {
            final int ironSourceErrorCode = ironSourceError.getErrorCode();
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( ironSourceErrorCode )
            {
                case IronSourceError.ERROR_CODE_DECRYPT_FAILED:
                case IronSourceError.ERROR_NON_EXISTENT_INSTANCE:
                case IronSourceError.ERROR_BN_LOAD_EXCEPTION:
                case IronSourceError.ERROR_BN_INSTANCE_LOAD_EMPTY_BANNER:
                case IronSourceError.ERROR_BN_INSTANCE_LOAD_EMPTY_ADAPTER:
                case IronSourceError.ERROR_BN_RELOAD_SKIP_INVISIBLE:
                case IronSourceError.ERROR_BN_RELOAD_SKIP_BACKGROUND:
                case IronSourceError.AUCTION_ERROR_REQUEST:
                case IronSourceError.AUCTION_ERROR_RESPONSE_NOT_200:
                case IronSourceError.AUCTION_ERROR_PARSE:
                case IronSourceError.AUCTION_ERROR_DECRYPTION:
                case IronSourceError.AUCTION_ERROR_EMPTY_WATERFALL:
                case IronSourceError.AUCTION_ERROR_NO_CANDIDATES:
                case IronSourceError.AUCTION_REQUEST_ERROR_MISSING_PARAMS:
                case IronSourceError.AUCTION_ERROR_DECOMPRESSION:
                case IronSourceError.ERROR_RV_LOAD_SUCCESS_UNEXPECTED:
                case IronSourceError.ERROR_RV_LOAD_FAIL_UNEXPECTED:
                case IronSourceError.ERROR_RV_LOAD_UNEXPECTED_CALLBACK:
                case IronSourceError.ERROR_RV_SHOW_EXCEPTION:
                case IronSourceError.ERROR_IS_SHOW_EXCEPTION:
                case IronSourceError.ERROR_RV_INIT_EXCEPTION:
                case IronSourceError.ERROR_IS_INIT_EXCEPTION:
                case IronSourceError.ERROR_DO_RV_LOAD_MISSING_ACTIVITY:
                case IronSourceError.ERROR_DO_IS_LOAD_MISSING_ACTIVITY:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
                case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                case IronSourceError.ERROR_CODE_KEY_NOT_SET:
                case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
                case IronSourceError.ERROR_BN_LOAD_NO_CONFIG:
                case IronSourceError.ERROR_BN_UNSUPPORTED_SIZE:
                case IronSourceError.ERROR_IS_EMPTY_DEFAULT_PLACEMENT:
                case IronSourceError.ERROR_RV_EMPTY_DEFAULT_PLACEMENT:
                case IronSourceError.ERROR_RV_LOAD_SUCCESS_WRONG_AUCTION_ID:
                case IronSourceError.ERROR_RV_LOAD_FAIL_WRONG_AUCTION_ID:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
                case IronSourceError.ERROR_CODE_INIT_FAILED:
                case IronSourceError.ERROR_BN_LOAD_AFTER_INIT_FAILED:
                case IronSourceError.ERROR_BN_LOAD_AFTER_LONG_INITIATION:
                case IronSourceError.ERROR_BN_INIT_FAILED_AFTER_LOAD:
                case IronSourceError.ERROR_BN_LOAD_WHILE_LONG_INITIATION:
                case IronSourceError.ERROR_BN_INSTANCE_INIT_TIMEOUT:
                case IronSourceError.ERROR_BN_INSTANCE_INIT_ERROR:
                case IronSourceError.INIT_ERROR_NO_ADAPTERS_LOADED:
                    adapterError = MaxAdapterError.NOT_INITIALIZED;
                    break;
                case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                case IronSourceError.ERROR_BN_LOAD_NO_FILL:
                case IronSourceError.ERROR_RV_LOAD_FAILED_NO_CANDIDATES:
                case IronSourceError.ERROR_IS_LOAD_FAILED_NO_CANDIDATES:
                case IronSourceError.ERROR_RV_LOAD_NO_FILL:
                case IronSourceError.ERROR_IS_LOAD_NO_FILL:
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case IronSourceError.ERROR_CODE_GENERIC:
                    adapterError = MaxAdapterError.UNSPECIFIED;
                    break;
                case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case IronSourceError.ERROR_REACHED_CAP_LIMIT_PER_PLACEMENT:
                case IronSourceError.ERROR_CAPPED_PER_SESSION:
                case IronSourceError.ERROR_BN_LOAD_PLACEMENT_CAPPED:
                    adapterError = MaxAdapterError.AD_FREQUENCY_CAPPED;
                    break;
                case IronSourceError.ERROR_BN_INSTANCE_LOAD_TIMEOUT:
                case IronSourceError.ERROR_BN_INSTANCE_RELOAD_TIMEOUT:
                case IronSourceError.ERROR_RV_INIT_FAILED_TIMEOUT:
                case IronSourceError.ERROR_RV_LOAD_FAIL_DUE_TO_INIT:
                case IronSourceError.ERROR_DO_IS_LOAD_TIMED_OUT:
                case IronSourceError.ERROR_DO_RV_LOAD_TIMED_OUT:
                    adapterError = MaxAdapterError.TIMEOUT;
                    break;
                case IronSourceError.AUCTION_ERROR_TIMED_OUT:
                    adapterError = MaxAdapterError.SIGNAL_COLLECTION_TIMEOUT;
                    break;
                case IronSourceError.ERROR_RV_SHOW_CALLED_DURING_SHOW:
                case IronSourceError.ERROR_RV_SHOW_CALLED_WRONG_STATE:
                case IronSourceError.ERROR_RV_LOAD_DURING_LOAD:
                case IronSourceError.ERROR_RV_LOAD_DURING_SHOW:
                case IronSourceError.ERROR_IS_SHOW_CALLED_DURING_SHOW:
                case IronSourceError.ERROR_IS_LOAD_DURING_SHOW:
                case IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS:
                case IronSourceError.ERROR_DO_RV_LOAD_ALREADY_IN_PROGRESS:
                case IronSourceError.ERROR_DO_RV_LOAD_DURING_SHOW:
                    adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                    break;
                case IronSourceError.ERROR_DO_IS_CALL_LOAD_BEFORE_SHOW:
                case IronSourceError.ERROR_DO_RV_CALL_LOAD_BEFORE_SHOW:
                    adapterError = MaxAdapterError.AD_NOT_READY;
                    break;
                case IronSourceError.ERROR_RV_EXPIRED_ADS:
                    adapterError = MaxAdapterError.AD_EXPIRED;
                    break;
            }

            return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), ironSourceErrorCode, ironSourceError.getErrorMessage() );
        }

        private static String getInterstitialRouterIdentifier(final String instanceId)
        {
            return instanceId + "-" + IronSource.AD_UNIT.INTERSTITIAL.toString();
        }

        private static String getRewardedVideoRouterIdentifier(final String instanceId)
        {
            return instanceId + "-" + IronSource.AD_UNIT.REWARDED_VIDEO.toString();
        }
    }

    private class AdViewListener
            implements ISDemandOnlyBannerListener
    {
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onBannerAdLoaded(final String instanceId)
        {
            log( "AdView loaded for instance ID: " + instanceId );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onBannerAdLoadFailed(String instanceId, IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = IronSourceRouter.toMaxError( ironSourceError );
            log( "AdView ad failed to load for instance ID: " + instanceId + " with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onBannerAdShown(String instanceId)
        {
            log( "AdView ad displayed for instance ID: " + instanceId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onBannerAdClicked(String instanceId)
        {
            log( "AdView ad clicked for instance ID: " + instanceId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onBannerAdLeftApplication(String instanceId)
        {
            log( "AdView ad left application for instance ID: " + instanceId );
        }
    }
}
