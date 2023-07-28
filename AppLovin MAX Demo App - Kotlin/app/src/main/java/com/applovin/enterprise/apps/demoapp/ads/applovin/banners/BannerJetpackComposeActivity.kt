package com.applovin.enterprise.apps.demoapp.ads.applovin.banners

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.AppLovinAdComposable
import com.applovin.enterprise.apps.demoapp.ui.composables.AppLovinComposableAdLoader
import com.applovin.sdk.AppLovinAdSize

class BannerJetpackComposeActivity : BaseJetpackComposeAdActivity() {
    private lateinit var adLoader: AppLovinComposableAdLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ad with ad loader.
        adLoader = AppLovinComposableAdLoader(AppLovinAdSize.BANNER, this, this)

        setContent {
            Box(Modifier.fillMaxSize())
            {
                Box(Modifier.align(Alignment.TopCenter))
                {
                    ListCallbacks()
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                )
                {
                    // Load ad whenever button is tapped.
                    TextButton(
                        onClick = { adLoader.loadAd() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Load")
                    }
                    AppLovinAdComposable(adLoader)
                }
            }
        }

    }
}