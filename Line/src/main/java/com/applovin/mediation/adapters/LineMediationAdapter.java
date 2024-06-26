package com.applovin.mediation.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.line.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.five_corp.ad.FiveAd;
import com.five_corp.ad.FiveAdConfig;
import com.five_corp.ad.FiveAdCustomLayout;
import com.five_corp.ad.FiveAdCustomLayoutEventListener;
import com.five_corp.ad.FiveAdErrorCode;
import com.five_corp.ad.FiveAdInterface;
import com.five_corp.ad.FiveAdInterstitial;
import com.five_corp.ad.FiveAdInterstitialEventListener;
import com.five_corp.ad.FiveAdLoadListener;
import com.five_corp.ad.FiveAdNative;
import com.five_corp.ad.FiveAdNativeEventListener;
import com.five_corp.ad.FiveAdState;
import com.five_corp.ad.FiveAdVideoReward;
import com.five_corp.ad.FiveAdVideoRewardEventListener;
import com.five_corp.ad.NeedChildDirectedTreatment;
import com.five_corp.ad.NeedGdprNonPersonalizedAdsTreatment;

import java.util.ArrayList;
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

            Bundle serverParameters = parameters.getServerParameters();
            // Overwritten by `mute_state` setting, unless `mute_state` is disabled
            if ( serverParameters.containsKey( "is_muted" ) )
            {
                boolean muted = serverParameters.getBoolean( "is_muted" );
                config.enableSoundByDefault( !muted );
            }

            //
            // GDPR options
            //
            Boolean hasUserConsent = parameters.hasUserConsent();
            if ( hasUserConsent != null )
            {
                config.needGdprNonPersonalizedAdsTreatment = hasUserConsent ? NeedGdprNonPersonalizedAdsTreatment.FALSE : NeedGdprNonPersonalizedAdsTreatment.TRUE;
            }

            //
            // COPPA options
            // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
            // https://support.google.com/googleplay/android-developer/answer/9283445?hl=en
            Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
            if ( isAgeRestrictedUser != null )
            {
                config.needChildDirectedTreatment = isAgeRestrictedUser ? NeedChildDirectedTreatment.TRUE : NeedChildDirectedTreatment.FALSE;
            }

            FiveAd.initialize( getApplicationContext(), config );

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

        interstitialAd = new FiveAdInterstitial( getApplicationContext(), slotId );

        InterstitialListener interstitialListener = new InterstitialListener( listener );
        interstitialAd.setLoadListener( interstitialListener );
        interstitialAd.setEventListener( interstitialListener );
        interstitialAd.loadAdAsync();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad for slot id: " + slotId + "..." );

        interstitialAd.showAd();
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for slot id: " + slotId + "..." );

        rewardedAd = new FiveAdVideoReward( getApplicationContext(), slotId );

        RewardedListener rewardedListener = new RewardedListener( listener );
        rewardedAd.setLoadListener( rewardedListener );
        rewardedAd.setEventListener( rewardedListener );
        rewardedAd.loadAdAsync();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad for slot id: " + slotId + "..." );

        configureReward( parameters );
        rewardedAd.showAd();
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        String slotId = parameters.getThirdPartyAdPlacementId();

        log( "Loading " + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for slot id: " + slotId + "..." );

        if ( isNative )
        {
            nativeAd = new FiveAdNative( getApplicationContext(), slotId, new DisplayMetrics().widthPixels );
            NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( listener, adFormat, parameters.getServerParameters() );
            nativeAd.setLoadListener( nativeAdViewListener );
            nativeAd.setEventListener( nativeAdViewListener );

            // We always want to mute banners and MRECs
            nativeAd.enableSound( false );

            nativeAd.loadAdAsync();
        }
        else
        {
            adView = new FiveAdCustomLayout( getApplicationContext(), slotId, new DisplayMetrics().widthPixels );
            AdViewListener adViewListener = new AdViewListener( listener, adFormat );
            adView.setLoadListener( adViewListener );
            adView.setEventListener( adViewListener );

            // We always want to mute banners and MRECs
            adView.enableSound( false );

            adView.loadAdAsync();
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String slotId = parameters.getThirdPartyAdPlacementId();
        log( "Loading native ad for slot id: " + slotId + "..." );

        nativeAd = new FiveAdNative( getApplicationContext(), slotId, new DisplayMetrics().widthPixels );
        NativeAdListener nativeAdListener = new NativeAdListener( listener, parameters.getServerParameters() );
        nativeAd.setLoadListener( nativeAdListener );
        nativeAd.setEventListener( nativeAdListener );

        // We always want to mute banners and MRECs
        nativeAd.enableSound( false );

        nativeAd.loadAdAsync();
    }

    private static MaxAdapterError toMaxError(FiveAdErrorCode lineAdsError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        String thirdPartySdkErrorMessage = "Please contact us.";
        switch ( lineAdsError )
        {
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                thirdPartySdkErrorMessage = "Please try again in a stable network environment.";
                break;
            case NO_AD:
                adapterError = MaxAdapterError.NO_FILL;
                thirdPartySdkErrorMessage = "Ad was not ready at display time. Please try again.";
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
            case INVALID_STATE:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                thirdPartySdkErrorMessage = "There is a problem with the implementation. Please check the following. Whether the initialization process (FiveAd.initialize) is executed before the creation of the ad object or loadAdAsync. Are you calling loadAdAsync multiple times for one ad object?";
                break;
            case BAD_SLOT_ID:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                thirdPartySdkErrorMessage = "Make sure you are using the SlotID issued on the FIVE Dashboard.";
                break;
            case SUPPRESSED:
            case PLAYER_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                thirdPartySdkErrorMessage = "Please contact us.";
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), lineAdsError.ordinal(), thirdPartySdkErrorMessage );
    }

    private class InterstitialListener
            implements FiveAdLoadListener, FiveAdInterstitialEventListener
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
        public void onFiveAdLoadError(final FiveAdInterface ad, final FiveAdErrorCode errorCode)
        {
            log( "Interstitial ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onInterstitialAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdInterstitial ad, final FiveAdErrorCode errorCode)
        {
            log( "Interstitial ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( -4205, "Ad Display Failed", errorCode.value, "Please Contact Us" );
            listener.onInterstitialAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onFullScreenClose(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onFullScreenOpen(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad shown for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPlay(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdInterstitial ad)
        {
            log( "Interstitial ad completed for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class RewardedListener
            implements FiveAdLoadListener, FiveAdVideoRewardEventListener
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
        public void onFiveAdLoadError(final FiveAdInterface ad, final FiveAdErrorCode errorCode)
        {
            log( "Rewarded ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onRewardedAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdVideoReward ad, final FiveAdErrorCode errorCode)
        {
            log( "Rewarded ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( -4205, "Ad Display Failed", errorCode.value, "Please Contact Us" );
            listener.onRewardedAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onFullScreenClose(final FiveAdVideoReward ad)
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
        public void onFullScreenOpen(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad shown for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPlay(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onReward(final FiveAdVideoReward ad)
        {
            log( "Rewarded ad granted reward for slot id: " + ad.getSlotId() );
            hasGrantedReward = true;
        }
    }

    private class AdViewListener
            implements FiveAdLoadListener, FiveAdCustomLayoutEventListener
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
        public void onFiveAdLoadError(final FiveAdInterface ad, final FiveAdErrorCode errorCode)
        {
            log( adFormat.getLabel() + " ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdCustomLayout ad, final FiveAdErrorCode errorCode)
        {
            log( adFormat.getLabel() + " ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( -4205, "Ad Display Failed", errorCode.value, "Please Contact Us" );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad clicked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onRemove(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onPlay(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdCustomLayout ad)
        {
            log( adFormat.getLabel() + " ad completed for slot id: " + ad.getSlotId() + "..." );
        }
    }

    private class NativeAdViewListener
            implements FiveAdLoadListener, FiveAdNativeEventListener
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;
        private final Bundle                   serverParameters;

        NativeAdViewListener(final MaxAdViewAdapterListener listener, final MaxAdFormat adFormat, final Bundle serverParameters)
        {
            this.listener = listener;
            this.adFormat = adFormat;
            this.serverParameters = serverParameters;
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

            renderCustomNativeBanner( ad.getSlotId() );
        }

        @Override
        public void onFiveAdLoadError(final FiveAdInterface ad, final FiveAdErrorCode errorCode)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdNative ad, final FiveAdErrorCode errorCode)
        {
            log( "Native " + adFormat.getLabel() + " ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = new MaxAdapterError( -4205, "Ad Display Failed", errorCode.value, "Please Contact Us" );
            listener.onAdViewAdDisplayFailed( error );
        }

        @Override
        public void onImpression(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onClick(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked for slot id: " + ad.getSlotId() );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onRemove(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad hidden for slot id: " + ad.getSlotId() + "..." );
            listener.onAdViewAdHidden();
        }

        @Override
        public void onPlay(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdNative ad)
        {
            log( "Native " + adFormat.getLabel() + " ad completed for slot id: " + ad.getSlotId() + "..." );
        }

        private void renderCustomNativeBanner(final String slotId)
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
                                    .setIcon( new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( getApplicationContext().getResources(), bitmap ) ) )
                                    .setMediaView( nativeAd.getAdMainView() )
                                    .build();

                            // Backend will pass down `vertical` as the template to indicate using a vertical native template
                            final String templateName = BundleUtils.getString( "template", "", serverParameters );
                            if ( templateName.contains( "vertical" ) && AppLovinSdk.VERSION_CODE < 9140500 )
                            {
                                log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default native template will be used." );
                            }

                            final MaxNativeAdView maxNativeAdView;
                            // Fallback case to be removed when backend sends down full template names for vertical native ads
                            if ( templateName.equals( "vertical" ) )
                            {
                                String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, verticalTemplateName, getApplicationContext() );
                            }
                            else
                            {
                                maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
                            }

                            final List<View> clickableViews = new ArrayList<>( 5 );

                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getTitle() ) && maxNativeAdView.getTitleTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getTitleTextView() );
                            }
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getBody() ) && maxNativeAdView.getBodyTextView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getBodyTextView() );
                            }
                            if ( AppLovinSdkUtils.isValidString( maxNativeAd.getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
                            {
                                clickableViews.add( maxNativeAdView.getCallToActionButton() );
                            }
                            if ( maxNativeAd.getIcon() != null && maxNativeAdView.getIconImageView() != null )
                            {
                                clickableViews.add( maxNativeAdView.getIconImageView() );
                            }
                            final View mediaContentView = ( AppLovinSdk.VERSION_CODE >= 11000000 ) ? maxNativeAdView.getMediaContentViewGroup() : maxNativeAdView.getMediaContentView();
                            if ( maxNativeAd.getMediaView() != null && mediaContentView != null )
                            {
                                clickableViews.add( mediaContentView );
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
            implements FiveAdLoadListener, FiveAdNativeEventListener
    {
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;

        NativeAdListener(final MaxNativeAdAdapterListener listener, final Bundle serverParameters)
        {
            this.listener = listener;
            this.serverParameters = serverParameters;
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

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( loadedNativeAd.getAdTitle() ) )
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
                            .setAdvertiser( nativeAd.getAdvertiserName() )
                            .setBody( nativeAd.getDescriptionText() )
                            .setCallToAction( nativeAd.getButtonText() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( getApplicationContext().getResources(), bitmap ) ) )
                            .setMediaView( nativeAd.getAdMainView() );
                    MaxNativeAd maxNativeAd = new MaxLineNativeAd( builder );

                    listener.onNativeAdLoaded( maxNativeAd, null );
                }
            } );
        }

        @Override
        public void onFiveAdLoadError(final FiveAdInterface ad, final FiveAdErrorCode errorCode)
        {
            log( "Native ad failed to load for slot id: " + ad.getSlotId() + " with error: " + errorCode );
            MaxAdapterError error = toMaxError( errorCode );
            listener.onNativeAdLoadFailed( error );
        }

        @Override
        public void onViewError(final FiveAdNative ad, final FiveAdErrorCode errorCode)
        {
            log( "Native ad failed to show for slot id: " + ad.getSlotId() + " with error: " + errorCode );
        }

        @Override
        public void onImpression(final FiveAdNative ad)
        {
            log( "Native ad impression tracked for slot id: " + ad.getSlotId() + "..." );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onClick(final FiveAdNative ad)
        {
            log( "Native ad clicked for slot id: " + ad.getSlotId() );
            listener.onNativeAdClicked();
        }

        @Override
        public void onRemove(final FiveAdNative ad)
        {
            log( "Native ad hidden for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPlay(final FiveAdNative ad)
        {
            log( "Native ad did play for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onPause(final FiveAdNative ad)
        {
            log( "Native ad did pause for slot id: " + ad.getSlotId() + "..." );
        }

        @Override
        public void onViewThrough(final FiveAdNative ad)
        {
            log( "Native ad completed for slot id: " + ad.getSlotId() + "..." );
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

            prepareForInteraction( clickableViews, maxNativeAdView );
        }

        // @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            FiveAdNative nativeAd = LineMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            ImageView iconImageView = null;
            for ( final View clickableView : clickableViews )
            {
                if ( clickableView instanceof ImageView )
                {
                    iconImageView = (ImageView) clickableView;
                    break;
                }
            }

            nativeAd.registerViews( container, iconImageView, clickableViews );

            return true;
        }
    }
}
