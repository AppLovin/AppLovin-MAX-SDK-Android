package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.smaato.sdk.banner.ad.AutoReloadInterval;
import com.smaato.sdk.banner.ad.BannerAdSize;
import com.smaato.sdk.banner.widget.BannerError;
import com.smaato.sdk.banner.widget.BannerView;
import com.smaato.sdk.core.Config;
import com.smaato.sdk.core.SmaatoSdk;
import com.smaato.sdk.core.ad.AdRequestParams;
import com.smaato.sdk.core.lifecycle.Lifecycling;
import com.smaato.sdk.core.log.LogLevel;
import com.smaato.sdk.iahb.InAppBid;
import com.smaato.sdk.iahb.InAppBiddingException;
import com.smaato.sdk.iahb.SmaatoSdkInAppBidding;
import com.smaato.sdk.interstitial.Interstitial;
import com.smaato.sdk.interstitial.InterstitialAd;
import com.smaato.sdk.interstitial.InterstitialError;
import com.smaato.sdk.interstitial.InterstitialRequestError;
import com.smaato.sdk.nativead.NativeAd;
import com.smaato.sdk.nativead.NativeAdAssets;
import com.smaato.sdk.nativead.NativeAdError;
import com.smaato.sdk.nativead.NativeAdRenderer;
import com.smaato.sdk.nativead.NativeAdRequest;
import com.smaato.sdk.rewarded.RewardedError;
import com.smaato.sdk.rewarded.RewardedInterstitial;
import com.smaato.sdk.rewarded.RewardedInterstitialAd;
import com.smaato.sdk.rewarded.RewardedRequestError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Christopher Cong on March 11 2019
 */
public class SmaatoMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter /* MaxNativeAdAdapter */
{
    private static final SmaatoMediationAdapterRouter ROUTER;
    private static final AtomicBoolean                INITIALIZED = new AtomicBoolean();

    // Used by the mediation adapter router
    private String placementId;

    // Ad Objects
    private BannerView             adView;
    private InterstitialAd         interstitialAd;
    private RewardedInterstitialAd rewardedAd;
    private NativeAdRenderer       nativeAdRenderer;

    static
    {
        ROUTER = (SmaatoMediationAdapterRouter) MediationAdapterRouter.getSharedInstance( SmaatoMediationAdapterRouter.class );
    }

    public SmaatoMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String pubId = parameters.getServerParameters().getString( "pub_id", "" );
            log( "Initializing Smaato SDK with publisher id: " + pubId + "..." );

            final Config config = Config.builder()
                    .setLogLevel( parameters.isTesting() ? LogLevel.DEBUG : LogLevel.ERROR )
                    .build();

            // NOTE: `getContext()` will always return an application context, so it is safe to cast.
            final Application application = (Application) getContext( activity );

            SmaatoSdk.init( application, config, pubId );

            // NOTE: This does not work atm
            updateLocationCollectionEnabled( parameters );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return SmaatoSdk.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return com.applovin.mediation.adapters.smaato.BuildConfig.VERSION_NAME;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateLocationCollectionEnabled( parameters );

