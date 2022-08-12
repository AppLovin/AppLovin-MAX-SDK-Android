package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

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
import com.applovin.mediation.adapters.chartboost.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.LoggingLevel;
import com.chartboost.sdk.Mediation;
import com.chartboost.sdk.ads.Banner;
import com.chartboost.sdk.ads.Banner.BannerSize;
import com.chartboost.sdk.ads.Interstitial;
import com.chartboost.sdk.ads.Rewarded;
import com.chartboost.sdk.callbacks.BannerCallback;
import com.chartboost.sdk.callbacks.InterstitialCallback;
import com.chartboost.sdk.callbacks.RewardedCallback;
import com.chartboost.sdk.callbacks.StartCallback;
import com.chartboost.sdk.events.CacheError;
import com.chartboost.sdk.events.CacheEvent;
import com.chartboost.sdk.events.ClickError;
import com.chartboost.sdk.events.ClickEvent;
import com.chartboost.sdk.events.DismissEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.RewardEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.COPPA;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChartboostMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean initialized        = new AtomicBoolean();
    private static final Mediation     MEDIATION_PROVIDER = new Mediation( "MAX", AppLovinSdk.VERSION, BuildConfig.VERSION_NAME );

    private static InitializationStatus status;

    private Interstitial interstitialAd;
    private Rewarded     rewardedAd;
    private Banner       adView;

    // Explicit default constructor declaration
    public ChartboostMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

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

            // NOTE: Unlike iOS, Chartboost will call `didInitialize()` in the event of a failure.
            Chartboost.startWithAppId( context, appId, appSignature, new StartCallback()
            {
                @Override
                public void onStartCompleted(@Nullable final StartError startError)
                {
                    if ( startError != null )
                    {
                        log( "Chartboost SDK initialized failed because of error: " + startError );
                        status = InitializationStatus.INITIALIZED_FAILURE;

                        onCompletionListener.onCompletion( status, startError.toString() );

                        return;
                    }

                    log( "Chartboost SDK initialized successfully" );
                    status = InitializationStatus.INITIALIZED_SUCCESS;

                    onCompletionListener.onCompletion( status, null );
                }
            } );

            // Real test mode should be enabled from UI (https://answers.chartboost.com/en-us/articles/200780549)
            if ( parameters.isTesting() )
            {
                Chartboost.setLoggingLevel( LoggingLevel.ALL );
            }
        }
        else
        {
            log( "Chartboost SDK already initialized..." );

            onCompletionListener.onCompletion( status, null );
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
        log( "Destroy called for adapter " + this );

        if ( interstitialAd != null )
        {
            interstitialAd.clearCache();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.clearCache();
            rewardedAd = null;
        }

        if ( adView != null )
        {
            adView.detach();
            adView.clearCache();
            adView = null;
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Loading interstitial ad for location \"" + location + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );

        interstitialAd = new Interstitial( location, new InterstitialAdListener( listener ), MEDIATION_PROVIDER );

        if ( interstitialAd.isCached() )
        {
            log( "Ad is available already" );
            listener.onInterstitialAdLoaded();
        }
        else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            interstitialAd.cache();
        }
        else // Chartboost does not support showing interstitial ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing interstitial ads for devices with Android versions lower than 21" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Showing interstitial ad for location \"" + location + "\"..." );

        if ( interstitialAd != null && interstitialAd.isCached() )
        {
            interstitialAd.show();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Loading rewarded ad for location \"" + location + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );

        rewardedAd = new Rewarded( location, new RewardedAdListener( listener ), MEDIATION_PROVIDER );

        if ( rewardedAd.isCached() )
        {
            log( "Ad is available already" );
            listener.onRewardedAdLoaded();
        }
        else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            rewardedAd.cache();
        }
        else // Chartboost does not support showing rewarded ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing rewarded ads for devices with Android versions lower than 21" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Showing rewarded ad for location \"" + location + "\"..." );

        if ( rewardedAd != null && rewardedAd.isCached() )
        {
            // Configure userReward from server.
            configureReward( parameters );
            rewardedAd.show();
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Loading " + adFormat.getLabel() + " ad for location \"" + location + "\"..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );

        adView = new Banner( activity.getApplicationContext(), location, toAdSize( adFormat ), new AdViewAdListener( listener, adFormat ), MEDIATION_PROVIDER );

        if ( adView.isCached() )
        {
            log( "Ad is available already" );
            listener.onAdViewAdLoaded( adView );
            showAdViewDelayed( listener );
        }
        else if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP )
        {
            adView.cache();
        }
        else  // Chartboost does not support showing ad view ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing " + adFormat.getLabel() + " ads for devices with Android versions lower than 21" );
            listener.onAdViewAdDisplayFailed( MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    //region GDPR

    private void updateConsentStatus(MaxAdapterParameters parameters, Context applicationContext)
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

    private static MaxAdapterError toMaxError(ClickError chartboostError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( chartboostError.getCode() )
        {
            case INTERNAL:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case URI_INVALID:
            case URI_UNRECOGNIZED:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
        }

        return new MaxAdapterError( adapterError, chartboostError.getCode().getErrorCode(), chartboostError.toString() );
    }

    private void showAdViewDelayed(final MaxAdViewAdapterListener listener)
    {
        // Chartboost requires manual show after caching ad views. Delay to allow enough time for attaching to parent.
        AppLovinSdkUtils.runOnUiThreadDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                if ( adView != null )
                {
                    adView.show();
                }
                else
                {
                    log( "Ad load failed: Chartboost Banner AdView is not ready." );
                    listener.onAdViewAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
                }
            }
        }, 500 );
    }

    //endregion

    private class InterstitialAdListener
            implements InterstitialCallback
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final CacheEvent cacheEvent, @Nullable final CacheError cacheError)
        {
            String location = cacheEvent.getAd().getLocation();
            if ( cacheError != null )
            {
                log( "Interstitial ad failed \"" + location + "\" to load with error: " + cacheError );
                listener.onInterstitialAdLoadFailed( toMaxError( cacheError ) );

                return;
            }

            log( "Interstitial ad loaded: " + location );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdRequestedToShow(@NonNull final ShowEvent showEvent)
        {
            log( "Interstitial ad requested to show: " + showEvent.getAd().getLocation() );
        }

        @Override
        public void onAdShown(@NonNull final ShowEvent showEvent, @Nullable final ShowError showError)
        {
            String location = showEvent.getAd().getLocation();
            if ( showError != null )
            {
                log( "Interstitial ad failed \"" + location + "\" to show with error: " + showError );
                listener.onInterstitialAdDisplayFailed( toMaxError( showError ) );

                return;
            }

            log( "Interstitial ad shown: " + location );
        }

        @Override
        public void onAdClicked(@NonNull final ClickEvent clickEvent, @Nullable final ClickError clickError)
        {
            String location = clickEvent.getAd().getLocation();
            if ( clickError != null )
            {
                log( "Failed to record interstitial ad click on \"" + location + "\" because of error: " + clickError );

                return;
            }

            log( "Interstitial ad clicked: " + location );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onImpressionRecorded(@NonNull final ImpressionEvent impressionEvent)
        {
            log( "Interstitial ad impression tracked: " + impressionEvent.getAd().getLocation() );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( impressionEvent.getAdID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionEvent.getAdID() );

                listener.onInterstitialAdDisplayed( extraInfo );
            }
            else
            {
                listener.onInterstitialAdDisplayed();
            }
        }

        @Override
        public void onAdDismiss(@NonNull final DismissEvent dismissEvent)
        {
            log( "Interstitial ad hidden: " + dismissEvent.getAd().getLocation() );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements RewardedCallback
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final CacheEvent cacheEvent, @Nullable final CacheError cacheError)
        {
            String location = cacheEvent.getAd().getLocation();
            if ( cacheError != null )
            {
                log( "Rewarded ad failed \"" + location + "\" to load with error: " + cacheError );
                listener.onRewardedAdLoadFailed( toMaxError( cacheError ) );

                return;
            }

            log( "Rewarded ad loaded: " + location );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdRequestedToShow(@NonNull final ShowEvent showEvent)
        {
            log( "Rewarded ad requested to show: " + showEvent.getAd().getLocation() );
        }

        @Override
        public void onAdShown(@NonNull final ShowEvent showEvent, @Nullable final ShowError showError)
        {
            String location = showEvent.getAd().getLocation();
            if ( showError != null )
            {
                log( "Rewarded ad failed \"" + location + "\" to show with error: " + showError );
                listener.onRewardedAdDisplayFailed( toMaxError( showError ) );

                return;
            }

            log( "Rewarded ad shown: " + location );
        }

        @Override
        public void onAdClicked(@NonNull final ClickEvent clickEvent, @Nullable final ClickError clickError)
        {
            String location = clickEvent.getAd().getLocation();
            if ( clickError != null )
            {
                log( "Failed to record rewarded ad click on \"" + location + "\" because of error: " + clickError );

                return;
            }

            log( "Rewarded ad clicked: " + location );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onImpressionRecorded(@NonNull final ImpressionEvent impressionEvent)
        {
            log( "Rewarded ad impression tracked: " + impressionEvent.getAd().getLocation() );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( impressionEvent.getAdID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionEvent.getAdID() );

                listener.onRewardedAdDisplayed( extraInfo );
            }
            else
            {
                listener.onRewardedAdDisplayed();
            }

            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onRewardEarned(@NonNull final RewardEvent rewardEvent)
        {
            log( "Rewarded ad granted reward: " + rewardEvent.getAd().getLocation() );
            hasGrantedReward = true;
        }

        @Override
        public void onAdDismiss(@NonNull final DismissEvent dismissEvent)
        {
            String location = dismissEvent.getAd().getLocation();
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded ad user with reward: " + reward + " at location: " + location );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden: " + location );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewAdListener
            implements BannerCallback
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;

        private AdViewAdListener(final MaxAdViewAdapterListener listener, final MaxAdFormat adFormat)
        {
            this.listener = listener;
            this.adFormat = adFormat;
        }

        @Override
        public void onAdLoaded(@NonNull final CacheEvent cacheEvent, @Nullable final CacheError cacheError)
        {
            String location = cacheEvent.getAd().getLocation();
            if ( cacheError != null )
            {
                log( adFormat.getLabel() + " ad failed \"" + location + "\" to load with error: " + cacheError );
                listener.onAdViewAdLoadFailed( toMaxError( cacheError ) );

                return;
            }

            log( adFormat.getLabel() + " ad loaded: " + location );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( cacheEvent.getAdID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", cacheEvent.getAdID() );

                listener.onAdViewAdLoaded( adView, extraInfo );
            }
            else
            {
                listener.onAdViewAdLoaded( adView );
            }

            showAdViewDelayed( listener );
        }

        @Override
        public void onAdRequestedToShow(@NonNull final ShowEvent showEvent)
        {
            log( adFormat.getLabel() + " ad requested to show: " + showEvent.getAd().getLocation() );
        }

        @Override
        public void onAdShown(@NonNull final ShowEvent showEvent, @Nullable final ShowError showError)
        {
            String location = showEvent.getAd().getLocation();
            if ( showError != null )
            {
                log( adFormat.getLabel() + " ad failed \"" + location + "\" to show with error: " + showError );
                listener.onAdViewAdDisplayFailed( toMaxError( showError ) );

                return;
            }

            log( adFormat.getLabel() + " ad shown: " + location );
        }

        @Override
        public void onAdClicked(@NonNull final ClickEvent clickEvent, @Nullable final ClickError clickError)
        {
            String location = clickEvent.getAd().getLocation();
            if ( clickError != null )
            {
                log( "Failed to record " + adFormat.getLabel() + " ad click on \"" + location + "\" because of error: " + clickError );

                return;
            }

            log( adFormat.getLabel() + " ad clicked: " + location );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onImpressionRecorded(@NonNull final ImpressionEvent impressionEvent)
        {
            log( adFormat.getLabel() + " ad impression tracked: " + impressionEvent.getAd().getLocation() );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( impressionEvent.getAdID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionEvent.getAdID() );

                listener.onAdViewAdDisplayed( extraInfo );
            }
            else
            {
                listener.onAdViewAdDisplayed();
            }
        }
    }
}
