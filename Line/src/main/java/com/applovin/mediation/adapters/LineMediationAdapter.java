package com.applovin.mediation.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.line.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.five_corp.ad.FiveAd;
import com.five_corp.ad.FiveAdConfig;
import com.five_corp.ad.FiveAdCustomLayout;
import com.five_corp.ad.FiveAdFormat;
import com.five_corp.ad.FiveAdInterface;
import com.five_corp.ad.FiveAdInterstitial;
import com.five_corp.ad.FiveAdListener;
import com.five_corp.ad.FiveAdNative;
import com.five_corp.ad.FiveAdState;
import com.five_corp.ad.FiveAdVideoReward;
import com.five_corp.ad.NeedChildDirectedTreatment;
import com.five_corp.ad.NeedGdprNonPersonalizedAdsTreatment;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LineMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private FiveAdInterstitial interstitialAd;
    private FiveAdVideoReward  rewardedAd;
    private FiveAdCustomLayout adView;
    private FiveAdNative       nativeAd;

    public LineMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return FiveAd.getSdkSemanticVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String appId = parameters.getServerParameters().getString( "app_id" );
            log( "Initializing Line SDK with app id: " + appId + "..." );

            FiveAdConfig config = new FiveAdConfig( appId );
            config.isTest = parameters.isTesting();
            config.formats = EnumSet.of(
                    FiveAdFormat.VIDEO_REWARD,
                    FiveAdFormat.CUSTOM_LAYOUT
            );

            //
            // GDPR options
            //
            if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
            {
                Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
                if ( hasUserConsent != null )
                {
                    config.needGdprNonPersonalizedAdsTreatment = hasUserConsent ? NeedGdprNonPersonalizedAdsTreatment.FALSE : NeedGdprNonPersonalizedAdsTreatment.TRUE;
                }
            }

            //
            // COPPA options
            //
            Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
            if ( isAgeRestrictedUser != null )
            {
                config.needChildDirectedTreatment = isAgeRestrictedUser ? NeedChildDirectedTreatment.TRUE : NeedChildDirectedTreatment.FALSE;
            }

            FiveAd.initialize( activity, config );
            onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_UNKNOWN, null );
        }
        else
        {
            if ( FiveAd.isInitialized() )
            {
                log( "Line SDK is already initialized" );
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_UNKNOWN, null );
            }
            else
            {
                log( "Line SDK still initializing" );
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZING, null );
            }
        }
    }

    @Override
    public void onDestroy()
    {
        interstitialAd = null;
        rewardedAd = null;
        adView = null;
        nativeAd = null;
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad for slot id: " + slotId + "..." );

        updateMuteState( parameters );

        interstitialAd = new FiveAdInterstitial( activity, slotId );
        interstitialAd.setListener( new InterstitialListener( listener ) );

        interstitialAd.loadAdAsync();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for slot id: " + slotId + "..." );

        interstitialAd.show( activity );
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for slot id: " + slotId + "..." );

        updateMuteState( parameters );

        rewardedAd = new FiveAdVideoReward( activity, slotId );
        rewardedAd.setListener( new RewardedListener( listener ) );

        rewardedAd.loadAdAsync();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for slot id: " + slotId + "..." );

        configureReward( parameters );
        rewardedAd.show( activity );
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        String slotId = parameters.getThirdPartyAdPlacementId();

        log( "Loading " + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for slot id: " + slotId + "..." );

        if ( isNative )
        {
            nativeAd = new FiveAdNative( activity, slotId, new DisplayMetrics().widthPixels );
            nativeAd.setListener( new NativeAdViewListener( listener, adFormat, parameters.getServerParameters(), activity ) );

            // We always want to mute banners and MRECs
            nativeAd.enableSound( false );

            nativeAd.loadAdAsync();
        }
        else
        {
            adView = new FiveAdCustomLayout( activity, slotId, new DisplayMetrics().widthPixels );
            adView.setListener( new AdViewListener( listener, adFormat ) );

            // We always want to mute banners and MRECs
            adView.enableSound( false );

            adView.loadAdAsync();
        }
    }

    // @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading native ad for slot id: " + slotId + "..." );

        nativeAd = new FiveAdNative( activity, slotId, new DisplayMetrics().widthPixels );
        nativeAd.setListener( new NativeAdListener( listener, parameters.getServerParameters(), activity ) );

        // We always want to mute banners and MRECs
        nativeAd.enableSound( false );

        nativeAd.loadAdAsync();
    }

    private void updateMuteState(MaxAdapterResponseParameters parameters)
    {
        final Bundle serverParameters = parameters.getServerParameters();
        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        if ( serverParameters.containsKey( "is_muted" ) )
        {
            boolean muted = serverParameters.getBoolean( "is_muted" );
            FiveAd.getSingleton().enableSound( !muted );
        }
    }

    private static MaxAdapterError toMaxError(FiveAdListener.ErrorCode lineAdsError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        String thirdPartySdkErrorMessage = "Please contact us.";
        switch ( lineAdsError )
        {
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                thirdPartySdkErrorMessage = "Please try again in a stable network environment.";
                break;
            case NO_CACHED_AD:
                adapterError = MaxAdapterError.AD_NOT_READY;
                thirdPartySdkErrorMessage = "Please enable isTest and try again";
                break;
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                thirdPartySdkErrorMessage = "Please enable isTest and try again";
                break;
            case BAD_APP_ID:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                thirdPartySdkErrorMessage = "Check if the OS type, PackageName, and issued AppID registered in FIVE Dashboard and the application settings match. Please be careful about blanks.";
                break;
            case STORAGE_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                thirdPartySdkErrorMessage = "There is a problem with the device storage. Please try again with another device.";
                break;
            case INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                thirdPartySdkErrorMessage = "Please contact us.";
                break;
            case UNSUPPORTED_OS_VERSION:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                thirdPartySdkErrorMessage = "Please check with Android 4.0.0 or above.";
                break;
            case INVALID_STATE:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                thirdPartySdkErrorMessage = "There is a problem with the implementation. Please check the following. Whether the initialization process (FiveAd.initialize) is executed before the creation of the ad object or loadAdAsync. Are you calling loadAdAsync multiple times for one ad object?";
                break;
            case BAD_SLOT_ID:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                thirdPartySdkErrorMessage = "Make sure you are using the SlotID issued on the FIVE Dashboard.";
                break;
            case SUPPRESSED:
            case CONTENT_UNAVAILABLE:
            case PLAYER_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                thirdPartySdkErrorMessage = "Please contact us.";
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), lineAdsError.ordinal(), thirdPartySdkErrorMessage );
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

    private class InterstitialListener
            implements FiveAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onFiveAdLoad(final FiveAdInterface ad)
        {
            log( "Interstitial ad loaded for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onFiveAdError(final FiveAdInterface ad, final ErrorCode errorCode)
        {
            log( "Interstitial ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onInterstitialAdLoadFailed( error );
        }

        @Override
        public void onFiveAdImpressionImage(final FiveAdInterface ad)
        {
            log( "Interstitial ad impression tracked for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for graphic-only interstitial ads.
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onFiveAdClick(final FiveAdInterface ad)
        {
            log( "Interstitial ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onFiveAdClose(final FiveAdInterface ad)
        {
            log( "Interstitial ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onFiveAdStart(final FiveAdInterface ad)
        {
            log( "Interstitial ad shown for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for video-only interstitial ads.
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onFiveAdPause(final FiveAdInterface ad)
        {
            log( "Interstitial ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdResume(final FiveAdInterface ad)
        {
            log( "Interstitial ad did resume for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdViewThrough(final FiveAdInterface ad)
        {
            log( "Interstitial ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdReplay(final FiveAdInterface ad)
        {
            log( "Interstitial ad did replay for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdStall(final FiveAdInterface ad)
        {
            log( "Interstitial ad did stall for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdRecover(final FiveAdInterface ad)
        {
            log( "Interstitial ad did recover for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class RewardedListener
            implements FiveAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onFiveAdLoad(final FiveAdInterface ad)
        {
            log( "Rewarded ad loaded for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onFiveAdError(final FiveAdInterface ad, final ErrorCode errorCode)
        {
            log( "Rewarded ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onRewardedAdLoadFailed( error );
        }

        @Override
        public void onFiveAdImpressionImage(final FiveAdInterface ad)
        {
            log( "Rewarded ad impression tracked for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for graphic-only rewarded ads.
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onFiveAdClick(final FiveAdInterface ad)
        {
            log( "Rewarded ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onFiveAdClose(final FiveAdInterface ad)
        {
            if ( ad.getState() != FiveAdState.ERROR )
            {
                if ( hasGrantedReward || shouldAlwaysRewardUser() )
                {
                    final MaxReward reward = getReward();

                    log( "Rewarded ad user with reward: " + reward + " for slot id: " + ad.getSlotId() + "..." );
                    listener.onUserRewarded( reward );
                }
            }
            log( "Rewarded ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onFiveAdStart(final FiveAdInterface ad)
        {
            log( "Rewarded ad shown for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for video-only rewarded ads.
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onFiveAdPause(final FiveAdInterface ad)
        {
            log( "Rewarded ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdResume(final FiveAdInterface ad)
        {
            log( "Rewarded ad did resume for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdViewThrough(final FiveAdInterface ad)
        {
            log( "Rewarded ad completed for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdVideoCompleted();
            hasGrantedReward = true;
        }

        @Override
        public void onFiveAdReplay(final FiveAdInterface ad)
        {
            log( "Rewarded ad did replay for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdStall(final FiveAdInterface ad)
        {
            log( "Rewarded ad did stall for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdRecover(final FiveAdInterface ad)
        {
            log( "Rewarded ad did recover for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class AdViewListener
            implements FiveAdListener
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;

        AdViewListener(final MaxAdViewAdapterListener listener, final MaxAdFormat adFormat)
        {
            this.listener = listener;
            this.adFormat = adFormat;
        }

        @Override
        public void onFiveAdLoad(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad loaded for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onFiveAdError(final FiveAdInterface ad, final ErrorCode errorCode)
        {
            log( adFormat.getLabel() + " ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onFiveAdImpressionImage(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad impression tracked for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for graphic-only adview ads.
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onFiveAdClick(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onFiveAdClose(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onFiveAdStart(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad shown for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for video-only adview ads.
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onFiveAdPause(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdResume(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad did resume for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdViewThrough(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdReplay(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad did replay for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdStall(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad did stall for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdRecover(final FiveAdInterface ad)
        {
            log( adFormat.getLabel() + " ad did recover for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class NativeAdViewListener
            implements FiveAdListener
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;
        private final Bundle                   serverParameters;
        private final WeakReference<Activity>  activityRef;

        NativeAdViewListener(final MaxAdViewAdapterListener listener, final MaxAdFormat adFormat, final Bundle serverParameters, final Activity activity)
        {
            this.listener = listener;
            this.adFormat = adFormat;
            this.serverParameters = serverParameters;
            this.activityRef = new WeakReference<>( activity );
        }

        @Override
        public void onFiveAdLoad(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded for slot id: " + ad.getSlotId() + "..." );

            if ( nativeAd == null )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: no fill for slot id: " + ad.getSlotId() + "..." );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
                return;
            }

            renderCustomNativeBanner( ad.getSlotId(), activityRef.get() );
        }

        @Override
        public void onFiveAdError(final FiveAdInterface ad, final ErrorCode errorCode)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onFiveAdImpressionImage(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad impression tracked for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for graphic-only native adview ads.
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onFiveAdClick(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked for slot id: " + ad.getSlotId() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onFiveAdClose(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onFiveAdStart(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad shown for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for video-only native adview ads.
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onFiveAdPause(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdResume(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did resume for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdViewThrough(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdReplay(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did replay for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdStall(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did stall for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdRecover(final FiveAdInterface ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did recover for slot id: " + ad.getSlotId() + "..." );
        }

        private void renderCustomNativeBanner(final String slotId, final Activity activity)
        {
            nativeAd.loadIconImageAsync( new FiveAdNative.LoadImageCallback()
            {
                @Override
                public void onImageLoad(final Bitmap bitmap)
                {
                    // Ensure UI rendering is done on UI thread
                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
                            if ( nativeAd == null )
                            {
                                log( "Native " + adFormat.getLabel() + " ad destroyed before assets finished load for slot id: " + slotId );
                                return;
                            }

                            final MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                                    .setAdFormat( adFormat )
                                    .setTitle( nativeAd.getAdTitle() )
                                    .setBody( nativeAd.getDescriptionText() )
                                    .setCallToAction( nativeAd.getButtonText() )
                                    .setIcon( new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( activity.getResources(), bitmap ) ) )
                                    .setMediaView( nativeAd.getAdMainView() )
                                    .build();

                            // Backend will pass down `vertical` as the template to indicate using a vertical native template
                            final String templateName = BundleUtils.getString( "template", "", serverParameters );
                            if ( templateName.contains( "vertical" ) && AppLovinSdk.VERSION_CODE < 9140500 )
                            {
                                log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default native template will be used." );
                            }

                            final MaxNativeAdView maxNativeAdView;
                            if ( AppLovinSdk.VERSION_CODE < 9140000 )
                            {
                                log( "Native ads with media views are only supported on MAX SDK version 9.14.0 and above. Default native template will be used." );
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, activity );
                            }
                            // Fallback case to be removed when backend sends down full template names for vertical native ads
                            else if ( templateName.equals( "vertical" ) )
                            {
                                String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, verticalTemplateName, activity );
                            }
                            else
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, activity );
                            }

                            final List<View> clickableViews = new ArrayList<>();
                            if ( maxNativeAd.getIcon() != null && maxNativeAdView.getIconImageView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getIconImageView() );
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
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getBody() ) && maxNativeAdView.getBodyTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getBodyTextView() );
                            }

                            nativeAd.registerViews( maxNativeAdView, maxNativeAdView.getIconImageView(), clickableViews );
                            listener.onAdViewAdLoaded( maxNativeAdView );
                        }
                    } );
                }
            } );
        }
    }

    private class NativeAdListener
            implements FiveAdListener
    {
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;
        private final WeakReference<Activity>    activityRef;

        NativeAdListener(final MaxNativeAdAdapterListener listener, final Bundle serverParameters, final Activity activity)
        {
            this.listener = listener;
            this.serverParameters = serverParameters;
            this.activityRef = new WeakReference<>( activity );
        }

        @Override
        public void onFiveAdLoad(final FiveAdInterface ad)
        {
            log( "Native ad loaded for slot id: " + ad.getSlotId() + "..." );

            FiveAdNative loadedNativeAd = nativeAd;
            if ( loadedNativeAd == null )
            {
                log( "Native ad destroyed before the ad successfully loaded: " + ad.getSlotId() + "..." );
                listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                return;
            }

            final Activity activity = activityRef.get();
            if ( activity == null )
            {
                log( "Native ad (" + ad.getSlotId() + ") failed to load: activity reference is null when ad is loaded" );
                listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                return;
            }

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( !hasRequiredAssets( isTemplateAd, loadedNativeAd ) )
            {
                e( "Native ad (" + ad + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            loadedNativeAd.loadIconImageAsync( new FiveAdNative.LoadImageCallback()
            {
                @Override
                public void onImageLoad(final Bitmap bitmap)
                {
                    FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
                    if ( nativeAd == null )
                    {
                        log( "Native ad destroyed before assets finished load for slot id: " + ad.getSlotId() );
                        return;
                    }

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getAdTitle() )
                            .setBody( nativeAd.getDescriptionText() )
                            .setCallToAction( nativeAd.getButtonText() )
                            .setAdvertiser( nativeAd.getAdvertiserName() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( activity.getResources(), bitmap ) ) )
                            .setMediaView( nativeAd.getAdMainView() )
                            .setAdvertiser( nativeAd.getAdvertiserName() );
                    MaxNativeAd maxNativeAd = new MaxLineNativeAd( builder );

                    listener.onNativeAdLoaded( maxNativeAd, null );
                }
            } );
        }

        @Override
        public void onFiveAdError(final FiveAdInterface ad, final ErrorCode errorCode)
        {
            log( "Native ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onNativeAdLoadFailed( error );
        }

        @Override
        public void onFiveAdImpressionImage(final FiveAdInterface ad)
        {
            log( "Native ad impression tracked for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for graphic-only native adview ads.
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onFiveAdClick(final FiveAdInterface ad)
        {
            log( "Native ad clicked for slot id: " + ad.getSlotId() );
            listener.onNativeAdClicked();
        }

        @Override
        public void onFiveAdClose(final FiveAdInterface ad)
        {
            log( "Native ad hidden for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdStart(final FiveAdInterface ad)
        {
            log( "Native ad shown for slot id: " + ad.getSlotId() + "..." );

            // NOTE: Called for video-only native adview ads.
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onFiveAdPause(final FiveAdInterface ad)
        {
            log( "Native ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdResume(final FiveAdInterface ad)
        {
            log( "Native ad did resume for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdViewThrough(final FiveAdInterface ad)
        {
            log( "Native ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdReplay(final FiveAdInterface ad)
        {
            log( "Native ad did replay for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdStall(final FiveAdInterface ad)
        {
            log( "Native ad did stall for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onFiveAdRecover(final FiveAdInterface ad)
        {
            log( "Native ad did recover for slot id: " + ad.getSlotId() + "..." );
        }

        private boolean hasRequiredAssets(final boolean isTemplateAd, final FiveAdNative nativeAd)
        {
            if ( isTemplateAd )
            {
                return AppLovinSdkUtils.isValidString( nativeAd.getAdTitle() );
            }
            else
            {
                return AppLovinSdkUtils.isValidString( nativeAd.getAdTitle() )
                        && AppLovinSdkUtils.isValidString( nativeAd.getButtonText() )
                        && nativeAd.getAdMainView() != null;
            }
        }
    }

    private class MaxLineNativeAd
            extends MaxNativeAd
    {
        private MaxLineNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
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
            if ( AppLovinSdkUtils.isValidString( getAdvertiser() ) && maxNativeAdView.getAdvertiserTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
            }

            nativeAd.registerViews( maxNativeAdView, maxNativeAdView.getIconImageView(), clickableViews );
        }
    }
}
