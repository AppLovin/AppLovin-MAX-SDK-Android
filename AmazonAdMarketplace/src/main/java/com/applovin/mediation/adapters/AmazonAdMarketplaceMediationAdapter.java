package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.amazon.aps.ads.Aps;
import com.amazon.aps.ads.ApsAd;
import com.amazon.aps.ads.ApsAdController;
import com.amazon.aps.ads.ApsAdError;
import com.amazon.aps.ads.ApsAdNetworkInfo;
import com.amazon.aps.ads.ApsAdRequest;
import com.amazon.aps.ads.ApsConstants;
import com.amazon.aps.ads.listeners.ApsAdListener;
import com.amazon.aps.ads.listeners.ApsAdRequestListener;
import com.amazon.aps.ads.model.ApsAdNetwork;
import com.amazon.aps.shared.APSAnalytics;
import com.amazon.aps.shared.ApsMetrics;
import com.amazon.aps.shared.analytics.APSEventSeverity;
import com.amazon.aps.shared.analytics.APSEventType;
import com.amazon.aps.shared.metrics.ApsMetricsPerfEventModelBuilder;
import com.amazon.aps.shared.metrics.model.ApsMetricsResult;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.DTBAdLoader;
import com.amazon.device.ads.DTBAdRequest;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdSize;
import com.amazon.device.ads.SDKUtilities;
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
import com.applovin.mediation.adapters.amazonadmarketplace.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThreadDelayed;

/**
 * Created by Thomas So on December 9 2021
 * <p>
 * This is used for EXTERNAL Amazon Publisher Services integrations.
 */
public class AmazonAdMarketplaceMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter
{
    // Ad loader object used for collecting signal in non-maiden requests
    private static final Map<MaxAdFormat, DTBAdLoader> adLoaders = Collections.synchronizedMap( new HashMap<MaxAdFormat, DTBAdLoader>() );

    // NOTE: Will remove for more space-efficient implementation
    private static final Set<Integer> usedAdLoaders = Collections.synchronizedSet( new HashSet<>() );

    // Contains mapping of encoded (bid id)_(ad format) -> mediation hints / bid info
    private static final Map<String, MediationHints> mediationHintsCache     = new HashMap<>();
    private static final Object                      mediationHintsCacheLock = new Object();

    // Contains mapping of ad format -> amazon hashed bidder identifier / amznp
    private static final Map<MaxAdFormat, String> hashedBidderIds     = new HashMap<>();
    private static final Object                   hashedBidderIdsLock = new Object();

    private ApsAdController adViewController;
    private ApsAdController interstitialAdController;
    private ApsAdController rewardedAdController;

    public AmazonAdMarketplaceMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        // NOTE: Amazon wants publishers to initialize their SDK alongside MAX
        if ( parameters.isTesting() )
        {
            Aps.setTestingMode( true );
            Aps.enableLogging( true );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return Aps.getSdkVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        maybeCleanupAdView( adViewController );
        adViewController = null;

        maybeCleanupAdView( interstitialAdController );
        interstitialAdController = null;

        maybeCleanupAdView( rewardedAdController );
        rewardedAdController = null;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        long startTime = System.currentTimeMillis();
        ApsMetrics.Companion.setAdapterVersion( "MAX" + getAdapterVersion() );
        ApsMetricsResult metricsResult = ApsMetricsResult.Success;
        String apsBidId = null;
        String corrId = UUID.randomUUID().toString();

        final MaxAdFormat adFormat = parameters.getAdFormat();
        final Object adResponseObj = parameters.getLocalExtraParameters().get( ApsConstants.AMAZON_SUCCESS_RESPONSE );
        final Object adErrorObj = parameters.getLocalExtraParameters().get( ApsConstants.AMAZON_ERROR_RESPONSE );

        // There may be cases where pubs pass in info from integration (e.g. CCPA) directly into a _new_ ad loader - check (and update) for that
        // There may also be cases where we have both a response and error object - for which one is stale - check for that
        DTBAdLoader adLoader = null;

        if ( adResponseObj instanceof ApsAd )
        {
            ApsAdRequest retrievedAdLoader = ( (ApsAd) adResponseObj ).getAdLoader();

            if ( !usedAdLoaders.contains( retrievedAdLoader.hashCode() ) )
            {
                d( "Using ad loader from ad response object: " + retrievedAdLoader );
                adLoader = retrievedAdLoader;
            }
            else
            {
                parameters.getLocalExtraParameters().remove( ApsConstants.AMAZON_SUCCESS_RESPONSE );
            }

            apsBidId = ( (ApsAd) adResponseObj ).getBidId();
        }
        else if ( adResponseObj instanceof DTBAdResponse )
        {
            // Backward compatibility with legacy APIs
            DTBAdLoader retrievedAdLoader = ( (DTBAdResponse) adResponseObj ).getAdLoader();

            if ( !usedAdLoaders.contains( retrievedAdLoader.hashCode() ) )
            {
                d( "Using ad loader from ad response object: " + retrievedAdLoader );
                adLoader = retrievedAdLoader;
            }
            else
            {
                parameters.getLocalExtraParameters().remove( ApsConstants.AMAZON_SUCCESS_RESPONSE );
            }

            apsBidId = ( (DTBAdResponse) adResponseObj ).getBidId();
        }

        if ( adErrorObj instanceof ApsAdError )
        {
            ApsAdRequest retrievedAdLoader = (ApsAdRequest) ( (ApsAdError) adErrorObj ).getAdLoader();
            corrId = retrievedAdLoader.getCorrelationId();

            if ( !usedAdLoaders.contains( retrievedAdLoader.hashCode() ) )
            {
                d( "Using ad loader from ad error object: " + retrievedAdLoader );
                adLoader = retrievedAdLoader;
            }
            else
            {
                parameters.getLocalExtraParameters().remove( ApsConstants.AMAZON_ERROR_RESPONSE );
            }
        }
        else if ( adErrorObj instanceof AdError )
        {
            // Backward compatibility with legacy APIs
            DTBAdLoader retrievedAdLoader = ( (AdError) adErrorObj ).getAdLoader();
            corrId = ( (DTBAdRequest) retrievedAdLoader ).getCorrelationId();

            if ( !usedAdLoaders.contains( retrievedAdLoader.hashCode() ) )
            {
                d( "Using ad loader from ad error object: " + retrievedAdLoader );
                adLoader = retrievedAdLoader;
            }
            else
            {
                parameters.getLocalExtraParameters().remove( "amazon_ad_error" );
            }
        }

        DTBAdLoader currentAdLoader = adLoaders.get( adFormat );

        if ( adLoader != null )
        {
            // We already have this ad loader - load _new_ signal
            if ( adLoader == currentAdLoader )
            {
                d( "Passed in ad loader same as current ad loader: " + currentAdLoader );
                loadSubsequentSignal( adLoader, corrId, parameters, adFormat, callback );
            }
            // If new ad loader - update for ad format and proceed to initial signal collection logic
            else
            {
                d( "New loader passed in for " + adFormat + ": " + adLoader + ", replacing current ad loader: " + currentAdLoader );

                adLoaders.put( adFormat, adLoader );
                usedAdLoaders.add( adLoader.hashCode() );

                if ( adResponseObj instanceof DTBAdResponse )
                {
                    DTBAdResponse adResponse = (DTBAdResponse) adResponseObj;

                    processAdResponse( parameters, adResponse, adFormat, callback );
                }
                else // AdError
                {
                    failSignalCollection( (AdError) adErrorObj, callback );
                }
            }
        }
        else
        {
            // Use cached ad loader
            if ( currentAdLoader != null )
            {
                d( "Using cached ad loader: " + currentAdLoader );
                loadSubsequentSignal( currentAdLoader, corrId, parameters, adFormat, callback );
            }
            // No ad loader passed in, and no ad loaders cached - fail signal collection
            else
            {
                failSignalCollection( "DTBAdResponse or AdError not passed in ad load API", callback );
            }
        }

        ApsMetrics.adapterEvent( apsBidId, new ApsMetricsPerfEventModelBuilder()
                .withAdapterStartTime( startTime )
                .withCorrelationId( corrId )
                .withAdapterEndTime( metricsResult, System.currentTimeMillis() )
                .withBidId( apsBidId ) );
    }

