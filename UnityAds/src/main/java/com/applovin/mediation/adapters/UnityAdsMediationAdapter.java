package com.applovin.mediation.adapters;

import android.app.Activity;
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
import com.unity3d.ads.AdFormat;
import com.unity3d.ads.BannerAd;
import com.unity3d.ads.BannerConfiguration;
import com.unity3d.ads.BannerShowListener;
import com.unity3d.ads.BannerSize;
import com.unity3d.ads.InitializationConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.InterstitialAd;
import com.unity3d.ads.InterstitialShowListener;
import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.LoadListener;
import com.unity3d.ads.MediationInfo;
import com.unity3d.ads.RewardedAd;
import com.unity3d.ads.RewardedShowListener;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.TokenConfiguration;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This is a mediation adapterWrapper for the Unity Ads SDK
 */
public class UnityAdsMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private BannerAd       bannerAd;

    // Explicit default constructor declaration
    public UnityAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        updatePrivacyConsent( parameters );

        if ( initialized.compareAndSet( false, true ) )
        {
            Bundle serverParameters = parameters.getServerParameters();
            final String gameId = serverParameters.getString( "game_id", null );
            log( "Initializing UnityAds SDK with game id: " + gameId + "..." );
            initializationStatus = InitializationStatus.INITIALIZING;

            InitializationConfiguration config = new InitializationConfiguration.Builder( gameId )
                    .withTestMode( parameters.isTesting() )
                    .withMediationInfo( createMediationInfo() )
                    .build();

            UnityAds.initialize( config, new InitializationListener()
            {
                @Override
                public void onInitializationComplete(@Nullable final UnityAdsError error)
                {
                    if ( error == null )
                    {
                        log( "UnityAds SDK initialized" );
                        initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                        onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
                    }
                    else
                    {
                        log( "UnityAds SDK failed to initialize with error: " + error.getMessage() );
                        initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                        onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, error.getMessage() );
                    }
                }
            } );
        }
        else
        {
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
        interstitialAd = null;
        rewardedAd = null;

        if ( bannerAd != null )
        {
            bannerAd = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacyConsent( parameters );

        AdFormat unityFormat = toUnityAdFormat( parameters );
        TokenConfiguration tokenConfiguration = new TokenConfiguration.Builder( unityFormat )
                .withMediationInfo( createMediationInfo() )
                .build();
        UnityAds.getToken( tokenConfiguration, token -> {
            log( "Collected signal" );
            callback.onSignalCollected( token );
        } );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "interstitial ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters );

        InterstitialAd.load( createLoadConfiguration( parameters ), new LoadListener<InterstitialAd>()
        {
            @Override
            public void onAdLoaded(@Nullable final InterstitialAd ad, @Nullable final UnityAdsError error)
            {
                if ( error == null )
                {
                    log( "Interstitial placement \"" + placementId + "\" loaded" );
                    interstitialAd = ad;
                    listener.onInterstitialAdLoaded();
                }
                else
                {
                    log( "Interstitial placement \"" + placementId + "\" failed to load with error: " + error.getCode() + ": " + error.getMessage() );
                    listener.onInterstitialAdLoadFailed( toMaxError( error ) );
                }
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for placement \"" + placementId + "\"..." );

        if ( interstitialAd != null )
        {
            interstitialAd.show( activity, new ShowConfiguration.Builder().build(), new InterstitialShowListener()
            {
                @Override
                public void onFailed(@NonNull final InterstitialAd ad, @NonNull final UnityAdsError error)
                {
                    log( "Interstitial placement \"" + placementId + "\" failed to display with error: " + error.getCode() + ": " + error.getMessage() );
                    listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                                 error.getCode(),
                                                                                 error.getMessage() ) );
                }

                @Override
                public void onStarted(@NonNull final InterstitialAd ad)
                {
                    log( "Interstitial placement \"" + placementId + "\" displayed" );
                    listener.onInterstitialAdDisplayed();
                }

                @Override
                public void onClicked(@NonNull final InterstitialAd ad)
                {
                    log( "Interstitial placement \"" + placementId + "\" clicked" );
                    listener.onInterstitialAdClicked();
                }

                @Override
                public void onCompleted(@NonNull final InterstitialAd ad, @NonNull final ShowFinishState state)
                {
                    log( "Interstitial placement \"" + placementId + "\" hidden with completion state: " + state );
                    listener.onInterstitialAdHidden();
                }
            } );
        }
        else
        {
            log( "Interstitial ad failed to display for placement \"" + placementId + "\" - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                                         MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "rewarded ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters );

        RewardedAd.load( createLoadConfiguration( parameters ), new LoadListener<RewardedAd>()
        {
            @Override
            public void onAdLoaded(@Nullable final RewardedAd ad, @Nullable final UnityAdsError error)
            {
                if ( error == null )
                {
                    log( "Rewarded ad placement \"" + placementId + "\" loaded" );
                    rewardedAd = ad;
                    listener.onRewardedAdLoaded();
                }
                else
                {
                    log( "Rewarded ad placement \"" + placementId + "\" failed to load with error: " + error.getCode() + ": " + error.getMessage() );
                    listener.onRewardedAdLoadFailed( toMaxError( error ) );
                }
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for placement \"" + placementId + "\"..." );

        // Configure userReward from server.
        configureReward( parameters );

        if ( rewardedAd != null )
        {
            rewardedAd.show( activity, new ShowConfiguration.Builder().build(), new RewardedShowListener()
            {
                @Override
                public void onFailed(@NonNull final RewardedAd ad, @NonNull final UnityAdsError error)
                {
                    log( "Rewarded ad placement \"" + placementId + "\" failed to display with error: " + error.getCode() + ": " + error.getMessage() );
                    listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                             error.getCode(),
                                                                             error.getMessage() ) );
                }

                @Override
                public void onStarted(@NonNull final RewardedAd ad)
                {
                    log( "Rewarded ad placement \"" + placementId + "\" displayed" );
                    listener.onRewardedAdDisplayed();
                }

                @Override
                public void onClicked(@NonNull final RewardedAd ad)
                {
                    log( "Rewarded ad placement \"" + placementId + "\" clicked" );
                    listener.onRewardedAdClicked();
                }

                @Override
                public void onRewarded(@NonNull final RewardedAd ad)
                {
                    if ( !shouldAlwaysRewardUser() )
                    {
                        listener.onUserRewarded( getReward() );
                    }
                }

                @Override
                public void onCompleted(@NonNull final RewardedAd ad, @NonNull final ShowFinishState state)
                {
                    log( "Rewarded ad placement \"" + placementId + "\" hidden with completion state: " + state );

                    if ( shouldAlwaysRewardUser() )
                    {
                        listener.onUserRewarded( getReward() );
                    }
                    listener.onRewardedAdHidden();
                }
            } );
        }
        else
        {
            log( "Rewarded ad failed to display for placement \"" + placementId + "\" - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + adFormat.getLabel() + " ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters );

        BannerShowListener showListener = new BannerShowListener()
        {
            @Override
            public void onImpression(@NonNull final BannerAd ad)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" shown" );
                listener.onAdViewAdDisplayed();
            }

            @Override
            public void onClicked(@NonNull final BannerAd ad)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" clicked" );
                listener.onAdViewAdClicked();
            }

            @Override
            public void onFailedToShow(@NonNull final BannerAd ad, @NonNull final UnityAdsError error)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" failed to show with error: " + error.getCode() + ": " + error.getMessage() );
                listener.onAdViewAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                       error.getCode(),
                                                                       error.getMessage() ) );
            }
        };

        BannerConfiguration.Builder builder = new BannerConfiguration.Builder( placementId, toUnityBannerSize( adFormat ), showListener )
                .withMediationInfo( createMediationInfo() );

        String bidResponse = parameters.getBidResponse();
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            builder.withAdMarkup( bidResponse );
        }

        BannerAd.load( builder.build(), new LoadListener<BannerAd>()
        {
            @Override
            public void onAdLoaded(@Nullable final BannerAd ad, @Nullable final UnityAdsError error)
            {
                if ( error == null )
                {
                    log( adFormat.getLabel() + " ad placement \"" + placementId + "\" loaded" );
                    bannerAd = ad;
                    listener.onAdViewAdLoaded( ad != null ? ad.getView() : null );
                }
                else
                {
                    log( adFormat.getLabel() + " ad placement \"" + placementId + "\" failed to load with error: " + error.getCode() + ": " + error.getMessage() );
                    listener.onAdViewAdLoadFailed( toMaxError( error ) );
                }
            }
        } );
    }

    private LoadConfiguration createLoadConfiguration(final MaxAdapterResponseParameters parameters)
    {
        LoadConfiguration.Builder builder = new LoadConfiguration.Builder( parameters.getThirdPartyAdPlacementId() )
                .withMediationInfo( createMediationInfo() );

        String bidResponse = parameters.getBidResponse();
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            builder.withAdMarkup( bidResponse );
        }

        return builder.build();
    }

    private AdFormat toUnityAdFormat(final MaxAdapterSignalCollectionParameters parameters)
    {
        MaxAdFormat adFormat = parameters.getAdFormat();
        if ( adFormat.isAdViewAd() )
        {
            return AdFormat.BANNER;
        }
        else if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            return AdFormat.INTERSTITIAL;
        }
        else if ( adFormat == MaxAdFormat.REWARDED )
        {
            return AdFormat.REWARDED;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private MediationInfo createMediationInfo()
    {
        return new MediationInfo( "MAX", AppLovinSdk.VERSION, getAdapterVersion() );
    }

    private BannerSize toUnityBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return new BannerSize( 320, 50 );
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return new BannerSize( 728, 90 );
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return new BannerSize( 300, 250 );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private static MaxAdapterError toMaxError(final UnityAdsError unityAdsError)
    {
        final int unityAdsErrorCode = unityAdsError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( unityAdsErrorCode )
        {
            case 2: // Timeout
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case 52100: // No Fill
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case 52101: // Not Initialized
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case 52102: // Placement Not Found
            case 52104: // Unsupported Placement
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
        }

        return new MaxAdapterError( adapterError, unityAdsErrorCode, unityAdsError.getMessage() );
    }

    private void updatePrivacyConsent(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            UnityAds.setUserConsent( hasUserConsent );
        }

        // CCPA compliance - https://unityads.unity3d.com/help/legal/gdpr
        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            UnityAds.setUserOptOut( isDoNotSell );
        }

        UnityAds.setNonBehavioral( false );
    }
}
