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

    private static volatile InitializationStatus status = InitializationStatus.INITIALIZING;

    private Interstitial interstitialAd;
    private Rewarded     rewardedAd;
    private Banner       adView;

    // NOTE: Expiry is tracked on the listener rather than here. Each load builds a new ad and a new
    // listener, but a superseded ad's listener stays registered with the Chartboost SDK and can still
    // deliver `onAdExpired`. An adapter-level flag would let that late callback mark a newer, valid ad
    // as expired. This mirrors how `hasGrantedReward` is already scoped in `RewardedAdListener`.
    private InterstitialAdListener interstitialAdListener;
    private RewardedAdListener     rewardedAdListener;

    // Explicit default constructor declaration
    public ChartboostMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
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
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.destroy();
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

        // NOTE: `getBidderToken()` returns null until the Chartboost SDK has started, and MAX would
        // otherwise record that as a successful collection carrying no token.
        if ( !Chartboost.isSdkStarted() )
        {
            log( "Signal collection failed: Chartboost SDK is not started" );
            callback.onSignalCollectionFailed( "Chartboost SDK is not started" );

            return;
        }

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

        interstitialAdListener = new InterstitialAdListener( listener );
        interstitialAd = new Interstitial( location, interstitialAdListener, MEDIATION_PROVIDER );

        // NOTE: Do not use `isCached()` since it does not reliably indicate ad readiness.
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

        if ( interstitialAdListener != null && interstitialAdListener.adExpired )
        {
            log( "Interstitial ad expired" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_EXPIRED.getCode(),
                                                                         MaxAdapterError.AD_EXPIRED.getMessage() ) );

            return;
        }

        // NOTE: Do not use `isCached()` since it does not reliably indicate ad readiness.
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

        rewardedAdListener = new RewardedAdListener( listener );
        rewardedAd = new Rewarded( location, rewardedAdListener, MEDIATION_PROVIDER );

        // NOTE: Do not use `isCached()` since it does not reliably indicate ad readiness.
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

        if ( rewardedAdListener != null && rewardedAdListener.adExpired )
        {
            log( "Rewarded ad expired" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_EXPIRED.getCode(),
                                                                     MaxAdapterError.AD_EXPIRED.getMessage() ) );

            return;
        }

        // NOTE: Do not use `isCached()` since it does not reliably indicate ad readiness.
        if ( rewardedAd != null )
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

        // NOTE: Do not use `isCached()` since it does not reliably indicate ad readiness.
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
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
            listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
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

    // NOTE: `INVALID_RESPONSE`, `INVALID_ADM` and `UNSUPPORTED_CODEC` are deliberately unmapped. The Chartboost
    // SDK routes their sources to `SERVER_ERROR` and `ASSET_DOWNLOAD_FAILURE`, so it cannot produce those codes.
    private static MaxAdapterError toMaxError(CacheError chartboostError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( chartboostError.getCode() )
        {
            case INTERNAL:
            case NO_STORAGE:
            case NO_MRAID_JS:
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
            case INVALID_REQUEST:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case BANNER_DISABLED:
            case BANNER_VIEW_IS_DETACHED:
            case DISABLED:
            case INVALID_PLACEMENT:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case SERVER_ERROR:
            case RATE_LIMITED:
            case INVALID_HTML:
            case INVALID_ASSET_URL:
            case VAST_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case LOAD_IN_PROGRESS:
            case ALREADY_LOADED:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case WEBVIEW_FAILED:
            case WEBVIEW_CRASHED:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError, chartboostError.getCode().getErrorCode(), chartboostError.toString() );
    }

    // NOTE: `creative_id` carries Chartboost's auction ID, not a creative ID, and the Chartboost SDK exposes no
    // creative ID that could correct it. It is left in place because existing reporting already joins on it, and
    // the same value is repeated under a truthful name so publishers have one key that means what it says.
    private static Bundle createExtraInfo(final String auctionId)
    {
        Bundle adValues = new Bundle( 1 );
        adValues.putString( "chartboost_auction_id", auctionId );

        Bundle extraInfo = new Bundle( 2 );
        extraInfo.putString( "creative_id", auctionId );
        extraInfo.putBundle( "ad_values", adValues );

        return extraInfo;
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
        private final    MaxInterstitialAdapterListener listener;
        private volatile boolean                        adExpired;

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
            adExpired = true;
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
                // NOTE: There is deliberately no `ShowError` counterpart to `toMaxError( CacheError )`. Show
                // failures report `AD_DISPLAY_FAILED` and pass the Chartboost code and message through in the
                // mediated-network slot, which is what the other adapters in this repo do.
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
                listener.onInterstitialAdDisplayed( createExtraInfo( impressionEvent.getAdID() ) );
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
        private final    MaxRewardedAdapterListener listener;
        private          boolean                    hasGrantedReward;
        private volatile boolean                    adExpired;

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
            adExpired = true;
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
                listener.onRewardedAdDisplayed( createExtraInfo( impressionEvent.getAdID() ) );
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

            final Banner loadedAdView = (Banner) cacheEvent.getAd();

            if ( TextUtils.isEmpty( cacheEvent.getAdID() ) )
            {
                listener.onAdViewAdLoaded( loadedAdView );
            }
            else
            {
                listener.onAdViewAdLoaded( loadedAdView, createExtraInfo( cacheEvent.getAdID() ) );
            }

            // Chartboost requires a manual show after caching ad views. Since Chartboost SDK 9.14.0 its
            // visibility tracker attaches to the window before it starts, and registers an
            // OnAttachStateChangeListener when the view is not attached yet, so showing here is safe even
            // though MAX has not added this Banner to the publisher's layout. Earlier SDK versions bound the
            // visibility check to a view tree that was then replaced and the impression never fired, which is
            // why the 9.13.0.0 adapter deferred this call behind a fixed 500ms delay.
            loadedAdView.show();
        }

        @Override
        public void onAdExpired(@NonNull final ExpirationEvent expirationEvent)
        {
            // NOTE: Deliberately log-only. MAX has no ad view callback for an ad that expires after it is
            // displayed, and it drives its own banner refresh. The show in `onAdLoaded` is synchronous with
            // the load, so there is no window between the two in which an expiry could be acted on either.
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
                listener.onAdViewAdDisplayed( createExtraInfo( impressionEvent.getAdID() ) );
            }
        }
    }
}
