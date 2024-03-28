package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import com.applovin.mediation.adapter.MaxRewardedInterstitialAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdExperienceType;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BidderTokenProvider;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdExtendedListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdBase;
import com.facebook.ads.NativeAdListener;
import com.facebook.ads.NativeAdView;
import com.facebook.ads.NativeBannerAd;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdExtendedListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.isValidString;
import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

/**
 * Copyright © 2019 AppLovin Corporation. All rights reserved.
 */
public class FacebookMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxSignalProvider /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private static InitializationStatus sStatus;

    private AdView          mAdView;
    private NativeAd        mNativeAd;
    private NativeBannerAd  mNativeBannerAd;
    private InterstitialAd  mInterstitialAd;
    private RewardedVideoAd mRewardedVideoAd;
    private RewardedVideoAd mRewardedInterAd;

    private final AtomicBoolean onInterstitialAdHiddenCalled = new AtomicBoolean();
    private final AtomicBoolean onRewardedAdHiddenCalled     = new AtomicBoolean();

    // Explicit default constructor declaration
    public FacebookMediationAdapter(final AppLovinSdk sdk)
    {
        super( sdk );
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        // Update ad settings
        updateAdSettings( parameters );

        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            sStatus = InitializationStatus.INITIALIZING;

            final List<String> placementIds = parameters.getServerParameters().getStringArrayList( "placement_ids" );
            final AudienceNetworkAds.InitListener initListener = new AudienceNetworkAds.InitListener()
            {
                @Override
                public void onInitialized(final AudienceNetworkAds.InitResult initResult)
                {
                    if ( initResult.isSuccess() )
                    {
                        log( "Facebook SDK successfully finished initialization: " + initResult.getMessage() );

                        sStatus = InitializationStatus.INITIALIZED_SUCCESS;
                        onCompletionListener.onCompletion( sStatus, null );
                    }
                    else
                    {
                        log( "Facebook SDK failed to finished initialization: " + initResult.getMessage() );

                        sStatus = InitializationStatus.INITIALIZED_FAILURE;
                        onCompletionListener.onCompletion( sStatus, initResult.getMessage() );
                    }
                }
            };

            if ( parameters.isTesting() )
            {
                AdSettings.setDebugBuild( true );
            }

            log( "Initializing Facebook SDK with placements: " + placementIds );

            AudienceNetworkAds.buildInitSettings( getContext( activity ) )
                    .withMediationService( getMediationIdentifier() )
                    .withPlacementIds( placementIds )
                    .withInitListener( initListener )
                    .initialize();
        }
        else
        {
            log( "Facebook attempted initialization already - marking initialization as completed" );

            onCompletionListener.onCompletion( sStatus, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( com.facebook.ads.BuildConfig.class, "VERSION_NAME" );
    }

    @Override
    public String getAdapterVersion()
    {
        return com.applovin.mediation.adapters.facebook.BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( mInterstitialAd != null )
        {
            mInterstitialAd.destroy();
            mInterstitialAd = null;
        }

        if ( mRewardedVideoAd != null )
        {
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }

        if ( mRewardedInterAd != null )
        {
            mRewardedInterAd.destroy();
            mRewardedInterAd = null;
        }

        if ( mAdView != null )
        {
            mAdView.destroy();
            mAdView = null;
        }

        if ( mNativeAd != null )
        {
            mNativeAd.unregisterView();
            mNativeAd.destroy();
            mNativeAd = null;
        }

        if ( mNativeBannerAd != null )
        {
            mNativeBannerAd.unregisterView();
            mNativeBannerAd.destroy();
            mNativeBannerAd = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateAdSettings( parameters );

        // Must be ran on bg thread
        String signal = BidderTokenProvider.getBidderToken( getContext( activity ) );
        callback.onSignalCollected( signal );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading interstitial ad: " + placementId + "..." );

        updateAdSettings( parameters );

        mInterstitialAd = new InterstitialAd( activity.getApplicationContext(), placementId );
        InterstitialAd.InterstitialAdLoadConfigBuilder adLoadConfigBuilder = mInterstitialAd.buildLoadAdConfig().withAdListener( new InterstitialAdListener( listener ) );

        if ( mInterstitialAd.isAdLoaded() && !mInterstitialAd.isAdInvalidated() )
        {
            log( "An interstitial ad has been loaded already" );
            listener.onInterstitialAdLoaded();
        }
        else
        {
            log( "Loading bidding interstitial ad..." );
            mInterstitialAd.loadAd( adLoadConfigBuilder.withBid( parameters.getBidResponse() ).build() );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: " + parameters.getThirdPartyAdPlacementId() + "..." );

        if ( mInterstitialAd != null && mInterstitialAd.isAdLoaded() )
        {
            // Check if ad is already expired or invalidated, and do not show ad if that is
            // the case. You will not get paid to show an invalidated ad.
            if ( !mInterstitialAd.isAdInvalidated() )
            {
                mInterstitialAd.show();
            }
            else
            {
                log( "Unable to show interstitial - ad expired..." );
                listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_EXPIRED );
            }
        }
        else
        {
            log( "Unable to show interstitial - no ad loaded..." );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    @Override
    public void loadRewardedInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded interstitial: " + placementId + "..." );

        updateAdSettings( parameters );

        mRewardedInterAd = new RewardedVideoAd( activity.getApplicationContext(), placementId );
        RewardedVideoAd.RewardedVideoAdLoadConfigBuilder adLoadConfigBuilder = mRewardedInterAd.buildLoadAdConfig()
                .withAdExperience( AdExperienceType.AD_EXPERIENCE_TYPE_REWARDED_INTERSTITIAL )
                .withAdListener( new RewardedVideoAdExtendedListener()
                {
                    private boolean hasGrantedReward;

                    @Override
                    public void onAdLoaded(final Ad ad)
                    {
                        log( "Rewarded interstitial ad loaded: " + placementId );
                        listener.onRewardedInterstitialAdLoaded();
                    }

                    @Override
                    public void onError(final Ad ad, final AdError adError)
                    {
                        MaxAdapterError adapterError = toMaxError( adError );
                        log( "Rewarded interstitial ad (" + placementId + ") failed to load with error: " + adapterError );
                        listener.onRewardedInterstitialAdLoadFailed( adapterError );
                    }

                    @Override
                    public void onAdClicked(final Ad ad)
                    {
                        log( "Rewarded interstitial ad clicked: " + placementId );
                        listener.onRewardedInterstitialAdClicked();
                    }

                    @Override
                    public void onRewardedVideoClosed()
                    {
                        if ( onRewardedAdHiddenCalled.compareAndSet( false, true ) )
                        {
                            if ( hasGrantedReward || shouldAlwaysRewardUser() )
                            {
                                final MaxReward reward = getReward();
                                log( "Rewarded user with reward: " + reward );
                                listener.onUserRewarded( reward );
                            }

                            log( "Rewarded interstitial ad hidden: " + placementId );
                            listener.onRewardedInterstitialAdHidden();
                        }
                        else
                        {
                            log( "Rewarded interstitial ad hidden: " + placementId );
                        }
                    }

                    @Override
                    public void onRewardedVideoCompleted()
                    {
                        log( "Rewarded interstitial ad video completed: " + placementId );

                        hasGrantedReward = true;
                    }

                    @Override
                    public void onLoggingImpression(final Ad ad)
                    {
                        log( "Rewarded interstitial ad logging impression: " + placementId );

                        listener.onRewardedInterstitialAdDisplayed();
                    }

                    @Override
                    public void onRewardedVideoActivityDestroyed()
                    {
                        log( "Rewarded interstitial ad Activity destroyed: " + placementId );

                        //
                        // We will not reward the user if Activity is destroyed - this may be due to launching from app icon and having the `android:launchMode="singleTask"` flag
                        //

                        if ( onRewardedAdHiddenCalled.compareAndSet( false, true ) )
                        {
                            listener.onRewardedInterstitialAdHidden();
                        }
                    }
                } );

        if ( mRewardedInterAd.isAdLoaded() && !mRewardedInterAd.isAdInvalidated() )
        {
            log( "A rewarded interstitial ad has been loaded already" );
            listener.onRewardedInterstitialAdLoaded();
        }
        else
        {
            log( "Loading bidding rewarded interstitial ad..." );
            mRewardedInterAd.loadAd( adLoadConfigBuilder.withBid( parameters.getBidResponse() ).build() );
        }
    }

    @Override
    public void showRewardedInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedInterstitialAdapterListener listener)
    {
        log( "Showing rewarded interstitial ad: " + parameters.getThirdPartyAdPlacementId() + "..." );

        if ( mRewardedInterAd != null && mRewardedInterAd.isAdLoaded() )
        {
            // Check if ad is already expired or invalidated, and do not show ad if that is the case. You will not get paid to show an invalidated ad.
            if ( !mRewardedInterAd.isAdInvalidated() )
            {
                // Configure userReward from server.
                configureReward( parameters );

                mRewardedInterAd.show();
            }
            else
            {
                log( "Unable to show rewarded interstitial ad - ad expired..." );
                listener.onRewardedInterstitialAdDisplayFailed( MaxAdapterError.AD_EXPIRED );
            }
        }
        else
        {
            log( "Unable to show rewarded interstitial ad - no ad loaded..." );
            listener.onRewardedInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded Interstitial ad not ready" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded: " + placementId + "..." );

        updateAdSettings( parameters );

        mRewardedVideoAd = new RewardedVideoAd( activity.getApplicationContext(), placementId );
        RewardedVideoAd.RewardedVideoAdLoadConfigBuilder adLoadConfigBuilder = mRewardedVideoAd.buildLoadAdConfig().withAdListener( new RewardedAdListener( listener ) );

        if ( mRewardedVideoAd.isAdLoaded() && !mRewardedVideoAd.isAdInvalidated() )
        {
            log( "A rewarded ad has been loaded already" );
            listener.onRewardedAdLoaded();
        }
        else
        {
            log( "Loading bidding rewarded ad..." );
            mRewardedVideoAd.loadAd( adLoadConfigBuilder.withBid( parameters.getBidResponse() ).build() );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: " + parameters.getThirdPartyAdPlacementId() + "..." );

        if ( mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded() )
        {
            // Check if ad is already expired or invalidated, and do not show ad if that is the case. You will not get paid to show an invalidated ad.
            if ( !mRewardedVideoAd.isAdInvalidated() )
            {
                // Configure userReward from server.
                configureReward( parameters );

                mRewardedVideoAd.show();
            }
            else
            {
                log( "Unable to show rewarded ad - ad expired..." );
                listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_EXPIRED );
            }
        }
        else
        {
            log( "Unable to show rewarded ad - no ad loaded..." );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading" + ( isNative ? " native " : " " ) + adFormat.getLabel() + " ad: " + placementId + "..." );

        updateAdSettings( parameters );

        // NOTE: FB native is no longer supported in banners but is kept in for backwards compatibility for existing users.
        if ( isNative )
        {
            mNativeAd = new NativeAd( getContext( activity ), placementId );
            mNativeAd.loadAd( mNativeAd.buildLoadAdConfig()
                                      .withAdListener( new NativeAdViewListener( parameters.getServerParameters(), adFormat, activity, listener ) )
                                      .withBid( parameters.getBidResponse() )
                                      .build() );
        }
        else
        {
            mAdView = new AdView( getContext( activity ), placementId, toAdSize( adFormat ) );
            mAdView.loadAd( mAdView.buildLoadAdConfig()
                                    .withAdListener( new AdViewListener( adFormat, listener ) )
                                    .withBid( parameters.getBidResponse() )
                                    .build() );
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final Bundle serverParameters = parameters.getServerParameters();
        final boolean isNativeBanner = BundleUtils.getBoolean( "is_native_banner", serverParameters );
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading native " + ( isNativeBanner ? "banner " : "" ) + "ad: " + placementId + "..." );

        updateAdSettings( parameters );

        final Context context = getContext( activity );

        if ( isNativeBanner )
        {
            mNativeBannerAd = new NativeBannerAd( context, placementId );
            mNativeBannerAd.loadAd( mNativeBannerAd.buildLoadAdConfig()
                                            .withAdListener( new MaxNativeAdListener( parameters.getServerParameters(), context, listener ) )
                                            .withBid( parameters.getBidResponse() )
                                            .build() );
        }
        else
        {
            mNativeAd = new NativeAd( context, placementId );
            mNativeAd.loadAd( mNativeAd.buildLoadAdConfig()
                                      .withAdListener( new MaxNativeAdListener( parameters.getServerParameters(), context, listener ) )
                                      .withBid( parameters.getBidResponse() )
                                      .build() );
        }
    }

    private AdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.BANNER_HEIGHT_50;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.BANNER_HEIGHT_90;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.RECTANGLE_HEIGHT_250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private void updateAdSettings(final MaxAdapterParameters parameters)
    {
        final Bundle serverParameters = parameters.getServerParameters();

        if ( serverParameters.containsKey( "video_autoplay" ) )
        {
            final boolean videoAutoplay = serverParameters.getBoolean( "video_autoplay" );
            AdSettings.setVideoAutoplay( videoAutoplay );
        }

        // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
        // https://support.google.com/googleplay/android-developer/answer/9283445?hl=en
        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            AdSettings.setMixedAudience( isAgeRestrictedUser );
        }

        final String testDevicesString = serverParameters.getString( "test_device_ids", null );
        if ( !TextUtils.isEmpty( testDevicesString ) )
        {
            final List<String> testDeviceList = Arrays.asList( testDevicesString.split( "," ) );
            AdSettings.addTestDevices( testDeviceList );
        }

        // Update mediation service
        AdSettings.setMediationService( getMediationIdentifier() );
    }

    private static MaxAdapterError toMaxError(final AdError facebookError)
    {
        final int facebookErrorCode = facebookError.getErrorCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;

        // Facebook's SDK sometimes creates new instances of their pre-defined enums, so we should extract the raw int, and not do pointer equality
        switch ( facebookErrorCode )
        {
            case AdError.NETWORK_ERROR_CODE:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case AdError.NO_FILL_ERROR_CODE:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case AdError.SERVER_ERROR_CODE:
            case AdError.REMOTE_ADS_SERVICE_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case AdError.INTERNAL_ERROR_CODE: // It's actually a timeout event...
            case AdError.INTERSTITIAL_AD_TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case AdError.CACHE_ERROR_CODE:
            case AdError.BROKEN_MEDIA_ERROR_CODE:
            case AdError.SHOW_CALLED_BEFORE_LOAD_ERROR_CODE:
            case AdError.LOAD_CALLED_WHILE_SHOWING_AD:
            case AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE:
            case AdError.NATIVE_AD_IS_NOT_LOADED:
            case AdError.INCORRECT_STATE_ERROR:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case AdError.INTERNAL_ERROR_2003:
            case AdError.INTERNAL_ERROR_2004:
            case AdError.INTERNAL_ERROR_2006:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case AdError.MEDIAVIEW_MISSING_ERROR_CODE:
            case AdError.ICONVIEW_MISSING_ERROR_CODE:
            case AdError.AD_ASSETS_UNSUPPORTED_TYPE_ERROR_CODE:
                adapterError = new MaxAdapterError( -5400, "Missing Native Ad Assets" );
                break;
            case AdError.CLEAR_TEXT_SUPPORT_NOT_ALLOWED:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case AdError.MISSING_DEPENDENCIES_ERROR:
            case AdError.API_NOT_SUPPORTED:
            case AdError.AD_PRESENTATION_ERROR_CODE:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(),
                                    adapterError.getErrorMessage(),
                                    facebookErrorCode,
                                    facebookError.getErrorMessage() );
    }

    private String getMediationIdentifier()
    {
        return "APPLOVIN_" + AppLovinSdk.VERSION + ":" + getAdapterVersion();
    }

    private MaxNativeAdView createMaxNativeAdView(final MaxNativeAd maxNativeAd, final String templateName, final Activity activity)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11_01_00_00 )
        {
            return new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
        }
        else
        {
            return new MaxNativeAdView( maxNativeAd, templateName, activity );
        }
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private class InterstitialAdListener
            implements InterstitialAdExtendedListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final Ad ad)
        {
            log( "Interstitial ad loaded: " + ad.getPlacementId() );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onError(final Ad ad, final AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );
            log( "Interstitial ad (" + ad.getPlacementId() + ") failed to load with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdClicked(final Ad ad)
        {
            log( "Interstitial ad clicked: " + ad.getPlacementId() );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onLoggingImpression(final Ad ad)
        {
            // Max does its own impression tracking
            log( "Interstitial ad logging impression: " + ad.getPlacementId() );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onInterstitialDisplayed(final Ad ad)
        {
            log( "Interstitial ad displayed: " + ad.getPlacementId() );
        }

        @Override
        public void onInterstitialDismissed(final Ad ad)
        {
            log( "Interstitial ad hidden: " + ad.getPlacementId() );

            if ( onInterstitialAdHiddenCalled.compareAndSet( false, true ) )
            {
                listener.onInterstitialAdHidden();
            }
        }

        @Override
        public void onInterstitialActivityDestroyed()
        {
            log( "Interstitial ad Activity destroyed" );

            //
            // This may be due to launching from app icon and having the `android:launchMode="singleTask"` flag
            //
            if ( onInterstitialAdHiddenCalled.compareAndSet( false, true ) )
            {
                listener.onInterstitialAdHidden();
            }
        }

        @Override
        public void onRewardedAdCompleted() { }

        @Override
        public void onRewardedAdServerSucceeded() { }

        @Override
        public void onRewardedAdServerFailed() { }
    }

    private class RewardedAdListener
            implements RewardedVideoAdExtendedListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final Ad ad)
        {
            log( "Rewarded ad loaded: " + ad.getPlacementId() );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onError(final Ad ad, final AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );
            log( "Rewarded ad (" + ad.getPlacementId() + ") failed to load with error (" + adapterError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdClicked(final Ad ad)
        {
            log( "Rewarded ad clicked: " + ad.getPlacementId() );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onRewardedVideoClosed()
        {
            log( "Rewarded ad hidden" );

            if ( onRewardedAdHiddenCalled.compareAndSet( false, true ) )
            {
                if ( hasGrantedReward || shouldAlwaysRewardUser() )
                {
                    final MaxReward reward = getReward();
                    log( "Rewarded user with reward: " + reward );
                    listener.onUserRewarded( reward );
                }

                listener.onRewardedAdHidden();
            }
        }

        @Override
        public void onRewardedVideoCompleted()
        {
            log( "Rewarded ad video completed" );

            hasGrantedReward = true;
        }

        @Override
        public void onLoggingImpression(final Ad ad)
        {
            log( "Rewarded ad logging impression: " + ad.getPlacementId() );

            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onRewardedVideoActivityDestroyed()
        {
            log( "Rewarded ad Activity destroyed" );

            //
            // We will not reward the user if Activity is destroyed - this may be due to launching from app icon and having the `android:launchMode="singleTask"` flag
            //

            if ( onRewardedAdHiddenCalled.compareAndSet( false, true ) )
            {
                listener.onRewardedAdHidden();
            }
        }
    }

    private class AdViewListener
            implements AdListener
    {
        final MaxAdFormat              adFormat;
        final MaxAdViewAdapterListener listener;

        AdViewListener(final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(final Ad ad)
        {
            log( adFormat.getLabel() + " ad loaded: " + ad.getPlacementId() );
            listener.onAdViewAdLoaded( mAdView );
        }

        @Override
        public void onError(final Ad ad, final AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );
            log( adFormat.getLabel() + " ad (" + ad.getPlacementId() + ") failed to load with error (" + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdClicked(final Ad ad)
        {
            log( adFormat.getLabel() + " ad clicked: " + ad.getPlacementId() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onLoggingImpression(final Ad ad)
        {
            log( adFormat.getLabel() + " ad displayed: " + ad.getPlacementId() );
            listener.onAdViewAdDisplayed();
        }
    }

    /**
     * Implementation of Facebook's {@link NativeAdListener} for MAX adview ads.
     */
    private class NativeAdViewListener
            implements NativeAdListener
    {
        final Bundle                   serverParameters;
        final WeakReference<Activity>  activityRef;
        final MaxAdFormat              adFormat;
        final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final Bundle serverParameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
        {
            this.serverParameters = serverParameters;
            this.activityRef = new WeakReference<>( activity );
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(Ad ad)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded: " + ad.getPlacementId() );

            // `mNativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `ad` may be null if the user cannot get fill for FB native ads.
            if ( mNativeAd == null || mNativeAd != ad )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: no fill" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            if ( mNativeAd.isAdInvalidated() )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: ad is no longer valid" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.AD_EXPIRED );

                return;
            }

            if ( adFormat == MaxAdFormat.MREC )
            {
                View mrecView = NativeAdView.render( getContext( activityRef.get() ), mNativeAd );
                listener.onAdViewAdLoaded( mrecView );
            }
            else
            {
                renderNativeAdView();
            }
        }

        @Override
        public void onMediaDownloaded(Ad ad)
        {
            log( "Native " + adFormat.getLabel() + " successfully downloaded media: " + ad.getPlacementId() );
        }

        @Override
        public void onError(Ad ad, AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );
            log( "Native " + adFormat.getLabel() + " ad (" + ad.getPlacementId() + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onLoggingImpression(Ad ad)
        {
            log( "Native " + adFormat.getLabel() + " shown: " + ad.getPlacementId() );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(Ad ad)
        {
            log( "Native " + adFormat.getLabel() + " clicked: " + ad.getPlacementId() );
            listener.onAdViewAdClicked();
        }

        private void renderNativeAdView()
        {
            // Ensure UI rendering is done on UI thread
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final Activity activity = activityRef.get();
                    final Context context = getContext( activity );

                    final MediaView iconView = new MediaView( context );
                    final MediaView mediaView = new MediaView( context );

                    final MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                            .setAdFormat( adFormat )
                            .setTitle( mNativeAd.getAdHeadline() )
                            .setAdvertiser( mNativeAd.getAdvertiserName() )
                            .setBody( mNativeAd.getAdBodyText() )
                            .setCallToAction( mNativeAd.getAdCallToAction() )
                            .setIconView( iconView )
                            .setOptionsView( new AdOptionsView( context, mNativeAd, null ) )
                            .setMediaView( mediaView )
                            .build();

                    // Backend will pass down `vertical` as the template to indicate using a vertical native template
                    final MaxNativeAdView maxNativeAdView;
                    final String templateName = BundleUtils.getString( "template", "", serverParameters );
                    if ( templateName.contains( "vertical" ) )
                    {
                        if ( AppLovinSdk.VERSION_CODE < 9140500 )
                        {
                            log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default native template will be used." );
                        }

                        if ( templateName.equals( "vertical" ) )
                        {
                            String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                            maxNativeAdView = createMaxNativeAdView( maxNativeAd, verticalTemplateName, activity );
                        }
                        else
                        {
                            maxNativeAdView = createMaxNativeAdView( maxNativeAd, templateName, activity );
                        }
                    }
                    else if ( AppLovinSdk.VERSION_CODE < 9140500 )
                    {
                        maxNativeAdView = createMaxNativeAdView( maxNativeAd,
                                                                 AppLovinSdkUtils.isValidString( templateName ) ? templateName : "no_body_banner_template",
                                                                 activity );
                    }
                    else
                    {
                        maxNativeAdView = createMaxNativeAdView( maxNativeAd,
                                                                 AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                                 activity );
                    }

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
                    if ( maxNativeAd.getIconView() != null && maxNativeAdView.getIconContentView() != null )
                    {
                        clickableViews.add( maxNativeAdView.getIconContentView() );
                    }

                    final View mediaContentView = ( AppLovinSdk.VERSION_CODE >= 11000000 ) ? maxNativeAdView.getMediaContentViewGroup() : maxNativeAdView.getMediaContentView();
                    if ( maxNativeAd.getMediaView() != null && mediaContentView != null )
                    {
                        clickableViews.add( mediaContentView );
                    }

                    mNativeAd.registerViewForInteraction( maxNativeAdView, mediaView, iconView, clickableViews );

                    listener.onAdViewAdLoaded( maxNativeAdView );
                }
            } );
        }
    }

    /**
     * Implementation of Facebook's {@link NativeAdListener} for MAX native ads.
     */
    private class MaxNativeAdListener
            implements NativeAdListener
    {
        final Bundle                     serverParameters;
        final Context                    context;
        final MaxNativeAdAdapterListener listener;

        MaxNativeAdListener(final Bundle serverParameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = serverParameters;
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(Ad ad)
        {
            log( "Native ad loaded: " + ad.getPlacementId() );

            // This listener is used for both native ads and native banner ads
            final NativeAdBase nativeAd = ( mNativeAd != null ) ? mNativeAd : mNativeBannerAd;

            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `ad` could be null if the user cannot get fill for FB native ads.
            if ( nativeAd == null || nativeAd != ad )
            {
                log( "Native ad failed to load: no fill" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            if ( nativeAd.isAdInvalidated() )
            {
                log( "Native ad failed to load: ad is no longer valid" );
                listener.onNativeAdLoadFailed( MaxAdapterError.AD_EXPIRED );

                return;
            }

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getAdHeadline() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            // Ensure UI rendering is done on UI thread
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final MediaView mediaView = new MediaView( context );
                    final Drawable iconDrawable = nativeAd.getPreloadedIconViewDrawable();
                    final NativeAdBase.Image icon = nativeAd.getAdIcon();

                    if ( iconDrawable != null )
                    {
                        handleNativeAdLoaded( nativeAd, iconDrawable, mediaView, context );
                    }
                    else if ( icon != null )
                    {
                        // Meta Audience Network's icon image resource might be a URL that needs to be fetched async on a background thread
                        getCachingExecutorService().execute( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Drawable iconDrawable = null;

                                if ( isValidString( icon.getUrl() ) )
                                {
                                    log( "Adding native ad icon (" + icon.getUrl() + ") to queue to be fetched" );

                                    final Future<Drawable> iconDrawableFuture = createDrawableFuture( icon.getUrl(), context.getResources() );
                                    final int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", 10, serverParameters );

                                    try
                                    {
                                        if ( iconDrawableFuture != null )
                                        {
                                            iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                                        }
                                    }
                                    catch ( Throwable th )
                                    {
                                        e( "Image fetching tasks failed", th );
                                    }
                                }

                                handleNativeAdLoaded( nativeAd, iconDrawable, mediaView, context );
                            }
                        } );
                    }
                    else
                    {
                        // No ad icon available. Not a failure because it's not a required ad asset for Meta Audience Network:
                        // https://developers.facebook.com/docs/audience-network/reference/android/com/facebook/ads/nativead.html/?version=v6.2.0
                        log( "No native ad icon (optional) available for the current creative." );
                        handleNativeAdLoaded( nativeAd, null, mediaView, context );
                    }
                }
            } );
        }

        @Override
        public void onMediaDownloaded(Ad ad)
        {
            log( "Native ad successfully downloaded media: " + ad.getPlacementId() );
        }

        @Override
        public void onError(Ad ad, AdError adError)
        {
            MaxAdapterError adapterError = toMaxError( adError );
            log( "Native ad (" + ad.getPlacementId() + ") failed to load with error (" + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onLoggingImpression(Ad ad)
        {
            log( "Native shown: " + ad.getPlacementId() );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked(Ad ad)
        {
            log( "Native clicked: " + ad.getPlacementId() );
            listener.onNativeAdClicked();
        }

        private void handleNativeAdLoaded(final NativeAdBase nativeAd, final Drawable iconDrawable, final MediaView mediaView, final Context context)
        {
            MaxNativeAd.MaxNativeAdImage mainImage = null;
            // only get ad cover image when the ad is a NativeAd (and not a banner native ad)
            if ( nativeAd instanceof NativeAd && nativeAd.getAdCoverImage() != null )
            {
                Uri uri = Uri.parse( nativeAd.getAdCoverImage().getUrl() );
                mainImage = new MaxNativeAd.MaxNativeAdImage( uri );
            }

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( nativeAd.getAdHeadline() )
                    .setAdvertiser( nativeAd.getAdvertiserName() )
                    .setBody( nativeAd.getAdBodyText() )
                    .setCallToAction( nativeAd.getAdCallToAction() )
                    .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                    .setOptionsView( new AdOptionsView( context, nativeAd, null ) );
            if ( nativeAd instanceof NativeAd && AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
            {
                builder.setMainImage( mainImage );
            }

            float mediaViewAspectRatio = 0;
            if ( nativeAd instanceof NativeBannerAd )
            {
                // Facebook true native banners do not provide media views so use icon asset in place of it
                ImageView mediaViewImageView = new ImageView( context );
                mediaViewImageView.setImageDrawable( iconDrawable );
                builder.setMediaView( mediaViewImageView );

                if ( iconDrawable != null )
                {
                    mediaViewAspectRatio = (float) iconDrawable.getIntrinsicWidth() / (float) iconDrawable.getIntrinsicHeight();
                }
            }
            else
            {
                builder.setMediaView( mediaView );
                mediaViewAspectRatio = (float) mediaView.getMediaWidth() / (float) mediaView.getMediaHeight();
            }

            if ( AppLovinSdk.VERSION_CODE >= 11_04_00_00 )
            {
                builder.setMediaContentAspectRatio( mediaViewAspectRatio );
            }

            final MaxFacebookNativeAd maxNativeAd = new MaxFacebookNativeAd( builder );
            listener.onNativeAdLoaded( maxNativeAd, null );
        }
    }

    private class MaxFacebookNativeAd
            extends MaxNativeAd
    {
        private MaxFacebookNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            final List<View> clickableViews = new ArrayList<>( 6 );
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

            // To avoid `java.lang.IllegalArgumentException: Invalid set of clickable views` with size=0
            if ( clickableViews.isEmpty() )
            {
                e( "No clickable views to prepare" );
                return;
            }

            prepareForInteraction( clickableViews, maxNativeAdView );
        }

        // @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAdBase nativeAd = ( mNativeAd != null ) ? mNativeAd : mNativeBannerAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            // To avoid `java.lang.IllegalArgumentException: Invalid set of clickable views` with size=0
            if ( clickableViews.isEmpty() )
            {
                e( "No clickable views to prepare" );
                return false;
            }

            ImageView iconImageView = null;
            for ( final View clickableView : clickableViews )
            {
                if ( clickableView instanceof ImageView )
                {
                    iconImageView = (ImageView) clickableView;
                    break;
                }
            }

            if ( getMediaView() != null )
            {
                clickableViews.add( getMediaView() );
            }

            if ( nativeAd instanceof NativeBannerAd )
            {
                if ( iconImageView != null )
                {
                    ( (NativeBannerAd) nativeAd ).registerViewForInteraction( container, iconImageView, clickableViews );
                }
                else if ( getMediaView() != null )
                {
                    ( (NativeBannerAd) nativeAd ).registerViewForInteraction( container, (ImageView) getMediaView(), clickableViews );
                }
                else
                {
                    e( "Failed to register native ad view for interaction: icon image view and media view are null" );
                    return false;
                }
            }
            else
            {
                ( (NativeAd) nativeAd ).registerViewForInteraction( container, (MediaView) getMediaView(), iconImageView, clickableViews );
            }

            return true;
        }
    }
}
