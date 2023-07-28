package com.applovin.enterprise.apps.demoapp.ads.max.mrecs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxAdComposable
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxComposableAdLoader
import com.applovin.mediation.MaxAdFormat

/**
 * [android.app.Activity] used to show AppLovin MAX MREC ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-20.
 */

class JetpackComposeMrecAdActivity : BaseJetpackComposeAdActivity() {
    private lateinit var adLoader: MaxComposableAdLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_mrecs)

        // Initialize ad with ad loader.
        adLoader = MaxComposableAdLoader("YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this, this)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                MaxAdComposable(adLoader)
                Spacer(modifier = Modifier.height(4.dp))
                ListCallbacks()
            }
        }
    }
}
