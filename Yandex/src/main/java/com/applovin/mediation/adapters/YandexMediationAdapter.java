package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.yandex.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.AdType;
import com.yandex.mobile.ads.common.BidderTokenLoadListener;
import com.yandex.mobile.ads.common.BidderTokenLoader;
import com.yandex.mobile.ads.common.BidderTokenRequestConfiguration;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.nativeads.MediaView;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdAssets;
import com.yandex.mobile.ads.nativeads.NativeAdEventListener;
import com.yandex.mobile.ads.nativeads.NativeAdException;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdLoader;
import com.yandex.mobile.ads.nativeads.NativeAdRequestConfiguration;
import com.yandex.mobile.ads.nativeads.NativeAdView;
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Andrew Tian on 9/16/19.
 */
public class YandexMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final int TITLE_LABEL_TAG          = 1;
    private static final int MEDIA_VIEW_CONTAINER_TAG = 2;
    private static final int ICON_VIEW_TAG            = 3;
    private static final int BODY_VIEW_TAG            = 4;
    private static final int CALL_TO_ACTION_VIEW_TAG  = 5;
    private static final int ADVERTISER_VIEW_TAG      = 8;

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    // Required parameters given by Yandex
    private static final Map<String, String> adRequestParameters = new HashMap<String, String>()
    {
        {
            put( "adapter_network_name", "applovin" );
            put( "adapter_version", BuildConfig.VERSION_NAME );
            put( "adapter_network_sdk_version", AppLovinSdk.VERSION );
        }
    };

    private static InitializationStatus status;

    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private BannerAdView   adView;
    private NativeAd       nativeAd;
    private NativeAdView   nativeAdView;

    private InterstitialAdListener interstitialAdListener;
    private RewardedAdListener     rewardedAdListener;

    // Explicit default constructor declaration
    public YandexMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter

    @Override
    public String getSdkVersion()
    {
        return MobileAds.getLibraryVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    // @Override
    @Nullable
    public Boolean shouldLoadAdsOnUiThread(final MaxAdFormat adFormat)
    {
        // Yandex requires all ad formats to be loaded on UI thread.
        return true;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialAd != null )
        {
            interstitialAd.setAdEventListener( null );
            interstitialAdListener = null;
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setAdEventListener( null );
            rewardedAdListener = null;
            rewardedAd = null;
        }

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }

        nativeAd = null;
        nativeAdView = null;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            log( "Initializing Yandex SDK" + ( parameters.isTesting() ? " in test mode " : "" ) + "..." );

            status = InitializationStatus.INITIALIZING;

            updatePrivacySettings( parameters );

            if ( parameters.isTesting() )
            {
                MobileAds.enableLogging( true );
            }

            Context context = getContext( activity );

            MobileAds.initialize( context, new InitializationListener()
            {
                @Override
                public void onInitializationCompleted()
                {
                    log( "Yandex SDK initialized" );

                    status = InitializationStatus.INITIALIZED_UNKNOWN;
                    onCompletionListener.onCompletion( status, null );
                }
            } );
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    //endregion

    //region MaxSignalProvider

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacySettings( parameters );

        Context context = getContext( activity );

        BidderTokenRequestConfiguration bidderTokenRequest = createBidderTokenRequestConfiguration( parameters, context, parameters.getAdFormat() );

        BidderTokenLoader.loadBidderToken( context, bidderTokenRequest, new BidderTokenLoadListener()
        {
            @Override
            public void onBidderTokenLoaded(@NonNull final String bidderToken)
            {
                log( "Signal collection successful" );

                callback.onSignalCollected( bidderToken );
            }

            @Override
            public void onBidderTokenFailedToLoad(@NonNull final String failureReason)
            {
                log( "Signal collection failed: " + failureReason );

                callback.onSignalCollectionFailed( failureReason );
            }
        } );
    }

    //endregion

    //region MaxInterstitialAdapter

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        updatePrivacySettings( parameters );

        Runnable loadInterstitialAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                InterstitialAdLoader interstitialAdLoader = new InterstitialAdLoader( getContext( activity ) );
                interstitialAdListener = new InterstitialAdListener( parameters, listener );
                interstitialAdLoader.setAdLoadListener( interstitialAdListener );
                interstitialAdLoader.loadAd( createAdRequestConfiguration( placementId, parameters ) );
            }
        };

        loadAdOnUiThread( loadInterstitialAdRunnable );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAd == null )
        {
            log( "Interstitial ad failed to show - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                                         MaxAdapterError.AD_NOT_READY.getMessage() ) );
            return;
        }

        if ( activity == null )
        {
            log( "Interstitial ad display failed: Activity is null" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.MISSING_ACTIVITY );
            return;
        }

        interstitialAd.setAdEventListener( interstitialAdListener );
        interstitialAd.show( activity );
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );

        updatePrivacySettings( parameters );

        Runnable loadRewardedAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                RewardedAdLoader rewardedAdLoader = new RewardedAdLoader( getContext( activity ) );
                rewardedAdListener = new RewardedAdListener( parameters, listener );
                rewardedAdLoader.setAdLoadListener( rewardedAdListener );
                rewardedAdLoader.loadAd( createAdRequestConfiguration( placementId, parameters ) );
            }
        };

        loadAdOnUiThread( loadRewardedAdRunnable );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAd == null )
        {
            log( "Rewarded ad failed to show - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );
            return;
        }

        if ( activity == null )
        {
            log( "Rewarded ad display failed: Activity is null" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.MISSING_ACTIVITY );
            return;
        }

        // Configure reward from server.
        configureReward( parameters );

        rewardedAd.setAdEventListener( rewardedAdListener );
        rewardedAd.show( activity );
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        // NOTE: Native banners and MRECs are not supported due to the Yandex SDK's requirement for a media view.
        final String adFormatLabel = adFormat.getLabel();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        final Context applicationContext = getContext( activity );

        updatePrivacySettings( parameters );

        Runnable loadAdViewAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                // Check if adaptive ad view sizes should be used
                boolean isAdaptiveAdViewEnabled = parameters.getServerParameters().getBoolean( "adaptive_banner", false );
                if ( isAdaptiveAdViewEnabled && AppLovinSdk.VERSION_CODE < 13_02_00_99 )
                {
                    isAdaptiveAdViewEnabled = false;
                    userError( "Please update AppLovin MAX SDK to version 13.2.0 or higher in order to use Yandex adaptive ads" );
                }

                BannerAdSize adSize = toYandexBannerAdSize( adFormat, isAdaptiveAdViewEnabled, parameters, getContext( activity ) );
                adView = new BannerAdView( applicationContext );
                adView.setAdUnitId( placementId );
                adView.setAdSize( adSize );
                adView.setBannerAdEventListener( new AdViewListener( adFormatLabel, listener ) );
                adView.loadAd( createAdRequest( parameters ) );
            }
        };

        loadAdOnUiThread( loadAdViewAdRunnable );
    }

    //endregion

    //region MaxNativeAdapter

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        final Context applicationContext = getContext( activity );

        updatePrivacySettings( parameters );

        Runnable loadNativeAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                NativeAdLoader nativeAdLoader = new NativeAdLoader( applicationContext );
                nativeAdLoader.setNativeAdLoadListener( new NativeAdListener( parameters, applicationContext, listener ) );
                nativeAdLoader.loadAd( createNativeAdRequestConfiguration( placementId, parameters ) );
            }
        };

        loadAdOnUiThread( loadNativeAdRunnable );
    }

    //endregion

    //region Helper Methods

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplication() : getApplicationContext();
    }

    private void loadAdOnUiThread(final Runnable loadOrShowRunnable)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11_08_03_00 )
        {
            // The `shouldLoadAdsOnUiThread` setting is added in SDK version 11.8.3. So, the SDK should already be running this on UI thread.
            loadOrShowRunnable.run();
        }
        else
        {
            AppLovinSdkUtils.runOnUiThread( loadOrShowRunnable );
        }
    }

    private void updatePrivacySettings(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            MobileAds.setUserConsent( hasUserConsent );
        }
    }

    private AdRequest createAdRequest(MaxAdapterResponseParameters parameters)
    {
        AdRequest.Builder builder = new AdRequest.Builder()
                .setParameters( adRequestParameters );

        final String bidResponse = parameters.getBidResponse();
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            builder.setBiddingData( bidResponse );
        }

        return builder.build();
    }

    private AdRequestConfiguration createAdRequestConfiguration(final String placementId, final MaxAdapterResponseParameters parameters)
    {
        AdRequestConfiguration.Builder builder = new AdRequestConfiguration.Builder( placementId )
                .setParameters( adRequestParameters );

        final String bidResponse = parameters.getBidResponse();
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            builder.setBiddingData( bidResponse );
        }

        return builder.build();
    }

    private NativeAdRequestConfiguration createNativeAdRequestConfiguration(final String placementId, final MaxAdapterResponseParameters parameters)
    {
        NativeAdRequestConfiguration.Builder builder = new NativeAdRequestConfiguration.Builder( placementId )
                .setParameters( adRequestParameters )
                .setShouldLoadImagesAutomatically( true ); // images will be loaded before ad is ready

        final String bidResponse = parameters.getBidResponse();
        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            builder.setBiddingData( bidResponse );
        }

        return builder.build();
    }

    private BidderTokenRequestConfiguration createBidderTokenRequestConfiguration(final MaxAdapterSignalCollectionParameters parameters, final Context context, final MaxAdFormat adFormat)
    {
        AdType adType = toAdType( adFormat );
        BidderTokenRequestConfiguration.Builder requestBuilder = new BidderTokenRequestConfiguration.Builder( adType );

        if ( adType == AdType.BANNER )
        {
            Object isAdaptiveBannerObj = parameters.getLocalExtraParameters().get( "adaptive_banner" );
            boolean isAdaptiveAdViewEnabled = isAdaptiveBannerObj instanceof String && "true".equalsIgnoreCase( (String) isAdaptiveBannerObj );

            if ( isAdaptiveAdViewEnabled && AppLovinSdk.VERSION_CODE < 13_02_00_99 )
            {
                isAdaptiveAdViewEnabled = false;
                userError( "Please update AppLovin MAX SDK to version 13.2.0 or higher in order to use Yandex adaptive ads" );
            }
            BannerAdSize adSize = toYandexBannerAdSize( adFormat, isAdaptiveAdViewEnabled, parameters, context );
            requestBuilder.setBannerAdSize( adSize );
        }

        return requestBuilder.setParameters( adRequestParameters ).build();
    }

    private static AdType toAdType(final MaxAdFormat adFormat)
    {
        if ( adFormat.isAdViewAd() )
        {
            return AdType.BANNER;
        }
        else if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            return AdType.INTERSTITIAL;
        }
        else if ( adFormat == MaxAdFormat.REWARDED )
        {
            return AdType.REWARDED;
        }
        else if ( adFormat == MaxAdFormat.APP_OPEN )
        {
            return AdType.APP_OPEN_AD;
        }
        else if ( adFormat == MaxAdFormat.NATIVE )
        {
            return AdType.NATIVE;
        }
        else
        {
            return AdType.UNKNOWN;
        }
    }

    private BannerAdSize toYandexBannerAdSize(final MaxAdFormat adFormat,
                                              final boolean isAdaptiveAdViewEnabled,
                                              final MaxAdapterParameters parameters,
                                              final Context context)
    {
        if ( !adFormat.isAdViewAd() )
        {
            throw new IllegalArgumentException( "Unsupported ad view ad format: " + adFormat.getLabel() );
        }

        if ( isAdaptiveAdViewEnabled && isAdaptiveAdViewFormat( adFormat, parameters ) )
        {
            return getAdaptiveAdSize( parameters, context );
        }

        return BannerAdSize.fixedSize( context, adFormat.getSize().getWidth(), adFormat.getSize().getHeight() );
    }

    private BannerAdSize getAdaptiveAdSize(final MaxAdapterParameters parameters, final Context context)
    {
        final int adaptiveAdWidth = getAdaptiveAdViewWidth( parameters, context );

        if ( isInlineAdaptiveAdView( parameters ) )
        {
            final int inlineMaximumHeight = getInlineAdaptiveAdViewMaximumHeight( parameters );
            if ( inlineMaximumHeight > 0 )
            {
                return BannerAdSize.inlineSize( context, adaptiveAdWidth, inlineMaximumHeight );
            }

            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int deviceHeight = AppLovinSdkUtils.pxToDp( context, displayMetrics.heightPixels );
            return BannerAdSize.inlineSize( context, adaptiveAdWidth, deviceHeight );
        }

        // Anchored banners use the default adaptive height
        final int anchoredHeight = MaxAdFormat.BANNER.getAdaptiveSize( adaptiveAdWidth, context ).getHeight();
        return BannerAdSize.fixedSize( context, adaptiveAdWidth, anchoredHeight );
    }

    private static MaxAdapterError toMaxError(final AdRequestError yandexError)
    {
        final int yandexErrorCode = yandexError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( yandexErrorCode )
        {
            case AdRequestError.Code.INTERNAL_ERROR:
            case AdRequestError.Code.SYSTEM_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case AdRequestError.Code.INVALID_REQUEST:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case AdRequestError.Code.NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case AdRequestError.Code.NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case AdRequestError.Code.UNKNOWN_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
        }

        return new MaxAdapterError( adapterError, yandexError.getCode(), yandexError.getDescription() );
    }

    //endregion

    //region Ad Listeners

    private class InterstitialAdListener
            implements InterstitialAdLoadListener, InterstitialAdEventListener
    {
        private final MaxAdapterResponseParameters   parameters;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final MaxAdapterResponseParameters parameters, final MaxInterstitialAdapterListener listener)
        {
            this.parameters = parameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial ad loaded" );
            YandexMediationAdapter.this.interstitialAd = interstitialAd;
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError)
        {
            log( "Interstitial ad failed to load with error code " + adRequestError.getCode() + " and description: " + adRequestError.getDescription() );

            MaxAdapterError adapterError = toMaxError( adRequestError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShown()
        {
            log( "Interstitial ad shown" );

            // Fire callbacks here for test mode ads since onImpression() doesn't get called for them
            if ( parameters.isTesting() )
            {
                listener.onInterstitialAdDisplayed();
            }
        }

        // Note: This method is generally called with a 3 second delay after the ad has been displayed.
        //       This method is not called for test mode ads.
        @Override
        public void onAdImpression(@Nullable final ImpressionData impressionData)
        {
            log( "Interstitial ad impression tracked" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdFailedToShow(@NonNull final AdError adError)
        {
            log( "Interstitial ad failed to show with error description: " + adError.getDescription() );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         0,
                                                                         adError.getDescription() ) );
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdDismissed()
        {
            log( "Interstitial ad dismissed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements RewardedAdLoadListener, RewardedAdEventListener
    {
        private final MaxAdapterResponseParameters parameters;
        private final MaxRewardedAdapterListener   listener;

        private boolean hasGrantedReward;

        RewardedAdListener(final MaxAdapterResponseParameters parameters, final MaxRewardedAdapterListener listener)
        {
            this.parameters = parameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final RewardedAd rewardedAd)
        {
            log( "Rewarded ad loaded" );
            YandexMediationAdapter.this.rewardedAd = rewardedAd;
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError)
        {
            log( "Rewarded ad failed to load with error code " + adRequestError.getCode() + " and description: " + adRequestError.getDescription() );

            MaxAdapterError adapterError = toMaxError( adRequestError );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdShown()
        {
            log( "Rewarded ad shown" );

            // Fire callbacks here for test mode ads since onImpression() doesn't get called for them
            if ( parameters.isTesting() )
            {
                listener.onRewardedAdDisplayed();
            }
        }

        // Note: This method is generally called with a 3 second delay after the ad has been displayed.
        //       This method is not called for test mode ads.
        @Override
        public void onAdImpression(@Nullable final ImpressionData impressionData)
        {
            log( "Rewarded ad impression tracked" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdFailedToShow(@NonNull final AdError adError)
        {
            log( "Rewarded ad failed to show with error description: " + adError.getDescription() );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     0,
                                                                     adError.getDescription() ) );
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onRewarded(@NonNull final Reward reward)
        {
            log( "Rewarded user with reward: " + reward.getAmount() + " " + reward.getType() );
            hasGrantedReward = true;
        }

        @Override
        public void onAdDismissed()
        {
            log( "Rewarded ad hidden" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            implements BannerAdEventListener
    {
        private final String                   adFormatLabel;
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final String adFormatLabel, final MaxAdViewAdapterListener listener)
        {
            this.adFormatLabel = adFormatLabel;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( adFormatLabel + " ad loaded" );

            Bundle extraInfo = new Bundle( 2 );
            if ( adView != null )
            {
                BannerAdSize adSize = adView.getAdSize();
                extraInfo.putInt( "ad_width", adSize.getWidth() );
                extraInfo.putInt( "ad_height", adSize.getHeight() );
            }

            listener.onAdViewAdLoaded( adView, extraInfo );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError)
        {
            log( adFormatLabel + " ad failed to load with error code " + adRequestError.getCode() + " and description: " + adRequestError.getDescription() );

            MaxAdapterError adapterError = toMaxError( adRequestError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "AdView ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onLeftApplication()
        {
            log( adFormatLabel + " ad left application after click" );
        }

        @Override
        public void onReturnedToApplication()
        {
            log( adFormatLabel + " ad returned to application" );
        }

        // Note: This method is generally called with a 3 second delay after the ad has been displayed.
        //       This method is not called for test mode ads.
        public void onImpression(@Nullable final ImpressionData impressionData)
        {
            log( "AdView ad impression tracked" );
            listener.onAdViewAdDisplayed();
        }
    }

    //endregion

    private class NativeAdListener
            implements NativeAdLoadListener, NativeAdEventListener
    {
        private final String                     placementId;
        private final Bundle                     serverParameters;
        private final Context                    applicationContext;
        private final MaxNativeAdAdapterListener listener;

        private NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.placementId = parameters.getThirdPartyAdPlacementId();
            this.serverParameters = parameters.getServerParameters();
            this.applicationContext = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad loaded: " + placementId );

            YandexMediationAdapter.this.nativeAd = nativeAd;
            nativeAd.setNativeAdEventListener( this );

            final NativeAdAssets assets = nativeAd.getAdAssets();

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( assets.getTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    Drawable iconDrawable = null;
                    if ( assets.getIcon() != null )
                    {
                        iconDrawable = new BitmapDrawable( applicationContext.getResources(), assets.getIcon().getBitmap() );
                    }

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( assets.getTitle() )
                            .setAdvertiser( assets.getDomain() )
                            .setBody( assets.getBody() )
                            .setCallToAction( assets.getCallToAction() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                            .setOptionsView( new ImageView( applicationContext ) )
                            .setMediaView( new MediaView( applicationContext ) ); // Yandex requires rendering MediaView with their own bind method

                    if ( AppLovinSdk.VERSION_CODE >= 11_07_00_00 && assets.getRating() != null )
                    {
                        builder.setStarRating( (double) assets.getRating() );
                    }
                    MaxNativeAd maxNativeAd = new MaxYandexNativeAd( builder );

                    listener.onNativeAdLoaded( maxNativeAd, null );
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError)
        {
            MaxAdapterError adapterError = toMaxError( adRequestError );
            log( "Native ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onImpression(@Nullable final ImpressionData impressionData)
        {
            log( "Native ad (" + placementId + ") shown" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad (" + placementId + ") clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onLeftApplication()
        {
            log( "Native ad (" + placementId + ") left application" );
        }

        @Override
        public void onReturnedToApplication()
        {
            log( "Native ad (" + placementId + ") returned to application" );
        }
    }

    private class MaxYandexNativeAd
            extends MaxNativeAd
    {
        public MaxYandexNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            if ( YandexMediationAdapter.this.nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            nativeAdView = new NativeAdView( container.getContext() );

            // Native integrations
            if ( container instanceof MaxNativeAdView )
            {
                // The Yandex Native Ad View needs to be wrapped around the main native ad view to get impressions.
                MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;
                View mainView = maxNativeAdView.getMainView();
                maxNativeAdView.removeView( mainView );
                nativeAdView.addView( mainView );
                maxNativeAdView.addView( nativeAdView );

                final NativeAdViewBinder binder = new NativeAdViewBinder.Builder( nativeAdView )
                        .setIconView( maxNativeAdView.getIconImageView() )
                        .setTitleView( maxNativeAdView.getTitleTextView() )
                        .setDomainView( maxNativeAdView.getAdvertiserTextView() )
                        .setBodyView( maxNativeAdView.getBodyTextView() )
                        .setMediaView( (MediaView) getMediaView() )
                        .setFeedbackView( (ImageView) getOptionsView() )
                        .setCallToActionView( maxNativeAdView.getCallToActionButton() )
                        .build();

                try
                {
                    YandexMediationAdapter.this.nativeAd.bindNativeAd( binder );
                    nativeAdView.setVisibility( View.VISIBLE );
                }
                catch ( NativeAdException exception )
                {
                    e( "Failed to register native ad views.", exception );
                }
            }
            // Plugins
            else
            {
                // The Yandex Native Ad View needs to be wrapped around the main native ad view to get impressions.
                try
                {
                    // Remove mediaView from its parent
                    MediaView mediaView = (MediaView) getMediaView();
                    ViewGroup mediaViewContainer = (ViewGroup) mediaView.getParent();
                    mediaViewContainer.removeView( mediaView );

                    // Add mediaView to nativeAdView
                    ViewGroup.LayoutParams mediaViewLayout = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    nativeAdView.addView( mediaView, mediaViewLayout );

                    // Add nativeAdview to the original parent of the mediaView, noting that Flutter has a layout to add it but React Native doesn't.
                    boolean hasPluginLayout = ( mediaViewContainer instanceof RelativeLayout || mediaViewContainer instanceof FrameLayout );
                    if ( hasPluginLayout )
                    {
                        ViewGroup.LayoutParams nativeAdViewLayout = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                        mediaViewContainer.addView( nativeAdView, nativeAdViewLayout );
                    }
                    else
                    {
                        nativeAdView.measure(
                                View.MeasureSpec.makeMeasureSpec( mediaViewContainer.getWidth(), View.MeasureSpec.EXACTLY ),
                                View.MeasureSpec.makeMeasureSpec( mediaViewContainer.getHeight(), View.MeasureSpec.EXACTLY ) );
                        nativeAdView.layout( 0, 0, mediaViewContainer.getWidth(), mediaViewContainer.getHeight() );
                        mediaViewContainer.addView( nativeAdView );
                    }

                    // Provide the asset views to NativeAdViewBinder for binding them to the Yandex nativeAd.
                    final NativeAdViewBinder.Builder binder = new NativeAdViewBinder.Builder( nativeAdView );
                    binder.setMediaView( mediaView );

                    // Set the rest of the asset views from clickableViews.
                    for ( View view : clickableViews )
                    {
                        Object viewTag = view.getTag();
                        if ( viewTag == null ) continue;

                        int tag = (int) viewTag;

                        if ( tag == ICON_VIEW_TAG )
                        {
                            if ( view instanceof ImageView )
                            {
                                binder.setIconView( (ImageView) view );
                            }
                        }
                        else if ( tag == TITLE_LABEL_TAG )
                        {
                            TextView textView = createTextViewIfNeeded( view );
                            if ( textView != null )
                            {
                                binder.setTitleView( textView );
                            }
                        }
                        else if ( tag == ADVERTISER_VIEW_TAG )
                        {
                            TextView textView = createTextViewIfNeeded( view );
                            if ( textView != null )
                            {
                                binder.setDomainView( textView );
                            }
                        }
                        else if ( tag == BODY_VIEW_TAG )
                        {
                            TextView textView = createTextViewIfNeeded( view );
                            if ( textView != null )
                            {
                                binder.setBodyView( textView );
                            }
                        }
                        else if ( tag == CALL_TO_ACTION_VIEW_TAG )
                        {
                            TextView textView = createTextViewIfNeeded( view );
                            if ( textView != null )
                            {
                                binder.setCallToActionView( textView );
                            }
                        }
                    }

                    YandexMediationAdapter.this.nativeAd.bindNativeAd( binder.build() );
                    nativeAdView.setVisibility( View.VISIBLE );
                }
                catch ( Throwable th )
                {
                    e( "Failed to register native ad views.", th );
                }
            }

            return true;
        }

        private TextView createTextViewIfNeeded(final View view)
        {
            TextView textView = null;
            if ( view instanceof TextView )
            {
                textView = (TextView) view;
            }
            else if ( view instanceof ViewGroup )
            {
                textView = new TextView( view.getContext() );
                // Hide the text content since this textView is for receiving touch events.
                textView.setAlpha( 0 );
                ( (ViewGroup) view ).addView( textView );
            }
            return textView;
        }
    }
}
