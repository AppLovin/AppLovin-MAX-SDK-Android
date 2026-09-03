package com.applovin.mediation.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdapter
import com.applovin.mediation.adapter.MaxAdapter.InitializationStatus
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.MaxInterstitialAdapter
import com.applovin.mediation.adapter.MaxNativeAdAdapter
import com.applovin.mediation.adapter.MaxRewardedAdapter
import com.applovin.mediation.adapter.MaxSignalProvider
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters
import com.applovin.mediation.nativeAds.MaxNativeAd
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdk
import io.adn.sdk.publisher.AdnAdError
import io.adn.sdk.publisher.AdnAdInfo
import io.adn.sdk.publisher.AdnAdPlacement
import io.adn.sdk.publisher.AdnAdRequest
import io.adn.sdk.publisher.AdnBidTokenCallback
import io.adn.sdk.publisher.AdnClickElement
import io.adn.sdk.publisher.AdnFullscreenAd
import io.adn.sdk.publisher.AdnFullscreenAdListener
import io.adn.sdk.publisher.AdnInitializationCallback
import io.adn.sdk.publisher.AdnInitializationStatus
import io.adn.sdk.publisher.AdnLoadTimeout
import io.adn.sdk.publisher.AdnMediationType
import io.adn.sdk.publisher.AdnNativeAd
import io.adn.sdk.publisher.AdnNativeAdListener
import io.adn.sdk.publisher.AdnNativeInteractionHandler
import io.adn.sdk.publisher.AdnSdk
import java.util.concurrent.atomic.AtomicBoolean