        final String signal = SmaatoSdk.collectSignals( getContext( activity ) );
        callback.onSignalCollected( signal );
    }

    @Override
    public void onDestroy()
    {
        if ( adView != null )
        {
            adView.setEventListener( null );
            adView.destroy();
            adView = null;
        }

        interstitialAd = null;
        rewardedAd = null;
        nativeAdRenderer = null;

        ROUTER.removeAdapter( this, placementId );
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for placement: " + placementId + "..." );

        updateLocationCollectionEnabled( parameters );

        if ( isNative )
        {
            if ( activity == null )
            {
                log( "Native " + adFormat.getLabel() + " ad load failed: Activity is null" );

                final MaxAdapterError error = new MaxAdapterError( -5601, "Missing Activity" );
                listener.onAdViewAdLoadFailed( error );

                return;
            }

            final NativeAdRequest nativeAdRequest = createNativeAdRequest( placementId, bidResponse );
            if ( nativeAdRequest == null )
            {
                log( adFormat.getLabel() + " ad load failed: ad request null with invalid bid response" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

                return;
            }

            final NativeAdViewListener nativeListener = new NativeAdViewListener( parameters, adFormat, getContext( activity ), listener );
            NativeAd.loadAd( Lifecycling.of( activity ), nativeAdRequest, nativeListener );
        }
        else
        {
            adView = new BannerView( getContext( activity ) );
            adView.setAutoReloadInterval( AutoReloadInterval.DISABLED );
            adView.setEventListener( new AdViewListener( listener ) );

            if ( isBiddingAd )
            {
                final AdRequestParams adRequestParams = createBiddingAdRequestParams( bidResponse );
                if ( adRequestParams != null && adRequestParams.getUBUniqueId() != null ) // We must null check the ID
                {
                    adView.loadAd( placementId, toAdSize( adFormat ), adRequestParams );
                }
                else
                {
                    log( adFormat.getLabel() + " ad load failed: ad request null with invalid bid response" );
                    listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
                }
            }
            else
            {
                adView.loadAd( placementId, toAdSize( adFormat ) );
            }
        }
    }

    //endregion

    //region MaxInterstitialAdapter

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        updateLocationCollectionEnabled( parameters );

        ROUTER.addInterstitialAdapter( this, listener, placementId );

        final InterstitialAd loadedAd = ROUTER.getInterstitialAd( placementId );
        if ( loadedAd != null && loadedAd.isAvailableForPresentation() )
        {
            log( "Interstitial ad already loaded for placement: " + placementId + "..." );
            listener.onInterstitialAdLoaded();

            return;
        }

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            final AdRequestParams adRequestParams = createBiddingAdRequestParams( bidResponse );
            if ( adRequestParams != null && adRequestParams.getUBUniqueId() != null ) // We must null check the ID
            {
                Interstitial.loadAd( placementId, ROUTER, adRequestParams );
            }
            else
            {
                log( "Interstitial load failed: ad request null with invalid bid response" );
                listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            }
        }
        else
        {
            Interstitial.loadAd( placementId, ROUTER );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for placement: " + placementId + "..." );

        ROUTER.addShowingAdapter( this );

        interstitialAd = ROUTER.getInterstitialAd( placementId );
        if ( interstitialAd == null || !interstitialAd.isAvailableForPresentation() )
        {
            log( "Interstitial ad failed to load - ad not ready" );
            ROUTER.onAdDisplayFailed( placementId, new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Interstitial ad not ready" ) );
            return;
        }

        if ( activity == null )
        {
            log( "Interstitial ad display failed: Activity is null" );
            ROUTER.onAdDisplayFailed( placementId, MaxAdapterError.MISSING_ACTIVITY );
            return;
        }

        interstitialAd.showAd( activity );
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );

        updateLocationCollectionEnabled( parameters );

        ROUTER.addRewardedAdapter( this, listener, placementId );

        final RewardedInterstitialAd loadedAd = ROUTER.getRewardedAd( placementId );
        if ( loadedAd != null && loadedAd.isAvailableForPresentation() )
        {
            log( "Rewarded ad already loaded for placement: " + placementId + "..." );
            listener.onRewardedAdLoaded();

            return;
        }

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            final AdRequestParams adRequestParams = createBiddingAdRequestParams( bidResponse );
            if ( adRequestParams != null && adRequestParams.getUBUniqueId() != null ) // We must null check the ID
            {
                RewardedInterstitial.loadAd( placementId, ROUTER, adRequestParams );
            }
            else
            {
                log( "Rewarded ad load failed: ad request null with invalid bid response" );
                listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
            }
        }
        else
        {
            RewardedInterstitial.loadAd( placementId, ROUTER );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for placement: " + placementId + "..." );

        ROUTER.addShowingAdapter( this );

        rewardedAd = ROUTER.getRewardedAd( placementId );
        if ( rewardedAd != null && rewardedAd.isAvailableForPresentation() )
        {
            // Configure userReward from server.
            configureReward( parameters );

            rewardedAd.showAd();
        }
        else
        {
            log( "Rewarded ad not ready." );
            ROUTER.onAdDisplayFailed( placementId, new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, 0, "Rewarded ad not ready" ) );
        }
    }

    //endregion

    //region MaxNativeAdAdapter

    // @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        if ( activity == null )
        {
            log( "Native ad load failed: Activity is null" );

            final MaxAdapterError error = new MaxAdapterError( -5601, "Missing Activity" );
            listener.onNativeAdLoadFailed( error );

            return;
        }

        updateLocationCollectionEnabled( parameters );

        final NativeAdRequest nativeAdRequest = createNativeAdRequest( placementId, bidResponse );
        if ( nativeAdRequest == null )
        {
            log( "Native ad load failed: ad request null with invalid bid response" );
            listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

            return;
        }

        NativeAd.loadAd( Lifecycling.of( activity ), nativeAdRequest, new NativeAdListener( parameters, getContext( activity ), listener ) );
    }

    //endregion

    //region Helper Methods

    // TODO: Add local params support on init
    private void updateLocationCollectionEnabled(final MaxAdapterParameters parameters)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        final Object isLocationCollectionEnabledObj = localExtraParameters.get( "is_location_collection_enabled" );
        if ( isLocationCollectionEnabledObj instanceof Boolean )
        {
            log( "Setting location collection enabled: " + isLocationCollectionEnabledObj );
            // NOTE: According to docs - this is disabled by default
            SmaatoSdk.setGPSEnabled( (boolean) isLocationCollectionEnabledObj );
        }
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplication() : getApplicationContext();
    }

    private BannerAdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return BannerAdSize.XX_LARGE_320x50;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return BannerAdSize.MEDIUM_RECTANGLE_300x250;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return BannerAdSize.LEADERBOARD_728x90;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(final BannerError smaatoBannerError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( smaatoBannerError )
        {
            case NO_AD_AVAILABLE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case INVALID_REQUEST:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case INTERNAL_ERROR:
            case CACHE_LIMIT_REACHED:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case CREATIVE_RESOURCE_EXPIRED:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case AD_UNLOADED:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
        }

        return new MaxAdapterError( adapterError, smaatoBannerError.ordinal(), smaatoBannerError.name() );
    }

    private static MaxAdapterError toMaxError(final NativeAdError smaatoNativeError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( smaatoNativeError )
        {
            case NO_AD_AVAILABLE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case INVALID_REQUEST:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case INTERNAL_ERROR:
            case CACHE_LIMIT_REACHED:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError, smaatoNativeError.ordinal(), smaatoNativeError.name() );
    }

    private AdRequestParams createBiddingAdRequestParams(final String bidResponse)
    {
        final String token;
        try
        {
            final InAppBid inAppBid = InAppBid.create( bidResponse );
            token = SmaatoSdkInAppBidding.saveBid( inAppBid );
        }
        catch ( final InAppBiddingException exception )
        {
            log( "Error occurred in saving pre-bid: " + bidResponse, exception );

            return null;
        }

        return AdRequestParams.builder().setUBUniqueId( token ).build();
    }

    private NativeAdRequest createNativeAdRequest(final String placementId, final String bidResponse)
    {
        final NativeAdRequest.Builder adRequestBuilder = NativeAdRequest.builder()
                .adSpaceId( placementId )
                .shouldReturnUrlsForImageAssets( false );
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            final AdRequestParams adRequestParams = createBiddingAdRequestParams( bidResponse );
            if ( adRequestParams == null || adRequestParams.getUBUniqueId() == null ) return null;

            adRequestBuilder.uniqueUBId( adRequestParams.getUBUniqueId() );
        }

        return adRequestBuilder.build();
    }

    private static List<View> getClickableViews(final MaxNativeAd maxNativeAd, final MaxNativeAdView maxNativeAdView)
    {
        final List<View> clickableViews = new ArrayList<>( 6 );
        if ( AppLovinSdkUtils.isValidString( maxNativeAd.getTitle() ) && maxNativeAdView.getTitleTextView() != null )
        {
            clickableViews.add( maxNativeAdView.getTitleTextView() );
        }
        if ( AppLovinSdkUtils.isValidString( maxNativeAd.getAdvertiser() ) && maxNativeAdView.getAdvertiserTextView() != null )
        {
            clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
        }
        if ( AppLovinSdkUtils.isValidString( maxNativeAd.getBody() ) && maxNativeAdView.getBodyTextView() != null )
        {
            clickableViews.add( maxNativeAdView.getBodyTextView() );
        }
        if ( AppLovinSdkUtils.isValidString( maxNativeAd.getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
        {
            clickableViews.add( maxNativeAdView.getCallToActionButton() );
        }
        if ( maxNativeAd.getIcon() != null && maxNativeAdView.getIconImageView() != null )
        {
            clickableViews.add( maxNativeAdView.getIconImageView() );
        }
        if ( maxNativeAd.getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null )
        {
            clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );
        }

        return clickableViews;
    }

    //endregion

    //region Ad View Listener

    private class AdViewListener
            implements BannerView.EventListener
    {
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final BannerView bannerView)
        {
            log( "AdView loaded" );

            if ( !TextUtils.isEmpty( bannerView.getCreativeId() ) )
            {
                final Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", bannerView.getCreativeId() );

                listener.onAdViewAdLoaded( adView, extraInfo );
            }
            else
            {
                listener.onAdViewAdLoaded( adView );
            }
        }

        @Override
        public void onAdFailedToLoad(@NonNull final BannerView bannerView, @NonNull final BannerError bannerError)
        {
            log( "AdView load failed to load with error: " + bannerError );

            final MaxAdapterError error = toMaxError( bannerError );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onAdImpression(@NonNull final BannerView bannerView)
        {
            log( "AdView displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final BannerView bannerView)
        {
            log( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdTTLExpired(@NonNull final BannerView bannerView)
        {
            log( "AdView ad expired" );
        }
    }

    //endregion

    //region Native Ad View Listener

    private class NativeAdViewListener
            implements NativeAd.Listener
    {
        final String                   placementId;
        final Bundle                   serverParameters;
        final MaxAdFormat              adFormat;
        final Context                  context;
        final MaxAdViewAdapterListener listener;

        public NativeAdViewListener(final MaxAdapterResponseParameters parameters,
                                    final MaxAdFormat adFormat,
                                    final Context context,
                                    final MaxAdViewAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();
            this.adFormat = adFormat;
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final NativeAd nativeAd, @NonNull final NativeAdRenderer renderer)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded: " + placementId );

            // Save the renderer in order to register the native ad view later.
            nativeAdRenderer = renderer;

            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final NativeAdAssets assets = renderer.getAssets();

                    MaxNativeAd.MaxNativeAdImage iconMaxNativeAdImage = null;
                    if ( assets.icon() != null && assets.icon().drawable() != null )
                    {
                        iconMaxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( assets.icon().drawable() );
                    }

                    ImageView maxNativeAdMediaView = null;
                    if ( assets.images().size() > 0 )
                    {
                        NativeAdAssets.Image image = assets.images().get( 0 );
                        if ( image.drawable() != null )
                        {
                            maxNativeAdMediaView = new ImageView( context );
                            maxNativeAdMediaView.setImageDrawable( image.drawable() );
                        }
                    }

                    final String templateName = getValidTemplateName( BundleUtils.getString( "template", "", serverParameters ) );
                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( adFormat )
                            .setTitle( assets.title() )
                            .setAdvertiser( assets.sponsored() )
                            .setBody( assets.text() )
                            .setCallToAction( assets.cta() )
                            .setIcon( iconMaxNativeAdImage )
                            .setMediaView( maxNativeAdMediaView );
                    final MaxNativeAd maxNativeAd = new MaxSmaatoNativeAd( builder );
                    final MaxNativeAdView maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, context );

                    maxNativeAd.prepareForInteraction( getClickableViews( maxNativeAd, maxNativeAdView ), maxNativeAdView );

                    log( "Native " + adFormat.getLabel() + " ad fully loaded: " + placementId );
                    listener.onAdViewAdLoaded( maxNativeAdView );
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final NativeAd nativeAd, @NonNull final NativeAdError error)
        {
            MaxAdapterError adapterError = toMaxError( error );
            log( "Native " + adFormat.getLabel() + " ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpressed(@NonNull final NativeAd nativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad shown: " + placementId );
            listener.onAdViewAdDisplayed( null );
        }

        @Override
        public void onAdClicked(@NonNull final NativeAd nativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked: " + placementId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onTtlExpired(@NonNull final NativeAd nativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad expired" );
        }

        private String getValidTemplateName(final String templateName)
        {
            if ( AppLovinSdkUtils.isValidString( templateName ) )
            {
                if ( templateName.contains( "media" ) || templateName.contains( "leader" ) )
                {
                    return templateName;
                }
                else if ( templateName.contains( "vertical" ) )
                {
                    return ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                }
            }

            return "media_banner_template";
        }
    }

    //endregion

    //region Native Ad Listener

    private class NativeAdListener
            implements NativeAd.Listener
    {
        final String                     placementId;
        final Bundle                     serverParameters;
        final Context                    context;
        final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();
            this.context = context;

            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final NativeAd nativeAd, @NonNull final NativeAdRenderer renderer)
        {
            log( "Native ad loaded: " + placementId );

            // Save the renderer in order to register the native ad view later.
            nativeAdRenderer = renderer;

            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final NativeAdAssets assets = renderer.getAssets();
                    final String templateName = BundleUtils.getString( "template", "", serverParameters );
                    final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
                    if ( isTemplateAd && TextUtils.isEmpty( assets.title() ) )
                    {
                        e( "Native ad (" + nativeAd + ") does not have required assets." );
                        listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                        return;
                    }

                    MaxNativeAd.MaxNativeAdImage maxNativeAdIcon = null;
                    if ( assets.icon() != null && assets.icon().drawable() != null )
                    {
                        maxNativeAdIcon = new MaxNativeAd.MaxNativeAdImage( assets.icon().drawable() );
                    }

                    ImageView maxNativeAdMediaView = null;
                    MaxNativeAd.MaxNativeAdImage maxNativeMainImage = null;
                    if ( assets.images().size() > 0 )
                    {
                        NativeAdAssets.Image image = assets.images().get( 0 );
                        if ( image.drawable() != null )
                        {
                            maxNativeAdMediaView = new ImageView( context );
                            maxNativeAdMediaView.setImageDrawable( image.drawable() );
                            maxNativeMainImage = new MaxNativeAd.MaxNativeAdImage( image.drawable() );
                        }
                    }

                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( assets.title() )
                            .setAdvertiser( assets.sponsored() )
                            .setBody( assets.text() )
                            .setCallToAction( assets.cta() )
                            .setIcon( maxNativeAdIcon )
                            .setMediaView( maxNativeAdMediaView )
                            .setMainImage( maxNativeMainImage );

                    final MaxNativeAd maxNativeAd = new MaxSmaatoNativeAd( builder );

                    log( "Native ad fully loaded: " + placementId );
                    listener.onNativeAdLoaded( maxNativeAd, null );
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final NativeAd nativeAd, @NonNull final NativeAdError error)
        {
            final MaxAdapterError adapterError = toMaxError( error );
            log( "Native ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpressed(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad shown" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onTtlExpired(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad expired" );
        }
    }

    private class MaxSmaatoNativeAd
            extends MaxNativeAd
    {
        private MaxSmaatoNativeAd(final Builder builder) { super( builder ); }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAdRenderer nativeAdRenderer = SmaatoMediationAdapter.this.nativeAdRenderer;
            if ( nativeAdRenderer == null )
            {
                e( "Failed to register native ad view for interaction. Native ad renderer is null" );
                return false;
            }

            d( "Preparing views for interaction with container: " + container );

            nativeAdRenderer.registerForImpression( container );
            nativeAdRenderer.registerForClicks( container );      // Doesn't make the entire container clickable on its own.
            nativeAdRenderer.registerForClicks( clickableViews ); // Necessary to make CTA and other items clickable.

            return true;
        }
    }

    //endregion

    //region SmaatoMediationAdapterRouter

    /**
     * Router for interstitial/rewarded ad events.
     * Ads are removed on ad displayed/expired, as Smaato will allow a new ad load for the same adSpaceId.
     */
    private static class SmaatoMediationAdapterRouter
            extends MediationAdapterRouter
            implements com.smaato.sdk.interstitial.EventListener, com.smaato.sdk.rewarded.EventListener
    {
        // Interstitial
        private final Map<String, InterstitialAd> interstitialAds     = new HashMap<>();
        private final Object                      interstitialAdsLock = new Object();

        // Rewarded
        private final Map<String, RewardedInterstitialAd> rewardedAds     = new HashMap<>();
        private final Object                              rewardedAdsLock = new Object();

        private boolean hasGrantedReward;

        @Override
        void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener) { }

        public InterstitialAd getInterstitialAd(final String placementId)
        {
            synchronized ( interstitialAdsLock )
            {
                return interstitialAds.get( placementId );
            }
        }

        public RewardedInterstitialAd getRewardedAd(final String placementId)
        {
            synchronized ( rewardedAdsLock )
            {
                return rewardedAds.get( placementId );
            }
        }

        //region Interstitial listener

        @Override
        public void onAdLoaded(final InterstitialAd interstitialAd)
        {
            final String placementId = interstitialAd.getAdSpaceId();

            synchronized ( interstitialAdsLock )
            {
                interstitialAds.put( placementId, interstitialAd );
            }

            log( "Interstitial loaded for placement: " + placementId + "..." );
            onAdLoaded( placementId, interstitialAd.getCreativeId() );
        }

        @Override
        public void onAdFailedToLoad(final InterstitialRequestError interstitialRequestError)
        {
            final String placementId = interstitialRequestError.getAdSpaceId();

            log( "Interstitial failed to load for placement: " + placementId + "...with error: " + interstitialRequestError.getInterstitialError() );

            onAdLoadFailed( placementId, toMaxError( interstitialRequestError.getInterstitialError() ) );
        }

        @Override
        public void onAdError(@NonNull final InterstitialAd interstitialAd, @NonNull final InterstitialError interstitialError)
        {
            log( "Interstitial failed to display with error: " + interstitialError );

            if ( interstitialAd != null )
            {
                final String placementId = interstitialAd.getAdSpaceId();

                synchronized ( interstitialAdsLock )
                {
                    interstitialAds.remove( placementId );
                }

                final MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, interstitialError.ordinal(), interstitialError.name() );
                onAdDisplayFailed( placementId, adapterError );
            }
        }

        @Override
        public void onAdImpression(final InterstitialAd interstitialAd)
        {
            final String placementId = interstitialAd.getAdSpaceId();

            // Allow the next rewarded ad to load
            synchronized ( interstitialAdsLock )
            {
                interstitialAds.remove( placementId );
            }

            log( "Interstitial displayed" );
            onAdDisplayed( placementId );
        }

        @Override
        public void onAdOpened(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial opened" );
        }

        @Override
        public void onAdClicked(final InterstitialAd interstitialAd)
        {
            log( "Interstitial clicked" );
            onAdClicked( interstitialAd.getAdSpaceId() );
        }

        @Override
        public void onAdClosed(final InterstitialAd interstitialAd)
        {
            log( "Interstitial hidden" );
            onAdHidden( interstitialAd.getAdSpaceId() );
        }

        @Override
        public void onAdTTLExpired(final InterstitialAd interstitialAd)
        {
            log( "Interstitial expired" );

            synchronized ( interstitialAdsLock )
            {
                interstitialAds.remove( interstitialAd.getAdSpaceId() );
            }
        }

        private static MaxAdapterError toMaxError(final InterstitialError smaatoInterstitialError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( smaatoInterstitialError )
            {
                case NO_AD_AVAILABLE:
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case INVALID_REQUEST:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
                case NETWORK_ERROR:
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case CACHE_LIMIT_REACHED:
                case INTERNAL_ERROR:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case CREATIVE_RESOURCE_EXPIRED:
                    adapterError = MaxAdapterError.AD_EXPIRED;
                    break;
                case AD_UNLOADED:
                    adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                    break;
            }

            return new MaxAdapterError( adapterError, smaatoInterstitialError.ordinal(), smaatoInterstitialError.name() );
        }

        //endregion

        //region Rewarded listener

        @Override
        public void onAdLoaded(final RewardedInterstitialAd rewardedInterstitialAd)
        {
            final String placementId = rewardedInterstitialAd.getAdSpaceId();

            synchronized ( rewardedAdsLock )
            {
                rewardedAds.put( placementId, rewardedInterstitialAd );
            }

            log( "Rewarded ad loaded for placement: " + placementId + "..." );
            onAdLoaded( placementId, rewardedInterstitialAd.getCreativeId() );
        }

        @Override
        public void onAdFailedToLoad(final RewardedRequestError rewardedRequestError)
        {
            final String placementId = rewardedRequestError.getAdSpaceId();

            log( "Rewarded ad failed to load for placement: " + placementId + "...with error: " + rewardedRequestError.getRewardedError() );
            onAdLoadFailed( placementId, toMaxError( rewardedRequestError.getRewardedError() ) );
        }

        @Override
        public void onAdError(@NonNull final RewardedInterstitialAd rewardedInterstitialAd, @NonNull final RewardedError rewardedError)
        {
            log( "Rewarded ad failed to display with error: " + rewardedError );

            if ( rewardedInterstitialAd != null )
            {
                final String placementId = rewardedInterstitialAd.getAdSpaceId();

                synchronized ( rewardedAdsLock )
                {
                    rewardedAds.remove( placementId );
                }

                final MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED, rewardedError.ordinal(), rewardedError.name() );
                onAdDisplayFailed( placementId, adapterError );
            }
        }

        @Override
        public void onAdStarted(final RewardedInterstitialAd rewardedInterstitialAd)
        {
            final String placementId = rewardedInterstitialAd.getAdSpaceId();

            // Allow the next rewarded ad to load
            synchronized ( rewardedAdsLock )
            {
                rewardedAds.remove( placementId );
            }

            log( "Rewarded ad displayed" );
            onAdDisplayed( placementId );
        }

        @Override
        public void onAdClicked(final RewardedInterstitialAd rewardedInterstitialAd)
        {
            log( "Rewarded ad clicked" );
            onAdClicked( rewardedInterstitialAd.getAdSpaceId() );
        }

        @Override
        public void onAdReward(final RewardedInterstitialAd rewardedInterstitialAd)
        {
            log( "Rewarded ad video completed" );

            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed(final RewardedInterstitialAd rewardedInterstitialAd)
        {
            final String placementId = rewardedInterstitialAd.getAdSpaceId();

            if ( hasGrantedReward || shouldAlwaysRewardUser( placementId ) )
            {
                final MaxReward reward = getReward( placementId );
                log( "Rewarded user with reward: " + reward );
                onUserRewarded( placementId, reward );
            }

            log( "Rewarded ad hidden" );
            onAdHidden( placementId );
        }

        @Override
        public void onAdTTLExpired(final RewardedInterstitialAd rewardedInterstitialAd)
        {
            log( "Rewarded ad expired" );

            synchronized ( rewardedAdsLock )
            {
                rewardedAds.remove( rewardedInterstitialAd.getAdSpaceId() );
            }
        }

        private void onAdLoaded(final String placementId, final String creativeId)
        {
            if ( !TextUtils.isEmpty( creativeId ) )
            {
                final Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );

                onAdLoaded( placementId, extraInfo );
            }
            else
            {
                onAdLoaded( placementId );
            }
        }

        private static MaxAdapterError toMaxError(final RewardedError smaatoRewardedError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( smaatoRewardedError )
            {
                case NO_AD_AVAILABLE:
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case INVALID_REQUEST:
                    adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                    break;
                case NETWORK_ERROR:
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case INTERNAL_ERROR:
                case CACHE_LIMIT_REACHED:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
                case CREATIVE_RESOURCE_EXPIRED:
                    adapterError = MaxAdapterError.AD_EXPIRED;
                    break;
            }

            return new MaxAdapterError( adapterError, smaatoRewardedError.ordinal(), smaatoRewardedError.name() );
        }

        //endregion
    }

    //endregion
}
