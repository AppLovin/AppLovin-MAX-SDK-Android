package com.applovin.enterprise.apps.demoapp.ads.max.mrecs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxAdViewComposable
import com.applovin.enterprise.apps.demoapp.ui.composables.MaxAdViewComposableViewModel
import com.applovin.mediation.MaxAdFormat

/**
 * [android.app.Activity] used to show AppLovin MAX MREC ads using Jetpack Compose.
 * <p>
 * Created by Matthew Nguyen on 2023-07-20.
 */
class JetpackComposeMrecAdActivity : BaseJetpackComposeAdActivity() {
    private lateinit var mrecViewModel: MaxAdViewComposableViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.activity_jetpack_compose_mrecs)

        // Initialize ad with ad loader.
        mrecViewModel = MaxAdViewComposableViewModel(this)
        setContent {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                MaxAdViewComposable("YOUR_AD_UNIT_ID", MaxAdFormat.MREC, mrecViewModel)
                ListCallbacks()
            }
        }
    }
}
