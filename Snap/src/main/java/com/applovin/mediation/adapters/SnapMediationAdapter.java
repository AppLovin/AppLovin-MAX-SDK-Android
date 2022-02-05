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
import com.applovin.mediation.adapters.snap.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.snap.adkit.external.AdKitAudienceAdsNetwork;
import com.snap.adkit.external.AdKitSlotType;
import com.snap.adkit.external.AudienceNetworkAdsApi;
import com.snap.adkit.external.BannerView;
import com.snap.adkit.external.LoadAdConfig;
import com.snap.adkit.external.LoadAdConfigBuilder;
import com.snap.adkit.external.NetworkInitSettings;
import com.snap.adkit.external.SnapAdClicked;
import com.snap.adkit.external.SnapAdDismissed;
import com.snap.adkit.external.SnapAdEventListener;
import com.snap.adkit.external.SnapAdImpressionHappened;
import com.snap.adkit.external.SnapAdInitFailed;
import com.snap.adkit.external.SnapAdInitSucceeded;
import com.snap.adkit.external.SnapAdKitEvent;
import com.snap.adkit.external.SnapAdKitSlot;
import com.snap.adkit.external.SnapAdLoadFailed;
import com.snap.adkit.external.SnapAdLoadSucceeded;
import com.snap.adkit.external.SnapAdRewardEarned;
import com.snap.adkit.external.SnapAdSize;
import com.snap.adkit.external.SnapAdVisible;
import com.snap.adkit.external.SnapBannerAdImpressionRecorded;

import java.util.concurrent.atomic.AtomicBoolean;

