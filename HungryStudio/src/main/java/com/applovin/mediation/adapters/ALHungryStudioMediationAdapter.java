package com.applovin.mediation.adapters;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.applovin.impl.mediation.MaxRewardImpl;
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
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.sdk.AppLovinSdk;
import com.hs.adx.ad.api.HSAdxBanner;
import com.hs.adx.ad.api.HSAdxInterstitial;
import com.hs.adx.ad.api.HSAdxReward;
import com.hs.adx.ad.base.AdSize;
import com.hs.adx.ad.base.IAdListener;
import com.hs.adx.ad.core.AdError;
import com.hs.adx.api.HellaAd;
import com.hs.adx.api.HellaAdsSdk;
import com.hs.adx.bid.HSBidTokenProvider;
import com.hs.adx.utils.AppUtils;

public class ALHungryStudioMediationAdapter extends MediationAdapterBase implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxSignalProvider {

    private static final String TAG = "ALHungryStudioMediationAdapter";
    private HSAdxBanner mHsBanner;
    private HSAdxInterstitial mInterstitial;
    private HSAdxReward mRewardedVideoAd;

    public ALHungryStudioMediationAdapter(AppLovinSdk appLovinSdk) {
        super(appLovinSdk);
    }

    @Override
    public void initialize(MaxAdapterInitializationParameters maxAdapterInitializationParameters, Activity activity, OnCompletionListener onCompletionListener) {
        try {
            onCompletionListener.onCompletion(InitializationStatus.INITIALIZING, "HS Ads try init");
            if (HellaAdsSdk.hasInitialized()) {
                log("has init hs-sdk return");
                onCompletionListener.onCompletion(InitializationStatus.INITIALIZED_SUCCESS, "HS Ads already initialized");
                return;
            }

            final Bundle serverParameters = maxAdapterInitializationParameters.getServerParameters();
            final String appId = serverParameters.getString("app_id");
            log("Initializing appId = " + appId);
            HellaAdsSdk.init(activity.getApplicationContext(), appId, new HellaAdsSdk.OnInitListener() {
                @Override
                public void onInitSuccess() {
                    log("Initializing HS Ads success");
                    onCompletionListener.onCompletion(InitializationStatus.INITIALIZED_SUCCESS, "HS Ads initialize success");
                }

                @Override
                public void onInitFail(String errorMsg) {
                    log("Initializing HS Ads fail =" + errorMsg);
                    onCompletionListener.onCompletion(InitializationStatus.INITIALIZED_FAILURE, "HS Ads initialize fail");
                }
            });
        } catch (Exception e) {
            log("Initializing HS Ads has encountered an exception." + e.getMessage());
            onCompletionListener.onCompletion(InitializationStatus.INITIALIZED_FAILURE, "Initializing HS Ads has encountered an exception");
            return;
        }
        log("HS Ads initialized");
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback) {
        log("Collecting signal...");
        HSBidTokenProvider.getBidderToken(new HSBidTokenProvider.IBiddingTokenCallbackListener() {
            @Override
            public void onBiddingTokenCollected(String bidToken) {
                callback.onSignalCollected(bidToken);
            }

            @Override
            public void onBiddingTokenFailed(String errorMsg) {
                callback.onSignalCollectionFailed(errorMsg);
            }
        });
    }

