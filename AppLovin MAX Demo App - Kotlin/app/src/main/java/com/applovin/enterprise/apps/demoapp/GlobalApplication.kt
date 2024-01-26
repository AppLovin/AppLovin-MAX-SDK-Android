package com.applovin.enterprise.apps.demoapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.Collections
import java.util.concurrent.Executors


class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // If you want to test your own AppLovin SDK key,
        // update the value in AndroidManifest.xml under the "applovin.sdk.key" key, and update the package name to your app's name.

        val executor = Executors.newSingleThreadExecutor();
        executor.execute {
            // Enable test mode by default for the current device. Cannot be run on the main thread.
            val currentGaid = AdvertisingIdClient.getAdvertisingIdInfo(this).id

            val settings = AppLovinSdkSettings(this)
            if (currentGaid != null) {
                settings.testDeviceAdvertisingIds = Collections.singletonList(currentGaid)
            }

            // Initialize the AppLovin SDK
            val sdk = AppLovinSdk.getInstance(settings, this)
            sdk.mediationProvider = AppLovinMediationProvider.MAX
            sdk.initializeSdk {
                // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached

                // Initialize Adjust SDK
                val config = AdjustConfig(this, "{YourAppToken}", AdjustConfig.ENVIRONMENT_SANDBOX)
                Adjust.onCreate(config)

                registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
            }

            executor.shutdown()
        }
    }

    private class AdjustLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}

        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}
    }
}