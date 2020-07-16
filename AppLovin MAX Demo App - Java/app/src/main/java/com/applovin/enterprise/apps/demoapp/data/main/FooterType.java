package com.applovin.enterprise.apps.demoapp.data.main;

import android.os.Build;
import android.util.Log;

import com.applovin.enterprise.apps.demoapp.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

public class FooterType
        implements ListItem
{
    private static final String TAG = "Footer";

    /**
     * @return The device details: App Version, SDK Version, OS Version
     */
    public String getAppDetails()
    {
        String appVersion = BuildConfig.VERSION_NAME;
        String sdkVersion = AppLovinSdk.VERSION;
        String versionName = "";
        try
        {
            versionName = Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();
        }
        catch ( Throwable th )
        {
            Log.e( TAG, "Unable to get Android SDK codename", th );
        }
        int apiLevel = Build.VERSION.SDK_INT;

        String footer = "\nApp Version: " + appVersion +
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
