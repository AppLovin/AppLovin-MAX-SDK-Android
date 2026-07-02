package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;

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
import com.unity3d.ads.InterstitialAd;
import com.unity3d.ads.InterstitialShowListener;
import com.unity3d.ads.LoadConfiguration;
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

    @Nullable private InterstitialAd loadedInterstitialAd;
    @Nullable private RewardedAd     loadedRewardedAd;
    @Nullable private BannerAd       loadedBannerAd;

    // Explicit default constructor declaration
    public UnityAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        updatePrivacyConsent( parameters );

        if ( initialized.compareAndSet( false, true ) )
        {
            final String gameId = parameters.getServerParameters().getString( "game_id", null );
            log( "Initializing UnityAds SDK with game id: " + gameId + "..." );
            initializationStatus = InitializationStatus.INITIALIZING;

            UnityAds.setDebugMode( parameters.isTesting() );

            InitializationConfiguration config = new InitializationConfiguration.Builder( gameId )
                    .withTestMode( parameters.isTesting() )
                    .withMediationInfo( createMediationInfo() )
                    .build();

            UnityAds.initialize( config, error -> {
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
            });
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
        if ( loadedBannerAd != null )
        {
            loadedBannerAd = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacyConsent( parameters );

        AdFormat unityFormat = toUnityAdFormat( parameters );
        UnityAds.getToken( new TokenConfiguration( unityFormat ), token -> {
            log( "Collected signal" );
            callback.onSignalCollected( token );
        });
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "interstitial ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters );

        LoadConfiguration.Builder builder = new LoadConfiguration.Builder( placementId )
                .withMediationInfo( createMediationInfo() );
        if ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) )
        {
            builder.withAdMarkup( parameters.getBidResponse() );
        }

        InterstitialAd.load( builder.build(), (ad, error) -> {
            if ( error == null )
            {
                log( "Interstitial placement \"" + placementId + "\" loaded" );
                loadedInterstitialAd = ad;
                listener.onInterstitialAdLoaded();
            }
            else
            {
                log( "Interstitial placement \"" + placementId + "\" failed to load with error: " + error.getCode() + ": " + error.getMessage() );
                listener.onInterstitialAdLoadFailed( toMaxError( error ) );
            }
        });
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for placement \"" + placementId + "\"..." );

        if(loadedInterstitialAd == null) {
            log( "Interstitial ad placement \"" + placementId + "\" failed to display with error: No Ad loaded" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                    -1,
                    "No Ad Loaded") );
            return;
        }

        if(activity == null) {
            log("Interstitial ad placement \"" + placementId + "\" failed to display with error: Non null activity is needed");
            listener.onInterstitialAdDisplayFailed(new MaxAdapterError(MaxAdapterError.AD_DISPLAY_FAILED,
                    -1,
                    "Non null activity is needed for ad display"));
            return;
        }
        
        loadedInterstitialAd.show( activity, new ShowConfiguration.Builder().build(), new InterstitialShowListener()
        {
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

            @Override
            public void onFailed(@NonNull final InterstitialAd ad, @NonNull final UnityAdsError error)
            {
                log( "Interstitial placement \"" + placementId + "\" failed to display with error: " + error.getCode() + ": " + error.getMessage() );
                listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                             error.getCode(),
                                                                             error.getMessage() ) );
            }
        } );
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "rewarded ad for placement \"" + placementId + "\"..." );

        updatePrivacyConsent( parameters );

        LoadConfiguration.Builder builder = new LoadConfiguration.Builder( placementId )
                .withMediationInfo( createMediationInfo() );
        if ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) )
        {
            builder.withAdMarkup( parameters.getBidResponse() );
        }

        RewardedAd.load( builder.build(), (ad, error) -> {
            if ( error == null )
            {
                log( "Rewarded ad placement \"" + placementId + "\" loaded" );
                loadedRewardedAd = ad;
                listener.onRewardedAdLoaded();
            }
            else
            {
                log( "Rewarded ad placement \"" + placementId + "\" failed to load with error: " + error.getCode() + ": " + error.getMessage() );
                listener.onRewardedAdLoadFailed( toMaxError( error ) );
            }
        });
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for placement \"" + placementId + "\"..." );

        configureReward( parameters );
        
        if(loadedRewardedAd == null) {
            log( "Rewarded ad placement \"" + placementId + "\" failed to display with error: No Ad loaded" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                    -1,
                    "No Ad Loaded") );
            return;
        }
        
        if(activity == null) {
            log("Rewarded ad placement \"" + placementId + "\" failed to display with error: Non null activity is needed");
            listener.onRewardedAdDisplayFailed(new MaxAdapterError(MaxAdapterError.AD_DISPLAY_FAILED,
                    -1,
                    "Non null activity is needed for ad display"));
            return;
        }
        
        loadedRewardedAd.show(activity, new ShowConfiguration.Builder().build(), new RewardedShowListener()
        {
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

            @Override
            public void onFailed(@NonNull final RewardedAd ad, @NonNull final UnityAdsError error)
            {
                log( "Rewarded ad placement \"" + placementId + "\" failed to display with error: " + error.getCode() + ": " + error.getMessage() );
                listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         error.getCode(),
                                                                         error.getMessage() ) );
            }
        } );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + adFormat.getLabel() + " ad for placement \"" + placementId + "\"..." );

        if ( activity == null )
        {
            log( adFormat.getLabel() + " ad placement \"" + placementId + "\" load failed: Activity is null" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.MISSING_ACTIVITY );
            return;
        }

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
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" failed to show" );
            }
        };

        BannerSize bannerSize = toBannerSize( parameters, adFormat, activity );

        BannerConfiguration.Builder builder =
                new BannerConfiguration.Builder( placementId, bannerSize, showListener )
                        .withMediationInfo( createMediationInfo() );
        if ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) )
        {
            builder.withAdMarkup( parameters.getBidResponse() );
        }

        BannerAd.load( builder.build(), (ad, error) -> {
            if ( error == null)
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" loaded" );
                loadedBannerAd = ad;
                listener.onAdViewAdLoaded(ad != null ? ad.getView() : null);
            }
            else
            {
                log( adFormat.getLabel() + " ad placement \"" + placementId + "\" failed to load with error: " + error.getCode() + ": " + error.getMessage() );
                listener.onAdViewAdLoadFailed( toMaxError( error ) );
            }
        });
    }

    private MediationInfo createMediationInfo()
    {
        return new MediationInfo( "MAX", UnityAds.getVersion(), getAdapterVersion() );
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

    private BannerSize toBannerSize(final MaxAdapterResponseParameters parameters,
                                    final MaxAdFormat adFormat,
                                    final Context context)
    {
        final boolean isAdaptiveBanner = parameters.getServerParameters().getBoolean( "adaptive_banner", false );

        if ( isAdaptiveBanner && AppLovinSdk.VERSION_CODE >= 13_02_00_99 && adFormat != MaxAdFormat.MREC )
        {
            int width = getAdaptiveAdViewWidth( parameters, context );
            if ( width <= 0 ) width = adFormat.getSize().getWidth();

            if ( isInlineAdaptiveAdView( parameters ) )
            {
                final int maxHeight = getInlineAdaptiveAdViewMaximumHeight( parameters );
                if ( maxHeight > 0 ) return new BannerSize( width, maxHeight );

                return new BannerSize( width, getAdaptiveMaxHeight( context ) );
            }

            final AppLovinSdkUtils.Size appSize = MaxAdFormat.BANNER.getAdaptiveSize( width, context );
            final int anchoredHeight = appSize == null ? 0 : appSize.getHeight();
            return new BannerSize( width, anchoredHeight );
        }

        return toBannerSize( adFormat );
    }

    private BannerSize toBannerSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER ) return new BannerSize( 320, 50 );
        if ( adFormat == MaxAdFormat.LEADER ) return new BannerSize( 728, 90 );
        if ( adFormat == MaxAdFormat.MREC )   return new BannerSize( 300, 250 );
        throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
    }

    private int getAdaptiveMaxHeight(final Context context)
    {
        final float screenHeightDp;
        if(context == null || context.getResources() == null || context.getResources().getDisplayMetrics() == null) {
            screenHeightDp = 0;
        } else {
            screenHeightDp = (float) context.getResources().getDisplayMetrics().heightPixels
                    / context.getResources().getDisplayMetrics().density;
        }
        return Math.min( 90, Math.max( 50, Math.round( screenHeightDp * 0.15f ) ) );
    }

    private MaxAdapterError toMaxError(final UnityAdsError error)
    {
        final int code = error.getCode();
        final MaxAdapterError adapterError;
        if ( code == 52100 )
        {
            adapterError = MaxAdapterError.NO_FILL;
        }
        else if ( code == 52101 )
        {
            adapterError = MaxAdapterError.NOT_INITIALIZED;
        }
        else if ( code == 52102 || code == 52104 )
        {
            adapterError = MaxAdapterError.INVALID_CONFIGURATION;
        }
        else if ( code == 2 )
        {
            adapterError = MaxAdapterError.TIMEOUT;
        }
        else
        {
            adapterError = MaxAdapterError.UNSPECIFIED;
        }
        return new MaxAdapterError( adapterError, code, error.getMessage() );
    }

    private void updatePrivacyConsent(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            UnityAds.setUserConsent( hasUserConsent );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            UnityAds.setUserOptOut( isDoNotSell );
        }
    }
}
