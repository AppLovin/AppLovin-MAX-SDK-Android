package com.applovin.enterprise.apps.demoapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GlobalApplication
        extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        // If you want to test your own AppLovin SDK key,
        // update the value in AndroidManifest.xml under the "applovin.sdk.key" key, and update the package name to your app's name.

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute( () -> {
            // Enable test mode by default for the current device. Cannot be run on the main thread.
            String currentGaid = null;
            try
            {
                currentGaid = AdvertisingIdClient.getAdvertisingIdInfo( this ).getId();
            }
            catch ( Throwable ignored ) { }

            AppLovinSdkSettings settings = new AppLovinSdkSettings( this );
            if ( currentGaid != null )
            {
                settings.setTestDeviceAdvertisingIds( Collections.singletonList( currentGaid ) );
            }

            // Initialize the AppLovin SDK
            AppLovinSdk sdk = AppLovinSdk.getInstance( settings, this );
            sdk.setMediationProvider( AppLovinMediationProvider.MAX );
            sdk.initializeSdk( appLovinSdkConfiguration -> {
                // AppLovin SDK is initialized, start loading ads now or later if ad gate is reached

                // Initialize Adjust SDK
                AdjustConfig adjustConfig = new AdjustConfig( getApplicationContext(), "{YourAppToken}", AdjustConfig.ENVIRONMENT_SANDBOX );
                Adjust.onCreate( adjustConfig );

                registerActivityLifecycleCallbacks( new AdjustLifecycleCallbacks() );
            } );

            executor.shutdown();
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
