package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.applovin.mediation.adapters.bidmachine.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.bidmachine.BidMachine;
import io.bidmachine.ImageData;
import io.bidmachine.InitializationCallback;
import io.bidmachine.MediaAssetType;
import io.bidmachine.banner.BannerListener;
import io.bidmachine.banner.BannerRequest;
import io.bidmachine.banner.BannerSize;
import io.bidmachine.banner.BannerView;
import io.bidmachine.interstitial.InterstitialAd;
import io.bidmachine.interstitial.InterstitialListener;
import io.bidmachine.interstitial.InterstitialRequest;
import io.bidmachine.nativead.NativeAd;
import io.bidmachine.nativead.NativeListener;
import io.bidmachine.nativead.NativeRequest;
import io.bidmachine.nativead.view.NativeMediaView;
import io.bidmachine.rewarded.RewardedAd;
import io.bidmachine.rewarded.RewardedListener;
import io.bidmachine.rewarded.RewardedRequest;
import io.bidmachine.utils.BMError;

public class BidMachineMediationAdapter extends MediationAdapterBase
        implements MaxSignalProvider, MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter {

    private static final AtomicBoolean initialized = new AtomicBoolean();
    private static InitializationStatus status;

    private BannerView bannerView;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private NativeAd nativeAd;

    public BidMachineMediationAdapter(AppLovinSdk appLovinSdk) {
        super(appLovinSdk);
    }

    @Override
    public void initialize(MaxAdapterInitializationParameters parameters,
                           Activity activity,
                           OnCompletionListener onCompletionListener) {
        updateSettings(parameters);

        if (initialized.compareAndSet(false, true)) {
            final String sourceId = parameters.getServerParameters().getString("source_id");
            status = InitializationStatus.INITIALIZING;
            log("Initializing BidMachine SDK with source id: " + sourceId + "...");

            InitializationCallback callback = new InitializationCallback() {
                @Override
                public void onInitialized() {
                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion(status, null);
                }
            };

            BidMachine.initialize(getApplicationContext(activity), sourceId, callback);
        } else {
            onCompletionListener.onCompletion(status, null);
        }
    }

    @Override
    public String getSdkVersion() {
        return BidMachine.VERSION;
    }

    @Override
    public String getAdapterVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy() {
        if (bannerView != null) {
            bannerView.setListener(null);
            bannerView.destroy();
            bannerView = null;
        }
        if (interstitialAd != null) {
            interstitialAd.setListener(null);
            interstitialAd.destroy();
            interstitialAd = null;
        }
        if (rewardedAd != null) {
            rewardedAd.setListener(null);
            rewardedAd.destroy();
            rewardedAd = null;
        }
        if (nativeAd != null) {
            nativeAd.setListener(null);
            nativeAd.unregisterView();
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    @Override
    public void collectSignal(MaxAdapterSignalCollectionParameters parameters,
                              Activity activity,
                              MaxSignalCollectionListener callback) {
        log("Collecting signal...");

        updateSettings(parameters);

        // Must be ran on bg thread
        String bidToken = BidMachine.getBidToken(getApplicationContext(activity));
        callback.onSignalCollected(bidToken);
    }

    @Override
    public void loadAdViewAd(MaxAdapterResponseParameters parameters,
                             MaxAdFormat maxAdFormat,
                             Activity activity,
                             MaxAdViewAdapterListener listener) {
        BannerRequest bannerRequest = new BannerRequest.Builder()
                .setSize(toBannerSize(maxAdFormat))
                .setBidPayload(parameters.getBidResponse())
                .build();
        bannerView = new BannerView(activity);
        bannerView.setListener(new BannerViewListener(listener));
        bannerView.load(bannerRequest);
    }

    @Override
    public void loadInterstitialAd(MaxAdapterResponseParameters parameters,
                                   Activity activity,
                                   MaxInterstitialAdapterListener listener) {
        InterstitialRequest interstitialRequest = new InterstitialRequest.Builder()
                .setBidPayload(parameters.getBidResponse())
                .build();
        interstitialAd = new InterstitialAd(getApplicationContext(activity));
        interstitialAd.setListener(new InterstitialAdListener(listener));
        interstitialAd.load(interstitialRequest);
    }

    @Override
    public void showInterstitialAd(MaxAdapterResponseParameters parameters,
                                   Activity activity,
                                   MaxInterstitialAdapterListener listener) {
        if (interstitialAd == null) {
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
            return;
        }
        if (interstitialAd.isExpired()) {
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_EXPIRED);
            return;
        }
        if (interstitialAd.canShow()) {
            interstitialAd.show();
        } else {
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
        }
    }

    @Override
    public void loadRewardedAd(MaxAdapterResponseParameters parameters,
                               Activity activity,
                               MaxRewardedAdapterListener listener) {
        RewardedRequest rewardedRequest = new RewardedRequest.Builder()
                .setBidPayload(parameters.getBidResponse())
                .build();
        rewardedAd = new RewardedAd(getApplicationContext(activity));
        rewardedAd.setListener(new RewardedAdListener(listener));
        rewardedAd.load(rewardedRequest);
    }

    @Override
    public void showRewardedAd(MaxAdapterResponseParameters parameters,
                               Activity activity,
                               MaxRewardedAdapterListener listener) {
        if (rewardedAd == null) {
            listener.onRewardedAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
            return;
        }
        if (rewardedAd.isExpired()) {
            listener.onRewardedAdDisplayFailed(MaxAdapterError.AD_EXPIRED);
            return;
        }
        if (rewardedAd.canShow()) {
            rewardedAd.show();
        } else {
            listener.onRewardedAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
        }
    }

    @Override
    public void loadNativeAd(MaxAdapterResponseParameters parameters,
                             Activity activity,
                             MaxNativeAdAdapterListener listener) {
        Context applicationContext = getApplicationContext(activity);
        NativeRequest nativeRequest = new NativeRequest.Builder()
                .setMediaAssetTypes(MediaAssetType.All)
                .setBidPayload(parameters.getBidResponse())
                .build();
        nativeAd = new NativeAd(applicationContext);
        nativeAd.setListener(new NativeAdListener(applicationContext, parameters.getServerParameters(), listener));
        nativeAd.load(nativeRequest);
    }

    private Context getApplicationContext(@Nullable Activity activity) {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return (activity != null) ? activity.getApplicationContext() : getApplicationContext();
    }

    private void updateSettings(MaxAdapterParameters parameters) {
        BidMachine.setLoggingEnabled(parameters.isTesting());
        BidMachine.setTestMode(parameters.isTesting());

        Boolean isAgeRestrictedUser = getPrivacySetting("isAgeRestrictedUser", parameters);
        if (isAgeRestrictedUser != null) {
            BidMachine.setCoppa(isAgeRestrictedUser);
        }

        AppLovinSdkConfiguration.ConsentDialogState state = getWrappingSdk().getConfiguration().getConsentDialogState();
        if (state == AppLovinSdkConfiguration.ConsentDialogState.APPLIES) {
            BidMachine.setSubjectToGDPR(true);
        } else if (state == AppLovinSdkConfiguration.ConsentDialogState.DOES_NOT_APPLY) {
            BidMachine.setSubjectToGDPR(false);
        }

        Boolean hasUserConsent = getPrivacySetting("hasUserConsent", parameters);
        if (hasUserConsent != null) {
            BidMachine.setConsentConfig(hasUserConsent, null);
        }
    }

    private Boolean getPrivacySetting(final String privacySetting, final MaxAdapterParameters parameters) {
        try {
            // Use reflection because compiled adapters have trouble fetching `boolean` from old SDKs and `Boolean` from new SDKs (above 9.14.0)
            Class<?> parametersClass = parameters.getClass();
            Method privacyMethod = parametersClass.getMethod(privacySetting);
            return (Boolean) privacyMethod.invoke(parameters);
        } catch (Exception exception) {
            log("Error getting privacy setting " + privacySetting + " with exception: ", exception);
            return (AppLovinSdk.VERSION_CODE >= 9140000) ? null : false;
        }
    }

    private BannerSize toBannerSize(MaxAdFormat maxAdFormat) {
        if (maxAdFormat == MaxAdFormat.BANNER) {
            return BannerSize.Size_320x50;
        } else if (maxAdFormat == MaxAdFormat.LEADER) {
            return BannerSize.Size_728x90;
        } else if (maxAdFormat == MaxAdFormat.MREC) {
            return BannerSize.Size_300x250;
        } else {
            throw new IllegalArgumentException("Invalid ad format: " + maxAdFormat);
        }
    }

    private MaxAdapterError toMaxAdapterError(BMError bmError) {
        int bidMachineErrorCode = bmError.getCode();
        MaxAdapterError maxAdapterError = MaxAdapterError.UNSPECIFIED;
        switch (bidMachineErrorCode) {
            case BMError.NO_CONNECTION:
                maxAdapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case BMError.TIMEOUT:
                maxAdapterError = MaxAdapterError.TIMEOUT;
                break;
            case BMError.HTTP_BAD_REQUEST:
                maxAdapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case BMError.SERVER:
                maxAdapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case BMError.NO_CONTENT:
            case BMError.BAD_CONTENT:
                maxAdapterError = MaxAdapterError.NO_FILL;
                break;
            case BMError.EXPIRED:
                maxAdapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case BMError.INTERNAL:
                maxAdapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }
        return new MaxAdapterError(maxAdapterError.getErrorCode(),
                                   maxAdapterError.getErrorMessage(),
                                   bidMachineErrorCode,
                                   bmError.getMessage());
    }

    private class BannerViewListener implements BannerListener {

        private final MaxAdViewAdapterListener listener;

        public BannerViewListener(MaxAdViewAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull BannerView bannerView) {
            log("Banner ad loaded");
            listener.onAdViewAdLoaded(bannerView);
        }

        @Override
        public void onAdLoadFailed(@NonNull BannerView bannerView, @NonNull BMError bmError) {
            MaxAdapterError maxAdapterError = toMaxAdapterError(bmError);
            log("Banner ad failed to load with error (" + maxAdapterError + ")");
            listener.onAdViewAdLoadFailed(maxAdapterError);
        }

        @Override
        public void onAdShown(@NonNull BannerView bannerView) {
            log("Banner ad shown");
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdImpression(@NonNull BannerView bannerView) {
            log("Banner ad impression");
        }

        @Override
        public void onAdClicked(@NonNull BannerView bannerView) {
            log("Banner ad clicked");
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdExpired(@NonNull BannerView bannerView) {
            log("Banner ad expired");
        }

    }

    private class InterstitialAdListener implements InterstitialListener {

        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdListener(MaxInterstitialAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
            log("Interstitial ad loaded");
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdLoadFailed(@NonNull InterstitialAd interstitialAd, @NonNull BMError bmError) {
            MaxAdapterError maxAdapterError = toMaxAdapterError(bmError);
            log("Interstitial ad failed to load with error (" + maxAdapterError + ")");
            listener.onInterstitialAdLoadFailed(maxAdapterError);
        }

        @Override
        public void onAdShown(@NonNull InterstitialAd interstitialAd) {
            log("Interstitial ad shown");
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdShowFailed(@NonNull InterstitialAd interstitialAd, @NonNull BMError bmError) {
            MaxAdapterError maxAdapterError = toMaxAdapterError(bmError);
            log("Interstitial ad failed to show with error (" + maxAdapterError + ")");
            listener.onInterstitialAdDisplayFailed(maxAdapterError);
        }

        @Override
        public void onAdImpression(@NonNull InterstitialAd interstitialAd) {
            log("Interstitial ad impression");
        }

        @Override
        public void onAdClicked(@NonNull InterstitialAd interstitialAd) {
            log("Interstitial ad clicked");
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed(@NonNull InterstitialAd interstitialAd, boolean finished) {
            log("Interstitial ad closed");
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdExpired(@NonNull InterstitialAd interstitialAd) {
            log("Interstitial ad expired");
        }

    }

    private class RewardedAdListener implements RewardedListener {

        private final MaxRewardedAdapterListener listener;

        public RewardedAdListener(MaxRewardedAdapterListener listener) {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
            log("Rewarded ad loaded");
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdLoadFailed(@NonNull RewardedAd rewardedAd, @NonNull BMError bmError) {
            MaxAdapterError maxAdapterError = toMaxAdapterError(bmError);
            log("Rewarded ad failed to load with error (" + maxAdapterError + ")");
            listener.onRewardedAdLoadFailed(maxAdapterError);
        }

        @Override
        public void onAdShown(@NonNull RewardedAd rewardedAd) {
            log("Rewarded ad shown");
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdShowFailed(@NonNull RewardedAd rewardedAd, @NonNull BMError bmError) {
            MaxAdapterError maxAdapterError = toMaxAdapterError(bmError);
            log("Rewarded ad failed to show with error (" + maxAdapterError + ")");
            listener.onRewardedAdDisplayFailed(maxAdapterError);
        }

        @Override
        public void onAdImpression(@NonNull RewardedAd rewardedAd) {
            log("Rewarded ad impression");
        }

        @Override
        public void onAdClicked(@NonNull RewardedAd rewardedAd) {
            log("Rewarded ad clicked");
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdClosed(@NonNull RewardedAd rewardedAd, boolean finished) {
            log("Rewarded ad closed");
            listener.onRewardedAdHidden();
        }

        @Override
        public void onAdRewarded(@NonNull RewardedAd rewardedAd) {
            final MaxReward reward = getReward();
            log("Rewarded user with reward: " + reward);
            listener.onUserRewarded(reward);
        }

        @Override
        public void onAdExpired(@NonNull RewardedAd rewardedAd) {
            log("Rewarded ad expired");
        }

    }

    private class NativeAdListener implements NativeListener {

        private final Context applicationContext;
        private final Bundle serverParameters;
        private final MaxNativeAdAdapterListener listener;

        public NativeAdListener(Context applicationContext,
                                Bundle serverParameters,
                                MaxNativeAdAdapterListener listener) {
            this.applicationContext = applicationContext;
            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull NativeAd nativeAd) {
            log("Native ad loaded");

            String templateName = BundleUtils.getString("template", "", serverParameters);
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString(templateName);
            if (!hasRequiredAssets(isTemplateAd, nativeAd)) {
                e("Native ad does not have required assets - " + nativeAd);
                listener.onNativeAdLoadFailed(MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS);

                return;
            }

            handleNativeAdLoaded(applicationContext, nativeAd);
        }

        @Override
        public void onAdLoadFailed(@NonNull NativeAd nativeAd, @NonNull BMError bmError) {
            MaxAdapterError maxAdapterError = toMaxAdapterError(bmError);
            log("Native ad failed to load with error (" + maxAdapterError + ")");
            listener.onNativeAdLoadFailed(maxAdapterError);
        }

        @Override
        public void onAdShown(@NonNull NativeAd nativeAd) {
            log("Native ad shown");
            listener.onNativeAdDisplayed(null);
        }

        @Override
        public void onAdImpression(@NonNull NativeAd nativeAd) {
            log("Native ad impression");
        }

        @Override
        public void onAdClicked(@NonNull NativeAd nativeAd) {
            log("Native ad clicked");
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdExpired(@NonNull NativeAd nativeAd) {
            log("Native ad expired");
        }

        private boolean hasRequiredAssets(final boolean isTemplateAd, final NativeAd nativeAd) {
            if (isTemplateAd) {
                return AppLovinSdkUtils.isValidString(nativeAd.getTitle());
            } else {
                // NOTE: media view is created and will always be non-null
                return AppLovinSdkUtils.isValidString(nativeAd.getTitle())
                        && AppLovinSdkUtils.isValidString(nativeAd.getCallToAction());
            }
        }

        private void handleNativeAdLoaded(@NonNull Context context, @NonNull NativeAd nativeAd) {
            ImageData iconImageData = nativeAd.getIcon();
            if (iconImageData != null) {
                Drawable image = iconImageData.getImage();
                Uri localUri = iconImageData.getLocalUri();
                String remoteUrl = iconImageData.getRemoteUrl();
                if (image != null) {
                    MaxNativeAd.MaxNativeAdImage maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage(image);
                    handleNativeAdLoaded(context, nativeAd, maxNativeAdImage);
                    return;
                } else if (localUri != null) {
                    MaxNativeAd.MaxNativeAdImage maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage(localUri);
                    handleNativeAdLoaded(context, nativeAd, maxNativeAdImage);
                    return;
                } else if (remoteUrl != null) {
                    getCachingExecutorService().execute(new Runnable() {
                        @Override
                        public void run() {
                            Drawable image = null;

                            if (AppLovinSdkUtils.isValidString(remoteUrl)) {
                                log("Adding native ad icon (" + remoteUrl + ") to queue to be fetched");

                                final Future<Drawable> imageFuture = createDrawableFuture(remoteUrl,
                                                                                          context.getResources());
                                final int imageTaskTimeoutSeconds = BundleUtils.getInt("image_task_timeout_seconds",
                                                                                       10,
                                                                                       serverParameters);
                                try {
                                    if (imageFuture != null) {
                                        image = imageFuture.get(imageTaskTimeoutSeconds, TimeUnit.SECONDS);
                                    }
                                } catch (Throwable th) {
                                    e("Image fetching tasks failed", th);
                                }
                            }

                            MaxNativeAd.MaxNativeAdImage maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage(image);
                            handleNativeAdLoaded(context, nativeAd, maxNativeAdImage);
                        }
                    });
                    return;
                }
            }
            handleNativeAdLoaded(context, nativeAd, null);
        }

        private void handleNativeAdLoaded(@NonNull Context context,
                                          @NonNull NativeAd nativeAd,
                                          @Nullable MaxNativeAd.MaxNativeAdImage iconMaxNativeAdImage) {
            AppLovinSdkUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final NativeMediaView mediaView = new NativeMediaView(context);
                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat(MaxAdFormat.NATIVE)
                            .setTitle(nativeAd.getTitle())
                            .setBody(nativeAd.getDescription())
                            .setCallToAction(nativeAd.getCallToAction())
                            .setIcon(iconMaxNativeAdImage)
                            .setMediaView(mediaView)
                            .setOptionsView(nativeAd.getProviderView(context));
                    final MaxBidMachineNativeAd maxBidMachineNativeAd = new MaxBidMachineNativeAd(builder);
                    listener.onNativeAdLoaded(maxBidMachineNativeAd, null);
                }
            });
        }

    }

    private class MaxBidMachineNativeAd extends MaxNativeAd {

        public MaxBidMachineNativeAd(Builder builder) {
            super(builder);
        }

        @Override
        public void prepareViewForInteraction(MaxNativeAdView maxNativeAdView) {
            if (nativeAd == null) {
                e("Failed to register native ad views: native ad is null.");
                return;
            }

            final Set<View> clickableViews = new HashSet<>();
            if (AppLovinSdkUtils.isValidString(getTitle()) && maxNativeAdView.getTitleTextView() != null) {
                clickableViews.add(maxNativeAdView.getTitleTextView());
            }
            if (AppLovinSdkUtils.isValidString(getBody()) && maxNativeAdView.getBodyTextView() != null) {
                clickableViews.add(maxNativeAdView.getBodyTextView());
            }
            if (AppLovinSdkUtils.isValidString(getCallToAction()) && maxNativeAdView.getCallToActionButton() != null) {
                clickableViews.add(maxNativeAdView.getCallToActionButton());
            }
            ImageView iconImageView = maxNativeAdView.getIconImageView();
            if (getIcon() != null && iconImageView != null) {
                clickableViews.add(iconImageView);
            }
            if (getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null) {
                clickableViews.add(maxNativeAdView.getMediaContentViewGroup());
            }

            nativeAd.registerView(maxNativeAdView, iconImageView, (NativeMediaView) getMediaView(), clickableViews);
        }

    }

}