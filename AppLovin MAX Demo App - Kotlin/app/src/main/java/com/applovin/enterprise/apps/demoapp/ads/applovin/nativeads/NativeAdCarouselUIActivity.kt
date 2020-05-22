package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardMediaView
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardState
import com.applovin.nativeAds.AppLovinNativeAd
import com.applovin.nativeAds.AppLovinNativeAdLoadListener
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener
import com.applovin.sdk.AppLovinErrorCodes
import com.applovin.sdk.AppLovinPostbackListener
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkUtils
import kotlinx.android.synthetic.main.activity_native_ad_carousel_ui.*

class NativeAdCarouselUIActivity : AdStatusActivity()
{

    private val NUM_ADS_TO_LOAD = 1

    private var nativeAd: AppLovinNativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_ad_carousel_ui)

        adStatusTextView = status_label

        appIcon.setOnClickListener({
                                       nativeAd?.launchClickTarget(it.context)
                                   })

        loadButton.setOnClickListener {
            log("Native ad loading...")

            loadButton.isEnabled = false
            precacheButton.isEnabled = false
            showButton.isEnabled = false

            impressionStatusTextView.text = "No impression to track"

            loadNativeAds(NUM_ADS_TO_LOAD)
        }

        precacheButton.setOnClickListener({
                                              log("Native ad precaching...")

                                              val sdk = AppLovinSdk.getInstance(applicationContext)
                                              sdk.nativeAdService.precacheResources(nativeAd, object : AppLovinNativeAdPrecacheListener
                                              {
                                                  override fun onNativeAdImagesPrecached(appLovinNativeAd: AppLovinNativeAd)
                                                  {
                                                      log("Native ad precached images")
                                                  }

                                                  override fun onNativeAdVideoPreceached(appLovinNativeAd: AppLovinNativeAd)
                                                  {
                                                      // This will get called whether an ad actually has a video to precache or not
                                                      log("Native ad done precaching")
                                                      runOnUiThread {
                                                          showButton.isEnabled = true
                                                          precacheButton.isEnabled = false
                                                      }
                                                  }

                                                  override fun onNativeAdImagePrecachingFailed(appLovinNativeAd: AppLovinNativeAd, i: Int)
                                                  {
                                                      log("Native ad failed to precache images with error code " + i)
                                                  }

                                                  override fun onNativeAdVideoPrecachingFailed(appLovinNativeAd: AppLovinNativeAd, i: Int)
                                                  {
                                                      log("Native ad failed to precache videos with error code " + i)
                                                  }
                                              })
                                          })

        showButton.setOnClickListener({
                                          runOnUiThread {
                                              nativeAd?.let { nativeAd ->
                                                  log("Native ad rendered")

                                                  loadButton.isEnabled = true
                                                  showButton.isEnabled = false

                                                  appTitleTextView.text = nativeAd.title
                                                  appDescriptionTextView.text = nativeAd.descriptionText

                                                  AppLovinSdkUtils.safePopulateImageView(appIcon, Uri.parse(nativeAd.iconUrl), AppLovinSdkUtils.dpToPx(applicationContext, AppLovinCarouselViewSettings.ICON_IMAGE_MAX_SCALE_SIZE))

                                                  val starRatingDrawable = getStarRatingDrawable(nativeAd.starRating)
                                                  appRating.setImageDrawable(starRatingDrawable)

                                                  appDownloadButton.text = nativeAd.ctaText

                                                  val mediaView = InlineCarouselCardMediaView(this)
                                                  mediaView.ad = nativeAd
                                                  mediaView.cardState = InlineCarouselCardState()
                                                  mediaView.sdk = AppLovinSdk.getInstance(applicationContext)
                                                  mediaView.setUiHandler(Handler(Looper.getMainLooper()))
                                                  mediaView.setUpView()
                                                  mediaView.autoplayVideo()

                                                  mediaViewPlaceholder.removeAllViews()
                                                  mediaViewPlaceholder.addView(mediaView)

                                                  //
                                                  // You are responsible for firing impressions
                                                  //
                                                  trackImpression(nativeAd)
                                              }
                                          }
                                      })

        appDownloadButton.setOnClickListener({
                                                 nativeAd?.launchClickTarget(it.context)
                                             })
    }

    fun loadNativeAds(numAdsToLoad: Int)
    {
        val sdk = AppLovinSdk.getInstance(applicationContext)
        sdk.nativeAdService.loadNativeAds(numAdsToLoad, object : AppLovinNativeAdLoadListener
        {
            override fun onNativeAdsLoaded(list: List<AppLovinNativeAd>)
            {
                // Native ads loaded; do something with this, e.g. render into your custom view.

                runOnUiThread {
                    log("Native ad loaded, assets not retrieved yet.")

                    nativeAd = list[0]
                    precacheButton.isEnabled = true
                }
            }

            override fun onNativeAdsFailedToLoad(errorCode: Int)
            {
                // Native ads failed to load for some reason, likely a network error.
                // Compare errorCode to the available constants in AppLovinErrorCodes.

                log("Native ad failed to load with error code " + errorCode)

                if (errorCode == AppLovinErrorCodes.NO_FILL)
                {
                    // No ad was available for this placement
                }
                // else if (errorCode == .... ) { ... }
            }
        })
    }

    // Track an impression, though all other postbacks are handled identically
    private fun trackImpression(nativeAd: AppLovinNativeAd)
    {
        impressionStatusTextView.text = "Tracking Impression..."

        nativeAd.trackImpression(object : AppLovinPostbackListener
                                 {
                                     override fun onPostbackSuccess(url: String)
                                     {
                                         // Impression tracked!
                                         runOnUiThread { impressionStatusTextView.text = "Impression Tracked!" }
                                     }

                                     override fun onPostbackFailure(url: String, errorCode: Int)
                                     {
                                         // Impression could not be tracked. Retry the postback later.
                                         runOnUiThread { impressionStatusTextView.text = "Impression Failed to Track!" }
                                     }
                                 })
    }

    private fun getStarRatingDrawable(starRating: Float): Drawable
    {
        val sanitizedRating = java.lang.Float.toString(starRating).replace(".", "_")
        val resourceName = "applovin_star_sprite_" + sanitizedRating
        val drawableId = applicationContext.resources.getIdentifier(resourceName, "drawable", applicationContext.packageName)

        if (Build.VERSION.SDK_INT >= 22)
        {
            return applicationContext.resources.getDrawable(drawableId, theme)
        }
        else
        {
            @Suppress("DEPRECATION")
            return applicationContext.resources.getDrawable(drawableId)
        }
    }
}
