package com.applovin.enterprise.apps.demoapp.ads.max.banners;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ui.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.ui.DemoMenuItem;

public class MaxBannerDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic", new Intent( this, ProgrammaticBannerAdActivity.class ) ),
                new DemoMenuItem( "Layout", new Intent( this, LayoutEditorBannerAdActivity.class ) ),
        };
        return result;
    }
}
