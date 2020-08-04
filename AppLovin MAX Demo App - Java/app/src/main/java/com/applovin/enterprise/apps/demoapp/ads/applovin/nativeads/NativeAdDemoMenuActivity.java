package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads;

import android.content.Intent;

import com.applovin.enterprise.apps.demoapp.ads.applovin.DemoMenuActivity;
import com.applovin.enterprise.apps.demoapp.data.main.DemoMenuItem;

public class NativeAdDemoMenuActivity
        extends DemoMenuActivity
{
    @Override
    protected DemoMenuItem[] getListViewContents()
    {
        return new DemoMenuItem[] {
                new DemoMenuItem( "Single Ad", new Intent( this, NativeAdCarouselUIActivity.class ) ),
                new DemoMenuItem( "Multiple Ads", new Intent( this, NativeAdRecyclerViewActivity.class ) )
        };
    }
}
