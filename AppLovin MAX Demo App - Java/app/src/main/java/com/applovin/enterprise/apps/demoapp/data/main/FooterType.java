package com.applovin.enterprise.apps.demoapp.data.main;

import android.os.Build;

import com.applovin.enterprise.apps.demoapp.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

public class FooterType
        implements ListItem
{

    /**
     * @return The device details: App Version, SDK Version, OS Version
     */
    public String getAppDetails()
    {
        final String appVersion = BuildConfig.VERSION_NAME;
        final String sdkVersion = AppLovinSdk.VERSION;
        final String versionName = Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();
        final int apiLevel = Build.VERSION.SDK_INT;

        final String footer = "\nApp Version: " + appVersion +
                "\nSDK Version: " + sdkVersion +
                "\nOS Version: " + versionName + "(API " + apiLevel + ")";
        return footer;
    }

    @Override
    public int getType()
    {
        return TYPE_FOOTER;
    }
}
