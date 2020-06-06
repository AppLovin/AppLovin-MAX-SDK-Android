package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders

import android.os.Bundle
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.sdk.*
import kotlinx.android.synthetic.main.activity_leader_programmatic.*

class LeaderProgrammaticActivity : AdStatusActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_programmatic)

        adStatusTextView = status_label

        val adView = AppLovinAdView(AppLovinAdSize.LEADER, this)

        load_button.setOnClickListener { adView.loadNextAd() }

        //
        // Optional: Set listeners
        //
        adView.setAdLoadListener(object : AppLovinAdLoadListener
                                 {
                                     override fun adReceived(ad: AppLovinAd)
                                     {
                                         log("Leader loaded")
                                     }

                                     override fun failedToReceiveAd(errorCode: Int)
                                     {
                                         // Look at AppLovinErrorCodes.java for list of error codes
                                         log("Leader failed to load with error code $errorCode")
                                     }
                                 })

        adView.setAdDisplayListener(object : AppLovinAdDisplayListener
                                    {
                                        override fun adDisplayed(ad: AppLovinAd)
                                        {
                                            log("Leader Displayed")
                                        }

                                        override fun adHidden(ad: AppLovinAd)
                                        {
                                            log("Leader Hidden")
                                        }
                                    })

        adView.setAdClickListener { log("Banner Clicked") }

        adView.setAdViewEventListener(object : AppLovinAdViewEventListener
                                      {
                                          override fun adOpenedFullscreen(ad: AppLovinAd, adView: AppLovinAdView)
                                          {
                                              log("Leader opened fullscreen")
                                          }

                                          override fun adClosedFullscreen(ad: AppLovinAd, adView: AppLovinAdView)
                                          {
                                              log("Leader closed fullscreen")
                                          }

                                          override fun adLeftApplication(ad: AppLovinAd, adView: AppLovinAdView)
                                          {
                                              log("Leader left application")
                                          }

                                          override fun adFailedToDisplay(ad: AppLovinAd, adView: AppLovinAdView, code: AppLovinAdViewDisplayErrorCode)
                                          {
                                              log("Leader failed to display with error code $code")
                                          }
                                      })

        // Add programmatically created banner into our container
        leader_programmatic_layout.addView(adView, ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AppLovinSdkUtils.dpToPx(this, 90)))

        val constraintSet = ConstraintSet()
        constraintSet.clone(leader_programmatic_layout)
        constraintSet.connect(adView.id, ConstraintSet.BOTTOM, R.id.leader_programmatic_layout, ConstraintSet.BOTTOM, 0)
        constraintSet.applyTo(leader_programmatic_layout)

        // Load an ad!
        adView.loadNextAd()
    }
}