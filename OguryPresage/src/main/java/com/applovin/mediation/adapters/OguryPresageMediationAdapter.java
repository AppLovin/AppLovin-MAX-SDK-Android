package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;

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
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.ogurypresage.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.ogury.ad.OguryAdError;
import com.ogury.ad.OguryBannerAdSize;
import com.ogury.ad.OguryBannerAdView;
import com.ogury.ad.OguryBannerAdViewListener;
import com.ogury.ad.OguryBidTokenListener;
import com.ogury.ad.OguryBidTokenProvider;
import com.ogury.ad.OguryInterstitialAd;
import com.ogury.ad.OguryInterstitialAdListener;
import com.ogury.ad.OguryLoadErrorCode;
import com.ogury.ad.OguryReward;
import com.ogury.ad.OguryRewardedAd;
import com.ogury.ad.OguryRewardedAdListener;
import com.ogury.ad.common.OguryMediation;
import com.ogury.core.OguryError;
import com.ogury.sdk.Ogury;
import com.ogury.sdk.OguryOnStartListener;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This is an AppLovin Mediation Adapter for Ogury Presage SDK.
 * <p>
 * Created by santoshbagadi on 11/22/19.
 */
public class OguryPresageMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private OguryInterstitialAd interstitialAd;
    private OguryRewardedAd     rewardedAd;
    private OguryBannerAdView   adView;

    // Explicit default constructor declaration
    public OguryPresageMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter methods
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            final String assetKey = parameters.getServerParameters().getString( "asset_key" );
            log( "Initializing Ogury Presage SDK with asset key: " + assetKey + "..." );

            Ogury.start( getContext( activity ), assetKey, new OguryOnStartListener()
            {
                @Override
                public void onStarted()
                {
                    log( "Ogury Presage SDK initialized" );
                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }

                @Override
                public void onFailed(@NonNull final OguryError oguryError)
                {
                    log( "Ogury Presage SDK failed to initialize with error: " + oguryError );
                    status = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( status, oguryError.getMessage() );
                }
            } );
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return Ogury.getSdkVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        interstitialAd = null;

        rewardedAd = null;

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }
    }
    //endregion

    //region Signal Collection
    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        OguryBidTokenProvider.getBidToken( getContext( activity ), new OguryBidTokenListener()
        {
            @Override
            public void onBidTokenGenerated(@NonNull final String signal)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( signal );
            }

            @Override
            public void onBidTokenGenerationFailed(@NonNull final OguryError oguryError)
            {
                log( "Signal collection failed with error: " + oguryError );
                callback.onSignalCollectionFailed( oguryError.getMessage() );
            }
        } );
    }
    //endregion

    //region MaxInterstitialAdapter methods
    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad: " + placementId + "..." );

        interstitialAd = new OguryInterstitialAd( getContext( activity ), placementId, new OguryMediation( "AppLovin MAX", AppLovinSdk.VERSION ) );

        InterstitialAdListener adListener = new InterstitialAdListener( placementId, listener );
        interstitialAd.setListener( adListener );

        if ( interstitialAd.isLoaded() )
        {
            log( "Ad is available already" );
            listener.onInterstitialAdLoaded();

            return;
        }

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            interstitialAd.load( bidResponse );
        }
        else
        {
            interstitialAd.load();
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + placementId + "..." );

        if ( !interstitialAd.isLoaded() )
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );

            return;
        }

        interstitialAd.show();
    }
    //endregion

    //region MaxRewardedAdapter methods
    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad: " + placementId + "..." );

        rewardedAd = new OguryRewardedAd( getContext( activity ), placementId, new OguryMediation( "AppLovin MAX", AppLovinSdk.VERSION ) );

        RewardedAdListener adListener = new RewardedAdListener( placementId, listener );
        rewardedAd.setListener( adListener );

        if ( rewardedAd.isLoaded() )
        {
            log( "Ad is available already" );
            listener.onRewardedAdLoaded();

            return;
        }

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            rewardedAd.load( bidResponse );
        }
        else
        {
            rewardedAd.load();
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad: " + placementId + "..." );

        if ( !rewardedAd.isLoaded() )
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );

            return;
        }

        // Configure userReward from server
        configureReward( parameters );

        rewardedAd.show();
    }
    //endregion

    //region MaxAdViewAdapter methods
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + adFormat.getLabel() + " ad: " + placementId + "..." );

        adView = new OguryBannerAdView( getContext( activity ), placementId, toAdSize( adFormat ), new OguryMediation( "AppLovin MAX", AppLovinSdk.VERSION ) );

        AdViewListener adListener = new AdViewListener( placementId, listener );
        adView.setListener( adListener );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            adView.load( bidResponse );
        }
        else
        {
            adView.load();
        }
    }
    //endregion

    //region Helper Methods
    private static MaxAdapterError toMaxError(final OguryAdError oguryError)
    {
        final int oguryErrorCode = oguryError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( oguryErrorCode )
        {
            case OguryLoadErrorCode.AD_DISABLED_UNSPECIFIED_REASON:
                // We are not sure what kind of load error it is - may be misconfigured ad unit id, et al...
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case OguryLoadErrorCode.NO_ACTIVE_INTERNET_CONNECTION:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case OguryLoadErrorCode.AD_DISABLED_COUNTRY_NOT_OPENED:
            case OguryLoadErrorCode.AD_DISABLED_CONSENT_DENIED:
            case OguryLoadErrorCode.AD_DISABLED_CONSENT_MISSING:
            case OguryLoadErrorCode.INVALID_CONFIGURATION:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case OguryLoadErrorCode.SDK_NOT_STARTED:
            case OguryLoadErrorCode.SDK_NOT_PROPERLY_INITIALIZED:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case OguryLoadErrorCode.AD_REQUEST_FAILED:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case OguryLoadErrorCode.AD_PARSING_FAILED: // 2010
            case OguryLoadErrorCode.AD_PRECACHING_FAILED: // 2010
            case OguryLoadErrorCode.AD_PRECACHING_TIMEOUT: // 2010
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case OguryLoadErrorCode.NO_FILL: // 2008
                adapterError = MaxAdapterError.NO_FILL;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), oguryErrorCode, oguryError.getMessage() );
    }

    private OguryBannerAdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER )
        {
            return OguryBannerAdSize.SMALL_BANNER_320x50;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return OguryBannerAdSize.MREC_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }
    //endregion

    private class InterstitialAdListener
            implements OguryInterstitialAdListener
    {
        private final String                         placementId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String placementId, final MaxInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final OguryInterstitialAd interstitialAd)
        {
            log( "Interstitial loaded: " + placementId );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdImpression(@NonNull final OguryInterstitialAd interstitialAd)
        {
            log( "Interstitial triggered impression: " + placementId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final OguryInterstitialAd interstitialAd)
        {
            log( "Interstitial clicked: " + placementId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed(@NonNull final OguryInterstitialAd interstitialAd)
        {
            log( "Interstitial hidden: " + placementId );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdError(@NonNull final OguryInterstitialAd interstitialAd, @NonNull final OguryAdError oguryError)
        {
            if ( oguryError.getType() == OguryAdError.Type.SHOW_ERROR )
            {
                log( "Interstitial (" + placementId + ") failed to show with error: " + oguryError );
                listener.onInterstitialAdDisplayFailed( toMaxError( oguryError ) );
            }
            else
            {
                log( "Interstitial (" + placementId + ") failed to load with error: " + oguryError );
                listener.onInterstitialAdLoadFailed( toMaxError( oguryError ) );
            }
        }
    }

    private class RewardedAdListener
            implements OguryRewardedAdListener
    {
        private final String                     placementId;
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedAdListener(final String placementId, final MaxRewardedAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final OguryRewardedAd rewardedAd)
        {
            log( "Rewarded ad loaded: " + placementId );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdImpression(@NonNull final OguryRewardedAd rewardedAd)
        {
            log( "Rewarded ad triggered impression: " + placementId );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final OguryRewardedAd rewardedAd)
        {
            log( "Rewarded ad clicked: " + placementId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdClosed(@NonNull final OguryRewardedAd rewardedAd)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden: " + placementId );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onAdRewarded(@NonNull final OguryRewardedAd rewardedAd, final OguryReward oguryReward)
        {
            log( "Rewarded ad (" + placementId + ") granted reward with rewardName: " + oguryReward.getName() + ", rewardValue: " + oguryReward.getValue() );
            hasGrantedReward = true;
        }

        @Override
        public void onAdError(@NonNull final OguryRewardedAd rewardedAd, final OguryAdError oguryError)
        {
            if ( oguryError.getType() == OguryAdError.Type.SHOW_ERROR )
            {
                log( "Rewarded ad (" + placementId + ") failed to show with error: " + oguryError );
                listener.onRewardedAdDisplayFailed( toMaxError( oguryError ) );
            }
            else
            {
                log( "Rewarded ad (" + placementId + ") failed to load with error: " + oguryError );
                listener.onRewardedAdLoadFailed( toMaxError( oguryError ) );
            }
        }
    }

    private class AdViewListener
            implements OguryBannerAdViewListener
    {
        private final String                   placementId;
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final String placementId, final MaxAdViewAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final OguryBannerAdView bannerAd)
        {
            log( "AdView loaded: " + placementId );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onAdImpression(@NonNull final OguryBannerAdView bannerAd)
        {
            log( "AdView triggered impression: " + placementId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final OguryBannerAdView bannerAd)
        {
            log( "AdView clicked: " + placementId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdClosed(@NonNull final OguryBannerAdView bannerAd)
        {
            log( "AdView ad hidden: " + placementId );
        }

        @Override
        public void onAdError(@NonNull final OguryBannerAdView bannerAd, @NonNull final OguryAdError oguryError)
        {
            if ( oguryError.getType() == OguryAdError.Type.SHOW_ERROR )
            {
                log( "AdView ad (" + placementId + ") failed to show with error: " + oguryError );
                listener.onAdViewAdDisplayFailed( toMaxError( oguryError ) );
            }
            else
            {
                log( "AdView ad (" + placementId + ") failed to load with error: " + oguryError );
                listener.onAdViewAdLoadFailed( toMaxError( oguryError ) );
            }
        }
    }
}
