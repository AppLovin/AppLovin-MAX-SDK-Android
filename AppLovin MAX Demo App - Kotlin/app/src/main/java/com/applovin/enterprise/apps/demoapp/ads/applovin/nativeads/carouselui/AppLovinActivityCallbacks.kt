package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui

import android.app.Activity

/**
 * Defines activity lifecycle methods which should be forwarded to an AppLovin view.
 */
interface AppLovinActivityCallbacks
{
    /**
     * Override your activity or fragment's onResume method and invoke this method with a reference to the current activity.
     * @param activity Current activity.
     */
    fun onResume(activity: Activity)

    /**
     * Override your activity or fragment's onResume method and invoke this method with a reference to the current activity.
     * @param activity Current activity.
     */
    fun onStop(activity: Activity)
}
