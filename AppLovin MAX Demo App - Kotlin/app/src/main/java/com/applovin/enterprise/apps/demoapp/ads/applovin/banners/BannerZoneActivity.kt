package com.applovin.enterprise.apps.demoapp.ads.applovin.banners

import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import android.view.ViewGroup
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.sdk.*
import kotlinx.android.synthetic.main.activity_banner_programmatic.*

class BannerZoneActivity : AdStatusActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banner_programmatic)

        adStatusTextView = status_label
        val adView = AppLovinAdView(AppLovinAdSize.BANNER, "YOUR_ZONE_ID", this)
        adView.id = ViewCompat.generateViewId()

        load_button.setOnClickListener { adView.loadNextAd() }

        //
        // Optional: Set listeners
        //
        adView.setAdLoadListener(object : AppLovinAdLoadListener
                                 {
                                     override fun adReceived(ad: AppLovinAd)
                                     {
                                         log("Banner loaded")
                                     }

                                     override fun failedToReceiveAd(errorCode: Int)
                                     {
                                         // Look at AppLovinErrorCodes.java for list of error codes
                                         log("Banner failed to load with error code " + errorCode)
                                     }
                                 })

        adView.setAdDisplayListener(object : AppLovinAdDisplayListener
                                    {
                                        override fun adDisplayed(ad: AppLovinAd)
                                        {
                                            log("Banner Displayed")
                                        }

                                        override fun adHidden(ad: AppLovinAd)
                                        {
                                            log("Banner Hidden")
                                        }
                                    })

        adView.setAdClickListener { log("Banner Clicked") }

        adView.setAdViewEventListener(object : AppLovinAdViewEventListener
                                      {
                                          override fun adOpenedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?)
                                          {
                                              log("Banner opened fullscreen")
                                          }

                                          override fun adClosedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?)
                                          {

                                              log("Banner closed fullscreen")
                                          }

                                          override fun adLeftApplication(ad: AppLovinAd?, adView: AppLovinAdView?)
                                          {
                                              log("Banner left application")
                                          }

                                          override fun adFailedToDisplay(ad: AppLovinAd?, adView: AppLovinAdView?, code: AppLovinAdViewDisplayErrorCode?)
                                          {
                                              log("Banner failed to display with error code " + code)
                                          }
                                      })

        // Add programmatically created banner into our container
        banner_programmatic_content_layout.addView(adView, ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx(this, 50)))

        val constraintSet = ConstraintSet()
        constraintSet.clone(banner_programmatic_content_layout)
        constraintSet.connect(adView.id, ConstraintSet.BOTTOM, R.id.banner_programmatic_content_layout, ConstraintSet.BOTTOM, 0)
        constraintSet.applyTo(banner_programmatic_content_layout)

        // Load an ad!
        adView.loadNextAd()
    }
}
