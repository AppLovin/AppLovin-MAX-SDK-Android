package com.applovin.enterprise.apps.demoapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk


class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the AppLovin SDK
        AppLovinSdk.getInstance(this).mediationProvider = AppLovinMediationProvider.MAX
        AppLovinSdk.getInstance(this).initializeSdk {
            // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached
            // set up Adjust SDK
            val appToken = "{YourAppToken}"
            val environment = AdjustConfig.ENVIRONMENT_SANDBOX
            val config = AdjustConfig(this, appToken, environment)
            Adjust.onCreate(config)

            registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
        }
    }

    private class AdjustLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {}

        override fun onActivityStarted(p0: Activity) {}

        override fun onActivityResumed(p0: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(p0: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(p0: Activity) {}

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

        override fun onActivityDestroyed(p0: Activity) {}
    }
}