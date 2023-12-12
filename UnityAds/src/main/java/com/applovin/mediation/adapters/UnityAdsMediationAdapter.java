package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.applovin.mediation.MaxAdFormat;
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
import com.applovin.mediation.adapters.unityads.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.IUnityAdsTokenListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsLoadOptions;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.BannerErrorCode;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a mediation adapterWrapper for the Unity Ads SDK
 */
public class UnityAdsMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

    private String     biddingAdId;
    private BannerView bannerView;

    // Explicit default constructor declaration
    public UnityAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        final Context context = getContext( activity );

        updatePrivacyConsent( parameters, context );

        if ( initialized.compareAndSet( false, true ) )
        {
            Bundle serverParameters = parameters.getServerParameters();
            final String gameId = serverParameters.getString( "game_id", null );
            log( "Initializing UnityAds SDK with game id: " + gameId + "..." );
            initializationStatus = InitializationStatus.INITIALIZING;

            MediationMetaData mediationMetaData = new MediationMetaData( context );
            mediationMetaData.setName( "MAX" );
            mediationMetaData.setVersion( AppLovinSdk.VERSION );
            mediationMetaData.set( "adapter_version", getAdapterVersion() );
            mediationMetaData.commit();

            UnityAds.setDebugMode( parameters.isTesting() );

            UnityAds.initialize( context, gameId, parameters.isTesting(), new IUnityAdsInitializationListener()
            {
                @Override
                public void onInitializationComplete()
                {
                    log( "UnityAds SDK initialized" );
                    initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
                }

                @Override
                public void onInitializationFailed(final UnityAds.UnityAdsInitializationError error, final String message)
                {
                    log( "UnityAds SDK failed to initialize with error: " + message );
                    initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, message );
                }
            } );
        }
        else
        {
            log( "UnityAds SDK already initialized" );
            onCompletionListener.onCompletion( initializationStatus, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return UnityAds.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( bannerView != null )
        {
            bannerView.destroy();
            bannerView = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacyConsent( parameters, activity.getApplicationContext() );

        UnityAds.getToken( new IUnityAdsTokenListener()
        {
            @Override
            public void onUnityAdsTokenReady(final String token)
            {
                log( "Collected signal" );
                callback.onSignalCollected( token );
            }
        } );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "interstitial ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters, activity.getApplicationContext() );

        // Every ad needs a random ID associated with each load and show
        biddingAdId = UUID.randomUUID().toString();

        // Note: Most load callbacks are also fired in onUnityAdsPlacementStateChanged() but not all, we need these callbacks to catch all load errors.
        UnityAds.load( placementId, createAdLoadOptions( parameters ), new IUnityAdsLoadListener()
        {
            @Override
            public void onUnityAdsAdLoaded(final String placementId)
            {
                log( "Interstitial placement \"" + placementId + "\" loaded" );
                listener.onInterstitialAdLoaded();
            }

            @Override
            public void onUnityAdsFailedToLoad(final String placementId, final UnityAds.UnityAdsLoadError error, final String message)
            {
                log( "Interstitial placement \"" + placementId + "\" failed to load with error: " + error + ": " + message );
                listener.onInterstitialAdLoadFailed( toMaxError( error, message ) );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for placement \"" + placementId + "\"..." );

        UnityAds.show( activity, placementId, createAdShowOptions(), new IUnityAdsShowListener()
        {
            @Override
            public void onUnityAdsShowFailure(final String placementId, final UnityAds.UnityAdsShowError error, final String message)
            {
                log( "Interstitial placement \"" + placementId + "\" failed to display with error: " + error + ": " + message );
                listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", error.ordinal(), message ) );
            }

            @Override
            public void onUnityAdsShowStart(final String placementId)
            {
                log( "Interstitial placement \"" + placementId + "\" displayed" );
                listener.onInterstitialAdDisplayed();
            }

            @Override
            public void onUnityAdsShowClick(final String placementId)
            {
                log( "Interstitial placement \"" + placementId + "\" clicked" );
                listener.onInterstitialAdClicked();
            }

            @Override
            public void onUnityAdsShowComplete(final String placementId, final UnityAds.UnityAdsShowCompletionState state)
            {
                log( "Interstitial placement \"" + placementId + "\" hidden with completion state: " + state );
                listener.onInterstitialAdHidden();
            }
        } );
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "rewarded ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters, activity.getApplicationContext() );

        // Every ad needs a random ID associated with each load and show
        biddingAdId = UUID.randomUUID().toString();

        // Note: Most load callbacks are also fired in onUnityAdsPlacementStateChanged() but not all, we need these callbacks to catch all load errors.
        UnityAds.load( placementId, createAdLoadOptions( parameters ), new IUnityAdsLoadListener()
        {
            @Override
            public void onUnityAdsAdLoaded(final String placementId)
            {
                log( "Rewarded ad placement \"" + placementId + "\" loaded" );
                listener.onRewardedAdLoaded();
            }

            @Override
            public void onUnityAdsFailedToLoad(final String placementId, final UnityAds.UnityAdsLoadError error, final String message)
            {
                log( "Rewarded ad placement \"" + placementId + "\" failed to load with error: " + error + ": " + message );
                listener.onRewardedAdLoadFailed( toMaxError( error, message ) );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for placement \"" + placementId + "\"..." );

        // Configure userReward from server.
        configureReward( parameters );

        UnityAds.show( activity, placementId, createAdShowOptions(), new IUnityAdsShowListener()
        {
            @Override
            public void onUnityAdsShowFailure(final String placementId, final UnityAds.UnityAdsShowError error, final String message)
            {
                log( "Rewarded ad placement \"" + placementId + "\" failed to display with error: " + error + ": " + message );
                listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", error.ordinal(), message ) );
            }

            @Override
            public void onUnityAdsShowStart(final String placementId)
            {
                log( "Rewarded ad placement \"" + placementId + "\" displayed" );
                listener.onRewardedAdDisplayed();
                listener.onRewardedAdVideoStarted();
            }

            @Override
            public void onUnityAdsShowClick(final String placementId)
            {
                log( "Rewarded ad placement \"" + placementId + "\" clicked" );
                listener.onRewardedAdClicked();
            }

            @Override
            public void onUnityAdsShowComplete(final String placementId, final UnityAds.UnityAdsShowCompletionState state)
            {
                log( "Rewarded ad placement \"" + placementId + "\" hidden with completion state: " + state );
                listener.onRewardedAdVideoCompleted();
                if ( state == UnityAds.UnityAdsShowCompletionState.COMPLETED || shouldAlwaysRewardUser() )
                {
                    listener.onUserRewarded( getReward() );
                }
                listener.onRewardedAdHidden();
            }
        } );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + adFormat.getLabel() + " ad for placement \"" + placementId + "\"..." );

        if ( activity == null )
        {
            log( adFormat.getLabel() + " ad placement \"" + placementId + "\" load failed: Activity is null" );

            MaxAdapterError error = new MaxAdapterError( -5601, "Missing Activity" );
            listener.onAdViewAdLoadFailed( error );

            return;
        }

        updatePrivacyConsent( parameters, activity.getApplicationContext() );

        // Every ad needs a random ID associated with each load and show
        biddingAdId = UUID.randomUUID().toString();

        bannerView = new BannerView( activity, placementId, toUnityBannerSize( adFormat ) );
        bannerView.setListener( new BannerView.IListener()
        {
            @Override
            public void onBannerLoaded(final BannerView bannerAdView)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" loaded" );
                listener.onAdViewAdLoaded( bannerAdView );
            }

            @Override
            public void onBannerFailedToLoad(final BannerView bannerAdView, final BannerErrorInfo errorInfo)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" failed to load" );
                listener.onAdViewAdLoadFailed( toMaxError( errorInfo ) );
            }

            @Override
            public void onBannerShown(final BannerView bannerAdView)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" shown" );
                listener.onAdViewAdDisplayed();
            }

            @Override
            public void onBannerClick(final BannerView bannerAdView)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" clicked" );
                listener.onAdViewAdClicked();
            }

            @Override
            public void onBannerLeftApplication(final BannerView bannerView)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" left application" );
            }
        } );

        bannerView.load( createAdLoadOptions( parameters ) );
    }

    private UnityAdsLoadOptions createAdLoadOptions(final MaxAdapterResponseParameters parameters)
    {
        UnityAdsLoadOptions options = new UnityAdsLoadOptions();

        String bidResponse = parameters.getBidResponse();
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            options.setAdMarkup( bidResponse );
        }

        if ( AppLovinSdkUtils.isValidString( biddingAdId ) )
        {
            options.setObjectId( biddingAdId );
        }

        return options;
    }

    private UnityAdsShowOptions createAdShowOptions()
    {
        UnityAdsShowOptions options = new UnityAdsShowOptions();
        if ( AppLovinSdkUtils.isValidString( biddingAdId ) )
        {
            options.setObjectId( biddingAdId );
        }

        return options;
    }

    private UnityBannerSize toUnityBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return new UnityBannerSize( 320, 50 );
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return new UnityBannerSize( 728, 90 );
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return new UnityBannerSize( 300, 250 );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(final BannerErrorInfo unityAdsBannerError)
    {
        final MaxAdapterError adapterError;

        if ( unityAdsBannerError.errorCode == BannerErrorCode.NO_FILL )
        {
            adapterError = MaxAdapterError.NO_FILL;
        }
        else if ( unityAdsBannerError.errorCode == BannerErrorCode.NATIVE_ERROR )
        {
            adapterError = MaxAdapterError.INTERNAL_ERROR;
        }
        else if ( unityAdsBannerError.errorCode == BannerErrorCode.WEBVIEW_ERROR )
        {
            adapterError = MaxAdapterError.WEBVIEW_ERROR;
        }
        else
        {
            adapterError = MaxAdapterError.UNSPECIFIED;
        }
        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), unityAdsBannerError.errorCode.ordinal(), unityAdsBannerError.errorMessage );
    }

    private static MaxAdapterError toMaxError(final UnityAds.UnityAdsLoadError loadError, final String errorMessage)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( loadError )
        {
            case INITIALIZE_FAILED:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case INVALID_ARGUMENT:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), loadError.ordinal(), errorMessage );
    }

    private static MaxAdapterError toMaxError(final UnityAds.UnityAdsShowError showError, final String errorMessage)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( showError )
        {
            case NOT_INITIALIZED:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case NOT_READY:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case VIDEO_PLAYER_ERROR:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
            case INVALID_ARGUMENT:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case NO_CONNECTION:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case ALREADY_SHOWING:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), showError.ordinal(), errorMessage );
    }

    private void updatePrivacyConsent(final MaxAdapterParameters parameters, final Context context)
    {
        MetaData privacyMetaData = new MetaData( context );

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            privacyMetaData.set( "gdpr.consent", hasUserConsent );
            privacyMetaData.commit();
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null ) // CCPA compliance - https://unityads.unity3d.com/help/legal/gdpr
        {
            privacyMetaData.set( "privacy.consent", !isDoNotSell ); // isDoNotSell means user has opted out and is equivalent to false.
            privacyMetaData.commit();
        }

        privacyMetaData.set( "privacy.mode", "mixed" );
        privacyMetaData.commit();

        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            privacyMetaData.set( "user.nonbehavioral", isAgeRestrictedUser );
            privacyMetaData.commit();
        }
    }

    private Context getContext(Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }
}
