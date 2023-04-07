package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.huawei.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.Image;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.ads.consent.constant.ConsentStatus;
import com.huawei.hms.ads.consent.inter.Consent;
import com.huawei.hms.ads.nativead.MediaContent;
import com.huawei.hms.ads.nativead.MediaView;
import com.huawei.hms.ads.nativead.NativeAd;
import com.huawei.hms.ads.nativead.NativeAdConfiguration;
import com.huawei.hms.ads.nativead.NativeAdLoader;
import com.huawei.hms.ads.nativead.NativeView;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdLoadListener;
import com.huawei.hms.ads.reward.RewardAdStatusListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;
import static com.huawei.hms.ads.AdParam.ErrorCode.AD_LOADING;
import static com.huawei.hms.ads.AdParam.ErrorCode.BANNER_AD_CANCEL;
import static com.huawei.hms.ads.AdParam.ErrorCode.BANNER_AD_EXPIRE;
import static com.huawei.hms.ads.AdParam.ErrorCode.HMS_NOT_SUPPORT_SET_APP;
import static com.huawei.hms.ads.AdParam.ErrorCode.INNER;
import static com.huawei.hms.ads.AdParam.ErrorCode.INVALID_REQUEST;
import static com.huawei.hms.ads.AdParam.ErrorCode.LOW_API;
import static com.huawei.hms.ads.AdParam.ErrorCode.NETWORK_ERROR;
import static com.huawei.hms.ads.AdParam.ErrorCode.NO_AD;

public class HuaweiMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter /* MaxNativeAdAdapter */
{
    private static final String BANNER_TEST_AD_ID       = "testw6vs28auh3";
    private static final String INTERSTITIAL_TEST_AD_ID = "testb4znbuh3n2";
    private static final String REWARDED_TEST_AD_ID     = "testx9dtjwj8hp";
    private static final String NATIVE_TEST_AD_ID       = "testy63txaom86";

    private static final AtomicBoolean initialized = new AtomicBoolean();

    private BannerView     adView;
    private InterstitialAd interstitialAd;
    private RewardAd       rewardedAd;
    private NativeAd       nativeAd;
    private NativeView     nativeView;

    // Explicit default constructor declaration
    public HuaweiMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            d( "Initializing Huawei SDK..." );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            updateConsentStatus( parameters, context );
            HwAds.init( context );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return HwAds.getSDKVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log( "Destroy called for adapter " + this );

        if ( adView != null )
        {
            adView.destroy();
            adView.setAdListener( null );
            adView = null;
        }

        if ( interstitialAd != null )
        {
            interstitialAd.setAdListener( null );
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setRewardAdListener( null );
            rewardedAd = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
        }

