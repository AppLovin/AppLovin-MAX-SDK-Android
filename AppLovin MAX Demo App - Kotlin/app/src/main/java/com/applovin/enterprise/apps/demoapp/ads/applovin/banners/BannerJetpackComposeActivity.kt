package com.applovin.enterprise.apps.demoapp.ads.applovin.banners

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.AppLovinAdViewComposable
import com.applovin.enterprise.apps.demoapp.ui.composables.AppLovinAdViewComposableViewModel
import com.applovin.sdk.AppLovinAdSize

/**
 * [android.app.Activity] used to show AppLovin banner ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-26.
 */
class BannerJetpackComposeActivity : BaseJetpackComposeAdActivity() {
    private lateinit var bannerViewModel: AppLovinAdViewComposableViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view model.
        bannerViewModel = AppLovinAdViewComposableViewModel(this)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                {
                    ListCallbacks()
                    // Load new ad whenever button is tapped.
                    LoadButton(
                        onClick = { bannerViewModel.loadAd() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
                AppLovinAdViewComposable(AppLovinAdSize.BANNER, bannerViewModel)
            }
        }
    }
}