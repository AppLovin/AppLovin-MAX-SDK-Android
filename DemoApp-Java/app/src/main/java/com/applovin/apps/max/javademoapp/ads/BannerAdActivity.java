package com.applovin.apps.max.javademoapp.ads;

import android.os.Bundle;

import com.applovin.apps.max.javademoapp.R;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX banner ads.
 * <p>
 * Created by santoshbagadi on 2019-09-10.
 */
public class BannerAdActivity
        extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_banner_ad );
    }
}
