package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials

import android.os.Bundle
import com.applovin.adview.AppLovinInterstitialAd
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity

import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdVideoPlaybackListener
import com.applovin.sdk.AppLovinSdk
import kotlinx.android.synthetic.main.activity_interstitial_manual_loading.*
import com.applovin.enterprise.apps.demoapp.R

class InterstitialZoneActivity : AdStatusActivity()
{
    private var currentAd: AppLovinAd? = null
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interstitial_manual_loading)

        adStatusTextView = status_label

        val sdk = AppLovinSdk.getInstance(applicationContext)
        val interstitialAdDialog = AppLovinInterstitialAd.create(sdk, this)

        loadButton.setOnClickListener {
            log("Interstitial loading...")
            showButton.isEnabled = false

            AppLovinSdk.getInstance(applicationContext).adService.loadNextAdForZoneId("YOUR_ZONE_ID", object : AppLovinAdLoadListener
            {
                override fun adReceived(ad: AppLovinAd)
                {
                    log("Interstitial Loaded")
                    currentAd = ad
                    runOnUiThread { showButton.isEnabled = true }
                }

                override fun failedToReceiveAd(errorCode: Int)
                {
                    // Look at AppLovinErrorCodes.java for list of error codes
                    log("Interstitial failed to load with error code " + errorCode)
                }
            })
        }

        showButton.setOnClickListener {
            currentAd?.let {
                interstitialAdDialog.showAndRender(it)
            }
        }

        //
        // Optional: Set ad display, ad click, and ad video playback callback listeners
        //
        interstitialAdDialog.setAdDisplayListener(object : AppLovinAdDisplayListener
                                                  {
                                                      override fun adDisplayed(appLovinAd: AppLovinAd)
                                                      {
                                                          log("Interstitial Displayed")
                                                      }

                                                      override fun adHidden(appLovinAd: AppLovinAd)
                                                      {
                                                          log("Interstitial Hidden")
                                                      }
                                                  })

        interstitialAdDialog.setAdClickListener { log("Interstitial Clicked") }

        // This will only ever be used if you have video ads enabled.
        interstitialAdDialog.setAdVideoPlaybackListener(object : AppLovinAdVideoPlaybackListener
                                                        {
                                                            override fun videoPlaybackBegan(appLovinAd: AppLovinAd)
                                                            {
                                                                log("Video Started")
                                                            }

                                                            override fun videoPlaybackEnded(appLovinAd: AppLovinAd, percentViewed: Double, wasFullyViewed: Boolean)
                                                            {
                                                                log("Video Ended")
                                                            }
                                                        })
    }
}
