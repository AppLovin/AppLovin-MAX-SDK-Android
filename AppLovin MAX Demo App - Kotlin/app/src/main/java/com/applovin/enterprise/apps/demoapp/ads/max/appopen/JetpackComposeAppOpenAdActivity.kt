package com.applovin.enterprise.apps.demoapp.ads.max.appopen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxAdRevenueListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd

/**
 * [android.app.Activity] used to show AppLovin MAX App Open ads in Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-20.
 */

class JetpackComposeAppOpenAdActivity : BaseJetpackComposeAdActivity(),
    MaxAdListener, MaxAdRevenueListener {
    private lateinit var appOpenAd: MaxAppOpenAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_app_open)

        appOpenAd = MaxAppOpenAd("YOUR_AD_UNIT_ID", this)

        appOpenAd.setListener(this)
        appOpenAd.setRevenueListener(this)

        // Load the first ad.
        appOpenAd.loadAd()

        setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopCenter))
                {
                    ListCallbacks()
                }
                Button(
                    onClick = {
                        if (appOpenAd.isReady) {
                            appOpenAd.showAd()
                        }
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(Color.LightGray),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                {
                    Text(
                        text = "SHOW AD",
                        color = Color.Black
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Destroy interstitial ad to prevent memory leaks.
        appOpenAd.destroy()
    }


    //region MAX Ad Listener

    override fun onAdLoaded(ad: MaxAd?) {
        // App Open ad is ready to be shown. AppOpenAd.isReady() will now return 'true'.
        logCallback()
    }

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {
        logCallback()
    }

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {
        logCallback()

        // App Open ad failed to display. We recommend loading the next ad.
        appOpenAd.loadAd()
    }

    override fun onAdDisplayed(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdClicked(ad: MaxAd?) {
        logCallback()
    }

    override fun onAdHidden(ad: MaxAd?) {
        logCallback()

        // App Open Ad is hidden. Pre-load the next ad
        appOpenAd.loadAd()
    }

    //endregion

    //region MAX Ad Revenue Listener

    override fun onAdRevenuePaid(ad: MaxAd?) {
        logCallback()

        val adjustAdRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_APPLOVIN_MAX)
        adjustAdRevenue.setRevenue(ad?.revenue, "USD")
        adjustAdRevenue.setAdRevenueNetwork(ad?.networkName)
        adjustAdRevenue.setAdRevenueUnit(ad?.adUnitId)
        adjustAdRevenue.setAdRevenuePlacement(ad?.placement)

        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    //endregion
}