    private void loadSubsequentSignal(DTBAdLoader adLoader,
                                      final String corrId,
                                      final MaxAdapterSignalCollectionParameters parameters,
                                      final MaxAdFormat adFormat,
                                      final MaxSignalCollectionListener callback)
    {
        d( "Found existing ad loader (" + adLoader + ") for format: " + adFormat + " - loading..." );

        if ( !( adLoader instanceof ApsAdRequest ) )
        {
            // Convert adloader to ApsAdRequest
            adLoader = new ApsAdRequest( (DTBAdRequest) adLoader );
        }

        ApsAdRequest apsAdRequest = (ApsAdRequest) adLoader;
        apsAdRequest.setCorrelationId( corrId );

        if ( apsAdRequest.getAdNetworkInfo() == null )
        {
            apsAdRequest.setNetworkInfo( new ApsAdNetworkInfo( ApsAdNetwork.MAX ) );
            ApsMetrics.customEvent( "APPLOVIN_SET_NETWORK_EVENT", "AdNetwork Type : null", null );
        }
        else
        {
            String adNetworkName = apsAdRequest.getAdNetworkInfo().getAdNetworkName();
            if ( !ApsAdNetwork.MAX.toString().equalsIgnoreCase( adNetworkName ) )
            {
                apsAdRequest.setNetworkInfo( new ApsAdNetworkInfo( ApsAdNetwork.MAX ) );

                String customEventValue = "AdNetwork Type : mismatch . Network name set as " + adNetworkName + ", instead of " + ApsAdNetwork.MAX;
                ApsMetrics.customEvent( "APPLOVIN_SET_NETWORK_EVENT", customEventValue, null );
            }
        }

        apsAdRequest.loadAd( new ApsAdRequestListener()
        {
            @Override
            public void onSuccess(final ApsAd apsAd)
            {
                // Store ad loader for future ad refresh token collection
                adLoaders.put( adFormat, apsAd.getAdLoader() );

                usedAdLoaders.add( apsAd.getAdLoader().hashCode() );

                d( "Signal collected for ad loader: " + apsAd.getAdLoader() );

                processAdResponse( parameters, apsAd, adFormat, callback );
            }

            @Override
            public void onFailure(final ApsAdError apsAdError)
            {
                // Store ad requests for future ad refresh token collection
                if ( apsAdError.getAdLoader() != null )
                {
                    adLoaders.put( adFormat, apsAdError.getAdLoader() );
                    usedAdLoaders.add( apsAdError.getAdLoader().hashCode() );

                    d( "Signal failed to collect for ad loader: " + apsAdError.getAdLoader() );

                    failSignalCollection( apsAdError, callback );
                }
                else
                {
                    APSAnalytics.logEvent( APSEventSeverity.FATAL, APSEventType.EXCEPTION, "MAX - ApsAdError getAdLoader returns null" );
                }
            }
        } );
    }

    private void processAdResponse(final MaxAdapterSignalCollectionParameters parameters, final DTBAdResponse adResponse, final MaxAdFormat adFormat, final MaxSignalCollectionListener callback)
    {
        d( "Processing ad response..." );

        final String encodedBidId = SDKUtilities.getPricePoint( adResponse );
        if ( AppLovinSdkUtils.isValidString( encodedBidId ) )
        {
            final MediationHints mediationHints = new MediationHints( adResponse );
            final String mediationHintsCacheId = getMediationHintsCacheId( encodedBidId, adFormat );

            synchronized ( mediationHintsCacheLock )
            {
                // Store mediation hints for the actual ad request
                mediationHintsCache.put( mediationHintsCacheId, mediationHints );
            }

            // In the case that Amazon loses the auction - clean up the mediation hints
            long mediationHintsCacheCleanupDelaySec = parameters.getServerParameters().getLong( "mediation_hints_cleanup_delay_sec",
                                                                                                TimeUnit.MINUTES.toSeconds( 5 ) );
            final long mediationHintsCacheCleanupDelayMillis = TimeUnit.SECONDS.toMillis( mediationHintsCacheCleanupDelaySec );
            if ( mediationHintsCacheCleanupDelayMillis > 0 )
            {
                runOnUiThreadDelayed( new CleanupMediationHintsTask( mediationHintsCacheId, mediationHints ), mediationHintsCacheCleanupDelayMillis );
            }

            String hashedBidderId;
            if ( adFormat.isAdViewAd() )
            {
                hashedBidderId = String.valueOf( adResponse.getDefaultDisplayAdsRequestCustomParams().get( "amznp" ) );
            }
            else // Fullscreen ads except static interstitials. `getDefaultDisplayAdsRequestCustomParams()` for static interstitials
            {
                hashedBidderId = String.valueOf( adResponse.getDefaultVideoAdsRequestCustomParams().get( "amznp" ) );
            }
            setHashedBidderId( adFormat, hashedBidderId );

            d( "Successfully loaded encoded bid id: " + encodedBidId );

            callback.onSignalCollected( encodedBidId );
        }
        else
        {
            failSignalCollection( "Received empty bid id", callback );
        }
    }

