package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

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
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.hyprmx.android.sdk.banner.HyprMXBannerListener;
import com.hyprmx.android.sdk.banner.HyprMXBannerSize;
import com.hyprmx.android.sdk.banner.HyprMXBannerView;
import com.hyprmx.android.sdk.consent.ConsentStatus;
import com.hyprmx.android.sdk.core.HyprMX;
import com.hyprmx.android.sdk.core.HyprMXErrors;
import com.hyprmx.android.sdk.core.HyprMXIf;
import com.hyprmx.android.sdk.core.HyprMXState;
import com.hyprmx.android.sdk.placement.Placement;
import com.hyprmx.android.sdk.placement.PlacementListener;
import com.hyprmx.android.sdk.placement.RewardedPlacementListener;
import com.hyprmx.android.sdk.utility.HyprMXLog;

import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;

public class HyprMXMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter
{
    private static final String KEY_RANDOM_HYPRMX_USER_ID = "com.applovin.sdk.mediation.random_hyprmx_user_id";

    private HyprMXBannerView adView;
    private Placement        interstitialAd;
    private Placement        rewardedAd;

    // Explicit default constructor declaration
    public HyprMXMediationAdapter(final AppLovinSdk appLovinSdk)
    {
        super( appLovinSdk );
    }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( com.hyprmx.android.BuildConfig.class, "HYPRMX_VERSION" );
    }

    @Override
    public String getAdapterVersion()
    {
        return com.applovin.mediation.adapters.hyprmx.BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( adView != null )
        {
            adView.destroy();
            adView.setListener( null );
            adView = null;
        }

        if ( interstitialAd != null )
        {
            interstitialAd.setPlacementListener( null );
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setPlacementListener( null );
            rewardedAd = null;
        }
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( HyprMX.INSTANCE.getInitializationState() == HyprMXState.NOT_INITIALIZED )
        {
            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            final String distributorId = parameters.getServerParameters().getString( "distributor_id" );

            // HyprMX requires userId to initialize -> generate a random one
            String userId = getWrappingSdk().getUserIdentifier();
            if ( TextUtils.isEmpty( userId ) )
            {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( context );
                userId = sharedPreferences.getString( KEY_RANDOM_HYPRMX_USER_ID, null );
                if ( TextUtils.isEmpty( userId ) )
                {
                    userId = UUID.randomUUID().toString().toLowerCase( Locale.US );
                    sharedPreferences.edit().putString( KEY_RANDOM_HYPRMX_USER_ID, userId ).apply();
                }
            }

            log( "Initializing HyprMX SDK with distributor id: " + distributorId );

            HyprMXLog.enableDebugLogs( parameters.isTesting() );

            HyprMX.INSTANCE.setMediationProvider( "applovin_max", getAdapterVersion(), AppLovinSdk.VERSION );

            // NOTE: HyprMX deals with CCPA via their UI. Backend will filter HyprMX out in EU region.
            HyprMX.INSTANCE.initialize( context, distributorId, userId, getConsentStatus( parameters ), parameters.isAgeRestrictedUser(), new HyprMXIf.HyprMXInitializationListener()
            {
                @Override
                public void initializationComplete()
                {
                    log( "HyprMX SDK initialized" );
                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
                }

                @Override
                public void initializationFailed()
                {
                    log( "HyprMX SDK failed to initialize" );
                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, null );
                }
            } );
        }
        else
        {
            if ( HyprMX.INSTANCE.getInitializationState() == HyprMXState.INITIALIZATION_COMPLETE )
            {
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
            }
            else if ( HyprMX.INSTANCE.getInitializationState() == HyprMXState.INITIALIZATION_FAILED )
            {
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, null );
            }
            else if ( HyprMX.INSTANCE.getInitializationState() == HyprMXState.INITIALIZING )
            {
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZING, null );
            }
            else
            {
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_UNKNOWN, null );
            }
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + adFormat.getLabel() + " AdView ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        Context context = ( activity != null ) ? activity : getApplicationContext();

        adView = new HyprMXBannerView( context, null, placementId, toAdSize( adFormat ) );
        adView.setListener( new AdViewListener( listener ) );

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService( Context.WINDOW_SERVICE );
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics( displayMetrics );

        AppLovinSdkUtils.Size size = adFormat.getSize();
        adView.setLayoutParams( new LinearLayout.LayoutParams( Math.round( size.getWidth() * displayMetrics.density ),
                                                               Math.round( size.getHeight() * displayMetrics.density ) ) );
        adView.loadAd();
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad for placement: " + placementId );

        updateUserConsent( parameters );

        interstitialAd = createFullscreenAd( placementId, new InterstitialListener( listener ) );
        interstitialAd.loadAd();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad" );

        if ( interstitialAd.isAdAvailable() )
        {
            interstitialAd.showAd();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for placement: " + placementId );

        updateUserConsent( parameters );

        rewardedAd = createFullscreenAd( placementId, new RewardedAdListener( listener ) );
        rewardedAd.loadAd();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad" );

        if ( rewardedAd.isAdAvailable() )
        {
            // Configure reward from server.
            configureReward( parameters );

            rewardedAd.showAd();
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    //region Helper Methods

    private ConsentStatus getConsentStatus(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        Boolean isDoNotSell = parameters.isDoNotSell();

        // isTrue/isFalse/isNull to match the spec from HyprMX while avoiding NPEs
        if ( ( isNull( isDoNotSell ) || isFalse( isDoNotSell ) ) && isTrue( hasUserConsent ) )
        {
            return ConsentStatus.CONSENT_GIVEN;
        }
        else if ( isTrue( isDoNotSell ) || isFalse( hasUserConsent ) )
        {
            return ConsentStatus.CONSENT_DECLINED;
        }
        else
        {
            return ConsentStatus.CONSENT_STATUS_UNKNOWN;
        }
    }

    private boolean isTrue(Boolean privacyConsent)
    {
        return privacyConsent != null && privacyConsent;
    }

    private boolean isFalse(Boolean privacyConsent)
    {
        return privacyConsent != null && !privacyConsent;
    }

    private boolean isNull(Boolean privacyConsent)
    {
        return privacyConsent == null;
    }

    private void updateUserConsent(final MaxAdapterResponseParameters parameters)
    {
        // NOTE: HyprMX requested to always set GDPR regardless of region.
        HyprMX.INSTANCE.setConsentStatus( getConsentStatus( parameters ) );
    }

    private Placement createFullscreenAd(final String placementId, final PlacementListener listener)
    {
        Placement fullscreenPlacement = HyprMX.INSTANCE.getPlacement( placementId );
        fullscreenPlacement.setPlacementListener( listener );

        return fullscreenPlacement;
    }

    private HyprMXBannerSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return HyprMXBannerSize.HyprMXAdSizeBanner.INSTANCE;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return HyprMXBannerSize.HyprMXAdSizeMediumRectangle.INSTANCE;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return HyprMXBannerSize.HyprMXAdSizeLeaderboard.INSTANCE;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(HyprMXErrors hyprMXError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;

        if ( HyprMX.INSTANCE.getInitializationState() != HyprMXState.INITIALIZATION_COMPLETE )
        {
            return MaxAdapterError.NOT_INITIALIZED;
        }

        switch ( hyprMXError )
        {
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case DISPLAY_ERROR:
                adapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
            case AD_FAILED_TO_RENDER:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case PLACEMENT_DOES_NOT_EXIST:
            case AD_SIZE_NOT_SET:
            case PLACEMENT_NAME_NOT_SET:
            case INVALID_BANNER_PLACEMENT_NAME:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case SDK_NOT_INITIALIZED:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), hyprMXError.ordinal(), "" );
    }

    //endregion

    private class AdViewListener
            implements HyprMXBannerListener
    {
        final MaxAdViewAdapterListener listener;

        AdViewListener(MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull HyprMXBannerView ad)
        {
            log( "AdView loaded" );
            listener.onAdViewAdLoaded( ad );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdFailedToLoad(@NonNull HyprMXBannerView ad, @NonNull HyprMXErrors error)
        {
            log( "AdView failed to load with error " + error );
            listener.onAdViewAdLoadFailed( toMaxError( error ) );
        }

        @Override
        public void onAdOpened(@NonNull HyprMXBannerView ad)
        {
            log( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdClosed(@NonNull HyprMXBannerView ad)
        {
            log( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdClicked(@NonNull HyprMXBannerView ad)
        {
            log( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(@NonNull HyprMXBannerView ad)
        {
            log( "AdView will leave application" );
        }
    }

    private class InterstitialListener
            implements PlacementListener
    {
        final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdAvailable(Placement placement)
        {
            log( "Interstitial ad loaded: " + placement.getName() );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdNotAvailable(Placement placement)
        {
            log( "Interstitial failed to load: " + placement.getName() );
            listener.onInterstitialAdLoadFailed( toMaxError( HyprMXErrors.NO_FILL ) );
        }

        @Override
        public void onAdExpired(Placement placement)
        {
            log( "Interstitial expired: " + placement.getName() );
        }

        @Override
        public void onAdStarted(Placement placement)
        {
            log( "Interstitial did show: " + placement.getName() );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClosed(Placement placement, boolean finished)
        {
            log( "Interstitial ad hidden with finished state: " + finished + " for placement: " + placement.getName() );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdDisplayError(Placement placement, HyprMXErrors hyprMXError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", hyprMXError.ordinal(), hyprMXError.name() );
            log( "Interstitial failed to display with error: " + adapterError + ", for placement: " + placement.getName() );

            listener.onInterstitialAdDisplayFailed( adapterError );
        }
    }

    private class RewardedAdListener
            implements PlacementListener, RewardedPlacementListener
    {
        final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdAvailable(Placement placement)
        {
            log( "Rewarded ad loaded: " + placement.getName() );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdNotAvailable(Placement placement)
        {
            log( "Rewarded ad failed to load: " + placement.getName() );
            listener.onRewardedAdLoadFailed( toMaxError( HyprMXErrors.NO_FILL ) );
        }

        @Override
        public void onAdExpired(Placement placement)
        {
            log( "Rewarded ad expired: " + placement.getName() );
        }

        @Override
        public void onAdStarted(Placement placement)
        {
            log( "Rewarded ad did show: " + placement.getName() );

            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdClosed(Placement placement, boolean finished)
        {
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden with finished state: " + finished + " for placement: " + placement.getName() );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onAdDisplayError(Placement placement, HyprMXErrors hyprMXError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.UNSPECIFIED.getErrorCode(), MaxAdapterError.UNSPECIFIED.getErrorMessage(), hyprMXError.ordinal(), hyprMXError.name() );
            log( "Rewarded ad failed to display with error: " + adapterError + ", for placement: " + placement.getName() );

            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdRewarded(Placement placement, String rewardName, int rewardValue)
        {
            log( "Rewarded ad for placement: " + placement.getName() + " granted reward with rewardName: " + rewardName + " rewardValue: " + rewardValue );
            hasGrantedReward = true;
        }
    }
}
