package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapters.MaxAdapterError;
import com.applovin.mediation.adapters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.MaxAdViewAdapter;
import com.applovin.mediation.adapters.MaxInterstitialAdapter;
import com.applovin.mediation.adapters.MaxNativeAdAdapter;
import com.applovin.mediation.adapters.MaxRewardedAdapter;
import com.applovin.mediation.adapters.MaxSignalProvider;
import com.applovin.mediation.adapters.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapters.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.sdk.AppLovinSdk;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bidscube MAX Adapter - Simplified Version for Testing
 */
public class BidscubeMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxNativeAdAdapter, MaxSignalProvider
{
    private static final AtomicBoolean initialized = new AtomicBoolean();
    private static InitializationStatus status;

    public BidscubeMediationAdapter(final AppLovinSdk sdk) {
        super(sdk);
    }
    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity,
            final MaxSignalCollectionListener callback) {
        callback.onSignalCollected("bidscube_test_signal");
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity,
            final OnCompletionListener onCompletionListener) {
        if (initialized.compareAndSet(false, true)) {
            final String appId = parameters.getServerParameters().getString("app_id");
            log("Initializing Bidscube SDK with app id: " + appId + "...");

            if (appId == null) {
                log("Bidscube SDK initialization failed with null app id");
                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, "App id is null");
                return;
            }

            try {
                log("Bidscube SDK successfully initialized with app id: " + appId);
                status = InitializationStatus.INITIALIZED_SUCCESS;
                onCompletionListener.onCompletion(status, null);
            } catch (Exception e) {
                log("Bidscube SDK initialization failed with error: " + e.getMessage());
                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion(status, e.getMessage());
            }
        } else {
            onCompletionListener.onCompletion(status, null);
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat,
            @Nullable final Activity activity, final MaxAdViewAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading " + adFormat.getLabel() + " ad for placement: " + placementId + "...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS) {
            log("Bidscube SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load...");
            listener.onAdViewAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }
        View testAdView = createTestAdView(adFormat);

        log("Bidscube " + adFormat.getLabel() + " ad loaded successfully");
        listener.onAdViewAdLoaded(testAdView);
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxInterstitialAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading interstitial ad for placement: " + placementId + "...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS) {
            log("Bidscube SDK not successfully initialized: failing interstitial ad load...");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        log("Bidscube interstitial ad loaded successfully");
        listener.onInterstitialAdLoaded();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxInterstitialAdapterListener listener) {
        log("Showing Bidscube interstitial ad...");

        log("Bidscube interstitial ad displayed");
        listener.onInterstitialAdDisplayed();

        listener.onInterstitialAdClicked();

        listener.onInterstitialAdHidden();
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log("Loading rewarded ad for placement: " + placementId + "...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS) {
            log("Bidscube SDK not successfully initialized: failing rewarded ad load...");
            listener.onRewardedAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        log("Bidscube rewarded ad loaded successfully");
        listener.onRewardedAdLoaded();
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxRewardedAdapterListener listener) {
        log("Showing Bidscube rewarded ad...");

        configureReward(parameters);

        log("Bidscube rewarded ad displayed");
        listener.onRewardedAdDisplayed();

        listener.onRewardedAdClicked();

        final MaxReward reward = getReward();
        log("Rewarded user with reward: " + reward);
        listener.onUserRewarded(reward);

        listener.onRewardedAdHidden();
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity,
            final MaxNativeAdAdapterListener listener) {
        log("Loading Bidscube native ad...");

        if (status != InitializationStatus.INITIALIZED_SUCCESS) {
            log("Bidscube SDK not successfully initialized: failing native ad load...");
            listener.onNativeAdLoadFailed(MaxAdapterError.NOT_INITIALIZED);
            return;
        }

        MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                .setAdFormat(MaxAdFormat.NATIVE)
                .setTitle("Bidscube Native Ad")
                .setBody("This is a test native ad from Bidscube")
                .setCallToAction("Learn More");

        MaxNativeAd maxNativeAd = builder.build();
        log("Bidscube native ad loaded successfully");
        listener.onNativeAdLoaded(maxNativeAd, null);
    }

    private View createTestAdView(MaxAdFormat adFormat) {
        View view = new View(getApplicationContext());
        view.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                adFormat == MaxAdFormat.BANNER ? 320 : 728,
                adFormat == MaxAdFormat.BANNER ? 50 : 90
        ));
        return view;
    }

    private static class InitializationStatus {
        public static final InitializationStatus INITIALIZING = new InitializationStatus();
        public static final InitializationStatus INITIALIZED_SUCCESS = new InitializationStatus();
        public static final InitializationStatus INITIALIZED_FAILURE = new InitializationStatus();
    }
}