    @Override
    public void loadAdViewAd(MaxAdapterResponseParameters maxAdapterResponseParameters, MaxAdFormat maxAdFormat, Activity activity, MaxAdViewAdapterListener maxAdViewAdapterListener) {
        String mPlacementId = maxAdapterResponseParameters.getThirdPartyAdPlacementId();
        String bidResponse = maxAdapterResponseParameters.getBidResponse();
        log("HsBanner Ads load mPlacementId = " + mPlacementId+ ", bidResponse=" + bidResponse);
        mHsBanner = new HSAdxBanner(mPlacementId);
        mHsBanner.setAdSize(getBannerAdSize(maxAdFormat));
        mHsBanner.setAdLoadListener(new IAdListener.AdLoadListener() {
            @Override
            public void onAdLoaded(HellaAd hellaAd) {
                log("adapter onAdLoaded");
                maxAdViewAdapterListener.onAdViewAdLoaded(mHsBanner.getAdView(), hellaAd.getExtraBundle());
            }

            @Override
            public void onAdLoadError(AdError adError) {
                log("adapter onAdLoadError");
                MaxAdapterError error = getMaxAdapterError(adError);
                maxAdViewAdapterListener.onAdViewAdLoadFailed(error);
            }
        });
        mHsBanner.setAdActionListener(new IAdListener.AdActionListener() {
            @Override
            public void onAdImpressionError(AdError error) {
                log("adapter onAdImpressionError");
                maxAdViewAdapterListener.onAdViewAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED);
            }

            @Override
            public void onAdImpression(HellaAd hellaAd) {
                log("adapter onAdImpression");
                maxAdViewAdapterListener.onAdViewAdDisplayed(hellaAd.getExtraBundle());
            }

            @Override
            public void onAdClicked(HellaAd hellaAd) {
                log("adapter onAdClicked");
                maxAdViewAdapterListener.onAdViewAdClicked(hellaAd.getExtraBundle());
            }

            @Override
            public void onAdRevenue(HellaAd hellaAd) {
                log("adapter onAdRevenue=" + hellaAd.getRevenue());
            }

            @Override
            public void onAdRewarded(HellaAd hellaAd) {

            }

            @Override
            public void onAdClosed(boolean hasRewarded, HellaAd hellaAd) {
                log("adapter onAdClosed");
                maxAdViewAdapterListener.onAdViewAdHidden(hellaAd.getExtraBundle());
            }
        });
        mHsBanner.load(bidResponse);
    }

    @Override
    public String getSdkVersion() {
        return AppUtils.getSdkVerName();
    }

    @Override
    public String getAdapterVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy() {
        if (mHsBanner != null) {
            mHsBanner.destroy();
            mHsBanner = null;
        }

        if (mInterstitial != null) {
            mInterstitial.destroy();
            mInterstitial = null;
        }

        if (mRewardedVideoAd != null) {
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }
    }

    private AdSize getBannerAdSize(MaxAdFormat maxAdFormat) {
        if (maxAdFormat == MaxAdFormat.MREC) {
            return AdSize.MEDIUM_RECTANGLE;
        }
        return AdSize.BANNER;
    }

    private MaxAdapterError getMaxAdapterError(AdError adError) {
        MaxAdapterError error;
        switch (adError.getErrorCode()) {
            case AdError.ErrorCode.NO_FILL:
                error = MaxAdapterError.NO_FILL;
                break;
            case AdError.ErrorCode.INTERNAL_ERROR:
                error = MaxAdapterError.INTERNAL_ERROR;
                break;
            case AdError.ErrorCode.DIS_CONDITION:
                error = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
            case AdError.ErrorCode.INITIALIZE_ERROR:
                error = MaxAdapterError.NOT_INITIALIZED;
                break;
            case AdError.ErrorCode.SERVER_ERROR:
                error = MaxAdapterError.SERVER_ERROR;
                break;
            case AdError.ErrorCode.TIMEOUT:
                error = MaxAdapterError.TIMEOUT;
                break;
            case AdError.ErrorCode.NETWORK_ERROR:
                error = MaxAdapterError.BAD_REQUEST;
                break;
            default:
                error = MaxAdapterError.INVALID_LOAD_STATE;
        }
        return error;
    }

    private class InterstitialAdLoadListener implements IAdListener.AdLoadListener {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdLoadListener(final MaxInterstitialAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(HellaAd hellaAd) {
            log("Interstitial received");
            listener.onInterstitialAdLoaded(hellaAd.getExtraBundle());
        }

        @Override
        public void onAdLoadError(AdError adError) {
            MaxAdapterError adapterError = getMaxAdapterError(adError);
            log("Interstitial failed to load with error: " + adapterError);
            listener.onInterstitialAdLoadFailed(adapterError);
        }
    }

    private class InterstitialAdActionListener implements IAdListener.AdActionListener {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdActionListener(final MaxInterstitialAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAdImpressionError(AdError error) {
            final MaxAdapterError adapterError = getMaxAdapterError(error);
            log("Interstitial failed to show with error: " + adapterError);
            listener.onInterstitialAdDisplayFailed(adapterError);
        }

        @Override
        public void onAdImpression(HellaAd hellaAd) {
            log("Interstitial impression");
            listener.onInterstitialAdDisplayed(hellaAd.getExtraBundle());
        }

        @Override
        public void onAdClicked(HellaAd hellaAd) {
            log("Interstitial clicked");
            listener.onInterstitialAdClicked(hellaAd.getExtraBundle());
        }

        @Override
        public void onAdRevenue(HellaAd hellaAd) {

        }

        @Override
        public void onAdRewarded(HellaAd hellaAd) {

        }

        @Override
        public void onAdClosed(boolean hasRewarded, HellaAd hellaAd) {
            log("Interstitial closed");
            listener.onInterstitialAdHidden(hellaAd.getExtraBundle());
        }
    }

    @Override
    public void loadInterstitialAd(MaxAdapterResponseParameters maxAdapterResponseParameters, Activity activity, MaxInterstitialAdapterListener maxInterstitialAdapterListener) {
        String mPlacementId = maxAdapterResponseParameters.getThirdPartyAdPlacementId();
        String bidResponse = maxAdapterResponseParameters.getBidResponse();
        log("HSInterstitial Ads load mPlacementId = " + mPlacementId + ", bidResponse=" + bidResponse);
        mInterstitial = new HSAdxInterstitial(mPlacementId);
        if (maxInterstitialAdapterListener != null) {
            mInterstitial.setAdLoadListener(new InterstitialAdLoadListener(maxInterstitialAdapterListener));
            mInterstitial.setAdActionListener(new InterstitialAdActionListener(maxInterstitialAdapterListener));
        }
        mInterstitial.load(bidResponse);
    }

    @Override
    public void showInterstitialAd(MaxAdapterResponseParameters maxAdapterResponseParameters, Activity activity, MaxInterstitialAdapterListener maxInterstitialAdapterListener) {
        if (mInterstitial != null && mInterstitial.isAdReady()) {
            mInterstitial.show();
        } else {
            maxInterstitialAdapterListener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
        }
    }

    @Override
    public void loadRewardedAd(MaxAdapterResponseParameters maxAdapterResponseParameters, Activity activity, MaxRewardedAdapterListener maxRewardedAdapterListener) {
        String mPlacementId = maxAdapterResponseParameters.getThirdPartyAdPlacementId();
        String bidResponse = maxAdapterResponseParameters.getBidResponse();
        log("HSRewardedVideo Ads load mPlacementId = " + mPlacementId + ", bidResponse=" + bidResponse);

        mRewardedVideoAd = new HSAdxReward(mPlacementId);
        if (maxRewardedAdapterListener != null) {
            mRewardedVideoAd.setAdLoadListener(new RewardAdLoadListener(maxRewardedAdapterListener));
            mRewardedVideoAd.setAdActionListener(new RewardAdActionListener(maxRewardedAdapterListener));
        }

        mRewardedVideoAd.load(bidResponse);
    }

    @Override
    public void showRewardedAd(MaxAdapterResponseParameters maxAdapterResponseParameters, Activity activity, MaxRewardedAdapterListener maxRewardedAdapterListener) {
        if (mRewardedVideoAd != null && mRewardedVideoAd.isAdReady()) {
            mRewardedVideoAd.show();
        } else {
            maxRewardedAdapterListener.onRewardedAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
        }
    }

    private class RewardAdLoadListener implements IAdListener.AdLoadListener {
        private final MaxRewardedAdapterListener maxRewardedAdapterListener;

        public RewardAdLoadListener(final MaxRewardedAdapterListener listener) {
            this.maxRewardedAdapterListener = listener;
        }

        @Override
        public void onAdLoaded(HellaAd hellaAd) {
            maxRewardedAdapterListener.onRewardedAdLoaded(hellaAd.getExtraBundle());
        }

        @Override
        public void onAdLoadError(AdError adError) {
            MaxAdapterError error = getMaxAdapterError(adError);
            maxRewardedAdapterListener.onRewardedAdLoadFailed(error);
        }
    }

    private class RewardAdActionListener implements IAdListener.AdActionListener {
        private final MaxRewardedAdapterListener listener;

        public RewardAdActionListener(final MaxRewardedAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAdImpressionError(AdError error) {
            final MaxAdapterError adapterError = getMaxAdapterError(error);
            log("Rewarded ad failed to show with error: " + adapterError);
            listener.onRewardedAdDisplayFailed(adapterError);
        }

        @Override
        public void onAdImpression(HellaAd hellaAd) {
            log("Rewarded ad impression");
            listener.onRewardedAdDisplayed(hellaAd.getExtraBundle());
        }

        @Override
        public void onAdClicked(HellaAd hellaAd) {
            log("Rewarded ad clicked");
            listener.onRewardedAdClicked(hellaAd.getExtraBundle());
        }

        @Override
        public void onAdRevenue(HellaAd hellaAd) {

        }

        @Override
        public void onAdRewarded(HellaAd hellaAd) {
            log("Rewarded ad reward granted");
        }

        @Override
        public void onAdClosed(boolean hasRewarded, HellaAd hellaAd) {
            if (hasRewarded) {
                final MaxReward reward = getReward();
                log("Rewarded user with reward: " + reward);
                listener.onUserRewarded(MaxRewardImpl.createDefault(), hellaAd.getExtraBundle());
            }

            log("Rewarded ad closed");
            listener.onRewardedAdHidden(hellaAd.getExtraBundle());
        }
    }
}
