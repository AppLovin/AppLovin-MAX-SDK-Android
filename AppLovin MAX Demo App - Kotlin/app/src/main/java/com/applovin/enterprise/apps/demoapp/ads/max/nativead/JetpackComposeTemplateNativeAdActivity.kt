package com.applovin.enterprise.apps.demoapp.ads.max.nativead

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxComposableNativeAdTemplateLoader
import com.applovin.enterprise.apps.demoapp.ui.composables.NativeTemplateAdComposable

class JetpackComposeTemplateNativeAdActivity : BaseJetpackComposeAdActivity() {
    private lateinit var adLoader: MaxComposableNativeAdTemplateLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_template_native_ad)

        // Initialize ad with ad loader.
        adLoader = MaxComposableNativeAdTemplateLoader("YOUR_AD_UNIT_ID", this, this)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                NativeTemplateAdComposable(adLoader)
                Box(Modifier.fillMaxSize())
                {
                    ListCallbacks()

                    // If ad is finished loading, show ad whenever button is tapped.
                    Button(
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(Color.LightGray),
                        onClick = { adLoader.loadAd() },
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
    }

    override fun onDestroy() {
        // Destroy ad loader to prevent memory leaks.
        adLoader.destroy()
        super.onDestroy()
    }
}
