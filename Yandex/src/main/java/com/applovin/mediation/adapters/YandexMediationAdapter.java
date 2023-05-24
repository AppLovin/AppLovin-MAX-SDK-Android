package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.yandex.mobile.ads.banner.AdSize;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.BidderTokenLoadListener;
import com.yandex.mobile.ads.common.BidderTokenLoader;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
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

import java.util.HashMap;
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
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    // Required parameters given by Yandex
    private static final Map<String, String> adRequestParameters = new HashMap<>( 3 );

    private static InitializationStatus status;

    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private BannerAdView   adView;
    private NativeAd       nativeAd;
    private NativeAdView   nativeAdView;

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
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.destroy();
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
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            log( "Initializing Yandex SDK" + ( parameters.isTesting() ? " in test mode " : "" ) + "..." );

            status = InitializationStatus.INITIALIZING;

            updateUserConsent( parameters );

            if ( parameters.isTesting() )
            {
                MobileAds.enableLogging( true );
            }

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

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

        adRequestParameters.put( "adapter_network_name", "applovin" );
        adRequestParameters.put( "adapter_version", getAdapterVersion() );
        adRequestParameters.put( "adapter_network_sdk_version", AppLovinSdk.VERSION );
    }

    //endregion

    //region MaxSignalProvider

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserConsent( parameters );

        BidderTokenLoader.loadBidderToken( activity, new BidderTokenLoadListener()
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
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        Runnable loadInterstitialAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                interstitialAd = new InterstitialAd( activity.getApplicationContext() );
                interstitialAd.setAdUnitId( placementId );
                interstitialAd.setInterstitialAdEventListener( new InterstitialAdListener( parameters, listener ) );
                interstitialAd.loadAd( createAdRequest( parameters ) );
            }
        };

        loadAdOnUiThread( loadInterstitialAdRunnable );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAd == null || !interstitialAd.isLoaded() )
        {
            log( "Interstitial ad failed to load - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
            return;
        }

        interstitialAd.show();
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "rewarded ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        Runnable loadRewardedAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                rewardedAd = new RewardedAd( activity.getApplicationContext() );
                rewardedAd.setAdUnitId( placementId );
                rewardedAd.setRewardedAdEventListener( new RewardedAdListener( parameters, listener ) );
                rewardedAd.loadAd( createAdRequest( parameters ) );
            }
        };

        loadAdOnUiThread( loadRewardedAdRunnable );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAd == null || !rewardedAd.isLoaded() )
        {
            log( "Rewarded ad failed to load - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
            return;
        }

        // Configure reward from server.
        configureReward( parameters );

        rewardedAd.show();
    }

    //endregion

    //region MaxAdViewAdapter

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String adFormatLabel = adFormat.getLabel();
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + adFormatLabel + " ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        Runnable loadAdViewAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                adView = new BannerAdView( activity.getApplicationContext() );
                adView.setAdUnitId( placementId );
                adView.setAdSize( toAdSize( adFormat ) );
                adView.setBannerAdEventListener( new AdViewListener( adFormatLabel, listener ) );
                adView.loadAd( createAdRequest( parameters ) );
            }
        };

        loadAdOnUiThread( loadAdViewAdRunnable );
    }

    //endregion

    //region MaxNativeAdapter

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        final Context applicationContext = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

        updateUserConsent( parameters );

        Runnable loadNativeAdRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                NativeAdLoader nativeAdLoader = new NativeAdLoader( applicationContext );
                nativeAdLoader.setNativeAdLoadListener( new NativeAdListener( parameters, applicationContext, listener ) );
                NativeAdRequestConfiguration nativeAdRequestConfiguration = new NativeAdRequestConfiguration.Builder( placementId )
                        .setBiddingData( bidResponse )
                        .setParameters( adRequestParameters )
                        .setShouldLoadImagesAutomatically( true ) // images will be loaded before ad is ready
                        .build();
                nativeAdLoader.loadAd( nativeAdRequestConfiguration );
            }
        };

        loadAdOnUiThread( loadNativeAdRunnable );
    }

    //endregion

    //region Helper Methods

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

    private void updateUserConsent(final MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            MobileAds.setUserConsent( hasUserConsent );
        }
    }

    private AdRequest createAdRequest(MaxAdapterResponseParameters parameters)
    {
        return new AdRequest.Builder()
                .setBiddingData( parameters.getBidResponse() )
                .setParameters( adRequestParameters )
                .build();
    }

    private static AdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.BANNER_320x50;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.BANNER_300x250;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.BANNER_728x90;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
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

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), yandexError.getCode(), yandexError.getDescription() );
    }

    //endregion

    //region Ad Listeners

    private class InterstitialAdListener
            implements InterstitialAdEventListener
    {
        private final MaxAdapterResponseParameters   parameters;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final MaxAdapterResponseParameters parameters, final MaxInterstitialAdapterListener listener)
        {
            this.parameters = parameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdFailedToLoad(final AdRequestError adRequestError)
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
        public void onImpression(final ImpressionData impressionData)
        {
            log( "Interstitial ad impression tracked" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onLeftApplication()
        {
            log( "Interstitial left application after click" );
        }

        @Override
        public void onReturnedToApplication()
        {
            log( "Interstitial returned to application" );
        }

        @Override
        public void onAdDismissed()
        {
            log( "Interstitial ad dismissed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements RewardedAdEventListener
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
        public void onAdLoaded()
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdFailedToLoad(final AdRequestError adRequestError)
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
                listener.onRewardedAdVideoStarted();
            }
        }

        // Note: This method is generally called with a 3 second delay after the ad has been displayed.
        //       This method is not called for test mode ads.
        public void onImpression(final ImpressionData impressionData)
        {
            log( "Rewarded ad impression tracked" );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onLeftApplication()
        {
            log( "Rewarded ad left application after click" );
        }

        @Override
        public void onReturnedToApplication()
        {
            log( "Rewarded ad returned to application" );
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
            listener.onRewardedAdVideoCompleted();

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
            listener.onAdViewAdLoaded( adView );
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
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

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

                    LinearLayout disclaimerSponsoredOptionsViewContainer = new LinearLayout( applicationContext );
                    TextView disclaimer = new TextView( applicationContext );
                    TextView sponsored = new TextView( applicationContext );

                    disclaimer.setText( assets.getWarning() );
                    sponsored.setText( assets.getSponsored() );
                    disclaimerSponsoredOptionsViewContainer.addView( disclaimer ); // new TextView as disclaimer view
                    disclaimerSponsoredOptionsViewContainer.addView( sponsored ); // new TextView as sponsored view
                    disclaimerSponsoredOptionsViewContainer.addView( new ImageView( applicationContext ) ); // new ImageView as options view

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( assets.getTitle() )
                            .setAdvertiser( assets.getDomain() )
                            .setBody( assets.getBody() )
                            .setCallToAction( assets.getCallToAction() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                            .setOptionsView( disclaimerSponsoredOptionsViewContainer )
                            .setMediaView( new MediaView( applicationContext ) ); // Yandex requires rendering MediaView with their own bind method

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
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            if ( YandexMediationAdapter.this.nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return;
            }

            nativeAdView = new NativeAdView( maxNativeAdView.getContext() );

            // The Yandex Native Ad View needs to be wrapped around the main native ad view to get impressions.
            View mainView = maxNativeAdView.getMainView();
            maxNativeAdView.removeView( mainView );
            nativeAdView.addView( mainView );
            maxNativeAdView.addView( nativeAdView );

            LinearLayout disclaimerSponsoredOptionsViewContainer = (LinearLayout) maxNativeAdView.getOptionsContentViewGroup().getChildAt( 0 );

            final NativeAdViewBinder binder = new NativeAdViewBinder.Builder( nativeAdView )
                    .setIconView( maxNativeAdView.getIconImageView() )
                    .setTitleView( maxNativeAdView.getTitleTextView() )
                    .setDomainView( maxNativeAdView.getAdvertiserTextView() )
                    .setBodyView( maxNativeAdView.getBodyTextView() )
                    .setMediaView( (MediaView) getMediaView() )
                    .setWarningView( (TextView) disclaimerSponsoredOptionsViewContainer.getChildAt( 0 ) )
                    .setSponsoredView( (TextView) disclaimerSponsoredOptionsViewContainer.getChildAt( 1 ) )
                    .setFeedbackView( (ImageView) disclaimerSponsoredOptionsViewContainer.getChildAt( 2 ) )
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
    }
}
