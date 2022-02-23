package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import java.lang.reflect.Method;
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

    private final AtomicBoolean onInterstitialAdHiddenCalled     = new AtomicBoolean();
    private final AtomicBoolean onRewardedAdVideoCompletedCalled = new AtomicBoolean();
    private final AtomicBoolean onRewardedAdHiddenCalled         = new AtomicBoolean();

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
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
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

                        if ( onRewardedAdVideoCompletedCalled.compareAndSet( false, true ) )
                        {
                            listener.onRewardedInterstitialAdVideoCompleted();

                            hasGrantedReward = true;
                        }
                    }

                    @Override
                    public void onLoggingImpression(final Ad ad)
                    {
                        log( "Rewarded interstitial ad logging impression: " + placementId );

                        listener.onRewardedInterstitialAdDisplayed();
                        listener.onRewardedInterstitialAdVideoStarted();
                    }

                    @Override
                    public void onRewardedVideoActivityDestroyed()
                    {
                        log( "Rewarded interstitial ad Activity destroyed: " + placementId );

                        //
                        // We will not reward the user if Activity is destroyed - this may be due to launching from app icon and having the `android:launchMode="singleTask"` flag
                        //

                        if ( onRewardedAdVideoCompletedCalled.compareAndSet( false, true ) )
                        {
                            listener.onRewardedInterstitialAdVideoCompleted();
                        }

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
            listener.onRewardedInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
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
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );

        log( "Loading" + ( isNative ? " native " : " " ) + adFormat.getLabel() + " ad: " + placementId + "..." );

        updateAdSettings( parameters );

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
            mAdView = new AdView( activity, placementId, toAdSize( adFormat ) );
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

        if ( isNativeBanner )
        {
            mNativeBannerAd = new NativeBannerAd( getContext( activity ), placementId );
            mNativeBannerAd.loadAd( mNativeBannerAd.buildLoadAdConfig()
                                            .withAdListener( new MaxNativeAdListener( parameters.getServerParameters(), activity, listener ) )
                                            .withBid( parameters.getBidResponse() )
                                            .build() );
        }
        else
        {
            mNativeAd = new NativeAd( getContext( activity ), placementId );
            mNativeAd.loadAd( mNativeAd.buildLoadAdConfig()
                                      .withAdListener( new MaxNativeAdListener( parameters.getServerParameters(), activity, listener ) )
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

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
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

    private static MaxAdapterError toMaxError(final AdError facebookError)
    {
        final int facebookErrorCode = facebookError.getErrorCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        // Facebook's SDK sometimes creates new instances of their pre-defined enums, so we should extract the raw int, and not do pointer equality
        switch ( facebookErrorCode )
        {
            case AdError.NETWORK_ERROR_CODE: // 1000
                adapterError = MaxAdapterError.NO_CONNECTION; // -5207
                break;
            case AdError.NO_FILL_ERROR_CODE: // 1001
                adapterError = MaxAdapterError.NO_FILL; // 204
                break;
            case AdError.LOAD_TOO_FREQUENTLY_ERROR_CODE: // 1002
                adapterError = MaxAdapterError.INVALID_LOAD_STATE; // -5201
                break;
            case AdError.SERVER_ERROR_CODE: // 2000
                adapterError = MaxAdapterError.SERVER_ERROR; // -5208
                break;
            case AdError.INTERNAL_ERROR_CODE: // 2001 - it's actually a timeout event...
                adapterError = MaxAdapterError.TIMEOUT; // -5209
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), facebookErrorCode, facebookError.getErrorMessage() );
    }

    private String getMediationIdentifier()
    {
        return "APPLOVIN_" + AppLovinSdk.VERSION + ":" + getAdapterVersion();
    }

    private MaxNativeAdView createMaxNativeAdView(final MaxNativeAd maxNativeAd, final String templateName, final Activity activity)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11010000 )
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

            if ( onRewardedAdVideoCompletedCalled.compareAndSet( false, true ) )
            {
                listener.onRewardedAdVideoCompleted();

                hasGrantedReward = true;
            }
        }

        @Override
        public void onLoggingImpression(final Ad ad)
        {
            log( "Rewarded ad logging impression: " + ad.getPlacementId() );

            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onRewardedVideoActivityDestroyed()
        {
            log( "Rewarded ad Activity destroyed" );

            //
            // We will not reward the user if Activity is destroyed - this may be due to launching from app icon and having the `android:launchMode="singleTask"` flag
            //

            if ( onRewardedAdVideoCompletedCalled.compareAndSet( false, true ) )
            {
                listener.onRewardedAdVideoCompleted();
            }

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

            final Activity activity = activityRef.get();
            if ( activity == null )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: activity reference is null when ad is loaded" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                return;
            }

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
                View mrecView = NativeAdView.render( activity, mNativeAd );
                listener.onAdViewAdLoaded( mrecView );
            }
            else if ( AppLovinSdk.VERSION_CODE >= 9140000 ) // Native banners and leaders use APIs in newer SDKs
            {
                renderNativeAdView( activity );
            }
            else
            {
                log( "Native " + adFormat.getLabel() + " failed to load: AppLovin SDK version must be at least 9.14.0" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );
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

        private void renderNativeAdView(final Activity activity)
        {
            // Ensure UI rendering is done on UI thread
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final MediaView iconView = new MediaView( activity );
                    final MediaView mediaView = new MediaView( activity );

                    final MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                            .setAdFormat( adFormat )
                            .setTitle( mNativeAd.getAdHeadline() )
                            .setAdvertiser( mNativeAd.getAdvertiserName() )
                            .setBody( mNativeAd.getAdBodyText() )
                            .setCallToAction( mNativeAd.getAdCallToAction() )
                            .setIconView( iconView )
                            .setMediaView( mediaView )
                            .setOptionsView( new AdOptionsView( activity, mNativeAd, null ) )
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

                    final List<View> clickableViews = new ArrayList<>();
                    if ( maxNativeAd.getIconView() != null && maxNativeAdView.getIconContentView() != null )
                    {
                        clickableViews.add( maxNativeAdView.getIconContentView() );
                    }

                    final View mediaContentView = ( AppLovinSdk.VERSION_CODE >= 11000000 ) ? maxNativeAdView.getMediaContentViewGroup() : maxNativeAdView.getMediaContentView();
                    if ( maxNativeAd.getMediaView() != null && mediaContentView != null )
                    {
                        clickableViews.add( mediaContentView );
                    }
                    if ( AppLovinSdkUtils.isValidString( maxNativeAd.getTitle() ) && maxNativeAdView.getTitleTextView() != null )
                    {
                        clickableViews.add( maxNativeAdView.getTitleTextView() );
                    }
                    if ( AppLovinSdkUtils.isValidString( maxNativeAd.getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
                    {
                        clickableViews.add( maxNativeAdView.getCallToActionButton() );
                    }
                    if ( AppLovinSdkUtils.isValidString( maxNativeAd.getAdvertiser() ) && maxNativeAdView.getAdvertiserTextView() != null )
                    {
                        clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
                    }
                    if ( AppLovinSdkUtils.isValidString( maxNativeAd.getBody() ) && maxNativeAdView.getBodyTextView() != null )
                    {
                        clickableViews.add( maxNativeAdView.getBodyTextView() );
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
        final WeakReference<Activity>    activityRef;
        final MaxNativeAdAdapterListener listener;

        MaxNativeAdListener(final Bundle serverParameters, final Activity activity, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = serverParameters;
            this.activityRef = new WeakReference<>( activity );
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(Ad ad)
        {
            log( "Native ad loaded: " + ad.getPlacementId() );

            final Activity activity = activityRef.get();
            if ( activity == null )
            {
                log( "Native ad failed to load: activity reference is null when ad is loaded" );
                listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                return;
            }

            // This listener is used for both native ads and native banner ads
            final NativeAdBase nativeAd = mNativeAd != null ? mNativeAd : mNativeBannerAd;

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
            if ( !hasRequiredAssets( isTemplateAd, nativeAd ) )
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
                    final MediaView mediaView = new MediaView( activity );
                    final Drawable iconDrawable = nativeAd.getPreloadedIconViewDrawable();
                    final NativeAdBase.Image icon = nativeAd.getAdIcon();

                    if ( iconDrawable != null )
                    {
                        handleNativeAdLoaded( nativeAd, iconDrawable, mediaView, activity );
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

                                    final Future<Drawable> iconDrawableFuture = createDrawableFuture( icon.getUrl(), activity.getResources() );
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

                                handleNativeAdLoaded( nativeAd, iconDrawable, mediaView, activity );
                            }
                        } );
                    }
                    else
                    {
                        // No ad icon available. Not a failure because it's not a required ad asset for Meta Audience Network:
                        // https://developers.facebook.com/docs/audience-network/reference/android/com/facebook/ads/nativead.html/?version=v6.2.0
                        log( "No native ad icon (optional) available for the current creative." );
                        handleNativeAdLoaded( nativeAd, null, mediaView, activity );
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

        private void handleNativeAdLoaded(final NativeAdBase nativeAd, final Drawable iconDrawable, final MediaView mediaView, final Activity activity)
        {
            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( nativeAd.getAdHeadline() )
                    .setAdvertiser( nativeAd.getAdvertiserName() )
                    .setBody( nativeAd.getAdBodyText() )
                    .setCallToAction( nativeAd.getAdCallToAction() )
                    .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                    .setOptionsView( new AdOptionsView( activity, nativeAd, null ) );

            if ( nativeAd instanceof NativeBannerAd )
            {
                // Facebook true native banners do not provide media views so use icon asset in place of it
                ImageView mediaViewImageView = new ImageView( activity );
                mediaViewImageView.setImageDrawable( iconDrawable );
                builder.setMediaView( mediaViewImageView );
            }
            else
            {
                builder.setMediaView( mediaView );
            }

            final MaxFacebookNativeAd maxNativeAd = new MaxFacebookNativeAd( builder );
            listener.onNativeAdLoaded( maxNativeAd, null );
        }

        private boolean hasRequiredAssets(final boolean isTemplateAd, final NativeAdBase nativeAd)
        {
            if ( isTemplateAd )
            {
                return AppLovinSdkUtils.isValidString( nativeAd.getAdHeadline() );
            }
            else
            {
                // NOTE: media view is created and will always be non-null
                return AppLovinSdkUtils.isValidString( nativeAd.getAdHeadline() )
                        && AppLovinSdkUtils.isValidString( nativeAd.getAdCallToAction() );
            }
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
            final NativeAdBase nativeAd = mNativeAd != null ? mNativeAd : mNativeBannerAd;
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

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    if ( nativeAd instanceof NativeBannerAd )
                    {
                        if ( maxNativeAdView.getIconImageView() != null )
                        {
                            ( (NativeBannerAd) nativeAd ).registerViewForInteraction( maxNativeAdView, maxNativeAdView.getIconImageView(), clickableViews );
                        }
                        else if ( getMediaView() != null )
                        {
                            ( (NativeBannerAd) nativeAd ).registerViewForInteraction( maxNativeAdView, (ImageView) getMediaView(), clickableViews );
                        }
                        else
                        {
                            e( "Failed to register native ad view for interaction: icon image view and media view are null" );
                        }
                    }
                    else
                    {
                        ( (NativeAd) nativeAd ).registerViewForInteraction( maxNativeAdView, (MediaView) getMediaView(), (MediaView) getIconView(), clickableViews );
                    }
                }
            } );
        }
    }
}
