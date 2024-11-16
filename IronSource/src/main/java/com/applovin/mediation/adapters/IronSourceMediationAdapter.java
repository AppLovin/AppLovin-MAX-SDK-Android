package com.applovin.mediation.adapters;

import android.app.Activity;
import android.os.Bundle;
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
import com.applovin.mediation.adapters.ironsource.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.ironsource.mediationsdk.ISBannerSize;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerListener;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyInterstitialListener;
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyRewardedVideoListener;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.utils.IronSourceConstants;
import com.ironsource.mediationsdk.utils.IronSourceUtils;
import com.unity3d.ironsourceads.AdSize;
import com.unity3d.ironsourceads.InitListener;
import com.unity3d.ironsourceads.InitRequest;
import com.unity3d.ironsourceads.IronSourceAds;
import com.unity3d.ironsourceads.banner.BannerAdLoader;
import com.unity3d.ironsourceads.banner.BannerAdLoaderListener;
import com.unity3d.ironsourceads.banner.BannerAdRequest;
import com.unity3d.ironsourceads.banner.BannerAdView;
import com.unity3d.ironsourceads.banner.BannerAdViewListener;
import com.unity3d.ironsourceads.interstitial.InterstitialAd;
import com.unity3d.ironsourceads.interstitial.InterstitialAdListener;
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoader;
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoaderListener;
import com.unity3d.ironsourceads.interstitial.InterstitialAdRequest;
import com.unity3d.ironsourceads.rewarded.RewardedAd;
import com.unity3d.ironsourceads.rewarded.RewardedAdListener;
import com.unity3d.ironsourceads.rewarded.RewardedAdLoader;
import com.unity3d.ironsourceads.rewarded.RewardedAdLoaderListener;
import com.unity3d.ironsourceads.rewarded.RewardedAdRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IronSourceMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final IronSourceRouter       ROUTER                           = new IronSourceRouter();
    private static final AtomicBoolean          INITIALIZED                      = new AtomicBoolean();
    private static final List<String>           loadedAdViewPlacementIdentifiers = Collections.synchronizedList( new ArrayList<>() );
    private static       InitializationStatus   status;

    private String                   mRouterPlacementIdentifier;
    @Nullable
    private String                   adViewPlacementIdentifier;
    private ISDemandOnlyBannerLayout adView;

    private BannerAdView   biddingAdView;
    private InterstitialAd biddingInterstitialAd;
    private RewardedAd     biddingRewardedAd;

    private BiddingRewardedListener     biddingRewardedListener;
    private BiddingInterstitialListener biddingInterstitialListener;

    public IronSourceMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter Methods

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final String appKey = parameters.getServerParameters().getString( "app_key" );
            log( "Initializing IronSource SDK with app key: " + appKey + "..." );

            IronSource.setMediationType( "MAX" + getAdapterVersionCode() + "SDK" + AppLovinSdk.VERSION_CODE );

            setPrivacySettings( parameters );

            Boolean isDoNotSell = parameters.isDoNotSell();
            if ( isDoNotSell != null )
            {
                // NOTE: `setMetaData` must be called _before_ initializing their SDK
                IronSource.setMetaData( "do_not_sell", Boolean.toString( isDoNotSell ) );
            }

            IronSource.setAdaptersDebug( parameters.isTesting() );
            IronSource.setISDemandOnlyInterstitialListener( ROUTER );
            IronSource.setISDemandOnlyRewardedVideoListener( ROUTER );

            InitRequest initRequest = new InitRequest.Builder( appKey ).withLegacyAdFormats( getAdFormatsToInitialize( parameters ) ).build();

            IronSourceAds.init( getApplicationContext(), initRequest, new InitListener()
            {
                @Override
                public void onInitSuccess()
                {
                    log( "IronSource SDK initialized." );
                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }

                @Override
                public void onInitFailed(@NonNull final IronSourceError ironSourceError)
                {
                    log( "Failed to initialize IronSource SDK with error: " + ironSourceError );
                    status = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( status, ironSourceError.getErrorMessage() );
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
        return IronSourceUtils.getSDKVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }
    //endregion

    @Override
    public void onDestroy()
    {
        if ( adViewPlacementIdentifier != null )
        {
            log( "Destroying adview with instance ID: " + adViewPlacementIdentifier );

            IronSource.destroyISDemandOnlyBanner( adViewPlacementIdentifier );
            loadedAdViewPlacementIdentifiers.remove( adViewPlacementIdentifier );
        }

        ROUTER.removeAdapter( this, mRouterPlacementIdentifier );

        if ( biddingAdView != null )
        {
            biddingAdView.setListener( null );
            biddingAdView = null;
        }

        if ( biddingInterstitialAd != null )
        {
            biddingInterstitialAd.setListener( null );
            biddingInterstitialAd = null;
        }

        if ( biddingRewardedAd != null )
        {
            biddingRewardedAd.setListener( null );
            biddingRewardedAd = null;
        }

        biddingInterstitialListener = null;
        biddingRewardedListener = null;
    }

    //endregion

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        setPrivacySettings( parameters );

        String signal = IronSource.getISDemandOnlyBiddingData( getApplicationContext() );
        callback.onSignalCollected( signal );
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        setPrivacySettings( parameters );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Loading ironSource " + ( isBiddingAd ? "bidding " : "" ) + "interstitial for instance ID: " + instanceId );

        if ( isBiddingAd )
        {
            InterstitialAdRequest adRequest = new InterstitialAdRequest.Builder( instanceId, bidResponse ).build();
            biddingInterstitialListener = new BiddingInterstitialListener( listener );
            InterstitialAdLoader.loadAd( adRequest, biddingInterstitialListener );
        }
        else
        {
            // Create a format specific router identifier to ensure that the router can distinguish between them.
            mRouterPlacementIdentifier = IronSourceRouter.getInterstitialRouterIdentifier( instanceId );
            ROUTER.addInterstitialAdapter( this, listener, mRouterPlacementIdentifier );

            if ( IronSource.isISDemandOnlyInterstitialReady( instanceId ) )
            {
                log( "Ad is available already for instance ID: " + instanceId );
                ROUTER.onAdLoaded( mRouterPlacementIdentifier );
            }
            else
            {
                // Tested that ad still successfully loads with a `null` Activity
                IronSource.loadISDemandOnlyInterstitial( activity, instanceId );
            }
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Showing ironSource interstitial for instance ID: " + instanceId );

        if ( isBiddingAd )
        {
            if ( biddingInterstitialAd == null || !biddingInterstitialAd.isReadyToShow() )
            {
                log( "Unable to show ironSource interstitial - ad is not ready for instance ID: " + instanceId );
                listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
                return;
            }

            if ( activity == null )
            {
                log( "Interstitial ad display failed: Activity is null" );
                listener.onInterstitialAdDisplayFailed( MaxAdapterError.MISSING_ACTIVITY );
                return;
            }

            biddingInterstitialAd.setListener( biddingInterstitialListener );
            biddingInterstitialAd.show( activity );
        }
        else
        {
            ROUTER.addShowingAdapter( this );

            if ( !IronSource.isISDemandOnlyInterstitialReady( instanceId ) )
            {
                log( "Unable to show ironSource interstitial - no ad loaded for instance ID: " + instanceId );
                ROUTER.onAdDisplayFailed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ), new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
                return;
            }

            IronSource.showISDemandOnlyInterstitial( instanceId );
        }
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        setPrivacySettings( parameters );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Loading ironSource " + ( isBiddingAd ? "bidding " : "" ) + "rewarded for instance ID: " + instanceId );

        if ( isBiddingAd )
        {
            RewardedAdRequest adRequest = new RewardedAdRequest.Builder( instanceId, bidResponse ).build();
            biddingRewardedListener = new BiddingRewardedListener( listener );
            RewardedAdLoader.loadAd( adRequest, biddingRewardedListener );
        }
        else
        {
            // Create a format specific router identifier to ensure that the router can distinguish between them.
            mRouterPlacementIdentifier = IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId );
            ROUTER.addRewardedAdapter( this, listener, mRouterPlacementIdentifier );

            if ( IronSource.isISDemandOnlyRewardedVideoAvailable( instanceId ) )
            {
                log( "Ad is available already for instance ID: " + instanceId );
                ROUTER.onAdLoaded( mRouterPlacementIdentifier );
            }
            else
            {
                // Tested that ad still successfully loads with a `null` Activity
                IronSource.loadISDemandOnlyRewardedVideo( activity, instanceId );
            }
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final String instanceId = parameters.getThirdPartyAdPlacementId();

        log( "Showing ironSource rewarded for instance ID: " + instanceId );

        if ( isBiddingAd )
        {
            if ( biddingRewardedAd == null || !biddingRewardedAd.isReadyToShow() )
            {
                log( "Unable to show ironSource rewarded - ad is not ready for instance ID: " + instanceId );
                listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
                return;
            }

            if ( activity == null )
            {
                log( "Rewarded ad display failed: Activity is null" );
                listener.onRewardedAdDisplayFailed( MaxAdapterError.MISSING_ACTIVITY );
                return;
            }

            // Configure userReward from server.
            configureReward( parameters );

            biddingRewardedAd.setListener( biddingRewardedListener );
            biddingRewardedAd.show( activity );
        }
        else
        {
            ROUTER.addShowingAdapter( this );

            if ( !IronSource.isISDemandOnlyRewardedVideoAvailable( instanceId ) )
            {
                log( "Unable to show ironSource rewarded - no ad loaded..." );
                ROUTER.onAdDisplayFailed( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ), new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
                return;
            }

            // Configure userReward from server.
            configureReward( parameters );

            IronSource.showISDemandOnlyRewardedVideo( instanceId );
        }
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        setPrivacySettings( parameters );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );

        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + adFormat.getLabel() + " ad for instance ID: " + parameters.getThirdPartyAdPlacementId() );

        if ( isBiddingAd )
        {
            AdSize adSize = toISAdSize( adFormat );
            BannerAdRequest bannerAdRequest = new BannerAdRequest.Builder( getApplicationContext(), parameters.getThirdPartyAdPlacementId(), bidResponse, adSize ).build();
            BannerAdLoader.loadAd( bannerAdRequest, new BiddingAdViewListener( listener ) );
        }
        else
        {
            if ( activity == null )
            {
                log( adFormat.getLabel() + " ad load failed: Activity is null" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );

                return;
            }

            // IronSource does not support b2b with same instance ID for banners/MRECs
            if ( loadedAdViewPlacementIdentifiers.contains( parameters.getThirdPartyAdPlacementId() ) )
            {
                log( "AdView ad failed to load for instance ID: " + parameters.getThirdPartyAdPlacementId() + ". An ad with the same instance ID is already loaded" );
                listener.onAdViewAdLoadFailed( new MaxAdapterError( MaxAdapterError.INTERNAL_ERROR.getCode(), MaxAdapterError.INTERNAL_ERROR.getMessage(), 0, "An ad with the same instance ID is already loaded" ) );

                return;
            }

            adViewPlacementIdentifier = parameters.getThirdPartyAdPlacementId(); // Set it only if it is not an instance ID of an already loaded ad to avoid destroying the currently showing ad

            // If we pass in a null Activity, `createBannerForDemandOnly` will return null
            adView = IronSource.createBannerForDemandOnly( activity, toISBannerSize( adFormat ) );
            adView.setBannerDemandOnlyListener( new AdViewListener( listener ) );

            IronSource.loadISDemandOnlyBanner( activity, adView, adViewPlacementIdentifier );
        }
    }

    //endregion

    //region Helper Methods

    private void setPrivacySettings(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            IronSource.setConsent( hasUserConsent );
        }
    }

    private List<IronSourceAds.AdFormat> getAdFormatsToInitialize(final MaxAdapterInitializationParameters parameters)
    {
        List<String> adFormats = parameters.getServerParameters().getStringArrayList( "init_ad_formats" );
        if ( adFormats == null || adFormats.isEmpty() )
        {
            // Default to initialize all ad formats if backend doesn't send down which ones to initialize
            return Arrays.asList( IronSourceAds.AdFormat.INTERSTITIAL, IronSourceAds.AdFormat.REWARDED, IronSourceAds.AdFormat.BANNER );
        }

        List<IronSourceAds.AdFormat> adFormatsToInitialize = new ArrayList<>();
        if ( adFormats.contains( "inter" ) )
        {
            adFormatsToInitialize.add( IronSourceAds.AdFormat.INTERSTITIAL );
        }

        if ( adFormats.contains( "rewarded" ) )
        {
            adFormatsToInitialize.add( IronSourceAds.AdFormat.REWARDED );
        }

        if ( adFormats.contains( "banner" ) )
        {
            adFormatsToInitialize.add( IronSourceAds.AdFormat.BANNER );
        }

        return adFormatsToInitialize;
    }

    private ISBannerSize toISBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return ISBannerSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return ISBannerSize.LARGE; // Note: LARGE is 320x90 - leaders weren't supported at the time of implementation.
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return ISBannerSize.RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private AdSize toISAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.banner();
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.leaderboard();
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.mediumRectangle();
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(final IronSourceError ironSourceError)
    {
        final int ironSourceErrorCode = ironSourceError.getErrorCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( ironSourceErrorCode )
        {
            case IronSourceError.ERROR_CODE_DECRYPT_FAILED:
            case IronSourceError.ERROR_NON_EXISTENT_INSTANCE:
            case IronSourceError.ERROR_BN_LOAD_EXCEPTION:
            case IronSourceError.ERROR_BN_INSTANCE_LOAD_EMPTY_BANNER:
            case IronSourceError.ERROR_BN_INSTANCE_LOAD_EMPTY_ADAPTER:
            case IronSourceError.ERROR_BN_RELOAD_SKIP_INVISIBLE:
            case IronSourceError.ERROR_BN_RELOAD_SKIP_BACKGROUND:
            case IronSourceError.AUCTION_ERROR_REQUEST:
            case IronSourceError.AUCTION_ERROR_RESPONSE_CODE_NOT_VALID:
            case IronSourceError.AUCTION_ERROR_PARSE:
            case IronSourceError.AUCTION_ERROR_DECRYPTION:
            case IronSourceError.AUCTION_ERROR_EMPTY_WATERFALL:
            case IronSourceError.AUCTION_ERROR_NO_CANDIDATES:
            case IronSourceError.AUCTION_REQUEST_ERROR_MISSING_PARAMS:
            case IronSourceError.AUCTION_ERROR_DECOMPRESSION:
            case IronSourceError.ERROR_RV_LOAD_SUCCESS_UNEXPECTED:
            case IronSourceError.ERROR_RV_LOAD_FAIL_UNEXPECTED:
            case IronSourceError.ERROR_RV_LOAD_UNEXPECTED_CALLBACK:
            case IronSourceError.ERROR_RV_SHOW_EXCEPTION:
            case IronSourceError.ERROR_IS_SHOW_EXCEPTION:
            case IronSourceError.ERROR_RV_INSTANCE_INIT_EXCEPTION:
            case IronSourceError.ERROR_IS_INSTANCE_INIT_EXCEPTION:
            case IronSourceError.ERROR_DO_RV_LOAD_MISSING_ACTIVITY:
            case IronSourceError.ERROR_DO_IS_LOAD_MISSING_ACTIVITY:
            case 7101:
            case 7102:
            case 7103:
            case 7104:
            case 7201:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_BN_LOAD_NO_CONFIG:
            case IronSourceError.ERROR_BN_UNSUPPORTED_SIZE:
            case IronSourceError.ERROR_IS_EMPTY_DEFAULT_PLACEMENT:
            case IronSourceError.ERROR_RV_EMPTY_DEFAULT_PLACEMENT:
            case IronSourceError.ERROR_RV_LOAD_SUCCESS_WRONG_AUCTION_ID:
            case IronSourceError.ERROR_RV_LOAD_FAIL_WRONG_AUCTION_ID:
            case 7105:
            case 7106:
            case 7107:
            case 7108:
            case 7109:
            case 7110:
            case 7111:
            case 7112:
            case 7116:
            case 7117:
            case 7118:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case IronSourceError.ERROR_CODE_INIT_FAILED:
            case IronSourceError.ERROR_BN_LOAD_AFTER_INIT_FAILED:
            case IronSourceError.ERROR_BN_LOAD_AFTER_LONG_INITIATION:
            case IronSourceError.ERROR_BN_INIT_FAILED_AFTER_LOAD:
            case IronSourceError.ERROR_BN_LOAD_WHILE_LONG_INITIATION:
            case IronSourceError.ERROR_BN_INSTANCE_INIT_TIMEOUT:
            case IronSourceError.ERROR_BN_INSTANCE_INIT_EXCEPTION:
            case IronSourceError.INIT_ERROR_NO_ADAPTERS_LOADED:
            case 7001:
            case 7002:
            case 7003:
            case 7004:
            case 7115:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
            case IronSourceError.ERROR_BN_LOAD_NO_FILL:
            case IronSourceError.ERROR_RV_LOAD_FAILED_NO_CANDIDATES:
            case IronSourceError.ERROR_IS_LOAD_FAILED_NO_CANDIDATES:
            case IronSourceError.ERROR_RV_LOAD_NO_FILL:
            case IronSourceError.ERROR_IS_LOAD_NO_FILL:
            case IronSourceError.ERROR_BN_INSTANCE_LOAD_AUCTION_FAILED:
            case IronSourceConstants.BN_INSTANCE_LOAD_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case IronSourceError.ERROR_CODE_GENERIC:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case IronSourceError.ERROR_CAPPED_PER_SESSION:
            case IronSourceError.ERROR_BN_LOAD_PLACEMENT_CAPPED:
                adapterError = MaxAdapterError.AD_FREQUENCY_CAPPED;
                break;
            case IronSourceError.ERROR_BN_INSTANCE_LOAD_TIMEOUT:
            case IronSourceError.ERROR_BN_INSTANCE_RELOAD_TIMEOUT:
            case IronSourceError.ERROR_RV_INIT_FAILED_TIMEOUT:
            case IronSourceError.ERROR_RV_LOAD_FAIL_DUE_TO_INIT:
            case IronSourceError.ERROR_DO_IS_LOAD_TIMED_OUT:
            case IronSourceError.ERROR_DO_RV_LOAD_TIMED_OUT:
            case 7113:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case IronSourceError.AUCTION_ERROR_TIMED_OUT:
                adapterError = MaxAdapterError.SIGNAL_COLLECTION_TIMEOUT;
                break;
            case IronSourceError.ERROR_RV_SHOW_CALLED_DURING_SHOW:
            case IronSourceError.ERROR_RV_SHOW_CALLED_WRONG_STATE:
            case IronSourceError.ERROR_RV_LOAD_DURING_LOAD:
            case IronSourceError.ERROR_RV_LOAD_DURING_SHOW:
            case IronSourceError.ERROR_IS_SHOW_CALLED_DURING_SHOW:
            case IronSourceError.ERROR_IS_LOAD_DURING_SHOW:
            case IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS:
            case IronSourceError.ERROR_DO_RV_LOAD_ALREADY_IN_PROGRESS:
            case IronSourceError.ERROR_DO_RV_LOAD_DURING_SHOW:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case IronSourceError.ERROR_DO_IS_CALL_LOAD_BEFORE_SHOW:
            case IronSourceError.ERROR_DO_RV_CALL_LOAD_BEFORE_SHOW:
            case 7202:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case IronSourceError.ERROR_RV_EXPIRED_ADS:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), ironSourceErrorCode, ironSourceError.getErrorMessage() );
    }

    private long getAdapterVersionCode()
    {
        String simplifiedVersionString = getAdapterVersion().replaceAll( "[^0-9.]", "" );
        String[] versionNumbers = simplifiedVersionString.split( "\\." );

        long versionCode = 0;
        for ( String num : versionNumbers )
        {
            versionCode *= 100;
            if ( versionCode != 0 && num.length() > 2 )
            {
                versionCode += Integer.parseInt( num.substring( 0, 2 ) );
            }
            else
            {
                versionCode += num.isEmpty() ? 0 : Integer.parseInt( num );
            }
        }

        return versionCode;
    }

    //endregion

    //region IronSource Router

    private static class IronSourceRouter
            extends MediationAdapterRouter
            implements ISDemandOnlyInterstitialListener, ISDemandOnlyRewardedVideoListener
    {
        private boolean hasGrantedReward;

        void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener) { }

        @Override
        public void onInterstitialAdReady(final String instanceId)
        {
            log( "Interstitial loaded for instance ID: " + instanceId );
            onAdLoaded( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onInterstitialAdLoadFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            log( "Interstitial ad failed to load for instance ID: " + instanceId + " with error: " + ironSourceError );
            onAdLoadFailed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ), toMaxError( ironSourceError ) );
        }

        @Override
        public void onInterstitialAdOpened(final String instanceId)
        {
            log( "Interstitial ad displayed for instance ID: " + instanceId );
            onAdDisplayed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onInterstitialAdShowFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage() );
            log( "Interstitial ad failed to show for instance ID: " + instanceId + " with error: " + adapterError );
            onAdDisplayFailed( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ), adapterError );
        }

        @Override
        public void onInterstitialAdClicked(final String instanceId)
        {
            log( "Interstitial ad clicked for instance ID: " + instanceId );
            onAdClicked( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onInterstitialAdClosed(final String instanceId)
        {
            log( "Interstitial ad closed for instance ID: " + instanceId );
            onAdHidden( IronSourceRouter.getInterstitialRouterIdentifier( instanceId ) );
        }

        @Override
        public void onRewardedVideoAdLoadSuccess(final String instanceId)
        {
            log( "Rewarded ad loaded for instance ID: " + instanceId );
            onAdLoaded( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ) );
        }

        @Override
        public void onRewardedVideoAdLoadFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            log( "Rewarded ad failed to load for instance ID: " + instanceId );
            onAdLoadFailed( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ), toMaxError( ironSourceError ) );
        }

        @Override
        public void onRewardedVideoAdOpened(final String instanceId)
        {
            log( "Rewarded ad shown for instance ID: " + instanceId );

            final String routerPlacementId = IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId );
            onAdDisplayed( routerPlacementId );
        }

        @Override
        public void onRewardedVideoAdShowFailed(final String instanceId, final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage() );
            log( "Rewarded ad failed to show for instance ID: " + instanceId + " with error: " + adapterError );
            onAdDisplayFailed( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ), adapterError );
        }

        @Override
        public void onRewardedVideoAdClicked(final String instanceId)
        {
            log( "Rewarded ad clicked for instance ID: " + instanceId );
            onAdClicked( IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId ) );
        }

        @Override
        public void onRewardedVideoAdClosed(final String instanceId)
        {
            final String routerPlacementId = IronSourceRouter.getRewardedVideoRouterIdentifier( instanceId );
            if ( hasGrantedReward || shouldAlwaysRewardUser( routerPlacementId ) )
            {
                MaxReward reward = getReward( routerPlacementId );

                log( "Rewarded  ad rewarded user with reward: " + reward + " for instance ID: " + instanceId );
                onUserRewarded( routerPlacementId, reward );
            }

            log( "Rewarded ad hidden for instance ID: " + instanceId );
            onAdHidden( routerPlacementId );
        }

        @Override
        public void onRewardedVideoAdRewarded(final String instanceId)
        {
            log( "Rewarded ad granted reward for instance ID: " + instanceId );

            hasGrantedReward = true;
        }

        private static String getInterstitialRouterIdentifier(final String instanceId)
        {
            return instanceId + "-" + IronSource.AD_UNIT.INTERSTITIAL;
        }

        private static String getRewardedVideoRouterIdentifier(final String instanceId)
        {
            return instanceId + "-" + IronSource.AD_UNIT.REWARDED_VIDEO;
        }
    }

    //endregion

    //region AdView Listener

    private class AdViewListener
            implements ISDemandOnlyBannerListener
    {
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onBannerAdLoaded(final String instanceId)
        {
            log( "AdView loaded for instance ID: " + instanceId );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onBannerAdLoadFailed(String instanceId, IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = toMaxError( ironSourceError );
            log( "AdView ad failed to load for instance ID: " + instanceId + " with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onBannerAdClicked(String instanceId)
        {
            log( "AdView ad clicked for instance ID: " + instanceId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onBannerAdShown(String instanceId)
        {
            loadedAdViewPlacementIdentifiers.add( instanceId );

            log( "AdView ad displayed for instance ID: " + instanceId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onBannerAdLeftApplication(String instanceId)
        {
            log( "AdView ad left application for instance ID: " + instanceId );
        }
    }

    //endregion

    //region Bidding Interstitial Ad Listener

    private class BiddingInterstitialListener
            implements InterstitialAdLoaderListener, InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        BiddingInterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onInterstitialAdLoaded(@NonNull final InterstitialAd ad)
        {
            log( "Interstitial loaded for bidding instance ID: " + ad.getAdInfo().getInstanceId() );

            biddingInterstitialAd = ad;

            listener.onInterstitialAdLoaded( createExtraInfo( ad ) );
        }

        @Override
        public void onInterstitialAdLoadFailed(@NonNull final IronSourceError ironSourceError)
        {
            log( "Interstitial ad failed to load for bidding instance with error: " + ironSourceError );
            listener.onInterstitialAdLoadFailed( toMaxError( ironSourceError ) );
        }

        @Override
        public void onInterstitialAdShown(@NonNull final InterstitialAd ad)
        {
            log( "Interstitial ad displayed for bidding instance ID: " + ad.getAdInfo().getInstanceId() );
            listener.onInterstitialAdDisplayed( createExtraInfo( ad ) );
        }

        @Override
        public void onInterstitialAdFailedToShow(@NonNull final InterstitialAd ad, @NonNull final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage() );
            log( "Interstitial ad failed to show for bidding instance ID: " + ad.getAdInfo().getInstanceId() + " with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError, createExtraInfo( ad ) );
        }

        @Override
        public void onInterstitialAdClicked(@NonNull final InterstitialAd ad)
        {
            log( "Interstitial ad clicked for bidding instance ID: " + ad.getAdInfo().getInstanceId() );
            listener.onInterstitialAdClicked( createExtraInfo( ad ) );
        }

        @Override
        public void onInterstitialAdDismissed(@NonNull final InterstitialAd ad)
        {
            log( "Interstitial ad closed for bidding instance ID: " + ad.getAdInfo().getInstanceId() );
            listener.onInterstitialAdHidden( createExtraInfo( ad ) );
        }

        @Nullable
        private Bundle createExtraInfo(@NonNull final InterstitialAd ad)
        {
            String adId = ad.getAdInfo().getAdId();

            if ( TextUtils.isEmpty( adId ) )
            {
                return null;
            }

            Bundle extraInfo = new Bundle( 1 );
            extraInfo.putString( "creative_id", adId );
            return extraInfo;
        }
    }

    //endregion

    //region Bidding Rewarded Ad Listener

    private class BiddingRewardedListener
            implements RewardedAdLoaderListener, RewardedAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        BiddingRewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRewardedAdLoaded(@NonNull final RewardedAd ad)
        {
            log( "Rewarded ad loaded for bidding instance ID: " + ad.getAdInfo().getInstanceId() );

            biddingRewardedAd = ad;

            listener.onRewardedAdLoaded( createExtraInfo( ad ) );
        }

        @Override
        public void onRewardedAdLoadFailed(@NonNull final IronSourceError ironSourceError)
        {
            log( "Rewarded ad failed to load for bidding instance with error: " + ironSourceError );
            listener.onRewardedAdLoadFailed( toMaxError( ironSourceError ) );
        }

        @Override
        public void onRewardedAdShown(@NonNull final RewardedAd ad)
        {
            log( "Rewarded ad shown for bidding instance ID: " + ad.getAdInfo().getInstanceId() );
            listener.onRewardedAdDisplayed( createExtraInfo( ad ) );
        }

        @Override
        public void onRewardedAdFailedToShow(@NonNull final RewardedAd ad, @NonNull final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", ironSourceError.getErrorCode(), ironSourceError.getErrorMessage() );
            log( "Rewarded ad failed to show for bidding instance ID: " + ad.getAdInfo().getInstanceId() + " with error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError, createExtraInfo( ad ) );
        }

        @Override
        public void onRewardedAdClicked(@NonNull final RewardedAd ad)
        {
            log( "Rewarded ad clicked for instance ID: " + ad.getAdInfo().getInstanceId() );
            listener.onRewardedAdClicked( createExtraInfo( ad ) );
        }

        @Override
        public void onRewardedAdDismissed(@NonNull final RewardedAd ad)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                MaxReward reward = getReward();

                log( "Rewarded ad rewarded user with reward: " + reward + " for instance ID: " + ad.getAdInfo().getInstanceId() );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden for instance ID: " + ad.getAdInfo().getInstanceId() );
            listener.onRewardedAdHidden( createExtraInfo( ad ) );
        }

        @Override
        public void onUserEarnedReward(@NonNull final RewardedAd ad)
        {
            log( "Rewarded ad granted reward for instance ID: " + ad.getAdInfo().getInstanceId() );
            hasGrantedReward = true;
        }

        @Nullable
        private Bundle createExtraInfo(@NonNull final RewardedAd ad)
        {
            String adId = ad.getAdInfo().getAdId();

            if ( TextUtils.isEmpty( adId ) )
            {
                return null;
            }

            Bundle extraInfo = new Bundle( 1 );
            extraInfo.putString( "creative_id", adId );
            return extraInfo;
        }
    }

    //endregion

    //region Bidding AdView Ad Listener

    private class BiddingAdViewListener
            implements BannerAdLoaderListener, BannerAdViewListener
    {
        private final MaxAdViewAdapterListener listener;

        BiddingAdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onBannerAdLoaded(@NonNull final BannerAdView ad)
        {
            log( "AdView loaded for instance ID: " + ad.getAdInfo().getInstanceId() );

            biddingAdView = ad;
            biddingAdView.setListener( this );

            listener.onAdViewAdLoaded( biddingAdView, createExtraInfo( ad ) );
        }

        @Override
        public void onBannerAdLoadFailed(@NonNull final IronSourceError ironSourceError)
        {
            MaxAdapterError adapterError = toMaxError( ironSourceError );
            log( "AdView ad failed to load for bidding instance with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onBannerAdClicked(@NonNull final BannerAdView ad)
        {
            log( "AdView ad clicked" );
            listener.onAdViewAdClicked( createExtraInfo( ad ) );
        }

        @Override
        public void onBannerAdShown(@NonNull final BannerAdView ad)
        {
            log( "AdView ad displayed" );
            listener.onAdViewAdDisplayed( createExtraInfo( ad ) );
        }

        @Nullable
        private Bundle createExtraInfo(@NonNull final BannerAdView ad)
        {
            String adId = ad.getAdInfo().getAdId();

            if ( TextUtils.isEmpty( adId ) )
            {
                return null;
            }

            Bundle extraInfo = new Bundle( 1 );
            extraInfo.putString( "creative_id", adId );
            return extraInfo;
        }
    }

    //endregion
}