        if ( nativeView != null )
        {
            nativeView.destroy();
            nativeView = null;
        }
    }

    //region MaxAdViewAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String adId = parameters.getThirdPartyAdPlacementId();
        d( "Loading " + adFormat.getLabel() + " AdView ad for placement: " + adId + "..." );

        updateConsentStatus( parameters, activity.getApplicationContext() );

        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                adView = new BannerView( activity.getApplicationContext() );

                if ( parameters.isTesting() )
                {
                    adView.setAdId( BANNER_TEST_AD_ID );
                }
                else
                {
                    adView.setAdId( adId );
                }

                adView.setBannerAdSize( toAdSize( adFormat ) );
                adView.pause();
                adView.setAdListener( new AdViewListener( listener ) );

                AdParam adParam = createAdParam( activity.getApplicationContext() );
                adView.loadAd( adParam );
            }
        } );
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String adId = parameters.getThirdPartyAdPlacementId();
        d( "Loading interstitial ad for ad id: " + adId + "..." );

        updateMuteState( parameters );
        updateConsentStatus( parameters, activity.getApplicationContext() );

        interstitialAd = new InterstitialAd( activity.getApplicationContext() );

        if ( parameters.isTesting() )
        {
            interstitialAd.setAdId( INTERSTITIAL_TEST_AD_ID );
        }
        else
        {
            interstitialAd.setAdId( adId );
        }

        interstitialAd.setAdListener( new InterstitialListener( listener ) );

        AdParam adParam = createAdParam( activity.getApplicationContext() );
        interstitialAd.loadAd( adParam );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String adId = parameters.getThirdPartyAdPlacementId();
        d( "Showing interstitial ad: " + adId );

        if ( interstitialAd != null && interstitialAd.isLoaded() )
        {
            interstitialAd.show( activity );
        }
        else
        {
            e( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String adId = parameters.getThirdPartyAdPlacementId();
        d( "Loading rewarded ad for ad id: " + adId + "..." );

        updateMuteState( parameters );
        updateConsentStatus( parameters, activity );

        if ( parameters.isTesting() )
        {
            rewardedAd = new RewardAd( activity, REWARDED_TEST_AD_ID );
        }
        else
        {
            rewardedAd = new RewardAd( activity, adId );
        }

        AdParam adParam = createAdParam( activity.getApplicationContext() );
        rewardedAd.loadAd( adParam, new RewardedLoadAdListener( listener ) );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String adId = parameters.getThirdPartyAdPlacementId();
        d( "Showing rewarded ad: " + adId );

        if ( rewardedAd != null && rewardedAd.isLoaded() )
        {
            // Configure reward from server.
            configureReward( parameters );

            rewardedAd.show( activity, new RewardedShowAdListener( listener ) );
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String adId = parameters.getThirdPartyAdPlacementId();
        d( "Loading native ad for ad id: " + adId + "..." );

        Bundle serverParameters = parameters.getServerParameters();
        NativeAdListener nativeAdListener = new NativeAdListener( serverParameters, activity.getApplicationContext(), listener );

        NativeAdConfiguration.Builder nativeAdOptionsBuilder = new NativeAdConfiguration.Builder();
        nativeAdOptionsBuilder.setChoicesPosition( NativeAdConfiguration.ChoicesPosition.TOP_RIGHT );
        nativeAdOptionsBuilder.setRequestMultiImages( false );

        NativeAdLoader.Builder nativeAdLoaderBuilder;
        if ( parameters.isTesting() )
        {
            nativeAdLoaderBuilder = new NativeAdLoader.Builder( activity, NATIVE_TEST_AD_ID );
        }
        else
        {
            nativeAdLoaderBuilder = new NativeAdLoader.Builder( activity, adId );
        }

        nativeAdLoaderBuilder.setNativeAdLoadedListener( nativeAdListener ).setAdListener( nativeAdListener );

        NativeAdLoader nativeAdLoader = nativeAdLoaderBuilder.setNativeAdOptions( nativeAdOptionsBuilder.build() ).build();

        AdParam adParam = createAdParam( activity.getApplicationContext() );
        nativeAdLoader.loadAd( adParam );
    }

    //endregion

    //region Helper Methods

    private static BannerAdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return BannerAdSize.BANNER_SIZE_320_50;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return BannerAdSize.BANNER_SIZE_300_250;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return BannerAdSize.BANNER_SIZE_728_90;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private MaxAdapterError toMaxShowError(int huaweiShowError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;

        switch ( huaweiShowError )
        {
            case RewardAdStatusListener.ErrorCode.INTERNAL:
            case RewardAdStatusListener.ErrorCode.REUSED:
            case RewardAdStatusListener.ErrorCode.BACKGROUND:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case RewardAdStatusListener.ErrorCode.NOT_LOADED:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), huaweiShowError, "" );
    }

    /**
     * Update the global mute state for AdMob - must be done _before_ ad load to restrict inventory which requires playing with volume.
     */
    private void updateMuteState(final MaxAdapterResponseParameters parameters)
    {
        Bundle serverParameters = parameters.getServerParameters();
        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        if ( serverParameters.containsKey( "is_muted" ) ) // Introduced in 9.10.0
        {
            HwAds.setVideoMuted( serverParameters.getBoolean( "is_muted" ) );
        }
    }

    private void updateConsentStatus(MaxAdapterParameters parameters, Context applicationContext)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            ConsentStatus gdprConsent = hasUserConsent ? ConsentStatus.PERSONALIZED : ConsentStatus.NON_PERSONALIZED;
            Consent.getInstance( applicationContext ).setConsentStatus( gdprConsent );
        }

        // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
        // https://support.google.com/googleplay/android-developer/answer/9283445?hl=en
        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            Consent.getInstance( applicationContext ).setUnderAgeOfPromise( isAgeRestrictedUser );
        }
    }

    private AdParam createAdParam(Context applicationContext)
    {
        AdParam.Builder adParamBuilder = new AdParam.Builder();

        // Based on the request here: https://github.com/AppLovin/AppLovin-MAX-SDK-Android/issues/214#issuecomment-1065114064
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences( "SharedPreferences", Context.MODE_PRIVATE );
        String tcfString = sharedPreferences.getString( "IABTCF_TCString", "" );
        if ( AppLovinSdkUtils.isValidString( tcfString ) )
        {
            adParamBuilder.setConsent( tcfString );
        }

        return adParamBuilder.build();
    }

    private static MaxAdapterError toMaxError(final int huaweiError)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( huaweiError )
        {
            case NO_AD:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case INNER:
            case BANNER_AD_CANCEL:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case INVALID_REQUEST:
            case LOW_API:
            case HMS_NOT_SUPPORT_SET_APP:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case AD_LOADING:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case BANNER_AD_EXPIRE:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), huaweiError, "" );
    }

    //endregion

    private class AdViewListener
            extends AdListener
    {
        final MaxAdViewAdapterListener listener;

        AdViewListener(MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            d( "AdView loaded" );

            listener.onAdViewAdLoaded( adView );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdFailed(int errorCode)
        {
            MaxAdapterError error = toMaxError( errorCode );
            e( "AdView failed to load with error " + error );
            listener.onAdViewAdLoadFailed( error );
        }

        @Override
        public void onAdOpened()
        {
            d( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdClosed()
        {
            d( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdClicked()
        {
            d( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdLeave()
        {
            d( "AdView will leave application" );
        }
    }

    private class InterstitialListener
            extends AdListener
    {
        private final MaxInterstitialAdapterListener listener;

        private InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            d( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdFailed(int errorCode)
        {
            MaxAdapterError error = toMaxError( errorCode );
            e( "Interstitial failed to load: " + error );
            listener.onInterstitialAdLoadFailed( error );
        }

        @Override
        public void onAdOpened()
        {
            d( "Interstitial did open" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdLeave()
        {
            d( "Interstitial left the application" );
            listener.onInterstitialAdClicked(); // MoPub adapter fires this in onAdLeave()
        }

        @Override
        public void onAdClosed()
        {
            d( "Interstitial ad closed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedLoadAdListener
            extends RewardAdLoadListener
    {
        private final MaxRewardedAdapterListener listener;

        private RewardedLoadAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRewardedLoaded()
        {
            d( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onRewardAdFailedToLoad(int errorCode)
        {
            MaxAdapterError error = toMaxError( errorCode );
            d( "Rewarded ad failed to load with error: " + error );
            listener.onRewardedAdLoadFailed( error );
        }
    }

    private class RewardedShowAdListener
            extends RewardAdStatusListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        private RewardedShowAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRewardAdOpened()
        {
            d( "Rewarded ad opened" );

            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onRewardAdFailedToShow(int errorCode)
        {
            MaxAdapterError error = toMaxShowError( errorCode );
            e( "Rewarded ad failed to show with error: " + error );
            listener.onRewardedAdDisplayFailed( error );
        }

        @Override
        public void onRewardAdClosed()
        {
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad closed" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onRewarded(Reward reward)
        {
            log( "Reward granted with reward name: " + reward.getName() + " reward amount: " + reward.getAmount() );
            hasGrantedReward = true;
        }
    }

    private class NativeAdListener
            extends AdListener
            implements NativeAd.NativeAdLoadedListener
    {
        private final Bundle                     serverParameters;
        private final Context                    context;
        private final MaxNativeAdAdapterListener listener;

        private NativeAdListener(final Bundle serverParameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.context = context;
            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(final NativeAd nativeAd)
        {
            d( "Native ad loaded" );

            HuaweiMediationAdapter.this.nativeAd = nativeAd;

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    View mediaView = null;
                    MediaContent mediaContent = nativeAd.getMediaContent();
                    Drawable mainImage = null;

                    if ( mediaContent != null )
                    {
                        MediaView huaweiMediaView = new MediaView( context );
                        huaweiMediaView.setMediaContent( mediaContent );
                        mainImage = mediaContent.getImage();

                        mediaView = huaweiMediaView;
                    }
                    else if ( nativeAd.getImages() != null )
                    {
                        List<Image> images = nativeAd.getImages();
                        if ( images.size() > 0 )
                        {
                            Image image = images.get( 0 );
                            ImageView mainImageView = new ImageView( context );
                            mainImageView.setImageDrawable( image.getDrawable() );
                            mainImage = image.getDrawable();

                            mediaView = mainImageView;
                        }
                    }

                    Image icon = nativeAd.getIcon();
                    MaxNativeAd.MaxNativeAdImage iconImage = null;

                    if ( icon != null )
                    {
                        if ( icon.getDrawable() != null )
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( icon.getDrawable() );
                        }
                        else
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( icon.getUri() );
                        }
                    }

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getTitle() )
                            .setAdvertiser( nativeAd.getAdSource() )
                            .setBody( nativeAd.getDescription() )
                            .setCallToAction( nativeAd.getCallToAction() )
                            .setIcon( iconImage )
                            .setMediaView( mediaView );
                    if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
                    {
                        builder.setMainImage( new MaxNativeAd.MaxNativeAdImage( mainImage ) );
                    }
                    MaxNativeAd maxNativeAd = new MaxHuaweiNativeAd( builder );

                    listener.onNativeAdLoaded( maxNativeAd, null );
                }
            } );
        }

        @Override
        public void onAdLoaded()
        {
            d( "Native Ad loaded" );
        }

        @Override
        public void onAdFailed(int errorCode)
        {
            MaxAdapterError error = toMaxError( errorCode );
            e( "Native failed to load: " + error );
            listener.onNativeAdLoadFailed( error );
        }

        @Override
        public void onAdImpression()
        {
            d( "Native ad displayed" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            d( "Native ad clicked" );
            listener.onNativeAdClicked();
        }
    }

    private class MaxHuaweiNativeAd
            extends MaxNativeAd
    {
        public MaxHuaweiNativeAd(final Builder builder) { super( builder ); }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            final NativeAd nativeAd = HuaweiMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return;
            }

            nativeView = new NativeView( maxNativeAdView.getContext() );

            // The Huawei Native Ad View needs to be wrapped around the main native ad view.
            View mainView = maxNativeAdView.getMainView();
            maxNativeAdView.removeView( mainView );
            nativeView.addView( mainView );
            maxNativeAdView.addView( nativeView );

            nativeView.setIconView( maxNativeAdView.getIconImageView() );
            nativeView.setTitleView( maxNativeAdView.getTitleTextView() );
            nativeView.setAdSourceView( maxNativeAdView.getAdvertiserTextView() );
            nativeView.setDescriptionView( maxNativeAdView.getBodyTextView() );
            nativeView.setCallToActionView( maxNativeAdView.getCallToActionButton() );

            View mediaView = getMediaView();
            if ( mediaView instanceof MediaView )
            {
                nativeView.setMediaView( (MediaView) mediaView );
            }
            else if ( mediaView instanceof ImageView )
            {
                nativeView.setImageView( mediaView );
            }

            nativeView.setNativeAd( nativeAd );
        }
    }
}
