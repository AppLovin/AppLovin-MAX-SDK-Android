package com.applovin.enterprise.apps.demoapp.ads.applovin.rewarded;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class RewardedVideosDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Basic Integration", new Intent( this, RewardedVideosActivity.class ) ),
                new DemoMenuItem( "Zone Integration", new Intent( this, RewardedVideosZoneActivity.class ) )
        };
    }
}

