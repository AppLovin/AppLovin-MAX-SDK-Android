package com.applovin.enterprise.apps.demoapp.ads.applovin.leaders

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener

class LeaderLayoutEditorActivity : AdStatusActivity()
{

    protected override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leader_layout_editor)

        adStatusTextView = findViewById(R.id.status_label) as TextView

        val adView = findViewById(R.id.ad_view) as AppLovinAdView

        val loadButton = findViewById(R.id.load_button) as Button

        loadButton.setOnClickListener { adView.loadNextAd() }

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
                                         // Look at AppLovinErrorCodes.java for list of error codes.
                                         log("Leader failed to load with error code $errorCode")
                                     }
                                 })

        adView.setAdDisplayListener(object : AppLovinAdDisplayListener
                                    {
                                        override fun adDisplayed(ad: AppLovinAd)
                                        {
                                            log("Leader displayed")
                                        }

                                        override fun adHidden(ad: AppLovinAd)
                                        {
                                            log("Leader hidden")
                                        }
                                    })

        adView.setAdClickListener { log("Leader clicked") }

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

        // Load an ad!
        adView.loadNextAd()

        //
        // Please note that the AppLovinAdView CAN AUTOMATICALLY invoke loadNextAd() upon inflation from layout
        // To do so, add the following attributes to the com.applovin.adview.AppLovinAdView element:
        //
        // xmlns:demo="http://schemas.applovin.com/android/1.0"
        // demo:loadAdOnCreate="true"
        //
    }
}