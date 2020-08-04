package com.applovin.enterprise.apps.demoapp.ads.applovin.banner;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class BannerDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Programmatic", new Intent( this, BannerProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", new Intent( this, BannerLayoutEditorActivity.class ) ),
                new DemoMenuItem( "Zone Integration", new Intent( this, BannerZoneActivity.class ) ),
        };
    }
}
