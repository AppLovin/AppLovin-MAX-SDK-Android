package com.applovin.enterprise.apps.demoapp.ads.max.nativead

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxTemplateNativeAdViewComposableLoader
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxTemplateNativeAdViewComposable

/**
 * [android.app.Activity] used to show AppLovin MAX native ads using the Templates API with Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-25.
 */
class JetpackComposeTemplateNativeAdActivity : BaseJetpackComposeAdActivity() {
    private lateinit var nativeAdLoader: MaxTemplateNativeAdViewComposableLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_template_native_ad)

        // Initialize ad with ad loader.
        nativeAdLoader = MaxTemplateNativeAdViewComposableLoader("YOUR_AD_UNIT_ID", this, this)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                MaxTemplateNativeAdViewComposable(nativeAdLoader)
                Box(Modifier.fillMaxSize())
                {
                    ListCallbacks()
                    // If ad is finished loading, show ad whenever button is tapped.
                    ShowAdButton(
                        onClick = { nativeAdLoader.loadAd() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        // Destroy ad loader to prevent memory leaks.
        nativeAdLoader.destroy()
        super.onDestroy()
    }
}
