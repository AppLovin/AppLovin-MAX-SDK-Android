package com.applovin.enterprise.apps.demoapp.ads.applovin.mrecs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.AppLovinAdComposable
import com.applovin.enterprise.apps.demoapp.ui.composables.AppLovinComposableAdLoader
import com.applovin.sdk.AppLovinAdSize

class MRecJetpackComposeActivity : BaseJetpackComposeAdActivity() {
    private lateinit var adLoader: AppLovinComposableAdLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ad with ad loader.
        adLoader = AppLovinComposableAdLoader(AppLovinAdSize.MREC, this, this)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                AppLovinAdComposable(adLoader)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxSize())
                {
                    ListCallbacks()

                    // Load ad whenever button is tapped.
                    TextButton(
                        onClick = { adLoader.loadAd() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    {
                        Text("Load")
                    }
                }
            }
        }
    }
}