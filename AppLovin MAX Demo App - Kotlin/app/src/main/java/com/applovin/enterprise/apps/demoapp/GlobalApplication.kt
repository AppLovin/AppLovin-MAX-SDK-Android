package com.applovin.enterprise.apps.demoapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk


class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the AppLovin SDK
        AppLovinSdk.getInstance(this).mediationProvider = AppLovinMediationProvider.MAX
        AppLovinSdk.getInstance(this).initializeSdk {
            // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached

            // Initialize Adjust SDK
            val config = AdjustConfig(this, "{YourAppToken}", AdjustConfig.ENVIRONMENT_SANDBOX)
            Adjust.onCreate(config)

            registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
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