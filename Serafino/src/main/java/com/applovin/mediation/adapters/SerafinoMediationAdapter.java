package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.MediationAdapterBase;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.odc.adn.ads.AdConfig;
import com.odc.adn.ads.ODCAd;
import com.odc.adn.ads.ODCAdError;
import com.odc.adn.ads.ODCPrivacyConfiguration;
import com.odc.adn.ads.banner.AdView;
import com.odc.adn.ads.banner.BannerAdListener;
import com.odc.adn.ads.interstitial.InterstitialAd;
import com.odc.adn.ads.interstitial.InterstitialAdListener;
import com.odc.adn.ads.nativead.Image;
import com.odc.adn.ads.nativead.MediaView;
import com.odc.adn.ads.nativead.NativeAd;
import com.odc.adn.ads.nativead.NativeAdListener;
import com.odc.adn.ads.nativead.NativeAdView;
import com.odc.adn.ads.rewarded.RewardItem;
import com.odc.adn.ads.rewarded.RewardedAd;
import com.odc.adn.ads.rewarded.RewardedAdListener;
import com.odc.adn.managers.ODCAdSdk;
import com.odc.adn.managers.OnStartListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;


public class SerafinoMediationAdapter extends MediationAdapterBase implements MaxRewardedAdapter,
    MaxInterstitialAdapter, MaxAdViewAdapter, MaxNativeAdAdapter {

    private static final int TITLE_LABEL_TAG          = 1;
    private static final int MEDIA_VIEW_CONTAINER_TAG = 2;
    private static final int ICON_VIEW_TAG            = 3;
    private static final int BODY_VIEW_TAG            = 4;
    private static final int CALL_TO_ACTION_VIEW_TAG  = 5;
    // not use
    private static final int RATING_VIEW_TAG          = 6;
    private static final int OPTION_VIEW_TAG          = 7;
    private static final int ADVERTISER_VIEW_TAG      = 8;
    private static final String TAG = "SerafinoMediationAdapter";
    private static final String ADAPTER_VERSION = ODCAdSdk.getSdkVersion();
    private static final AtomicBoolean mInitialized= new AtomicBoolean();
    private static InitializationStatus sInitializationStatus;
    private static final boolean DEFAULT_AGE_RESTRICTED = false;

    private RewardedAd mODCRewardedAd;
    private InterstitialAd mODCInterstitialAd;
    private AdView mODCBannerAd;
    private NativeAd mODCNativeAd;
    private NativeAdView mODCNativeAdView;

    private RewardedAdListener mODCRewardedAdListener;
    private InterstitialAdListener mODCInterstitialAdListener;
    private BannerAdListener mODCBannerAdListener;
    private NativeAdListener mODCNativeAdListener;


    private static final String TIMEOUT = "Timeout";
    private static final String PARAM_IS_MUTED = "is_muted";
    private static final String KEY_CREATIVE_ID = "creative_id";

    // Explicit default constructor declaration
    public SerafinoMediationAdapter(AppLovinSdk appLovinSdk) {
        super(appLovinSdk);
    }

    @Override
    public void initialize(MaxAdapterInitializationParameters parameters, Activity activity,
                           OnCompletionListener onCompletionListener) {
        if (mInitialized.compareAndSet(false, true)){
            sInitializationStatus = InitializationStatus.INITIALIZING;
            final Bundle serverParameters = parameters.getServerParameters();
            final String appId = serverParameters.getString( "app_id" );
            if (appId== null)  {
                sInitializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion( sInitializationStatus, "app_id is null" );
                return;
            }
            Log.d(TAG, "Initializing SDK with app id: " + appId + "..." );

            // Configure the user's privacy settings
            Boolean hasUserConsent = parameters.hasUserConsent();
            if (hasUserConsent != null)
            {
                ODCPrivacyConfiguration.setUserConsent(hasUserConsent);
            }
            Log.d(TAG, "hasUserConsent is: " + ODCPrivacyConfiguration.hasUserConsent());

            Boolean isDoNotSell = parameters.isDoNotSell();
            if (isDoNotSell != null)
            {
                ODCPrivacyConfiguration.setDoNotSell( isDoNotSell);
            }
            Log.d(TAG, "isDoNotSell is: " + ODCPrivacyConfiguration.isDoNotSell());

            ODCPrivacyConfiguration.setAgeRestrictedUser(DEFAULT_AGE_RESTRICTED);
            Log.d(TAG, "isAgeRestrictedUser is: " + ODCPrivacyConfiguration.isAgeRestrictedUser());

            // Initialize the SDK
            ODCAdSdk.getInstance().init(getContext(activity), appId);
            ODCAdSdk.getInstance().start(new OnStartListener() {
                @Override
                public void onStartComplete() {
                    Log.d(TAG,  "SDK initialized" );
                    sInitializationStatus = InitializationStatus.INITIALIZED_SUCCESS;
                    // Initialization completed, callback the current status and empty error message
                    onCompletionListener.onCompletion( sInitializationStatus, null );
                }

                @Override
                public void onStartFailed(ODCAdError error) {
                    Log.d(TAG,  "SDK failed to initialize with code: " + error.getErrorCode() + " and message: "
                        + error.getErrorMsg());
                    sInitializationStatus = InitializationStatus.INITIALIZED_FAILURE;
                    onCompletionListener.onCompletion( sInitializationStatus, error.toString());
                }
            });
        }
        else
        {
            // Initializing, callback the current status and empty error message
            onCompletionListener.onCompletion( sInitializationStatus, null );
        }
    }

    @Override
    public String getSdkVersion() {
        Log.d(TAG, "getSdkVersion start");
        return ODCAdSdk.getSdkVersion();
    }

    @Override
    public String getAdapterVersion() {
        Log.d(TAG, "getAdapterVersion");
        return ADAPTER_VERSION;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mODCRewardedAd = null;
        mODCRewardedAdListener = null;

        mODCInterstitialAdListener = null;
        mODCInterstitialAd = null;

        if (mODCBannerAd != null) {
            mODCBannerAd.destroy();
            mODCBannerAd = null;
            mODCBannerAdListener = null;
        }

        if (mODCNativeAd != null) {
            mODCNativeAd.destroy();
            mODCNativeAd = null;
            mODCNativeAdView = null;
            mODCNativeAdListener = null;
        }
    }

    //region MaxRewardedAdapter Methods
    @Override
    public void loadRewardedAd(MaxAdapterResponseParameters parameters, Activity activity,
                               MaxRewardedAdapterListener listener) {
        Log.d(TAG, "loadRewardedAd start");
        // 1. Get adUnitId ID from the AppLovin parameters
        String adUnitId = parameters.getThirdPartyAdPlacementId();
        Bundle serverParameters = parameters.getServerParameters();
        AdConfig.SoundState muteState = getMuteStateFromParams(serverParameters);
        Log.d(TAG, "Loading rewarded ad for unit id:" + adUnitId + ",muteState:" + muteState);
        // 2. Validate adUnitId validity
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.e(TAG, "loadRewardedAd: ODC adUnitId ID is empty");
            listener.onRewardedAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }

        // 3. Create an instance of RewardedAd in ODC
        Context context = getContext(activity);
        if (context == null) {
            Log.e(TAG, "Context is null, cannot proceed");
            listener.onRewardedAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }
        mODCRewardedAd = new RewardedAd(context, adUnitId);

        // 4. Construct AdConfig
        AdConfig adConfig = new AdConfig.Builder().mute(muteState).build();

        // 5. Load ads and bind a custom listener
        mODCRewardedAdListener = new ODCRewardedAdListener(adUnitId, listener);
        mODCRewardedAd.loadAd(adConfig, mODCRewardedAdListener);
    }

    @Override
    public void showRewardedAd(MaxAdapterResponseParameters parameters, Activity activity, MaxRewardedAdapterListener listener) {
        Log.d(TAG, "showRewardedAd start");
        if (mODCRewardedAd != null && mODCRewardedAd.isValid()) {
            Map<String, Object> localExt = parameters.getLocalExtraParameters();
            Map<String, Object> customReward = new java.util.HashMap<>();

            Object rewardCount = localExt.get("reward_count");
            Object rewardItem = localExt.get("reward_item");

            if (rewardCount != null) {
                customReward.put("reward_count", rewardCount);
            }
            if (rewardItem != null) {
                customReward.put("reward_item", rewardItem);
            }

            mODCRewardedAd.showAd(activity, customReward);
        } else {
            Log.e(TAG, "showRewardedAd: Ad not loaded or has been invalidated");
            listener.onRewardedAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
        }
    }
    //endregion

    //region MaxInterstitialAdapter Methods
    @Override
    public void loadInterstitialAd(MaxAdapterResponseParameters parameters, Activity activity,
                                   MaxInterstitialAdapterListener listener) {
        Log.d(TAG, "loadInterstitialAd start");
        // 1. Get adUnitId ID from the AppLovin parameters
        String adUnitId = parameters.getThirdPartyAdPlacementId();
        Bundle serverParameters = parameters.getServerParameters();
        AdConfig.SoundState muteState = getMuteStateFromParams(serverParameters);
        Log.d(TAG, "Loading InterstitialAd ad for unit id:" + adUnitId + ",muteState:" + muteState);
        // 2. Validate adUnitId validity
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.e(TAG, "loadInterstitialAd: ODC adUnitId ID is empty");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }

        // 3. Create an instance of InterstitialAd in ODC
        Context context = getContext(activity);
        if (context == null) {
            Log.e(TAG, "Context is null, cannot proceed");
            listener.onInterstitialAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }
        mODCInterstitialAd = new InterstitialAd(context, adUnitId);

        // 4. Construct AdConfig
        AdConfig adConfig = new AdConfig.Builder().mute(muteState).build();

        // 5. Load ads and bind a custom listener
        mODCInterstitialAdListener = new ODCInterstitialAdListener(adUnitId, listener);
        mODCInterstitialAd.loadAd(adConfig, mODCInterstitialAdListener);
    }

    @Override
    public void showInterstitialAd(MaxAdapterResponseParameters parameters, Activity activity,
                                   MaxInterstitialAdapterListener maxInterstitialAdapterListener) {
        Log.d(TAG, "showInterstitialAd start");
        if (mODCInterstitialAd != null && mODCInterstitialAd.isValid()) {
            mODCInterstitialAd.showAd(activity);
        } else {
            Log.e(TAG, "showInterstitialAd: Ad not loaded or has been invalidated");
            maxInterstitialAdapterListener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_NOT_READY);
        }
    }
    //endregion

    //region MaxAdViewAdapter Methods
    @Override
    public void loadAdViewAd(MaxAdapterResponseParameters parameters, MaxAdFormat adFormat,
                             Activity activity, MaxAdViewAdapterListener listener) {
        Log.d(TAG, "loadAdViewAd start");
        // 1. Get adUnitId ID from the AppLovin parameters
        String adUnitId = parameters.getThirdPartyAdPlacementId();
        Log.d(TAG, "Loading Banner ad for unit id:" + adUnitId);

        // 2. Validate adUnitId validity
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.e(TAG, "loadAdViewAd: ODC adUnitId ID is empty");
            listener.onAdViewAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }

        // 3. Create an instance of BannerAd in ODC
        Context context = getContext(activity);
        if (context == null) {
            Log.e(TAG, "Context is null, cannot proceed");
            listener.onAdViewAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }
        mODCBannerAd = new AdView(context, adUnitId);

        // 4. Construct AdConfig
        AdConfig adConfig = new AdConfig.Builder().build();

        // 5. Load ads and bind a custom listener
        mODCBannerAdListener = new ODCBannerAdListener(adUnitId, listener);
        mODCBannerAd.setListener(mODCBannerAdListener);
        mODCBannerAd.loadAd(adConfig);
    }
    //endregion

    //region MaxNativeAdapter Methods
    @Override
    public void loadNativeAd(MaxAdapterResponseParameters maxAdapterResponseParameters, Activity activity,
                             MaxNativeAdAdapterListener maxNativeAdAdapterListener) {
        Log.d(TAG, "loadNativeAd start");
        // 1. Get adUnitId ID from the AppLovin parameters
        String adUnitId = maxAdapterResponseParameters.getThirdPartyAdPlacementId();
        Log.d(TAG, "Loading Native ad for unit id:" + adUnitId);

        // 2. Validate adUnitId validity
        if (adUnitId == null || adUnitId.isEmpty()) {
            Log.e(TAG, "loadNativeAd: ODC adUnitId ID is empty");
            maxNativeAdAdapterListener.onNativeAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }

        // 3. Create an instance of NativeAd in ODC
        Context context = getContext(activity);
        if (context == null) {
            Log.e(TAG, "Context is null, cannot proceed");
            maxNativeAdAdapterListener.onNativeAdLoadFailed(MaxAdapterError.INVALID_CONFIGURATION);
            return;
        }

        mODCNativeAd = new NativeAd(context, adUnitId);
        // 4. Construct AdConfig
        AdConfig adConfig = new AdConfig.Builder().build();

        // 5. Load ads and bind a custom listener
        mODCNativeAdListener = new ODCNativeAdListener(maxAdapterResponseParameters.getServerParameters(), adUnitId, maxNativeAdAdapterListener);
        mODCNativeAd.loadAd(adConfig, mODCNativeAdListener);
    }
    //endregion

    //region Helper Methods
    private MaxAdapterError toMaxError(final int odcErrorCode, final String odcErrorMsg)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch (odcErrorCode)
        {
            case ODCAdError.ERROR_CODE_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case ODCAdError.ERROR_CODE_NETWORK_ERROR:
                if(odcErrorMsg.contains(TIMEOUT)){
                    adapterError = MaxAdapterError.TIMEOUT;
                }else {
                    adapterError = MaxAdapterError.NO_CONNECTION;
                }
                break;
            case ODCAdError.ERROR_CODE_AD_SERVER_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case ODCAdError.ERROR_CODE_INTERNAL_ERROR:
            case ODCAdError.ERROR_CODE_COMPLIANCE:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case ODCAdError.ERROR_CODE_DISPLAY_ERROR:
                adapterError = MaxAdapterError.AD_DISPLAY_FAILED;
                break;
        }
        return new MaxAdapterError(adapterError, odcErrorCode, odcErrorMsg);
    }

    private Context getContext(@Nullable final Activity activity)
    {
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private AdConfig.SoundState getMuteStateFromParams(Bundle serverParameters) {
        if (serverParameters.containsKey( PARAM_IS_MUTED))
        {
            return serverParameters.getBoolean( PARAM_IS_MUTED)?
                AdConfig.SoundState.MUTED:AdConfig.SoundState.UNMUTED;
        }
        return AdConfig.SoundState.DEFAULT;
    }
    //endregion

    private class ODCRewardedAdListener implements RewardedAdListener {
        private final String mAdUnitId;
        private final MaxRewardedAdapterListener mListener;
        private boolean mHasGrantedReward;

        ODCRewardedAdListener(final String adUnitId, final MaxRewardedAdapterListener
            listener) {
            this.mAdUnitId = adUnitId;
            this.mListener = listener;
        }

        @Override
        public void onAdLoaded(ODCAd rewardedAd) {
            if (rewardedAd == null) {
                Log.e(TAG, "onAdLoaded: ODC Rewarded load error（NO FILL）");
                mListener.onRewardedAdLoadFailed(MaxAdapterError.NO_FILL);
                return;
            }
            Log.d(TAG, "onAdLoaded: ODC Rewarded load success，adUnitId：" + mAdUnitId +
                ",rewardedAd:"+rewardedAd);
            Bundle bundle = new Bundle();
            bundle.putString(KEY_CREATIVE_ID, rewardedAd.getCreativeId());
            mListener.onRewardedAdLoaded(bundle);
        }

        @Override
        public void onAdFailed(ODCAdError odcAdError) {
            // Convert ODC error to AppLovin error code
            MaxAdapterError adapterError = toMaxError(odcAdError.getErrorCode(), odcAdError.getErrorMsg());
            Log.e(TAG, "onAdFailed: ODC Rewarded load error：" + adapterError);
            // Distribute callbacks based on error type (load failure/displays failure)
            if (odcAdError.getErrorCode() == ODCAdError.ERROR_CODE_DISPLAY_ERROR) {
                mListener.onRewardedAdDisplayFailed(adapterError);
            } else {
                mListener.onRewardedAdLoadFailed(adapterError);
            }
        }

        @Override
        public void onAdImpression(ODCAd rewardedAd) {
            Log.d(TAG, "onAdImpression: ODC Rewarded displayed，adUnitId：" + mAdUnitId);
            mListener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked(ODCAd rewardedAd) {
            Log.d(TAG, "onAdClicked: ODC Rewarded clicked，adUnitId：" + mAdUnitId);
            mListener.onRewardedAdClicked();
        }
        @Override
        public void onUserEarnedReward(ODCAd rewardedAd, RewardItem rewardItem)
        {
            Log.d( TAG,"Rewarded user with reward: " + rewardItem  );
            mHasGrantedReward = true;
        }

        @Override
        public void onAdDismissed(ODCAd rewardedAd)
        {
            Log.d(TAG, "onAdDismissed: ODC Rewarded ad hidden，adUnitId：" + mAdUnitId);
            if (mHasGrantedReward || shouldAlwaysRewardUser())
            {
                final MaxReward reward = getReward();
                Log.d(TAG,  "Rewarded user with reward: " + reward );
                mListener.onUserRewarded( reward );
            }

            mListener.onRewardedAdHidden();
        }
    }

    private class ODCInterstitialAdListener implements InterstitialAdListener {
        private final String mAdUnitId;
        private final MaxInterstitialAdapterListener mListener;

        ODCInterstitialAdListener(final String adUnitId, final MaxInterstitialAdapterListener
            listener) {
            this.mAdUnitId = adUnitId;
            this.mListener = listener;
        }

        @Override
        public void onAdLoaded(ODCAd interstitialAd) {

            if (interstitialAd == null) {
                Log.e(TAG, "onAdLoaded: ODC Interstitial load error（NO FILL）");
                mListener.onInterstitialAdLoadFailed(MaxAdapterError.NO_FILL);
                return;
            }
            Log.d(TAG, "onAdLoaded: ODC Interstitial load success，adUnitId：" + mAdUnitId +
                ",interstitialAd:"+interstitialAd);
            Bundle bundle = new Bundle();
            bundle.putString(KEY_CREATIVE_ID, interstitialAd.getCreativeId());
            mListener.onInterstitialAdLoaded(bundle);
        }

        @Override
        public void onAdFailed(ODCAdError odcAdError) {
            // Convert ODC error to AppLovin error code
            MaxAdapterError adapterError = toMaxError(odcAdError.getErrorCode(), odcAdError.getErrorMsg());
            Log.e(TAG, "onAdFailed: ODC Interstitial load error：" + adapterError);
            // Distribute callbacks based on error type (load failure/displays failure)
            if (odcAdError.getErrorCode() == ODCAdError.ERROR_CODE_DISPLAY_ERROR) {
                mListener.onInterstitialAdDisplayFailed(adapterError);
            } else {
                mListener.onInterstitialAdLoadFailed(adapterError);
            }
        }

        @Override
        public void onAdImpression(ODCAd interstitialAd) {
            Log.d(TAG, "onAdImpression: ODC Interstitial displayed，adUnitId：" + mAdUnitId);
            mListener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked(ODCAd interstitialAd) {
            Log.d(TAG, "onAdClicked: ODC Interstitial clicked，adUnitId：" + mAdUnitId);
            mListener.onInterstitialAdClicked();
        }

        @Override
        public void onAdDismissed(ODCAd interstitialAd)
        {
            Log.d(TAG, "onAdDismissed: ODC Interstitial ad hidden，adUnitId：" + mAdUnitId);
            mListener.onInterstitialAdHidden();
        }
    }

    private class ODCBannerAdListener implements BannerAdListener {
        private final String mAdUnitId;
        private final MaxAdViewAdapterListener mListener;

        ODCBannerAdListener(final String adUnitId, final MaxAdViewAdapterListener listener) {
            this.mAdUnitId = adUnitId;
            this.mListener = listener;
        }

        @Override
        public void onAdLoaded(ODCAd bannerAd) {
            Log.d(TAG, "onAdLoaded: ODC Banner ad loaded, adUnitId: " + mAdUnitId + ", bannerAd: " + bannerAd);
            Bundle bundle = new Bundle();
            if (bannerAd != null) {
                bundle.putString(KEY_CREATIVE_ID, bannerAd.getCreativeId());
            }
            mListener.onAdViewAdLoaded(mODCBannerAd, bundle);
        }

        @Override
        public void onAdFailed(ODCAdError odcAdError) {
            Log.e(TAG, "onAdFailed: ODCAdError：" + odcAdError);
            // Convert ODC error to AppLovin error code
            MaxAdapterError adapterError = toMaxError(odcAdError.getErrorCode(),
                odcAdError.getErrorMsg());
            Log.e(TAG, "onAdFailed: ODC Banner load error：" + adapterError);
            // Distribute callbacks based on error type (load failure/displays failure)
            if (odcAdError.getErrorCode() == ODCAdError.ERROR_CODE_DISPLAY_ERROR) {
                mListener.onAdViewAdDisplayFailed(adapterError);
            } else {
                mListener.onAdViewAdLoadFailed(adapterError);
            }
        }

        @Override
        public void onAdImpression(ODCAd bannerAd) {
            Log.d(TAG, "onAdImpression: ODC Banner ad shown, adUnitId: " + mAdUnitId);
            mListener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(ODCAd bannerAd) {
            Log.d(TAG, "onAdClicked: ODC Banner ad clicked, adUnitId: " + mAdUnitId);
            mListener.onAdViewAdClicked();
        }

        @Override
        public void onAdDismissed(ODCAd bannerAd) {
            Log.d(TAG, "onAdDismissed: ODC Banner ad dismissed, adUnitId: " + mAdUnitId);
            mListener.onAdViewAdHidden();
        }
    }

    private class ODCNativeAdListener implements NativeAdListener {
        private final Bundle serverParameters;
        private final String mAdUnitId;
        private final MaxNativeAdAdapterListener mListener;

        ODCNativeAdListener(final Bundle serverParameters, final String adUnitId, final MaxNativeAdAdapterListener listener) {
            this.serverParameters = serverParameters;
            this.mAdUnitId = adUnitId;
            this.mListener = listener;
        }

        @Override
        public void onAdLoaded(ODCAd nativeAd) {
            Log.d(TAG, "onAdLoaded: ODC Native ad loaded, adUnitId: " + mAdUnitId);
            mODCNativeAdView = new NativeAdView(getContext(null));
            Bundle bundle = new Bundle();
            if (nativeAd != null) {
                bundle.putString(KEY_CREATIVE_ID, nativeAd.getCreativeId());
            }
            getCachingExecutorService().execute(() -> {
                Drawable iconDrawable = null;
                Image icon = mODCNativeAd.getIcon();
                if (icon != null && !icon.getUrl().isEmpty()) {
                    Log.d(TAG, "Adding native ad icon (" + icon.getUrl() + ") to queue to be fetched");

                    final Future<Drawable> iconDrawableFuture = createDrawableFuture(icon.getUrl(), getContext(null).getResources());
                    final int imageTaskTimeoutSeconds = BundleUtils.getInt("image_task_timeout_seconds", 10, serverParameters);

                    try {
                        if (iconDrawableFuture != null) {
                            iconDrawable = iconDrawableFuture.get(imageTaskTimeoutSeconds, TimeUnit.SECONDS);
                        }
                    } catch (Throwable th) {
                        Log.e(TAG, "Image fetching tasks failed", th);
                    }
                }

                // Post back to main thread after async task completes
                final Drawable finalIconDrawable = iconDrawable;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleNativeAdLoaded(finalIconDrawable, mODCNativeAd, bundle);
                    }
                });
            });
        }

        private void handleNativeAdLoaded(final Drawable iconDrawable, final NativeAd ad, final Bundle bundle) {
            MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                .setAdFormat(MaxAdFormat.NATIVE)
                .setTitle(ad.getHeadline())
                .setBody(ad.getBody())
                .setCallToAction(ad.getCallToAction())
                .setIcon(new MaxNativeAd.MaxNativeAdImage(iconDrawable))
                .setOptionsView(ad.getAdChoicesView())
                .setMediaView(new MediaView(getContext(null)));
            MaxNativeAd nativeAd = new MaxODCNativeAd(builder);
            mListener.onNativeAdLoaded(nativeAd, bundle);
        }

        @Override
        public void onAdFailed(ODCAdError odcAdError) {
            Log.e(TAG, "onAdFailed: ODCAdError：" + odcAdError);
            // Convert ODC error to AppLovin error code
            MaxAdapterError adapterError = toMaxError(odcAdError.getErrorCode(),
                odcAdError.getErrorMsg());
            Log.e(TAG, "onAdFailed: ODC Native load error：" + adapterError);
            mListener.onNativeAdLoadFailed(adapterError);
        }

        @Override
        public void onAdImpression(ODCAd odcAd) {
            Log.d(TAG, "onAdImpression: ODC Native ad shown, adUnitId: " + mAdUnitId);
            mListener.onNativeAdDisplayed(null);
        }

        @Override
        public void onAdClicked(ODCAd odcAd) {
            Log.d(TAG, "onAdClicked: ODC Native ad clicked, adUnitId: " + mAdUnitId);
            mListener.onNativeAdClicked();
        }
    }

    private class MaxODCNativeAd extends MaxNativeAd {

        public MaxODCNativeAd(Builder builder) {
            super(builder);
        }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container) {
            final NativeAd ad = mODCNativeAd;
            Log.d(TAG, "prepareForInteraction: ");
            if (ad == null) {
                Log.e(TAG, "Failed to register native ad views: native ad is null.");
                return false;
            }

            if (container instanceof MaxNativeAdView) {
                MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;

                View mainView = maxNativeAdView.getMainView();
                maxNativeAdView.removeAllViews();
                mODCNativeAdView.addView(mainView);
                maxNativeAdView.addView(mODCNativeAdView);

                mODCNativeAdView.setHeadLineView(maxNativeAdView.getTitleTextView());
                mODCNativeAdView.setIconView(maxNativeAdView.getIconImageView());
                mODCNativeAdView.setCallToActionView(maxNativeAdView.getCallToActionButton());
                mODCNativeAdView.setBodyView(maxNativeAdView.getBodyTextView());
                mODCNativeAdView.setMediaView((MediaView) getMediaView());

                clickableViews.add(mODCNativeAdView.getIconView());
                clickableViews.add(mODCNativeAdView.getBodyView());
                clickableViews.add(mODCNativeAdView.getCallToActionView());
                clickableViews.add(mODCNativeAdView.getHeadLineView());
                ad.registerViewForInteraction(mODCNativeAdView, clickableViews);
            }
            // Plugins
            else {
                for (View view : clickableViews) {
                    Object viewTag = view.getTag();
                    if (viewTag == null) {
                        continue;
                    }
                    int tag = (int) viewTag;

                    if (tag == TITLE_LABEL_TAG) {
                        mODCNativeAdView.setHeadLineView(view);
                    }
                    else if (tag == MEDIA_VIEW_CONTAINER_TAG) {
                        mODCNativeAdView.setMediaView((MediaView) getMediaView());
                    }
                    else if (tag == ICON_VIEW_TAG) {
                        mODCNativeAdView.setIconView(view);
                    }
                    else if (tag == BODY_VIEW_TAG) {
                        mODCNativeAdView.setBodyView(view);
                    }
                    else if (tag == CALL_TO_ACTION_VIEW_TAG) {
                        mODCNativeAdView.setCallToActionView(view);
                    }
                    mODCNativeAdView.addView(view);
                }
                ad.registerViewForInteraction(mODCNativeAdView, clickableViews);
            }
            return true;
        }
    }
}