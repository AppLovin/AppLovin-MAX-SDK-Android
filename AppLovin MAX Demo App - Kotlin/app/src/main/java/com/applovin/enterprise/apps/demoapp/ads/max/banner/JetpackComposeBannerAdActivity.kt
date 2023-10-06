package com.applovin.enterprise.apps.demoapp.ads.max.banner

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxAdViewComposable
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxAdViewComposableViewModel
import com.applovin.mediation.MaxAdFormat
import com.applovin.sdk.AppLovinSdkUtils

/**
 * A [android.app.Activity] to show AppLovin MAX banner ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 7/13/2023
 */
class JetpackComposeBannerAdActivity : BaseJetpackComposeAdActivity() {
    private lateinit var bannerViewModel: MaxAdViewComposableViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_banners)

        // Initialize ad with ad loader.
        bannerViewModel = MaxAdViewComposableViewModel(this)

        val isTablet = AppLovinSdkUtils.isTablet(this)
        val adFormat = if (isTablet) MaxAdFormat.LEADER else MaxAdFormat.BANNER

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            )
            {
                MaxAdViewComposable("YOUR_AD_UNIT_ID", adFormat, bannerViewModel)
                ListCallbacks()
            }
        }
    }

}