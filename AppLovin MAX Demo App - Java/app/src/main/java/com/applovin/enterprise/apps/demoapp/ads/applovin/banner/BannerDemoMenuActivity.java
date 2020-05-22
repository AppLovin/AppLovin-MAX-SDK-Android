package com.applovin.enterprise.apps.demoapp.ads.applovin.banner;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuItem;

public class BannerDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic", "Programmatically creating an instance of it", new Intent( this, BannerProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", "Create a banner from the layout editor", new Intent( this, BannerLayoutEditorActivity.class ) ),
                new DemoMenuItem( "Zone Integration", "Create different user experiences of the same ad type", new Intent( this, BannerZoneActivity.class ) ),
        };
        return result;
    }
}
