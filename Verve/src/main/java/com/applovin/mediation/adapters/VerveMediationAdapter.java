package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
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
import com.applovin.mediation.adapters.verve.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

import net.pubnative.lite.sdk.HyBid;
import net.pubnative.lite.sdk.HyBidError;
import net.pubnative.lite.sdk.UserDataManager;
import net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd;
import net.pubnative.lite.sdk.models.AdSize;
import net.pubnative.lite.sdk.models.ImpressionTrackingMethod;
import net.pubnative.lite.sdk.rewarded.HyBidRewardedAd;
import net.pubnative.lite.sdk.views.HyBidAdView;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

public class VerveMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxSignalProvider
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private HyBidInterstitialAd interstitialAd;
    private HyBidRewardedAd     rewardedAd;
    private HyBidAdView         adViewAd;

    // Explicit default constructor declaration
    public VerveMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return HyBid.getSDKVersionInfo();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final String appToken = parameters.getServerParameters().getString( "app_token", "" );
            log( "Initializing Verve SDK with app token: " + appToken + "..." );

            if ( parameters.isTesting() )
            {
                HyBid.setTestMode( true );
            }

            HyBid.initialize( appToken, (Application) getApplicationContext() );

            if ( HyBid.isInitialized() )
            {
                log( "Verve SDK initialized" );
                status = InitializationStatus.INITIALIZED_SUCCESS;
            }
            else
            {
                log( "Verve SDK failed to initialize" );
                status = InitializationStatus.INITIALIZED_FAILURE;
            }

            onCompletionListener.onCompletion( status, null );
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public void onDestroy()
    {
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

        if ( adViewAd != null )
        {
            adViewAd.destroy();
            adViewAd = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting Signal..." );

        // Update local params, since not available on init
        updateLocationCollectionEnabled( parameters );
        updateUserConsent( parameters );

        String signal = HyBid.getCustomRequestSignalData();
        callback.onSignalCollected( signal );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Loading interstitial ad" );

        if ( !HyBid.isInitialized() )
        {
            log( "Verve SDK is not initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateLocationCollectionEnabled( parameters );
        updateUserConsent( parameters );

        interstitialAd = new HyBidInterstitialAd( getApplicationContext(), null, "", new InterstitialListener( listener ) );
        interstitialAd.prepareAd( parameters.getBidResponse() );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAd.isReady() )
        {
            interstitialAd.show();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Loading rewarded ad" );

        if ( !HyBid.isInitialized() )
        {
            log( "Verve SDK is not initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateLocationCollectionEnabled( parameters );
        updateUserConsent( parameters );

        rewardedAd = new HyBidRewardedAd( getApplicationContext(), null, "", new RewardedListener( listener ) );
        rewardedAd.prepareAd( parameters.getBidResponse() );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAd.isReady() )
        {
            configureReward( parameters );
            rewardedAd.show();
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        log( "Loading " + adFormat.getLabel() + " ad view ad..." );

        if ( !HyBid.isInitialized() )
        {
            log( "Verve SDK is not initialized: failing " + adFormat.getLabel() + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateLocationCollectionEnabled( parameters );
        updateUserConsent( parameters );

        adViewAd = new HyBidAdView( getApplicationContext(), getSize( adFormat ) );
        adViewAd.setTrackingMethod( ImpressionTrackingMethod.AD_VIEWABLE );
        adViewAd.renderAd( parameters.getBidResponse(), new AdViewListener( listener ) );
    }

    private void updateUserConsent(final MaxAdapterParameters parameters)
    {
        // From PubNative: "HyBid SDK is TCF v2 compliant, so any change in the IAB consent string will be picked up by the SDK."
        // Because of this, they requested that we don't update consent values if one is already set and use binary consent state as a fallback.
        // As a side effect, pubs that use the MAX consent flow will not be able to update consent values mid-session.
        // Full context in this PR: https://github.com/AppLovin/AppLovin-MAX-SDK-iOS/pull/57

        UserDataManager userDataManager = HyBid.getUserDataManager();

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null && userDataManager != null )
        {
            // NOTE: verveGDPRConsentString can be nil, TCFv2 consent string, "1" or "0"
            String verveGDPRConsentString = userDataManager.getIABGDPRConsentString();

            // If hasUserConsent is set to false, set consent string to "0"
            if ( !hasUserConsent )
            {
                userDataManager.setIABGDPRConsentString( "0" );
            }
            // If hasUserConsent is set to true, only override if it has not been set to a TCFv2 consent string or is set to "0"
            else if ( TextUtils.isEmpty( verveGDPRConsentString ) || verveGDPRConsentString.equals( "0" ) )
            {
                userDataManager.setIABGDPRConsentString( "1" );
            }
        }

        if ( userDataManager != null && TextUtils.isEmpty( userDataManager.getIABUSPrivacyString() ) )
        {
            Boolean isDoNotSell = parameters.isDoNotSell();
            if ( isDoNotSell != null && isDoNotSell )
            {
                // NOTE: PubNative suggested this US Privacy String, so it does not match other adapters.
                userDataManager.setIABUSPrivacyString( "1NYN" );
            }
        }
    }

    private void updateLocationCollectionEnabled(final MaxAdapterParameters parameters)
    {
        Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        Object isLocationCollectionEnabledObj = localExtraParameters.get( "is_location_collection_enabled" );
        if ( isLocationCollectionEnabledObj instanceof Boolean )
        {
            log( "Setting location collection enabled: " + isLocationCollectionEnabledObj );
            HyBid.setLocationUpdatesEnabled( (boolean) isLocationCollectionEnabledObj );
        }
    }

    private static AdSize getSize(MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.SIZE_320x50;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.SIZE_728x90;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.SIZE_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(Throwable verveError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        if ( verveError instanceof HyBidError )
        {
            HyBidError hyBidError = (HyBidError) verveError;
            switch ( hyBidError.getErrorCode() )
            {
                case NO_FILL:
                case NULL_AD:
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case INVALID_ASSET:
                case UNSUPPORTED_ASSET:
                case MRAID_PLAYER_ERROR:
                case VAST_PLAYER_ERROR:
                case ERROR_TRACKING_URL:
                case ERROR_TRACKING_JS:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
                case PARSER_ERROR:
                case SERVER_ERROR_PREFIX:
                    adapterError = MaxAdapterError.SERVER_ERROR;
                    break;
                case INVALID_AD:
                case INVALID_ZONE_ID:
                case INVALID_SIGNAL_DATA:
                case INVALID_URL:
                    adapterError = MaxAdapterError.BAD_REQUEST;
                    break;
                case NOT_INITIALISED:
                    adapterError = MaxAdapterError.NOT_INITIALIZED;
                    break;
                case AUCTION_NO_AD:
                case ERROR_RENDERING_BANNER:
                case ERROR_RENDERING_INTERSTITIAL:
                case ERROR_RENDERING_REWARDED:
                    adapterError = MaxAdapterError.AD_NOT_READY;
                    break;
                case INTERNAL_ERROR:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case DISABLED_FORMAT:
                case DISABLED_RENDERING_ENGINE:
                case ERROR_LOADING_FEEDBACK:
                    adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                    break;
                case EXPIRED_AD:
                    adapterError = MaxAdapterError.AD_EXPIRED;
                    break;
            }
        }
        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), 0, verveError.getMessage() );
    }

    private class InterstitialListener
            implements HyBidInterstitialAd.Listener
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onInterstitialLoaded()
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onInterstitialLoadFailed(final Throwable error)
        {
            log( "Interstitial ad failed to load with error: " + error );
            MaxAdapterError adapterError = toMaxError( error );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onInterstitialImpression()
        {
            log( "Interstitial did track impression" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onInterstitialClick()
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onInterstitialDismissed()
        {
            log( "Interstitial hidden" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedListener
            implements HyBidRewardedAd.Listener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        private RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRewardedLoaded()
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onRewardedLoadFailed(final Throwable error)
        {
            log( "Rewarded ad failed to load with error: " + error );
            MaxAdapterError adapterError = toMaxError( error );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onRewardedOpened()
        {
            log( "Rewarded ad did track impression" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onRewardedClick()
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onReward()
        {
            log( "Rewarded ad reward granted" );
            hasGrantedReward = true;
        }

        @Override
        public void onRewardedClosed()
        {
            log( "Rewarded ad did disappear" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden" );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            implements HyBidAdView.Listener
    {
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "AdView ad loaded" );
            listener.onAdViewAdLoaded( adViewAd );
        }

        @Override
        public void onAdLoadFailed(final Throwable error)
        {
            log( "AdView failed to load with error: " + error );
            MaxAdapterError adapterError = toMaxError( error );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( "AdView did track impression" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClick()
        {
            log( "AdView clicked" );
            listener.onAdViewAdClicked();
        }
    }
}
