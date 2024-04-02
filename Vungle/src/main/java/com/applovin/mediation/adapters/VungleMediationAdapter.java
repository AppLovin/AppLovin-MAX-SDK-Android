package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

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
import com.vungle.ads.AdConfig;
import com.vungle.ads.BannerAd;
import com.vungle.ads.BannerAdListener;
import com.vungle.ads.BannerAdSize;
import com.vungle.ads.BannerView;
import com.vungle.ads.BaseAd;
import com.vungle.ads.InitializationListener;
import com.vungle.ads.InterstitialAd;
import com.vungle.ads.InterstitialAdListener;
import com.vungle.ads.NativeAd;
import com.vungle.ads.NativeAdListener;
import com.vungle.ads.RewardedAd;
import com.vungle.ads.RewardedAdListener;
import com.vungle.ads.VungleAds;
import com.vungle.ads.VungleError;
import com.vungle.ads.VunglePrivacySettings;
import com.vungle.ads.internal.ui.view.MediaView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

public class VungleMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, /* MaxAppOpenAdapter */ MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initializationStatus;

    private BannerAd       bannerAd;
    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private NativeAd       nativeAd;
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

            initializationStatus = InitializationStatus.INITIALIZING;

            VungleAds.setIntegrationName( VungleAds.WrapperFramework.max, getAdapterVersion() );

            // Note: Vungle requires the Application Context
            VungleAds.init( getContext( activity ), appId, new InitializationListener()
            {
                @Override
                public void onSuccess()
                {
                    log( "Vungle SDK initialized" );

                    initializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( initializationStatus, null );
                }

                @Override
                public void onError(final VungleError vungleError)
                {
                    log( "Vungle SDK failed to initialize with error: ", vungleError );

                    initializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( initializationStatus, vungleError.getErrorMessage() );
                }
            } );
        }
        else
        {
            log( "Vungle SDK already initialized" );
            onCompletionListener.onCompletion( initializationStatus, null );
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
        if ( bannerAd != null )
        {
            bannerAd.setAdListener( null );
            bannerAd.finishAd();
            bannerAd = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.setAdListener( null );
            nativeAd.unregisterView();
            nativeAd = null;
        }

        if ( interstitialAd != null )
        {
            interstitialAd.setAdListener( null );
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setAdListener( null );
            rewardedAd = null;
        }

        if ( appOpenAd != null )
        {
            appOpenAd.setAdListener( null );
            appOpenAd = null;
        }
    }

    //region Signal Collection

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserPrivacySettings( parameters );

        String signal = VungleAds.getBiddingToken( getContext( activity ) );
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

        interstitialAd = new InterstitialAd( getContext( activity ), placementId, new AdConfig() );
        interstitialAd.setAdListener( new InterstitialListener( listener ) );

        interstitialAd.load( bidResponse );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        if ( interstitialAd != null && interstitialAd.canPlayAd() )
        {
            log( "Showing interstitial ad for placement: " + parameters.getThirdPartyAdPlacementId() + "..." );
            interstitialAd.play( getContext( activity ) );
        }
        else
        {
            log( "Interstitial ad is not ready: " + parameters.getThirdPartyAdPlacementId() + "..." );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad is not ready" ) );
        }
    }

    //endregion

    //region MaxAppOpenAdapter

    public void loadAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
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

        appOpenAd = new InterstitialAd( getContext( activity ), placementId, new AdConfig() );
        appOpenAd.setAdListener( new AppOpenAdListener( listener ) );

        appOpenAd.load( bidResponse );
    }

    public void showAppOpenAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxAppOpenAdapterListener listener)
    {
        if ( appOpenAd != null && appOpenAd.canPlayAd() )
        {
            log( "Showing app open ad for placement: " + parameters.getThirdPartyAdPlacementId() + "..." );
            appOpenAd.play( getContext( activity ) );
        }
        else
        {
            log( "App open ad is not ready: " + parameters.getThirdPartyAdPlacementId() + "..." );
            listener.onAppOpenAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "App open ad is not ready" ) );
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

        rewardedAd = new RewardedAd( getContext( activity ), placementId, new AdConfig() );
        rewardedAd.setAdListener( new RewardedListener( listener ) );

        rewardedAd.load( bidResponse );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        if ( rewardedAd != null && rewardedAd.canPlayAd() )
        {
            log( "Showing rewarded ad for placement: " + parameters.getThirdPartyAdPlacementId() + "..." );

            configureReward( parameters );
            rewardedAd.play( getContext( activity ) );
        }
        else
        {
            log( "Rewarded ad is not ready: " + parameters.getThirdPartyAdPlacementId() + "..." );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad is not ready" ) );
        }
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String bidResponse = parameters.getBidResponse();
        final String adFormatLabel = adFormat.getLabel();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final Context context = getContext( activity );

        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        if ( !VungleAds.isInitialized() )
        {
            log( "Vungle SDK not successfully initialized: failing " + adFormatLabel + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateUserPrivacySettings( parameters );

        if ( isNative )
        {
            final NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( parameters, adFormat, context, listener );
            nativeAd = new NativeAd( activity, placementId );
            nativeAd.setAdListener( nativeAdViewListener );

            nativeAd.load( bidResponse );

            return;
        }

        BannerAdSize adSize = vungleAdSize( adFormat );
        bannerAd = new BannerAd( context, placementId, adSize );
        bannerAd.setAdListener( new AdViewAdListener( adFormatLabel, listener ) );

        bannerAd.load( bidResponse );
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

        nativeAd = new NativeAd( activity, placementId );
        nativeAd.setAdListener( new NativeListener( parameters, getContext( activity ), listener ) );

        nativeAd.load( bidResponse );
    }

    //endregion

    //region Helper Methods

    private void updateUserPrivacySettings(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            VunglePrivacySettings.setGDPRStatus( hasUserConsent, "" );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            VunglePrivacySettings.setCCPAStatus( !isDoNotSell );
        }

        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            VunglePrivacySettings.setCOPPAStatus( isAgeRestrictedUser );
        }
    }

    private static BannerAdSize vungleAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return BannerAdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return BannerAdSize.BANNER_LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return BannerAdSize.VUNGLE_MREC;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad view ad format: " + adFormat.getLabel() );
        }
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private List<View> getClickableViews(final MaxNativeAdView maxNativeAdView)
    {
        final List<View> clickableViews = new ArrayList<>( 6 );
        if ( maxNativeAdView.getTitleTextView() != null ) clickableViews.add( maxNativeAdView.getTitleTextView() );
        if ( maxNativeAdView.getAdvertiserTextView() != null ) clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
        if ( maxNativeAdView.getBodyTextView() != null ) clickableViews.add( maxNativeAdView.getBodyTextView() );
        if ( maxNativeAdView.getCallToActionButton() != null ) clickableViews.add( maxNativeAdView.getCallToActionButton() );
        if ( maxNativeAdView.getIconImageView() != null ) clickableViews.add( maxNativeAdView.getIconImageView() );
        final View mediaContentView = ( AppLovinSdk.VERSION_CODE >= 11_00_00_00 ) ? maxNativeAdView.getMediaContentViewGroup() : maxNativeAdView.getMediaContentView();
        if ( mediaContentView != null ) clickableViews.add( mediaContentView );

        return clickableViews;
    }

    private static MaxAdapterError toMaxError(final VungleError vungleError)
    {
        final int vungleErrorCode = vungleError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( vungleErrorCode )
        {
            case VungleError.NO_SERVE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case VungleError.UNKNOWN_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case VungleError.INVALID_APP_ID:
            case VungleError.CONFIGURATION_ERROR:
            case VungleError.INVALID_SIZE:
            case VungleError.NETWORK_PERMISSIONS_NOT_GRANTED:
            case VungleError.NO_SPACE_TO_DOWNLOAD_ASSETS:
            case VungleError.PLACEMENT_NOT_FOUND:
            case VungleError.SDK_VERSION_BELOW_REQUIRED_VERSION:
            case VungleError.PLACEMENT_AD_TYPE_MISMATCH:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case VungleError.AD_EXPIRED:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case VungleError.SDK_NOT_INITIALIZED:
            case VungleError.CURRENTLY_INITIALIZING:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case VungleError.AD_UNABLE_TO_PLAY:
            case VungleError.CREATIVE_ERROR:
            case VungleError.OUT_OF_MEMORY:
            case VungleError.AD_MARKUP_INVALID:
            case VungleError.MRAID_DOWNLOAD_JS_ERROR:
            case VungleError.ASSET_RESPONSE_DATA_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case VungleError.AD_FAILED_TO_DOWNLOAD:
            case VungleError.AD_RENDER_NETWORK_ERROR:
            case VungleError.ASSET_DOWNLOAD_ERROR:
            case VungleError.NETWORK_ERROR:
            case VungleError.NETWORK_UNREACHABLE:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case VungleError.SERVER_RETRY_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case VungleError.ALREADY_PLAYING_ANOTHER_AD:
            case VungleError.INVALID_AD_STATE:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case VungleError.WEBVIEW_RENDER_UNRESPONSIVE:
            case VungleError.WEB_CRASH:
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
            case VungleError.NETWORK_TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), vungleErrorCode, vungleError.getLocalizedMessage() );
    }

    @Nullable
    private Bundle maybeCreateExtraInfoBundle(final BaseAd baseAd)
    {
        String creativeId = baseAd.getCreativeId();

        if ( TextUtils.isEmpty( creativeId ) ) return null;

        Bundle extraInfo = new Bundle( 1 );
        extraInfo.putString( "creative_id", creativeId );

        return extraInfo;
    }

    //endregion

    private class InterstitialListener
            implements InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd baseAd)
        {
            log( "Interstitial ad loaded" );

            Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
            listener.onInterstitialAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( "Interstitial ad failed to load with error: " + error );
            listener.onInterstitialAdLoadFailed( error );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "Interstitial ad started" );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "Interstitial ad displayed" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( "Interstitial ad failed to display with error: " + error );
            listener.onInterstitialAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "Interstitial ad left application" );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }
    }

    private class AppOpenAdListener
            implements InterstitialAdListener
    {
        private final MaxAppOpenAdapterListener listener;

        AppOpenAdListener(final MaxAppOpenAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd baseAd)
        {
            log( "App Open ad loaded" );

            Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
            listener.onAppOpenAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( "App Open ad failed to load with error: " + error );
            listener.onAppOpenAdLoadFailed( error );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "App Open ad started" );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "App Open ad displayed" );
            listener.onAppOpenAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( "App Open ad failed to display with error: " + error );
            listener.onAppOpenAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "App Open ad clicked" );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "App Open ad left application" );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "App Open ad hidden" );
            listener.onAppOpenAdHidden();
        }
    }

    private class RewardedListener
            implements RewardedAdListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd baseAd)
        {
            log( "Rewarded ad loaded" );

            Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
            listener.onRewardedAdLoaded( extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( "Rewarded ad failed to load with error: " + error );
            listener.onRewardedAdLoadFailed( error );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "Rewarded ad started" );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "Rewarded ad displayed" );

            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( "Rewarded ad failed to display with error: " + error );
            listener.onRewardedAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "Rewarded ad left application" );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "Rewarded ad video completed" );

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
        public void onAdRewarded(final BaseAd baseAd)
        {
            log( "User was rewarded" );
            hasGrantedReward = true;
        }
    }

    private class AdViewAdListener
            implements BannerAdListener
    {
        private final String                   adFormatLabel;
        private final MaxAdViewAdapterListener listener;

        AdViewAdListener(final String adFormatLabel, final MaxAdViewAdapterListener listener)
        {
            this.adFormatLabel = adFormatLabel;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd baseAd)
        {
            log( "Showing " + adFormatLabel + " ad for placement: " + baseAd.getPlacementId() + "..." );

            if ( bannerAd != null && bannerAd.getBannerView() != null )
            {
                BannerView bannerView = bannerAd.getBannerView();
                bannerView.setGravity( Gravity.CENTER );
                log( adFormatLabel + " ad loaded" );

                Bundle extraInfo = maybeCreateExtraInfoBundle( baseAd );
                listener.onAdViewAdLoaded( bannerView, extraInfo );
            }
            else
            {
                MaxAdapterError error = MaxAdapterError.INVALID_LOAD_STATE;
                log( adFormatLabel + " ad failed to load: " + error );
                listener.onAdViewAdLoadFailed( error );
            }
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( adFormatLabel + " ad failed to load with error: " + error );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( adFormatLabel + " ad started" );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( adFormatLabel + " ad displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError error = toMaxError( vungleError );
            log( adFormatLabel + " ad display failed with error: " + error );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( adFormatLabel + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( adFormatLabel + " ad left application" );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( adFormatLabel + " ad hidden" );
            listener.onAdViewAdHidden();
        }
    }

    private class NativeAdViewListener
            implements NativeAdListener
    {
        private final Context                  applicationContext;
        private final Bundle                   serverParameters;
        private final MaxAdFormat              adFormat;
        private final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Context applicationContext, final MaxAdViewAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.adFormat = adFormat;
            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd ad)
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
            else
            {
                maxNativeAdView = new MaxNativeAdView( maxVungleNativeAd,
                                                       AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                       applicationContext );
            }

            maxVungleNativeAd.prepareViewForInteraction( maxNativeAdView );

            Bundle extraInfo = maybeCreateExtraInfoBundle( ad );
            listener.onAdViewAdLoaded( maxNativeAdView, extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError adapterError = toMaxError( vungleError );
            log( "Native " + adFormat.getLabel() + " ad failed to load with error " + adapterError + " with placement id: " + baseAd.getPlacementId() );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad start with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad shown with placement id: " + baseAd.getPlacementId() );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, final VungleError vungleError)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to play with error " + toMaxError( vungleError ) + " with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked with placement id: " + baseAd.getPlacementId() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad left application with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "Native " + adFormat.getLabel() + " ad end with placement id: " + baseAd.getPlacementId() );
        }
    }

    private class NativeListener
            implements NativeAdListener
    {
        private final Context                    applicationContext;
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;

        NativeListener(final MaxAdapterResponseParameters parameters, final Context applicationContext, final MaxNativeAdAdapterListener listener)
        {
            serverParameters = parameters.getServerParameters();

            this.applicationContext = applicationContext;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final BaseAd ad)
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

            Bundle extraInfo = maybeCreateExtraInfoBundle( ad );
            listener.onNativeAdLoaded( maxVungleNativeAd, extraInfo );
        }

        @Override
        public void onAdFailedToLoad(final BaseAd baseAd, final VungleError vungleError)
        {
            MaxAdapterError adapterError = toMaxError( vungleError );
            log( "Native ad failed to load with error " + adapterError + " with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdStart(final BaseAd baseAd)
        {
            log( "Native ad start with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdImpression(final BaseAd baseAd)
        {
            log( "Native ad shown with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdFailedToPlay(final BaseAd baseAd, final VungleError vungleError)
        {
            log( "Native ad failed to play with error " + toMaxError( vungleError ) + " with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdClicked(final BaseAd baseAd)
        {
            log( "Native ad clicked with placement id: " + baseAd.getPlacementId() );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdLeftApplication(final BaseAd baseAd)
        {
            log( "Native ad left application with placement id: " + baseAd.getPlacementId() );
        }

        @Override
        public void onAdEnd(final BaseAd baseAd)
        {
            log( "Native ad end with placement id: " + baseAd.getPlacementId() );
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

            if ( !nativeAd.canPlayAd() )
            {
                e( "Failed to play native ad or native ad is registered." );
                return;
            }

            View mediaView = getMediaView();
            if ( mediaView == null )
            {
                e( "Failed to register native ad views: mediaView is null." );
                return;
            }

            if ( mediaView.getParent() != null )
            {
                ( (ViewGroup) mediaView.getParent() ).removeView( mediaView );
            }

            ViewGroup contentViewGroup = maxNativeAdView.getMediaContentViewGroup();
            if ( contentViewGroup != null )
            {
                contentViewGroup.removeAllViews();
                contentViewGroup.addView( mediaView );
            }

            nativeAd.registerViewForInteraction( maxNativeAdView, (MediaView) mediaView, maxNativeAdView.getIconImageView(), VungleMediationAdapter.this.getClickableViews( maxNativeAdView ) );
        }
    }
}
