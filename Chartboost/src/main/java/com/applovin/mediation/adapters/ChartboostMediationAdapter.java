package com.applovin.mediation.adapters;

import static com.applovin.mediation.adapter.MaxAdapterError.ERROR_CODE_AD_DISPLAY_FAILED;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.LoggingLevel;
import com.chartboost.sdk.Mediation;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.ads.Interstitial;
import com.chartboost.sdk.ads.Rewarded;
import com.chartboost.sdk.callbacks.BannerCallback;
import com.chartboost.sdk.callbacks.InterstitialCallback;
import com.chartboost.sdk.callbacks.RewardedCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.RewardEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;
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

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            // We must update consent _before_ calling {@link Chartboost#startWithAppId()} `startWithAppId:appSignature:delegate`
            // (https://answers.chartboost.com/en-us/child_article/android#gdpr)
            updateConsentStatus( parameters, context );

            // NOTE: We should have autoinit server parameters AND ad response server parameters return credentials due to race condition
            String appSignature = serverParameters.getString( "app_signature" );

            Chartboost.startWithAppId(context, appId, appSignature, startError -> {
                log( "Chartboost SDK initialized" );

                sStatus = InitializationStatus.INITIALIZED_UNKNOWN;

                if ( onCompletionListener != null )
                {
                    if ( startError == null ) {
                        sStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    } else {
                        sStatus = InitializationStatus.INITIALIZED_FAILURE;
                    }

                    onCompletionListener.onCompletion( sStatus, null );
                }
            });

            // Real test mode should be enabled from UI (https://answers.chartboost.com/en-us/articles/200780549)
            if ( parameters.isTesting() )
            {
                Chartboost.setLoggingLevel( LoggingLevel.ALL );
            }
        }
        else
        {
            log( "Chartboost SDK already initialized..." );
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
        Mediation mediation = new Mediation("AppLovinMax", AppLovinSdk.VERSION, getAdapterVersion() );
        Interstitial interstitial = new Interstitial(mLocation, ROUTER.getInterstitialListener(), mediation);
        ROUTER.setInterstitial( interstitial );

        if ( interstitial.isCached() )
        {
            log( "Ad is available already" );
            ROUTER.onAdLoaded( mLocation );
        }
        else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            interstitial.cache();
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
        Interstitial interstitial = ROUTER.getInterstitial();
        if ( interstitial != null && interstitial.isCached() )
        {
            interstitial.show();
        }
        else
        {
            log( "Interstitial ad not ready" );
            ROUTER.onAdDisplayFailed( mLocation, new MaxAdapterError( ERROR_CODE_AD_DISPLAY_FAILED, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        mLocation = retrieveLocation( parameters );
        log( "Loading rewarded ad for location \"" + mLocation + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );
        ROUTER.addRewardedAdapter( this, listener, mLocation );
        Mediation mediation = new Mediation("AppLovinMax", AppLovinSdk.VERSION, getAdapterVersion() );
        Rewarded rewarded = new Rewarded(mLocation, ROUTER.getRewardedListener(), mediation);
        ROUTER.setRewarded( rewarded );

        if ( rewarded.isCached() )
        {
            log( "Ad is available already" );
            ROUTER.onAdLoaded( mLocation );
        }
        else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            rewarded.cache();
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
        Rewarded rewarded = ROUTER.getRewarded();

        if ( rewarded != null && rewarded.isCached() )
        {
            // Configure userReward from server.
            configureReward( parameters );
            rewarded.show();
        }
        else
        {
            log( "Rewarded ad not ready" );
            ROUTER.onAdDisplayFailed( mLocation, new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        mLocation = retrieveLocation( parameters );
        log( "Loading " + adFormat.getLabel() + " ad for location \"" + mLocation + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );

        Mediation mediation = new Mediation("AppLovinMax", AppLovinSdk.VERSION, getAdapterVersion() );
        Banner adView = new Banner(activity.getApplicationContext(), mLocation, toAdSize(adFormat), ROUTER.getBannerListener(), mediation);
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
            adView.cache();
        }
        else  // Chartboost does not support showing ad view ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing " + adFormat.getLabel() + " ads for devices with Android versions lower than 21" );
            ROUTER.onAdLoadFailed( parameters.getThirdPartyAdPlacementId(), MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    //region GDPR

    private AppLovinSdkConfiguration.ConsentDialogState getConsentDialogStateSafe() {
        AppLovinSdk sdk = getWrappingSdk();
        if ( sdk == null) {
            return AppLovinSdkConfiguration.ConsentDialogState.UNKNOWN;
        }

        AppLovinSdkConfiguration configuration = sdk.getConfiguration();
        if ( configuration == null) {
            return AppLovinSdkConfiguration.ConsentDialogState.UNKNOWN;
        }

        AppLovinSdkConfiguration.ConsentDialogState state = configuration.getConsentDialogState();
        if (state == null) {
            return AppLovinSdkConfiguration.ConsentDialogState.UNKNOWN;
        }
        return state;
    }

    void updateConsentStatus(MaxAdapterParameters parameters, Context applicationContext)
    {

        if ( getConsentDialogStateSafe() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
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

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            DataUseConsent coppaConsent = new COPPA( isAgeRestrictedUser );
            Chartboost.addDataUseConsent( applicationContext, coppaConsent );
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
            return "Default";
        }
    }

    private Banner.BannerSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return Banner.BannerSize.STANDARD;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return Banner.BannerSize.LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return Banner.BannerSize.MEDIUM;
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

        private boolean hasGrantedReward;

        private Interstitial interstitial;

        private Rewarded rewarded;

        private Banner adView;

        private final InterstitialCallback chartboostInterstitialListener = new InterstitialCallback()
        {
            @Override
            public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
                isShowingAd.set( false );

                log( "Interstitial hidden" );
                onAdHidden( dismissEvent.getAd().getLocation() );
            }

            @Override
            public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
                log( "Interstitial onImpressionRecorded: " + impressionEvent.getAd().getLocation() );
            }

            @Override
            public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
                String location = adView.getLocation();
                if ( showError != null )
                {
                    Exception exception = showError.getException();
                    String exceptionMsg = null;
                    if(exception != null) {
                        exceptionMsg = exception.toString();
                    }
                    MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", showError.getCode().getErrorCode(), exceptionMsg );

                    log( "Interstitial failed \"" + location + "\" to show with error: " + showError.getCode() );
                    ChartboostMediationAdapterRouter.this.onAdDisplayFailed( location, adapterError );
                }
                else
                {
                    log( "Interstitial shown: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( showEvent.getAdID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", showEvent.getAdID() );

                        ChartboostMediationAdapterRouter.this.onAdDisplayed( location, extraInfo );
                    }
                    else
                    {
                        ChartboostMediationAdapterRouter.this.onAdDisplayed( location );
                    }
                }
            }

            @Override
            public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
                log( "Interstitial onAdRequestedToShow: " + showEvent.getAd().getLocation() );
            }

            @Override
            public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
                String location = adView.getLocation();
                if ( cacheError != null )
                {
                    MaxAdapterError adapterError = toMaxError( cacheError );

                    log( "Interstitial failed \"" + location + "\" to load with error: " + cacheError.getCode() );
                    onAdLoadFailed( location, adapterError );
                }
                else
                {
                    log( "Interstitial loaded: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( cacheEvent.getAdID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", cacheEvent.getAdID() );

                        ChartboostMediationAdapterRouter.this.onAdLoaded( location, extraInfo );
                    }
                    else
                    {
                        ChartboostMediationAdapterRouter.this.onAdLoaded( location );
                    }

                    showAdViewDelayed();
                }
            }

            @Override
            public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
                String location = adView.getLocation();
                if ( clickError != null )
                {
                    log( "Failed to record click on \"" + location + "\" because of error: " + clickError.getCode() );
                }
                else
                {
                    log( "Interstitial clicked: " + location );

                    ChartboostMediationAdapterRouter.this.onAdClicked( location );
                }
            }
        };

        private final RewardedCallback chartboostRewardedListener = new RewardedCallback()
        {
            @Override
            public void onRewardEarned(@NonNull RewardEvent rewardEvent) {
                log( "Rewarded ad video completed" );
                onRewardedAdVideoCompleted( rewardEvent.getAd().getLocation() );

                hasGrantedReward = true;
            }

            @Override
            public void onAdDismiss(@NonNull DismissEvent dismissEvent) {
                isShowingAd.set( false );
                String location = dismissEvent.getAd().getLocation();

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

            @Override
            public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
                log( "Rewarded onImpressionRecorded: " + impressionEvent.getAd().getLocation() );
            }

            @Override
            public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
                String location = adView.getLocation();
                if ( showError != null )
                {
                    Exception exception = showError.getException();
                    String exceptionMsg = null;
                    if(exception != null) {
                        exceptionMsg = exception.toString();
                    }
                    MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", showError.getCode().getErrorCode(), exceptionMsg );

                    log( "Rewarded failed \"" + location + "\" to show with error: " + showError.getCode() );
                    ChartboostMediationAdapterRouter.this.onAdDisplayFailed( location, adapterError );
                }
                else
                {
                    log( "Rewarded shown: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( showEvent.getAdID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", showEvent.getAdID() );

                        ChartboostMediationAdapterRouter.this.onAdDisplayed( location, extraInfo );
                    }
                    else
                    {
                        ChartboostMediationAdapterRouter.this.onAdDisplayed( location );
                    }
                    onRewardedAdVideoStarted( location );
                }
            }

            @Override
            public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
                log( "Rewarded onAdRequestedToShow: " + showEvent.getAd().getLocation() );
            }

            @Override
            public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
                String location = adView.getLocation();
                if ( cacheError != null )
                {
                    MaxAdapterError adapterError = toMaxError( cacheError );

                    log( "Rewarded failed \"" + location + "\" to load with error: " + cacheError.getCode() );
                    onAdLoadFailed( location, adapterError );
                }
                else
                {
                    log( "Rewarded loaded: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( cacheEvent.getAdID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", cacheEvent.getAdID() );

                        ChartboostMediationAdapterRouter.this.onAdLoaded( location, extraInfo );
                    }
                    else
                    {
                        ChartboostMediationAdapterRouter.this.onAdLoaded( location );
                    }

                    showAdViewDelayed();
                }
            }

            @Override
            public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
                String location = adView.getLocation();
                if ( clickError != null )
                {
                    log( "Failed to record click on \"" + location + "\" because of error: " + clickError.getCode() );
                }
                else
                {
                    log( "Rewarded clicked: " + location );

                    ChartboostMediationAdapterRouter.this.onAdClicked( location );
                }
            }
        };

        private final BannerCallback chartboostBannerListener = new BannerCallback()
        {
            @Override
            public void onImpressionRecorded(@NonNull ImpressionEvent impressionEvent) {
                log( "AdView onImpressionRecorded: " + impressionEvent.getAd().getLocation() );
            }

            @Override
            public void onAdShown(@NonNull ShowEvent showEvent, @Nullable ShowError showError) {
                String location = adView.getLocation();
                if ( showError != null )
                {
                    Exception exception = showError.getException();
                    String exceptionMsg = null;
                    if(exception != null) {
                        exceptionMsg = exception.toString();
                    }
                    MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", showError.getCode().getErrorCode(), exceptionMsg );

                    log( "AdView failed \"" + location + "\" to show with error: " + showError.getCode() );
                    ChartboostMediationAdapterRouter.this.onAdDisplayFailed( location, adapterError );
                }
                else
                {
                    log( "AdView shown: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( showEvent.getAdID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", showEvent.getAdID() );

                        ChartboostMediationAdapterRouter.this.onAdDisplayed( location, extraInfo );
                    }
                    else
                    {
                        ChartboostMediationAdapterRouter.this.onAdDisplayed( location );
                    }
                }
            }

            @Override
            public void onAdRequestedToShow(@NonNull ShowEvent showEvent) {
                log( "AdView onAdRequestedToShow: " + showEvent.getAd().getLocation() );
            }

            @Override
            public void onAdLoaded(@NonNull CacheEvent cacheEvent, @Nullable CacheError cacheError) {
                String location = adView.getLocation();
                if ( cacheError != null )
                {
                    MaxAdapterError adapterError = toMaxError( cacheError );

                    log( "AdView failed \"" + location + "\" to load with error: " + cacheError.getCode() );
                    onAdLoadFailed( location, adapterError );
                }
                else
                {
                    log( "AdView loaded: " + location );

                    // Passing extra info such as creative id supported in 9.15.0+
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( cacheEvent.getAdID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", cacheEvent.getAdID() );

                        ChartboostMediationAdapterRouter.this.onAdLoaded( location, extraInfo );
                    }
                    else
                    {
                        ChartboostMediationAdapterRouter.this.onAdLoaded( location );
                    }

                    showAdViewDelayed();
                }
            }

            @Override
            public void onAdClicked(@NonNull ClickEvent clickEvent, @Nullable ClickError clickError) {
                String location = adView.getLocation();
                if ( clickError != null )
                {
                    log( "Failed to record click on \"" + location + "\" because of error: " + clickError.getCode() );
                }
                else
                {
                    log( "AdView clicked: " + location );

                    ChartboostMediationAdapterRouter.this.onAdClicked( location );
                }
            }
        };

        InterstitialCallback getInterstitialListener()
        {
            return chartboostInterstitialListener;
        }

        RewardedCallback getRewardedListener()
        {
            return chartboostRewardedListener;
        }

        BannerCallback getBannerListener()
        {
            return chartboostBannerListener;
        }

        Interstitial getInterstitial()
        {
            return interstitial ;
        }

        void setInterstitial(final Interstitial interstitial)
        {
            this.interstitial = interstitial;
        }

        Rewarded getRewarded()
        {
            return rewarded ;
        }

        void setRewarded(final Rewarded rewarded)
        {
            this.rewarded = rewarded;
        }

        void setAdView(final Banner adView)
        {
            this.adView = adView;
        }

        void showAdViewDelayed()
        {
            // Chartboost requires manual show after caching ad views. Delay to allow enough time for attaching to parent.
            AppLovinSdkUtils.runOnUiThreadDelayed( new Runnable()
            {
                @Override
                public void run()
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

        private static MaxAdapterError toMaxError(CacheError chartboostError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( chartboostError.getCode() )
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

            return new MaxAdapterError( adapterError, chartboostError.getCode().getErrorCode(), chartboostError.toString() );
        }

        private static MaxAdapterError toMaxError(ShowError chartboostError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( chartboostError.getCode() )
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

            return new MaxAdapterError( adapterError, chartboostError.getCode().getErrorCode(), chartboostError.toString() );
        }
    }
}
