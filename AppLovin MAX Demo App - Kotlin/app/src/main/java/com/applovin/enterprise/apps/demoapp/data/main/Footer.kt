package com.applovin.enterprise.apps.demoapp.data.main

import android.os.Build
import android.os.Build.VERSION_CODES
import com.applovin.enterprise.apps.demoapp.BuildConfig
import com.applovin.sdk.AppLovinSdk


/**
 * A [ListItem] representing a footer on the main screen
 * <p>
 * Created by Jason Zheng on 7/15/2020.
 */
data class Footer(override var type: Int = ListItem.FOOTER) : ListItem {
    fun getFooterDetails(): String {
        val appVersion: String = BuildConfig.VERSION_NAME
        val sdkVersion: String = AppLovinSdk.VERSION
        val versionName = VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name
        val apiLevel = Build.VERSION.SDK_INT

        return """
            App Version: $appVersion
            SDK Version: $sdkVersion
            OS Version: $versionName(API $apiLevel)
            """.trimIndent()
    }
}