public class SnapMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final SnapRouter    ROUTER         = new SnapRouter();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean();

    private static AudienceNetworkAdsApi snapAdsApi;
    private static InitializationStatus  status;

    private String     slotId;
    private BannerView adView;

    public SnapMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        if ( snapAdsApi != null )
        {
            return snapAdsApi.getSdkVersion();
        }
        else
        {
            String adapterVersion = getAdapterVersion();
            return adapterVersion.substring( 0, adapterVersion.lastIndexOf( "." ) );
        }
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    //region MaxAdapter methods
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( IS_INITIALIZED.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            String appId = parameters.getServerParameters().getString( "app_id" );
            log( "Initializing Snap SDK and registering with app id " + appId + "..." );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            NetworkInitSettings initSettings = AdKitAudienceAdsNetwork.buildNetworkInitSettings( context )
                    .withAppId( appId )
                    .withTestModeEnabled( parameters.isTesting() )
                    .withSnapAdEventListener( ROUTER )
                    .build();

            snapAdsApi = AdKitAudienceAdsNetwork.init( initSettings );

            if ( snapAdsApi != null )
            {
                ROUTER.setOnCompletionListener( onCompletionListener );
            }
            else
            {
                String error = "Failed to initialize Snap Ads API object.";
                log( "Snap SDK initialization failed. " + error );

                status = InitializationStatus.INITIALIZED_FAILURE;

                onCompletionListener.onCompletion( status, error );
            }
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public void onDestroy()
    {
        ROUTER.removeAdapter( this, slotId );
        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }
    }
    //endregion

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        String signal = AdKitAudienceAdsNetwork.getAdsNetwork().requestBidToken();
        callback.onSignalCollected( signal );
    }

    //endregion

    //region MaxInterstitialAdapter methods
    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        if ( snapAdsApi == null )
        {
            log( "Snap Ads API is not initialized" );

            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad for slot id \"" + slotId + "\"..." );

        ROUTER.addInterstitialAdapter( this, listener, slotId );

        LoadAdConfig loadAdConfig = new LoadAdConfigBuilder()
                .withPublisherSlotId( slotId )
                .withBid( bidResponse )
                .build();
        snapAdsApi.loadInterstitial( loadAdConfig );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad for slot id: " + slotId + "..." );

        ROUTER.addShowingAdapter( this );

        snapAdsApi.playAd( new SnapAdKitSlot( slotId, AdKitSlotType.INTERSTITIAL ) );
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        if ( snapAdsApi == null )
        {
            log( "Snap Ads API is not initialized" );

            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad for slot id \"" + slotId + "\"..." );

        ROUTER.addRewardedAdapter( this, listener, slotId );

        LoadAdConfig loadAdConfig = new LoadAdConfigBuilder()
                .withPublisherSlotId( slotId )
                .withBid( bidResponse )
                .build();
        snapAdsApi.loadRewarded( loadAdConfig );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing Rewarded ad for slot id: " + slotId + "..." );

        ROUTER.addShowingAdapter( this );
        configureReward( parameters );

        snapAdsApi.playAd( new SnapAdKitSlot( slotId, AdKitSlotType.REWARDED ) );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        if ( snapAdsApi == null )
        {
            log( "Snap Ads API is not initialized" );

            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        slotId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + adFormat.getLabel() + " ad for slot id \"" + slotId + "\"..." );

        adView = new BannerView( activity );
        adView.setAdSize( toAdSize( adFormat ) );

        ROUTER.addAdViewAdapter( this, listener, slotId, adView );
        adView.setupListener( ROUTER );

        LoadAdConfig loadAdConfig = new LoadAdConfigBuilder()
                .withPublisherSlotId( slotId )
                .withBid( bidResponse )
                .build();
        adView.loadAd( loadAdConfig );
    }

    private SnapAdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER )
        {
            return SnapAdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return SnapAdSize.MEDIUM_RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    //endregion

    private static class SnapRouter
            extends MediationAdapterRouter
            implements SnapAdEventListener
    {
        private OnCompletionListener onCompletionListener;

        private boolean hasGrantedReward = false;

        //TODO: marked for deletion, pending SDK change.
        @Override
        void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener) { }

        @Override
        public void onEvent(final SnapAdKitEvent snapAdKitEvent, final String slotId)
        {
            if ( snapAdKitEvent instanceof SnapAdInitSucceeded )
            {
                log( "Snap SDK initialized" );

                status = InitializationStatus.INITIALIZED_SUCCESS;

                if ( onCompletionListener != null )
                {
                    onCompletionListener.onCompletion( status, null );
                    onCompletionListener = null;
                }
            }
            else if ( snapAdKitEvent instanceof SnapAdInitFailed )
            {
                String errorString = "unknown";
                final Throwable throwable = ( (SnapAdInitFailed) snapAdKitEvent ).getThrowable();
                if ( throwable != null )
                {
                    errorString = throwable.getLocalizedMessage();
                }

                log( "Snap SDK initialization failed due to error: " + errorString + "..." );

                status = InitializationStatus.INITIALIZED_FAILURE;

                if ( onCompletionListener != null )
                {
                    onCompletionListener.onCompletion( status, errorString );
                    onCompletionListener = null;
                }
            }
            else if ( snapAdKitEvent instanceof SnapAdLoadSucceeded )
            {
                log( "Ad loaded for slot id: " + slotId + "..." );
                onAdLoaded( slotId );
            }
            else if ( snapAdKitEvent instanceof SnapAdLoadFailed )
            {
                String errorString = "unknown";
                MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
                final Throwable throwable = ( (SnapAdLoadFailed) snapAdKitEvent ).getThrowable();
                if ( throwable != null )
                {
                    errorString = throwable.getMessage();

                    if ( "No Fill".equalsIgnoreCase( errorString ) )
                    {
                        adapterError = MaxAdapterError.NO_FILL;
                    }
                    else
                    {
                        adapterError = new MaxAdapterError( MaxAdapterError.UNSPECIFIED, 0, errorString );
                    }
                }

                log( "Ad load failed for slot id: " + slotId + "with error: " + errorString + "..." );
                onAdLoadFailed( slotId, adapterError );
            }
            else if ( snapAdKitEvent instanceof SnapAdVisible )
            {
                log( "Ad shown for slot id: " + slotId + "..." );
            }
            else if ( snapAdKitEvent instanceof SnapAdImpressionHappened )
            {
                log( "Ad logging impression for slot id: " + slotId + "..." );
                onAdDisplayed( slotId );
            }
            else if ( snapAdKitEvent instanceof SnapAdClicked )
            {
                log( "Ad clicked for slot id: " + slotId + "..." );
                onAdClicked( slotId );
            }
            else if ( snapAdKitEvent instanceof SnapAdDismissed )
            {
                log( "Ad dismissed for slot id: " + slotId + "..." );

                if ( hasGrantedReward || shouldAlwaysRewardUser( slotId ) )
                {
                    MaxReward reward = getReward( slotId );

                    log( "Rewarded ad user with reward: " + reward + " for slot id: " + slotId );
                    onUserRewarded( slotId, reward );

                    // clear hasGrantedReward
                    hasGrantedReward = false;
                }

                onAdHidden( slotId );
            }
            else if ( snapAdKitEvent instanceof SnapAdRewardEarned )
            {
                log( "Rewarded ad completed for slot id: " + slotId + "..." );
                onRewardedAdVideoCompleted( slotId );

                hasGrantedReward = true;
            }
            else if ( snapAdKitEvent instanceof SnapBannerAdImpressionRecorded )
            {
                // Note: SnapAdImpressionHappened event will not be sent for banners. Extra impressions will not be recorded.
                log( "Banner ad impression recorded for slot id: " + slotId + "..." );
                onAdDisplayed( slotId );
            }
            else
            {
                log( "Received unknown event for slot id: " + slotId + " from Ad Kit: " + snapAdKitEvent.toString() + "..." );
            }
        }

        public void setOnCompletionListener(final OnCompletionListener onCompletionListener)
        {
            this.onCompletionListener = onCompletionListener;
        }
    }
}
