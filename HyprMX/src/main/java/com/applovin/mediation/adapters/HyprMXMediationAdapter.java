package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
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
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.hyprmx.android.sdk.banner.HyprMXBannerListener;
import com.hyprmx.android.sdk.banner.HyprMXBannerSize;
import com.hyprmx.android.sdk.banner.HyprMXBannerView;
import com.hyprmx.android.sdk.consent.ConsentStatus;
import com.hyprmx.android.sdk.core.HyprMX;
import com.hyprmx.android.sdk.core.HyprMXErrors;
import com.hyprmx.android.sdk.placement.HyprMXLoadAdListener;
import com.hyprmx.android.sdk.placement.HyprMXRewardedShowListener;
import com.hyprmx.android.sdk.placement.HyprMXShowListener;
import com.hyprmx.android.sdk.placement.Placement;
import com.hyprmx.android.sdk.utility.HyprMXLog;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HyprMXMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

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

        interstitialAd = null;
        rewardedAd = null;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            initializationStatus = InitializationStatus.INITIALIZING;

            final String distributorId = parameters.getServerParameters().getString( "distributor_id" );

            log( "Initializing HyprMX SDK with distributor id: " + distributorId );

            HyprMXLog.enableDebugLogs( parameters.isTesting() );

            HyprMX.INSTANCE.setMediationProvider( "applovin_max", getAdapterVersion(), AppLovinSdk.VERSION );

            updateUserConsent( parameters );

            HyprMX.INSTANCE.initialize( getApplicationContext(), distributorId, initResult -> {

                if ( !initResult.isSuccess() )
                {
                    log( "HyprMX SDK failed to initialize for distributorId: " + distributorId );
                    initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( initializationStatus, initResult.getMessage() );
                    return;
                }

                log( "HyprMX SDK initialized for distributorId: " + distributorId );
                initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                onCompletionListener.onCompletion( initializationStatus, null );
            } );
        }
        else
        {
            onCompletionListener.onCompletion( initializationStatus, null );
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserConsent( parameters );

        String signal = HyprMX.INSTANCE.sessionToken();
        callback.onSignalCollected( signal );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + adFormat.getLabel() + " AdView ad for placement: " + placementId + "..." );

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

        HyprMXLoadAdListener adViewAdLoadListener = isAdAvailable -> {

            if ( !isAdAvailable )
            {
                log( "AdView failed to load for placement: " + placementId );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            log( "AdView loaded for placement: " + placementId );
            listener.onAdViewAdLoaded( adView );
        };

        if ( isBidding )
        {
            adView.loadAd( bidResponse, adViewAdLoadListener );
        }
        else
        {
            adView.loadAd( adViewAdLoadListener );
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + "interstitial ad for placement: " + placementId );

        updateUserConsent( parameters );

        interstitialAd = HyprMX.INSTANCE.getPlacement( placementId );

        HyprMXLoadAdListener interstitialAdLoadListener = isAdAvailable -> {

            if ( !isAdAvailable )
            {
                log( "Interstitial failed to load for placement: " + placementId );
                listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            log( "Interstitial ad loaded for placement: " + placementId );
            listener.onInterstitialAdLoaded();
        };

        if ( isBidding )
        {
            interstitialAd.loadAd( bidResponse, interstitialAdLoadListener );
        }
        else
        {
            interstitialAd.loadAd( interstitialAdLoadListener );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for placement: " + placementId );

        if ( interstitialAd == null || !interstitialAd.isAdAvailable() )
        {
            log( "Interstitial ad not ready for placement: " + placementId );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.ERROR_CODE_AD_DISPLAY_FAILED, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
            return;
        }

        interstitialAd.showAd( new InterstitialListener( listener ) );
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        final boolean isBidding = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBidding ? "bidding " : "" ) + "rewarded ad for placement: " + placementId );

        updateUserConsent( parameters );

        rewardedAd = HyprMX.INSTANCE.getPlacement( placementId );

        HyprMXLoadAdListener rewardedAdLoadListener = isAdAvailable -> {

            if ( !isAdAvailable )
            {
                log( "Rewarded ad failed to load for placement: " + placementId );
                listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            log( "Rewarded ad loaded for placement: " + placementId );
            listener.onRewardedAdLoaded();
        };

        if ( isBidding )
        {
            rewardedAd.loadAd( bidResponse, rewardedAdLoadListener );
        }
        else
        {
            rewardedAd.loadAd( rewardedAdLoadListener );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for placement: " + placementId );

        if ( rewardedAd == null || !rewardedAd.isAdAvailable() )
        {
            log( "Rewarded ad not ready for placement: " + placementId );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.ERROR_CODE_AD_DISPLAY_FAILED, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
            return;
        }

        // Configure reward from server.
        configureReward( parameters );

        rewardedAd.showAd( new RewardedAdListener( listener ) );
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

    private void updateUserConsent(final MaxAdapterParameters parameters)
    {
        // NOTE: HyprMX requested to always set GDPR regardless of region.
        HyprMX.INSTANCE.setConsentStatus( getConsentStatus( parameters ) );
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
        public void onAdImpression(@NonNull final HyprMXBannerView ad)
        {
            log( "AdView tracked impression for placement: " + ad.getPlacementName() );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final HyprMXBannerView ad)
        {
            log( "AdView clicked for placement: " + ad.getPlacementName() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdOpened(@NonNull final HyprMXBannerView ad)
        {
            log( "AdView expanded for placement: " + ad.getPlacementName() );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdClosed(@NonNull final HyprMXBannerView ad)
        {
            log( "AdView collapsed for placement: " + ad.getPlacementName() );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdLeftApplication(@NonNull final HyprMXBannerView ad)
        {
            log( "AdView will leave application for placement: " + ad.getPlacementName() );
        }
    }

    private class InterstitialListener
            implements HyprMXShowListener
    {
        final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdStarted(@NonNull final Placement placement)
        {
            log( "Interstitial did show: " + placement.getName() );
        }

        @Override
        public void onAdImpression(@NonNull final Placement placement)
        {
            log( "Interstitial did track impression: " + placement.getName() );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdDisplayError(@NonNull final Placement placement, @NonNull final HyprMXErrors hyprMXError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.ERROR_CODE_AD_DISPLAY_FAILED, "Ad Display Failed", hyprMXError.ordinal(), hyprMXError.name() );
            log( "Interstitial failed to display with error: " + adapterError + ", for placement: " + placement.getName() );

            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClosed(@NonNull final Placement placement, final boolean finished)
        {
            log( "Interstitial ad hidden with finished state: " + finished + " for placement: " + placement.getName() );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements HyprMXRewardedShowListener
    {
        final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdStarted(@NonNull final Placement placement)
        {
            log( "Rewarded ad did show: " + placement.getName() );
        }

        @Override
        public void onAdImpression(@NonNull final Placement placement)
        {
            log( "Rewarded ad did track impression: " + placement.getName() );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdDisplayError(@NonNull final Placement placement, @NonNull final HyprMXErrors hyprMXError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.ERROR_CODE_AD_DISPLAY_FAILED, "Ad Display Failed", hyprMXError.ordinal(), hyprMXError.name() );
            log( "Rewarded ad failed to display with error: " + adapterError + ", for placement: " + placement.getName() );

            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdRewarded(@NonNull final Placement placement, @NonNull final String rewardName, final int rewardValue)
        {
            log( "Rewarded ad for placement: " + placement.getName() + " granted reward with rewardName: " + rewardName + " rewardValue: " + rewardValue );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed(@NonNull final Placement placement, final boolean finished)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward + " for placement: " + placement.getName() );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden with finished state: " + finished + " for placement: " + placement.getName() );
            listener.onRewardedAdHidden();
        }
    }
}
