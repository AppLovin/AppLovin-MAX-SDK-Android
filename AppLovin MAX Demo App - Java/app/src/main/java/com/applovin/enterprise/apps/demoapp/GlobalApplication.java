package com.applovin.enterprise.apps.demoapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GlobalApplication
        extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize the AppLovin SDK
        AppLovinSdk.getInstance( this ).setMediationProvider( AppLovinMediationProvider.MAX );
        AppLovinSdk.getInstance( this ).initializeSdk( config -> {
            // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached
            // set up Adjust SDK
            String appToken = "{YourAppToken}";
            String environment = AdjustConfig.ENVIRONMENT_SANDBOX;
            AdjustConfig adjustConfig = new AdjustConfig( getApplicationContext(), appToken, environment );
            Adjust.onCreate( adjustConfig );

            registerActivityLifecycleCallbacks( new AdjustLifecycleCallbacks() );
        } );
    }

    private static final class AdjustLifecycleCallbacks
            implements ActivityLifecycleCallbacks
    {
        @Override
        public void onActivityCreated(@NonNull final Activity activity, @Nullable final Bundle bundle) {}

        @Override
        public void onActivityStarted(@NonNull final Activity activity) {}

        @Override
        public void onActivityResumed(Activity activity)
        {
            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(Activity activity)
        {
            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(@NonNull final Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle bundle) {}

        @Override
        public void onActivityDestroyed(@NonNull final Activity activity) {}
    }
}
