package com.applovin.mediation.adapters;

import android.app.Activity;
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
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.amazonadmarketplace.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThreadDelayed;

/**
 * Created by Thomas So on December 9 2021
 * <p>
 * This is used for EXTERNAL Amazon Publisher Services integrations.
 */
public class AmazonAdMarketplaceMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxAdViewAdapter, MaxInterstitialAdapter
{
    // Ad loader object used for collecting signal in non-maiden requests
    private static final Map<MaxAdFormat, DTBAdLoader> adLoaders = Collections.synchronizedMap( new HashMap<MaxAdFormat, DTBAdLoader>() );

    // Contains mapping of encoded bid id -> mediation hints / bid info
    private static final Map<String, MediationHints> mediationHintsCache     = new HashMap<>();
    private static final Object                      mediationHintsCacheLock = new Object();

    private DTBAdView         adView;
    private DTBAdInterstitial interstitialAd;

    // Explicit default constructor declaration
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
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        final MaxAdFormat adFormat = parameters.getAdFormat();
        final Object adResponseObj = parameters.getLocalExtraParameters().get( "amazon_ad_response" );
        final Object adErrorObj = parameters.getLocalExtraParameters().get( "amazon_ad_error" );

        // There may be cases where pubs pass in info from integration (e.g. CCPA) directly into a _new_ ad loader - check (and update) for that
        DTBAdLoader adLoader = null;
        if ( adResponseObj instanceof DTBAdResponse )
        {
            adLoader = ( (DTBAdResponse) adResponseObj ).getAdLoader();
        }
        else if ( adErrorObj instanceof AdError )
        {
            adLoader = ( (AdError) adErrorObj ).getAdLoader();
        }

        DTBAdLoader currentAdLoader = adLoaders.get( adFormat );

        if ( adLoader != null )
        {
            // We already have this ad loader - load _new_ signal
            if ( adLoader == currentAdLoader )
            {
                loadSubsequentSignal( adLoader, parameters, adFormat, callback );
            }
            // If new ad loader - update for ad format and proceed to initial signal collection logic
            else
            {
                d( "New loader passed in: " + adLoader + ", replacing current ad loader: " + currentAdLoader );

                adLoaders.put( adFormat, adLoader );

                if ( adResponseObj instanceof DTBAdResponse )
                {
                    processAdResponse( parameters, (DTBAdResponse) adResponseObj, callback );
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
        d( "Found existing ad loader for format: " + adFormat );

        adLoader.loadAd( new DTBAdCallback()
        {
            @Override
            public void onSuccess(final DTBAdResponse dtbAdResponse)
            {
                // Store ad loader for future ad refresh token collection
                adLoaders.put( adFormat, dtbAdResponse.getAdLoader() );

                processAdResponse( parameters, dtbAdResponse, callback );
            }

            @Override
            public void onFailure(final AdError adError)
            {
                // Store ad loader for future ad refresh token collection
                adLoaders.put( adFormat, adError.getAdLoader() );

                failSignalCollection( adError, callback );
            }
        } );
    }

    private void processAdResponse(final MaxAdapterSignalCollectionParameters parameters,
                                   final DTBAdResponse adResponse,
                                   final MaxSignalCollectionListener callback)
    {
        d( "Processing ad response..." );

        final String encodedBidId = SDKUtilities.getPricePoint( adResponse );
        if ( AppLovinSdkUtils.isValidString( encodedBidId ) )
        {
            final MediationHints mediationHints = new MediationHints( SDKUtilities.getBidInfo( adResponse ) );

            synchronized ( mediationHintsCacheLock )
            {
                // Store mediation hints for the actual ad request
                mediationHintsCache.put( encodedBidId, mediationHints );
            }

            // In the case that Amazon loses the auction - clean up the mediation hints
            long mediationHintsCacheCleanupDelaySec = parameters.getServerParameters().getLong( "mediation_hints_cleanup_delay_sec",
                                                                                                TimeUnit.MINUTES.toSeconds( 5 ) );
            final long mediationHintsCacheCleanupDelayMillis = TimeUnit.SECONDS.toMillis( mediationHintsCacheCleanupDelaySec );
            if ( mediationHintsCacheCleanupDelayMillis > 0 )
            {
                runOnUiThreadDelayed( new CleanupMediationHintsTask( encodedBidId, mediationHints ), mediationHintsCacheCleanupDelayMillis );
            }

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
    public void loadAdViewAd(MaxAdapterResponseParameters parameters, MaxAdFormat adFormat, Activity activity, final MaxAdViewAdapterListener listener)
    {
        String encodedBidId = parameters.getServerParameters().getString( "encoded_bid_id" );
        d( "Loading " + adFormat.getLabel() + " ad view ad for encoded bid id: " + encodedBidId + "..." );

        if ( TextUtils.isEmpty( encodedBidId ) )
        {
            listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            return;
        }

        MediationHints mediationHints;
        synchronized ( mediationHintsCacheLock )
        {
            mediationHints = mediationHintsCache.get( encodedBidId );
            mediationHintsCache.remove( encodedBidId );
        }

        // Paranoia
        if ( mediationHints != null )
        {
            adView = new DTBAdView( activity, new AdViewListener( listener ) );
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

        MediationHints mediationHints;
        synchronized ( mediationHintsCacheLock )
        {
            mediationHints = mediationHintsCache.get( encodedBidId );
            mediationHintsCache.remove( encodedBidId );
        }

        // Paranoia
        if ( mediationHints != null )
        {
            interstitialAd = new DTBAdInterstitial( activity, new InterstitialListener( listener ) );
            interstitialAd.fetchAd( mediationHints.value );
        }
        else
        {
            e( "Unable to find mediation hints" );
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
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
        }
    }

    private class AdViewListener
            implements DTBAdBannerListener
    {
        private final MaxAdViewAdapterListener listener;

        private AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final View view)
        {
            d( "AdView ad loaded" );
            listener.onAdViewAdLoaded( view );
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
        final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final View view)
        {
            d( "Interstitial loaded" );
            listener.onInterstitialAdLoaded();
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

        @Override
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
        private final String         encodedBidId;
        private final MediationHints mediationHints;

        private CleanupMediationHintsTask(final String encodedBidId, final MediationHints mediationHints)
        {
            this.encodedBidId = encodedBidId;
            this.mediationHints = mediationHints;
        }

        @Override
        public void run()
        {
            synchronized ( mediationHintsCacheLock )
            {
                // Check if this is the same mediation hints / bid info as when the cleanup was scheduled
                MediationHints currentMediationHints = mediationHintsCache.get( encodedBidId );
                if ( currentMediationHints != null && currentMediationHints.id.equals( mediationHints.id ) )
                {
                    mediationHintsCache.remove( encodedBidId );
                }
            }
        }
    }
}
