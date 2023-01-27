package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.DTBAdBannerListener;
import com.amazon.device.ads.DTBAdCallback;
import com.amazon.device.ads.DTBAdInterstitial;
import com.amazon.device.ads.DTBAdInterstitialListener;
import com.amazon.device.ads.DTBAdLoader;
import com.amazon.device.ads.DTBAdResponse;
import com.amazon.device.ads.DTBAdView;
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
    private static final Set<Integer> usedAdLoaders = new HashSet<>();

    // Contains mapping of encoded (bid id)_(ad format) -> mediation hints / bid info
    private static final Map<String, MediationHints> mediationHintsCache     = new HashMap<>();
    private static final Object                      mediationHintsCacheLock = new Object();

    // Contains mapping of ad format -> crid
    private static final Map<MaxAdFormat, String> creativeIds     = new HashMap<>();
    private static final Object                   creativeIdsLock = new Object();

    // Contains mapping of ad format -> amazon hashed bidder identifier / amznp
    private static final Map<MaxAdFormat, String> hashedBidderIds     = new HashMap<>();
    private static final Object                   hashedBidderIdsLock = new Object();

    private DTBAdView         adView;
    private DTBAdInterstitial interstitialAd;
    private DTBAdInterstitial rewardedAd;

    public AmazonAdMarketplaceMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        // NOTE: Amazon wants publishers to initialize their SDK alongside MAX
        if ( parameters.isTesting() )
        {
            AdRegistration.enableTesting( true );
            AdRegistration.enableLogging( true );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return AdRegistration.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        adView = null;
        interstitialAd = null;
        rewardedAd = null;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        final MaxAdFormat adFormat = parameters.getAdFormat();
        final Object adResponseObj = parameters.getLocalExtraParameters().get( "amazon_ad_response" );
        final Object adErrorObj = parameters.getLocalExtraParameters().get( "amazon_ad_error" );

        // There may be cases where pubs pass in info from integration (e.g. CCPA) directly into a _new_ ad loader - check (and update) for that
        // There may also be cases where we have both a response and error object - for which one is stale - check for that
        DTBAdLoader adLoader = null;

        if ( adResponseObj instanceof DTBAdResponse )
        {
            DTBAdLoader retrievedAdLoader = ( (DTBAdResponse) adResponseObj ).getAdLoader();
            if ( !usedAdLoaders.contains( retrievedAdLoader.hashCode() ) )
            {
                d( "Using ad loader from ad response object: " + retrievedAdLoader );
                adLoader = retrievedAdLoader;
            }
            else
            {
                parameters.getLocalExtraParameters().remove( "amazon_ad_response" );
            }
        }

        if ( adErrorObj instanceof AdError )
        {
            DTBAdLoader retrievedAdLoader = ( (AdError) adErrorObj ).getAdLoader();
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
                loadSubsequentSignal( adLoader, parameters, adFormat, callback );
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
                loadSubsequentSignal( currentAdLoader, parameters, adFormat, callback );
            }
            // No ad loader passed in, and no ad loaders cached - fail signal collection
            else
            {
                failSignalCollection( "DTBAdResponse or AdError not passed in ad load API", callback );
            }
        }
    }

    private void loadSubsequentSignal(final DTBAdLoader adLoader,
                                      final MaxAdapterSignalCollectionParameters parameters,
                                      final MaxAdFormat adFormat,
                                      final MaxSignalCollectionListener callback)
    {
        d( "Found existing ad loader (" + adLoader + ") for format: " + adFormat + " - loading..." );

        adLoader.loadAd( new DTBAdCallback()
        {
            @Override
            public void onSuccess(@NonNull final DTBAdResponse dtbAdResponse)
            {
                // Store ad loader for future ad refresh token collection
                adLoaders.put( adFormat, dtbAdResponse.getAdLoader() );

                usedAdLoaders.add( dtbAdResponse.getAdLoader().hashCode() );

                d( "Signal collected for ad loader: " + dtbAdResponse.getAdLoader() );

                processAdResponse( parameters, dtbAdResponse, adFormat, callback );
            }

            @Override
            public void onFailure(@NonNull final AdError adError)
            {
                // Store ad loader for future ad refresh token collection
                adLoaders.put( adFormat, adError.getAdLoader() );

                usedAdLoaders.add( adError.getAdLoader().hashCode() );

                d( "Signal failed to collect for ad loader: " + adError.getAdLoader() );

                failSignalCollection( adError, callback );
            }
        } );
    }

    private void processAdResponse(final MaxAdapterSignalCollectionParameters parameters, final DTBAdResponse adResponse, final MaxAdFormat adFormat, final MaxSignalCollectionListener callback)
    {
        d( "Processing ad response..." );

        final String encodedBidId = SDKUtilities.getPricePoint( adResponse );
        if ( AppLovinSdkUtils.isValidString( encodedBidId ) )
        {
            final MediationHints mediationHints = new MediationHints( SDKUtilities.getBidInfo( adResponse ) );
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
            setCreativeId( adFormat, adResponse.getCrid() );

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
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
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
            adView = new DTBAdView( getContext( activity ), new AdViewListener( adFormat, listener ) );
            adView.fetchAd( mediationHints.value );
        }
        else
        {
            e( "Unable to find mediation hints" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String encodedBidId = parameters.getServerParameters().getString( "encoded_bid_id" );
        d( "Loading interstitial ad for encoded bid id: " + encodedBidId + "..." );

        if ( TextUtils.isEmpty( encodedBidId ) )
        {
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            return;
        }

        interstitialAd = new DTBAdInterstitial( activity, new InterstitialListener( listener ) );

        final String mediationHintsCacheId = getMediationHintsCacheId( encodedBidId, MaxAdFormat.INTERSTITIAL );
        final boolean success = loadFullscreenAd( mediationHintsCacheId, interstitialAd );
        if ( !success )
        {
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAd != null )
        {
            interstitialAd.show();
        }
        else
        {
            e( "Interstitial ad is null" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad is null" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String encodedBidId = parameters.getServerParameters().getString( "encoded_bid_id" );
        d( "Loading rewarded ad for encoded bid id: " + encodedBidId + "..." );

        if ( TextUtils.isEmpty( encodedBidId ) )
        {
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            return;
        }

        rewardedAd = new DTBAdInterstitial( activity, new RewardedAdListener( listener ) );

        final String mediationHintsCacheId = getMediationHintsCacheId( encodedBidId, MaxAdFormat.REWARDED );
        final boolean success = loadFullscreenAd( mediationHintsCacheId, rewardedAd );
        if ( !success )
        {
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAd != null )
        {
            // Configure userReward from server.
            configureReward( parameters );

            rewardedAd.show();
        }
        else
        {
            e( "Rewarded ad is null" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad is null" ) );
        }
    }

    //region Helper Methods

    private boolean loadFullscreenAd(final String mediationHintsCacheId, final DTBAdInterstitial interstitial)
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

        interstitial.fetchAd( mediationHints.value );

        return true;
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private void setCreativeId(final MaxAdFormat adFormat, final String creativeId)
    {
        synchronized ( creativeIdsLock )
        {
            creativeIds.put( adFormat, creativeId );
        }
    }

    private void setHashedBidderId(final MaxAdFormat adFormat, final String hashedBidderId)
    {
        synchronized ( hashedBidderIdsLock )
        {
            hashedBidderIds.put( adFormat, hashedBidderId );
        }
    }

    private Bundle createExtraInfo(final MaxAdFormat adFormat)
    {
        Bundle extraInfo = new Bundle( 2 );

        synchronized ( creativeIdsLock )
        {
            String creativeId = creativeIds.get( adFormat );
            if ( AppLovinSdkUtils.isValidString( creativeId ) )
            {
                extraInfo.putString( "creative_id", creativeId );
            }
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
        return encodedBidId + "_" + adFormat.getLabel();
    }

    //endregion

    private class AdViewListener
            implements DTBAdBannerListener
    {
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final View view)
        {
            d( "AdView ad loaded" );

            Bundle extraInfo = createExtraInfo( adFormat );
            listener.onAdViewAdLoaded( view, extraInfo );
        }

        @Override
        public void onAdFailed(final View view)
        {
            e( "AdView failed to load" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onAdClicked(final View view)
        {
            d( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onImpressionFired(final View view)
        {
            d( "AdView impression fired" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdOpen(final View view)
        {
            d( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdClosed(final View view)
        {
            d( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdLeftApplication(final View view)
        {
            d( "AdView left application" );
        }
    }

    private class InterstitialListener
            implements DTBAdInterstitialListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final View view)
        {
            d( "Interstitial loaded" );

            Bundle extraInfo = createExtraInfo( MaxAdFormat.INTERSTITIAL );
            listener.onInterstitialAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailed(final View view)
        {
            e( "Interstitial failed to load" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onImpressionFired(final View view)
        {
            d( "Interstitial did fire impression" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdOpen(final View view)
        {
            d( "Interstitial did open" );
        }

        @Override
        public void onAdClicked(final View view)
        {
            d( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onVideoCompleted(final View view)
        {
            d( "Interstitial video completed" );
        }

        @Override
        public void onAdClosed(final View view)
        {
            d( "Interstitial closed" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdLeftApplication(final View view)
        {
            d( "Interstitial will leave application" );
        }
    }

    private class RewardedAdListener
            implements DTBAdInterstitialListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final View view)
        {
            d( "Rewarded ad loaded" );

            Bundle extraInfo = createExtraInfo( MaxAdFormat.REWARDED );
            listener.onRewardedAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailed(final View view)
        {
            e( "Rewarded ad failed to load" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onImpressionFired(final View view)
        {
            d( "Rewarded ad did fire impression" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdOpen(final View view)
        {
            d( "Rewarded ad did open" );
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdClicked(final View view)
        {
            d( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onVideoCompleted(final View view)
        {
            d( "Rewarded ad video completed" );
            hasGrantedReward = true;
            listener.onRewardedAdVideoCompleted();
        }

        @Override
        public void onAdClosed(final View view)
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

        @Override
        public void onAdLeftApplication(final View view)
        {
            d( "Rewarded ad will leave application" );
        }
    }

    /**
     * Container object for holding mediation hints dict generated from Amazon's SDK and the timestamp it was geenrated at.
     */
    private static class MediationHints
    {
        /**
         * The bid info / mediation hints generated from Amazon's SDK.
         */
        private final String value;

        /**
         * The unique identifier for this instance of the mediation hints.
         */
        private final String id;

        private MediationHints(final String value)
        {
            this.id = UUID.randomUUID().toString().toLowerCase( Locale.US );
            this.value = value;
        }

        @Override
        public boolean equals(Object o)
        {
            if ( this == o ) return true;
            if ( !( o instanceof MediationHints ) ) return false;

            MediationHints mediationHints = (MediationHints) o;

            if ( id != null ? !id.equals( mediationHints.id ) : mediationHints.id != null )
                return false;
            return value != null ? value.equals( mediationHints.value ) : mediationHints.value == null;
        }

        @Override
        public int hashCode()
        {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + ( value != null ? value.hashCode() : 0 );
            return result;
        }

        @Override @NonNull
        public String toString()
        {
            return "MediationHints{" +
                    "id=" + id +
                    ", value=" + value +
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
