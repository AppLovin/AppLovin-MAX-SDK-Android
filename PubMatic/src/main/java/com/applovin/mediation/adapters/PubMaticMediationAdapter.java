package com.applovin.mediation.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;

import com.applovin.impl.sdk.utils.BundleUtils;
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
import com.applovin.mediation.adapters.pubmatic.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.pubmatic.sdk.common.OpenWrapSDK;
import com.pubmatic.sdk.common.OpenWrapSDKConfig;
import com.pubmatic.sdk.common.OpenWrapSDKInitializer;
import com.pubmatic.sdk.common.POBAdFormat;
import com.pubmatic.sdk.common.POBAdSize;
import com.pubmatic.sdk.common.POBError;
import com.pubmatic.sdk.openwrap.banner.POBBannerView;
import com.pubmatic.sdk.openwrap.core.POBReward;
import com.pubmatic.sdk.openwrap.core.signal.POBBiddingHost;
import com.pubmatic.sdk.openwrap.core.signal.POBSignalConfig;
import com.pubmatic.sdk.openwrap.core.signal.POBSignalGenerator;
import com.pubmatic.sdk.openwrap.interstitial.POBInterstitial;
import com.pubmatic.sdk.rewardedad.POBRewardedAd;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PubMaticMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private POBInterstitial interstitialAd;
    private POBRewardedAd   rewardedAd;
    private POBBannerView   adView;

    public PubMaticMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final String publisherId = getPublisherId( parameters );
            final int profileId = getProfileId( parameters );

            log( "Initializing PubMatic SDK with publisherId: " + publisherId + ", profileId: " + profileId + "..." );

            final OpenWrapSDKConfig config = new OpenWrapSDKConfig.Builder( publisherId, Collections.singletonList( profileId ) ).build();

            OpenWrapSDK.initialize( getApplicationContext(), config, new OpenWrapSDKInitializer.Listener()
            {
                @Override
                public void onSuccess()
                {
                    log( "PubMatic SDK initialized" );
                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }

                @Override
                public void onFailure(final POBError pobError)
                {
                    log( "PubMatic SDK failed to initialize with error: " + pobError );
                    status = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( status, pobError.getErrorMessage() );
                }
            } );
        }
        else
        {
            log( "PubMatic SDK already initializing" );
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return OpenWrapSDK.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateAdSettings( parameters );

        final POBAdFormat adFormat = toPubMaticAdFormat( parameters.getAdFormat() );
        final POBSignalConfig config = new POBSignalConfig.Builder( adFormat ).build();
        final String bidToken = POBSignalGenerator.generateSignal( getApplicationContext(), POBBiddingHost.ALMAX, config );

        callback.onSignalCollected( bidToken );
    }

    @Override
    @Nullable
    public Boolean shouldLoadAdsOnUiThread(final MaxAdFormat adFormat)
    {
        // PubMatic requires banner and interstitial ads to be loaded on UI thread.
        return true;
    }

    @Override
    @Nullable
    public Boolean shouldShowAdsOnUiThread(final MaxAdFormat adFormat)
    {
        // PubMatic requires interstitial ads to be shown on UI thread.
        return true;
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

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String publisherId = getPublisherId( parameters );
        final int profileId = getProfileId( parameters );
        final String adUnitId = getAdUnitId( parameters );
        final String bidResponse = parameters.getBidResponse();

        log( "Loading interstitial ad: " + adUnitId + "..." );

        updateAdSettings( parameters );

        interstitialAd = new POBInterstitial( getApplicationContext(), publisherId, profileId, adUnitId );
        interstitialAd.setListener( new InterstitialListener( listener ) );
        interstitialAd.loadAd( bidResponse );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String adUnitId = getAdUnitId( parameters );
        log( "Showing interstitial ad: " + adUnitId + "..." );

        if ( interstitialAd == null )
        {
            log( "Interstitial ad failed to load - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Interstitial ad not ready" ) );
            return;
        }

        interstitialAd.show();
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String publisherId = getPublisherId( parameters );
        final int profileId = getProfileId( parameters );
        final String adUnitId = getAdUnitId( parameters );
        final String bidResponse = parameters.getBidResponse();

        log( "Loading rewarded ad: " + adUnitId + "..." );

        updateAdSettings( parameters );

        rewardedAd = POBRewardedAd.getRewardedAd( getApplicationContext(), publisherId, profileId, adUnitId );
        rewardedAd.setListener( new RewardedListener( listener ) );
        rewardedAd.loadAd( bidResponse );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String adUnitId = getAdUnitId( parameters );
        log( "Showing rewarded ad: " + adUnitId + "..." );

        if ( rewardedAd == null )
        {
            log( "Rewarded ad failed to load - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Rewarded ad not ready" ) );
            return;
        }

        configureReward( parameters );
        rewardedAd.show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String publisherId = getPublisherId( parameters );
        final int profileId = getProfileId( parameters );
        final String adUnitId = getAdUnitId( parameters );
        final POBAdSize adSize = toPubMaticAdSize( adFormat );
        final String bidResponse = parameters.getBidResponse();

        log( "Loading " + adFormat.getLabel() + " ad: " + adUnitId + "..." );

        updateAdSettings( parameters );

        adView = new POBBannerView( getApplicationContext(), publisherId, profileId, adUnitId, adSize );
        adView.setListener( new AdViewListener( listener ) );
        adView.loadAd( bidResponse );
        adView.pauseAutoRefresh();
    }

    //endregion

    //region Helpers

    private void updateAdSettings(final MaxAdapterParameters parameters)
    {
        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            OpenWrapSDK.setCoppa( isAgeRestrictedUser );
        }
    }

    private static String getPublisherId(MaxAdapterParameters parameters)
    {
        return BundleUtils.getString( "publisher_id", parameters.getServerParameters() );
    }

    private static int getProfileId(MaxAdapterParameters parameters)
    {
        return BundleUtils.getInt( "profile_id", parameters.getServerParameters() );
    }

    private static String getAdUnitId(MaxAdapterResponseParameters parameters)
    {
        return parameters.getThirdPartyAdPlacementId();
    }

    private static POBAdSize toPubMaticAdSize(MaxAdFormat maxAdFormat)
    {
        if ( maxAdFormat == MaxAdFormat.BANNER )
        {
            return POBAdSize.BANNER_SIZE_320x50;
        }
        else if ( maxAdFormat == MaxAdFormat.LEADER )
        {
            return POBAdSize.BANNER_SIZE_728x90;
        }
        else if ( maxAdFormat == MaxAdFormat.MREC )
        {
            return POBAdSize.BANNER_SIZE_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + maxAdFormat );
        }
    }

    /**
     * Translates a MaxAdFormat to a POBAdFormat. Returns null if ad format not supported by PubMatic.
     */
    private static POBAdFormat toPubMaticAdFormat(MaxAdFormat maxAdFormat)
    {
        if ( maxAdFormat == MaxAdFormat.BANNER )
        {
            return POBAdFormat.BANNER;
        }
        else if ( maxAdFormat == MaxAdFormat.LEADER )
        {
            return POBAdFormat.BANNER;
        }
        else if ( maxAdFormat == MaxAdFormat.MREC )
        {
            return POBAdFormat.MREC;
        }
        else if ( maxAdFormat == MaxAdFormat.INTERSTITIAL )
        {
            return POBAdFormat.INTERSTITIAL;
        }
        else if ( maxAdFormat == MaxAdFormat.REWARDED )
        {
            return POBAdFormat.REWARDEDAD;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + maxAdFormat );
        }
    }

    /**
     * Translates a POBError to a MaxAdapterError.
     */
    private static MaxAdapterError toMaxError(POBError error)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( error.getErrorCode() )
        {
            case POBError.AD_REQUEST_NOT_ALLOWED:
            case POBError.INVALID_REQUEST:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case POBError.NO_ADS_AVAILABLE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case POBError.NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case POBError.SERVER_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case POBError.TIMEOUT_ERROR:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case POBError.INTERNAL_ERROR:
            case POBError.INVALID_RESPONSE:
            case POBError.REQUEST_CANCELLED:
            case POBError.OPENWRAP_SIGNALING_ERROR:
            case POBError.CLIENT_SIDE_AUCTION_LOST:
            case POBError.AD_ALREADY_SHOWN:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case POBError.RENDER_ERROR:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
            case POBError.AD_EXPIRED:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case POBError.AD_NOT_READY:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case POBError.INVALID_CONFIG:
            case POBError.INVALID_REWARD_SELECTED:
            case POBError.REWARD_NOT_SELECTED:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
        }

        return new MaxAdapterError( adapterError, error.getErrorCode(), error.getErrorMessage() );
    }

    //endregion

    //region Listeners

    private class InterstitialListener
            extends POBInterstitial.POBInterstitialListener
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdReceived(final POBInterstitial ad)
        {
            log( "Interstitial received" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdFailedToLoad(final POBInterstitial ad, final POBError error)
        {
            MaxAdapterError adapterError = toMaxError( error );
            log( "Interstitial failed to load with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression(final POBInterstitial ad)
        {
            // NOTE: This may fire on load, depending on the demand source
            log( "Interstitial impression" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdFailedToShow(final POBInterstitial ad, final POBError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Interstitial failed to show with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked(final POBInterstitial ad)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed(final POBInterstitial ad)
        {
            log( "Interstitial closed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedListener
            extends POBRewardedAd.POBRewardedAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward = false;

        public RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdReceived(final POBRewardedAd ad)
        {
            log( "Rewarded ad received" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdFailedToLoad(final POBRewardedAd ad, final POBError error)
        {
            MaxAdapterError adapterError = toMaxError( error );
            log( "Rewarded ad failed to load with error: " + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression(final POBRewardedAd ad)
        {
            // NOTE: This may fire on load, depending on the demand source
            log( "Rewarded ad impression" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdFailedToShow(final POBRewardedAd ad, final POBError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Rewarded ad failed to show with error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked(final POBRewardedAd ad)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onReceiveReward(@NonNull final POBRewardedAd rewardedAd, @NonNull final POBReward reward)
        {
            log( "Rewarded ad reward granted" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed(final POBRewardedAd ad)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad closed" );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            extends POBBannerView.POBBannerViewListener
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdReceived(final POBBannerView view)
        {
            log( "Ad view received" );
            listener.onAdViewAdLoaded( view );
        }

        @Override
        public void onAdFailed(final POBBannerView view, final POBError error)
        {
            MaxAdapterError adapterError = toMaxError( error );
            log( "Ad view failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression(final POBBannerView view)
        {
            // NOTE: This may fire on load, depending on the demand source
            log( "Ad view impression" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(final POBBannerView view)
        {
            log( "Ad view clicked" );
            listener.onAdViewAdClicked();
        }
    }

    //endregion
}
