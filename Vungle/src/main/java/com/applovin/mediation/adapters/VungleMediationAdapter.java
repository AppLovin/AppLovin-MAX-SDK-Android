package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxAppOpenAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.vungle.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.vungle.warren.AdConfig;
import com.vungle.warren.BannerAdConfig;
import com.vungle.warren.Banners;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.NativeAd;
import com.vungle.warren.NativeAdLayout;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Plugin;
import com.vungle.warren.Vungle;
import com.vungle.warren.VungleApiClient;
import com.vungle.warren.VungleBanner;
import com.vungle.warren.VungleSettings;
import com.vungle.warren.error.VungleException;
import com.vungle.warren.ui.view.MediaView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

public class VungleMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, /* MaxAppOpenAdapter */ MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private VungleBanner adViewAd;
    private NativeAd     nativeAd;

    // Explicit default constructor declaration
    public VungleMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        updateUserPrivacySettings( parameters );

        if ( initialized.compareAndSet( false, true ) )
        {
            String appId = parameters.getServerParameters().getString( "app_id", null );
            log( "Initializing Vungle SDK with app id: " + appId + "..." );

            status = InitializationStatus.INITIALIZING;

            // NOTE: Vungle's SDK will log error if setting COPPA state after it initializes
            Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
            if ( isAgeRestrictedUser != null )
            {
                Vungle.updateUserCoppaStatus( isAgeRestrictedUser );
            }

            Plugin.addWrapperInfo( VungleApiClient.WrapperFramework.max, getAdapterVersion() );

            VungleSettings settings = new VungleSettings.Builder().disableBannerRefresh().build();
            InitCallback initCallback = new InitCallback()
            {
                @Override
                public void onSuccess()
                {
                    log( "Vungle SDK initialized" );

                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }

                @Override
                public void onError(final VungleException exception)
                {
                    log( "Vungle SDK failed to initialize with error: ", exception );

                    status = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( status, exception.getLocalizedMessage() );
                }

                @Override
                public void onAutoCacheAdAvailable(final String id)
                {
                    log( "Auto-cached ad: " + id );
                }
            };

            // Note: Vungle requires the Application Context
            Vungle.init( appId, getContext( activity ), initCallback, settings );
        }
        else
        {
            log( "Vungle SDK already initialized" );
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( com.vungle.warren.BuildConfig.class, "VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( adViewAd != null )
        {
            adViewAd.destroyAd();
            adViewAd = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.unregisterView();
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserPrivacySettings( parameters );

        String signal = Vungle.getAvailableBidTokens( activity.getApplicationContext() );
        callback.onSignalCollected( signal );
    }

    //endregion

    //region MaxInterstitialAdapter

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        if ( !Vungle.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( !isValidPlacement( parameters ) )
        {
            log( "Interstitial ad failed to load due to an invalid placement id: " + placementId );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

            return;
        }

        if ( isBiddingAd )
        {
            if ( Vungle.canPlayAd( placementId, bidResponse ) )
            {
                log( "Interstitial ad loaded" );
                listener.onInterstitialAdLoaded();

                return;
            }
        }
        else if ( Vungle.canPlayAd( placementId ) )
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();

            return;
        }

        updateUserPrivacySettings( parameters );
        loadFullscreenAd( parameters, getContext( activity ), new LoadAdCallback()
        {
            @Override
            public void onAdLoad(final String id)
            {
                log( "Interstitial ad loaded" );
                listener.onInterstitialAdLoaded();
            }

            @Override
            public void onError(final String id, final VungleException exception)
            {
                MaxAdapterError error = toMaxError( exception );
                log( "Interstitial ad for placement " + id + " failed to load with error: " + error );
                listener.onInterstitialAdLoadFailed( error );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        if ( isBiddingAd )
        {
            if ( Vungle.canPlayAd( placementId, bidResponse ) )
            {
                showFullscreenAd( parameters, getContext( activity ), new InterstitialAdListener( listener ) );
                return;
            }
        }
        else if ( Vungle.canPlayAd( placementId ) )
        {
            showFullscreenAd( parameters, getContext( activity ), new InterstitialAdListener( listener ) );
            return;
        }

        log( "Interstitial ad not ready" );
        listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
    }

    //endregion

    //region MaxAppOpenAdapter

    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "app open ad for placement: " + placementId + "..." );

        if ( !Vungle.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing app open ad load..." );
            listener.onAppOpenAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( !isValidPlacement( parameters ) )
        {
            log( "App open ad failed to load due to an invalid placement id: " + placementId );
            listener.onAppOpenAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

            return;
        }

        if ( isBiddingAd )
        {
            if ( Vungle.canPlayAd( placementId, bidResponse ) )
            {
                log( "App open ad loaded" );
                listener.onAppOpenAdLoaded();

                return;
            }
        }
        else if ( Vungle.canPlayAd( placementId ) )
        {
            log( "App open ad loaded" );
            listener.onAppOpenAdLoaded();

            return;
        }

        updateUserPrivacySettings( parameters );
        loadFullscreenAd( parameters, getContext( activity ), new LoadAdCallback()
        {
            @Override
            public void onAdLoad(final String id)
            {
                log( "App open ad loaded" );
                listener.onAppOpenAdLoaded();
            }

            @Override
            public void onError(final String id, final VungleException exception)
            {
                MaxAdapterError error = toMaxError( exception );
                log( "App open ad for placement " + id + " failed to load with error: " + error );
                listener.onAppOpenAdLoadFailed( error );
            }
        } );
    }

    public void showAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + "app open ad for placement: " + placementId + "..." );

        final Context applicationContext = getContext( activity );

        if ( isBiddingAd )
        {
            if ( Vungle.canPlayAd( placementId, bidResponse ) )
            {
                showFullscreenAd( parameters, applicationContext, new AppOpenAdListener( listener ) );
                return;
            }
        }
        else if ( Vungle.canPlayAd( placementId ) )
        {
            showFullscreenAd( parameters, applicationContext, new AppOpenAdListener( listener ) );
            return;
        }

        log( "App open ad not ready" );
        listener.onAppOpenAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "App open ad not ready" ) );
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );

        if ( !Vungle.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( !isValidPlacement( parameters ) )
        {
            log( "Rewarded ad failed to load due to an invalid placement id: " + placementId );
            listener.onRewardedAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

            return;
        }

        if ( isBiddingAd )
        {
            if ( Vungle.canPlayAd( placementId, bidResponse ) )
            {
                log( "Rewarded ad loaded" );
                listener.onRewardedAdLoaded();

                return;
            }
        }
        else if ( Vungle.canPlayAd( placementId ) )
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();

            return;
        }

        updateUserPrivacySettings( parameters );
        loadFullscreenAd( parameters, getContext( activity ), new LoadAdCallback()
        {
            @Override
            public void onAdLoad(final String id)
            {
                log( "Rewarded ad loaded" );
                listener.onRewardedAdLoaded();
            }

            @Override
            public void onError(final String id, final VungleException exception)
            {
                MaxAdapterError error = toMaxError( exception );
                log( "Rewarded ad for placement " + id + " failed to load with error: " + error );
                listener.onRewardedAdLoadFailed( error );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );

        if ( isBiddingAd )
        {
            if ( Vungle.canPlayAd( placementId, bidResponse ) )
            {
                configureReward( parameters );
                showFullscreenAd( parameters, getContext( activity ), new RewardedAdListener( listener ) );

                return;
            }
        }
        else if ( Vungle.canPlayAd( placementId ) )
        {
            configureReward( parameters );
            showFullscreenAd( parameters, getContext( activity ), new RewardedAdListener( listener ) );

            return;
        }

        log( "Rewarded ad not ready" );
        listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String adFormatLabel = adFormat.getLabel();
        final String placementId = parameters.getThirdPartyAdPlacementId();

        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        if ( !Vungle.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing " + adFormatLabel + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( !isValidPlacement( parameters ) )
        {
            log( adFormatLabel + " ad failed to load due to an invalid placement id: " + placementId );
            listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

            return;
        }

        if ( isNative )
        {
            final Context applicationContext = getContext( activity );
            final NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( parameters, adFormat, applicationContext, listener );
            loadVungleNativeAd( parameters, applicationContext, nativeAdViewListener );

            return;
        }

        final PlayAdCallback playAdCallback = new AdViewAdListener( adFormatLabel, listener );
        final BannerAdConfig adConfig = new BannerAdConfig();
        final AdConfig.AdSize adSize = vungleAdSize( adFormat );
        adConfig.setAdSize( adSize );

        final Bundle serverParameters = parameters.getServerParameters();
        if ( serverParameters.containsKey( "is_muted" ) )
        {
            adConfig.setMuted( serverParameters.getBoolean( "is_muted" ) );
        }

        if ( isBiddingAd )
        {
            if ( Banners.canPlayAd( placementId, bidResponse, adSize ) )
            {
                showAdViewAd( adFormat, adConfig, parameters, listener, playAdCallback );
                return;
            }
        }
        else if ( Banners.canPlayAd( placementId, adSize ) )
        {
            showAdViewAd( adFormat, adConfig, parameters, listener, playAdCallback );
            return;
        }

        updateUserPrivacySettings( parameters );
        final LoadAdCallback loadAdCallback = new LoadAdCallback()
        {
            @Override
            public void onAdLoad(final String id)
            {
                showAdViewAd( adFormat, adConfig, parameters, listener, playAdCallback );
            }

            @Override
            public void onError(final String id, final VungleException exception)
            {
                MaxAdapterError error = toMaxError( exception );
                log( adFormatLabel + " ad for placement " + id + " failed to load with error: " + error );
                listener.onAdViewAdLoadFailed( error );
            }
        };

        // loadBanner() will fail with "VungleException: Operation was canceled" if a banner's placement ID is already in use
        if ( isBiddingAd )
        {
            Banners.loadBanner( placementId, bidResponse, adConfig, loadAdCallback );
        }
        else
        {
            Banners.loadBanner( placementId, adConfig, loadAdCallback );
        }
    }

    private void showAdViewAd(final MaxAdFormat adFormat,
                              final BannerAdConfig adConfig,
                              final MaxAdapterResponseParameters parameters,
                              final MaxAdViewAdapterListener listener,
                              final PlayAdCallback playAdCallback)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String adFormatLabel = adFormat.getLabel();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        if ( isBiddingAd )
        {
            adViewAd = Banners.getBanner( placementId, bidResponse, adConfig, playAdCallback );
        }
        else
        {
            adViewAd = Banners.getBanner( placementId, adConfig, playAdCallback );
        }

        if ( adViewAd != null )
        {
            log( adFormatLabel + " ad loaded" );
            adViewAd.setGravity( Gravity.CENTER );
            listener.onAdViewAdLoaded( adViewAd );
        }
        else
        {
            MaxAdapterError error = MaxAdapterError.INVALID_LOAD_STATE;
            log( adFormatLabel + " ad failed to load: " + error );
            listener.onAdViewAdLoadFailed( error );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        if ( !Vungle.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing interstitial ad load..." );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        if ( !isValidPlacement( parameters ) )
        {
            log( "Native ad failed to load due to an invalid placement id: " + placementId );
            listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

            return;
        }

        final Context applicationContext = getContext( activity );
        final NativeAdListener nativeAdListener = new NativeAdListener( parameters, applicationContext, listener );
        loadVungleNativeAd( parameters, applicationContext, nativeAdListener );
    }

    //endregion

    //region Helper Methods

    private void loadFullscreenAd(final MaxAdapterResponseParameters parameters, final Context context, final LoadAdCallback loadAdCallback)
    {
        AdConfig adConfig = createAdConfig( parameters.getServerParameters(), context );
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            Vungle.loadAd( placementId, bidResponse, adConfig, loadAdCallback );
        }
        else
        {
            Vungle.loadAd( placementId, adConfig, loadAdCallback );
        }
    }

    private void showFullscreenAd(final MaxAdapterResponseParameters parameters, final Context context, final PlayAdCallback adListener)
    {
        AdConfig adConfig = createAdConfig( parameters.getServerParameters(), context );
        String bidResponse = parameters.getBidResponse();
        String placementId = parameters.getThirdPartyAdPlacementId();

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            Vungle.playAd( placementId, bidResponse, adConfig, adListener );
        }
        else
        {
            Vungle.playAd( placementId, adConfig, adListener );
        }
    }

    private void loadVungleNativeAd(final MaxAdapterResponseParameters parameters, final Context context, final com.vungle.warren.NativeAdListener listener)
    {
        final AdConfig adConfig = new AdConfig();
        final String bidResponse = parameters.getBidResponse();
        final String placementId = parameters.getThirdPartyAdPlacementId();

        updateUserPrivacySettings( parameters );

        nativeAd = new NativeAd( context, placementId );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            nativeAd.loadAd( adConfig, bidResponse, listener );
        }
        else
        {
            nativeAd.loadAd( adConfig, listener );
        }
    }

    private AdConfig createAdConfig(final Bundle serverParameters, final Context context)
    {
        final AdConfig config = new AdConfig();
        if ( serverParameters.containsKey( "ordinal" ) )
        {
            config.setOrdinal( serverParameters.getInt( "ordinal" ) );
        }

        if ( serverParameters.containsKey( "immersive_mode" ) )
        {
            config.setImmersiveMode( serverParameters.getBoolean( "immersive_mode" ) );
        }

        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        if ( serverParameters.containsKey( "is_muted" ) ) // Introduced in 9.10.0
        {
            config.setMuted( serverParameters.getBoolean( "is_muted" ) );
        }

        config.setAdOrientation( vungleAdOrientation( context ) );

        return config;
    }

    private boolean isValidPlacement(final MaxAdapterResponseParameters parameters)
    {
        return Vungle.getValidPlacements().contains( parameters.getThirdPartyAdPlacementId() ) || parameters.isTesting();
    }

    private void updateUserPrivacySettings(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
        if ( hasUserConsent != null )
        {
            Vungle.Consent consentStatus = hasUserConsent ? Vungle.Consent.OPTED_IN : Vungle.Consent.OPTED_OUT;
            Vungle.updateConsentStatus( consentStatus, "" );
        }

        if ( AppLovinSdk.VERSION_CODE >= 9_11_00 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                Vungle.Consent ccpaStatus = isDoNotSell ? Vungle.Consent.OPTED_OUT : Vungle.Consent.OPTED_IN;
                Vungle.updateCCPAStatus( ccpaStatus );
            }
        }
    }

    private Boolean getPrivacySetting(final String privacySetting, final MaxAdapterParameters parameters)
    {
        try
        {
            // Use reflection because compiled adapters have trouble fetching `boolean` from old SDKs and `Boolean` from new SDKs (above 9.14.0)
            Class<?> parametersClass = parameters.getClass();
            Method privacyMethod = parametersClass.getMethod( privacySetting );
            return (Boolean) privacyMethod.invoke( parameters );
        }
        catch ( Exception exception )
        {
            log( "Error getting privacy setting " + privacySetting + " with exception: ", exception );
            return ( AppLovinSdk.VERSION_CODE >= 9_14_00_00 ) ? null : false;
        }
    }

    private static AdConfig.AdSize vungleAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdConfig.AdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdConfig.AdSize.BANNER_LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdConfig.AdSize.VUNGLE_MREC;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad view ad format: " + adFormat.getLabel() );
        }
    }

    private int vungleAdOrientation(final Context context)
    {
        int orientation = getOrientation( context );

        // 0 = PORTRAIT, 1 = LANDSCAPE, 2 = ALL/AUTO_ROTATE
        if ( orientation == Configuration.ORIENTATION_PORTRAIT )
        {
            return AdConfig.PORTRAIT;
        }
        else if ( orientation == Configuration.ORIENTATION_LANDSCAPE )
        {
            return AdConfig.LANDSCAPE;
        }
        else
        {
            return AdConfig.AUTO_ROTATE;
        }
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private int getOrientation(final Context context)
    {
        if ( context != null )
        {
            Resources resources = context.getResources();
            if ( resources != null )
            {
                Configuration configuration = resources.getConfiguration();
                if ( configuration != null )
                {
                    return configuration.orientation;
                }
            }
        }

        return Configuration.ORIENTATION_UNDEFINED;
    }

    private List<View> getClickableViews(final MaxNativeAdView maxNativeAdView)
    {
        List<View> clickableViews = new ArrayList<View>( 6 );
        if ( maxNativeAdView.getTitleTextView() != null ) clickableViews.add( maxNativeAdView.getTitleTextView() );
        if ( maxNativeAdView.getAdvertiserTextView() != null ) clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
        if ( maxNativeAdView.getBodyTextView() != null ) clickableViews.add( maxNativeAdView.getBodyTextView() );
        if ( maxNativeAdView.getIconImageView() != null ) clickableViews.add( maxNativeAdView.getIconImageView() );
        if ( maxNativeAdView.getCallToActionButton() != null ) clickableViews.add( maxNativeAdView.getCallToActionButton() );
        final View mediaContentView = ( AppLovinSdk.VERSION_CODE >= 11_00_00_00 ) ? maxNativeAdView.getMediaContentViewGroup() : maxNativeAdView.getMediaContentView();
        if ( mediaContentView != null ) clickableViews.add( mediaContentView );

        return clickableViews;
    }

    private static MaxAdapterError toMaxError(final VungleException vungleError)
    {
        final int vungleErrorCode = vungleError.getExceptionCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( vungleErrorCode )
        {
            case VungleException.NO_SERVE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case VungleException.UNKNOWN_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case VungleException.CONFIGURATION_ERROR:
            case VungleException.INCORRECT_BANNER_API_USAGE:
            case VungleException.INCORRECT_DEFAULT_API_USAGE:
            case VungleException.INCORRECT_DEFAULT_API_USAGE_NATIVE:
            case VungleException.INVALID_SIZE:
            case VungleException.MISSING_HBP_EVENT_ID:
            case VungleException.NETWORK_PERMISSIONS_NOT_GRANTED:
            case VungleException.NO_AUTO_CACHED_PLACEMENT:
            case VungleException.NO_SPACE_TO_DOWNLOAD_ASSETS:
            case VungleException.NO_SPACE_TO_LOAD_AD:
            case VungleException.NO_SPACE_TO_LOAD_AD_AUTO_CACHED:
            case VungleException.PLACEMENT_NOT_FOUND:
            case VungleException.SDK_VERSION_BELOW_REQUIRED_VERSION:
            case VungleException.UNSUPPORTED_CONFIGURATION:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case VungleException.AD_EXPIRED:
            case VungleException.AD_PAST_EXPIRATION:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case VungleException.APPLICATION_CONTEXT_REQUIRED:
            case VungleException.MISSING_REQUIRED_ARGUMENTS_FOR_INIT:
            case VungleException.NO_SPACE_TO_INIT:
            case VungleException.VUNGLE_NOT_INTIALIZED:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case VungleException.AD_UNABLE_TO_PLAY:
            case VungleException.OPERATION_CANCELED:
            case VungleException.CREATIVE_ERROR:
            case VungleException.OUT_OF_MEMORY:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case VungleException.AD_FAILED_TO_DOWNLOAD:
            case VungleException.AD_RENDER_NETWORK_ERROR:
            case VungleException.ASSET_DOWNLOAD_ERROR:
            case VungleException.ASSET_DOWNLOAD_RECOVERABLE:
            case VungleException.NETWORK_ERROR:
            case VungleException.NETWORK_UNREACHABLE:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case VungleException.DB_ERROR:
            case VungleException.SERVER_RETRY_ERROR:
            case VungleException.SERVER_ERROR:
            case VungleException.SERVER_TEMPORARY_UNAVAILABLE:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case VungleException.ALREADY_PLAYING_ANOTHER_AD:
            case VungleException.OPERATION_ONGOING:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case VungleException.RENDER_ERROR:
            case VungleException.WEBVIEW_RENDER_UNRESPONSIVE:
            case VungleException.WEB_CRASH:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), vungleErrorCode, vungleError.getLocalizedMessage() );
    }

    //endregion

    private class InterstitialAdListener
            implements PlayAdCallback
    {
        private final MaxInterstitialAdapterListener listener;

        private String creativeId;

        InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void creativeId(final String creativeId)
        {
            // This callback will fire just before onAdStart.
            log( "Interstitial ad with creative id: " + creativeId + " will be played" );
            this.creativeId = creativeId;
        }

        @Override
        public void onAdStart(final String id)
        {
            log( "Interstitial ad started" );
        }

        @Override
        public void onAdViewed(final String id)
        {
            log( "Interstitial ad displayed" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9_15_00_00 && AppLovinSdkUtils.isValidString( creativeId ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );

                listener.onInterstitialAdDisplayed( extraInfo );
            }
            else
            {
                listener.onInterstitialAdDisplayed();
            }
        }

        @Override
        public void onAdClick(final String id)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdLeftApplication(final String id)
        {
            log( "Interstitial ad left application" );
        }

        @Override
        public void onAdEnd(final String id)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onError(final String id, final VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            log( "Interstitial ad failed to display with error: " + error );
            listener.onInterstitialAdDisplayFailed( error );
        }

        @Override
        public void onAdRewarded(final String id)
        {
            // Interstitial ad listener does not use this method
        }

        @Override
        public void onAdEnd(final String id, final boolean completed, final boolean isCTAClicked)
        {
            // Deprecated callback
        }
    }

    private class AppOpenAdListener
            implements PlayAdCallback
    {
        private final MaxAppOpenAdapterListener listener;

        private String creativeId;

        AppOpenAdListener(final MaxAppOpenAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void creativeId(final String creativeId)
        {
            // This callback will fire just before onAdStart.
            log( "App open ad with creative id: " + creativeId + " will be played" );
            this.creativeId = creativeId;
        }

        @Override
        public void onAdStart(final String id)
        {
            log( "App open ad started" );
        }

        @Override
        public void onAdViewed(final String id)
        {
            log( "App open ad displayed" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9_15_00_00 && AppLovinSdkUtils.isValidString( creativeId ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );

                listener.onAppOpenAdDisplayed( extraInfo );
            }
            else
            {
                listener.onAppOpenAdDisplayed();
            }
        }

        @Override
        public void onAdClick(final String id)
        {
            log( "App open ad clicked" );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdLeftApplication(final String id)
        {
            log( "App open ad left application" );
        }

        @Override
        public void onAdEnd(final String id)
        {
            log( "App open ad hidden" );
            listener.onAppOpenAdHidden();
        }

        @Override
        public void onError(final String id, final VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            log( "App open ad failed to display with error: " + error );
            listener.onAppOpenAdDisplayFailed( error );
        }

        @Override
        public void onAdRewarded(final String id)
        {
            // App open ad listener does not use this method
        }

        @Override
        public void onAdEnd(final String id, final boolean completed, final boolean isCTAClicked)
        {
            // Deprecated callback
        }
    }

    private class RewardedAdListener
            implements PlayAdCallback
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;
        private String  creativeId;

        RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void creativeId(final String creativeId)
        {
            // This callback will fire just before onAdStart.
            log( "Rewarded ad with creative id: " + creativeId + " will be played" );
            this.creativeId = creativeId;
        }

        @Override
        public void onAdStart(final String id)
        {
            log( "Rewarded ad started" );
        }

        @Override
        public void onAdViewed(final String id)
        {
            log( "Rewarded ad displayed" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9_15_00_00 && AppLovinSdkUtils.isValidString( creativeId ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );

                listener.onRewardedAdDisplayed( extraInfo );
            }
            else
            {
                listener.onRewardedAdDisplayed();
            }

            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdClick(final String id)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdRewarded(final String id)
        {
            log( "Rewarded ad user did earn reward" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdLeftApplication(final String id)
        {
            log( "Rewarded ad left application" );
        }

        @Override
        public void onAdEnd(final String id)
        {
            log( "Rewarded ad video completed" );
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onError(final String id, final VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            log( "Rewarded ad failed to display with error: " + error );
            listener.onRewardedAdDisplayFailed( error );
        }

        @Override
        public void onAdEnd(final String id, final boolean completed, final boolean isCTAClicked)
        {
            // Deprecated callback
        }
    }

    private class AdViewAdListener
            implements PlayAdCallback
    {
        private final String                   adFormatLabel;
        private final MaxAdViewAdapterListener listener;

        private String creativeId;

        AdViewAdListener(final String adFormatLabel, final MaxAdViewAdapterListener listener)
        {
            this.adFormatLabel = adFormatLabel;
            this.listener = listener;
        }

        @Override
        public void creativeId(final String creativeId)
        {
            // This callback will fire just before onAdStart.
            log( adFormatLabel + "ad with creative id: " + creativeId + " will be played" );
            this.creativeId = creativeId;
        }

        @Override
        public void onAdStart(final String id)
        {
            log( adFormatLabel + " ad started" );
        }

        @Override
        public void onAdViewed(final String id)
        {
            log( adFormatLabel + " ad displayed" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9_15_00_00 && AppLovinSdkUtils.isValidString( creativeId ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );

                listener.onAdViewAdDisplayed( extraInfo );
            }
            else
            {
                listener.onAdViewAdDisplayed();
            }
        }

        @Override
        public void onAdClick(final String id)
        {
            log( adFormatLabel + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final String id)
        {
            log( adFormatLabel + " ad left application" );
        }

        @Override
        public void onAdEnd(final String id)
        {
            log( adFormatLabel + " ad hidden" );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onError(final String id, final VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            log( adFormatLabel + " ad display failed with error: " + error );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onAdRewarded(final String id)
        {
            // Ad view ad listener does not use this method
        }

        @Override
        public void onAdEnd(final String id, final boolean completed, final boolean isCTAClicked)
        {
            // Deprecated callback
        }
    }

    //region Native Ad View Ad Listener

    private class NativeAdViewListener
            implements com.vungle.warren.NativeAdListener
    {
        private final Context                  applicationContext;
        private final Bundle                   serverParameters;
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;
        private       String                   creativeId;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Context applicationContext, final MaxAdViewAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.adFormat = adFormat;
            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(final NativeAd ad)
        {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `ad` could be null if the user cannot get fill.
            if ( nativeAd == null || nativeAd != ad )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: no fill" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            if ( TextUtils.isEmpty( nativeAd.getAdTitle() ) )
            {
                e( "Native " + adFormat.getLabel() + " ad (" + nativeAd + ") does not have required assets." );
                listener.onAdViewAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded: " + nativeAd.getPlacementId() );

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final MediaView mediaView = new MediaView( applicationContext );
                    final String iconUrl = nativeAd.getAppIcon();

                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( adFormat )
                            .setTitle( nativeAd.getAdTitle() )
                            .setAdvertiser( nativeAd.getAdSponsoredText() )
                            .setBody( nativeAd.getAdBodyText() )
                            .setCallToAction( nativeAd.getAdCallToActionText() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( Uri.parse( iconUrl ) ) )
                            .setMediaView( mediaView );

                    final MaxVungleNativeAd maxVungleNativeAd = new MaxVungleNativeAd( builder );

                    // Backend will pass down `vertical` as the template to indicate using a vertical native template
                    final MaxNativeAdView maxNativeAdView;
                    final String templateName = BundleUtils.getString( "template", "", serverParameters );
                    if ( templateName.contains( "vertical" ) )
                    {
                        if ( AppLovinSdk.VERSION_CODE < 9_14_05_00 )
                        {
                            log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default horizontal native template will be used." );
                        }

                        if ( templateName.equals( "vertical" ) )
                        {
                            final String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                            maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd, verticalTemplateName, applicationContext );
                        }
                        else
                        {
                            maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd, templateName, applicationContext );
                        }
                    }
                    else if ( AppLovinSdk.VERSION_CODE < 9_14_05_00 )
                    {
                        maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd,
                                                               AppLovinSdkUtils.isValidString( templateName ) ? templateName : "no_body_banner_template",
                                                               applicationContext );
                    }
                    else
                    {
                        maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd,
                                                               AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                               applicationContext );
                    }

                    maxVungleNativeAd.prepareViewForInteraction( maxNativeAdView );

                    listener.onAdViewAdLoaded( maxNativeAdView );
                }
            } );
        }

        @Override
        public void onAdLoadError(final String placementId, final VungleException exception)
        {
            MaxAdapterError adapterError = toMaxError( exception );
            log( "Native " + adFormat.getLabel() + " ad failed to load with error " + adapterError + " with placement id: " + placementId );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdPlayError(final String placementId, final VungleException exception)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to play with error " + toMaxError( exception ) + " with placement id: " + placementId );
        }

        @Override
        public void creativeId(final String creativeId)
        {
            // This callback will fire just before onAdImpression.
            log( "Native " + adFormat.getLabel() + " ad with creative id: " + creativeId + " will be played" );
            this.creativeId = creativeId;
        }

        @Override
        public void onAdImpression(final String placementId)
        {
            log( "Native " + adFormat.getLabel() + " ad shown with placement id: " + placementId );

            Bundle extraInfo = null;
            if ( AppLovinSdkUtils.isValidString( creativeId ) )
            {
                extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );
            }

            listener.onAdViewAdDisplayed( extraInfo );
        }

        @Override
        public void onAdClick(final String placementId)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked with placement id: " + placementId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final String placementId)
        {
            log( "Native " + adFormat.getLabel() + " ad left application with placement id: " + placementId );
        }
    }

    //endregion

    //region Native Ad Listener

    private class NativeAdListener
            implements com.vungle.warren.NativeAdListener
    {
        private final Context                    applicationContext;
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;
        private       String                     creativeId;

        NativeAdListener(final MaxAdapterResponseParameters parameters, final Context applicationContext, final MaxNativeAdAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(final NativeAd ad)
        {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `ad` could be null if the user cannot get fill.
            if ( nativeAd == null || nativeAd != ad )
            {
                log( "Native ad failed to load: no fill" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getAdTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            log( "Native ad loaded: " + nativeAd.getPlacementId() );

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final MediaView mediaView = new MediaView( applicationContext );
                    final String iconUrl = nativeAd.getAppIcon();

                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getAdTitle() )
                            .setAdvertiser( nativeAd.getAdSponsoredText() )
                            .setBody( nativeAd.getAdBodyText() )
                            .setCallToAction( nativeAd.getAdCallToActionText() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( Uri.parse( iconUrl ) ) )
                            .setMediaView( mediaView );

                    final MaxVungleNativeAd maxVungleNativeAd = new MaxVungleNativeAd( builder );
                    listener.onNativeAdLoaded( maxVungleNativeAd, null );
                }
            } );
        }

        @Override
        public void onAdLoadError(final String placementId, final VungleException exception)
        {
            MaxAdapterError adapterError = toMaxError( exception );
            log( "Native ad failed to load with error " + adapterError + " with placement id: " + placementId );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdPlayError(final String placementId, final VungleException exception)
        {
            log( "Native ad failed to play with error " + toMaxError( exception ) + " with placement id: " + placementId );
        }

        @Override
        public void creativeId(final String creativeId)
        {
            // This callback will fire just before onAdImpression.
            log( "Native ad with creative id: " + creativeId + " will be played" );
            this.creativeId = creativeId;
        }

        @Override
        public void onAdImpression(final String placementId)
        {
            log( "Native ad shown with placement id: " + placementId );

            Bundle extraInfo = null;
            if ( AppLovinSdkUtils.isValidString( creativeId ) )
            {
                extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );
            }

            listener.onNativeAdDisplayed( extraInfo );
        }

        @Override
        public void onAdClick(final String placementId)
        {
            log( "Native ad clicked with placement id: " + placementId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdLeftApplication(final String placementId)
        {
            log( "Native ad left application with placement id: " + placementId );
        }
    }

    //endregion

    private class MaxVungleNativeAd
            extends MaxNativeAd
    {
        public MaxVungleNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            final NativeAd nativeAd = VungleMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return;
            }

            NativeAdLayout nativeAdLayout = new NativeAdLayout( maxNativeAdView.getContext() );
            View mainView = maxNativeAdView.getMainView();
            maxNativeAdView.removeView( mainView );
            nativeAdLayout.addView( mainView );
            maxNativeAdView.addView( nativeAdLayout );

            nativeAd.registerViewForInteraction( nativeAdLayout, (MediaView) getMediaView(), maxNativeAdView.getIconImageView(), VungleMediationAdapter.this.getClickableViews( maxNativeAdView ) );
        }
    }
}
