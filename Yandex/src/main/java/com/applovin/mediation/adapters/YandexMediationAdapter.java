package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.yandex.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
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
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

import java.lang.reflect.Method;
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
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private static InitializationStatus status;

    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private BannerAdView   adView;

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
    }

    //endregion

    //region MaxSignalProvider

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

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
        String placementId = parameters.getThirdPartyAdPlacementId();
        String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        interstitialAd = new InterstitialAd( activity.getApplicationContext() );
        interstitialAd.setBlockId( placementId );
        interstitialAd.setInterstitialAdEventListener( new InterstitialAdListener( parameters, listener ) );

        interstitialAd.loadAd( createAdRequest( parameters ) );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAd == null || !interstitialAd.isLoaded() )
        {
            log( "Interstitial ad failed to load - ad not ready" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
            return;
        }

        interstitialAd.show();
    }

    //endregion

    //region MaxRewardedAdapter

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        rewardedAd = new RewardedAd( activity.getApplicationContext() );
        rewardedAd.setBlockId( placementId );
        rewardedAd.setRewardedAdEventListener( new RewardedAdListener( parameters, listener ) );

        rewardedAd.loadAd( createAdRequest( parameters ) );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAd == null || !rewardedAd.isLoaded() )
        {
            log( "Rewarded ad failed to load - ad not ready" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
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
        log( "Loading " + adFormatLabel + " ad for placement: " + placementId + "..." );

        updateUserConsent( parameters );

        adView = new BannerAdView( activity.getApplicationContext() );
        adView.setBlockId( placementId );
        adView.setAdSize( toAdSize( adFormat ) );
        adView.setBannerAdEventListener( new AdViewListener( adFormatLabel, listener ) );

        adView.loadAd( createAdRequest( parameters ) );
    }

    //endregion

    //region Helper Methods

    private void updateUserConsent(final MaxAdapterParameters parameters)
    {
        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                MobileAds.setUserConsent( hasUserConsent );
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
            return ( AppLovinSdk.VERSION_CODE >= 9140000 ) ? null : false;
        }
    }

    private AdRequest createAdRequest(MaxAdapterResponseParameters parameters)
    {
        // Required parameters given by Yandex
        Map<String, String> adRequestParameters = new HashMap<>( 3 );
        adRequestParameters.put( "adapter_network_name", "applovin" );
        adRequestParameters.put( "adapter_version", getAdapterVersion() );
        adRequestParameters.put( "adapter_network_sdk_version", AppLovinSdk.VERSION );

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
        public void onLeftApplication()
        {
            log( "Interstitial clicked and left application" );
            listener.onInterstitialAdClicked();
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
        public void onLeftApplication()
        {
            log( "Rewarded ad clicked and left application" );
            listener.onRewardedAdClicked();
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
        public void onLeftApplication()
        {
            log( adFormatLabel + " ad clicked and left application" );
            listener.onAdViewAdClicked();
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
}
