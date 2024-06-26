package com.applovin.enterprise.apps.demoapp.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import com.applovin.adview.AppLovinAdView
import com.applovin.adview.AppLovinAdViewDisplayErrorCode
import com.applovin.adview.AppLovinAdViewEventListener
import com.applovin.enterprise.apps.demoapp.ui.BaseJetpackComposeAdActivity
import com.applovin.sdk.AppLovinAd
import com.applovin.sdk.AppLovinAdClickListener
import com.applovin.sdk.AppLovinAdDisplayListener
import com.applovin.sdk.AppLovinAdLoadListener
import com.applovin.sdk.AppLovinAdSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * View Model for AppLovin banner/leader/MREC ads using Jetpack Compose.
 */
class AppLovinAdViewComposableViewModel(baseActivity: BaseJetpackComposeAdActivity) : ViewModel() {
    private val _shouldLoadAd = MutableStateFlow(true)
    val shouldLoadAd: StateFlow<Boolean> get() = _shouldLoadAd

    fun loadAd() {
        _shouldLoadAd.value = true
    }

    val adLoadListener = object : AppLovinAdLoadListener {
        override fun adReceived(ad: AppLovinAd?) {
            baseActivity.logCallback()
        }

        override fun failedToReceiveAd(errorCode: Int) {
            // Look at AppLovinErrorCodes.java for list of error codes
            baseActivity.logCallback()
        }
    }

    val adDisplayListener = object : AppLovinAdDisplayListener {
        override fun adDisplayed(ad: AppLovinAd?) {
            _shouldLoadAd.value = false
            baseActivity.logCallback()

        }

        override fun adHidden(ad: AppLovinAd?) {
            baseActivity.logCallback()
        }
    }

    val adViewEventListener = object : AppLovinAdViewEventListener {
        override fun adOpenedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?) {
            baseActivity.logCallback()
        }

        override fun adClosedFullscreen(ad: AppLovinAd?, adView: AppLovinAdView?) {
            baseActivity.logCallback()
        }

        override fun adLeftApplication(ad: AppLovinAd?, adView: AppLovinAdView?) {
            baseActivity.logCallback()
        }

        override fun adFailedToDisplay(
            ad: AppLovinAd?,
            adView: AppLovinAdView?,
            code: AppLovinAdViewDisplayErrorCode?
        ) {
            _shouldLoadAd.value = false
            baseActivity.logCallback()
        }
    }

    val adClickListener = object : AppLovinAdClickListener {
        override fun adClicked(ad: AppLovinAd?) {
            baseActivity.logCallback()
        }
    }
}

/**
 * Jetpack Compose function to display AppLovin banner/leader/MREC ads.
 */
@Composable
fun AppLovinAdViewComposable(
    adFormat: AppLovinAdSize,
    viewModel: AppLovinAdViewComposableViewModel
) {
    val shouldLoadAd = viewModel.shouldLoadAd.collectAsState().value
    val context = LocalContext.current

    val adViewModifier = when (adFormat) {
        // Set background or background color for ads to be fully functional.
        AppLovinAdSize.BANNER -> Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black)

        AppLovinAdSize.LEADER -> Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.Black)

        AppLovinAdSize.MREC -> Modifier
            .width(300.dp)
            .height(250.dp)
            .background(Color.Black)

        else -> Modifier
            .fillMaxSize()
            .background(Color.Black)
    }

    val adView = remember {
        AppLovinAdView(adFormat, context).apply {
            setAdLoadListener(viewModel.adLoadListener)
            setAdDisplayListener(viewModel.adDisplayListener)
            setAdViewEventListener(viewModel.adViewEventListener)
            setAdClickListener(viewModel.adClickListener)
        }
    }

    AndroidView(
        factory = { adView },
        update = {
            // Load an ad once layout is inflated.
            if (shouldLoadAd) {
                adView.loadNextAd()
            }
        },
        modifier = adViewModifier,
    )

    DisposableEffect(adView) {
        onDispose {
            // Destroy ad view when composition is no longer needed.
            adView.destroy()
        }
    }
}