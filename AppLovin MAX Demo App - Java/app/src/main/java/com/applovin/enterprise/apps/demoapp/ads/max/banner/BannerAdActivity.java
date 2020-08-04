package com.applovin.enterprise.apps.demoapp.ads.max.banner;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class BannerAdActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        DemoMenuItem[] result = {
                new DemoMenuItem( "Programmatic Banners", new Intent( this, ProgrammaticBannerAdActivity.class ) ),
                new DemoMenuItem( "Layout Editor Banners", new Intent( this, LayoutEditorBannerAdActivity.class ) ),
        };
        return result;
    }
}
