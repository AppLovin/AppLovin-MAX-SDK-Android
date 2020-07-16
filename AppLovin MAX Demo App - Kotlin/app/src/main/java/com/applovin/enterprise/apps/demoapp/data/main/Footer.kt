package com.applovin.enterprise.apps.demoapp.data.main

import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import com.applovin.enterprise.apps.demoapp.BuildConfig
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselView
import com.applovin.sdk.AppLovinSdk


/**
 * A [ListItem] representing a footer on the main screen
 * <p>
 * Created by Jason Zheng on 7/15/2020.
 */
data class Footer(override val type: Int = ListItem.FOOTER) : ListItem
{
    fun getAppDetails(): String
    {
        val appVersion: String = BuildConfig.VERSION_NAME
        val sdkVersion: String = AppLovinSdk.VERSION
        var versionName: String = ""
        try
        {
            versionName = VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name
        }
        catch (ex: Exception)
        {
            Log.e(Footer.TAG, "Unable to get Android SDK codename", ex)
        }

        val apiLevel = Build.VERSION.SDK_INT

        return """
            App Version: $appVersion
            SDK Version: $sdkVersion
            OS Version: $versionName(API $apiLevel)
            """.trimIndent()
    }

    companion object
    {
        private val TAG = "Footer"
    }
}
