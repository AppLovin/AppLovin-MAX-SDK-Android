package com.applovin.enterprise.apps.demoapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.applovin.sdk.AppLovinSdkUtils;

import java.util.concurrent.TimeUnit;

public class SplashActivity
        extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_splash );

        AppLovinSdkUtils.runOnUiThreadDelayed( () -> {
            Intent intent = new Intent( SplashActivity.this, MainActivity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION );
            startActivity( intent );
        }, TimeUnit.SECONDS.toMillis( 2 ) );
    }
}
