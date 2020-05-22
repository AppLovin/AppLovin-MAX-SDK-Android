package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs


import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import android.widget.Button
import android.widget.TextView
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.ads.AdStatusActivity
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.sdk.*

/**
 * Created by monica ong on 7/20/17.
 */

class MRecProgrammaticActivity : AdStatusActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mrec_programmatic)

        adStatusTextView = findViewById(R.id.status_label) as TextView

        // Create MRec
        val adView = AppLovinAdView(AppLovinAdSize.MREC, this)
        adView.id = ViewCompat.generateViewId()

        val mrecConstraintLayout = findViewById<ConstraintLayout>(R.id.mrec_programmatic_constraint_layout)
        val layoutParams = ConstraintLayout.LayoutParams(AppLovinSdkUtils.dpToPx(this, AppLovinAdSize.MREC.width),
                                                         AppLovinSdkUtils.dpToPx(this, AppLovinAdSize.MREC.height))
        mrecConstraintLayout.addView(adView, layoutParams)

        val set = ConstraintSet()
        set.clone(mrecConstraintLayout)
        set.connect(adView.id, ConstraintSet.TOP, mrecConstraintLayout.id, ConstraintSet.TOP, AppLovinSdkUtils.dpToPx(this, 80))
        set.centerHorizontally(adView.id, mrecConstraintLayout.id)
        set.applyTo(mrecConstraintLayout)

        // Set up load button
        val loadButton = findViewById(R.id.load_button) as Button

        loadButton.setOnClickListener { adView.loadNextAd() }

        //
        // Optional: Set listeners
        //
        adView.setAdLoadListener(object : AppLovinAdLoadListener
                                 {
                                     override fun adReceived(ad: AppLovinAd)
                                     {
                                         log("MRec loaded")
                                     }

                                     override fun failedToReceiveAd(errorCode: Int)
                                     {
                                         // Look at AppLovinErrorCodes.java for list of error codes
                                         log("MRec failed to load with error code " + errorCode)
                                     }
                                 })

        adView.setAdDisplayListener(object : AppLovinAdDisplayListener
                                    {
                                        override fun adDisplayed(ad: AppLovinAd)
                                        {
                                            log("MRec Displayed")
                                        }

                                        override fun adHidden(ad: AppLovinAd)
                                        {
                                            log("MRec Hidden")
                                        }
                                    })

        adView.setAdClickListener(AppLovinAdClickListener { log("MRec Clicked") })

        adView.setAdViewEventListener(object : AppLovinAdViewEventListener
                                      {
                                          override fun adOpenedFullscreen(ad: AppLovinAd, adView: AppLovinAdView)
                                          {
                                              log("MRec opened fullscreen")
                                          }

                                          override fun adClosedFullscreen(ad: AppLovinAd, adView: AppLovinAdView)
                                          {
                                              log("MRec closed fullscreen")
                                          }

                                          override fun adLeftApplication(ad: AppLovinAd, adView: AppLovinAdView)
                                          {
                                              log("MRec left application")
                                          }

                                          override fun adFailedToDisplay(ad: AppLovinAd, adView: AppLovinAdView, code: AppLovinAdViewDisplayErrorCode)
                                          {
                                              log("MRec failed to display with error code " + code)
                                          }
                                      })
    }
}