class AdnMediationAdapter(sdk: AppLovinSdk) :
    MediationAdapterBase(sdk),
    MaxSignalProvider,
    MaxInterstitialAdapter,
    MaxRewardedAdapter,
    MaxNativeAdAdapter {

    private var interstitialAd: AdnFullscreenAd? = null
    private var rewardedAd: AdnFullscreenAd? = null
    private var nativeAd: AdnNativeAd? = null

    //region MaxAdapter Methods

    override fun initialize(
        parameters: MaxAdapterInitializationParameters,
        activity: Activity?,
        onCompletionListener: MaxAdapter.OnCompletionListener
    ) {
        if (initialized.compareAndSet(false, true)) {
            initializationStatus = InitializationStatus.INITIALIZING

            AdnSdk.setMediationType(AdnMediationType.MAX_ADS)
            AdnSdk.initialize(applicationContext, object : AdnInitializationCallback {
                override fun onCompletion(status: AdnInitializationStatus) {
                    if (status == AdnInitializationStatus.Success) {
                        log("Adn SDK initialized")
                        initializationStatus = InitializationStatus.INITIALIZED_SUCCESS
                        onCompletionListener.onCompletion(initializationStatus, null)
                    } else {
                        log("Adn SDK failed to initialize with error: $status")
                        initializationStatus = InitializationStatus.INITIALIZED_FAILURE
                        onCompletionListener.onCompletion(initializationStatus, status.toString())
                    }
                }
            })
        } else {
            onCompletionListener.onCompletion(initializationStatus, null)
        }
    }

    override fun getSdkVersion(): String = AdnSdk.getVersion()

    override fun getAdapterVersion(): String = BuildConfig.VERSION_NAME

    override fun onDestroy() {
        interstitialAd?.apply {
            setListener(null)
            destroy()
        }
        rewardedAd?.apply {
            setListener(null)
            destroy()
        }
        nativeAd?.apply {
            setListener(null)
            destroy()
        }

        interstitialAd = null
        rewardedAd = null
        nativeAd = null
    }

    //endregion

    //region MAX Signal Provider Methods

    override fun collectSignal(
        parameters: MaxAdapterSignalCollectionParameters,
        activity: Activity?,
        callback: MaxSignalCollectionListener
    ) {
        log("Collecting signal")

        val adPlacement = toAdnAdFormat(parameters.adFormat)
        if (adPlacement == null) {
            log("Signal collection failed: not implemented Ad format")
            callback.onSignalCollectionFailed("Not implemented Ad format")
            return
        }

        AdnSdk.getBidToken(adPlacement, object : AdnBidTokenCallback {
            override fun onComplete(response: String) {
                log("Signal collection successful")
                callback.onSignalCollected(response)
            }
        })
    }

    //endregion

    //region MAX Interstitial Adapter Methods

    override fun loadInterstitialAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxInterstitialAdapterListener
    ) {
        val placementId = parameters.thirdPartyAdPlacementId
        log("Loading interstitial ad: $placementId")

        if (!AdnSdk.isInitialized()) {
            listener.onInterstitialAdLoadFailed(MaxAdapterError.NOT_INITIALIZED)
            return
        }

        if (activity == null) {
            listener.onInterstitialAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY)
            return
        }

        updateMuteSetting(parameters)
        interstitialAd = AdnSdk.getInterstitialAdInstance(activity, InterstitialAdListener(listener))
        interstitialAd?.load(
            AdnAdRequest.AdBidRequest(
                placement = AdnAdPlacement.INTERSTITIAL,
                bidPayload = parameters.bidResponse,
                timeout = AdnLoadTimeout.MEDIATION
            )
        )
    }

    override fun showInterstitialAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxInterstitialAdapterListener
    ) {
        log("Showing interstitial ad: ${parameters.thirdPartyAdPlacementId}")

        if (interstitialAd?.isReady() != true) {
            log("Unable to show interstitial - ad not ready")
            listener.onInterstitialAdDisplayFailed(
                MaxAdapterError(
                    MaxAdapterError.AD_DISPLAY_FAILED,
                    MaxAdapterError.AD_NOT_READY.code,
                    MaxAdapterError.AD_NOT_READY.message
                )
            )
            return
        }

        interstitialAd?.show()
    }

    //endregion

    //region MAX Rewarded Adapter Methods

    override fun loadRewardedAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxRewardedAdapterListener
    ) {
        val placementId = parameters.thirdPartyAdPlacementId
        log("Loading rewarded ad: $placementId")

        if (!AdnSdk.isInitialized()) {
            listener.onRewardedAdLoadFailed(MaxAdapterError.NOT_INITIALIZED)
            return
        }

        if (activity == null) {
            listener.onRewardedAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY)
            return
        }

        updateMuteSetting(parameters)
        rewardedAd = AdnSdk.getRewardedAdInstance(activity, RewardedAdListener(listener))
        rewardedAd?.load(
            AdnAdRequest.AdBidRequest(
                placement = AdnAdPlacement.REWARDED,
                bidPayload = parameters.bidResponse,
                timeout = AdnLoadTimeout.MEDIATION
            )
        )
    }

    override fun showRewardedAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxRewardedAdapterListener
    ) {
        log("Showing rewarded ad: ${parameters.thirdPartyAdPlacementId}")

        if (rewardedAd?.isReady() != true) {
            log("Unable to show rewarded ad - ad not ready")
            listener.onRewardedAdDisplayFailed(
                MaxAdapterError(
                    MaxAdapterError.AD_DISPLAY_FAILED,
                    MaxAdapterError.AD_NOT_READY.code,
                    MaxAdapterError.AD_NOT_READY.message
                )
            )
            return
        }

        configureReward(parameters)
        rewardedAd?.show()
    }

    //endregion

    //region MAX Native Ad Adapter Methods

    override fun loadNativeAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxNativeAdAdapterListener
    ) {
        val placementId = parameters.thirdPartyAdPlacementId
        log("Loading native ad: $placementId")

        if (!AdnSdk.isInitialized()) {
            val adapterError = MaxAdapterError.NOT_INITIALIZED
            log("Native ad load failed with error ($adapterError)")
            listener.onNativeAdLoadFailed(adapterError)
            return
        }

        if (activity == null) {
            val adapterError = MaxAdapterError.MISSING_ACTIVITY
            log("Native ad load failed with error ($adapterError)")
            listener.onNativeAdLoadFailed(adapterError)
            return
        }

        updateMuteSetting(parameters)

        val createdNativeAd = AdnSdk.getNativeAdInstance(activity, NativeAdListener(listener))

        nativeAd = createdNativeAd
        createdNativeAd.load(
            AdnAdRequest.AdBidRequest(
                placement = AdnAdPlacement.NATIVE,
                bidPayload = parameters.bidResponse,
                timeout = AdnLoadTimeout.MEDIATION
            )
        )
    }

    //endregion

    //region Helper Methods

    private fun toAdnAdFormat(adFormat: MaxAdFormat) = when (adFormat) {
        MaxAdFormat.INTERSTITIAL -> AdnAdPlacement.INTERSTITIAL
        MaxAdFormat.REWARDED -> AdnAdPlacement.REWARDED
        MaxAdFormat.NATIVE -> AdnAdPlacement.NATIVE
        else -> null
    }

    private fun toMaxError(error: AdnAdError) = when (error) {
        AdnAdError.SdkNotInitialized -> MaxAdapterError.NOT_INITIALIZED
        AdnAdError.Timeout -> MaxAdapterError.TIMEOUT
        AdnAdError.BadRequest -> MaxAdapterError.BAD_REQUEST
        AdnAdError.ServerError -> MaxAdapterError.SERVER_ERROR
        AdnAdError.NoFill -> MaxAdapterError.NO_FILL
        else -> MaxAdapterError(error.errorCode, error.errorMessage)
    }

    private fun updateMuteSetting(parameters: MaxAdapterResponseParameters) {
        val serverParameters = parameters.serverParameters
        if (serverParameters.containsKey("is_muted")) {
            AdnSdk.setAdsMuted(serverParameters.getBoolean("is_muted"))
        }
    }

    //endregion

    //region Ad Listeners

    private inner class InterstitialAdListener(
        private val listener: MaxInterstitialAdapterListener
    ) : AdnFullscreenAdListener {

        override fun onAdLoaded(adInfo: AdnAdInfo) {
            log("Interstitial ad loaded")
            listener.onInterstitialAdLoaded()
        }

        override fun onAdLoadFailed(error: AdnAdError) {
            val adapterError = toMaxError(error)
            log("Interstitial ad failed to load with error: $adapterError")
            listener.onInterstitialAdLoadFailed(adapterError)
        }

        override fun onAdShown(adInfo: AdnAdInfo?) {}

        override fun onAdShowFailed(adInfo: AdnAdInfo?, error: AdnAdError) {
            val adapterError = MaxAdapterError(
                MaxAdapterError.AD_DISPLAY_FAILED,
                error.errorCode,
                error.errorMessage
            )
            log("Interstitial ad failed to display with error: $adapterError")
            listener.onInterstitialAdDisplayFailed(adapterError)
        }

        override fun onAdImpression(adInfo: AdnAdInfo?) {
            log("Interstitial ad displayed")
            listener.onInterstitialAdDisplayed()
        }

        override fun onAdClicked(adInfo: AdnAdInfo?) {
            log("Interstitial ad clicked")
            listener.onInterstitialAdClicked()
        }

        override fun onAdClosed(adInfo: AdnAdInfo?) {
            log("Interstitial ad hidden")
            listener.onInterstitialAdHidden()
        }

        override fun onAdRewarded(adInfo: AdnAdInfo?) {}
    }

    private inner class RewardedAdListener(
        private val listener: MaxRewardedAdapterListener
    ) : AdnFullscreenAdListener {

        private var hasGrantedReward = false

        override fun onAdLoaded(adInfo: AdnAdInfo) {
            log("Rewarded ad loaded")
            listener.onRewardedAdLoaded()
        }

        override fun onAdLoadFailed(error: AdnAdError) {
            val adapterError = toMaxError(error)
            log("Rewarded ad failed to load with error: $adapterError")
            listener.onRewardedAdLoadFailed(adapterError)
        }

        override fun onAdShown(adInfo: AdnAdInfo?) {}

        override fun onAdShowFailed(adInfo: AdnAdInfo?, error: AdnAdError) {
            val adapterError = MaxAdapterError(
                MaxAdapterError.AD_DISPLAY_FAILED,
                error.errorCode,
                error.errorMessage
            )
            log("Rewarded ad failed to display error: $adapterError")
            listener.onRewardedAdDisplayFailed(adapterError)
        }

        override fun onAdImpression(adInfo: AdnAdInfo?) {
            log("Rewarded ad displayed")
            listener.onRewardedAdDisplayed()
        }

        override fun onAdClicked(adInfo: AdnAdInfo?) {
            log("Rewarded ad clicked")
            listener.onRewardedAdClicked()
        }

        override fun onAdRewarded(adInfo: AdnAdInfo?) {
            log("Rewarded video granted reward")
            hasGrantedReward = true
        }

        override fun onAdClosed(adInfo: AdnAdInfo?) {
            if (hasGrantedReward || shouldAlwaysRewardUser()) {
                val reward = reward
                log("Rewarded user with reward: $reward")
                listener.onUserRewarded(reward)
            }

            log("Rewarded ad hidden")
            listener.onRewardedAdHidden()
        }
    }

    private inner class NativeAdListener(
        private val listener: MaxNativeAdAdapterListener
    ) : AdnNativeAdListener {

        override fun onAdLoaded(adInfo: AdnAdInfo) {
            log("Native ad loaded")
            nativeAd?.renderingInfo?.let { renderingInfo ->
                val builder = MaxNativeAd.Builder().apply {
                    setAdFormat(MaxAdFormat.NATIVE)
                    setTitle(renderingInfo.title)
                    setAdvertiser(renderingInfo.advertiser)
                    setBody(renderingInfo.body)
                    setMediaContentAspectRatio(renderingInfo.aspectRatio)
                    setCallToAction(renderingInfo.callToAction)
                    renderingInfo.mainMediaView?.let { setMediaView(it) }
                    renderingInfo.iconUri?.let { setIcon(MaxNativeAd.MaxNativeAdImage(it)) }
                    renderingInfo.privacyView?.let { setOptionsView(it) }
                }
                listener.onNativeAdLoaded(MaxAdnNativeAd(builder), null)
            } ?: run {
                e( "Native ad assets object is null" )
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS )
            }
        }

        override fun onAdMetadataLoaded(adInfo: AdnAdInfo) {}

        override fun onAdLoadFailed(error: AdnAdError) {
            val adapterError = toMaxError(error)
            log("Native ad load failed with error ($adapterError)")
            listener.onNativeAdLoadFailed(adapterError)
        }

        override fun onAdShowFailed(adInfo: AdnAdInfo?, error: AdnAdError) {
            val adapterError = MaxAdapterError(
                MaxAdapterError.AD_DISPLAY_FAILED,
                error.errorCode,
                error.errorMessage
            )
            log("Native ad failed to display with error: $adapterError")
        }

        override fun onAdImpression(adInfo: AdnAdInfo?) {
            log("Native ad displayed")
            listener.onNativeAdDisplayed(null)
        }

        override fun onAdClicked(adInfo: AdnAdInfo?) {
            log("Native ad clicked")
            listener.onNativeAdClicked()
        }
    }

    private inner class MaxAdnNativeAd(builder: Builder) : MaxNativeAd(builder) {

        override fun prepareForInteraction(clickableViews: List<View>, container: ViewGroup): Boolean {
            val ad = nativeAd
            if (ad == null) {
                e("Failed to register native ad view: native ad is null.")
                return false
            }

            d("Preparing views for interaction: $clickableViews with container: $container")

            if (!ad.isReady()) {
                log("Unable to show native ad - ad not ready")
                return false
            }

            val maxView = container as? MaxNativeAdView ?: return false
            val interactionHandler = AdnNativeInteractionHandler()

            maxView.callToActionButton?.let { interactionHandler.addClickableView(AdnClickElement.Cta, it) }
            maxView.mediaContentViewGroup?.let { interactionHandler.addClickableView(AdnClickElement.MainMedia, it) }
            maxView.bodyTextView?.let { interactionHandler.addClickableView(AdnClickElement.Body, it) }
            maxView.titleTextView?.let { interactionHandler.addClickableView(AdnClickElement.Title, it) }
            maxView.iconImageView?.let { interactionHandler.addClickableView(AdnClickElement.Icon, it) }
            maxView.advertiserTextView?.let { interactionHandler.addClickableView(AdnClickElement.Advertiser, it) }
            interactionHandler.addClickableView(AdnClickElement.Unknown, maxView)

            ad.prepareForInteraction(interactionHandler)
            return true
        }
    }

    //endregion

    companion object {
        private val initialized = AtomicBoolean()
        private var initializationStatus: InitializationStatus? = null
    }
}
