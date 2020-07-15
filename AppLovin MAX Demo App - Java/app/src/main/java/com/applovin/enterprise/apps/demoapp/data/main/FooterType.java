package com.applovin.enterprise.apps.demoapp.data.main;

import android.os.Build;

import com.applovin.enterprise.apps.demoapp.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

public class FooterType implements ListItem {

    /**
     * @return The ad type that will be shown.
     */
    public String getAppDetails()
    {
        String versionName = BuildConfig.VERSION_NAME;
        String sdkVersion = AppLovinSdk.VERSION;
        Integer deviceOs = Build.VERSION.SDK_INT;

        StringBuilder builder = new StringBuilder();
        builder.append("Version Name: ").append(versionName).append("\n");
        builder.append("SDK Version: ").append(sdkVersion).append(("\n"));
        builder.append("Android Version: ").append(deviceOs);

        return builder.toString();
    }

    @Override
    public int getType() {
        return TYPE_FOOTER;
    }
}
