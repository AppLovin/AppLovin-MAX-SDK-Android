package com.applovin.enterprise.apps.demoapp.ads.applovin.interstitials;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class InterstitialDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Basic Integration", new Intent( this, InterstitialBasicIntegrationActivity.class ) ),
                new DemoMenuItem( "Manually Loading Ad", new Intent( this, InterstitialManualLoadingActivity.class ) ),
                new DemoMenuItem( "Zone Integration", new Intent( this, InterstitialZoneActivity.class ) )
        };
    }
}
