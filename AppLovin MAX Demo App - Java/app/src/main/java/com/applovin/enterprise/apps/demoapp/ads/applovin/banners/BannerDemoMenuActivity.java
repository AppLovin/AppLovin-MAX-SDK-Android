package com.applovin.enterprise.apps.demoapp.ads.applovin.banners;

import android.content.Intent;
import com.applovin.enterprise.apps.demoapp.ui.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ui.DemoMenuItem;

public class BannerDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic", new Intent( this, BannerProgrammaticActivity.class ) ),
                new DemoMenuItem( "Layout Editor", new Intent( this, BannerProgrammaticActivity.class ) ),
                new DemoMenuItem( "Zone Integration", new Intent( this, BannerProgrammaticActivity.class ) ),
        };
        return result;
    }
}
