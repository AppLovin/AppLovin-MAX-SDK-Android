package com.applovin.enterprise.apps.demoapp.ads;

import android.os.Bundle;

import com.applovin.enterprise.apps.demoapp.R;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX interstitial ads.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class InterstitialAdActivity
        extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_interstitial_ad );
    }
}