    private void failSignalCollection(final AdError adError, final MaxSignalCollectionListener callback)
    {
        failSignalCollection( "Signal collection failed: " + adError.getCode() + " - " + adError.getMessage(), callback );
    }

    private void failSignalCollection(final String errorMessage, final MaxSignalCollectionListener callback)
    {
        e( errorMessage );
        callback.onSignalCollectionFailed( errorMessage );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String encodedBidId = parameters.getServerParameters().getString( "encoded_bid_id" );
        d( "Loading " + adFormat.getLabel() + " ad view ad for encoded bid id: " + encodedBidId + "..." );

        if ( TextUtils.isEmpty( encodedBidId ) )
        {
            listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            return;
        }

        MediationHints mediationHints;
        final String mediationHintsCacheId = getMediationHintsCacheId( encodedBidId, adFormat );

        synchronized ( mediationHintsCacheLock )
        {
            mediationHints = mediationHintsCache.get( mediationHintsCacheId );
            mediationHintsCache.remove( mediationHintsCacheId );
        }

        // Paranoia
        if ( mediationHints != null )
        {
            adViewController = new ApsAdController( getContext( activity ), new AdViewListener( adFormat, listener ) );
            if ( mediationHints.dtbAdResponse instanceof ApsAd )
            {
                adViewController.fetchAd( (ApsAd) mediationHints.dtbAdResponse );
            }
            else // DTBAdResponse
            {
                DTBAdSize dtbAdSize = ( mediationHints.dtbAdResponse ).getDTBAds().get( 0 );
                adViewController.fetchBannerAd( SDKUtilities.getBidInfo( mediationHints.dtbAdResponse ), dtbAdSize.getWidth(), dtbAdSize.getHeight() );
            }
        }
        else
        {
            e( "Unable to find mediation hints" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String encodedBidId = parameters.getServerParameters().getString( "encoded_bid_id" );
        d( "Loading interstitial ad for encoded bid id: " + encodedBidId + "..." );

        if ( TextUtils.isEmpty( encodedBidId ) )
        {
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            return;
        }

        if ( activity == null )
        {
            log( "Interstitial ad load failed: Activity is null" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );
            return;
        }

        interstitialAdController = new ApsAdController( activity, new InterstitialListener( listener ) );

        final String mediationHintsCacheId = getMediationHintsCacheId( encodedBidId, MaxAdFormat.INTERSTITIAL );
        final boolean success = loadFullscreenAd( mediationHintsCacheId, interstitialAdController );
        if ( !success )
        {
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAdController != null )
        {
            interstitialAdController.show();
        }
        else
        {
            e( "Interstitial ad is null" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.INVALID_LOAD_STATE.getCode(),
                                                                         MaxAdapterError.INVALID_LOAD_STATE.getMessage() ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String encodedBidId = parameters.getServerParameters().getString( "encoded_bid_id" );
        d( "Loading rewarded ad for encoded bid id: " + encodedBidId + "..." );

        if ( TextUtils.isEmpty( encodedBidId ) )
        {
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            return;
        }

        if ( activity == null )
        {
            log( "Rewarded ad load failed: Activity is null" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );
            return;
        }

        rewardedAdController = new ApsAdController( activity, new RewardedAdListener( listener ) );

        final String mediationHintsCacheId = getMediationHintsCacheId( encodedBidId, MaxAdFormat.REWARDED );
        final boolean success = loadFullscreenAd( mediationHintsCacheId, rewardedAdController );
        if ( !success )
        {
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAdController != null )
        {
            // Configure userReward from server.
            configureReward( parameters );

            rewardedAdController.show();
        }
        else
        {
            e( "Rewarded ad is null" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.INVALID_LOAD_STATE.getCode(),
                                                                     MaxAdapterError.INVALID_LOAD_STATE.getMessage() ) );
        }
    }

    //region Helper Methods

    private boolean loadFullscreenAd(final String mediationHintsCacheId, final ApsAdController apsAdController)
    {
        MediationHints mediationHints;
        synchronized ( mediationHintsCacheLock )
        {
            mediationHints = mediationHintsCache.get( mediationHintsCacheId );
            mediationHintsCache.remove( mediationHintsCacheId );
        }

        // Paranoia
        if ( mediationHints == null )
        {
            e( "Unable to find mediation hints" );
            return false;
        }

        apsAdController.fetchInterstitialAd( SDKUtilities.getBidInfo( mediationHints.dtbAdResponse ) );

        return true;
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private void setHashedBidderId(final MaxAdFormat adFormat, final String hashedBidderId)
    {
        synchronized ( hashedBidderIdsLock )
        {
            hashedBidderIds.put( adFormat, hashedBidderId );
        }
    }

    private Bundle createExtraInfo(final MaxAdFormat adFormat, String creativeId)
    {
        Bundle extraInfo = new Bundle( 2 );

        if ( AppLovinSdkUtils.isValidString( creativeId ) )
        {
            extraInfo.putString( "creative_id", creativeId );
        }

        synchronized ( hashedBidderIdsLock )
        {
            String hashedBidderId = hashedBidderIds.get( adFormat );
            if ( AppLovinSdkUtils.isValidString( hashedBidderId ) )
            {
                Bundle adValues = new Bundle( 1 );
                adValues.putString( "amazon_hashed_bidder_id", hashedBidderId );

                extraInfo.putBundle( "ad_values", adValues );
            }
        }

        return extraInfo;
    }

    private String getMediationHintsCacheId(final String encodedBidId, final MaxAdFormat adFormat)
    {
        // Treat banners and leaders as the same ad format
        String adFormatLabel = ( adFormat == MaxAdFormat.LEADER ) ? MaxAdFormat.BANNER.getLabel() : adFormat.getLabel();

        return encodedBidId + "_" + adFormatLabel;
    }

    private void maybeCleanupAdView(final ApsAdController apsAdController)
    {
        if ( apsAdController != null && apsAdController.getApsAdView() != null )
        {
            apsAdController.getApsAdView().cleanup();
        }
    }

    //endregion

    private class AdViewListener
            implements ApsAdListener
    {
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final ApsAd apsAd)
        {
            d( "AdView ad loaded" );

            Bundle extraInfo = createExtraInfo( adFormat, apsAd.getCrid() );
            listener.onAdViewAdLoaded( apsAd.getAdView(), extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final ApsAd apsAd)
        {
            e( "AdView failed to load" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onAdClicked(final ApsAd apsAd)
        {
            d( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onImpressionFired(final ApsAd apsAd)
        {
            d( "AdView impression fired" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdError(final ApsAd apsAd)
        {
            // Catch all error callback, display failure callback. Ex: when a webview crashes, something crashes on the JS side, or the video stops playing midway. Implementation is not complete in the SDK
            // Do not call `onAdViewAdDisplayFailed()` because `onAdError()` is sometimes fired when ad display is successful.
            e( "AdView error" );
        }

        @Override
        public void onAdOpen(final ApsAd apsAd)
        {
            d( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdClosed(final ApsAd apsAd)
        {
            d( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }
    }

    private class InterstitialListener
            implements ApsAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final ApsAd apsAd)
        {
            d( "Interstitial loaded" );

            Bundle extraInfo = createExtraInfo( MaxAdFormat.INTERSTITIAL, apsAd.getCrid() );
            listener.onInterstitialAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final ApsAd apsAd)
        {
            e( "Interstitial failed to load" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onImpressionFired(final ApsAd apsAd)
        {
            d( "Interstitial did fire impression" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdError(final ApsAd apsAd)
        {
            // Catch all error callback, display failure callback. Ex: when a webview crashes, something crashes on the JS side, or the video stops playing midway. Implementation is not complete in the SDK
            // Do not call `onInterstitialAdDisplayFailed()` because `onAdError()` is sometimes fired when ad display is successful.
            e( "Interstitial ad error" );
        }

        @Override
        public void onAdOpen(final ApsAd apsAd)
        {
            d( "Interstitial did open" );
        }

        @Override
        public void onAdClicked(final ApsAd apsAd)
        {
            d( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onVideoCompleted(final ApsAd apsAd)
        {
            d( "Interstitial video completed" );
        }

        @Override
        public void onAdClosed(final ApsAd apsAd)
        {
            d( "Interstitial closed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements ApsAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final ApsAd apsAd)
        {
            d( "Rewarded ad loaded" );

            Bundle extraInfo = createExtraInfo( MaxAdFormat.REWARDED, apsAd.getCrid() );
            listener.onRewardedAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final ApsAd apsAd)
        {
            e( "Rewarded ad failed to load" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onImpressionFired(final ApsAd apsAd)
        {
            d( "Rewarded ad did fire impression" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdError(final ApsAd apsAd)
        {
            // Catch all error callback, display failure callback. Ex: when a webview crashes, something crashes on the JS side, or the video stops playing midway. Implementation is not complete in the SDK
            // Do not call `onRewardedAdDisplayFailed()` because `onAdError()` is sometimes fired when ad display is successful.
            e( "Rewarded ad error" );
        }

        @Override
        public void onAdOpen(final ApsAd apsAd)
        {
            d( "Rewarded ad did open" );
        }

        @Override
        public void onAdClicked(final ApsAd apsAd)
        {
            d( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onVideoCompleted(final ApsAd apsAd)
        {
            d( "Rewarded ad video completed" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed(final ApsAd apsAd)
        {
            d( "Rewarded ad closed" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                d( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }
    }

    /**
     * Container object for holding mediation hints dict generated from Amazon's SDK and the timestamp it was geenrated at.
     */
    private static class MediationHints
    {
        /**
         * The ApsAd object generated from Amazon's SDK.
         */
        private final DTBAdResponse dtbAdResponse;
        /**
         * The unique identifier for this instance of the mediation hints.
         */
        private final String        id;

        private MediationHints(final DTBAdResponse dtbAdResponse)
        {
            this.id = UUID.randomUUID().toString().toLowerCase( Locale.US );
            this.dtbAdResponse = dtbAdResponse;
        }

        @Override
        public boolean equals(Object o)
        {
            if ( this == o ) return true;
            if ( !( o instanceof MediationHints ) ) return false;

            MediationHints mediationHints = (MediationHints) o;

            if ( id != null ? !id.equals( mediationHints.id ) : mediationHints.id != null )
                return false;
            return dtbAdResponse != null ? dtbAdResponse.equals( mediationHints.dtbAdResponse ) : mediationHints.dtbAdResponse == null;
        }

        @Override
        public int hashCode()
        {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + ( dtbAdResponse != null ? dtbAdResponse.hashCode() : 0 );
            return result;
        }

        @Override
        @NonNull
        public String toString()
        {
            return "MediationHints{" +
                    "id=" + id +
                    ", dtbAdResponse=" + dtbAdResponse +
                    '}';
        }
    }

    private static class CleanupMediationHintsTask
            implements Runnable
    {
        private final String         mediationHintsCacheId;
        private final MediationHints mediationHints;

        private CleanupMediationHintsTask(final String mediationHintsCacheId, final MediationHints mediationHints)
        {
            this.mediationHintsCacheId = mediationHintsCacheId;
            this.mediationHints = mediationHints;
        }

        @Override
        public void run()
        {
            synchronized ( mediationHintsCacheLock )
            {
                // Check if this is the same mediation hints / bid info as when the cleanup was scheduled
                MediationHints currentMediationHints = mediationHintsCache.get( mediationHintsCacheId );
                if ( currentMediationHints != null && currentMediationHints.id.equals( mediationHints.id ) )
                {
                    mediationHintsCache.remove( mediationHintsCacheId );
                }
            }
        }
    }
}