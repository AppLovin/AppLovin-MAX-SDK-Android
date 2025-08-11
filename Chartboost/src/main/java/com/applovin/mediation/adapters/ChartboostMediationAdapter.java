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
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.chartboost.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
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
import com.chartboost.sdk.events.ExpirationEvent;
import com.chartboost.sdk.events.ImpressionEvent;
import com.chartboost.sdk.events.RewardEvent;
import com.chartboost.sdk.events.ShowError;
import com.chartboost.sdk.events.ShowEvent;
import com.chartboost.sdk.events.StartError;
import com.chartboost.sdk.privacy.model.CCPA;
import com.chartboost.sdk.privacy.model.DataUseConsent;
import com.chartboost.sdk.privacy.model.GDPR;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChartboostMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
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
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
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
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        String signal = Chartboost.getBidderToken();
        callback.onSignalCollected( signal );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        String bidResponse = parameters.getBidResponse();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + "interstitial ad for location \"" + location + "\"..." );

        updateConsentStatus( parameters, getContext( activity ) );

        interstitialAd = new Interstitial( location, new InterstitialAdListener( listener ), MEDIATION_PROVIDER );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            if ( isBidding )
            {
                interstitialAd.cache( bidResponse );
            }
            else
            {
                interstitialAd.cache();
            }
        }
        else // Chartboost does not support showing interstitial ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing interstitial ads for devices with Android versions lower than 21" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Showing interstitial ad for location \"" + location + "\"..." );

        if ( interstitialAd != null )
        {
            interstitialAd.show();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                                         MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        String bidResponse = parameters.getBidResponse();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + "rewarded ad for location \"" + location + "\"..." );

        updateConsentStatus( parameters, getContext( activity ) );

        rewardedAd = new Rewarded( location, new RewardedAdListener( listener ), MEDIATION_PROVIDER );

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
        {
            if ( isBidding )
            {
                rewardedAd.cache( bidResponse );
            }
            else
            {
                rewardedAd.cache();
            }
        }
        else // Chartboost does not support showing rewarded ads for devices with Android versions lower than 21
        {
            log( "Ad load failed: Chartboost does not support showing rewarded ads for devices with Android versions lower than 21" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        log( "Showing rewarded ad for location \"" + location + "\"..." );

        if ( rewardedAd != null && )
        {
            // Configure userReward from server.
            configureReward( parameters );
            rewardedAd.show();
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String location = retrieveLocation( parameters );
        String bidResponse = parameters.getBidResponse();
        boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBidding ? "bidding " : "" ) + adFormat.getLabel() + " ad for location \"" + location + "\"..." );

        updateConsentStatus( parameters, getContext( activity ) );

        adView = new Banner( getContext( activity ), location, toAdSize( adFormat ), new AdViewAdListener( listener, adFormat ), MEDIATION_PROVIDER );

        if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP )
        {
            if ( isBidding )
            {
                adView.cache( bidResponse );
            }
            else
            {
                adView.cache();
            }
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
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            DataUseConsent gdprConsent = new GDPR( hasUserConsent ? GDPR.GDPR_CONSENT.BEHAVIORAL : GDPR.GDPR_CONSENT.NON_BEHAVIORAL );
            Chartboost.addDataUseConsent( applicationContext, gdprConsent );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            DataUseConsent ccpaConsent = new CCPA( isDoNotSell ? CCPA.CCPA_CONSENT.OPT_OUT_SALE : CCPA.CCPA_CONSENT.OPT_IN_SALE );
            Chartboost.addDataUseConsent( applicationContext, ccpaConsent );
        }
    }

    //endregion

    //region Helper Methods

    private String retrieveLocation(MaxAdapterResponseParameters parameters)
    {
        if ( TextUtils.isEmpty( parameters.getThirdPartyAdPlacementId() ) )
        {
            return "Default";
        }
        else
        {
            return parameters.getThirdPartyAdPlacementId();
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
            case SERVER_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
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
                if ( adView != null && )
                {
                    adView.show();
                }
                else
                {
                    log( "Ad load failed: Chartboost Banner AdView is not ready." );
                    listener.onAdViewAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                           MaxAdapterError.AD_NOT_READY.getCode(),
                                                                           MaxAdapterError.AD_NOT_READY.getMessage() ) );
                }
            }
        }, 500 );
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
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
        public void onAdExpired(@NonNull final ExpirationEvent expirationEvent)
        {
            log( "Interstitial ad expired with reason: " + expirationEvent.getReason() );
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
                listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                             showError.getCode().getErrorCode(),
                                                                             showError.toString() ) );

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

            if ( TextUtils.isEmpty( impressionEvent.getAdID() ) )
            {
                listener.onInterstitialAdDisplayed();
            }
            else
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionEvent.getAdID() );

                listener.onInterstitialAdDisplayed( extraInfo );
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
        public void onAdExpired(@NonNull final ExpirationEvent expirationEvent)
        {
            log( "Rewarded ad expired with reason: " + expirationEvent.getReason() );
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
                listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         showError.getCode().getErrorCode(),
                                                                         showError.toString() ) );

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

            if ( TextUtils.isEmpty( impressionEvent.getAdID() ) )
            {
                listener.onRewardedAdDisplayed();
            }
            else
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionEvent.getAdID() );

                listener.onRewardedAdDisplayed( extraInfo );
            }
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

            if ( TextUtils.isEmpty( cacheEvent.getAdID() ) )
            {
                listener.onAdViewAdLoaded( adView );
            }
            else
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", cacheEvent.getAdID() );

                listener.onAdViewAdLoaded( adView, extraInfo );
            }

            showAdViewDelayed( listener );
        }

        @Override
        public void onAdExpired(@NonNull final ExpirationEvent expirationEvent)
        {
            log( "AdView ad expired with reason: " + expirationEvent.getReason() );
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
                listener.onAdViewAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                       showError.getCode().getErrorCode(),
                                                                       showError.toString() ) );

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

            if ( TextUtils.isEmpty( impressionEvent.getAdID() ) )
            {
                listener.onAdViewAdDisplayed();
            }
            else
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionEvent.getAdID() );

                listener.onAdViewAdDisplayed( extraInfo );
            }
        }
    }
}
