package com.applovin.enterprise.apps.demoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import com.applovin.sdk.AppLovinSdkUtils;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity
        extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_splash );

        TextView logoTextView = findViewById( R.id.logoTextView );
        logoTextView.setText( Html.fromHtml( "<b>App</b>Lovin" ) );

        AppLovinSdkUtils.runOnUiThreadDelayed( () -> {

            Intent intent = new Intent( SplashActivity.this, MainActivity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION );
            startActivity( intent );
        }, TimeUnit.SECONDS.toMillis( 2 ) );
    }
}
