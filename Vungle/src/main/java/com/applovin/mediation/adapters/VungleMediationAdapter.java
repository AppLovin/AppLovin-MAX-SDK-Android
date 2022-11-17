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
import android.widget.ImageView;

import androidx.annotation.NonNull;
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
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;

import com.vungle.ads.AdConfig;
import com.vungle.ads.AdSize;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BannerView;
import com.vungle.ads.BaseAd;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.NativeAd;
import com.vungle.ads.NativeAdListener;
import com.vungle.ads.Plugin;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleException;
import com.vungle.ads.VungleSettings;
import com.vungle.ads.internal.privacy.PrivacyConsent;
import com.vungle.ads.internal.ui.view.MediaView;
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

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private BannerAd bannerAd;
    private NativeAd nativeAd;
    private InterstitialAd appOpenAd;

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

            Plugin.addWrapperInfo( Plugin.WrapperFramework.max, getAdapterVersion() );

            InitializationListener initCallback = new InitializationListener()
            {
                @Override
                public void onSuccess()
                {
                    log( "Vungle SDK initialized" );

                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }

                @Override
                public void onError(final @NonNull VungleException exception)
                {
                    log( "Vungle SDK failed to initialize with error: ", exception );

                    status = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( status, exception.getLocalizedMessage() );
                }
            };

            // Note: Vungle requires the Application Context
            VungleAds.init(getContext( activity ), appId, initCallback, new VungleSettings() );
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
        return getVersionString( com.vungle.ads.BuildConfig.class, "VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log("onDestroy()");
        if ( bannerAd != null )
        {
            bannerAd.finishAd();
            bannerAd = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.unregisterView();
            nativeAd = null;
        }

        interstitialAd = null;
        rewardedAd = null;
        appOpenAd = null;
    }

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserPrivacySettings( parameters );

        String signal = VungleAds.getBiddingToken( getContext(activity) );
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

        if ( !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        Context context = getContext(activity);
        AdConfig adConfig = createAdConfig(parameters.getServerParameters(), context);
        interstitialAd = new InterstitialAd(context, placementId, adConfig);
        interstitialAd.setAdListener(new VngMaxInterstitialAdListener(listener));
        interstitialAd.load(isBiddingAd ? bidResponse : null);
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        if (interstitialAd != null && interstitialAd.canPlayAd())
        {
            String bidResponse = parameters.getBidResponse();
            boolean isBiddingAd = AppLovinSdkUtils.isValidString(bidResponse);
            String placementId = parameters.getThirdPartyAdPlacementId();
            log("Showing " + (isBiddingAd ? "bidding " : "") + "interstitial ad for placement: " + placementId + "...");
            interstitialAd.play();
        } else {
            log("Interstitial ad not ready");
            listener.onInterstitialAdDisplayFailed(new MaxAdapterError(-4205, "Ad Display Failed"));
        }
    }

    //endregion

    //region MaxAppOpenAdapter

    @Override
    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final @NonNull MaxAppOpenAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "app open ad for placement: " + placementId + "..." );

        if ( !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing app open ad load..." );
            listener.onAppOpenAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        Context context = getContext(activity);
        AdConfig adConfig = createAdConfig(parameters.getServerParameters(), context);
        appOpenAd = new InterstitialAd(context, placementId, adConfig);
        appOpenAd.setAdListener(new VngMaxAppOpenAdListener(listener));
        appOpenAd.load(isBiddingAd ? bidResponse : null);
    }

    @Override
    public void showAppOpenAd(final @NonNull MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final @NonNull MaxAppOpenAdapterListener listener)
    {
        if (appOpenAd != null && appOpenAd.canPlayAd())
        {
            String bidResponse = parameters.getBidResponse();
            boolean isBiddingAd = AppLovinSdkUtils.isValidString(bidResponse);
            String placementId = parameters.getThirdPartyAdPlacementId();
            log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + "app open ad for placement: " + placementId + "..." );
            appOpenAd.play();
        } else {
            log( "App open ad not ready" );
            listener.onAppOpenAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
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

        if ( !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        Context context = getContext(activity);
        AdConfig adConfig = createAdConfig(parameters.getServerParameters(), context);
        rewardedAd = new RewardedAd(context, placementId, adConfig);
        rewardedAd.setAdListener(new VngMaxRewardedAdListener(listener));
        rewardedAd.load(isBiddingAd ? bidResponse : null);
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        if (rewardedAd != null && rewardedAd.canPlayAd())
        {
            String bidResponse = parameters.getBidResponse();
            boolean isBiddingAd = AppLovinSdkUtils.isValidString(bidResponse);
            String placementId = parameters.getThirdPartyAdPlacementId();
            log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );
            rewardedAd.play();
        } else {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String bidResponse = parameters.getBidResponse();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final String adFormatLabel = adFormat.getLabel();
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        if ( !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing " + adFormatLabel + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        Context context = getContext(activity);
        AdConfig adConfig = createAdConfig(parameters.getServerParameters(), context);
        AdSize adSize = vungleAdSize( adFormat );
        adConfig.setAdSize( adSize );
        bannerAd = new BannerAd(context, placementId, adConfig);
        bannerAd.setAdListener(new VngMaxAdViewAdListener(adFormatLabel, listener,
            () -> showAdViewAd(isBiddingAd, placementId, adFormatLabel, listener)));
        bannerAd.load(isBiddingAd ? bidResponse : null);
    }

    private void showAdViewAd(final boolean isBiddingAd, final String placementId, final String adFormatLabel, final MaxAdViewAdapterListener listener)
    {
        log( "Showing " + ( isBiddingAd ? "bidding " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        if (bannerAd != null && bannerAd.getBannerView() != null) {
            BannerView bannerView = bannerAd.getBannerView();
            bannerView.setGravity( Gravity.CENTER );
            listener.onAdViewAdLoaded( bannerView );
        } else {
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

        if ( !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing interstitial ad load..." );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        Context context = getContext(activity);
        AdConfig adConfig = createAdConfig(parameters.getServerParameters(), context);
        nativeAd = new NativeAd(context, placementId, adConfig);
        nativeAd.setAdListener(new VngMaxNativeAdListener(parameters, context, listener));
        nativeAd.load(isBiddingAd ? bidResponse : null);
    }

    //endregion

    //region Helper Methods

    private AdConfig createAdConfig(final Bundle serverParameters, final Context context)
    {
        final AdConfig config = new AdConfig();
        /*
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
        */
        config.setAdOrientation( vungleAdOrientation( context ) );

        return config;
    }

    private void updateUserPrivacySettings(final MaxAdapterParameters parameters)
    {
        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                PrivacyConsent consentStatus = hasUserConsent ? PrivacyConsent.OPT_IN : PrivacyConsent.OPT_OUT;
                VungleAds.updateGDPRConsent( consentStatus, "" );
            }
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                PrivacyConsent ccpaStatus = isDoNotSell ? PrivacyConsent.OPT_OUT : PrivacyConsent.OPT_IN;
                VungleAds.updateCCPAStatus( ccpaStatus );
            }
        }

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            VungleAds.updateUserCoppaStatus( isAgeRestrictedUser );
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
            return ( AppLovinSdk.VERSION_CODE >= 9140000 ) ? null : false;
        }
    }

    private static AdSize vungleAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.BANNER_LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.VUNGLE_MREC;
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

    private class VngMaxInterstitialAdListener implements InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;
        private boolean adLoaded = false;

        VngMaxInterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void adLoaded(final @NonNull BaseAd baseAd)
        {
            log("Interstitial ad loaded");
            adLoaded = true;
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void adStart(final @NonNull BaseAd baseAd)
        {
            log( "Interstitial ad started" );
        }

        @Override
        public void adImpression(final @NonNull BaseAd baseAd)
        {
            log( "Interstitial ad displayed" );
            String creativeId = baseAd.getCreativeId();
            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( creativeId ) )
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
        public void adClick(final @NonNull BaseAd baseAd)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdLeftApplication(final @NonNull BaseAd baseAd)
        {
            log( "Interstitial ad left application" );
        }

        @Override
        public void adEnd(final @NonNull BaseAd baseAd)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void error(final @NonNull BaseAd baseAd, final @NonNull VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            if (adLoaded) {
                log("Interstitial ad failed to display with error: " + error);
                listener.onInterstitialAdDisplayFailed(error);
            } else {
                log("Interstitial ad for placement " + baseAd.getPlacementId()
                    + " failed to load with error: " + error);
                listener.onInterstitialAdLoadFailed(error);
            }
        }
    }

    private class VngMaxAppOpenAdListener implements InterstitialAdListener
    {
        private final MaxAppOpenAdapterListener listener;
        private boolean adLoaded = false;

        VngMaxAppOpenAdListener(final MaxAppOpenAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void adLoaded(final @NonNull BaseAd baseAd) {
            log("App open ad loaded");
            adLoaded = true;
            listener.onAppOpenAdLoaded();
        }

        @Override
        public void adStart(final @NonNull BaseAd baseAd)
        {
            log( "App open ad started" );
        }

        @Override
        public void adImpression(final @NonNull BaseAd baseAd)
        {
            log( "App open ad displayed" );
            String creativeId = baseAd.getCreativeId();
            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( creativeId ) )
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
        public void adClick(final @NonNull BaseAd baseAd)
        {
            log( "App open ad clicked" );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdLeftApplication(final @NonNull BaseAd baseAd)
        {
            log( "App open ad left application" );
        }

        @Override
        public void adEnd(final @NonNull BaseAd baseAd)
        {
            log( "App open ad hidden" );
            listener.onAppOpenAdHidden();
        }

        @Override
        public void error(final @NonNull BaseAd baseAd, final @NonNull VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            if (adLoaded) {
                log("App open ad failed to display with error: " + error);
                listener.onAppOpenAdDisplayFailed(error);
            } else {
                log( "App open ad for placement " + baseAd.getPlacementId() + " failed to load with error: " + error );
                listener.onAppOpenAdLoadFailed( error );
            }
        }
    }

    private class VngMaxRewardedAdListener implements RewardedAdListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;
        private boolean adLoaded = false;

        VngMaxRewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void adLoaded(final @NonNull BaseAd baseAd)
        {
            log( "Rewarded ad loaded" );
            adLoaded = true;
            listener.onRewardedAdLoaded();
        }

        @Override
        public void adStart(final @NonNull BaseAd baseAd)
        {
            log( "Rewarded ad started" );
        }

        @Override
        public void adImpression(final @NonNull BaseAd baseAd)
        {
            log( "Rewarded ad displayed" );
            String creativeId = baseAd.getCreativeId();
            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( creativeId ) )
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
        public void adClick(final @NonNull BaseAd baseAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void adRewarded(final @NonNull BaseAd baseAd)
        {
            log( "Rewarded ad user did earn reward" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdLeftApplication(final @NonNull BaseAd baseAd)
        {
            log( "Rewarded ad left application" );
        }

        @Override
        public void adEnd(final @NonNull BaseAd baseAd)
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
        public void error(final @NonNull BaseAd baseAd, final @NonNull VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            if (adLoaded) {
                log("Rewarded ad failed to display with error: " + error);
                listener.onRewardedAdDisplayFailed(error);
            } else {
                log( "Rewarded ad for placement " + baseAd.getPlacementId() + " failed to load with error: " + error );
                listener.onRewardedAdLoadFailed( error );
            }
        }
    }

    private interface VungleBannerLoadWrapper {
        void onAdLoaded();
    }

    private class VngMaxAdViewAdListener implements BannerAdListener
    {
        private final String adFormatLabel;
        private final MaxAdViewAdapterListener listener;
        private final VungleBannerLoadWrapper bannerLoadWrapper;

        private boolean adLoaded;

        VngMaxAdViewAdListener(final String adFormatLabel, final MaxAdViewAdapterListener listener, final VungleBannerLoadWrapper wrapper)
        {
            this.adFormatLabel = adFormatLabel;
            this.listener = listener;
            this.bannerLoadWrapper = wrapper;
        }

        @Override
        public void adLoaded(@NonNull BaseAd baseAd) {
            adLoaded = true;
            log( adFormatLabel + " ad loaded" );
            if (bannerLoadWrapper != null) {
                bannerLoadWrapper.onAdLoaded();
            }
        }

        @Override
        public void adStart(final @NonNull BaseAd baseAd)
        {
            log( adFormatLabel + " ad started" );
        }

        @Override
        public void adImpression(final @NonNull BaseAd baseAd)
        {
            log( adFormatLabel + " ad displayed" );
            String creativeId = baseAd.getCreativeId();
            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( creativeId ) )
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
        public void adClick(final @NonNull BaseAd baseAd)
        {
            log( adFormatLabel + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final @NonNull BaseAd baseAd)
        {
            log( adFormatLabel + " ad left application" );
        }

        @Override
        public void adEnd(final @NonNull BaseAd baseAd)
        {
            log( adFormatLabel + " ad hidden" );
            listener.onAdViewAdHidden();
        }

        @Override
        public void error(final @NonNull BaseAd baseAd, final @NonNull VungleException exception)
        {
            MaxAdapterError error = toMaxError( exception );
            if (adLoaded) {
                log(adFormatLabel + " ad display failed with error: " + error);
                listener.onAdViewAdDisplayFailed(error);
            } else {
                log( adFormatLabel + " ad for placement " + baseAd.getPlacementId() + " failed to load with error: " + error );
                listener.onAdViewAdLoadFailed( error );
            }
        }
    }

    //region Native Ad Listener

    private class VngMaxNativeAdListener implements NativeAdListener
    {
        private final Context                    applicationContext;
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;
        private boolean adLoaded;

        VngMaxNativeAdListener(final MaxAdapterResponseParameters parameters, final Context applicationContext, final MaxNativeAdAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void adLoaded(final @NonNull BaseAd ad)
        {
            adLoaded = true;
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

            runOnUiThread(() -> {
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
            });
        }

        @Override
        public void adImpression(final @NonNull BaseAd baseAd)
        {
            log( "Native ad shown with placement id: " + baseAd.getPlacementId() );
            String creativeId = baseAd.getCreativeId();
            Bundle extraInfo = null;
            if ( AppLovinSdkUtils.isValidString( creativeId ) )
            {
                extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeId );
            }

            listener.onNativeAdDisplayed( extraInfo );
        }

        @Override
        public void adClick(final @NonNull BaseAd baseAd)
        {
            log( "Native ad clicked with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdLeftApplication(final @NonNull BaseAd baseAd)
        {
            log( "Native ad left application with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void error(final @NonNull BaseAd baseAd, final @NonNull VungleException exception)
        {
            MaxAdapterError adapterError = toMaxError( exception );
            if (adLoaded) {
                log( "Native ad failed to play with error " + toMaxError( exception ) + " with placement id: " + baseAd.getPlacementId() );
            } else {
                log( "Native ad failed to load with error " + adapterError + " with placement id: " + baseAd.getPlacementId() );
                listener.onNativeAdLoadFailed( adapterError );
            }
        }

        @Override
        public void adEnd(@NonNull BaseAd baseAd)
        {
            log( "Native ad end with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void adStart(@NonNull BaseAd baseAd)
        {
            log( "Native ad start with placement id: " + baseAd.getPlacementId() );
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

            final List<View> clickableViews = new ArrayList<>();
            if ( AppLovinSdkUtils.isValidString( getTitle() ) && maxNativeAdView.getTitleTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getTitleTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getAdvertiser() ) && maxNativeAdView.getAdvertiserTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getBody() ) && maxNativeAdView.getBodyTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getBodyTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
            {
                clickableViews.add( maxNativeAdView.getCallToActionButton() );
            }
            if ( getIcon() != null && maxNativeAdView.getIconImageView() != null )
            {
                clickableViews.add( maxNativeAdView.getIconImageView() );
            }
            if ( getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null )
            {
                clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );
            }

            nativeAd.registerViewForInteraction( maxNativeAdView, (MediaView) getMediaView(), (ImageView) getIconView(), clickableViews );
        }
    }
}